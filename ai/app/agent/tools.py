"""Agent 工具集：真实调用业务后端 + 本地预测模型。"""

from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import datetime
from typing import Any

import httpx

from app.prediction import predictor


def backend_base() -> str:
    return os.environ.get("AI_BACKEND_BASE", "http://localhost:8080")


@dataclass
class ToolResult:
    name: str
    ok: bool
    summary: str
    data: Any = None


class ToolError(Exception):
    pass


class BackendTools:
    """所有请求透传用户 Token，权限仍由业务后端判定。"""

    def __init__(self, token: str | None) -> None:
        self._token = token

    def _get(self, path: str, params: dict | None = None) -> dict:
        if not self._token:
            raise ToolError("请先登录后再查询个人数据")
        headers = {"Authorization": f"Bearer {self._token}"}
        try:
            response = httpx.get(f"{backend_base()}{path}", params=params, headers=headers, timeout=5.0)
        except httpx.HTTPError as exc:
            raise ToolError("业务服务暂时不可用，请稍后再试") from exc
        if response.status_code == 401:
            raise ToolError("登录状态已过期，请重新登录")
        if response.status_code >= 400:
            raise ToolError(f"查询失败（HTTP {response.status_code}）")
        body = response.json()
        return body.get("data")

    # ---------- 工具 ----------
    def list_lots(self, keyword: str | None = None) -> ToolResult:
        data = self._get("/api/lots", {"page": 1, "size": 10, "keyword": keyword})
        records = (data or {}).get("records", [])
        if not records:
            return ToolResult("list_lots", True, "暂时没有在营停车场", [])

        lines = []
        for lot in records[:5]:
            lines.append(
                f"· {lot['name']}（{lot.get('area', '-')}）：空闲 {lot.get('freeCount', 0)}/{lot.get('capacity', 0)} 位"
                f"，{self._price_text(lot.get('priceFromCents'))}"
            )
        return ToolResult("list_lots", True, "\n".join(lines), records)

    def my_reservations(self) -> ToolResult:
        data = self._get("/api/reservations") or []
        active = [item for item in data if item.get("status") == 0]
        if not active:
            return ToolResult("my_reservations", True, "你当前没有待使用的预约。", data)
        lines = []
        for item in active[:3]:
            start = str(item.get("startTime", "")).replace("T", " ")[:16]
            lines.append(f"· {item.get('lotName', '停车场')} {item.get('spaceNo', '')}，{start} 开始")
        return ToolResult("my_reservations", True, "\n".join(lines), active)

    def my_orders(self) -> ToolResult:
        data = self._get("/api/orders", {"page": 1, "size": 5}) or {}
        records = data.get("records", [])
        if not records:
            return ToolResult("my_orders", True, "你还没有停车订单。", [])
        unpaid = [item for item in records if item.get("status") == 0]
        lines = []
        for item in records[:3]:
            amount = item.get("amountCents", 0) / 100
            lines.append(
                f"· {item.get('lotName', '停车场')} {item.get('spaceNo', '')}：¥{amount:.2f}，{item.get('statusText')}"
            )
        prefix = f"你有 {len(unpaid)} 笔待支付订单。\n" if unpaid else ""
        return ToolResult("my_orders", True, prefix + "\n".join(lines), records)

    def current_parking(self) -> ToolResult:
        data = self._get("/api/parking/current")
        if not data:
            return ToolResult("current_parking", True, "你当前没有进行中的停车。", None)
        entry = str(data.get("entryTime", "")).replace("T", " ")[11:16]
        estimated = data.get("estimatedCents")
        fee = "待计费" if estimated is None else f"¥{estimated / 100:.2f}"
        summary = (
            f"你正在 {data.get('lotName', '停车场')} {data.get('spaceNo', '')} 停车，"
            f"{entry} 入场，已停 {data.get('elapsedMinutes', 0)} 分钟，预估费用 {fee}。"
        )
        return ToolResult("current_parking", True, summary, data)

    def forecast(self, hours: int = 6) -> ToolResult:
        """按场景模型给未来占用率估算（场景数据与业务库解耦）。"""
        try:
            result = predictor.predict(1, datetime.now(), hours=hours)
        except Exception as exc:  # 预测失败不影响其它回答
            raise ToolError(f"预测服务不可用：{exc}") from exc

        points = result["points"]
        peak = max(points, key=lambda point: point["predictedRate"])
        low = min(points, key=lambda point: point["predictedRate"])
        summary = (
            f"{result['lotName']}未来 {hours} 小时："
            f"最紧张 {peak['time'].strftime('%H:%M')} 约 {peak['predictedRate'] * 100:.0f}%"
            f"（空闲 {peak['freeSpaces']} 位），"
            f"最宽松 {low['time'].strftime('%H:%M')} 约 {low['predictedRate'] * 100:.0f}%"
            f"（空闲 {low['freeSpaces']} 位）。"
        )
        return ToolResult("forecast", True, summary, points)

    @staticmethod
    def _price_text(cents: Any) -> str:
        if cents is None:
            return "价格待定"
        return f"首小时 ¥{cents / 100:.0f} 起"
