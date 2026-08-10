# 软著材料 AI 生产系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an internal platform that uses AI to automatically generate software copyright (软著) application materials, including source code documents, operation manuals, and software summaries.

**Architecture:** Modular monolith with Spring Boot backend, React frontend, MySQL database, MinIO file storage, and pluggable AI service integration. Async task processing for AI-driven content generation.

**Tech Stack:** Spring Boot 3.2+, MyBatis-Plus, MySQL 8.0, Redis 7.0, MinIO, Apache POI, Spring AI, React 18, Ant Design 5, Monaco Editor, TipTap, Vite

## Global Constraints

- Java 17+ required
- Spring Boot 3.2+ 
- MySQL 8.0+
- Node.js 18+ for frontend
- All AI calls must be pluggable (interface-based)
- Code documents: 60 pages, 50+ lines per page
- Main functions description: 800-1000 characters
- Target domain: 200-300 characters
- Development purpose: 300-500 characters
- Export format: ZIP containing 5 files (.txt, 2x .docx, 2x .pdf)

---

## Phase 1: Backend Foundation & Project Management

### Task 1: Initialize Spring Boot Project

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/company/ruanzhu/RuanzhuApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`

**Interfaces:**
- Consumes: None (project root)
- Produces: Working Spring Boot application with basic configuration

- [ ] **Step 1: Create Maven pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>
    
    <groupId>com.company</groupId>
    <artifactId>ruanzhu</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>ruanzhu</name>
    <description>软著材料 AI 生产系统</description>
    
    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.6</mybatis-plus.version>
        <minio.version>8.5.9</minio.version>
        <poi.version>5.2.5</poi.version>
        <spring-ai.version>1.0.0-M6</spring-ai.version>
        <jjwt.version>0.12.5</jjwt.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- MinIO -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>${minio.version}</version>
        </dependency>
        
        <!-- Document Generation -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>${poi.version}</version>
        </dependency>
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.26</version>
        </dependency>
        
        <!-- API Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.5.0</version>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create main application class**

```java
// src/main/java/com/company/ruanzhu/RuanzhuApplication.java
package com.company.ruanzhu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RuanzhuApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuanzhuApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application configuration**

```yaml
# src/main/resources/application.yml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  application:
    name: ruanzhu
  profiles:
    active: dev
  
  # Jackson
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    default-property-inclusion: non_null
  
  # Servlet
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

# MyBatis Plus
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.company.ruanzhu.**.model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# MinIO
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: ruanzhu

# JWT
jwt:
  secret: ruanzhu-secret-key-must-be-at-least-256-bits-long-for-hs256
  expiration: 86400000  # 24 hours

# Async Task
task:
  execution:
    pool:
      core-size: 5
      max-size: 10
      queue-capacity: 100
    thread-name-prefix: ruanzhu-async-

# Logging
logging:
  level:
    com.company.ruanzhu: DEBUG
    org.springframework.security: INFO
```

- [ ] **Step 4: Create dev profile configuration**

```yaml
# src/main/resources/application-dev.yml
spring:
  # DataSource
  datasource:
    url: jdbc:mysql://localhost:3306/ruanzhu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # Redis
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0

# Logging
logging:
  level:
    root: INFO
```

- [ ] **Step 5: Create directory structure**

```bash
mkdir -p src/main/java/com/company/ruanzhu/{project,generate,review,file,user,common}/{controller,service,repository,model,enums}
mkdir -p src/main/java/com/company/ruanzhu/common/{config,exception,util,event}
mkdir -p src/main/java/com/company/ruanzhu/generate/{analyzer,generator,checker,ai}
mkdir -p src/main/java/com/company/ruanzhu/file/{generator,storage,converter}
mkdir -p src/main/java/com/company/ruanzhu/user/security
mkdir -p src/main/resources/mapper
mkdir -p src/test/java/com/company/ruanzhu
```

- [ ] **Step 6: Run application to verify**

```bash
mvn spring-boot:run
```

