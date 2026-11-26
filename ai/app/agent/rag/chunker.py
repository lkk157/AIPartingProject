"""中文友好的文本分块：句末标点优先，块大小 500 字符、重叠 80 字符。"""

from __future__ import annotations

import re

DEFAULT_SIZE = 500
DEFAULT_OVERLAP = 80
_SENTENCE = re.compile(r"[^。！？；!?;\n]+[。！？；!?;\n]?")


def split_sentences(text: str) -> list[str]:
    return [part for part in _SENTENCE.findall(text) if part.strip()]


def chunk_text(text: str, size: int = DEFAULT_SIZE, overlap: int = DEFAULT_OVERLAP) -> list[str]:
    """按句子拼装，保证每块 ≤ size（单句超长时硬切），相邻块重叠约 overlap 字符。"""
    if not text.strip():
        return []
    if overlap >= size:
        raise ValueError("overlap 必须小于 size")

    # 逐句处理并保留换行：块内按行展示，便于前端渲染 Markdown 列表
    units: list[str] = []
    for line in text.splitlines():
        for sentence in split_sentences(line):
            if sentence.strip():
                units.append(sentence.strip())

    chunks: list[str] = []
    buffer = ""
    for unit in units:
        # 单句本身超长：硬切成 size 长度片段
        while len(unit) > size:
            head, unit = unit[:size], unit[size:]
            if buffer:
                chunks.append(buffer.strip())
                buffer = buffer[-overlap:] if overlap else ""
            chunks.append((buffer + "\n" + head).strip() if buffer else head.strip())
            buffer = buffer[-overlap:] if overlap else ""

        candidate = f"{buffer}\n{unit}" if buffer else unit
        if len(candidate) > size and buffer:
            chunks.append(buffer.strip())
            buffer = (buffer[-overlap:] if overlap else "") + "\n" + unit
        else:
            buffer = candidate

    if buffer.strip():
        chunks.append(buffer.strip())

    return [chunk for chunk in chunks if chunk]
