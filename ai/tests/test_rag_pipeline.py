"""分块与 MMR 检索的纯函数测试。"""

from __future__ import annotations

import pytest

from app.agent.rag.chunker import chunk_text
from app.agent.rag.retriever import hybrid_score, mmr_select


def test_chunks_respect_size_limit() -> None:
    text = "停车场收费规则说明。" * 120  # 1200 字符
    chunks = chunk_text(text, size=500, overlap=80)

    assert len(chunks) >= 3
    assert all(len(chunk) <= 520 for chunk in chunks)  # 允许一句话的边界溢出


def test_chunks_have_overlap_between_neighbours() -> None:
    text = "".join(f"第{i}条规则说明内容。" for i in range(80))
    chunks = chunk_text(text, size=200, overlap=40)

    assert len(chunks) >= 2
    tail = chunks[0][-40:]
    assert tail in chunks[1]


def test_long_single_sentence_is_hard_split() -> None:
    text = "啊" * 1200 + "。"
    chunks = chunk_text(text, size=500, overlap=80)

    assert len(chunks) >= 2
    assert all(len(chunk) <= 520 for chunk in chunks)


def test_blank_text_returns_empty() -> None:
    assert chunk_text("   \n  ") == []


def test_overlap_must_be_smaller_than_size() -> None:
    with pytest.raises(ValueError):
        chunk_text("内容" * 10, size=100, overlap=100)


def test_hybrid_score_boosts_domain_keyword() -> None:
    plain = hybrid_score("停车怎么收费", "停车流程与常见问题", "parking_faq", 0.15)
    billing = hybrid_score("停车怎么收费", "收费标准", "billing", 0.13)
    assert billing > plain


def test_mmr_selects_diverse_items() -> None:
    items = [
        {"score": 0.9, "embedding": [1.0, 0.0]},
        {"score": 0.88, "embedding": [0.99, 0.01]},
        {"score": 0.7, "embedding": [0.0, 1.0]},
    ]
    selected = mmr_select(items, k=2, lambda_=0.6)

    assert len(selected) == 2
    # 第二条应优先选与第一条不相似的，而不是分数接近的重复项
    assert selected[1]["embedding"] == [0.0, 1.0]
