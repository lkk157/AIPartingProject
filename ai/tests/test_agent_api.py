"""Agent / 知识库接口测试。"""

from __future__ import annotations

from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.agent.knowledge import reset_store
from app.main import app


@pytest.fixture(autouse=True)
def isolated_knowledge(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AI_RAG_DIR", str(tmp_path / "rag"))
    reset_store()


@pytest.fixture(scope="module")
def client() -> TestClient:
    return TestClient(app)


def test_greeting_chat(client: TestClient) -> None:
    response = client.post("/agent/chat", json={"message": "你好"})
    assert response.status_code == 200
    data = response.json()["data"]
    assert data["intent"] == "greeting"
    assert "智泊停车" in data["answer"]
    assert data["suggestions"]


def test_empty_message_rejected(client: TestClient) -> None:
    assert client.post("/agent/chat", json={"message": ""}).status_code == 422


def test_knowledge_answer_has_sources(client: TestClient) -> None:
    response = client.post("/agent/chat", json={"message": "停车怎么收费"})
    assert response.status_code == 200
    data = response.json()["data"]
    assert data["intent"] == "knowledge"
    assert data["sources"]
    assert "元" in data["answer"]


def test_personal_query_without_token_prompts_login(client: TestClient) -> None:
    response = client.post("/agent/chat", json={"message": "我的订单"})
    assert response.status_code == 200
    assert "登录" in response.json()["data"]["answer"]


def test_stream_endpoint_emits_events(client: TestClient) -> None:
    with client.stream("POST", "/agent/chat/stream", json={"message": "你好"}) as response:
        assert response.status_code == 200
        assert "text/event-stream" in response.headers["content-type"]
        body = "".join(response.iter_text())

    assert "event: meta" in body
    assert "event: delta" in body
    assert "event: done" in body


def test_llm_status(client: TestClient) -> None:
    response = client.get("/agent/llm/status")
    assert response.status_code == 200
    data = response.json()["data"]
    assert data["available"] in {True, False}
    assert data["model"]


def test_knowledge_crud(client: TestClient) -> None:
    docs = client.get("/knowledge/documents").json()["data"]
    assert any(doc["title"] == "收费标准" for doc in docs)
    assert all("chunks" in doc for doc in docs)

    created = client.post(
        "/knowledge/documents",
        json={"title": "夜间包月", "content": "夜间 20:00 至次日 08:00 包月 300 元，仅限住宅类停车场。"},
    )
    assert created.status_code == 200
    doc_id = created.json()["data"]["id"]

    hits = client.post("/knowledge/search", json={"query": "夜间包月多少钱", "topK": 3}).json()["data"]
    assert hits and doc_id in [hit["id"] for hit in hits]

    assert client.delete(f"/knowledge/documents/{doc_id}").status_code == 200

    seed_id = next(doc["id"] for doc in docs if doc["source"] == "seed")
    assert client.delete(f"/knowledge/documents/{seed_id}").status_code == 404


def test_upload_markdown_document(client: TestClient) -> None:
    import base64

    content = "# 夜间包月\n夜间 20:00 至次日 08:00 包月 300 元，仅限住宅类停车场。"
    payload = {
        "filename": "night-pass.md",
        "contentBase64": base64.b64encode(content.encode("utf-8")).decode(),
    }
    created = client.post("/knowledge/upload", json=payload)
    assert created.status_code == 200
    doc_id = created.json()["data"]["id"]

    hits = client.post("/knowledge/search", json={"query": "夜间包月多少钱", "topK": 5}).json()["data"]
    assert doc_id in [hit["id"] for hit in hits]

    assert client.delete(f"/knowledge/documents/{doc_id}").status_code == 200


def test_upload_rejects_unsupported_file(client: TestClient) -> None:
    import base64

    payload = {
        "filename": "rules.docx",
        "contentBase64": base64.b64encode(b"dummy content for docx").decode(),
    }
    assert client.post("/knowledge/upload", json=payload).status_code == 400


def test_knowledge_stats(client: TestClient) -> None:
    data = client.get("/knowledge/stats").json()["data"]
    assert data["chunks"] >= data["documents"] >= 5
    assert data["chunkSize"] == 500
    assert data["overlap"] == 80


def test_ops_overview_requires_token(client: TestClient) -> None:
    assert client.get("/agent/ops/overview").status_code == 401
