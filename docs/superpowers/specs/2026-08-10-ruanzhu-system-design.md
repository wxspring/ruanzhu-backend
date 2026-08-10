# 软著材料 AI 生产设计系统 - 技术设计文档

**版本**: 1.0  
**日期**: 2026-08-10  
**状态**: 设计完成，待实现

---

## 1. 项目概述

### 1.1 项目背景

软件著作权（软著）申请需要提交一系列材料，包括源代码文档、操作手册、软件概要等。这些材料的准备工作耗时耗力，尤其是源代码文档需要整理 60 页（约 3000 行）代码，操作手册需要编写详细的功能说明并配图。

### 1.2 系统定位

**内部工具**：帮助公司员工批量、高效地为客户生产软著申请材料。

### 1.3 核心价值

- **自动化生成**：AI 自动生成源代码文档、操作手册、软件概要
- **质量保证**：人工审核编辑后才交付，确保内容准确
- **效率提升**：从数天缩短到数小时完成一套材料

---

## 2. 需求规格

### 2.1 用户角色

| 角色 | 权限 |
|------|------|
| **普通员工** | 创建项目、编辑内容、提交审核 |
| **管理员** | 用户管理、审核内容、查看统计 |

### 2.2 功能需求

#### 2.2.1 项目管理
- 创建软著申请项目
- 填写软件概要信息
- 上传种子代码（ZIP 压缩包）
- 项目状态跟踪

#### 2.2.2 材料生成
- AI 分析种子代码，自动识别编程语言、框架
- AI 扩展/生成源代码（目标 5000-20000 行）
- AI 生成操作说明书（含功能说明、操作指引）
- AI 生成界面示意图（线框图或逼真截图）
- 异步任务，实时进度反馈

#### 2.2.3 审核编辑
- 在线编辑软件概要、源代码、操作手册
- 单字段重新生成（AI）
- 版本管理（可回溯历史版本）
- 提交审核流程

#### 2.2.4 导出打包
- 生成软件概要 .txt
- 生成源代码 .docx / .pdf
- 生成操作手册 .docx / .pdf
- 打包成 ZIP 下载

### 2.3 软件概要信息

```
软著名称：***
版本号：***
软件分类：***
开发的硬件环境：***
运行的硬件环境：***
开发该软件的操作系统：***
软件开发环境/开发工具：***
该软件的运行平台/操作系统：***
软件运行支撑环境/支持软件：***
编程语言：*** 
源程序量：*** 
开发目的：***（300-500字，核心关注点）
面向领域：***（200-300字，行业背景、应用场景）
主要功能：***（800-1000字，功能作用、关系、衔接流程）
技术特点：***（150-200字，架构、技术、创新点）
软件的技术特点选项：***
```

### 2.4 导出文件规范

ZIP 包结构：
```
{软著名称}.zip
├── {软著名称}.txt                  ← 软件概要
├── {软著名称}+程序.docx            ← 源代码（Word）
├── {软著名称}+程序.pdf             ← 源代码（PDF）
├── {软著名称}+说明.docx            ← 操作说明书（Word）
└── {软著名称}+说明.pdf             ← 操作说明书（PDF）
```

程序文档格式：
- 60 页，前 30 页 + 后 30 页
- 每页不少于 50 行代码
- 页眉：软件名称 + 版本号
- 等宽字体（Courier New / Consolas），小四号
- 每行显示行号

### 2.5 源代码策略

```
代码量判断逻辑：
├─ 代码总行数 < 3000 行
│   └─ 提交全部代码（按格式排版）
│
├─ 3000 行 ≤ 代码总行数 ≤ 20000 行
│   └─ 提交前 30 页 + 后 30 页（共 60 页）
│      ├─ 前 30 页：核心业务逻辑（Service、Controller）
│      └─ 后 30 页：特色功能、算法实现、数据处理
│
└─ 代码总行数 > 20000 行
    └─ 智能筛选最能体现功能的 60 页
```

---

## 3. 系统架构

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    React 前端（SPA）                          │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────┐    │
│  │ 项目管理 │  │ 编辑器  │  │ 审核台  │  │ 用户/系统管理 │    │
│  └─────────┘  └─────────┘  └─────────┘  └─────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │ REST API
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot 后端（模块化单体）                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              API Gateway / Controller 层              │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────┐   │
│  │ 项目模块 │  │ 生成模块 │  │ 审核模块 │  │   文件模块   │   │
│  │ Project │  │ Generate│  │ Review  │  │    File      │   │
│  └─────────┘  └─────────┘  └─────────┘  └─────────────┘   │
│  ┌─────────┐  ┌─────────────────────────────────────────┐  │
│  │ 用户模块 │  │         基础设施层 Infrastructure         │  │
│  │  User   │  │ AI客户端 | 存储客户端 | 文档生成器 | 消息队列 │  │
│  └─────────┘  └─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   ┌─────────┐        ┌─────────┐        ┌─────────┐
   │  MySQL  │        │  MinIO  │        │ AI 服务  │
   │ (数据)   │        │ (文件)   │        │ (可插拔) │
   └─────────┘        └─────────┘        └─────────┘
