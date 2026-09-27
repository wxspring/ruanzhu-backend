package com.company.ruanzhu.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 应用启动后自动执行的数据库 Schema 迁移。
 * 用于补充历史数据库中缺失的列（相比 DDL 写在 schema.sql，这里可在已创建的表上安全 ALTER）。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureSoftwareSummaryColumn("system_overview",
                    "ALTER TABLE software_summary ADD COLUMN system_overview TEXT COMMENT '系统概述'");
            ensureSoftwareSummaryColumn("functional_features",
                    "ALTER TABLE software_summary ADD COLUMN functional_features MEDIUMTEXT COMMENT '功能特点（16行完整文本）'");
            ensureSoftwareSummaryColumn("function_menu",
                    "ALTER TABLE software_summary ADD COLUMN function_menu MEDIUMTEXT COMMENT '功能菜单（一行一个菜单名）'");
        } catch (Exception e) {
            log.warn("Schema migration skipped (expected if table not created yet): {}", e.getMessage());
        }
    }

    private void ensureSoftwareSummaryColumn(String columnName, String alterSql) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'software_summary' AND COLUMN_NAME = ?",
                    columnName);
            if (!rows.isEmpty()) {
                log.info("SchemaMigration: software_summary.{} already exists, skip", columnName);
                return;
            }
            jdbcTemplate.execute(alterSql);
            log.info("SchemaMigration: executed -> {}", alterSql);
        } catch (Exception e) {
            log.warn("SchemaMigration: failed to ensure column software_summary.{}: {}", columnName, e.getMessage());
            throw e;
        }
    }
}