Expected: Application starts on port 8080 (will fail to connect to DB/Redis, but structure is valid)

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: initialize Spring Boot project structure"
```

---

### Task 2: Database Schema & Entity Models

**Files:**
- Create: `src/main/resources/db/schema.sql`
- Create: `src/main/java/com/company/ruanzhu/project/model/Project.java`
- Create: `src/main/java/com/company/ruanzhu/project/model/SoftwareSummary.java`
- Create: `src/main/java/com/company/ruanzhu/project/enums/ProjectStatus.java`
- Create: `src/main/java/com/company/ruanzhu/generate/model/GenerateTask.java`
- Create: `src/main/java/com/company/ruanzhu/generate/enums/TaskType.java`
- Create: `src/main/java/com/company/ruanzhu/generate/enums/TaskStatus.java`
- Create: `src/main/java/com/company/ruanzhu/file/model/FileRecord.java`
- Create: `src/main/java/com/company/ruanzhu/user/model/User.java`
- Create: `src/main/java/com/company/ruanzhu/user/enums/UserRole.java`
- Create: `src/main/java/com/company/ruanzhu/user/enums/UserStatus.java`
- Create: `src/main/java/com/company/ruanzhu/review/model/ReviewSession.java`
- Create: `src/main/java/com/company/ruanzhu/review/enums/ReviewStatus.java`

**Interfaces:**
- Consumes: Database connection
- Produces: Entity classes for all modules

- [ ] **Step 1: Create database schema SQL**

```sql
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
```

- [ ] **Step 2: Create base entity class**

```java
// src/main/java/com/company/ruanzhu/common/model/BaseEntity.java
package com.company.ruanzhu.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}
```

- [ ] **Step 3: Create Project entity**

```java
// src/main/java/com/company/ruanzhu/project/model/Project.java
package com.company.ruanzhu.project.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.ruanzhu.common.model.BaseEntity;
import com.company.ruanzhu.project.enums.ProjectStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends BaseEntity {
    private String name;
    private String customerName;
    private ProjectStatus status;
    private Long createdBy;
}
```

- [ ] **Step 4: Create ProjectStatus enum**

```java
// src/main/java/com/company/ruanzhu/project/enums/ProjectStatus.java
package com.company.ruanzhu.project.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ProjectStatus {
    CREATED("CREATED", "已创建"),
    SUMMARY_CONFIRMING("SUMMARY_CONFIRMING", "概要确认中"),
    GENERATING("GENERATING", "生成中"),
    REVIEWING("REVIEWING", "待审核"),
    APPROVED("APPROVED", "已审核"),
    EXPORTED("EXPORTED", "已导出");
    
    @EnumValue
    private final String code;
    private final String desc;
    
    ProjectStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

- [ ] **Step 5: Create SoftwareSummary entity**

```java
// src/main/java/com/company/ruanzhu/project/model/SoftwareSummary.java
package com.company.ruanzhu.project.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("software_summary")
public class SoftwareSummary {
    private Long id;
    private Long projectId;
    private String version;
    private String category;
    private String devHardware;
    private String runHardware;
    private String devOs;
    private String devTools;
    private String runPlatform;
    private String runSupport;
    private String language;
    private Integer codeLines;
    private String purpose;
    private String targetDomain;
    private String mainFunctions;
    private String techFeatures;
    private String techFeatureOptions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 6: Create remaining entities**

```java
// src/main/java/com/company/ruanzhu/generate/model/GenerateTask.java
package com.company.ruanzhu.generate.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.ruanzhu.generate.enums.TaskStatus;
import com.company.ruanzhu.generate.enums.TaskType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("generate_task")
public class GenerateTask {
    private Long id;
    private Long projectId;
    private TaskType taskType;
    private TaskStatus status;
    private Integer progress;
    private String errorMessage;
    private String resultPath;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
```

```java
// src/main/java/com/company/ruanzhu/generate/enums/TaskType.java
package com.company.ruanzhu.generate.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TaskType {
    CODE("CODE", "代码生成"),
    MANUAL("MANUAL", "手册生成"),
    SCREENSHOT("SCREENSHOT", "截图生成");
    
    @EnumValue
    private final String code;
    private final String desc;
    
    TaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

```java
// src/main/java/com/company/ruanzhu/generate/enums/TaskStatus.java
package com.company.ruanzhu.generate.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING("PENDING", "待处理"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");
    
    @EnumValue
    private final String code;
    private final String desc;
    
    TaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

```java
// src/main/java/com/company/ruanzhu/file/model/FileRecord.java
package com.company.ruanzhu.file.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_record")
public class FileRecord {
    private Long id;
    private Long projectId;
    private String fileType;
    private String fileName;
    private String storagePath;
    private Long fileSize;
    private Integer version;
    private LocalDateTime createdAt;
}
```

```java
// src/main/java/com/company/ruanzhu/user/model/User.java
package com.company.ruanzhu.user.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
```

```java
// src/main/java/com/company/ruanzhu/user/enums/UserRole.java
package com.company.ruanzhu.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserRole {
    STAFF("STAFF", "普通员工"),
    ADMIN("ADMIN", "管理员");
    
    @EnumValue
    private final String code;
    private final String desc;
    
    UserRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

```java
// src/main/java/com/company/ruanzhu/user/enums/UserStatus.java
package com.company.ruanzhu.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("ACTIVE", "正常"),
    DISABLED("DISABLED", "禁用");
    
    @EnumValue
    private final String code;
    private final String desc;
    
    UserStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

```java
// src/main/java/com/company/ruanzhu/review/model/ReviewSession.java
package com.company.ruanzhu.review.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.ruanzhu.review.enums.ReviewStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_session")
public class ReviewSession {
    private Long id;
    private Long projectId;
    private Long reviewerId;
    private ReviewStatus status;
    private String comments;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
```

```java
// src/main/java/com/company/ruanzhu/review/enums/ReviewStatus.java
package com.company.ruanzhu.review.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ReviewStatus {
    IN_PROGRESS("IN_PROGRESS", "审核中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝");
    
    @EnumValue
    private final String code;
    private final String desc;
    
    ReviewStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

- [ ] **Step 7: Create MyBatis Plus configuration**

```java
// src/main/java/com/company/ruanzhu/common/config/MybatisPlusConfig.java
package com.company.ruanzhu.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
            
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
```

- [ ] **Step 8: Run database migration**

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

- [ ] **Step 9: Commit**

```bash
git add .
git commit -m "feat: add database schema and entity models"
```

---

### Task 3: Common Infrastructure (Exceptions, Response, Utils)

**Files:**
- Create: `src/main/java/com/company/ruanzhu/common/exception/BusinessException.java`
- Create: `src/main/java/com/company/ruanzhu/common/exception/GlobalExceptionHandler.java`
- Create: `src/main/java/com/company/ruanzhu/common/model/Result.java`
- Create: `src/main/java/com/company/ruanzhu/common/util/PageRequest.java`
- Create: `src/main/java/com/company/ruanzhu/common/util/PageResult.java`

**Interfaces:**
- Consumes: None
- Produces: Common infrastructure used by all modules

- [ ] **Step 1: Create business exception**

```java
// src/main/java/com/company/ruanzhu/common/exception/BusinessException.java
package com.company.ruanzhu.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
```

```java
// src/main/java/com/company/ruanzhu/common/exception/ErrorCode.java
package com.company.ruanzhu.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),
    
    // 业务错误码 1000+
    PROJECT_NOT_FOUND(1001, "项目不存在"),
    PROJECT_STATUS_ERROR(1002, "项目状态错误"),
    SUMMARY_NOT_FOUND(1003, "软件概要不存在"),
    FILE_NOT_FOUND(1004, "文件不存在"),
    TASK_NOT_FOUND(1005, "任务不存在"),
    TASK_ALREADY_RUNNING(1006, "任务已在运行"),
    USER_NOT_FOUND(1007, "用户不存在"),
    USER_DISABLED(1008, "用户已禁用"),
    USERNAME_EXISTS(1009, "用户名已存在"),
    PASSWORD_ERROR(1010, "密码错误"),
    AI_SERVICE_ERROR(1011, "AI服务调用失败"),
    FILE_UPLOAD_ERROR(1012, "文件上传失败");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: Create global exception handler**

```java
// src/main/java/com/company/ruanzhu/common/exception/GlobalExceptionHandler.java
package com.company.ruanzhu.common.exception;

import com.company.ruanzhu.common.model.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.error(400, message);
    }
    
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数绑定失败");
        return Result.error(400, message);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        return Result.error(400, e.getMessage());
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return Result.error(400, "文件大小超出限制");
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("Unexpected error", e);
        return Result.error(500, "系统内部错误");
    }
}
```

- [ ] **Step 3: Create Result wrapper**

```java
// src/main/java/com/company/ruanzhu/common/model/Result.java
package com.company.ruanzhu.common.model;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    public static <T> Result<T> success() {
        return success(null);
    }
    
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
    
    public static <T> Result<T> error(String message) {
        return error(400, message);
    }
}
```

- [ ] **Step 4: Create pagination utilities**

```java
// src/main/java/com/company/ruanzhu/common/util/PageRequest.java
package com.company.ruanzhu.common.util;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageRequest {
    @Min(value = 1, message = "页码最小为1")
    private int page = 1;
    
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private int size = 10;
    
    private String sortBy;
    private boolean ascending = false;
    
    public long getOffset() {
        return (long) (page - 1) * size;
    }
}
```

```java
// src/main/java/com/company/ruanzhu/common/util/PageResult.java
package com.company.ruanzhu.common.util;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int size;
    private int totalPages;
    
    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setTotalPages((int) Math.ceil((double) total / size));
        return result;
    }
}
```

- [ ] **Step 5: Write unit test for Result**

```java
// src/test/java/com/company/ruanzhu/common/model/ResultTest.java
package com.company.ruanzhu.common.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {
    
    @Test
    void testSuccess() {
        Result<String> result = Result.success("test");
        assertEquals(0, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("test", result.getData());
    }
    
    @Test
    void testError() {
        Result<?> result = Result.error(400, "bad request");
        assertEquals(400, result.getCode());
        assertEquals("bad request", result.getMessage());
        assertNull(result.getData());
    }
}
```

- [ ] **Step 6: Run tests**

```bash
mvn test -Dtest=ResultTest
```

Expected: Tests pass

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: add common infrastructure (exceptions, result wrapper, pagination)"
```

---

### Task 4: User Module - Repository & Service

**Files:**
- Create: `src/main/java/com/company/ruanzhu/user/repository/UserRepository.java`
- Create: `src/main/java/com/company/ruanzhu/user/service/UserService.java`
- Create: `src/main/java/com/company/ruanzhu/user/service/impl/UserServiceImpl.java`
- Create: `src/main/java/com/company/ruanzhu/user/model/dto/UserCreateRequest.java`
- Create: `src/main/java/com/company/ruanzhu/user/model/dto/UserUpdateRequest.java`
- Create: `src/main/java/com/company/ruanzhu/user/model/vo/UserVO.java`

**Interfaces:**
- Consumes: User entity
- Produces: UserService for authentication and user management

- [ ] **Step 1: Create UserRepository**

```java
// src/main/java/com/company/ruanzhu/user/repository/UserRepository.java
package com.company.ruanzhu.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.ruanzhu.user.model.User;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper
public interface UserRepository extends BaseMapper<User> {
    default Optional<User> findByUsername(String username) {
        return Optional.ofNullable(selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        ));
    }
    
    default boolean existsByUsername(String username) {
        return selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        ) > 0;
    }
}
```

- [ ] **Step 2: Create User DTOs**

```java
// src/main/java/com/company/ruanzhu/user/model/dto/UserCreateRequest.java
package com.company.ruanzhu.user.model.dto;

import com.company.ruanzhu.user.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100")
    private String password;
    
    @Size(max = 50, message = "真实姓名最长50")
    private String realName;
    
    private UserRole role = UserRole.STAFF;
}
```

```java
// src/main/java/com/company/ruanzhu/user/model/dto/UserUpdateRequest.java
package com.company.ruanzhu.user.model.dto;

import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.enums.UserStatus;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String realName;
    private UserRole role;
    private UserStatus status;
}
```

```java
// src/main/java/com/company/ruanzhu/user/model/vo/UserVO.java
package com.company.ruanzhu.user.model.vo;

import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
```

- [ ] **Step 3: Create UserService interface**

```java
// src/main/java/com/company/ruanzhu/user/service/UserService.java
package com.company.ruanzhu.user.service;

import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.user.model.dto.UserCreateRequest;
import com.company.ruanzhu.user.model.dto.UserUpdateRequest;
import com.company.ruanzhu.user.model.vo.UserVO;

public interface UserService {
    UserVO createUser(UserCreateRequest request);
    UserVO updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
    UserVO getUserById(Long id);
    UserVO getUserByUsername(String username);
    PageResult<UserVO> listUsers(PageRequest pageRequest);
}
```

- [ ] **Step 4: Create UserService implementation**

```java
// src/main/java/com/company/ruanzhu/user/service/impl/UserServiceImpl.java
package com.company.ruanzhu.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.user.model.User;
import com.company.ruanzhu.user.model.dto.UserCreateRequest;
import com.company.ruanzhu.user.model.dto.UserUpdateRequest;
import com.company.ruanzhu.user.model.vo.UserVO;
import com.company.ruanzhu.user.repository.UserRepository;
import com.company.ruanzhu.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public UserVO createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        
        userRepository.insert(user);
        return toVO(user);
    }
    
    @Override
    @Transactional
    public UserVO updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        
        userRepository.updateById(user);
        return toVO(user);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(id);
    }
    
    @Override
    public UserVO getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }
    
    @Override
    public UserVO getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    
    @Override
    public PageResult<UserVO> listUsers(PageRequest pageRequest) {
        Page<User> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (pageRequest.getSortBy() != null) {
            // Add sorting if needed
        }
        wrapper.orderByDesc(User::getCreatedAt);
        
        Page<User> result = userRepository.selectPage(page, wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(),
                pageRequest.getPage(),
                pageRequest.getSize()
        );
    }
    
    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(user.getLastLoginAt());
        return vo;
    }
}
```

- [ ] **Step 5: Write unit tests**

```java
// src/test/java/com/company/ruanzhu/user/service/impl/UserServiceImplTest.java
package com.company.ruanzhu.user.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.model.User;
import com.company.ruanzhu.user.model.dto.UserCreateRequest;
import com.company.ruanzhu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @BeforeEach
    void setUp() {
        // Setup if needed
    }
    
    @Test
    void createUser_Success() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setRealName("Test User");
        request.setRole(UserRole.STAFF);
        
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.insert(any(User.class))).thenReturn(1);
        
        var result = userService.createUser(request);
        
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).insert(any(User.class));
    }
    
    @Test
    void createUser_UsernameExists_ThrowsException() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("existing");
        request.setPassword("password123");
        
        when(userRepository.existsByUsername("existing")).thenReturn(true);
        
        assertThrows(BusinessException.class, () -> userService.createUser(request));
    }
}
```

- [ ] **Step 6: Run tests**

```bash
mvn test -Dtest=UserServiceImplTest
```

Expected: Tests pass

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat: implement user repository and service"
```

---

### Task 5: Security Configuration & JWT Authentication

**Files:**
- Create: `src/main/java/com/company/ruanzhu/user/security/JwtTokenProvider.java`
- Create: `src/main/java/com/company/ruanzhu/user/security/JwtAuthenticationFilter.java`
- Create: `src/main/java/com/company/ruanzhu/user/security/SecurityConfig.java`
- Create: `src/main/java/com/company/ruanzhu/user/security/UserPrincipal.java`
- Create: `src/main/java/com/company/ruanzhu/user/model/dto/LoginRequest.java`
- Create: `src/main/java/com/company/ruanzhu/user/model/vo/LoginVO.java`
- Create: `src/main/java/com/company/ruanzhu/user/controller/AuthController.java`

**Interfaces:**
- Consumes: UserService, User entity
- Produces: JWT authentication, secured endpoints

- [ ] **Step 1: Create JwtTokenProvider**

```java
// src/main/java/com/company/ruanzhu/user/security/JwtTokenProvider.java
package com.company.ruanzhu.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    
    private final SecretKey key;
    private final long expiration;
    
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }
    
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }
    
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }
    
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }
    
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- [ ] **Step 2: Create UserPrincipal**

