# SeaURL Service

SeaURL 的后端服务，为个人网址收藏、笔记、文件管理及 AI 工具等功能提供 API。项目使用 Java 17 和 Spring Boot，将用户账户与个人空间功能放在同一个应用中，可通过 Maven 或 Docker 部署。

本仓库提供服务端代码，前端需要单独部署。目录名为 `seaurl-service`，Maven 构建产物名为 `seaurl-platform`。

## 功能

- **账户与个人主页**：注册、邮箱验证、登录、账户资料、关注和用户动态。
- **网址收藏**：网址分类、收藏管理、网站图标抓取、常用网址和分享浏览。
- **笔记与文件**：笔记分类、文件分类、上传下载、文件分享和传输记录。
- **AI 工具**：多模型对话、流式响应、对话记录、图像生成与翻译接口。
- **个人工具**：邮箱收发与同步、工时记录、密码条目管理、二维码识别、天气和节假日查询。

AI、邮箱、天气及云存储等功能需要对应服务商的账号、凭据和网络访问条件。

## 技术栈

| 组件 | 用途 |
| --- | --- |
| Java 17 / Spring Boot 3.0.2 | 应用运行环境与基础框架 |
| Spring WebFlux / Reactor | HTTP API、响应式请求处理与流式输出 |
| Spring Data MongoDB | 业务数据、登录会话与临时验证码持久化 |
| Caffeine | 进程内缓存 |
| JJWT / BCrypt | JWT 生成与账户密码散列 |
| 本地文件系统 / 阿里云 OSS | 文件存储 |
| Maven / Docker | 构建与部署 |

当前服务无需 Redis。配置模板中的 `SEAURL_REDIS_*` 为历史遗留项，可忽略。

## 项目结构

```text
seaurl-service/
├── config/
│   └── application-local.yml.example   # 本地配置模板
├── src/main/
│   ├── docker/Dockerfile               # JAR 运行镜像
│   ├── java/com/seaurl/platform/
│   │   ├── PlatformApplication.java    # 启动入口
│   │   ├── common/                     # 鉴权、上下文、配置与公共模型
│   │   ├── users/                      # 账户、用户资料与社交功能
│   │   └── space/                      # 网址、笔记、文件与工具功能
│   └── resources/
│       ├── application.yml            # 公共配置
│       ├── application-dev.yml        # 开发环境配置
│       └── application-prod.yml       # 生产环境配置
├── src/test/                          # 单元测试与集成测试
└── pom.xml
```

## 快速开始

### 1. 准备环境与依赖

- JDK 17。
- Maven 3.x，建议使用 3.8 或更新版本。
- 可连接、具备读写和创建索引权限的 MongoDB 实例。
- 公共依赖 `com.seaurl:seaurl-common:2.5.1.2-SNAPSHOT`。
- Docker：仅容器部署时需要。

```bash
git clone https://github.com/seaurl/seaurl-service.git
cd seaurl-service
```

`seaurl-common` 是独立依赖。若 Maven 仓库中尚无该版本，先取得对应源码并安装到本地仓库。假设它与本项目位于同一父目录：

```bash
mvn -f ../seaurl-common/pom.xml clean install -DskipTests
```

### 2. 配置本地环境

以下命令均在本项目根目录执行：

```bash
cp config/application-local.yml.example config/application-local.yml
```

编辑 `config/application-local.yml`，替换同名配置，并补充模板中没有的条目：

```yaml
SEAURL_PROFILE: dev
SEAURL_MONGODB_URI: mongodb://127.0.0.1:27017/dev_seaurl_db
SEAURL_SIGNING_KEY: replace-with-a-random-secret-at-least-32-bytes
SEAURL_CLIENT_SECRET: replace-with-your-own-client-secret
SEAURL_WEB_HOST: http://localhost:3000
SEAURL_COOKIE_DOMAIN: localhost
SEAURL_STORAGE_TYPE: local
SEAURL_STORAGE_LOCAL_BASE_URL: http://localhost:7777/api/uploads
```

将示例前端地址改为自己的地址。JWT 签名密钥至少需要 32 个 UTF-8 字节，可使用 `openssl rand -hex 32` 生成随机值。

配置模板中的第三方凭据也需要保留为可解析的配置值。当前配置没有统一的第三方功能开关；保留示例占位值不代表已接通对应服务，调用这些功能时仍需真实有效的凭据。

应用通过 `optional:file:./config/application-local.yml` 加载本地配置，路径相对于进程工作目录。该文件已被 Git 忽略，请勿提交真实凭据。

