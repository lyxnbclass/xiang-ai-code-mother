# Xiang AI Code Mother 微服务版

该目录是在保留根目录单体应用的基础上新增的微服务实现。用户能力、应用与 AI 能力、截图能力被拆为独立服务，服务间通过 Dubbo Triple 协议通信，并由 Nacos 提供注册与发现；用户服务和应用服务通过同一个 Redis 共享登录 Session。

## 模块说明

| 模块 | 类型 | 职责 |
| --- | --- | --- |
| `xiang-ai-code-common` | 依赖库 | 通用响应、异常、常量、注解和切面 |
| `xiang-ai-code-model` | 依赖库 | 跨模块共享的实体、DTO、VO 和枚举 |
| `xiang-ai-code-client` | 依赖库 | Dubbo 内部服务契约 |
| `xiang-ai-code-ai` | 依赖库 | AI 路由、代码生成、工具调用和流式处理 |
| `xiang-ai-code-user` | 可运行服务 | 用户、登录、权限与用户 RPC，HTTP 端口 `8124`，Dubbo 端口 `50051` |
| `xiang-ai-code-app` | 可运行服务 | 应用、对话、生成与部署，HTTP 端口 `8125`，Dubbo 端口 `50053` |
| `xiang-ai-code-screenshot` | 可运行服务 | 网页截图、COS 上传与截图 RPC，HTTP 端口 `8127`，Dubbo 端口 `50052` |

## 本地依赖

- JDK 21
- MySQL 8
- Redis 6 或更高版本
- Nacos 2.x（单机开发环境可使用 `startup.cmd -m standalone`）
- 可用的 AI 模型 API Key
- 截图功能需要 Chrome/Chromium 和腾讯云 COS 配置

先执行根目录的 `sql/create_table.sql` 初始化 `xiang_ai_code_mother` 数据库。复制 `.env.example` 中的配置到 IDE 环境变量或终端环境变量中，不要把真实密钥写入配置文件或提交到仓库。

三个服务必须使用相同的 `REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE` 和 `REDIS_PASSWORD`，这样应用服务才能读取用户服务创建的登录 Session。

## 构建与启动

在仓库根目录执行：

```powershell
.\mvnw.cmd -f xiang-ai-code-mother-microservice/pom.xml clean package
```

启动顺序建议为 Nacos、MySQL、Redis、用户服务、截图服务、应用服务：

```powershell
java -jar xiang-ai-code-mother-microservice/xiang-ai-code-user/target/xiang-ai-code-user-1.0-SNAPSHOT.jar
java -jar xiang-ai-code-mother-microservice/xiang-ai-code-screenshot/target/xiang-ai-code-screenshot-1.0-SNAPSHOT.jar
java -jar xiang-ai-code-mother-microservice/xiang-ai-code-app/target/xiang-ai-code-app-1.0-SNAPSHOT.jar
```

启动后可访问：

- 用户服务接口文档：`http://localhost:8124/api/doc.html`
- 应用服务接口文档：`http://localhost:8125/api/doc.html`
- 健康检查：各服务的 `/api/actuator/health`
- Prometheus 指标：各服务的 `/api/actuator/prometheus`
- Nacos 控制台：`http://localhost:8848/nacos`

生产环境建议由网关统一转发 `/api/user/**` 到用户服务、其余应用接口到应用服务，并限制 Actuator 和 Nacos 管理端点只能从管理网络访问。
