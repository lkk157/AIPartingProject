"""训练与推理测试（使用临时模型目录，轻量参数保证速度）。"""

from __future__ import annotations

from datetime import datetime
from pathlib import Path

import pytest

from app.prediction import predictor
from app.prediction.dataset import model_dir
from app.prediction.trainer import train


def test_train_writes_model_and_meta(model_env: Path) -> None:
    meta = predictor.load_meta()
    assert meta is not None
    assert meta["model"] in {"rf", "xgb"}
    assert set(meta["metrics"]) <= {"rf", "xgb"}
    assert meta["metrics"][meta["model"]]["mae"] < 0.25
    assert meta["baseline_mae"] is not None
    assert (model_dir() / f"occupancy_{meta['model']}.joblib").exists()


def test_predict_returns_reasonable_horizon(model_env: Path) -> None:
    result = predictor.predict(1, None, hours=6)

    assert result["lotId"] == 1
    assert len(result["points"]) == 6
    times = [point["time"] for point in result["points"]]
    assert times == sorted(times)
    # 默认从下一个整点开始
    assert times[0].minute == 0 and times[0] > datetime.now().replace(minute=0, second=0, microsecond=0)

    for point in result["points"]:
        assert 0.0 <= point["predictedRate"] <= 1.0
        assert point["predictedOccupied"] + point["freeSpaces"] == result["capacity"]


def test_predict_rejects_unknown_lot(model_env: Path) -> None:
    with pytest.raises(KeyError):
        predictor.predict(999, None, hours=2)


def test_train_rejects_too_short_test_window(model_env: Path) -> None:
    from app.prediction.dataset import generate_dataset

    small = generate_dataset(days=8)
    with pytest.raises(ValueError):
        train(df=small, n_estimators=4, test_days=3, save=False)
