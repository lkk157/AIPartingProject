"""AI 运营分析：预定义统计 + 预测高峰，不生成 SQL、不编造数据。"""

from __future__ import annotations

import os
from datetime import datetime

import httpx

from app.prediction import predictor


def backend_base() -> str:
    return os.environ.get("AI_BACKEND_BASE", "http://localhost:8080")


class OpsError(Exception):
    pass


def _get(path: str, token: str, params: dict | None = None) -> dict:
    try:
        response = httpx.get(
            f"{backend_base()}{path}",
            params=params,
            headers={"Authorization": f"Bearer {token}"},
            timeout=6.0,
        )
    except httpx.HTTPError as exc:
        raise OpsError("业务服务暂时不可用") from exc
    if response.status_code in (401, 403):
        raise OpsError("需要管理员权限才能查看运营数据")
    if response.status_code >= 400:
        raise OpsError(f"查询失败（HTTP {response.status_code}）")
    return response.json().get("data") or {}


def overview(token: str) -> dict:
    """返回结构化运营数据 + 一段可直接展示的结论文字。"""
    stats = _get("/api/admin/stats/overview", token)
    try:
        forecast = predictor.predict(1, datetime.now(), hours=12)
        points = forecast["points"]
        peak = max(points, key=lambda point: point["predictedRate"])
        forecast_summary = {
            "lotName": forecast["lotName"],
            "peakTime": peak["time"].strftime("%H:%M"),
            "peakRate": peak["predictedRate"],
            "peakFree": peak["freeSpaces"],
            "model": forecast["model"],
        }
    except Exception:  # 预测失败不影响运营数据
        forecast_summary = None

    revenue = (stats.get("todayRevenueCents") or 0) / 100
    lines = [
        f"今日订单 {stats.get('todayOrders', 0)} 笔，营收 ¥{revenue:.2f}。",
        f"当前在管停车场 {stats.get('lotCount', 0)} 个、车位 {stats.get('spaceCount', 0)} 个，"
        f"空闲 {stats.get('freeSpaces', 0)} 个、占用 {stats.get('occupiedSpaces', 0)} 个。",
        f"待使用预约 {stats.get('pendingReservations', 0)} 条。",
    ]
    if forecast_summary:
        lines.append(
            f"模型预测 {forecast_summary['lotName']} 未来 12 小时最紧张时刻为 "
            f"{forecast_summary['peakTime']}（占用率 {forecast_summary['peakRate'] * 100:.0f}%，"
            f"预计空闲 {forecast_summary['peakFree']} 位），建议提前引导分流。"
        )
    else:
        lines.append("预测服务暂不可用，本次未包含高峰预判。")

    return {
        "stats": stats,
        "forecast": forecast_summary,
        "summary": "\n".join(lines),
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
    }
