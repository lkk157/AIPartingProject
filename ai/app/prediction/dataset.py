"""占用率数据集：可复现的小时级样本生成 + 特征工程。

真实项目里这些数据来自 parking_record 的按小时聚合；
本项目内置生成器，保证固定 seed 下结果可复现，便于训练与答辩演示。
"""

from __future__ import annotations

import os
from datetime import date, datetime, timedelta
from pathlib import Path

import numpy as np
import pandas as pd

DATA_START = datetime(2026, 9, 14, 0, 0)
DATA_DAYS = 42
SEED = 42

# 国庆假期（用于 is_holiday 特征）
HOLIDAYS = {date(2026, 10, 1) + timedelta(days=offset) for offset in range(7)}

LOTS = [
    {"lot_id": 1, "name": "科技园办公停车场", "capacity": 120, "profile": "office"},
    {"lot_id": 2, "name": "万象城商业停车场", "capacity": 200, "profile": "mall"},
    {"lot_id": 3, "name": "滨海住宅停车场", "capacity": 80, "profile": "residential"},
]

_LOT_BY_ID = {lot["lot_id"]: lot for lot in LOTS}

# 典型日曲线（24 个整点）
_BASE_CURVE = {
    "office": [
        0.05, 0.04, 0.03, 0.03, 0.04, 0.08, 0.18, 0.42, 0.68, 0.86, 0.90, 0.91,
        0.88, 0.90, 0.92, 0.91, 0.90, 0.88, 0.78, 0.60, 0.42, 0.28, 0.16, 0.09,
    ],
    "mall": [
        0.20, 0.12, 0.08, 0.06, 0.05, 0.06, 0.10, 0.16, 0.24, 0.34, 0.48, 0.60,
        0.68, 0.74, 0.80, 0.84, 0.88, 0.90, 0.86, 0.78, 0.64, 0.48, 0.34, 0.26,
    ],
    "residential": [
        0.76, 0.72, 0.70, 0.68, 0.66, 0.62, 0.52, 0.40, 0.30, 0.24, 0.20, 0.18,
        0.18, 0.18, 0.20, 0.24, 0.30, 0.40, 0.52, 0.64, 0.72, 0.80, 0.82, 0.80,
    ],
}

_WEEKEND_FACTOR = {"office": 0.45, "mall": 1.15, "residential": 1.05}
_HOLIDAY_FACTOR = {"office": 0.30, "mall": 1.30, "residential": 1.00}

BASE_FEATURES = [
    "hour",
    "weekday",
    "is_weekend",
    "is_holiday",
    "hour_sin",
    "hour_cos",
    "weekday_sin",
    "weekday_cos",
    "lag_1",
    "lag_2",
    "lag_24",
    "lag_168",
    "roll_3",
    "roll_24",
    "capacity",
]


def data_path() -> Path:
    default = Path(__file__).resolve().parents[2] / "data" / "occupancy.csv"
    return Path(os.environ.get("AI_DATA_PATH", default))


def model_dir() -> Path:
    default = Path(__file__).resolve().parents[2] / "models"
    return Path(os.environ.get("AI_MODEL_DIR", default))


def get_lot(lot_id: int) -> dict | None:
    return _LOT_BY_ID.get(lot_id)


def is_holiday(day: date) -> bool:
    return day in HOLIDAYS


def _anchor_hour(now: datetime | None = None) -> datetime:
    """数据截止到「当前整点」，保证滞后特征是最新的。"""
    current = now or datetime.now()
    return current.replace(minute=0, second=0, microsecond=0)


