"""混合重排 + MMR：向量召回 → 领域词/标题加权 → 最大边际相关选块。"""

from __future__ import annotations

import numpy as np

# 领域词（与知识库文档主题对应），用于修正字符向量的高频词偏置
DOMAIN_KEYWORDS: dict[str, list[str]] = {
    "billing": ["收费", "费用", "价格", "多少钱", "计费", "封顶", "免费", "费率", "贵"],
    "reservation": ["预约", "取消", "改约", "爽约", "入场时间", "名额", "订位"],
    "parking_faq": ["流程", "支付", "发票", "换车位", "车牌", "忘记", "怎么办", "结算"],
    "prediction": ["预测", "占用率", "高峰", "趋势", "会不会满", "会满", "满吗", "空位多"],
    "account": ["登录", "注册", "密码", "权限", "隐私", "账号", "忘记密码"],
}

W_VECTOR = 0.6
W_KEYWORD = 0.12
W_TITLE = 0.15


def hybrid_score(query: str, title: str, doc_key: str, vector_score: float) -> float:
    matched = sum(1 for word in DOMAIN_KEYWORDS.get(doc_key, []) if word in query)
    query_chars = set(query)
    title_overlap = len(query_chars & set(title)) / max(len(query_chars), 1)
    return vector_score * W_VECTOR + min(matched, 2) * W_KEYWORD + title_overlap * W_TITLE


def mmr_select(items: list[dict], k: int, lambda_: float = 0.7) -> list[dict]:
    """items 需含 score 与 embedding；在候选池内做 MMR 去冗余。"""
    pool = list(items)
    selected: list[dict] = []
    while pool and len(selected) < k:
        best_index, best_value = 0, float("-inf")
        for index, candidate in enumerate(pool):
            redundancy = 0.0
            if selected:
                redundancy = max(float(np.dot(candidate["embedding"], chosen["embedding"])) for chosen in selected)
            value = lambda_ * candidate["score"] - (1 - lambda_) * redundancy
            if value > best_value:
                best_index, best_value = index, value
        selected.append(pool.pop(best_index))
    return selected
