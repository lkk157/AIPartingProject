"""训练脚本：python -m scripts.train_model [--estimators 200]

生成数据集（若缺失）→ 训练 RF/XGBoost → 输出指标 → 模型写入 models/。
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.prediction.dataset import data_path, generate_dataset, save_dataset  # noqa: E402
from app.prediction.trainer import train  # noqa: E402


def main() -> None:
    parser = argparse.ArgumentParser(description="训练停车场占用率预测模型")
    parser.add_argument("--estimators", type=int, default=200)
    parser.add_argument("--days", type=int, default=42)
    parser.add_argument("--test-days", type=int, default=7)
    args = parser.parse_args()

    if not data_path().exists():
        df = generate_dataset(days=args.days)
        save_dataset(df)
        print(f"已生成数据集: {data_path()} ({len(df)} 行)")

    meta = train(n_estimators=args.estimators, test_days=args.test_days)
    print(json.dumps(
        {
            "best": meta["model"],
            "metrics": meta["metrics"],
            "baseline_mae": meta["baseline_mae"],
            "samples": meta["samples"],
            "saved_to": meta.get("saved_to"),
        },
        ensure_ascii=False,
        indent=2,
    ))


if __name__ == "__main__":
    main()
