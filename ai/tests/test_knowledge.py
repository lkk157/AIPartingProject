"""知识库检索测试。"""

from __future__ import annotations

from pathlib import Path

import pytest

from app.agent.knowledge import KnowledgeStore, reset_store


@pytest.fixture()
def store(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> KnowledgeStore:
    monkeypatch.setenv("AI_KNOWLEDGE_PATH", str(tmp_path / "knowledge.json"))
    reset_store()
    return KnowledgeStore()


def test_seed_documents_loaded(store: KnowledgeStore) -> None:
    titles = [doc["title"] for doc in store.list_documents()]
    assert len(titles) >= 5
    assert "收费标准" in titles
    assert "预约规则" in titles


def test_search_billing_question_ranks_billing_doc(store: KnowledgeStore) -> None:
    hits = store.search("停车怎么收费")
    assert hits
    assert hits[0]["title"] == "收费标准"
    assert "元" in hits[0]["snippet"]


def test_search_cancel_question_hits_reservation_rules(store: KnowledgeStore) -> None:
    hits = store.search("预约可以取消吗")
    assert hits
    assert any(hit["title"] == "预约规则" for hit in hits[:2])


def test_custom_document_add_search_and_delete(store: KnowledgeStore) -> None:
    doc = store.add_document("夜间包月套餐", "夜间 20:00 至次日 08:00 包月 300 元，仅限住宅类停车场。")
    hits = store.search("夜间包月多少钱")
    assert hits and hits[0]["id"] == doc["id"]

    assert store.delete_document(doc["id"]) is True
    assert store.search("夜间包月多少钱") == []


def test_seed_document_cannot_be_deleted(store: KnowledgeStore) -> None:
    seed = next(doc for doc in store.list_documents() if doc["source"] == "seed")
    assert store.delete_document(seed["id"]) is False


def test_empty_query_returns_nothing(store: KnowledgeStore) -> None:
    assert store.search("   ") == []