```java
// src/main/java/com/company/ruanzhu/user/security/UserPrincipal.java
package com.company.ruanzhu.user.security;

import com.company.ruanzhu.user.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {
    
    private Long id;
    private String username;
    private UserRole role;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getPassword() {
        return null;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

- [ ] **Step 3: Create JwtAuthenticationFilter**

```java
// src/main/java/com/company/ruanzhu/user/security/JwtAuthenticationFilter.java
package com.company.ruanzhu.user.security;

import com.company.ruanzhu.user.enums.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider tokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);
        
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            Long userId = tokenProvider.getUserIdFromToken(token);
            String username = tokenProvider.getUsernameFromToken(token);
            String role = tokenProvider.getRoleFromToken(token);
            
            UserPrincipal principal = new UserPrincipal(userId, username, UserRole.valueOf(role));
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 4: Create SecurityConfig**

```java
// src/main/java/com/company/ruanzhu/user/security/SecurityConfig.java
package com.company.ruanzhu.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 5: Create Login DTOs**

```java
// src/main/java/com/company/ruanzhu/user/model/dto/LoginRequest.java
package com.company.ruanzhu.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

```java
// src/main/java/com/company/ruanzhu/user/model/vo/LoginVO.java
package com.company.ruanzhu.user.model.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserVO user;
}
```