```

### 3.2 模块职责

| 模块 | 职责 | 核心实体 |
|------|------|----------|
| **项目模块** | 管理软著申请项目、客户信息、基础配置 | `Project`, `Customer` |
| **生成模块** | 调用 AI 生成源代码、手册、截图 | `GenerateTask`, `CodeGeneration`, `ManualGeneration` |
| **审核模块** | 提供编辑界面、审核流程、版本管理 | `ReviewSession`, `DocumentVersion` |
| **文件模块** | 文件上传下载、格式转换、存储管理 | `FileRecord`, `StorageClient` |
| **用户模块** | 用户认证、角色权限、操作日志 | `User`, `Role`, `AuditLog` |
| **基础设施层** | AI 服务抽象、对象存储客户端、文档生成工具 | `AiClient`, `StorageClient`, `DocumentGenerator` |

---

## 4. 数据模型

### 4.1 数据库表结构

```sql
-- 项目表
CREATE TABLE project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '软著名称',
    customer_name VARCHAR(100) COMMENT '客户名称',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/GENERATING/REVIEWING/APPROVED/EXPORTED',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 软件概要表
CREATE TABLE software_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL UNIQUE,
    version VARCHAR(20) COMMENT '版本号',
    category VARCHAR(50) COMMENT '软件分类',
    dev_hardware VARCHAR(200) COMMENT '开发硬件环境',
    run_hardware VARCHAR(200) COMMENT '运行硬件环境',
    dev_os VARCHAR(100) COMMENT '开发操作系统',
    dev_tools VARCHAR(200) COMMENT '开发工具',
    run_platform VARCHAR(100) COMMENT '运行平台',
    run_support VARCHAR(200) COMMENT '运行支撑环境',
    language VARCHAR(50) COMMENT '编程语言',
    code_lines INT COMMENT '源程序量',
    purpose TEXT COMMENT '开发目的',
    target_domain TEXT COMMENT '面向领域',
    main_functions TEXT COMMENT '主要功能',
    tech_features TEXT COMMENT '技术特点',
    FOREIGN KEY (project_id) REFERENCES project(id)
);

-- 生成任务表
CREATE TABLE generate_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    task_type VARCHAR(20) NOT NULL COMMENT 'CODE/MANUAL/SCREENSHOT',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    progress INT DEFAULT 0 COMMENT '进度 0-100',
    error_message TEXT,
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME NOT NULL,
    INDEX idx_project (project_id),
    FOREIGN KEY (project_id) REFERENCES project(id)
);

-- 文件记录表
CREATE TABLE file_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    file_type VARCHAR(30) NOT NULL COMMENT 'SOURCE_CODE/MANUAL/SCREENSHOT/EXPORT_ZIP',
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL COMMENT '对象存储路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    version INT DEFAULT 1,
    created_at DATETIME NOT NULL,
    INDEX idx_project_type (project_id, file_type),
    FOREIGN KEY (project_id) REFERENCES project(id)
);

-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF' COMMENT 'STAFF/ADMIN',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    last_login_at DATETIME
);

-- 审核记录表
CREATE TABLE review_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS/APPROVED/REJECTED',
    comments TEXT,
    approved_at DATETIME,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (project_id) REFERENCES project(id),
    FOREIGN KEY (reviewer_id) REFERENCES user(id)
);
```

---

## 5. 核心流程

### 5.1 创建项目流程

```
1. 员工填写表单
   ├─ 软件概要信息（名称、版本、分类、环境等）
   └─ 上传种子代码（ZIP 压缩包）

2. 系统处理
   ├─ 保存项目信息 + 软件概要
   ├─ 解压种子代码 → 存储到对象存储
   ├─ 代码分析器：统计行数、识别模块结构、检测编程语言
   ├─ AI 生成软件概要初稿（主要功能、面向领域、开发目的）
   └─ 更新软件概要 → 项目状态变为"待确认"

3. 员工确认/修改软件概要信息
```

### 5.2 生成材料流程

```
1. 员工点击"生成" → 创建生成任务

2. 异步任务队列执行
   ├─ 代码生成/扩展（30%）
   │   ├─ 读取种子代码
   │   ├─ AI 补全业务逻辑
   │   └─ 格式化为 60 页
   ├─ 操作手册生成（60%）
   │   ├─ AI 生成章节结构
   │   └─ AI 填充功能说明
   ├─ 截图生成（90%）
   │   ├─ AI 生成界面示意图
   │   └─ 插入到手册对应位置
   └─ 质量检查（100%）
       ├─ 字数检查（主要功能 800-1000 字）
       └─ 代码量检查

