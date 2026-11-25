# 智泊停车 · 智能停车场管理系统

基于大语言模型与时序预测的智能停车场管理系统（本科毕业设计 / 工程实践）。

- 业务后端：Spring Boot 3.3 + MyBatis-Plus + MySQL 8 + Redis + JWT/RBAC
- AI 服务：FastAPI + LangChain/LangGraph + Ollama（qwen2.5:7b）+ Chroma RAG + RF/XGBoost 预测
- 管理端：Vue 3 + TypeScript + Element Plus + ECharts
- 用户端：uni-app（Vue 3 + TS），一套代码 → H5 + 微信小程序
- 部署：Docker Compose

## 目录结构

```
server/   Spring Boot 业务后端
ai/       FastAPI AI 服务（预测 / Agent / RAG）
admin/    Vue3 运营管理后台
app/      uni-app 用户端
infra/    Docker Compose 与初始化脚本
docs/     开发规划、阶段规范、UI 规范
```

## 环境要求

| 组件 | 版本 |
|---|---|
| JDK | 17 |
| Maven | 3.9+（使用仓库内 `mvnw` 即可） |
| Node.js | 22 |
| Python | 3.11 |
| Docker | 24+ |

## 快速开始

```bash
# 1. 基础设施（MySQL 3307 / Redis 6380，避开本机已占用的 3306/6379）
cd infra && cp .env.example .env && docker compose up -d

# 2. 后端（http://localhost:8080）
cd server && ./mvnw spring-boot:run

# 3. AI 服务（http://localhost:8000）
cd ai && source .venv/Scripts/activate   # Git Bash on Windows
pip install -r requirements.txt -r requirements-agent.txt -r requirements-ml.txt
uvicorn app.main:app --reload --port 8000

# 4. 管理端（http://localhost:5173）
cd admin && npm install && npm run dev

# 5. 用户端 H5（http://localhost:5174）
cd app && npm install && npm run dev:h5
```

## 测试

```bash
cd server && ./mvnw test     # JUnit5 + MockMvc
cd ai && pytest              # pytest
cd admin && npm test         # vitest
cd app && npm test           # vitest
```

## 文档

- [开发规划（LangChain 版）](docs/开发规划-LangChain版.md)
- [开发阶段划分与测试规范](docs/开发阶段划分与测试规范.md)
- [前端 UI 规范](docs/前端UI规范.md)

## 阶段与版本

| 阶段 | 内容 | tag |
|---|---|---|
| P0 | 工程骨架与环境 | v0.1.0 |
| P1 | 认证与权限 | v0.2.0 |
| P2 | 停车场 / 车位 / 收费规则 | v0.3.0 |
| P3 | 预约 / 计费 / 支付 | v0.4.0 |
| P4 | 用户端 + 管理端 | v0.5.0 |
| P5 | 占用率预测 | v0.6.0 |
| P6 | LangChain Agent | v0.7.0 |
| P7 | RAG + AI 运营分析 | v0.8.0 |
| P8 | 联调 / 部署 / 验收 | v1.0.0 |
