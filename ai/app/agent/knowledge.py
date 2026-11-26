"""知识库：内置规则文档 + 管理员新增文档，TF-IDF 字符 n-gram 向量检索。

不依赖任何外部 embedding 服务（离线可用），如需 Chroma/pgvector 可替换 VectorStore 实现。
"""

from __future__ import annotations

import json
import os
import threading
import uuid
from datetime import datetime
from pathlib import Path

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

SEED_DIR = Path(__file__).resolve().parents[2] / "knowledge"

# 关键词加权：字符向量召回后做领域词重排，避免「停车流程」抢走「收费」类问题
SEED_KEYWORDS = {
    "billing": ["收费", "费用", "价格", "多少钱", "计费", "封顶", "免费", "费率"],
    "reservation": ["预约", "取消", "改约", "爽约", "入场时间", "名额"],
    "parking_faq": ["流程", "支付", "发票", "换车位", "车牌", "忘记", "怎么办"],
    "prediction": ["预测", "占用率", "高峰", "趋势", "会不会满"],
    "account": ["登录", "注册", "密码", "权限", "隐私", "账号"],
}


def _custom_path() -> Path:
    default = Path(__file__).resolve().parents[2] / "data" / "knowledge.json"
    return Path(os.environ.get("AI_KNOWLEDGE_PATH", default))


class KnowledgeStore:
    """文档存储 + 检索。文档少（<1000），每次变更重建 TF-IDF 索引即可。"""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._docs: list[dict] = []
        self._vectorizer: TfidfVectorizer | None = None
        self._matrix = None
        self.reload()

    # ---------- 读写 ----------
    def reload(self) -> None:
        docs = self._load_seeds() + self._load_custom()
        with self._lock:
            self._docs = docs
            self._rebuild()

    def list_documents(self) -> list[dict]:
        return [
            {
                "id": doc["id"],
                "title": doc["title"],
                "source": doc["source"],
                "chars": len(doc["content"]),
                "createdAt": doc["createdAt"],
            }
            for doc in self._docs
        ]

    def add_document(self, title: str, content: str) -> dict:
        doc = {
            "id": uuid.uuid4().hex[:12],
            "title": title.strip(),
            "content": content.strip(),
            "source": "custom",
            "createdAt": datetime.now().isoformat(timespec="seconds"),
        }
        with self._lock:
            self._docs.append(doc)
            self._persist_custom()
            self._rebuild()
        return doc

    def delete_document(self, doc_id: str) -> bool:
        with self._lock:
            before = len(self._docs)
            self._docs = [doc for doc in self._docs if not (doc["id"] == doc_id and doc["source"] == "custom")]
            if len(self._docs) == before:
                return False
            self._persist_custom()
            self._rebuild()
            return True

    # ---------- 检索 ----------
    def search(self, query: str, top_k: int = 3) -> list[dict]:
        if not query.strip() or not self._docs:
            return []
        with self._lock:
            if self._vectorizer is None or self._matrix is None:
                return []
            query_vec = self._vectorizer.transform([query])
            base_scores = cosine_similarity(query_vec, self._matrix)[0].astype(float)
            scores = base_scores.copy()
            # 领域关键词加权（简单重排，可解释、可测试）
            for index, doc in enumerate(self._docs):
                matched = sum(1 for word in doc.get("keywords", []) if word in query)
                if matched:
                    scores[index] = min(scores[index] + 0.08 * matched, 0.99)

        ranked = np.argsort(scores)[::-1][: top_k * 2]
        results: list[dict] = []
        for index in ranked:
            index = int(index)
            score = float(scores[index])
            # 关键词加权不能救回语义完全不相关的文档
            if base_scores[index] < 0.03 and score < 0.2:
                continue
            if score < 0.05:
                continue
            doc = self._docs[int(index)]
            results.append(
                {
                    "id": doc["id"],
                    "title": doc["title"],
                    "source": doc["source"],
                    "score": round(score, 4),
                    "snippet": self._snippet(doc["content"], query),
                }
            )
            if len(results) >= top_k:
                break
        return results

    def get_document(self, doc_id: str) -> dict | None:
        return next((doc for doc in self._docs if doc["id"] == doc_id), None)

    # ---------- 内部 ----------
    def _load_seeds(self) -> list[dict]:
        docs: list[dict] = []
        if not SEED_DIR.exists():
            return docs
        for path in sorted(SEED_DIR.glob("*.md")):
            text = path.read_text(encoding="utf-8").strip()
            title = self._title_of(text, path.stem)
            docs.append(
                {
                    "id": f"seed-{path.stem}",
                    "title": title,
                    "content": text,
                    "source": "seed",
                    "keywords": SEED_KEYWORDS.get(path.stem, []),
                    "createdAt": datetime.fromtimestamp(path.stat().st_mtime).isoformat(timespec="seconds"),
                }
            )
        return docs

    def _load_custom(self) -> list[dict]:
        path = _custom_path()
        if not path.exists():
            return []
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            return [doc for doc in data if isinstance(doc, dict) and doc.get("id")]
        except (json.JSONDecodeError, OSError):
            return []

    def _persist_custom(self) -> None:
        path = _custom_path()
        path.parent.mkdir(parents=True, exist_ok=True)
        custom = [doc for doc in self._docs if doc["source"] == "custom"]
        path.write_text(json.dumps(custom, ensure_ascii=False, indent=2), encoding="utf-8")

    def _rebuild(self) -> None:
        corpus = [f"{doc['title']}\n{doc['content']}" for doc in self._docs]
        if not corpus:
            self._vectorizer = None
            self._matrix = None
            return
        # 中文无需分词：字符 2-3 gram 在短文档检索上效果稳定
        self._vectorizer = TfidfVectorizer(analyzer="char_wb", ngram_range=(2, 3), min_df=1)
        self._matrix = self._vectorizer.fit_transform(corpus)

    @staticmethod
    def _title_of(text: str, fallback: str) -> str:
        for line in text.splitlines():
            if line.startswith("# "):
                return line[2:].strip()
        return fallback

    @staticmethod
    def _snippet(content: str, query: str, limit: int = 120) -> str:
        body = "\n".join(
            line.strip() for line in content.splitlines() if line.strip() and not line.startswith("#")
        )
        position = body.find(query[:4]) if query else -1
        if position <= 0:
            return body[:limit] + ("…" if len(body) > limit else "")
        start = max(position - 30, 0)
        return body[start:start + limit] + ("…" if start + limit < len(body) else "")


_store: KnowledgeStore | None = None


def get_store() -> KnowledgeStore:
    global _store
    if _store is None:
        _store = KnowledgeStore()
    return _store


def reset_store() -> None:
    """测试用：清空单例，便于切换知识库文件路径。"""
    global _store
    _store = None
