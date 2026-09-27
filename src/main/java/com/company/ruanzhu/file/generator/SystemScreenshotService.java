package com.company.ruanzhu.file.generator;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用浏览器 headless 模式对模拟系统 HTML 页面截图，
 * 返回 截图标识 -> 截图文件 的映射。
 *
 * 截图标识约定：
 * - login        : 登录页
 * - home         : 首页/工作台
 * - list_1, list_2, ...  : 各模块列表页
 * - form_1, form_2, ...  : 各模块表单页
 */
@Slf4j
public class SystemScreenshotService {

    /** Edge/Chrome 可执行文件路径（按优先级尝试） */
    private static final String[] BROWSER_CANDIDATES = {
            "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"
    };

    private final String softwareName;
    private final String functionMenu;
    private final File workDir;

    public SystemScreenshotService(String softwareName, String functionMenu, File workDir) {
        this.softwareName = softwareName;
        this.functionMenu = functionMenu;
        this.workDir = workDir;
        if (!workDir.exists()) workDir.mkdirs();
    }

    /**
     * 生成全部截图。
     * @return 截图标识 -> 截图文件
     */
    public Map<String, File> captureAll() {
        Map<String, File> result = new LinkedHashMap<>();
        try {
            String browser = findBrowser();
            if (browser == null) {
                log.warn("未找到可用的浏览器，跳过系统截图生成");
                return result;
            }

            // 1. 生成 HTML 页面
            File htmlDir = new File(workDir, "html");
            MockSystemHtmlGenerator htmlGen = new MockSystemHtmlGenerator(softwareName, functionMenu, htmlDir);
            Map<String, File> htmlFiles = htmlGen.generateAll();

            // 2. 逐个截图
            File shotDir = new File(workDir, "shots");
            if (!shotDir.exists()) shotDir.mkdirs();

            for (Map.Entry<String, File> e : htmlFiles.entrySet()) {
                File png = new File(shotDir, e.getKey() + ".png");
                boolean ok = screenshot(browser, e.getValue(), png);
                if (ok && png.exists() && png.length() > 0) {
                    result.put(e.getKey(), png);
                    log.info("截图成功: {} -> {}", e.getKey(), png.getName());
                } else {
                    log.warn("截图失败: {}", e.getKey());
                }
            }
        } catch (Exception e) {
            log.error("系统截图生成异常", e);
        }
        return result;
    }

    /** 查找可用浏览器 */
    private String findBrowser() {
        for (String p : BROWSER_CANDIDATES) {
            if (new File(p).exists()) return p;
        }
        // 尝试 PATH
        for (String cmd : new String[]{"msedge", "chrome", "google-chrome"}) {
            try {
                ProcessBuilder pb = new ProcessBuilder("where", cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String out = new String(p.getInputStream().readAllBytes()).trim();
                p.waitFor();
                if (!out.isEmpty()) {
                    String first = out.split("\\r?\\n")[0].trim();
                    if (new File(first).exists()) return first;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** 调用浏览器 headless 截图 */
    private boolean screenshot(String browser, File htmlFile, File outPng) {
        try {
            File userDataDir = new File(workDir, "edge_profile_" + System.nanoTime());
            if (!userDataDir.exists()) userDataDir.mkdirs();

            List<String> cmd = new ArrayList<>();
            cmd.add(browser);
            cmd.add("--headless");
            cmd.add("--disable-gpu");
            cmd.add("--no-sandbox");
            cmd.add("--hide-scrollbars");
            cmd.add("--force-device-scale-factor=1");
            cmd.add("--window-size=1280,800");
            cmd.add("--user-data-dir=" + userDataDir.getAbsolutePath());
            cmd.add("--screenshot=" + outPng.getAbsolutePath());
            cmd.add(htmlFile.toURI().toString());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            byte[] buf = p.getInputStream().readAllBytes();
            int code = p.waitFor();
            if (code != 0) {
                log.warn("浏览器截图退出码={}, output={}", code, new String(buf));
            }
            // 清理 user-data-dir
            try { deleteRecursively(userDataDir); } catch (Exception ignored) {}
            return outPng.exists() && outPng.length() > 1000;
        } catch (IOException | InterruptedException e) {
            log.error("截图失败: {}", htmlFile.getName(), e);
            return false;
        }
    }

    /** 清理工作目录 */
    public void cleanup() {
        try {
            deleteRecursively(workDir);
        } catch (IOException e) {
            log.warn("清理截图工作目录失败", e);
        }
    }

    private static void deleteRecursively(File f) throws IOException {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        Files.deleteIfExists(f.toPath());
    }
}
