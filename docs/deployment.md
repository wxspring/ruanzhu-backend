# 软著材料 AI 生产系统 - 部署指南

## 目录

- [系统要求](#系统要求)
- [快速启动](#快速启动)
- [配置说明](#配置说明)
- [手动部署](#手动部署)
- [运维指南](#运维指南)
- [常见问题](#常见问题)

---

## 系统要求

### 硬件要求

| 环境 | CPU | 内存 | 磁盘 | 说明 |
|------|-----|------|------|------|
| 开发环境 | 2 核+ | 4 GB+ | 10 GB+ | 单机部署 |
| 生产环境 | 4 核+ | 8 GB+ | 50 GB+ | 建议配置 |

### 软件要求

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **操作系统**: Linux / macOS / Windows (WSL2 推荐)

---

## 快速启动

### 1. 克隆项目

```bash
git clone <repository-url>
cd ruanzhu
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，设置必要配置
vim .env
```

**必须配置**：
- `MYSQL_ROOT_PASSWORD`: MySQL root 密码
- `MYSQL_PASSWORD`: 应用数据库密码
- `REDIS_PASSWORD`: Redis 密码
- `MINIO_ROOT_PASSWORD`: MinIO 密码
- `JWT_SECRET`: JWT 签名密钥（至少 256 位）
- `OPENAI_API_KEY` 或 `QWEN_API_KEY`: AI 服务密钥（至少配置一个）

### 3. 启动服务

```bash
# 构建并启动所有服务
docker-compose up -d

# 查看启动状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 4. 初始化 MinIO

首次启动需要创建 MinIO bucket：

```bash
# 访问 MinIO 控制台：http://localhost:9001
# 使用 .env 中配置的 MINIO_ROOT_USER / MINIO_ROOT_PASSWORD 登录
# 创建名为 ruanzhu-system 的 bucket
```

或使用 mc 命令行：

```bash
# 安装 mc (MinIO Client)
# https://min.io/docs/minio/linux/reference/minio-mc.html

mc alias set ruanzhu http://localhost:9000 admin admin_password_here
mc mb ruanzhu/ruanzhu-system
```

### 5. 访问系统

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost | 默认端口 80 |
| 后端 API | http://localhost:8080 | 默认端口 8080 |
| MinIO 控制台 | http://localhost:9001 | 文件管理 |
| MySQL | localhost:3306 | 数据库 |
| Redis | localhost:6379 | 缓存 |

### 6. 创建管理员账号

首次使用需要通过 API 创建管理员：

```bash
# 直接插入数据库
docker exec -it ruanzhu-mysql mysql -u root -p ruanzhu

# 密码需要使用 BCrypt 加密（可以用后端测试工具生成）
INSERT INTO user (username, password_hash, real_name, role, status)
VALUES ('admin', '$2a$10$...', '系统管理员', 'ADMIN', 'ACTIVE');
```

或者使用系统提供的初始化脚本（如果有）。

---

## 配置说明

### 环境变量完整列表

参考 `.env.example` 文件，所有配置项都有默认值或注释说明。

### 后端配置 (`application-prod.yml`)

后端在生产环境使用 `SPRING_PROFILES_ACTIVE=prod`，配置项通过环境变量注入：

| 环境变量 | 默认值 | 说明 |
|---------|--------|------|
| `SPRING_DATASOURCE_URL` | jdbc:mysql://mysql:3306/ruanzhu | 数据库连接 |
| `SPRING_DATASOURCE_USERNAME` | ruanzhu | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | (必填) | 数据库密码 |
| `SPRING_DATA_REDIS_HOST` | redis | Redis 主机 |
| `SPRING_DATA_REDIS_PASSWORD` | (必填) | Redis 密码 |
| `MINIO_ENDPOINT` | http://minio:9000 | MinIO 端点 |
| `MINIO_ACCESS_KEY` | (必填) | MinIO 访问密钥 |
| `MINIO_SECRET_KEY` | (必填) | MinIO 秘密密钥 |
| `JWT_SECRET` | (必填) | JWT 签名密钥 |
| `OPENAI_API_KEY` | (可选) | OpenAI API 密钥 |
| `QWEN_API_KEY` | (可选) | 通义千问 API 密钥 |

### 前端 Nginx 配置

前端使用 Nginx 提供静态文件服务，并代理 API 请求到后端。配置详见 `frontend/nginx.conf`。

主要功能：
- 静态资源长期缓存
- API 请求代理到后端 (`/api/` → `backend:8080`)
- Swagger UI 代理
- SPA 路由支持（fallback 到 index.html）
- Gzip 压缩
- 安全头信息

---

## 手动部署

如果不使用 Docker，可以手动部署各组件：

### 1. 安装依赖

```bash
# MySQL 8.0
# Redis 7.0
# MinIO
# JDK 17
# Node.js 18
# Maven 3.9
```

### 2. 初始化数据库

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. 启动 MinIO

```bash
minio server /data/minio --console-address ":9001"
# 创建 bucket: ruanzhu-system
```

### 4. 构建后端

```bash
mvn clean package -DskipTests
java -jar target/ruanzhu-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### 5. 构建前端

```bash
cd frontend
npm install
npm run build
# 将 dist/ 目录部署到 nginx 或其他静态文件服务器
```

### 6. 配置 Nginx

参考 `frontend/nginx.conf`，调整 `proxy_pass` 指向实际的后端地址。

---

## 运维指南

### 常用命令

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启某个服务
docker-compose restart backend

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
docker-compose logs -f frontend

# 进入容器
docker exec -it ruanzhu-backend sh
docker exec -it ruanzhu-mysql mysql -u root -p

# 清理未使用的资源
docker system prune -a
```

### 数据备份

```bash
# 备份 MySQL 数据
docker exec ruanzhu-mysql mysqldump -u root -p ruanzhu > backup_$(date +%Y%m%d).sql

# 备份 MinIO 数据
docker run --rm -v ruanzhu_minio-data:/data -v $(pwd):/backup alpine \
  tar czf /backup/minio_backup_$(date +%Y%m%d).tar.gz /data
```

### 数据恢复

```bash
# 恢复 MySQL 数据
docker exec -i ruanzhu-mysql mysql -u root -p ruanzhu < backup_20260813.sql
```

### 监控

```bash
# 查看资源使用
docker stats

# 查看容器健康状态
docker inspect --format='{{.State.Health.Status}}' ruanzhu-backend
```

### 升级部署

```bash
# 1. 备份数据（见上）
# 2. 拉取新代码
git pull origin main

# 3. 重新构建并启动
docker-compose build
docker-compose up -d

# 4. 检查日志确认无错误
docker-compose logs -f
```

---

## 常见问题

### Q: 后端启动失败，报数据库连接错误

**A**: 检查以下几点：
1. MySQL 是否已完全启动：`docker-compose ps mysql`
2. 数据库初始化脚本是否执行成功：`docker-compose logs mysql`
3. `.env` 中的数据库配置是否正确

### Q: MinIO 无法上传文件

**A**: 确保已创建 `ruanzhu-system` bucket：
```bash
docker exec -it ruanzhu-minio mc alias set local http://localhost:9000 admin admin_password
docker exec -it ruanzhu-minio mc mb local/ruanzhu-system
```

### Q: AI 生成功能不可用

**A**: 检查 AI 服务配置：
1. `.env` 中配置了 `OPENAI_API_KEY` 或 `QWEN_API_KEY`
2. 后端日志是否有 API 调用错误：`docker-compose logs backend | grep -i ai`
3. 网络是否能访问 AI 服务 API

### Q: 前端页面空白或 404

**A**: 检查：
1. 前端容器是否启动：`docker-compose ps frontend`
2. Nginx 配置是否正确：`docker-compose logs frontend`
3. 后端 API 是否正常：`curl http://localhost:8080/`

### Q: 如何修改端口

**A**: 修改 `.env` 中对应的端口配置，然后重启：
```bash
FRONTEND_PORT=8081   # 修改前端端口
BACKEND_PORT=8082    # 修改后端端口
docker-compose down
docker-compose up -d
```

### Q: 如何查看 AI 生成任务状态

**A**: 通过 API 查询：
```bash
# 查询某个项目的所有任务
curl http://localhost:8080/api/tasks/projects/1 \
  -H "Authorization: Bearer <token>"
```

---

## 技术支持

如有问题，请联系开发团队或提交 Issue。