def generate_dataset(days: int = DATA_DAYS, end: datetime | None = None, seed: int = SEED) -> pd.DataFrame:
    """生成 24h 小时级占用率样本（含噪声与自相关，模拟真实波动）。

    默认截止到当前整点，使 lag_1 等特征与线上预测时刻衔接。
    """
    last = _anchor_hour(end)
    start = last - timedelta(hours=days * 24 - 1)
    rng = np.random.default_rng(seed)
    rows: list[dict] = []

    for lot in LOTS:
        capacity = lot["capacity"]
        curve = _BASE_CURVE[lot["profile"]]
        prev = curve[start.hour]
        for step in range(days * 24):
            ts = start + timedelta(hours=step)
            base = curve[ts.hour]
            weekend = ts.weekday() >= 5
            holiday = is_holiday(ts.date())

            factor = 1.0
            if weekend:
                factor *= _WEEKEND_FACTOR[lot["profile"]]
            if holiday:
                factor *= _HOLIDAY_FACTOR[lot["profile"]]

            target = min(max(base * factor, 0.02), 0.98)
            noise = float(rng.normal(0, 0.025))
            rate = 0.65 * prev + 0.35 * target + noise
            rate = float(min(max(rate, 0.02), 0.98))

            occupied = int(round(rate * capacity))
            occupied = min(max(occupied, 1), capacity)
            rate = occupied / capacity

            rows.append(
                {
                    "lot_id": lot["lot_id"],
                    "capacity": capacity,
                    "timestamp": ts,
                    "occupancy_rate": round(rate, 4),
                    "occupied": occupied,
                    "free": capacity - occupied,
                }
            )
            prev = rate

    df = pd.DataFrame(rows).sort_values(["lot_id", "timestamp"]).reset_index(drop=True)
    return df


def save_dataset(df: pd.DataFrame, path: Path | None = None) -> Path:
    target = path or data_path()
    target.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(target, index=False)
    return target


def load_dataset(path: Path | None = None, max_stale_hours: int = 24) -> pd.DataFrame:
    """读取数据集；若数据已过期（超过 max_stale_hours）则自动重建，保持滞后特征新鲜。"""
    target = path or data_path()
    if target.exists():
        df = pd.read_csv(target, parse_dates=["timestamp"])
        last = df["timestamp"].max()
        if last >= _anchor_hour() - timedelta(hours=max_stale_hours):
            return df

    df = generate_dataset()
    save_dataset(df, target)
    return df


def add_features(df: pd.DataFrame) -> pd.DataFrame:
    """向量化特征构造：时间特征 + 周期编码 + 滞后与滚动统计 + 停车场 one-hot。"""
    data = df.copy()
    data["timestamp"] = pd.to_datetime(data["timestamp"])
    data = data.sort_values(["lot_id", "timestamp"]).reset_index(drop=True)

    grouped = data.groupby("lot_id")["occupancy_rate"]
    data["lag_1"] = grouped.shift(1)
    data["lag_2"] = grouped.shift(2)
    data["lag_24"] = grouped.shift(24)
    data["lag_168"] = grouped.shift(168)

    shifted = data.groupby("lot_id")["occupancy_rate"].shift(1)
    data["roll_3"] = shifted.groupby(data["lot_id"]).transform(
        lambda series: series.rolling(3, min_periods=1).mean()
    )
    data["roll_24"] = shifted.groupby(data["lot_id"]).transform(
        lambda series: series.rolling(24, min_periods=1).mean()
    )

    data["hour"] = data["timestamp"].dt.hour
    data["weekday"] = data["timestamp"].dt.weekday
    data["is_weekend"] = (data["weekday"] >= 5).astype(int)
    data["is_holiday"] = data["timestamp"].dt.date.map(lambda day: int(is_holiday(day)))
    data["hour_sin"] = np.sin(2 * np.pi * data["hour"] / 24)
    data["hour_cos"] = np.cos(2 * np.pi * data["hour"] / 24)
    data["weekday_sin"] = np.sin(2 * np.pi * data["weekday"] / 7)
    data["weekday_cos"] = np.cos(2 * np.pi * data["weekday"] / 7)

    dummies = pd.get_dummies(data["lot_id"], prefix="lot").astype(int)
    data = pd.concat([data, dummies], axis=1)

    return data.dropna(subset=["lag_1", "lag_24", "lag_168"]).reset_index(drop=True)


def feature_columns(df_with_features: pd.DataFrame) -> list[str]:
    extra = sorted(
        column
        for column in df_with_features.columns
        if column.startswith("lot_") and column != "lot_id"
    )
    return BASE_FEATURES + extra
