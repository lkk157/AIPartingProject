"""预测相关 HTTP 接口。"""

from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.prediction import predictor
from app.prediction.dataset import get_lot, model_dir
from app.prediction.schema import ModelInfo, PredictRequest

router = APIRouter(prefix="/predict", tags=["prediction"])


@router.post("/occupancy")
def predict_occupancy(payload: PredictRequest) -> dict:
    if get_lot(payload.lotId) is None:
        raise HTTPException(status_code=404, detail=f"未知的停车场: {payload.lotId}")
    try:
        result = predictor.predict(payload.lotId, payload.startTime, payload.hours)
    except KeyError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # 训练/推理失败统一 503，避免暴露堆栈
        raise HTTPException(status_code=503, detail=f"预测服务暂不可用: {exc}") from exc
    return {"code": 0, "message": "ok", "data": result}


@router.get("/model/info")
def model_info() -> dict:
    meta = predictor.load_meta()
    if meta is None:
        info = ModelInfo(ready=False)
        return {"code": 0, "message": "ok", "data": info.model_dump()}

    metrics = {name: {"mae": value["mae"], "rmse": value["rmse"], "r2": value["r2"]}
               for name, value in meta.get("metrics", {}).items()}
    info = ModelInfo(
        ready=(model_dir() / f"occupancy_{meta['model']}.joblib").exists(),
        model=meta.get("model"),
        trainedAt=meta.get("trained_at"),
        samples=meta.get("samples"),
        features=meta.get("features", []),
        metrics=metrics,
        baselineMae=meta.get("baseline_mae"),
        lots=[
            {"lotId": lot["lot_id"], "name": lot["name"], "capacity": lot["capacity"]}
            for lot in meta.get("lots", [])
        ],
    )
    return {"code": 0, "message": "ok", "data": info.model_dump()}
