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
    FILE_UPLOAD_ERROR(1012, "文件上传失败"),
    FILE_INVALID_TYPE(1013, "文件类型不正确，仅支持ZIP格式"),
    SEED_CODE_NOT_FOUND(1014, "未找到种子代码，请先上传"),
    CODE_ANALYSIS_ERROR(1015, "代码分析失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
