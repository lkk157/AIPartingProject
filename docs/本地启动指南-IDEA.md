# 本地启动指南（IntelliJ IDEA）

> 目标：在 IDEA 里一键跑起「MySQL/Redis + Spring Boot 后端 + 管理端 + 用户端 H5」。
> 仓库已内置运行配置（`.run/` 目录），导入项目后右上角运行下拉框直接可见。

---

## 0. 前置检查

| 依赖 | 要求 | 本机参考 |
|---|---|---|
| JDK | 17（必须，Spring Boot 3 不支持 8/11） | `C:\Users\Lkk\dev\tools\jdk-17.0.21.1+2` |
| Node.js | 20 / 22 | `node -v` |
| Docker Desktop | 运行中（MySQL/Redis 用容器起） | 任务栏鲸鱼图标为绿色 |
| IDEA | 2023.3+（社区版/旗舰版均可，Maven 项目不需要 Ultimate） | — |

> 后端只需 JDK + Maven，前端只需 Node，数据库全在 Docker 里，不污染本机 MySQL。

---

## 1. 导入项目

1. IDEA → **File → Open**，选择仓库根目录 `AIPartingProject`（**不要**只打开 `server`）。
2. 弹出「Trust Project」选 **Trust**。
3. 右下角提示 Maven 项目，选 **Load Maven Project**；首次会下载依赖（约 1-3 分钟）。

导入后项目树应看到 5 个模块：`server`、`ai`、`admin`、`app`、`infra`。

---

## 2. 配置 JDK 与 Maven

### 2.1 Project SDK

**File → Project Structure → Project**

- SDK：`17`（Add SDK → JDK… → 选择上面的 JDK 目录）
- Language level：`17`

**File → Project Structure → Modules → server**，确认 Module SDK 也是 17。

### 2.2 Maven

**File → Settings → Build, Execution, Deployment → Build Tools → Maven**

- **User settings file**：`C:\Users\Lkk\.m2\settings.xml`（已配置阿里云 `repository/public` 镜像，比 `repository/central` 快很多）
- **JDK for importer**：17

> 仓库里 `server/.mvn/maven.config` 已设置 60 秒 HTTP 超时与 UTF-8，用 `mvnw` 时自动生效。

---

## 3. 启动基础设施（MySQL 3307 / Redis 6380）

两种方式任选：

**方式 A（推荐，一键）**：右上角运行配置选 **`infra: docker compose up -d`** → 点绿色 ▶。它会在终端执行 `docker compose up -d`。

**方式 B（终端）**：

```bash
cd infra
cp .env.example .env      # 首次
docker compose up -d
docker ps                  # 确认 parking-mysql / parking-redis 都是 healthy
```

> 端口特意用 3307/6380，避免和你本机已装的 MySQL(3306)/Redis(6379) 冲突。
> **这一步不做，后端启动会失败**（Flyway 报 `Communications link failure`）。

---

## 4. 启动后端（Spring Boot，8080）

三种方式，任选其一：

| 方式 | 操作 | 适用 |
|---|---|---|
| **A. 点绿三角**（最直观） | 打开 `server/src/main/java/com/aiparking/ParkingApplication.java`，点行号旁 ▶ → Run | 日常调试 |
| **B. 运行配置** | 右上角选 **`server: spring-boot:run`** → ▶ | 与团队一致 |
| **C. 终端** | `cd server && ./mvnw spring-boot:run` | 无 IDEA 时 |

启动成功日志关键行：

```
Flyway ... Successfully applied 3 migrations
Tomcat started on port 8080
```

首次启动 Flyway 自动建表（V1 用户/角色、V2 停车场/车位、V3 预约/订单/支付），并初始化管理员：**`admin / admin123`**。

验证：

```bash
curl http://localhost:8080/health
# {"code":0,"message":"ok","data":{"status":"ok", ...}}
```

---

## 5. 启动前端

### 5.1 管理端（5173）