- [ ] **Step 6: Create AuthController**

```java
// src/main/java/com/company/ruanzhu/user/controller/AuthController.java
package com.company.ruanzhu.user.controller;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.user.model.User;
import com.company.ruanzhu.user.model.dto.LoginRequest;
import com.company.ruanzhu.user.model.vo.LoginVO;
import com.company.ruanzhu.user.model.vo.UserVO;
import com.company.ruanzhu.user.repository.UserRepository;
import com.company.ruanzhu.user.security.JwtTokenProvider;
import com.company.ruanzhu.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setRealName(user.getRealName());
        userVO.setRole(user.getRole());
        userVO.setStatus(user.getStatus());
        vo.setUser(userVO);
        
        return Result.success(vo);
    }
    
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.selectById(principal.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(user.getLastLoginAt());
        
        return Result.success(vo);
    }
}
```

- [ ] **Step 7: Write test for JwtTokenProvider**

```java
// src/test/java/com/company/ruanzhu/user/security/JwtTokenProviderTest.java
package com.company.ruanzhu.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {
    
    private JwtTokenProvider tokenProvider;
    
    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
            "ruanzhu-secret-key-must-be-at-least-256-bits-long-for-hs256",
            86400000L
        );
    }
    
    @Test
    void generateAndValidateToken() {
        String token = tokenProvider.generateToken(1L, "testuser", "STAFF");
        
        assertTrue(tokenProvider.validateToken(token));
        assertEquals("testuser", tokenProvider.getUsernameFromToken(token));
        assertEquals(1L, tokenProvider.getUserIdFromToken(token));
        assertEquals("STAFF", tokenProvider.getRoleFromToken(token));
    }
    
    @Test
    void invalidToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid-token"));
        assertFalse(tokenProvider.validateToken(null));
        assertFalse(tokenProvider.validateToken(""));
    }
}
```