3. 更新项目状态 → "待审核"，通知员工
```

### 5.3 审核编辑流程

```
1. 员工打开审核台 → 查看生成结果

2. 进入编辑模式
   ├─ 软件概要编辑（表单）
   ├─ 源代码编辑（代码编辑器 Monaco）
   └─ 操作手册编辑（富文本编辑器 TipTap）

3. 功能支持
   ├─ 单字段重新生成（AI）
   ├─ 手动修改内容
   ├─ 保存版本（可回溯）
   └─ 预览效果（Word 预览）

4. 提交审核 → 管理员审核 → 审核通过 → 可导出
```

### 5.4 导出打包流程

```
1. 点击"导出"

2. 生成文件
   ├─ {名称}.txt（软件概要）
   ├─ {名称}+程序.docx（源代码）
   ├─ {名称}+程序.pdf（Word→PDF）
   ├─ {名称}+说明.docx（操作手册）
   └─ {名称}+说明.pdf（Word→PDF）

3. 打包成 {名称}.zip

4. 返回下载链接
```

---

## 6. API 设计

### 6.1 REST API

```yaml
# 项目相关
POST   /api/projects                    # 创建项目
GET    /api/projects                    # 项目列表（分页）
GET    /api/projects/{id}               # 项目详情
PUT    /api/projects/{id}               # 更新项目
DELETE /api/projects/{id}               # 删除项目

# 软件概要
GET    /api/projects/{id}/summary       # 获取软件概要
PUT    /api/projects/{id}/summary       # 更新软件概要
POST   /api/projects/{id}/summary/generate  # AI 重新生成概要

# 代码上传
POST   /api/projects/{id}/upload-code   # 上传种子代码

# 生成任务
POST   /api/projects/{id}/generate      # 开始生成
GET    /api/projects/{id}/tasks         # 生成任务列表
GET    /api/tasks/{taskId}/status       # 任务状态
POST   /api/tasks/{taskId}/cancel       # 取消任务

# 审核编辑
GET    /api/projects/{id}/code          # 获取生成的代码
PUT    /api/projects/{id}/code          # 保存编辑的代码
GET    /api/projects/{id}/manual        # 获取操作手册
PUT    /api/projects/{id}/manual        # 保存编辑的手册
POST   /api/projects/{id}/regenerate    # 重新生成某部分

# 导出
POST   /api/projects/{id}/export        # 导出 ZIP
GET    /api/projects/{id}/download/{fileName}  # 下载文件

# AI 辅助
POST   /api/ai/regenerate-field         # 重新生成单个字段
POST   /api/ai/expand-code              # 扩展代码

# 用户管理（管理员）
POST   /api/users                       # 创建用户
GET    /api/users                       # 用户列表
PUT    /api/users/{id}                  # 更新用户
DELETE /api/users/{id}                  # 删除用户

# 认证
POST   /api/auth/login                  # 登录
POST   /api/auth/logout                 # 登出
GET    /api/auth/me                     # 当前用户信息
```

---

## 7. 技术选型

| 层次 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **后端框架** | Spring Boot | 3.2+ | 主框架 |
| **构建工具** | Maven | 3.9+ | 项目构建 |
| **数据库** | MySQL | 8.0+ | 数据存储 |
| **ORM** | MyBatis-Plus | 3.5+ | 数据访问 |
| **缓存** | Redis | 7.0+ | 会话、任务队列 |
| **对象存储** | MinIO / 阿里云 OSS | - | 文件存储 |
| **文档生成** | Apache POI | 5.2+ | Word 文档生成 |
| **PDF 转换** | LibreOffice (headless) / iText | - | Word→PDF |
| **AI SDK** | Spring AI | 1.0+ | AI 模型集成 |
| **认证** | Spring Security + JWT | - | 用户认证 |
| **API 文档** | SpringDoc OpenAPI | 2.0+ | 接口文档 |
| **前端框架** | React | 18+ | 前端 |
| **前端构建** | Vite | 5+ | 前端构建工具 |
| **UI 组件** | Ant Design | 5+ | UI 组件库 |
| **状态管理** | Zustand | 4+ | 轻量状态管理 |
| **代码编辑器** | Monaco Editor | - | 代码编辑 |
| **富文本编辑** | TipTap | - | 手册编辑 |

---

## 8. 后端模块结构

```
com.company.ruanzhu
├── project/                    # 项目模块
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   └── enums/
│
├── generate/                   # 生成模块
│   ├── controller/
│   ├── service/
│   ├── analyzer/               # 代码分析器
│   ├── generator/              # 内容生成器
│   ├── checker/                # 质量检查器
│   └── ai/                     # AI 客户端（可插拔）
│
├── review/                     # 审核模块
│   ├── controller/
│   ├── service/
│   ├── model/
│   └── repository/
│
├── file/                       # 文件模块
│   ├── controller/
│   ├── service/
│   ├── generator/              # 文档生成器
│   ├── storage/                # 存储客户端
│   └── converter/              # 格式转换器
│
├── user/                       # 用户模块
│   ├── controller/
│   ├── service/
│   ├── model/
│   ├── repository/
│   └── security/
│
└── common/                     # 公共模块
    ├── config/
    ├── exception/
    ├── util/
    └── event/