- 运行配置 **`admin: npm run dev`** → ▶
- 或终端：`cd admin && npm install && npm run dev`
- 浏览器打开 http://localhost:5173 ，用 `admin / admin123` 登录
- `/api` 由 Vite 代理到 `http://localhost:8080`（见 `admin/vite.config.ts`），**后端必须先启动**

### 5.2 用户端 H5（5174）

- 运行配置 **`app: npm run dev:h5`** → ▶
- 或终端：`cd app && npm install && npm run dev:h5`
- 浏览器打开 http://localhost:5174 （建议用手机模拟视图，页面为移动端布局）

> 微信小程序端：`npm run dev:mp-weixin`，并用微信开发者工具导入 `app/dist/dev/mp-weixin`；
> 真机调试时需在 `app/.env.local` 设置 `VITE_API_BASE=http://<电脑局域网IP>:8080`（不能用 localhost）。

---

## 6. 启动顺序与验证清单

```
infra(MySQL/Redis) → server(8080) → admin(5173) / app(5174)
```

| 检查项 | 预期 |
|---|---|
| `curl localhost:8080/health` | `code: 0`，status ok |
| 管理端登录 admin/admin123 | 进入运营看板，出现停车场/车位/营收统计 |
| 管理端「停车场管理 → 详情」 | 车位网格按状态着色，可批量创建车位 |
| 用户端 H5 登录后首页 | 显示在营停车场列表 |
| 用户端选车位 → 预约 | 提示预约成功，管理端「预约管理」出现记录 |

---

## 7. 在 IDEA 里跑测试

- 单类/单方法：打开测试类，点类名或方法旁 ▶
- 全部后端测试：右键 `server/src/test/java` → **Run 'All Tests'**，或终端 `cd server && ./mvnw test`

> 集成测试连的是 `parking_test` 库（同样在 3307 的 MySQL 容器里），**需要 infra 已启动**；Flyway 会自动建表。
> 前端测试：`cd admin && npm test` / `cd app && npm test`。

---

## 8. 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| 启动报 `Communications link failure` / Flyway 连不上 | MySQL 容器没起或没 healthy | `docker ps` 看状态；`cd infra && docker compose up -d` |
| 启动报 `Port 8080 was already in use` | 8080 被占用 | `server/src/main/resources/application.yml` 改 `server.port`，或结束占用进程 |
| `程序包 jakarta.validation 不存在` 等编译错误 | SDK 不是 17 / Maven 未刷新 | Project Structure 改 SDK 17；Maven 面板点刷新 |
| Maven 依赖下载超时 | 镜像慢或网络 | 确认 Settings 里指向 `C:\Users\Lkk\.m2\settings.xml`；必要时删 `~/.m2/repository` 重下 |
| 管理端登录报「网络异常」 | 后端未启动或代理端口不对 | 先确认 8080 可访问；检查 `admin/vite.config.ts` 的 proxy target |
| 管理端 401 后一直跳登录 | token 过期属正常行为 | 重新登录；`localStorage` 里 `parking_admin_token` 会被自动清除 |
| Docker 引擎反复退出 | Docker Desktop 自动更新/休眠 | 设置里关闭自动更新与「闲置时休眠」，或重启 Docker Desktop |
| 中文乱码 | 终端编码非 UTF-8 | IDEA 设置 File Encodings 全改 UTF-8；`maven.config` 已带 `-Dfile.encoding=UTF-8` |

---

## 9. 一键配置清单（仓库已内置）

| 运行配置名 | 作用 | 端口 |
|---|---|---|
| `infra: docker compose up -d` | 启动 MySQL(3307)/Redis(6380) | 3307 / 6380 |
| `server: spring-boot:run` | 启动后端 | 8080 |
| `admin: npm run dev` | 管理端开发服务器 | 5173 |
| `app: npm run dev:h5` | 用户端 H5 开发服务器 | 5174 |

> 这些配置在 `.run/` 目录，已随仓库提交；导入项目后 IDEA 自动识别，不需要手动创建。