- [ ] **Step 8: Run tests**

```bash
mvn test -Dtest=JwtTokenProviderTest
```

Expected: Tests pass

- [ ] **Step 9: Commit**

```bash
git add .
git commit -m "feat: implement JWT authentication and security configuration"
```

---

### Task 6: User Management Controller

**Files:**
- Create: `src/main/java/com/company/ruanzhu/user/controller/UserController.java`

**Interfaces:**
- Consumes: UserService
- Produces: REST API for user management (admin only)

- [ ] **Step 1: Create UserController**

```java
// src/main/java/com/company/ruanzhu/user/controller/UserController.java
package com.company.ruanzhu.user.controller;

import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.user.model.dto.UserCreateRequest;
import com.company.ruanzhu.user.model.dto.UserUpdateRequest;
import com.company.ruanzhu.user.model.vo.UserVO;
import com.company.ruanzhu.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }
    
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.success(userService.updateUser(id, request));
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
    
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }
    
    @GetMapping
    public Result<PageResult<UserVO>> listUsers(@Valid PageRequest pageRequest) {
        return Result.success(userService.listUsers(pageRequest));
    }
}
```

- [ ] **Step 2: Start application and test**

```bash
mvn spring-boot:run
```

Test with curl:
```bash
# Create admin user first (direct SQL)
# INSERT INTO user (username, password_hash, real_name, role, status) VALUES ('admin', '$2a$10$...', 'Admin', 'ADMIN', 'ACTIVE');

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Get current user
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer {token}"
```

