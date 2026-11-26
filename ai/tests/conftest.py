"""测试夹具：用临时目录隔离数据集与模型，训练一个轻量模型供 API 测试使用。"""

from __future__ import annotations

import os
from pathlib import Path

import pytest

from app.prediction.dataset import generate_dataset, save_dataset
from app.prediction.trainer import train

DAYS = 18
TEST_DAYS = 3
ESTIMATORS = 8


@pytest.fixture(scope="session")
def model_env(tmp_path_factory: pytest.TempPathFactory) -> Path:
    root = tmp_path_factory.mktemp("prediction")
    os.environ["AI_DATA_PATH"] = str(root / "occupancy.csv")
    os.environ["AI_MODEL_DIR"] = str(root / "models")
    os.environ["AI_RAG_DIR"] = str(root / "rag")

    df = generate_dataset(days=DAYS)
    save_dataset(df)
    train(df=df, n_estimators=ESTIMATORS, test_days=TEST_DAYS)
    return root
