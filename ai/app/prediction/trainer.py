"""模型训练与评估：RandomForest 与 XGBoost 对比，时间序列切分避免泄漏。"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

from .dataset import LOTS, add_features, feature_columns, load_dataset, model_dir

try:  # xgboost 可选：未安装时仅训练随机森林
    from xgboost import XGBRegressor

    HAS_XGBOOST = True
except Exception:  # pragma: no cover - 环境差异
    HAS_XGBOOST = False


def compute_metrics(y_true: np.ndarray, y_pred: np.ndarray) -> dict:
    return {
        "mae": round(float(mean_absolute_error(y_true, y_pred)), 4),
        "rmse": round(float(np.sqrt(mean_squared_error(y_true, y_pred))), 4),
        "r2": round(float(r2_score(y_true, y_pred)), 4),
    }


def time_split(features: pd.DataFrame, test_days: int = 7) -> tuple[pd.DataFrame, pd.DataFrame]:
    cutoff = features["timestamp"].max() - pd.Timedelta(days=test_days)
    train = features[features["timestamp"] < cutoff]
    test = features[features["timestamp"] >= cutoff]
    return train, test


def train(
    df: pd.DataFrame | None = None,
    n_estimators: int = 200,
    test_days: int = 7,
    target_dir: Path | None = None,
    save: bool = True,
) -> dict:
    raw = load_dataset() if df is None else df
    features = add_features(raw)
    columns = feature_columns(features)
    train_df, test_df = time_split(features, test_days=test_days)

    if len(test_df) < 24:
        raise ValueError("测试集样本不足，请增加数据天数")
    if len(train_df) < 24:
        raise ValueError("训练集样本不足，请增加数据天数")

    x_train, y_train = train_df[columns], train_df["occupancy_rate"]
    x_test, y_test = test_df[columns], test_df["occupancy_rate"]

    models: dict[str, object] = {}
    metrics: dict[str, dict] = {}

    rf = RandomForestRegressor(
        n_estimators=n_estimators,
        max_depth=None,
        min_samples_leaf=2,
        random_state=42,
        n_jobs=-1,
    )
    rf.fit(x_train, y_train)
    models["rf"] = rf
    metrics["rf"] = compute_metrics(y_test.to_numpy(), rf.predict(x_test))

    if HAS_XGBOOST:
        xgb = XGBRegressor(
            n_estimators=max(60, n_estimators),
            max_depth=4,
            learning_rate=0.08,
            subsample=0.9,
            colsample_bytree=0.9,
            objective="reg:squarederror",
            random_state=42,
            n_jobs=-1,
        )
        xgb.fit(x_train, y_train)
        models["xgb"] = xgb
        metrics["xgb"] = compute_metrics(y_test.to_numpy(), xgb.predict(x_test))

    baseline_mae = float(mean_absolute_error(y_test, test_df["lag_168"]))
    best = min(metrics, key=lambda name: metrics[name]["mae"])

    meta = {
        "model": best,
        "trained_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "samples": int(len(features)),
        "train_samples": int(len(train_df)),
        "test_samples": int(len(test_df)),
        "features": columns,
        "lots": LOTS,
        "metrics": metrics,
        "baseline_mae": round(baseline_mae, 4),
        "has_xgboost": HAS_XGBOOST,
    }

    if save:
        target = target_dir or model_dir()
        target.mkdir(parents=True, exist_ok=True)
        for name, model in models.items():
            joblib.dump(model, target / f"occupancy_{name}.joblib")
        (target / "model_meta.json").write_text(
            json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        meta["saved_to"] = str(target)

    return meta
