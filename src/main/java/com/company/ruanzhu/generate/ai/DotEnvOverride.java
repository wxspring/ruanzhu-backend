package com.company.ruanzhu.generate.ai;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 强制读取项目 .env 文件并覆盖 Spring @Value 注入值的工具类。
 *
 * 背景：Spring Boot 的配置优先级中，"IDEA 运行配置 Environment variables（进程级 System.getenv）"
 * 永远高于 `me.paulschwarz:spring-dotenv` 注册的文件属性源。这导致用户即便在 .env 文件里写了正确的
 * API Key / base-url，只要 IDEA 运行配置里还留着旧脏值（比如之前手滑写的 xa6r key 或 带 ")," 的
 * base-url），@Value 取到的就永远是 IDEA 那张，改 .env 文件 100 次都没用。
 *
 * 解决方式：各 AiClient 构造函数内直接调用 {@link #loadDotEnv()} 读磁盘上的真实 .env，
 * 按需覆盖 @Value 注入进的字段。并返回 key 末尾 4 位，方便日志里打印给用户肉眼比对。
 */
@Slf4j
public final class DotEnvOverride {

    private DotEnvOverride() {}

    public record Result(Map<String, String> values, String sourcePath) {
        /** 取 .env 中给出的候选名的第一个非空值 */
        public String get(String... candidateKeys) {
            if (values == null || candidateKeys == null) return null;
            for (String k : candidateKeys) {
                String v = values.get(k);
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
            return null;
        }
    }

    /** 定位项目中的 .env 文件并解析为 Map。返回值包含 __source__ 键（记录实际读取到的文件路径）。 */
    public static Result loadDotEnv() {
        Map<String, String> m = new LinkedHashMap<>();
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(System.getProperty("user.dir", ".")).resolve(".env").toAbsolutePath());
        candidates.add(Paths.get(System.getProperty("user.dir", ".")).resolve("..").resolve(".env").toAbsolutePath().normalize());
        candidates.add(Paths.get(".env").toAbsolutePath());
        candidates.add(Paths.get(System.getProperty("user.dir", ".")).resolve("ruanzhu-backend").resolve(".env").toAbsolutePath().normalize());

        Path found = null;
        for (Path p : candidates) {
            try {
                if (Files.isRegularFile(p)) { found = p; break; }
            } catch (Exception ignore) {
                // SecurityManager 之类的跳过
            }
        }
        if (found == null) {
            m.put("__source__", "(未找到 .env 文件)");
            return new Result(m, m.get("__source__"));
        }
        String source = found.toString();
        m.put("__source__", source);
        try {
            int lineNum = 0;
            Map<String, Integer> firstLineOfKey = new java.util.HashMap<>();
            for (String raw : Files.readAllLines(found, StandardCharsets.UTF_8)) {
                lineNum++;
                String line = raw == null ? "" : raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int hash = line.indexOf('#');
                String content = hash >= 0 ? line.substring(0, hash).trim() : line;
                int eq = content.indexOf('=');
                if (eq <= 0) continue;
                String k = content.substring(0, eq).trim();
                String v = content.substring(eq + 1).trim();
                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                    v = v.substring(1, v.length() - 1);
                }
                if (k.isEmpty()) continue;
                if (m.containsKey(k)) {
                    log.warn("[DotEnvOverride] .env 中发现重复的 key：{}。文件 {} 第 {} 行（原值 {}）与第 {} 行（新值 {}）冲突，将以最后一次出现（L{}）的值为准。",
                            k, source, firstLineOfKey.get(k), m.get(k).isEmpty() ? "(空)" : "***" + tail4(m.get(k)),
                            lineNum, v.isEmpty() ? "(空)" : "***" + tail4(v), lineNum);
                } else {
                    firstLineOfKey.put(k, lineNum);
                }
                m.put(k, v);
            }
        } catch (Exception e) {
            log.warn("[DotEnvOverride] 读取 .env 失败，继续使用 @Value 默认：path={}, err={}", source, e.getMessage());
        }
        return new Result(m, source);
    }

    public static String tail4(String s) {
        if (s == null || s.length() < 4) return "????";
        return s.substring(s.length() - 4);
    }
}
