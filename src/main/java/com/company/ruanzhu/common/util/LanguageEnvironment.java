package com.company.ruanzhu.common.util;

import java.util.Locale;

/**
 * 按项目声明的编程语言，给出运行环境 / 依赖栈 / 开发工具 / 部署方式的描述。
 * <p>
 * 背景：说明文档、软件概要(TXT) 与 AI 占位内容曾把 Java/JDK 当作所有项目的默认环境，
 * 导致 Go、Python 等项目的文档里出现 "JDK 17+"、"java -jar" 等与项目无关的内容。
 * 这里统一按 {@code softwareSummary.language} 取值，保证文档与环境描述与项目语言一致。
 */
public final class LanguageEnvironment {

    private LanguageEnvironment() {}

    private static String norm(String language) {
        return language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
    }

    /** 服务器端运行环境（运行时）描述。 */
    public static String runtime(String language) {
        return switch (norm(language)) {
            case "java", "android" -> "JDK 17+（兼容 JDK 1.8+）";
            case "kotlin" -> "JDK 17+（Kotlin 运行于 JVM 之上）";
            case "go", "golang" -> "Go 1.21+ 运行环境（编译为单个静态可执行文件，部署时无需额外运行时）";
            case "python" -> "Python 3.10+";
            case "php" -> "PHP 8.1+（含 PHP-FPM 与 OPcache）";
            case "node.js", "nodejs", "node", "javascript", "js", "typescript", "ts" -> "Node.js 18.x+";
            case "c#", "csharp", "cs", "vb.net", "vbnet", "vb" -> ".NET 8.0 运行时（或 .NET Framework 4.8）";
            case "c", "c++", "cpp" -> "GCC/G++ 11+ 编译工具链（编译为原生可执行文件）";
            case "dart" -> "Dart 3.x SDK（或 Flutter 运行环境）";
            case "ruby" -> "Ruby 3.2+";
            case "objective-c", "objc", "objectivec" -> "macOS 13+ / Apple 系统运行时";
            case "plc" -> "PLC 运行时（IEC 61131-3 标准，如 CODESYS 运行时）";
            default -> "按项目实际部署环境配置运行时";
        };
    }

    /** 运行支撑环境/支持软件（运行时 + 常用中间件）。 */
    public static String supportStack(String language) {
        return runtime(language) + "、MySQL 8.0、Redis 6.0、Nginx 1.24 及 Chrome 100 以上浏览器";
    }

    /** 运行平台/操作系统描述。 */
    public static String runPlatform(String language) {
        return "Windows Server 2019/2022 64位或 CentOS 7/8 服务器操作系统，支持 " + runtime(language) + " 运行。";
    }

    /** 开发工具。 */
    public static String devTools(String language) {
        return switch (norm(language)) {
            case "java", "android" -> "IntelliJ IDEA, Maven, Git";
            case "kotlin" -> "IntelliJ IDEA, Gradle, Git";
            case "go", "golang" -> "GoLand / Visual Studio Code, Go Modules, Git";
            case "python" -> "PyCharm / Visual Studio Code, pip, Git";
            case "php" -> "PhpStorm / Visual Studio Code, Composer, Git";
            case "node.js", "nodejs", "node", "javascript", "js", "typescript", "ts" -> "Visual Studio Code, npm, Git";
            case "c#", "csharp", "cs", "vb.net", "vbnet", "vb" -> "Visual Studio, .NET SDK, Git";
            case "c", "c++", "cpp" -> "CLion / Visual Studio, CMake, Git";
            case "dart" -> "Android Studio / Visual Studio Code, pub, Git";
            case "ruby" -> "RubyMine / Visual Studio Code, Bundler, Git";
            case "objective-c", "objc", "objectivec" -> "Xcode, Git";
            case "plc" -> "CODESYS / TIA Portal, Git";
            default -> "Visual Studio Code, Git";
        };
    }

    /** 后端编译/发布产物描述。 */
    public static String artifact(String language) {
        return switch (norm(language)) {
            case "java", "kotlin" -> "后端可执行文件（JAR 包，如 app.jar）";
            case "go", "golang" -> "后端可执行文件（Linux 下为无扩展名文件，Windows 下为 .exe）";
            case "python" -> "后端源码（app.py，或打包后的可执行文件）";
            case "node.js", "nodejs", "node", "javascript", "js", "typescript", "ts" -> "后端源码（app.js，由 Node.js 运行）";
            case "php" -> "后端源码（index.php，由 PHP-FPM 运行）";
            case "c#", "csharp", "cs", "vb.net", "vbnet", "vb" -> "后端发布产物（.NET 程序集/可执行文件）";
            case "c", "c++", "cpp" -> "后端可执行文件（编译生成的原生二进制）";
            case "dart" -> "后端可执行文件（dart compile 编译产物）";
            case "ruby" -> "后端源码（由 Ruby 解释执行）";
            case "objective-c", "objc", "objectivec" -> "后端可执行文件（macOS 原生二进制）";
            case "plc" -> "PLC 程序（下载至 PLC 运行）";
            default -> "后端可执行文件";
        };
    }

