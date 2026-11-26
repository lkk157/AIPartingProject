"""Ollama 本地大模型接入（可选）。不可用时由规则引擎直接作答，保证离线可演示。"""

from __future__ import annotations

import os
import time

import httpx

_probe_cache: dict[str, float | bool] = {"at": 0.0, "ok": False}
_PROBE_TTL = 30.0


def ollama_base() -> str:
    return os.environ.get("AI_OLLAMA_BASE", "http://localhost:11434")


def model_name() -> str:
    return os.environ.get("AI_OLLAMA_MODEL", "qwen2.5:7b")


def available() -> bool:
    now = time.time()
    if now - float(_probe_cache["at"]) < _PROBE_TTL:
        return bool(_probe_cache["ok"])
    ok = False
    try:
        response = httpx.get(f"{ollama_base()}/api/tags", timeout=0.8)
        ok = response.status_code == 200
    except httpx.HTTPError:
        ok = False
    _probe_cache.update({"at": now, "ok": ok})
    return ok


def generate(prompt: str, system: str | None = None, timeout: float = 20.0) -> str | None:
    """调用 Ollama 生成；失败返回 None，由调用方回退到模板答案。"""
    if not available():
        return None
    payload: dict = {"model": model_name(), "prompt": prompt, "stream": False, "options": {"temperature": 0.2}}
    if system:
        payload["system"] = system
    try:
        response = httpx.post(f"{ollama_base()}/api/generate", json=payload, timeout=timeout)
        if response.status_code != 200:
            return None
        text = (response.json().get("response") or "").strip()
        return text or None
    except httpx.HTTPError:
        return None


def status() -> dict:
    return {"available": available(), "model": model_name(), "base": ollama_base()}