### 3. 启动服务

```bash
mvn spring-boot:run
```

默认端口为 `7777`，API 基础地址为 `http://localhost:7777/api`。本服务提供 API，访问根地址不会显示前端页面。

开发环境默认数据库名为 `dev_seaurl_db`，生产环境为 `prod_seaurl_db`。如需自定义数据库名，同时覆盖 `spring.data.mongodb.database`；不要只修改 MongoDB URI 中的数据库路径。

## 配置说明

完整字段参见 [`application.yml`](src/main/resources/application.yml)、[`application-prod.yml`](src/main/resources/application-prod.yml) 和[本地配置模板](config/application-local.yml.example)。

| 配置项 | 说明 |
| --- | --- |
| `SEAURL_PROFILE` | 激活环境，默认 `prod`；本地开发设置为 `dev` |
| `SEAURL_SERVER_PORT` | 监听端口，默认 `7777` |
| `SEAURL_MONGODB_URI` | MongoDB 连接地址，需按实际认证方式配置 |
| `SEAURL_SIGNING_KEY` / `SEAURL_CLIENT_SECRET` | JWT 签名密钥与客户端密钥 |
| `SEAURL_WEB_HOST` / `SEAURL_COOKIE_DOMAIN` | 前端地址与 Cookie 域名，注意下述生产环境覆盖规则 |
| `SEAURL_STORAGE_TYPE` | `local` 或 `oss`，默认 `local` |
| `SEAURL_STORAGE_LOCAL_BASE_PATH` | 本地文件目录，默认 `./data/uploads` |
| `SEAURL_STORAGE_LOCAL_BASE_URL` | 对外文件地址，默认 `http://localhost:7777/api/uploads` |
| `SEAURL_ALIYUN_ACCESS_KEY_ID` / `SEAURL_ALIYUN_ACCESS_KEY_SECRET` | 阿里云接口访问凭据 |
| `SEAURL_ALIYUN_RAM_ACCESS_KEY_ID` / `SEAURL_ALIYUN_RAM_ACCESS_KEY_SECRET` / `SEAURL_ALIYUN_RAM_ROLE_ARN` | OSS 临时上传凭据所需的 RAM / STS 配置 |
| `SEAURL_DASHSCOPE_API_KEY` | 阿里云百炼 / DashScope API Key |
| `SEAURL_BAIDUYUN_ACCESS_KEY_ID` / `SEAURL_BAIDUYUN_ACCESS_KEY_SECRET` | 百度云接口凭据 |
| `SEAURL_DEEPSEEK_ACCESS_KEY_SECRET` / `SEAURL_DOUBAO_ACCESS_KEY_SECRET` | DeepSeek、豆包 API Key |
| `SEAURL_WEATHER_FREE_*` / `SEAURL_WEATHER_STANDARD_*` | 天气服务对应档位的私钥、凭据 ID 和项目 ID |
| `SEAURL_MAIL_SMTP_PASSWORD` | 系统邮件发送凭据 |

### 存储方式

`local` 模式将文件保存在本地目录，并通过 `/api/uploads/**` 提供访问。部署到服务器时，将 `SEAURL_STORAGE_LOCAL_BASE_URL` 改为实际可访问的公网地址；容器部署需要持久化文件目录。

`oss` 模式需要配置阿里云访问凭据、Bucket、Endpoint 和 CDN 地址；使用临时上传凭据时还需要 RAM / STS 配置。`storage.minio` 目前仅预留配置，尚无对应存储实现。

### 生产环境配置覆盖

`application-prod.yml` 中的前端域名、Cookie 域名、OSS Bucket、部分接口地址和发件地址等仍包含 SeaURL 的固定值。这些字段不能仅通过同名 `SEAURL_*` 变量覆盖。自托管时，可创建外置配置文件，例如 `config/application-deploy.yml`，直接覆盖实际属性：

```yaml
web:
  host: https://your-domain.example
cookie:
  domain: your-domain.example
mail:
  address: notice@your-domain.example
# 使用 OSS 时，另行覆盖 aliyun.oss-bucket、oss-bucket-endpoint、
# oss-bucket-cdn-host 等实际属性。
```

启动时通过 `--spring.config.additional-location=file:./config/application-deploy.yml` 加载该文件。外置配置中的密钥同样应保留在本地或通过环境注入。

## 构建与部署

### 运行 JAR

```bash
mvn clean package
java -jar target/seaurl-platform-1.0-SNAPSHOT.jar
```

