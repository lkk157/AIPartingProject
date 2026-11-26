"""Agent 对话与知识库管理接口。"""

from __future__ import annotations

import json

from fastapi import APIRouter, Header, HTTPException
from fastapi.responses import StreamingResponse

from app.agent import engine, llm, ops
from app.agent.knowledge import get_store
from app.agent.schema import (
    ChatRequest,
    KnowledgeCreate,
    KnowledgeSearchRequest,
    KnowledgeUpload,
)

router = APIRouter(tags=["agent"])


def _token(authorization: str | None) -> str | None:
    if not authorization:
        return None
    value = authorization.strip()
    if value.lower().startswith("bearer "):
        return value[7:].strip()
    return value


def _ok(data) -> dict:
    return {"code": 0, "message": "ok", "data": data}


@router.post("/agent/chat")
def chat(payload: ChatRequest, authorization: str | None = Header(default=None)) -> dict:
    result = engine.chat(payload.message, _token(authorization))
    return _ok(
        {
            "answer": result.answer,
            "intent": result.intent,
            "toolCalls": result.tool_calls,
            "sources": result.sources,
            "suggestions": result.suggestions,
        }
    )


@router.post("/agent/chat/stream")
def chat_stream(payload: ChatRequest, authorization: str | None = Header(default=None)) -> StreamingResponse:
    result = engine.chat(payload.message, _token(authorization))

    def events():
        meta = {
            "intent": result.intent,
            "toolCalls": result.tool_calls,
            "sources": result.sources,
            "suggestions": result.suggestions,
        }
        yield f"event: meta\ndata: {json.dumps(meta, ensure_ascii=False)}\n\n"
        for chunk in engine.stream_chunks(result.answer):
            yield f"event: delta\ndata: {json.dumps({'text': chunk}, ensure_ascii=False)}\n\n"
        yield f"event: done\ndata: {json.dumps({'answer': result.answer}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        events(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.get("/agent/llm/status")
def llm_status() -> dict:
    return _ok(llm.status())


@router.get("/knowledge/documents")
def list_documents() -> dict:
    return _ok(get_store().list_documents())


@router.post("/knowledge/documents")
def create_document(payload: KnowledgeCreate) -> dict:
    doc = get_store().add_document(payload.title, payload.content)
    return _ok({k: doc[k] for k in ("id", "title", "source", "createdAt")})


@router.post("/knowledge/upload")
def upload_document(payload: KnowledgeUpload) -> dict:
    try:
        doc = get_store().add_file(payload.filename, payload.contentBase64)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    return _ok({key: doc[key] for key in ("id", "title", "source", "createdAt")})


@router.delete("/knowledge/documents/{doc_id}")
def delete_document(doc_id: str) -> dict:
    if not get_store().delete_document(doc_id):
        raise HTTPException(status_code=404, detail="文档不存在或为内置文档，无法删除")
    return _ok({"deleted": doc_id})


@router.post("/knowledge/search")
def search_documents(payload: KnowledgeSearchRequest) -> dict:
    return _ok(get_store().search(payload.query, top_k=payload.topK, use_mmr=payload.useMmr))


@router.get("/knowledge/stats")
def knowledge_stats() -> dict:
    return _ok(get_store().stats())


@router.get("/agent/ops/overview")
def ops_overview(authorization: str | None = Header(default=None)) -> dict:
    token = _token(authorization)
    if not token:
        raise HTTPException(status_code=401, detail="需要管理员登录")
    try:
        return _ok(ops.overview(token))
    except ops.OpsError as exc:
        message = str(exc)
        raise HTTPException(status_code=403 if "权限" in message else 502, detail=message) from exc
