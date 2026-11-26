"""运营分析测试：预定义统计 + 预测高峰，不依赖真实后端。"""

from __future__ import annotations

from datetime import datetime, timedelta
from types import SimpleNamespace

import pytest

from app.agent import ops


class FakePredictor:
    @staticmethod
    def predict(lot_id: int, start, hours: int = 12):
        base = datetime(2026, 11, 26, 9, 0)
        return {
            "lotName": "科技园办公停车场",
            "model": "xgb",
            "points": [
                {"time": base, "predictedRate": 0.4, "predictedOccupied": 48, "freeSpaces": 72},
                {"time": base + timedelta(hours=6), "predictedRate": 0.88, "predictedOccupied": 106, "freeSpaces": 14},
            ],
        }


def test_overview_builds_summary(monkeypatch: pytest.MonkeyPatch) -> None:
    stats = {
        "lotCount": 2,
        "spaceCount": 5,
        "freeSpaces": 3,
        "occupiedSpaces": 1,
        "todayOrders": 4,
        "todayRevenueCents": 1850,
        "pendingReservations": 1,
    }
    monkeypatch.setattr(ops, "_get", lambda path, token, params=None: stats)
    monkeypatch.setattr(ops, "predictor", FakePredictor)

    data = ops.overview("admin-token")

    assert data["stats"]["todayOrders"] == 4
    assert data["forecast"]["peakTime"] == "15:00"
    assert "今日订单 4 笔" in data["summary"]
    assert "¥18.50" in data["summary"]
    assert "15:00" in data["summary"]


def test_overview_survives_forecast_failure(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(ops, "_get", lambda path, token, params=None: {"todayOrders": 1})

    class BrokenPredictor:
        @staticmethod
        def predict(*args, **kwargs):
            raise RuntimeError("model down")

    monkeypatch.setattr(ops, "predictor", BrokenPredictor)
    data = ops.overview("token")

    assert data["forecast"] is None
    assert "预测服务暂不可用" in data["summary"]


def test_forbidden_backend_raises_ops_error(monkeypatch: pytest.MonkeyPatch) -> None:
    class FakeResponse:
        status_code = 403

        @staticmethod
        def json():
            return {}

    monkeypatch.setattr(ops.httpx, "get", lambda *args, **kwargs: FakeResponse())
    with pytest.raises(ops.OpsError):
        ops.overview("user-token")