构建产物由 `pom.xml` 中的 `artifactId` 和 `version` 决定。需要跳过测试时使用 `mvn clean package -DskipTests`。

### Docker 部署

先生成 JAR，再从项目根目录构建镜像：

```bash
docker build \
  --build-arg JAR_FILE=target/seaurl-platform-1.0-SNAPSHOT.jar \
  -t seaurl-service:local \
  -f src/main/docker/Dockerfile .
```

镜像只复制 JAR，凭据在运行时注入。准备本地 `.env` 文件，使用 Docker 要求的 `KEY=value` 格式，例如：

```dotenv
SEAURL_PROFILE=prod
SEAURL_MONGODB_URI=mongodb://your-mongodb-host:27017/prod_seaurl_db
SEAURL_SIGNING_KEY=replace-with-a-random-secret-at-least-32-bytes
SEAURL_CLIENT_SECRET=replace-with-your-own-client-secret
SEAURL_STORAGE_TYPE=local
SEAURL_STORAGE_LOCAL_BASE_PATH=/usr/local/seaurl-platform/data/uploads
SEAURL_STORAGE_LOCAL_BASE_URL=https://your-domain.example/api/uploads
```

此处只展示基础条目，还需将配置模板中的其余凭据以 `KEY=value` 格式补齐；不能直接把 YAML 文件改名为 `.env`。MongoDB 地址必须能从容器内访问，容器中的 `localhost` 指向容器自身。

准备好上一节的 `config/application-deploy.yml` 后运行：

```bash
mkdir -p data/uploads
docker run -d \
  --name seaurl-service \
  --restart unless-stopped \
  --env-file .env \
  -p 7777:7777 \
  -v "$(pwd)/data/uploads:/usr/local/seaurl-platform/data/uploads" \
  -v "$(pwd)/config/application-deploy.yml:/usr/local/seaurl-platform/config/application-deploy.yml:ro" \
  seaurl-service:local \
  --spring.config.additional-location=file:/usr/local/seaurl-platform/config/application-deploy.yml
```

生产部署时，按实际需要限制 Actuator 暴露范围和跨域来源；当前默认配置暴露全部管理端点，并允许任意来源的跨域请求。

## API 与登录鉴权

| 路径 | 用途 |
| --- | --- |
| `/api/users/**` | 账户与用户相关接口 |
| `/api/space/**` | 个人空间、文件与工具接口 |
| `/api/uploads/**` | 本地存储文件访问 |

注册入口为 `POST /api/users/web/register`。登录要求账户已完成邮箱验证，因此使用完整注册流程前需接通系统邮件发送服务。当前仓库未提供默认管理员账号或初始演示数据。

已有有效账户时，可以调用登录接口：

```bash
curl -X POST http://localhost:7777/api/users/web/login \
  -H 'Content-Type: application/json' \
  -d '{"userName":"your-user-name","password":"your-password"}'
```

取成功响应中的 `data.tokenId`，在后续受保护请求中传入 `token-id` 请求头：

```bash
curl http://localhost:7777/api/users/crud/me \
  -H 'token-id: <data.tokenId>'
```

鉴权过滤器对 `/users/info/**`、`/users/web/**`、`/space/info/**`、`/space/web/**`、上传文件与 WebSocket 前缀放行，具体接口仍可能自行检查登录状态。接口定义与参数以 `users/controller`、`space/controller` 及对应 `request` 类为准。

## 测试

```bash
# 默认单元测试，不包含命名为 *IT 的集成测试
mvn test

# MongoDB 集成测试：先准备隔离的开发数据库和完整配置
mvn test -Dtest=UserSessionServiceIT,TempCodeServiceIT,DashboardServiceIT

# 网站图标抓取测试：需要外网访问
mvn test -Dtest=ReptileServiceFaviconIT -DrunNetworkTests=true
```

MongoDB 集成测试会读写数据并创建索引，请使用专用测试库。`DashboardServiceIT` 还依赖已有的未删除网址数据，空库不一定满足其断言。项目目前未配置自动执行 `*IT` 的 Failsafe 流程。

## 参与贡献

欢迎通过 [Issue](https://github.com/seaurl/seaurl-service/issues) 反馈问题或提出建议，也欢迎提交 Pull Request。问题反馈请附上复现步骤、运行环境和已脱敏的日志；提交代码时请说明行为变化和验证方式，并同步更新相关文档。

## 许可证

当前仓库尚未提供 `LICENSE` 文件，具体使用、修改和再分发权限需由维护者明确。第三方依赖遵循各自的许可证。
