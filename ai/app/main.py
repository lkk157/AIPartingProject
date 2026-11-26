from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.agent import router as agent_router
from app.api.prediction import router as prediction_router

app = FastAPI(
    title="AI Parking Service",
    description="智能停车场管理系统 - AI 服务（预测 / Agent / RAG）",
    version="0.3.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(prediction_router)
app.include_router(agent_router)


@app.get("/health")
def health() -> dict:
    return {
        "code": 0,
        "message": "ok",
        "data": {"status": "ok", "service": "parking-ai", "version": app.version},
    }
