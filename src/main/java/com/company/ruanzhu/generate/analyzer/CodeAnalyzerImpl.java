package com.company.ruanzhu.generate.analyzer;

import com.company.ruanzhu.generate.model.vo.CodeAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default {@link CodeAnalyzer} implementation.
 * <p>
 * Walks the directory tree, counts lines in source files (skipping binary files and common
 * build/dependency directories), detects the primary language by file-extension frequency,
 * and detects frameworks by scanning for marker files.
 */
@Slf4j
@Component
public class CodeAnalyzerImpl implements CodeAnalyzer {

    // ------------------------------------------------------------------
    // Extension -> language mapping
    // ------------------------------------------------------------------

    private static final Map<String, String> EXTENSION_TO_LANGUAGE = new HashMap<>();

    static {
        EXTENSION_TO_LANGUAGE.put("java", "Java");
        EXTENSION_TO_LANGUAGE.put("js", "JavaScript");
        EXTENSION_TO_LANGUAGE.put("jsx", "JavaScript");
        EXTENSION_TO_LANGUAGE.put("ts", "TypeScript");
        EXTENSION_TO_LANGUAGE.put("tsx", "TypeScript");
        EXTENSION_TO_LANGUAGE.put("py", "Python");
        EXTENSION_TO_LANGUAGE.put("go", "Go");
        EXTENSION_TO_LANGUAGE.put("vue", "Vue");
        EXTENSION_TO_LANGUAGE.put("html", "HTML");
        EXTENSION_TO_LANGUAGE.put("htm", "HTML");
        EXTENSION_TO_LANGUAGE.put("css", "CSS");
        EXTENSION_TO_LANGUAGE.put("scss", "CSS");
        EXTENSION_TO_LANGUAGE.put("sass", "CSS");
        EXTENSION_TO_LANGUAGE.put("less", "CSS");
        EXTENSION_TO_LANGUAGE.put("xml", "XML");
        EXTENSION_TO_LANGUAGE.put("json", "JSON");
        EXTENSION_TO_LANGUAGE.put("yml", "YAML");
        EXTENSION_TO_LANGUAGE.put("yaml", "YAML");
        EXTENSION_TO_LANGUAGE.put("sql", "SQL");
        EXTENSION_TO_LANGUAGE.put("sh", "Shell");
        EXTENSION_TO_LANGUAGE.put("bash", "Shell");
        EXTENSION_TO_LANGUAGE.put("zsh", "Shell");
        EXTENSION_TO_LANGUAGE.put("c", "C");
        EXTENSION_TO_LANGUAGE.put("h", "C");
        EXTENSION_TO_LANGUAGE.put("cpp", "C++");
        EXTENSION_TO_LANGUAGE.put("hpp", "C++");
        EXTENSION_TO_LANGUAGE.put("cc", "C++");
        EXTENSION_TO_LANGUAGE.put("cxx", "C++");
        EXTENSION_TO_LANGUAGE.put("cs", "C#");
        EXTENSION_TO_LANGUAGE.put("rb", "Ruby");
        EXTENSION_TO_LANGUAGE.put("php", "PHP");
        EXTENSION_TO_LANGUAGE.put("swift", "Swift");
        EXTENSION_TO_LANGUAGE.put("kt", "Kotlin");
        EXTENSION_TO_LANGUAGE.put("kts", "Kotlin");
        EXTENSION_TO_LANGUAGE.put("rs", "Rust");
        EXTENSION_TO_LANGUAGE.put("dart", "Dart");
        EXTENSION_TO_LANGUAGE.put("lua", "Lua");
        EXTENSION_TO_LANGUAGE.put("r", "R");
        EXTENSION_TO_LANGUAGE.put("scala", "Scala");
        EXTENSION_TO_LANGUAGE.put("pl", "Perl");
        EXTENSION_TO_LANGUAGE.put("pm", "Perl");
        EXTENSION_TO_LANGUAGE.put("groovy", "Groovy");
        EXTENSION_TO_LANGUAGE.put("gradle", "Groovy");
    }

