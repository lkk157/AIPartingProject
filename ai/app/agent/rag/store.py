"""Chroma 持久化知识库索引：文档注册表 + 分块向量集合。"""

from __future__ import annotations

import io
import logging
import os
import json
import os
import uuid
from datetime import datetime
from pathlib import Path

import chromadb
from chromadb.config import Settings

from app.agent.rag.chunker import chunk_text
from app.agent.rag.embeddings import EmbeddingProvider, create_provider
from app.agent.rag.retriever import hybrid_score, mmr_select

os.environ.setdefault("ANONYMIZED_TELEMETRY", "False")
logging.getLogger("chromadb.telemetry").setLevel(logging.CRITICAL)

SEED_DIR = Path(__file__).resolve().parents[3] / "knowledge"
MAX_FILE_BYTES = 2 * 1024 * 1024


def rag_dir() -> Path:
    default = Path(__file__).resolve().parents[3] / "data" / "rag"
    return Path(os.environ.get("AI_RAG_DIR", default))


class RagIndex:
    def __init__(self, directory: Path | None = None, provider: EmbeddingProvider | None = None,
                 chunk_size: int = 500, overlap: int = 80) -> None:
        self._dir = directory or rag_dir()
        self._dir.mkdir(parents=True, exist_ok=True)
        self._provider = provider or create_provider()
        self._chunk_size = chunk_size
        self._overlap = overlap
        self._client = chromadb.PersistentClient(
            path=str(self._dir / "chroma"),
            settings=Settings(anonymized_telemetry=False, allow_reset=True),
        )
        self._collection_name = f"knowledge_{self._provider.key}"
        self._collection = None
        self._rebuild()

    # ---------- 对外接口 ----------
    def list_documents(self) -> list[dict]:
        docs = self._documents()
        counts = self._chunk_counts()
        return [
            {
                "id": doc["id"],
                "title": doc["title"],
                "source": doc["source"],
                "chars": len(doc["text"]),
                "chunks": counts.get(doc["id"], 0),
                "createdAt": doc["createdAt"],
            }
            for doc in docs
        ]

    def add_document(self, title: str, content: str, source: str = "custom") -> dict:
        doc = {
            "id": f"doc-{uuid.uuid4().hex[:12]}",
            "title": title.strip(),
            "text": content.strip(),
            "source": source,
            "createdAt": datetime.now().isoformat(timespec="seconds"),
        }
        docs = self._load_custom()
        docs.append(doc)
        self._save_custom(docs)
        self._index_document(doc)
        return doc

    def add_file(self, filename: str, data: bytes) -> dict:
        if len(data) > MAX_FILE_BYTES:
            raise ValueError("文件超过 2MB 限制")
        suffix = Path(filename).suffix.lower()
        if suffix in {".md", ".markdown", ".txt"}:
            text = data.decode("utf-8", errors="ignore")
        elif suffix == ".pdf":
            text = self._read_pdf(data)
        else:
            raise ValueError("仅支持 .md / .txt / .pdf 文件")
        if len(text.strip()) < 10:
            raise ValueError("文件内容为空或无法解析")

        title = Path(filename).stem
        for line in text.splitlines():
            if line.startswith("# "):
                title = line[2:].strip()
                break
        return self.add_document(title, text, source="upload")

    def delete_document(self, doc_id: str) -> bool:
        docs = self._load_custom()
        remaining = [doc for doc in docs if doc["id"] != doc_id]
        if len(remaining) == len(docs):
            return False
        self._save_custom(remaining)
        self._collection.delete(where={"docId": doc_id})
        return True

    def get_document(self, doc_id: str) -> dict | None:
        return next((doc for doc in self._documents() if doc["id"] == doc_id), None)

    def search(self, query: str, k: int = 4, pool: int = 12, use_mmr: bool = True) -> list[dict]:
        if not query.strip() or self._collection.count() == 0:
            return []

        vector = self._provider.encode([query])[0]
        result = self._collection.query(
            query_embeddings=[vector],
            n_results=min(max(pool, k), self._collection.count()),
            include=["metadatas", "documents", "distances", "embeddings"],
        )

        candidates: list[dict] = []
        for metadata, text, distance, embedding in zip(
            result["metadatas"][0], result["documents"][0], result["distances"][0], result["embeddings"][0]
        ):
            vector_score = 1 - float(distance)
            score = hybrid_score(query, metadata["title"], metadata["docKey"], vector_score)
            candidates.append(
                {
                    "id": metadata["docId"],
                    "title": metadata["title"],
                    "source": metadata.get("source", "seed"),
                    "docKey": metadata["docKey"],
                    "chunkIndex": metadata.get("chunkIndex", 0),
                    "text": text,
                    "vectorScore": round(vector_score, 4),
                    "score": round(score, 4),
                    "embedding": embedding,
                }
            )

        candidates.sort(key=lambda item: item["score"], reverse=True)
        if use_mmr and len(candidates) > 1:
            selected = mmr_select(candidates, k)
        else:
            selected = candidates[:k]

        return [
            {key: item[key] for key in ("id", "title", "source", "chunkIndex", "text", "score", "vectorScore")}
            for item in selected
        ]

    def stats(self) -> dict:
        return {
            "provider": self._provider.key,
            "dim": self._provider.dim,
            "documents": len(self._documents()),
            "chunks": self._collection.count(),
            "chunkSize": self._chunk_size,
            "overlap": self._overlap,
        }

    def reset(self) -> None:
        """清空自定义文档并重建索引（种子文档保留）。"""
        self._save_custom([])
        self._rebuild()

    # ---------- 内部实现 ----------
    def _documents(self) -> list[dict]:
        return self._load_seeds() + self._load_custom()

    def _load_seeds(self) -> list[dict]:
        docs: list[dict] = []
        if not SEED_DIR.exists():
            return docs
        for path in sorted(SEED_DIR.glob("*.md")):
            text = path.read_text(encoding="utf-8").strip()
            docs.append(
                {
                    "id": f"seed-{path.stem}",
                    "title": self._title_of(text, path.stem),
                    "text": text,
                    "source": "seed",
                    "createdAt": datetime.fromtimestamp(path.stat().st_mtime).isoformat(timespec="seconds"),
                }
            )
        return docs

    def _custom_path(self) -> Path:
        return self._dir / "registry.json"

    def _load_custom(self) -> list[dict]:
        path = self._custom_path()
        if not path.exists():
            return []
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            return [doc for doc in data.get("docs", []) if doc.get("id") and doc.get("text")]
        except (json.JSONDecodeError, OSError):
            return []

    def _save_custom(self, docs: list[dict]) -> None:
        path = self._custom_path()
        path.write_text(json.dumps({"docs": docs}, ensure_ascii=False, indent=2), encoding="utf-8")

    def _chunk_counts(self) -> dict[str, int]:
        counts: dict[str, int] = {}
        if self._collection.count() == 0:
            return counts
        data = self._collection.get(include=["metadatas"])
        for metadata in data["metadatas"]:
            counts[metadata["docId"]] = counts.get(metadata["docId"], 0) + 1
        return counts

    def _rebuild(self) -> None:
        try:
            self._client.delete_collection(self._collection_name)
        except Exception:
            pass
        self._collection = self._client.create_collection(
            name=self._collection_name,
            metadata={"hnsw:space": "cosine", "provider": self._provider.key},
        )
        for doc in self._documents():
            self._index_document(doc, rebuild=True)
        self._collection = self._client.get_collection(self._collection_name)

    def _index_document(self, doc: dict, rebuild: bool = False) -> None:
        chunks = chunk_text(doc["text"], self._chunk_size, self._overlap)
        if not chunks:
            return
        embeddings = self._provider.encode(chunks)
        doc_key = doc["id"].replace("seed-", "").replace("doc-", "")
        ids = [f"{doc['id']}-{index}" for index in range(len(chunks))]
        metadatas = [
            {
                "docId": doc["id"],
                "title": doc["title"],
                "docKey": doc_key,
                "source": doc["source"],
                "chunkIndex": index,
            }
            for index in range(len(chunks))
        ]
        self._collection.upsert(ids=ids, documents=chunks, metadatas=metadatas, embeddings=embeddings)

    @staticmethod
    def _title_of(text: str, fallback: str) -> str:
        for line in text.splitlines():
            if line.startswith("# "):
                return line[2:].strip()
        return fallback

    @staticmethod
    def _read_pdf(data: bytes) -> str:
        try:
            from pypdf import PdfReader
        except ImportError as exc:  # pragma: no cover
            raise ValueError("未安装 pypdf，无法解析 PDF") from exc
        reader = PdfReader(io.BytesIO(data))
        return "\n".join((page.extract_text() or "") for page in reader.pages)
