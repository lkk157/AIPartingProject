"""特征工程与指标计算测试（不依赖模型文件）。"""

from __future__ import annotations

from datetime import datetime

import numpy as np
import pandas as pd
import pytest

from app.prediction.dataset import (
    HOLIDAYS,
    add_features,
    feature_columns,
    generate_dataset,
    get_lot,
    is_holiday,
)
from app.prediction.trainer import compute_metrics, time_split


@pytest.fixture(scope="module")
def dataset() -> pd.DataFrame:
    return generate_dataset(days=20)


def test_dataset_shape_and_ranges(dataset: pd.DataFrame) -> None:
    assert len(dataset) == 20 * 24 * 3
    assert dataset["occupancy_rate"].between(0, 1).all()
    # 数据需要有真实波动，而不是常数序列
    assert dataset.groupby("lot_id")["occupancy_rate"].std().min() > 0.1
    assert (dataset["occupied"] + dataset["free"] == dataset["capacity"]).all()
    assert dataset["free"].min() >= 0


def test_holiday_flag_on_national_day() -> None:
    assert is_holiday(list(HOLIDAYS)[0])
    frame = add_features(generate_dataset(days=20, end=datetime(2026, 10, 10, 23, 0)))
    holiday_rows = frame[frame["timestamp"].dt.date.astype(str) == "2026-10-01"]
    assert len(holiday_rows) > 0
    assert (holiday_rows["is_holiday"] == 1).all()


def test_weekend_flag_matches_calendar(dataset: pd.DataFrame) -> None:
    frame = add_features(dataset)
    expected = (frame["timestamp"].dt.weekday >= 5).astype(int)
    assert (frame["is_weekend"] == expected).all()


def test_lag_features_follow_previous_hours(dataset: pd.DataFrame) -> None:
    frame = add_features(dataset)
    lot_one = frame[frame["lot_id"] == 1].sort_values("timestamp").reset_index(drop=True)
    raw_one = dataset[dataset["lot_id"] == 1].sort_values("timestamp").reset_index(drop=True)

    # lag_168 的前 168 行被丢弃，因此首条样本对应原始序列下标 168
    assert lot_one.loc[0, "lag_1"] == pytest.approx(raw_one.loc[167, "occupancy_rate"], abs=1e-9)
    assert lot_one.loc[0, "lag_24"] == pytest.approx(raw_one.loc[144, "occupancy_rate"], abs=1e-9)
    assert lot_one.loc[0, "lag_168"] == pytest.approx(raw_one.loc[0, "occupancy_rate"], abs=1e-9)


def test_features_have_no_nan_and_include_lot_dummies(dataset: pd.DataFrame) -> None:
    frame = add_features(dataset)
    columns = feature_columns(frame)
    assert not frame[columns].isna().any().any()
    assert {"lot_1", "lot_2", "lot_3"}.issubset(columns)
    assert len(columns) == 18


def test_unknown_lot_returns_none() -> None:
    assert get_lot(999) is None
    assert get_lot(1) is not None


def test_metrics_match_manual_calculation() -> None:
    y_true = np.array([0.2, 0.8])
    y_pred = np.array([0.3, 0.7])
    metrics = compute_metrics(y_true, y_pred)

    assert metrics["mae"] == pytest.approx(0.1, abs=1e-6)
    assert metrics["rmse"] == pytest.approx(0.1, abs=1e-6)
    assert metrics["r2"] == pytest.approx(0.8889, abs=1e-3)


def test_time_split_keeps_future_for_test(dataset: pd.DataFrame) -> None:
    frame = add_features(dataset)
    train, test = time_split(frame, test_days=3)

    assert len(train) > 0 and len(test) > 0
    assert train["timestamp"].max() < test["timestamp"].min()
    assert (test["timestamp"].max() - test["timestamp"].min()) <= pd.Timedelta(days=3)
