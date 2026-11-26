"""知识库门面测试（走 Chroma 索引）。"""

from __future__ import annotations

from pathlib import Path

import pytest

from app.agent.knowledge import get_store, reset_store


@pytest.fixture()
def store(tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
    monkeypatch.setenv("AI_RAG_DIR", str(tmp_path / "rag"))
    reset_store()
    return get_store()


def test_seed_documents_loaded(store) -> None:
    titles = [doc["title"] for doc in store.list_documents()]
    assert len(titles) >= 5
    assert "收费标准" in titles
    assert "预约规则" in titles


def test_search_billing_question_ranks_billing_doc(store) -> None:
    hits = store.search("停车怎么收费")
    assert hits
    assert hits[0]["title"] == "收费标准"
    assert "元" in hits[0]["snippet"]


def test_search_cancel_question_hits_reservation_rules(store) -> None:
    hits = store.search("预约可以取消吗")
    assert hits
    assert any(hit["title"] == "预约规则" for hit in hits[:2])


def test_custom_document_add_search_and_delete(store) -> None:
    doc = store.add_document("夜间包月套餐", "夜间 20:00 至次日 08:00 包月 300 元，仅限住宅类停车场。")
    hits = store.search("夜间包月套餐价格")
    assert hits
    assert doc["id"] in [hit["id"] for hit in hits]

    assert store.delete_document(doc["id"]) is True
    remaining = store.search("夜间包月套餐价格")
    assert all(hit["id"] != doc["id"] for hit in remaining)


def test_seed_document_cannot_be_deleted(store) -> None:
    seed = next(doc for doc in store.list_documents() if doc["source"] == "seed")
    assert store.delete_document(seed["id"]) is False


def test_empty_query_returns_nothing(store) -> None:
    assert store.search("   ") == []


def test_stats_exposes_chunk_configuration(store) -> None:
    stats = store.stats()
    assert stats["chunks"] >= stats["documents"]
    assert stats["provider"] in {"hash", "bge-m3"}
