"""Agent 与知识库接口模型。"""

from __future__ import annotations

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    role: str = Field(..., pattern="^(user|assistant)$")
    content: str


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=500)
    history: list[ChatMessage] = Field(default_factory=list, max_length=20)


class ToolCallOut(BaseModel):
    name: str
    ok: bool
    summary: str


class SourceOut(BaseModel):
    title: str
    score: float
    snippet: str


class ChatResponse(BaseModel):
    answer: str
    intent: str
    toolCalls: list[ToolCallOut] = []
    sources: list[SourceOut] = []
    suggestions: list[str] = []


class KnowledgeCreate(BaseModel):
    title: str = Field(..., min_length=2, max_length=80)
    content: str = Field(..., min_length=10, max_length=20000)


class KnowledgeDoc(BaseModel):
    id: str
    title: str
    source: str
    chars: int
    createdAt: str


class KnowledgeHit(BaseModel):
    id: str
    title: str
    source: str
    score: float
    snippet: str


class KnowledgeSearchRequest(BaseModel):
    query: str = Field(..., min_length=1, max_length=200)
    topK: int = Field(default=3, ge=1, le=10)
