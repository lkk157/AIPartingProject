"""推理：递归预测未来 N 小时的占用率（滞后特征逐小时滚动更新）。"""

from __future__ import annotations

import json
import threading
from datetime import datetime, timedelta
from pathlib import Path

import joblib
import numpy as np
import pandas as pd

from .dataset import add_features, feature_columns, get_lot, load_dataset, model_dir
from .trainer import train

_lock = threading.Lock()
_cache: dict[str, object] = {}


def _meta_path(target: Path) -> Path:
    return target / "model_meta.json"


def load_meta(target: Path | None = None) -> dict | None:
    path = _meta_path(target or model_dir())
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None


def ensure_model(target: Path | None = None) -> dict:
    """模型不存在时惰性训练（训练一次约 1-3 秒）。"""
    directory = target or model_dir()
    with _lock:
        meta = load_meta(directory)
        model_path = directory / f"occupancy_{meta['model'] if meta else 'rf'}.joblib"
        if meta and model_path.exists():
            return meta
        meta = train(target_dir=directory)
        return meta


def _load_model(target: Path, meta: dict):
    key = f"{target}::{meta['model']}"
    if key not in _cache:
        _cache[key] = joblib.load(target / f"occupancy_{meta['model']}.joblib")
    return _cache[key]


def _history_for_lot(lot_id: int, limit: int = 168) -> list[float]:
    raw = load_dataset()
    series = raw[raw["lot_id"] == lot_id].sort_values("timestamp")["occupancy_rate"].tolist()
    return series[-limit:]


def _next_hour(ts: datetime) -> datetime:
    base = ts.replace(minute=0, second=0, microsecond=0)
    return base + timedelta(hours=1)


def _build_row(lot: dict, ts: datetime, history: list[float]) -> dict:
    hour = ts.hour
    weekday = ts.weekday()
    row = {
        "hour": hour,
        "weekday": weekday,
        "is_weekend": int(weekday >= 5),
        "is_holiday": int(ts.date().isoformat() in _holiday_dates()),
        "hour_sin": float(np.sin(2 * np.pi * hour / 24)),
        "hour_cos": float(np.cos(2 * np.pi * hour / 24)),
        "weekday_sin": float(np.sin(2 * np.pi * weekday / 7)),
        "weekday_cos": float(np.cos(2 * np.pi * weekday / 7)),
        "lag_1": history[-1],
        "lag_2": history[-2] if len(history) >= 2 else history[-1],
        "lag_24": history[-24] if len(history) >= 24 else float(np.mean(history)),
        "lag_168": history[-168] if len(history) >= 168 else float(np.mean(history)),
        "roll_3": float(np.mean(history[-3:])),
        "roll_24": float(np.mean(history[-24:])),
        "capacity": lot["capacity"],
    }
    for candidate in _all_lots():
        row[f"lot_{candidate['lot_id']}"] = int(candidate["lot_id"] == lot["lot_id"])
    return row


def _all_lots() -> list[dict]:
    from .dataset import LOTS

    return LOTS


def _holiday_dates() -> set[str]:
    from .dataset import HOLIDAYS

    return {day.isoformat() for day in HOLIDAYS}


def predict(lot_id: int, start_time: datetime | None = None, hours: int = 24,
            target: Path | None = None) -> dict:
    lot = get_lot(lot_id)
    if lot is None:
        raise KeyError(f"未知的停车场: {lot_id}")

    directory = target or model_dir()
    meta = ensure_model(directory)
    model = _load_model(directory, meta)
    columns = meta["features"]

    history = _history_for_lot(lot_id)
    if not history:
        raise ValueError("该停车场没有历史数据，无法预测")

    start = _next_hour(start_time or datetime.now())
    points: list[dict] = []
    for step in range(hours):
        ts = start + timedelta(hours=step)
        row = _build_row(lot, ts, history)
        frame = pd.DataFrame([row]).reindex(columns=columns, fill_value=0)
        rate = float(model.predict(frame)[0])
        rate = float(min(max(rate, 0.0), 1.0))
        history.append(rate)

        occupied = int(round(rate * lot["capacity"]))
        occupied = min(max(occupied, 0), lot["capacity"])
        points.append(
            {
                "time": ts,
                "predictedRate": round(rate, 4),
                "predictedOccupied": occupied,
                "freeSpaces": lot["capacity"] - occupied,
            }
        )

    return {
        "lotId": lot["lot_id"],
        "lotName": lot["name"],
        "capacity": lot["capacity"],
        "model": meta["model"],
        "generatedAt": datetime.now(),
        "points": points,
    }


def train_and_report(target: Path | None = None, n_estimators: int = 200) -> dict:
    """CLI 入口：训练并返回指标。"""
    return train(n_estimators=n_estimators, target_dir=target)


def feature_snapshot() -> list[str]:
    return feature_columns(add_features(load_dataset()))