```

---

## 9. 前端结构

```
src/
├── api/                    # API 调用
├── components/             # 公共组件
├── pages/                  # 页面
│   ├── Dashboard/          # 工作台/项目列表
│   ├── ProjectCreate/      # 创建项目
│   ├── ProjectDetail/      # 项目详情
│   ├── SummaryEdit/        # 软件概要编辑
│   ├── CodeReview/         # 代码审核编辑
│   ├── ManualReview/       # 手册审核编辑
│   └── Admin/              # 管理后台
├── stores/                 # 状态管理
├── hooks/                  # 自定义 Hooks
├── utils/                  # 工具函数
└── types/                  # TypeScript 类型
```

---

## 10. 部署架构

### 10.1 单机部署（开发/小规模）

```
┌───────────────────────────────────────────────────────────────┐
│                          Nginx                                 │
│                    (反向代理 + 静态资源)                         │
└─────────────────────────────┬─────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
    ┌─────────────────┐             ┌─────────────────┐
    │   React 前端     │             │  Spring Boot    │
    │  (静态文件部署)   │             │   后端服务       │
    └─────────────────┘             └────────┬────────┘
                                             │
              ┌──────────────────────────────┼──────────────────────────────┐
              ▼                              ▼                              ▼
    ┌─────────────────┐           ┌─────────────────┐           ┌─────────────────┐
    │     MySQL       │           │     Redis       │           │     MinIO       │
    └─────────────────┘           └─────────────────┘           └─────────────────┘
```

使用 Docker Compose 一键部署。

### 10.2 生产部署（可选扩展）

- 后端可水平扩展（多实例 + 负载均衡）
- 数据库主从复制
- MinIO 集群模式

---

## 11. 错误处理

### 11.1 异常分类

```
├── 业务异常 (BusinessException)
│   ├── ProjectNotFoundException
│   ├── GenerateTaskFailedException
│   ├── FileStorageException
│   └── AiServiceException
│
├── AI 调用异常
│   ├── 超时：重试 3 次，间隔递增
│   ├── 限流：降级到备用模型或提示稍后重试
│   └── 内容违规：记录日志，标记任务失败
│
├── 文件处理异常
│   ├── 上传失败：提示检查文件大小/格式
│   ├── 存储失败：重试，切换备用存储
│   └── 转换失败：保留 Word，提示手动转换
│
└── 前端错误提示
    ├── 表单验证：实时提示
    ├── 操作失败：Toast 提示
    └── 网络异常：重试按钮
```

---

## 12. 测试策略

| 层次 | 工具 | 覆盖率目标 |
|------|------|----------|
| **单元测试** | JUnit 5 | >70% |
| **集成测试** | TestContainers | 核心流程 100% |
| **API 测试** | MockMvc / RestAssured | 主要接口 |
| **前端测试** | Jest + React Testing Library | 核心组件 |

---

## 13. 非功能性需求

| 需求 | 目标 | 实现方式 |
|------|------|----------|
| **性能** | 单项目生成 < 5 分钟 | 异步任务 + 进度反馈 |
| **并发** | 支持 10 个并发任务 | 线程池 + 任务队列 |
| **可用性** | 99% 可用 | 健康检查 + 自动重启 |
| **安全性** | 数据隔离 + 权限控制 | JWT + 角色权限 |
| **可维护性** | 模块化 + 文档完整 | 清晰分层 + API 文档 |
| **可扩展性** | AI 模型可插拔 | 策略模式 + 工厂模式 |

---

## 附录 A：AI 生成内容质量要求

| 字段 | 字数/要求 |
|------|----------|
| 主要功能 | 800-1000 字，描述功能作用、关系、衔接流程 |
| 面向领域 | 200-300 字，行业背景、应用场景、目标用户 |
| 开发目的 | 300-500 字，核心关注点、业务价值、技术目标 |
| 技术特点 | 150-200 字，架构选型、关键技术、创新点 |

---

**文档结束**
