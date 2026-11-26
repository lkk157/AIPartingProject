"""知识库对外门面：底层为 Chroma 分块索引（app.agent.rag.store.RagIndex）。"""

from __future__ import annotations

import base64

from app.agent.rag.store import RagIndex

_index: RagIndex | None = None
_service = None


def get_index() -> RagIndex:
    global _index
    if _index is None:
        _index = RagIndex()
    return _index


def reset_store() -> None:
    """测试/重建用：清空索引与门面单例（下次访问重新加载）。"""
    global _index, _service
    _index = None
    _service = None


class KnowledgeService:
    """保持 P6 的检索外观（带 snippet），供对话引擎直接使用。"""

    def list_documents(self) -> list[dict]:
        return get_index().list_documents()

    def add_document(self, title: str, content: str) -> dict:
        return get_index().add_document(title, content)

    def add_file(self, filename: str, content_base64: str) -> dict:
        try:
            data = base64.b64decode(content_base64, validate=True)
        except Exception as exc:
            raise ValueError("文件内容不是合法的 base64") from exc
        return get_index().add_file(filename, data)

    def delete_document(self, doc_id: str) -> bool:
        return get_index().delete_document(doc_id)

    def search(self, query: str, top_k: int = 3, use_mmr: bool = True) -> list[dict]:
        hits = get_index().search(query, k=top_k, use_mmr=use_mmr)
        return [
            {
                "id": hit["id"],
                "title": hit["title"],
                "source": hit["source"],
                "score": hit["score"],
                "chunkIndex": hit["chunkIndex"],
                "snippet": _clean_snippet(hit["text"]),
            }
            for hit in hits
        ]

    def stats(self) -> dict:
        return get_index().stats()


def _clean_snippet(text: str, limit: int = 180) -> str:
    """去掉 Markdown 标题、列表符转圆点，供移动端气泡直接展示。"""
    lines: list[str] = []
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if stripped.startswith("- "):
            stripped = "• " + stripped[2:]
        lines.append(stripped)
    body = "\n".join(lines)
    return body[:limit] + ("…" if len(body) > limit else "")


def get_store() -> KnowledgeService:
    global _service
    if _service is None:
        _service = KnowledgeService()
    return _service
