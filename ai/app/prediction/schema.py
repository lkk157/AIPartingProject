"""预测接口的请求/响应模型。"""

from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, Field


class PredictRequest(BaseModel):
    lotId: int = Field(..., ge=1, description="停车场 ID")
    startTime: datetime | None = Field(default=None, description="预测起始时间，默认下一个整点")
    hours: int = Field(default=24, ge=1, le=48, description="预测时长（小时）")


class PredictPoint(BaseModel):
    time: datetime
    predictedRate: float = Field(..., ge=0, le=1, description="预测占用率 0-1")
    predictedOccupied: int = Field(..., ge=0)
    freeSpaces: int = Field(..., ge=0)


class PredictResponse(BaseModel):
    lotId: int
    lotName: str
    capacity: int
    model: str
    generatedAt: datetime
    points: list[PredictPoint]


class MetricBlock(BaseModel):
    mae: float
    rmse: float
    r2: float


class ModelLot(BaseModel):
    lotId: int
    name: str
    capacity: int


class ModelInfo(BaseModel):
    ready: bool
    model: str | None = None
    trainedAt: str | None = None
    samples: int | None = None
    features: list[str] = []
    metrics: dict[str, MetricBlock] = {}
    baselineMae: float | None = None
    lots: list[ModelLot] = []
