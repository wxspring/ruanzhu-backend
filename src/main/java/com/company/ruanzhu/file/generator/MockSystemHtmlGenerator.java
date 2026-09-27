package com.company.ruanzhu.file.generator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据项目功能菜单生成模拟系统界面的 HTML 页面，
 * 供浏览器截图后嵌入操作手册。
 *
 * 生成的页面：
 * 1. 登录页
 * 2. 首页/工作台
 * 3. 每个一级模块的列表页（表格）
 * 4. 每个一级模块的表单页（录入）
 */
public class MockSystemHtmlGenerator {

    private final String softwareName;
    private final String functionMenu;
    private final File outputDir;

    // 一级模块 -> 该模块下的功能列表
    private final Map<String, List<String>> modules = new LinkedHashMap<>();

    public MockSystemHtmlGenerator(String softwareName, String functionMenu, File outputDir) {
        this.softwareName = softwareName;
        this.functionMenu = functionMenu == null ? "" : functionMenu;
        this.outputDir = outputDir;
        if (!outputDir.exists()) outputDir.mkdirs();
        parseMenu();
    }

    /** 解析功能菜单：每行格式 "功能名：模块名" 或 "模块名/功能名" */
    private void parseMenu() {
        String[] lines = functionMenu.split("\\r?\\n");
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            // 支持 中文冒号、英文冒号、斜杠 分隔
            String[] parts = line.split("[:：/\\\\]", 2);
            String func, module;
            if (parts.length == 2) {
                func = parts[0].trim();
                module = parts[1].trim();
            } else {
                func = line;
                module = "系统功能";
            }
            if (func.isEmpty()) continue;
            modules.computeIfAbsent(module, k -> new ArrayList<>()).add(func);
        }
        if (modules.isEmpty()) {
            modules.put("系统功能", List.of("数据管理", "查询统计", "系统设置"));
        }
    }

    public Map<String, List<String>> getModules() {
        return modules;
    }

    /** 生成全部 HTML 页面，返回 文件名->绝对路径 */
    public Map<String, File> generateAll() throws IOException {
        Map<String, File> files = new LinkedHashMap<>();
        files.put("login", write("login.html", loginHtml()));
        files.put("home", write("home.html", homeHtml()));

        int idx = 1;
        for (Map.Entry<String, List<String>> e : modules.entrySet()) {
            String mod = e.getKey();
            files.put("list_" + idx, write("list_" + idx + ".html", listHtml(mod, e.getValue())));
            files.put("form_" + idx, write("form_" + idx + ".html", formHtml(mod, e.getValue())));
            idx++;
        }
        return files;
    }

    private File write(String name, String content) throws IOException {
        File f = new File(outputDir, name);
        Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
        return f;
    }

    // ===================== HTML 模板 =====================

    private String head() {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN"><head><meta charset="UTF-8">
                <style>
                * { margin:0; padding:0; box-sizing:border-box; font-family:"Microsoft YaHei","PingFang SC",Arial,sans-serif; }
                body { background:#f0f2f5; color:#333; }
                /* 顶部栏 */
                .header { height:56px; background:#001529; color:#fff; display:flex; align-items:center;
                          padding:0 24px; justify-content:space-between; }
                .header .logo { font-size:18px; font-weight:bold; }
                .header .user { font-size:14px; opacity:.85; }
                /* 主体布局 */
                .layout { display:flex; height:calc(100vh - 56px); }
                .sidebar { width:220px; background:#001529; color:#fff; padding:12px 0; overflow-y:auto; }
                .sidebar .menu-item { padding:12px 24px; font-size:14px; cursor:pointer; border-left:3px solid transparent; }
                .sidebar .menu-item.active { background:#1890ff; border-left-color:#fff; }
                .sidebar .menu-group { padding:8px 24px; font-size:12px; color:#8c8c8c; margin-top:8px; }
                .content { flex:1; padding:20px; overflow-y:auto; }
                /* 面包屑 */
                .breadcrumb { font-size:13px; color:#8c8c8c; margin-bottom:16px; }
                .breadcrumb span.active { color:#333; }
                /* 卡片 */
                .card { background:#fff; border-radius:6px; padding:20px; box-shadow:0 1px 2px rgba(0,0,0,.08); margin-bottom:16px; }
                .card-title { font-size:16px; font-weight:600; margin-bottom:16px; padding-bottom:12px; border-bottom:1px solid #f0f0f0; }
                /* 统计卡片 */
                .stats { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; margin-bottom:16px; }
                .stat-card { background:#fff; border-radius:6px; padding:20px; box-shadow:0 1px 2px rgba(0,0,0,.08); }
                .stat-card .label { font-size:13px; color:#8c8c8c; margin-bottom:8px; }
                .stat-card .value { font-size:28px; font-weight:600; color:#1890ff; }
                .stat-card .trend { font-size:12px; color:#52c41a; margin-top:4px; }
                /* 表格 */
                table { width:100%; border-collapse:collapse; font-size:13px; }
                th, td { padding:10px 12px; text-align:left; border-bottom:1px solid #f0f0f0; }
                th { background:#fafafa; font-weight:600; color:#333; }
                tr:hover td { background:#e6f7ff; }
                .tag { display:inline-block; padding:2px 8px; border-radius:4px; font-size:12px; }
                .tag-green { background:#f6ffed; color:#52c41a; border:1px solid #b7eb8f; }
                .tag-red { background:#fff1f0; color:#f5222d; border:1px solid #ffa39e; }
                .tag-blue { background:#e6f7ff; color:#1890ff; border:1px solid #91d5ff; }
                .tag-orange { background:#fff7e6; color:#fa8c16; border:1px solid #ffd591; }
                /* 按钮 */
                .btn { padding:6px 16px; border-radius:4px; border:none; cursor:pointer; font-size:13px;
                       background:#1890ff; color:#fff; }
                .btn-default { background:#fff; color:#333; border:1px solid #d9d9d9; }
                .btn-sm { padding:4px 10px; font-size:12px; }
                .toolbar { margin-bottom:16px; display:flex; gap:8px; }
                /* 表单 */
                .form-item { margin-bottom:18px; }
                .form-label { display:block; font-size:13px; color:#333; margin-bottom:6px; }
                .form-label .req { color:#f5222d; margin-right:4px; }
                .form-input, .form-select, .form-textarea {
                    width:100%; padding:8px 12px; border:1px solid #d9d9d9; border-radius:4px;
                    font-size:13px; outline:none;
                }
                .form-input:focus, .form-select:focus, .form-textarea:focus { border-color:#1890ff; box-shadow:0 0 0 2px rgba(24,144,255,.2); }
                .form-textarea { resize:vertical; min-height:70px; font-family:inherit; }
                .form-row { display:grid; grid-template-columns:1fr 1fr; gap:16px; }
                /* 登录页 */
                .login-wrap { display:flex; align-items:center; justify-content:center; height:100vh;
                              background:linear-gradient(135deg,#001529,#1890ff); }
                .login-box { background:#fff; border-radius:8px; padding:40px; width:380px; box-shadow:0 8px 24px rgba(0,0,0,.2); }
                .login-title { font-size:22px; font-weight:bold; text-align:center; margin-bottom:8px; color:#333; }
                .login-sub { text-align:center; color:#8c8c8c; font-size:13px; margin-bottom:28px; }
                .login-input { width:100%; padding:10px 12px; border:1px solid #d9d9d9; border-radius:4px;
                               font-size:14px; margin-bottom:16px; }
                .login-btn { width:100%; padding:10px; background:#1890ff; color:#fff; border:none;
                             border-radius:4px; font-size:15px; cursor:pointer; }
                .login-footer { text-align:center; margin-top:16px; font-size:12px; color:#8c8c8c; }
                </style></head><body>
                """;
    }

    /** 侧边栏菜单 HTML */
    private String sidebar(String activeModule) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='layout'>");
        sb.append("<div class='sidebar'>");
        sb.append("<div class='menu-item ").append("home".equals(activeModule) ? "active" : "").append("'>工作台</div>");
        for (String mod : modules.keySet()) {
            sb.append("<div class='menu-group'>").append(mod).append("</div>");
            for (String f : modules.get(mod)) {
                sb.append("<div class='menu-item'>").append(f).append("</div>");
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    private String headerUser() {
        return "<div class='header'><div class='logo'>" + softwareName + "</div>"
                + "<div class='user'>管理员 admin</div></div>";
    }

    // ---- 登录页 ----
    private String loginHtml() {
        return head() + """
                <div class="login-wrap">
                  <div class="login-box">
                    <div class="login-title">__NAME__</div>
                    <div class="login-sub">请登录您的账户</div>
                    <input class="login-input" placeholder="用户名" value="admin">
                    <input class="login-input" type="password" placeholder="密码" value="********">
                    <button class="login-btn">登 录</button>
                    <div class="login-footer">© 2024 __NAME__  All Rights Reserved</div>
                  </div>
                </div>
                </body></html>
                """.replace("__NAME__", softwareName);
    }

    // ---- 首页/工作台 ----
    private String homeHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append(head());
        sb.append(headerUser());
        sb.append(sidebar("home"));
        sb.append("<div class='content'>");
        sb.append("<div class='breadcrumb'>首页 / <span class='active'>工作台</span></div>");

        // 统计卡片
        sb.append("<div class='stats'>");
        String[][] stats = {
                {"今日记录数", "128", "+12.5%"},
                {"本月记录数", "3,456", "+8.3%"},
                {"异常告警数", "6", "-3.2%"},
                {"在线用户数", "24", "+2"}
        };
        for (String[] s : stats) {
            sb.append("<div class='stat-card'><div class='label'>").append(s[0]).append("</div>")
                    .append("<div class='value'>").append(s[1]).append("</div>")
                    .append("<div class='trend'>").append(s[2]).append(" 较昨日</div></div>");
        }
        sb.append("</div>");

        // 功能模块入口
        sb.append("<div class='card'><div class='card-title'>功能模块</div>");
        sb.append("<div class='stats'>");
        int i = 0;
        for (String mod : modules.keySet()) {
            String icon = i == 0 ? "📋" : i == 1 ? "🔍" : i == 2 ? "📦" : "⚙️";
            sb.append("<div class='stat-card' style='text-align:center;'>")
                    .append("<div style='font-size:28px;margin-bottom:8px;'>").append(icon).append("</div>")
                    .append("<div class='value' style='font-size:15px;color:#333;'>").append(mod).append("</div>")
                    .append("<div class='label'>").append(modules.get(mod).size()).append(" 项功能</div>")
                    .append("</div>");
            i++;
        }
        sb.append("</div></div>");

        // 最近记录
        sb.append("<div class='card'><div class='card-title'>最近操作记录</div>");
        sb.append("<table><thead><tr><th>时间</th><th>操作人</th><th>操作内容</th><th>状态</th></tr></thead><tbody>");
        String[][] rows = {
                {"2024-06-12 09:15", "admin", "录入新记录", "成功"},
                {"2024-06-12 08:50", "zhangsan", "修改记录", "成功"},
                {"2024-06-11 17:30", "lisi", "导出报表", "成功"},
                {"2024-06-11 14:20", "admin", "系统巡检", "正常"},
                {"2024-06-11 10:05", "wangwu", "数据校验", "告警"}
        };
        for (String[] r : rows) {
            String tag = "告警".equals(r[3]) ? "tag-red" : "tag-green";
            sb.append("<tr><td>").append(r[0]).append("</td><td>").append(r[1])
                    .append("</td><td>").append(r[2]).append("</td><td><span class='tag ").append(tag)
                    .append("'>").append(r[3]).append("</span></td></tr>");
        }
        sb.append("</tbody></table></div>");
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    // ---- 模块列表页 ----
    private String listHtml(String module, List<String> funcs) {
        StringBuilder sb = new StringBuilder();
        sb.append(head());
        sb.append(headerUser());
        sb.append(sidebar(module));
        sb.append("<div class='content'>");
        sb.append("<div class='breadcrumb'>").append(module).append(" / <span class='active'>").append(funcs.get(0)).append("</span></div>");
        sb.append("<div class='card'><div class='card-title'>").append(funcs.get(0)).append("</div>");

        // 工具栏
        sb.append("<div class='toolbar'>");
        sb.append("<input class='form-input' style='width:200px;' placeholder='请输入关键词搜索'>");
        sb.append("<button class='btn'>查询</button>");
        sb.append("<button class='btn btn-default'>重置</button>");
        sb.append("<button class='btn' style='margin-left:auto;'>+ 新增</button>");
        sb.append("</div>");

        // 表格
        sb.append("<table><thead><tr><th>编号</th><th>名称</th><th>类型</th><th>录入时间</th><th>状态</th><th>操作</th></tr></thead><tbody>");
        String[] types = funcs.toArray(new String[0]);
        String[] statuses = {"正常", "正常", "待审核", "正常", "告警", "正常"};
        for (int i = 0; i < 6; i++) {
            String tag = "告警".equals(statuses[i]) ? "tag-red" : "待审核".equals(statuses[i]) ? "tag-orange" : "tag-green";
            sb.append("<tr><td>").append(String.format("REC%04d", i + 1)).append("</td>")
                    .append("<td>").append(module).append("-记录").append(i + 1).append("</td>")
                    .append("<td>").append(types[i % types.length]).append("</td>")
                    .append("<td>2024-06-1").append((i % 9) + 1).append(" 14:30</td>")
                    .append("<td><span class='tag ").append(tag).append("'>").append(statuses[i]).append("</span></td>")
                    .append("<td><button class='btn btn-sm'>查看</button> <button class='btn btn-sm btn-default'>编辑</button></td></tr>");
        }
        sb.append("</tbody></table>");
        sb.append("<div style='text-align:right;margin-top:16px;font-size:13px;color:#8c8c8c;'>共 128 条记录</div>");
        sb.append("</div></div></div></body></html>");
        return sb.toString();
    }

    // ---- 模块表单页 ----
    private String formHtml(String module, List<String> funcs) {
        StringBuilder sb = new StringBuilder();
        sb.append(head());
        sb.append(headerUser());
        sb.append(sidebar(module));
        sb.append("<div class='content'>");
        sb.append("<div class='breadcrumb'>").append(module).append(" / ").append(funcs.get(0))
                .append(" / <span class='active'>新增</span></div>");
        sb.append("<div class='card'><div class='card-title'>").append(funcs.get(0)).append(" - 信息录入</div>");

        sb.append("<div class='form-row'>");
        sb.append("<div class='form-item'><label class='form-label'><span class='req'>*</span>编号</label>")
                .append("<input class='form-input' value='REC0129'></div>");
        sb.append("<div class='form-item'><label class='form-label'><span class='req'>*</span>名称</label>")
                .append("<input class='form-input' placeholder='请输入名称'></div>");
        sb.append("</div>");

        sb.append("<div class='form-row'>");
        sb.append("<div class='form-item'><label class='form-label'><span class='req'>*</span>类型</label>")
                .append("<select class='form-select'>");
        for (String f : funcs) sb.append("<option>").append(f).append("</option>");
        sb.append("</select></div>");
        sb.append("<div class='form-item'><label class='form-label'>录入人</label>")
                .append("<input class='form-input' value='admin'></div>");
        sb.append("</div>");

        // 动态字段：根据模块功能生成数值字段
        sb.append("<div class='card-title' style='margin-top:8px;'>指标数据</div>");
        sb.append("<div class='form-row'>");
        String[] fields = {"数值A", "数值B", "数值C", "数值D"};
        for (int i = 0; i < 4; i++) {
            sb.append("<div class='form-item'><label class='form-label'>").append(fields[i]).append("</label>")
                    .append("<input class='form-input' placeholder='请输入").append(fields[i]).append("'></div>");
        }
        sb.append("</div>");

        sb.append("<div class='form-item'><label class='form-label'>备注说明</label>")
                .append("<textarea class='form-textarea' placeholder='请输入备注信息'></textarea></div>");

        sb.append("<div style='margin-top:24px;text-align:center;'>");
        sb.append("<button class='btn' style='margin-right:12px;'>保 存</button>");
        sb.append("<button class='btn btn-default'>取 消</button>");
        sb.append("</div>");

        sb.append("</div></div></div></body></html>");
        return sb.toString();
    }
}
