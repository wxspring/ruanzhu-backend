-- src/main/resources/db/schema.sql
CREATE DATABASE IF NOT EXISTS ruanzhu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ruanzhu;

-- 项目表
CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL COMMENT '软著名称',
    customer_name VARCHAR(100) COMMENT '客户名称',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 软件概要表
CREATE TABLE IF NOT EXISTS software_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
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
    tech_feature_options VARCHAR(200) COMMENT '技术特点选项',
    system_overview TEXT COMMENT '系统概述',
    functional_features MEDIUMTEXT COMMENT '功能特点（16行完整文本）',
    function_menu MEDIUMTEXT COMMENT '功能菜单（一行一个菜单名）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='软件概要表';

-- 生成任务表
CREATE TABLE IF NOT EXISTS generate_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    task_type VARCHAR(20) NOT NULL COMMENT '任务类型',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    progress INT NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    error_message TEXT COMMENT '错误信息',
    result_path VARCHAR(500) COMMENT '结果文件路径',
    started_at DATETIME DEFAULT NULL,
    completed_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成任务表';

-- 文件记录表
CREATE TABLE IF NOT EXISTS file_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    file_type VARCHAR(30) NOT NULL COMMENT '文件类型',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    storage_path VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_type (project_id, file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF' COMMENT '角色',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME DEFAULT NULL,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 审核记录表
CREATE TABLE IF NOT EXISTS review_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    reviewer_id BIGINT NOT NULL COMMENT '审核人ID',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态',
    comments TEXT COMMENT '审核意见',
    approved_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_reviewer_id (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录表';
