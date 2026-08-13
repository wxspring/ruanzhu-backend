# 软著材料 AI 生产系统 (Ruanzhu)

> 使用 AI 自动生成软件著作权（软著）申请材料，包括源代码文档、操作手册和软件概要。

## ✨ 功能特点

- 🤖 **AI 驱动内容生成**：自动生成源代码文档、操作手册、软件概要
- 📝 **在线编辑器**：Monaco Editor（代码）+ TipTap（富文本）
- 📂 **种子代码上传**：支持 ZIP 包上传，自动分析代码结构
- 🔄 **异步任务处理**：后台生成，实时进度追踪
- 👥 **多角色管理**：管理员 / 普通员工权限分离
- 📦 **一键导出**：打包生成所有申请材料（ZIP 格式）

## 🏗️ 技术架构

### 后端
- **Spring Boot 3.2.5** + Java 17
- **MyBatis-Plus** (ORM)
- **MySQL 8.0** (数据库)
- **Redis 7.0** (缓存)
- **MinIO** (对象存储)
- **Spring AI** (AI 集成)

### 前端
- **React 18** + **TypeScript** + **Vite**
- **Ant Design 5** (UI 组件)
- **Monaco Editor** (代码编辑)
- **TipTap** (富文本编辑)
- **Zustand** (状态管理)

## 🚀 快速启动

### 使用 Docker Compose（推荐）

```bash
# 1. 克隆项目
git clone <repository-url>
cd ruanzhu

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，配置必要的密码和 API Key

# 3. 启动所有服务
docker-compose up -d

# 4. 查看状态
docker-compose ps
```

启动后访问：
- 前端：http://localhost
- 后端 API：http://localhost:8080
- MinIO 控制台：http://localhost:9001

详细部署说明请参考 [部署指南](docs/deployment.md)。

### 本地开发

**后端：**
```bash
# 需要 JDK 17+ 和 Maven 3.9+
mvn spring-boot:run
```

**前端：**
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
```

## 📁 项目结构

```
ruanzhu/
├── src/main/java/com/company/ruanzhu/
│   ├── common/          # 公共模块（异常、工具、配置）
│   ├── project/         # 项目管理模块
│   ├── generate/        # 内容生成模块（AI 集成）
│   ├── file/            # 文件管理模块（MinIO）
│   ├── review/          # 审核模块
│   └── user/            # 用户认证模块（JWT）
├── frontend/            # React 前端
│   ├── src/
│   │   ├── components/  # 可复用组件
│   │   ├── pages/       # 页面组件
│   │   ├── stores/      # Zustand 状态
│   │   └── api/         # API 客户端
│   └── package.json
├── docker-compose.yml   # Docker Compose 配置
├── Dockerfile           # 后端 Dockerfile
├── .env.example         # 环境变量示例
└── docs/                # 文档
    └── deployment.md    # 部署指南
```

## 🔑 核心工作流

1. **创建项目**：填写软件基本信息
2. **上传种子代码**：上传 ZIP 格式源代码
3. **AI 生成内容**：
   - 软件概要（开发目的、面向领域、主要功能、技术特点）
   - 源代码文档（60 页，每页 50+ 行）
   - 操作手册（图文并茂）
4. **在线编辑**：在 Monaco / TipTap 中修改生成内容
5. **审核确认**：管理员审核生成的材料
6. **导出打包**：一键下载所有申请材料（ZIP）

## 📄 许可证

内部项目，仅供公司内部使用。
