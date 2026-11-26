"""对话引擎测试：意图识别、工具调用、降级路径（不依赖 Ollama 与真实后端）。"""

from __future__ import annotations

from pathlib import Path

import pytest

from app.agent import engine
from app.agent.knowledge import reset_store
from app.agent.tools import BackendTools, ToolResult


@pytest.fixture(autouse=True)
def isolated_knowledge(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AI_KNOWLEDGE_PATH", str(tmp_path / "knowledge.json"))
    reset_store()


@pytest.mark.parametrize(
    ("message", "intent"),
    [
        ("你好", "greeting"),
        ("附近哪里有空位", "lots"),
        ("我的预约", "reservations"),
        ("我的订单还有待支付吗", "orders"),
        ("我停车中吗", "current"),
        ("今天下午车位会满吗", "forecast"),
        ("停车怎么收费", "knowledge"),
    ],
)
def test_detect_intent(message: str, intent: str) -> None:
    assert engine.detect_intent(message) == intent


def test_lots_answer_contains_free_count(monkeypatch: pytest.MonkeyPatch) -> None:
    def fake_list_lots(self, keyword=None):
        return ToolResult("list_lots", True, "· 科技园停车场（南山区）：空闲 3/120 位，首小时 ¥6 起")

    monkeypatch.setattr(BackendTools, "list_lots", fake_list_lots)
    result = engine.chat("附近哪里有空位", token="fake-token")

    assert "科技园停车场" in result.answer
    assert "空闲 3/120" in result.answer
    assert result.tool_calls[0]["name"] == "list_lots"


def test_personal_query_without_token_asks_login() -> None:
    result = engine.chat("我的预约", token=None)
    assert "登录" in result.answer
    assert result.tool_calls[0]["ok"] is False


def test_orders_answer_uses_tool_summary(monkeypatch: pytest.MonkeyPatch) -> None:
    def fake_orders(self):
        return ToolResult("my_orders", True, "你有 1 笔待支付订单。\n· 科技园停车场 A-001：¥6.00，待支付")

    monkeypatch.setattr(BackendTools, "my_orders", fake_orders)
    result = engine.chat("我的订单", token="fake-token")

    assert "待支付订单" in result.answer
    assert "¥6.00" in result.answer


def test_forecast_answer_mentions_peak(monkeypatch: pytest.MonkeyPatch) -> None:
    def fake_forecast(self, hours=6):
        return ToolResult("forecast", True, "科技园停车场未来 6 小时：最紧张 15:00 约 87%（空闲 16 位）")

    monkeypatch.setattr(BackendTools, "forecast", fake_forecast)
    result = engine.chat("今天车位会满吗", token="fake-token")

    assert "87%" in result.answer
    assert result.intent == "forecast"


def test_knowledge_question_returns_citation() -> None:
    result = engine.chat("停车怎么收费", token=None)

    assert result.intent == "knowledge"
    assert "元" in result.answer
    assert result.sources
    assert result.sources[0]["title"] == "收费标准"


def test_unknown_question_returns_guidance() -> None:
    result = engine.chat("火星上能停车吗", token=None)

    assert result.intent in {"unknown", "knowledge"}
    assert result.suggestions


def test_stream_chunks_rebuild_original_text() -> None:
    text = "你好，我是智泊停车助手。可以帮你查空闲车位、预约、订单。"
    assert "".join(engine.stream_chunks(text, size=5)) == text
