"""Chroma 索引：入库、删除、检索、MMR 与统计。"""

from __future__ import annotations

from pathlib import Path

import pytest

from app.agent.rag.store import RagIndex

LONG_TEXT = "# 夜间停车管理办法\n" + "夜间时段为二十点至次日八点，仅限住宅类停车场使用。" * 60


@pytest.fixture()
def index(tmp_path: Path) -> RagIndex:
    return RagIndex(directory=tmp_path / "rag")


def test_seed_documents_are_indexed(index: RagIndex) -> None:
    docs = index.list_documents()
    titles = [doc["title"] for doc in docs]

    assert len(docs) >= 5
    assert "收费标准" in titles
    assert all(doc["chunks"] >= 1 for doc in docs)


def test_search_returns_chunk_with_source(index: RagIndex) -> None:
    hits = index.search("停车怎么收费", k=3)

    assert hits
    assert hits[0]["title"] == "收费标准"
    assert hits[0]["chunkIndex"] == 0
    assert hits[0]["score"] > 0


def test_add_document_increases_chunk_count(index: RagIndex) -> None:
    before = index.stats()["chunks"]
    doc = index.add_document("夜间停车管理办法", LONG_TEXT)

    after = index.stats()
    assert after["chunks"] > before
    stored = next(item for item in index.list_documents() if item["id"] == doc["id"])
    assert stored["chunks"] >= 3


def test_delete_document_removes_chunks(index: RagIndex) -> None:
    doc = index.add_document("夜间停车管理办法", LONG_TEXT)
    chunks_with_doc = index.stats()["chunks"]
    assert index.search("夜间停车管理办法", k=5)

    assert index.delete_document(doc["id"]) is True
    assert index.stats()["chunks"] < chunks_with_doc
    assert all(hit["id"] != doc["id"] for hit in index.search("夜间停车管理办法", k=5))


def test_seed_document_cannot_be_deleted(index: RagIndex) -> None:
    seed = next(doc for doc in index.list_documents() if doc["source"] == "seed")
    assert index.delete_document(seed["id"]) is False


def test_mmr_and_plain_search_both_respect_k(index: RagIndex) -> None:
    with_mmr = index.search("预约规则与取消", k=2, use_mmr=True)
    without_mmr = index.search("预约规则与取消", k=2, use_mmr=False)

    assert len(with_mmr) <= 2
    assert len(without_mmr) <= 2
    assert all(hit["title"] for hit in with_mmr)


def test_stats_reports_provider_and_chunk_config(index: RagIndex) -> None:
    stats = index.stats()
    assert stats["provider"] in {"hash", "bge-m3"}
    assert stats["dim"] > 0
    assert stats["chunkSize"] == 500
    assert stats["overlap"] == 80


def test_upload_rejects_unsupported_extension(index: RagIndex) -> None:
    with pytest.raises(ValueError):
        index.add_file("rules.docx", b"whatever")
