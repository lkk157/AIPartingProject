"""向量化提供方：默认离线哈希向量；配置了 Ollama bge-m3 时优先使用。"""

from __future__ import annotations

import os
from typing import Protocol

import httpx
import numpy as np
from sklearn.feature_extraction.text import HashingVectorizer


class EmbeddingProvider(Protocol):
    key: str
    dim: int

    def encode(self, texts: list[str]) -> list[list[float]]: ...


class HashingEmbedding:
    """字符 2-4 gram 哈希向量，无需模型下载，固定 2048 维，余弦空间稳定。"""

    key = "hash"
    dim = 2048

    def __init__(self) -> None:
        self._vectorizer = HashingVectorizer(
            analyzer="char_wb",
            ngram_range=(2, 4),
            n_features=self.dim,
            alternate_sign=False,
            norm="l2",
        )

    def encode(self, texts: list[str]) -> list[list[float]]:
        matrix = self._vectorizer.transform(texts).toarray()
        return [row.tolist() for row in matrix]


class OllamaEmbedding:
    """Ollama 的 bge-m3 向量（需本地 ollama pull bge-m3）。"""

    key = "bge-m3"
    dim = 1024

    def __init__(self, base: str, model: str = "bge-m3") -> None:
        self._base = base.rstrip("/")
        self._model = model
        self.dim = self._probe_dim()

    def _probe_dim(self) -> int:
        try:
            response = httpx.post(
                f"{self._base}/api/embeddings",
                json={"model": self._model, "prompt": "维度探测"},
                timeout=8.0,
            )
            if response.status_code == 200:
                return len(response.json().get("embedding", [])) or 1024
        except httpx.HTTPError:
            pass
        return 1024

    def encode(self, texts: list[str]) -> list[list[float]]:
        vectors: list[list[float]] = []
        for text in texts:
            try:
                response = httpx.post(
                    f"{self._base}/api/embeddings",
                    json={"model": self._model, "prompt": text},
                    timeout=15.0,
                )
                response.raise_for_status()
                vectors.append(response.json()["embedding"])
            except (httpx.HTTPError, KeyError):
                # 单条失败退回哈希向量，保证服务可用（维度不足补零）
                fallback = HashingEmbedding().encode([text])[0]
                vectors.append((fallback + [0.0] * self.dim)[: self.dim])
        return vectors


def create_provider() -> EmbeddingProvider:
    """AI_EMBEDDING=bge-m3 且 Ollama 可用时用 bge-m3，否则离线哈希向量。"""
    if os.environ.get("AI_EMBEDDING", "hash").lower() == "bge-m3":
        base = os.environ.get("AI_OLLAMA_BASE", "http://localhost:11434")
        try:
            return OllamaEmbedding(base)
        except Exception:
            return HashingEmbedding()
    return HashingEmbedding()


def cosine(a: list[float], b: list[float]) -> float:
    va, vb = np.asarray(a, dtype=float), np.asarray(b, dtype=float)
    denominator = float(np.linalg.norm(va) * np.linalg.norm(vb))
    if denominator == 0:
        return 0.0
    return float(np.dot(va, vb) / denominator)
