-- 测试账号数据

-- 管理员账号 (密码: test123456)
INSERT INTO user (username, password_hash, real_name, role, status) VALUES
('admin', '$2a$10$2wF.oTO56lr4GzwXNECdXeOkA1/8P1oRnpaN2RzIPv5VNbCHgGB6q', '系统管理员', 'ADMIN', 'ACTIVE');

-- 普通员工账号 (密码都是: test123456)
INSERT INTO user (username, password_hash, real_name, role, status) VALUES
('zhangsan', '$2a$10$2wF.oTO56lr4GzwXNECdXeOkA1/8P1oRnpaN2RzIPv5VNbCHgGB6q', '张三', 'STAFF', 'ACTIVE'),
('lisi', '$2a$10$2wF.oTO56lr4GzwXNECdXeOkA1/8P1oRnpaN2RzIPv5VNbCHgGB6q', '李四', 'STAFF', 'ACTIVE'),
('wangwu', '$2a$10$2wF.oTO56lr4GzwXNECdXeOkA1/8P1oRnpaN2RzIPv5VNbCHgGB6q', '王五', 'STAFF', 'ACTIVE');
