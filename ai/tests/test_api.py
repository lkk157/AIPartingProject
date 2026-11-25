"""预测接口测试。"""

from __future__ import annotations

from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture(scope="module")
def client(model_env: Path) -> TestClient:
    return TestClient(app)


def test_predict_endpoint_returns_points(client: TestClient) -> None:
    response = client.post(
        "/predict/occupancy",
        json={"lotId": 1, "hours": 4},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 0
    data = body["data"]
    assert data["lotId"] == 1
    assert data["lotName"]
    assert len(data["points"]) == 4
    assert all(0 <= point["predictedRate"] <= 1 for point in data["points"])


def test_predict_endpoint_default_hours(client: TestClient) -> None:
    response = client.post("/predict/occupancy", json={"lotId": 2})
    assert response.status_code == 200
    assert len(response.json()["data"]["points"]) == 24


def test_predict_endpoint_rejects_invalid_hours(client: TestClient) -> None:
    response = client.post("/predict/occupancy", json={"lotId": 1, "hours": 0})
    assert response.status_code == 422


def test_predict_endpoint_rejects_unknown_lot(client: TestClient) -> None:
    response = client.post("/predict/occupancy", json={"lotId": 999})
    assert response.status_code == 404
    assert "未知的停车场" in response.json()["detail"]


def test_model_info_reports_metrics(client: TestClient) -> None:
    response = client.get("/predict/model/info")
    assert response.status_code == 200
    data = response.json()["data"]
    assert data["ready"] is True
    assert data["model"] in {"rf", "xgb"}
    assert data["baselineMae"] is not None
    assert data["features"]
    assert "mae" in next(iter(data["metrics"].values()))


def test_health_still_ok(client: TestClient) -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["data"]["status"] == "ok"