    // ------------------------------------------------------------------
    // Binary / non-source extensions (skip entirely)
    // ------------------------------------------------------------------

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "class", "jar", "war", "ear", "exe", "dll", "so", "o", "a", "lib", "obj",
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg", "webp", "tiff", "tif",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "zip", "tar", "gz", "bz2", "xz", "rar", "7z",
            "woff", "woff2", "ttf", "eot", "otf",
            "mp3", "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm",
            "pyc", "pyo", "pyd",
            "db", "sqlite", "sqlite3"
    );

    // ------------------------------------------------------------------
    // Directories to skip entirely
    // ------------------------------------------------------------------

    private static final Set<String> SKIP_DIRS = Set.of(
            "node_modules", "target", ".git", ".svn", ".hg",
            "__pycache__", ".idea", ".vscode", "dist", "build",
            "out", "bin", ".gradle", ".mvn", ".yarn",
            ".cache", ".npm", ".nuxt", ".next", "coverage",
            "vendor", "venv", ".venv", "env", ".env"
    );

    @Override
    public CodeAnalysisResult analyze(String seedCodePath) {
        Path root = Paths.get(seedCodePath);
        if (!Files.isDirectory(root)) {
            log.warn("Seed code path is not a directory: {}", seedCodePath);
            return new CodeAnalysisResult();
        }

        CodeAnalysisResult result = new CodeAnalysisResult();
        Map<String, Integer> languageLineCounts = new HashMap<>();
        Map<String, Integer> fileExtensions = new HashMap<>();
        Map<String, Integer> directoryStructure = new HashMap<>();
        List<Path> allFiles = new ArrayList<>();

        // 1. Walk directory tree
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if (SKIP_DIRS.contains(dirName) && !dir.equals(root)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.getFileName().toString();
                    String ext = getExtension(fileName);

                    // Skip binary files
                    if (BINARY_EXTENSIONS.contains(ext.toLowerCase())) {
                        return FileVisitResult.CONTINUE;
                    }

                    // Skip hidden files
                    if (fileName.startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }

                    allFiles.add(file);

                    // Track extension
                    if (!ext.isEmpty()) {
                        fileExtensions.merge(ext, 1, Integer::sum);
                    }

                    // Track directory (relative to root)
                    Path relativeDir = root.relativize(file.getParent());
                    String dirKey = relativeDir.toString().isEmpty() ? "." : relativeDir.toString();
                    directoryStructure.merge(dirKey, 1, Integer::sum);

                    // Count lines
                    int lines = countLines(file);
                    if (lines > 0) {
                        String lang = EXTENSION_TO_LANGUAGE.get(ext.toLowerCase());
                        if (lang != null) {
                            languageLineCounts.merge(lang, lines, Integer::sum);
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Failed to visit file: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Failed to walk directory tree: {}", root, e);
        }

        // 2. Calculate totals
        int totalLines = languageLineCounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalFiles = allFiles.size();

        // 3. Detect primary language (by line count)
        String primaryLanguage = languageLineCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        // 4. Detect frameworks
        List<String> frameworks = detectFrameworks(root);

        // 5. Populate result
        result.setTotalLines(totalLines);
        result.setTotalFiles(totalFiles);
        result.setLanguage(primaryLanguage);
        result.setFrameworks(frameworks);
        result.setDirectoryStructure(directoryStructure);
        result.setFileExtensions(fileExtensions);

        log.info("Code analysis complete: {} files, {} lines, language={}, frameworks={}",
                totalFiles, totalLines, primaryLanguage, frameworks);

        return result;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private int countLines(Path file) {
        int lines = 0;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            while (reader.readLine() != null) {
                lines++;
            }
        } catch (IOException e) {
            log.warn("Failed to count lines in: {}", file, e);
        }
        return lines;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    /**
     * Detect frameworks by scanning for well-known marker files and their content.
     */
    private List<String> detectFrameworks(Path root) {
        Set<String> frameworks = new LinkedHashSet<>();

        // pom.xml -> check for Spring Boot parent/plugin
        Path pomXml = root.resolve("pom.xml");
        if (Files.isRegularFile(pomXml)) {
            String content = readFileContent(pomXml);
            if (content.contains("spring-boot-starter-parent") ||
                    content.contains("spring-boot-maven-plugin")) {
                frameworks.add("Spring Boot");
            }
            // Maven in general
            frameworks.add("Maven");
        }

        // build.gradle / build.gradle.kts -> check for Spring Boot plugin
        Path buildGradle = root.resolve("build.gradle");
        Path buildGradleKts = root.resolve("build.gradle.kts");
        if (Files.isRegularFile(buildGradle)) {
            String content = readFileContent(buildGradle);
            if (content.contains("org.springframework.boot")) {
                frameworks.add("Spring Boot");
            }
            frameworks.add("Gradle");
        } else if (Files.isRegularFile(buildGradleKts)) {
            String content = readFileContent(buildGradleKts);
            if (content.contains("org.springframework.boot")) {
                frameworks.add("Spring Boot");
            }
            frameworks.add("Gradle");
        }

        // package.json -> detect JS/TS frameworks
        Path packageJson = findFile(root, "package.json", 3);
        if (packageJson != null) {
            String content = readFileContent(packageJson);
            if (containsDependency(content, "react") || containsDependency(content, "react-dom")) {
                frameworks.add("React");
            }
            if (containsDependency(content, "vue")) {
                frameworks.add("Vue");
            }
            if (containsDependency(content, "@angular/core")) {
                frameworks.add("Angular");
            }
            if (containsDependency(content, "next")) {
                frameworks.add("Next.js");
            }
            if (containsDependency(content, "nuxt")) {
                frameworks.add("Nuxt.js");
            }
            if (containsDependency(content, "express")) {
                frameworks.add("Express");
            }
            if (containsDependency(content, "nestjs") || containsDependency(content, "@nestjs/core")) {
                frameworks.add("NestJS");
            }
            if (containsDependency(content, "electron")) {
                frameworks.add("Electron");
            }
        }

        // requirements.txt -> detect Python frameworks
        Path requirementsTxt = findFile(root, "requirements.txt", 3);
        if (requirementsTxt != null) {
            String content = readFileContent(requirementsTxt);
            if (containsPythonDep(content, "django")) {
                frameworks.add("Django");
            }
            if (containsPythonDep(content, "flask")) {
                frameworks.add("Flask");
            }
            if (containsPythonDep(content, "fastapi")) {
                frameworks.add("FastAPI");
            }
        }

        // Gemfile -> detect Ruby on Rails
        Path gemfile = findFile(root, "Gemfile", 2);
        if (gemfile != null) {
            String content = readFileContent(gemfile);
            if (content.contains("rails")) {
                frameworks.add("Ruby on Rails");
            }
        }

        // composer.json -> detect Laravel
        Path composerJson = findFile(root, "composer.json", 2);
        if (composerJson != null) {
            String content = readFileContent(composerJson);
            if (content.contains("laravel/framework")) {
                frameworks.add("Laravel");
            }
        }

        // go.mod -> Go modules
        Path goMod = findFile(root, "go.mod", 2);
        if (goMod != null) {
            frameworks.add("Go Modules");
        }

        // Cargo.toml -> Rust
        Path cargoToml = findFile(root, "Cargo.toml", 2);
        if (cargoToml != null) {
            frameworks.add("Cargo");
        }

        return new ArrayList<>(frameworks);
    }

    /**
     * Search for a file starting from root, up to maxDepth levels deep.
     */
    private Path findFile(Path root, String fileName, int maxDepth) {
        try {
            for (int depth = 0; depth <= maxDepth; depth++) {
                Path candidate = root.resolve(fileName);
                if (depth == 0 && Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
            // Also check immediate subdirectories
            try (var stream = Files.list(root)) {
                for (Path sub : stream.toList()) {
                    if (Files.isDirectory(sub) && !SKIP_DIRS.contains(sub.getFileName().toString())) {
                        Path candidate = sub.resolve(fileName);
                        if (Files.isRegularFile(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to search for {}: {}", fileName, root, e);
        }
        return null;
    }

    private String readFileContent(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read file: {}", file, e);
            return "";
        }
    }

    /**
     * Check whether a package.json-style content string contains a dependency on the given name
     * in dependencies, devDependencies, or peerDependencies.
     */
    private boolean containsDependency(String content, String depName) {
        // Match patterns like "react": "..." or "react": { ... }
        return content.contains("\"" + depName + "\"") &&
                (content.contains("\"dependencies\"") || content.contains("\"devDependencies\"") ||
                        content.contains("\"peerDependencies\""));
    }

    /**
     * Check whether a requirements.txt-style content string contains a Python dependency.
     */
    private boolean containsPythonDep(String content, String depName) {
        // Match lines like "django==3.2" or "django" or "Django" (case-insensitive)
        String lower = content.toLowerCase();
        return lower.contains(depName.toLowerCase());
    }
}