    /** 启动后端服务命令。 */
    public static String startCommand(String language) {
        return switch (norm(language)) {
            case "java", "kotlin" -> "java -jar app.jar --spring.config.location=application.yml";
            case "go", "golang" -> "./app";
            case "python" -> "python app.py";
            case "node.js", "nodejs", "node", "javascript", "js", "typescript", "ts" -> "node app.js";
            case "php" -> "通过 PHP-FPM + Nginx 运行 index.php";
            case "c#", "csharp", "cs", "vb.net", "vbnet", "vb" -> "dotnet app.dll";
            case "c", "c++", "cpp", "dart", "objective-c", "objc", "objectivec" -> "./app";
            case "ruby" -> "ruby app.rb";
            case "plc" -> "在 PLC 编程软件中下载程序并切换至 RUN 模式";
            default -> "./app";
        };
    }

    /**
     * 开发工具：已存字段若明显属于其它语言技术栈（历史遗留的 Java 模板值），按声明语言重新给出。
     * 已有值与声明语言不冲突时保留用户填写的值。
     */
    public static String devToolsOr(String language, String stored) {
        return foreign(language, stored) ? devTools(language) : nonBlank(stored, devTools(language));
    }

    /** 运行平台：已存字段若属于其它语言技术栈则按声明语言重新给出。 */
    public static String runPlatformOr(String language, String stored) {
        return foreign(language, stored) ? runPlatform(language) : nonBlank(stored, runPlatform(language));
    }

    /** 运行支撑环境：已存字段若属于其它语言技术栈则按声明语言重新给出。 */
    public static String supportStackOr(String language, String stored) {
        return foreign(language, stored) ? supportStack(language) : nonBlank(stored, supportStack(language));
    }

    private static String nonBlank(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    /** 判断文本里是否出现了与声明语言不同家族的标志性技术名词。 */
    private static boolean foreign(String language, String text) {
        if (text == null || text.isBlank()) return false;
        String own = familyOf(language);
        String t = text.toLowerCase(Locale.ROOT);
        for (String family : FAMILIES) {
            if (family.equals(own)) continue;
            for (String marker : markers(family)) {
                if (t.contains(marker)) return true;
            }
        }
        return false;
    }

    private static String familyOf(String language) {
        return switch (norm(language)) {
            case "java", "kotlin", "android" -> "jvm";
            case "go", "golang" -> "go";
            case "python" -> "python";
            case "php" -> "php";
            case "node.js", "nodejs", "node", "javascript", "js", "typescript", "ts" -> "node";
            case "c#", "csharp", "cs", "vb.net", "vbnet", "vb" -> "dotnet";
            case "c", "c++", "cpp" -> "native";
            case "dart" -> "dart";
            case "ruby" -> "ruby";
            case "objective-c", "objc", "objectivec" -> "objc";
            case "plc" -> "plc";
            default -> "";
        };
    }

    private static final String[] FAMILIES = {"jvm", "go", "python", "php", "node", "dotnet", "native", "dart", "ruby", "objc", "plc"};

    private static String[] markers(String family) {
        return switch (family) {
            // 不直接用 "java"，避免误伤 javascript / typescript
            case "jvm" -> new String[]{"jdk", "jvm", "intellij", "maven", "gradle", "tomcat", "spring boot"};
            case "go" -> new String[]{"golang", "goland", "go modules", "go 1.", "go build"};
            case "python" -> new String[]{"python", "pycharm"};
            case "php" -> new String[]{"php", "composer"};
            case "node" -> new String[]{"node", "npm", "javascript", "typescript"};
            case "dotnet" -> new String[]{".net", "dotnet", "nuget"};
            case "native" -> new String[]{"gcc", "g++", "cmake", "clion", "visual studio"};
            case "dart" -> new String[]{"dart", "flutter"};
            case "ruby" -> new String[]{"ruby", "bundler", "rubymine"};
            case "objc" -> new String[]{"xcode", "objective-c"};
            case "plc" -> new String[]{"plc", "codesys", "tia portal"};
            default -> new String[0];
        };
    }

    /** 停止后端服务命令。 */
    public static String stopCommand(String language) {
        return switch (norm(language)) {
            case "java", "kotlin" -> "kill $(pgrep -f app.jar)";
            case "go", "golang" -> "kill $(pgrep -f app)";
            case "python" -> "kill $(pgrep -f app.py)";
            case "node.js", "nodejs", "node", "javascript", "js", "typescript", "ts" -> "kill $(pgrep -f app.js)";
            case "php" -> "systemctl stop php-fpm";
            case "c#", "csharp", "cs", "vb.net", "vbnet", "vb" -> "kill $(pgrep -f dotnet)";
            case "ruby" -> "kill $(pgrep -f app.rb)";
            case "plc" -> "在 PLC 编程软件中切换至 STOP 模式并停止运行";
            default -> "kill $(pgrep -f app)";
        };
    }
}