Expected: Login returns token, /me returns user info

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add user management controller"
```

---

## Phase 1 Complete - Checkpoint

At this point you have:
- Working Spring Boot application
- Database schema created
- User authentication (JWT)
- User management CRUD
- Common infrastructure (exceptions, result wrapper)

**Test the checkpoint:**
```bash
mvn clean test
mvn spring-boot:run
```

You should be able to:
- Login via /api/auth/login
- Get current user via /api/auth/me
- Manage users via /api/users (as admin)

---

## Remaining Phases (Summary)

Due to length, the following phases are summarized. Each phase follows the same pattern: detailed tasks with code, tests, and commits.

### Phase 2: Project Management Module
- Task 7: Project repository and service
- Task 8: SoftwareSummary repository and service
- Task 9: Project controller with CRUD APIs
- Task 10: File upload for seed code (ZIP)
- Task 11: Code analyzer (count lines, detect language)

### Phase 3: File Storage (MinIO)
- Task 12: MinIO configuration and client
- Task 13: File storage service (upload, download, delete)
- Task 14: File record management

### Phase 4: AI Integration
- Task 15: AI client interface and factory
- Task 16: AI content generation service
- Task 17: Summary field generation (purpose, domain, functions)
- Task 18: Code expansion service

### Phase 5: Document Generation
- Task 19: Word document generator (Apache POI)
- Task 20: Source code document formatting (60 pages)
- Task 21: Manual document generation
- Task 22: Summary TXT generation
- Task 23: Word to PDF conversion
- Task 24: ZIP packaging

### Phase 6: Async Task Processing
- Task 25: Task service and repository
- Task 26: Async generation task executor
- Task 27: Task status tracking and progress

### Phase 7: Review Module
- Task 28: Review session service
- Task 29: Version management for documents
- Task 30: Review controller

### Phase 8: Frontend - Setup & Auth
- Task 31: React + Vite project setup
- Task 32: Ant Design integration
- Task 33: Auth store and login page
- Task 34: Route protection

### Phase 9: Frontend - Project Management
- Task 35: Dashboard / project list page
- Task 36: Create project page (form + upload)
- Task 37: Project detail page

### Phase 10: Frontend - Editing
- Task 38: Summary edit page
- Task 39: Code review page (Monaco Editor)
- Task 40: Manual review page (TipTap)

### Phase 11: Frontend - Export & Admin
- Task 41: Export functionality
- Task 42: Admin user management page

### Phase 12: Deployment
- Task 43: Dockerfile for backend
- Task 44: Dockerfile for frontend
- Task 45: Docker Compose configuration
- Task 46: Deployment documentation

---

**Plan complete and saved to `docs/superpowers/plans/2026-08-10-ruanzhu-system.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
