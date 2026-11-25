from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="AI Parking Service",
    description="智能停车场管理系统 - AI 服务（预测 / Agent / RAG）",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health() -> dict:
    return {
        "code": 0,
        "message": "ok",
        "data": {"status": "ok", "service": "parking-ai"},
    }
