"""对话引擎：意图识别 → 工具调用 → 组织回答。

LLM 可选：Ollama 可用时用它润色回答；不可用时规则模板直接作答（离线可演示、可测试）。
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

from app.agent import llm
from app.agent.knowledge import get_store
from app.agent.tools import BackendTools, ToolError

SYSTEM_PROMPT = (
    "你是「智泊停车」的客服助手。只根据提供的工具结果与知识库片段回答，"
    "不要编造车位数量、价格或订单信息；不确定时建议用户在小程序内查看。"
    "回答简洁，中文，控制在 120 字以内。"
)

GREETING_PATTERN = re.compile(r"^(你好|您好|hi|hello|在吗|嗨)[!！。?？\s]*$", re.IGNORECASE)


@dataclass
class ChatResult:
    answer: str
    intent: str
    tool_calls: list[dict] = field(default_factory=list)
    sources: list[dict] = field(default_factory=list)
    suggestions: list[str] = field(default_factory=list)


def _has_any(text: str, words: tuple[str, ...]) -> bool:
    return any(word in text for word in words)


def detect_intent(message: str) -> str:
    text = message.strip()
    if not text:
        return "unknown"
    if GREETING_PATTERN.match(text):
        return "greeting"

    if _has_any(text, ("预测", "趋势", "占用率", "高峰", "会不会满", "会满", "满吗", "紧张", "空位多")):
        return "forecast"
    if _has_any(text, ("停车中", "正在停", "我停", "停多久", "还要交多少")):
        return "current"
    if _has_any(text, ("我的预约", "预约记录", "预约到", "什么时候可以停", "有没有预约")):
        return "reservations"
    if _has_any(text, ("我的订单", "订单", "账单", "欠费", "待支付", "发票")):
        return "orders"
    if _has_any(text, ("车位", "停车场", "附近", "空闲", "哪里停", "有空")):
        return "lots"
    return "knowledge"


def _fallback_suggestions(intent: str) -> list[str]:
    mapping = {
        "lots": ["怎么预约车位", "停车怎么收费"],
        "orders": ["我的预约", "停车怎么收费"],
        "current": ["离场怎么结算", "我的订单"],
        "forecast": ["今天下午有车位吗", "停车怎么收费"],
        "knowledge": ["附近哪里有空位", "我的订单"],
        "unknown": ["附近哪里有空位", "停车怎么收费", "我的预约"],
    }
    return mapping.get(intent, ["附近哪里有空位", "停车怎么收费"])


def _polish(question: str, facts: str):

    if not llm.available():
        return None
    prompt = f"用户问题：{question}\n\n工具与知识库结果：\n{facts}\n\n请基于以上信息回答用户。"
    return llm.generate(prompt, system=SYSTEM_PROMPT)


def chat(message: str, token: str | None = None) -> ChatResult:
    intent = detect_intent(message)
    tools = BackendTools(token)
    tool_calls: list[dict] = []
    facts: list[str] = []
    sources: list[dict] = []
    answer = ""

    if intent == "greeting":
        answer = "你好，我是智泊停车助手。可以帮你查空闲车位、预约、订单，或解答收费与规则问题。"
        return ChatResult(answer, intent, [], [], ["附近哪里有空位", "我的预约", "停车怎么收费"])

    if intent == "lots":
        keyword = _extract_keyword(message)
        try:
            result = tools.list_lots(keyword)
            tool_calls.append({"name": result.name, "ok": result.ok, "summary": result.summary})
            facts.append(result.summary)
            answer = f"为你找到这些停车场：\n{result.summary}\n\n需要我帮你预约其中某个车位吗？"
        except ToolError as exc:
            hint = "登录后我可以直接帮你查实时空位。" if "登录" in str(exc) else "稍后再试，或先在首页查看。"
            answer = f"{exc}。{hint}"
            tool_calls.append({"name": "list_lots", "ok": False, "summary": str(exc)})

    elif intent in {"reservations", "orders", "current"}:
        tool_map = {
            "reservations": ("my_reservations", tools.my_reservations, "你的预约情况："),
            "orders": ("my_orders", tools.my_orders, "你的订单情况："),
            "current": ("current_parking", tools.current_parking, ""),
        }
        name, invoker, prefix = tool_map[intent]
        try:
            result = invoker()
            tool_calls.append({"name": result.name, "ok": result.ok, "summary": result.summary})
            facts.append(result.summary)
            answer = f"{prefix}{result.summary}" if result.summary else "暂时没有查到记录。"
        except ToolError as exc:
            answer = f"{exc}。请登录后再试。"
            tool_calls.append({"name": name, "ok": False, "summary": str(exc)})

    elif intent == "forecast":
        try:
            result = tools.forecast(hours=6)
            tool_calls.append({"name": result.name, "ok": result.ok, "summary": result.summary})
            facts.append(result.summary)
            answer = result.summary + "\n预测为概率估计，实际以现场为准。"
        except ToolError as exc:
            answer = f"{exc}，你可以先在首页查看实时空闲车位。"
            tool_calls.append({"name": "forecast", "ok": False, "summary": str(exc)})

    else:  # knowledge / unknown
        hits = get_store().search(message, top_k=3)
        sources = [{"title": hit["title"], "score": hit["score"], "snippet": hit["snippet"]} for hit in hits]
        tool_calls.append({"name": "search_knowledge", "ok": bool(hits), "summary": f"命中 {len(hits)} 篇文档"})
        if hits:
            facts.extend(f"{hit['title']}：{hit['snippet']}" for hit in hits)
            answer = f"{hits[0]['snippet']}\n\n（参考：{hits[0]['title']}）"
            if len(hits) > 1:
                answer += f"\n相关：《{hits[1]['title']}》"
        else:
            answer = "这个问题我暂时没有查到。你可以问我：空闲车位、预约规则、收费标准，或者直接查看「我的订单」。"
            intent = "unknown"

    answer = _pretty(answer)
    polished = _polish(message, "\n".join(facts)) if answer else None
    if polished:
        answer = polished

    return ChatResult(answer, intent, tool_calls, sources, _fallback_suggestions(intent))


def _pretty(text: str) -> str:
    """把 markdown 列表符号换成圆点，适配移动端气泡展示。"""
    lines = []
    for line in text.splitlines():
        stripped = line.lstrip()
        if stripped.startswith("- "):
            lines.append("• " + stripped[2:])
        else:
            lines.append(line)
    return "\n".join(lines)


def _extract_keyword(message: str) -> str | None:
    """从「科技园有空位吗」这类句子里粗粒度抽取停车场关键词。"""
    cleaned = re.sub(r"(有没有|有吗|吗|呢|请问|帮我|看看|查一下|附近|哪里|哪个|的|车位|停车场|空位|空闲)", "", message)
    cleaned = cleaned.strip("？?。!！，, ")
    return cleaned or None


def stream_chunks(text: str, size: int = 18):
    """把完整回答切成小块，供 SSE 逐段推送（前端呈现打字机效果）。"""
    for start in range(0, len(text), size):
        yield text[start:start + size]
