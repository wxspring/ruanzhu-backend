package com.company.ruanzhu.generate.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 多语言代码骨架生成器（占位兜底，不依赖 AI）。
 * <p>
 * 目标：当大模型调用失败/返回空时，按项目声明的 {@code language} 生成语法正确的多文件代码骨架，
 * 而不是旧版硬编码的 "com.highwater" Java 模板。
 * <p>
 * 支持 16 种语言：Java, Python, PHP, C++, C, C#, Go, JavaScript, TypeScript, Node.js,
 * Android, Dart, Kotlin, Objective-C, Ruby, PLC, VB.NET。
 * <p>
 * 命名空间/包名由软件名派生（去除非字母数字），确保换课题不再泄露上一行业的硬编码。
 */
public final class MultiLanguageCodeTemplate {

    private MultiLanguageCodeTemplate() {}

    /** 语言语法规格 */
    public record LangSpec(
            String ext,            // 文件扩展名
            String lineComment,    // 行注释
            String blockOpen,      // 块注释开
            String blockClose,     // 块注释闭
            String packageKw,      // 包/命名空间关键字
            String importKw,       // 导入关键字
            String classKw,        // 类定义关键字
            String funcKw,         // 函数定义关键字
            String varKw,          // 变量声明关键字
            boolean braceBlock,    // 用 {} 还是缩进/end
            String entryFile,      // 入口文件名
            String entrySignature  // 入口函数签名
    ) {}

    /** 软件名 → 合法包名 */
    private static String derivePkg(String name) {
        if (name == null) return "app";
        String s = name.replaceAll("[^a-zA-Z0-9]", "");
        if (s.isEmpty()) s = "app";
        if (Character.isDigit(s.charAt(0))) s = "App" + s;
        return s.toLowerCase(Locale.ROOT);
    }

    /** 软件名 → 合法类名 */
    private static String deriveClass(String name) {
        if (name == null) return "App";
        String[] parts = name.split("[^a-zA-Z0-9]+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.substring(1).toLowerCase(Locale.ROOT));
        }
        String r = sb.toString();
        if (r.isEmpty()) r = "App";
        if (Character.isDigit(r.charAt(0))) r = "App" + r;
        return r;
    }

    public static LangSpec spec(String language) {
        String lang = (language == null ? "Java" : language).trim().toLowerCase(Locale.ROOT);
        return switch (lang) {
            case "java" -> new LangSpec(".java", "//", "/*", "*/", "package", "import",
                    "class", "", "", true, "Application.java", "public static void main(String[] args)");
            case "kotlin" -> new LangSpec(".kt", "//", "/*", "*/", "package", "import",
                    "class", "fun", "", true, "Main.kt", "fun main(args: Array<String>)");
            case "python" -> new LangSpec(".py", "#", "\"\"\"", "\"\"\"", "", "import",
                    "class", "def", "", false, "main.py", "");
            case "php" -> new LangSpec(".php", "//", "/*", "*/", "", "use",
                    "class", "function", "$", true, "index.php", "");
            case "c++", "cpp" -> new LangSpec(".cpp", "//", "/*", "*/", "", "#include",
                    "class", "", "", true, "main.cpp", "int main(int argc, char* argv[])");
            case "c" -> new LangSpec(".c", "//", "/*", "*/", "", "#include",
                    "struct", "", "", true, "main.c", "int main(int argc, char* argv[])");
            case "c#", "csharp", "cs" -> new LangSpec(".cs", "//", "/*", "*/", "namespace", "using",
                    "class", "", "", true, "Program.cs", "static void Main(string[] args)");
            case "go" -> new LangSpec(".go", "//", "/*", "*/", "package", "import",
                    "type", "func", "", true, "main.go", "func main()");
            case "javascript", "js", "node.js", "nodejs", "node" -> new LangSpec(".js", "//", "/*", "*/", "",
                    "", "class", "function", "", true, "app.js", "");
            case "typescript", "ts" -> new LangSpec(".ts", "//", "/*", "*/", "",
                    "import", "class", "function", "", true, "app.ts", "");
            case "dart" -> new LangSpec(".dart", "//", "/*", "*/", "", "import",
                    "class", "", "", true, "main.dart", "void main()");
            case "ruby" -> new LangSpec(".rb", "#", "=begin", "=end", "", "require",
                    "class", "def", "", false, "main.rb", "");
            case "objective-c", "objc", "objectivec" -> new LangSpec(".m", "//", "/*", "*/", "",
                    "#import", "@interface", "", "", true, "main.m", "int main(int argc, char* argv[])");
            case "android" -> new LangSpec(".java", "//", "/*", "*/", "package", "import",
                    "class", "", "", true, "MainActivity.java", "public static void main(String[] args)");
            case "vb.net", "vbnet", "vb" -> new LangSpec(".vb", "'", null, null, "Namespace", "Imports",
                    "Class", "Sub", "Dim", false, "Program.vb", "");
            case "plc" -> new LangSpec(".st", "//", "(*", "*)", "", "",
                    "", "", "", false, "Program.st", "");
            default -> spec("java");
        };
    }

    /** 生成多文件代码骨架，按 "// File: 路径" 分隔。 */
    public static String build(String language, String softwareName, String domain) {
        LangSpec s = spec(language);
        String pkg = derivePkg(softwareName);
        String cls = deriveClass(softwareName);
        String dom = domain == null ? "业务" : domain;

        StringBuilder sb = new StringBuilder();
        sb.append(entryFile(s, pkg, cls, dom)).append("\n\n");
        sb.append(commonResult(s, pkg)).append("\n\n");
        for (String entity : Arrays.asList(cls + "Record", cls + "Config", cls + "Log")) {
            sb.append(entityFile(s, pkg, entity, dom)).append("\n\n");
        }
        for (String entity : Arrays.asList(cls + "Record", cls + "Config", cls + "Log")) {
            sb.append(serviceFile(s, pkg, entity, dom)).append("\n\n");
        }
        for (String entity : Arrays.asList(cls + "Record", cls + "Config", cls + "Log")) {
            sb.append(controllerFile(s, pkg, entity, dom)).append("\n\n");
        }
        return sb.toString();
    }

    // ===================== 公共辅助 =====================

    private static String fileHeader(LangSpec s, String path) {
        return s.lineComment() + " File: " + path;
    }

    private static String blockComment(LangSpec s, String text) {
        if (s.blockOpen() == null) return s.lineComment() + " " + text + "\n";
        return s.blockOpen() + "\n" + s.lineComment() + " " + text + "\n" + s.blockClose() + "\n";
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ===================== 入口文件 =====================

    private static String entryFile(LangSpec s, String pkg, String cls, String dom) {
        StringBuilder sb = new StringBuilder();
        sb.append(fileHeader(s, "src/" + pkg + "/" + s.entryFile())).append("\n");
        appendPackage(s, sb, pkg);
        sb.append(blockComment(s, cls + " 系统入口 - " + dom));
        switch (s.ext()) {
            case ".java" -> {
                sb.append("public class ").append(cls).append("Application {\n");
                sb.append("    public static void main(String[] args) {\n");
                sb.append("        System.out.println(\"").append(cls).append(" started\");\n");
                sb.append("    }\n}\n");
            }
            case ".kt" -> {
                sb.append("fun main() {\n");
                sb.append("    println(\"").append(cls).append(" started\")\n");
                sb.append("}\n");
            }
            case ".cs" -> {
                sb.append("class Program {\n");
                sb.append("    static void Main(string[] args) {\n");
                sb.append("        Console.WriteLine(\"").append(cls).append(" started\");\n");
                sb.append("    }\n}\n");
            }
            case ".go" -> {

                sb.append("import \"fmt\"\n\n");
                sb.append("func main() {\n");
                sb.append("\tfmt.Println(\"").append(cls).append(" started\")\n");
                sb.append("}\n");
            }
            case ".cpp" -> {
                sb.append("#include <iostream>\nusing namespace std;\n\n");
                sb.append("int main(int argc, char* argv[]) {\n");
                sb.append("    cout << \"" + cls + " started\" << endl;\n");
                sb.append("    return 0;\n}\n");
            }
            case ".c" -> {
                sb.append("#include <stdio.h>\n\n");
                sb.append("int main(int argc, char* argv[]) {\n");
                sb.append("    printf(\"" + cls + " started\\n\");\n");
                sb.append("    return 0;\n}\n");
            }
            case ".js" -> {
                sb.append("function main() {\n");
                sb.append("    console.log(\"" + cls + " started\");\n");
                sb.append("}\n\nmain();\n");
            }
            case ".ts" -> {
                sb.append("function main(): void {\n");
                sb.append("    console.log(\"" + cls + " started\");\n");
                sb.append("}\n\nmain();\n");
            }
            case ".dart" -> {
                sb.append("void main() {\n");
                sb.append("  print('" + cls + " started');\n");
                sb.append("}\n");
            }
            case ".m" -> {
                sb.append("#import <Foundation/Foundation.h>\n\n");
                sb.append("int main(int argc, char* argv[]) {\n");
                sb.append("    @autoreleasepool {\n");
                sb.append("        NSLog(@\"" + cls + " started\");\n");
                sb.append("    }\n");
                sb.append("    return 0;\n}\n");
            }
            case ".php" -> {
                sb.append("<?php\n");
                sb.append("echo \"" + cls + " started\";\n");
            }
            case ".py" -> {
                sb.append("def main():\n");
                sb.append("    print(\"" + cls + " started\")\n\n");
                sb.append("if __name__ == \"__main__\":\n");
                sb.append("    main()\n");
            }
            case ".rb" -> {
                sb.append("puts \"" + cls + " started\"\n");
            }
            case ".vb" -> {
                sb.append("Module ").append(cls).append("App\n");
                sb.append("    Sub Main()\n");
                sb.append("        Console.WriteLine(\"" + cls + " started\")\n");
                sb.append("    End Sub\n");
                sb.append("End Module\n");
            }
            case ".st" -> {
                sb.append("PROGRAM ").append(cls.toUpperCase(Locale.ROOT)).append("\n");
                sb.append("    ; ").append(cls).append(" 主程序入口\n");
                sb.append("END_PROGRAM\n");
            }
            default -> sb.append("public class ").append(cls).append("Application {}\n");
        }
        closePackage(s, sb);
        return sb.toString();
    }

    // ===================== 通用响应 =====================

    private static String commonResult(LangSpec s, String pkg) {
        StringBuilder sb = new StringBuilder();
        sb.append(fileHeader(s, "src/" + pkg + "/common/Result" + s.ext())).append("\n");
        appendPackage(s, sb, pkg + ".common");
        switch (s.ext()) {
            case ".java" -> {
                sb.append("public class Result<T> {\n");
                sb.append("    private int code;\n    private String message;\n    private T data;\n");
                sb.append("    public static <T> Result<T> ok(T data) { return new Result<>(0, \"success\", data); }\n");
                sb.append("    public static <T> Result<T> fail(String msg) { return new Result<>(500, msg, null); }\n");
                sb.append("}\n");
            }
            case ".kt" -> {
                sb.append("data class Result<T>(val code: Int = 0, val message: String = \"success\", val data: T? = null) {\n");
                sb.append("    companion object {\n");
                sb.append("        fun <T> ok(data: T) = Result(0, \"success\", data)\n");
                sb.append("        fun <T> fail(msg: String) = Result<T>(500, msg, null)\n");
                sb.append("    }\n}\n");
            }
            case ".cs" -> {
                sb.append("public class Result<T> {\n");
                sb.append("    public int Code { get; set; }\n");
                sb.append("    public string Message { get; set; }\n");
                sb.append("    public T Data { get; set; }\n");
                sb.append("    public static Result<T> Ok(T data) => new Result<T> { Code = 0, Message = \"success\", Data = data };\n");
                sb.append("}\n");
            }
            case ".go" -> {

                sb.append("type Result[T any] struct {\n");
                sb.append("\tCode    int    `json:\"code\"`\n");
                sb.append("\tMessage string `json:\"message\"`\n");
                sb.append("\tData    T      `json:\"data\"`\n");
                sb.append("}\n");
            }
            case ".cpp", ".c" -> {
                sb.append("typedef struct {\n    int code;\n    char* message;\n    void* data;\n} Result;\n");
            }
            case ".js" -> {
                sb.append("class Result {\n");
                sb.append("    constructor(code = 0, message = 'success', data = null) {\n");
                sb.append("        this.code = code; this.message = message; this.data = data;\n");
                sb.append("    }\n");
                sb.append("    static ok(data) { return new Result(0, 'success', data); }\n");
                sb.append("}\n");
            }
            case ".ts" -> {
                sb.append("export class Result<T> {\n");
                sb.append("    constructor(public code: number = 0, public message: string = 'success', public data: T | null = null) {}\n");
                sb.append("    static ok<T>(data: T): Result<T> { return new Result<T>(0, 'success', data); }\n");
                sb.append("}\n");
            }
            case ".dart" -> {
                sb.append("class Result<T> {\n");
                sb.append("  final int code;\n  final String message;\n  final T? data;\n");
                sb.append("  Result({this.code = 0, this.message = 'success', this.data});\n");
                sb.append("  static Result<T> ok<T>(T data) => Result<T>(code: 0, message: 'success', data: data);\n");
                sb.append("}\n");
            }
            case ".m" -> {
                sb.append("@interface Result : NSObject\n");
                sb.append("@property (nonatomic, assign) NSInteger code;\n");
                sb.append("@property (nonatomic, copy) NSString *message;\n");
                sb.append("@property (nonatomic, strong) id data;\n");
                sb.append("+ (instancetype)ok:(id)data;\n");
                sb.append("@end\n");
            }
            case ".php" -> {
                sb.append("<?php\nclass Result {\n");
                sb.append("    public $code; public $message; public $data;\n");
                sb.append("    public function __construct($code = 0, $message = 'success', $data = null) {\n");
                sb.append("        $this->code = $code; $this->message = $message; $this->data = $data;\n");
                sb.append("    }\n");
                sb.append("    public static function ok($data) { return new self(0, 'success', $data); }\n");
                sb.append("}\n");
            }
            case ".py" -> {
                sb.append("class Result:\n");
                sb.append("    def __init__(self, code=0, message=\"success\", data=None):\n");
                sb.append("        self.code = code\n        self.message = message\n        self.data = data\n");
                sb.append("    @staticmethod\n    def ok(data): return Result(0, \"success\", data)\n");
                sb.append("    @staticmethod\n    def fail(msg): return Result(500, msg)\n");
            }
            case ".rb" -> {
                sb.append("class Result\n");
                sb.append("  attr_accessor :code, :message, :data\n");
                sb.append("  def initialize(code = 0, message = 'success', data = nil)\n");
                sb.append("    @code, @message, @data = code, message, data\n");
                sb.append("  end\n");
                sb.append("  def self.ok(data); Result.new(0, 'success', data); end\n");
                sb.append("end\n");
            }
            case ".vb" -> {
                sb.append("Public Class Result(Of T)\n");
                sb.append("    Public Property Code As Integer\n");
                sb.append("    Public Property Message As String\n");
                sb.append("    Public Property Data As T\n");
                sb.append("    Public Shared Function Ok(data As T) As Result(Of T)\n");
                sb.append("        Return New Result(Of T) With {.Code = 0, .Message = \"success\", .Data = data}\n");
                sb.append("    End Function\n");
                sb.append("End Class\n");
            }
            case ".st" -> {
                sb.append("FUNCTION_BLOCK Result\n");
                sb.append("VAR\n    code : INT;\n    message : STRING;\nEND_VAR\n");
                sb.append("END_FUNCTION_BLOCK\n");
            }
            default -> sb.append("// Result placeholder\n");
        }
        closePackage(s, sb);
        return sb.toString();
    }

    // ===================== 实体文件 =====================

    private static String entityFile(LangSpec s, String pkg, String entity, String dom) {
        StringBuilder sb = new StringBuilder();
        sb.append(fileHeader(s, "src/" + pkg + "/entity/" + entity + s.ext())).append("\n");
        appendPackage(s, sb, pkg + ".entity");
        String[] fields = {"id", "code", "name", "status", "createdAt", "updatedAt", "remark", "operator"};
        switch (s.ext()) {
            case ".java" -> {
                sb.append("public class ").append(entity).append(" {\n");
                for (String f : fields) sb.append("    private ").append(javaType(f)).append(" ").append(f).append(";\n");
                for (String f : fields) {
                    String t = javaType(f);
                    sb.append("    public ").append(t).append(" get").append(cap(f)).append("() { return ").append(f).append("; }\n");
                    sb.append("    public void set").append(cap(f)).append("(").append(t).append(" ").append(f).append(") { this.").append(f).append(" = ").append(f).append("; }\n");
                }
                sb.append("}\n");
            }
            case ".kt" -> {
                sb.append("data class ").append(entity).append("(\n");
                sb.append("    var id: Long? = null,\n    var code: String? = null,\n    var name: String? = null,\n");
                sb.append("    var status: String? = null,\n    var createdAt: String? = null,\n    var updatedAt: String? = null,\n");
                sb.append("    var remark: String? = null,\n    var operator: String? = null\n");
                sb.append(")\n");
            }
            case ".cs" -> {
                sb.append("public class ").append(entity).append(" {\n");
                sb.append("    public long Id { get; set; }\n");
                for (String f : new String[]{"Code", "Name", "Status", "CreatedAt", "UpdatedAt", "Remark", "Operator"}) {
                    sb.append("    public string ").append(f).append(" { get; set; }\n");
                }
                sb.append("}\n");
            }
            case ".go" -> {

                sb.append("type ").append(entity).append(" struct {\n");
                sb.append("\tID        int64  `json:\"id\"`\n");
                sb.append("\tCode      string `json:\"code\"`\n");
                sb.append("\tName      string `json:\"name\"`\n");
                sb.append("\tStatus    string `json:\"status\"`\n");
                sb.append("\tCreatedAt string `json:\"createdAt\"`\n");
                sb.append("\tUpdatedAt string `json:\"updatedAt\"`\n");
                sb.append("\tRemark    string `json:\"remark\"`\n");
                sb.append("\tOperator  string `json:\"operator\"`\n");
                sb.append("}\n");
            }
            case ".cpp" -> {
                sb.append("#include <string>\nusing namespace std;\n\n");
                sb.append("class ").append(entity).append(" {\n");
                sb.append("public:\n    long id;\n    string code, name, status, createdAt, updatedAt, remark, op;\n");
                sb.append("};\n");
            }
            case ".c" -> {
                sb.append("typedef struct {\n");
                sb.append("    long id;\n    char code[64]; char name[64]; char status[32];\n");
                sb.append("    char createdAt[32]; char updatedAt[32]; char remark[256]; char operator[64];\n");
                sb.append("} ").append(entity).append(";\n");
            }
            case ".js" -> {
                sb.append("class ").append(entity).append(" {\n");
                sb.append("    constructor() {\n");
                for (String f : fields) sb.append("        this.").append(f).append(" = null;\n");
                sb.append("    }\n}\n");
            }
            case ".ts" -> {
                sb.append("export class ").append(entity).append(" {\n");
                for (String f : fields) sb.append("    ").append(f).append(": ").append(tsType(f)).append(" | null = null;\n");
                sb.append("}\n");
            }
            case ".dart" -> {
                sb.append("class ").append(entity).append(" {\n");
                for (String f : fields) sb.append("  ").append(dartType(f)).append("? ").append(f).append(";\n");
                sb.append("}\n");
            }
            case ".m" -> {
                sb.append("@interface ").append(entity).append(" : NSObject\n");
                sb.append("@property (nonatomic, assign) long long id;\n");
                for (String f : new String[]{"code", "name", "status", "createdAt", "updatedAt", "remark", "operator"}) {
                    sb.append("@property (nonatomic, copy) NSString *").append(f).append(";\n");
                }
                sb.append("@end\n");
            }
            case ".php" -> {
                sb.append("<?php\nclass ").append(entity).append(" {\n");
                for (String f : fields) sb.append("    public $").append(f).append(";\n");
                sb.append("}\n");
            }
            case ".py" -> {
                sb.append("class ").append(entity).append(":\n");
                sb.append("    def __init__(self):\n");
                for (String f : fields) sb.append("        self.").append(f).append(" = None\n");
            }
            case ".rb" -> {
                sb.append("class ").append(entity).append("\n");
                sb.append("  attr_accessor :id, :code, :name, :status, :created_at, :updated_at, :remark, :operator\n");
                sb.append("end\n");
            }
            case ".vb" -> {
                sb.append("Public Class ").append(entity).append("\n");
                for (String f : new String[]{"Id", "Code", "Name", "Status", "CreatedAt", "UpdatedAt", "Remark", "Operator"}) {
                    sb.append("    Public Property ").append(f).append(" As String\n");
                }
                sb.append("End Class\n");
            }
            case ".st" -> {
                sb.append("TYPE ").append(entity).append("\n");
                sb.append("    id : INT;\n    code : STRING;\n    name : STRING;\n    status : INT;\n");
                sb.append("END_TYPE\n");
            }
            default -> sb.append("// entity placeholder\n");
        }
        closePackage(s, sb);
        return sb.toString();
    }

    // ===================== 服务文件 =====================

    private static String serviceFile(LangSpec s, String pkg, String entity, String dom) {
        StringBuilder sb = new StringBuilder();
        sb.append(fileHeader(s, "src/" + pkg + "/service/" + entity + "Service" + s.ext())).append("\n");
        appendPackage(s, sb, pkg + ".service");
        switch (s.ext()) {
            case ".java" -> {
                sb.append("import java.util.ArrayList;\nimport java.util.List;\n\n");
                sb.append("public class ").append(entity).append("Service {\n");
                sb.append("    public ").append(entity).append(" getById(Long id) { return null; }\n");
                sb.append("    public List<").append(entity).append("> list() { return new ArrayList<>(); }\n");
                sb.append("    public void save(").append(entity).append(" e) { }\n");
                sb.append("    public void delete(Long id) { }\n");
                sb.append("}\n");
            }
            case ".kt" -> {
                sb.append("class ").append(entity).append("Service {\n");
                sb.append("    fun getById(id: Long): ").append(entity).append("? = null\n");
                sb.append("    fun list(): List<").append(entity).append("> = emptyList()\n");
                sb.append("    fun save(e: ").append(entity).append(") {}\n");
                sb.append("    fun delete(id: Long) {}\n");
                sb.append("}\n");
            }
            case ".cs" -> {
                sb.append("using System.Collections.Generic;\n\n");
                sb.append("public class ").append(entity).append("Service {\n");
                sb.append("    public ").append(entity).append(" GetById(long id) => null;\n");
                sb.append("    public List<").append(entity).append("> List() => new List<").append(entity).append(">();\n");
                sb.append("    public void Save(").append(entity).append(" e) {}\n");
                sb.append("}\n");
            }
            case ".go" -> {

                sb.append("type ").append(entity).append("Service struct{}\n\n");
                sb.append("func (s *").append(entity).append("Service) GetById(id int64) *").append(entity).append(" { return nil }\n");
                sb.append("func (s *").append(entity).append("Service) List() []*").append(entity).append(" { return nil }\n");
                sb.append("func (s *").append(entity).append("Service) Save(e *").append(entity).append(") {}\n");
            }
            case ".cpp" -> {
                sb.append("#include <vector>\nusing namespace std;\n\n");
                sb.append("class ").append(entity).append("Service {\n");
                sb.append("public:\n    ").append(entity).append("* getById(long id) { return nullptr; }\n");
                sb.append("    vector<").append(entity).append("> list() { return {}; }\n");
                sb.append("    void save(").append(entity).append(" e) {}\n");
                sb.append("};\n");
            }
            case ".c" -> {
                sb.append("void ").append(entity.toLowerCase(Locale.ROOT)).append("_save(").append(entity).append("* e) {}\n");
            }
            case ".js" -> {
                sb.append("class ").append(entity).append("Service {\n");
                sb.append("    getById(id) { return null; }\n");
                sb.append("    list() { return []; }\n");
                sb.append("    save(e) {}\n");
                sb.append("    delete(id) {}\n");
                sb.append("}\n");
            }
            case ".ts" -> {
                sb.append("export class ").append(entity).append("Service {\n");
                sb.append("    getById(id: number): ").append(entity).append(" | null { return null; }\n");
                sb.append("    list(): ").append(entity).append("[] { return []; }\n");
                sb.append("    save(e: ").append(entity).append("): void {}\n");
                sb.append("    delete(id: number): void {}\n");
                sb.append("}\n");
            }
            case ".dart" -> {
                sb.append("class ").append(entity).append("Service {\n");
                sb.append("  ").append(entity).append("? getById(int id) => null;\n");
                sb.append("  List<").append(entity).append("> list() => [];\n");
                sb.append("  void save(").append(entity).append(" e) {}\n");
                sb.append("}\n");
            }
            case ".m" -> {
                sb.append("@interface ").append(entity).append("Service : NSObject\n");
                sb.append("- (").append(entity).append(" *)getById:(long long)id;\n");
                sb.append("- (NSArray<").append(entity).append(" *> *)list;\n");
                sb.append("- (void)save:(").append(entity).append(" *)e;\n");
                sb.append("@end\n");
            }
            case ".php" -> {
                sb.append("<?php\nclass ").append(entity).append("Service {\n");
                sb.append("    public function getById($id) { return null; }\n");
                sb.append("    public function list() { return []; }\n");
                sb.append("    public function save($e) {}\n");
                sb.append("}\n");
            }
            case ".py" -> {
                sb.append("class ").append(entity).append("Service:\n");
                sb.append("    def get_by_id(self, id): return None\n");
                sb.append("    def list(self): return []\n");
                sb.append("    def save(self, e): pass\n");
                sb.append("    def delete(self, id): pass\n");
            }
            case ".rb" -> {
                sb.append("class ").append(entity).append("Service\n");
                sb.append("  def get_by_id(id); nil; end\n");
                sb.append("  def list; []; end\n");
                sb.append("  def save(e); end\n");
                sb.append("  def delete(id); end\n");
                sb.append("end\n");
            }
            case ".vb" -> {
                sb.append("Public Class ").append(entity).append("Service\n");
                sb.append("    Public Function GetById(id As Long) As ").append(entity).append("\n        Return Nothing\n    End Function\n");
                sb.append("    Public Function List() As List(Of ").append(entity).append(")\n        Return New List(Of ").append(entity).append(")()\n    End Function\n");
                sb.append("    Public Sub Save(e As ").append(entity).append(")\n    End Sub\n");
                sb.append("End Class\n");
            }
            case ".st" -> {
                sb.append("FUNCTION_BLOCK ").append(entity).append("Service\n");
                sb.append("VAR_INPUT\n    id : INT;\nEND_VAR\n");
                sb.append("END_FUNCTION_BLOCK\n");
            }
            default -> sb.append("// service placeholder\n");
        }
        closePackage(s, sb);
        return sb.toString();
    }

    // ===================== 控制器/路由 =====================

    private static String controllerFile(LangSpec s, String pkg, String entity, String dom) {
        StringBuilder sb = new StringBuilder();
        sb.append(fileHeader(s, "src/" + pkg + "/controller/" + entity + "Controller" + s.ext())).append("\n");
        appendPackage(s, sb, pkg + ".controller");
        String route = "/" + entity.toLowerCase(Locale.ROOT);
        switch (s.ext()) {
            case ".java" -> {
                sb.append("import org.springframework.web.bind.annotation.*;\nimport java.util.ArrayList;\n\n");
                sb.append("@RestController\n@RequestMapping(\"").append(route).append("\")\n");
                sb.append("public class ").append(entity).append("Controller {\n");
                sb.append("    @GetMapping\n    public Object list() { return new ArrayList<>(); }\n");
                sb.append("    @PostMapping\n    public void save(@RequestBody ").append(entity).append(" e) {}\n");
                sb.append("    @DeleteMapping(\"/{id}\")\n    public void delete(@PathVariable Long id) {}\n");
                sb.append("}\n");
            }
            case ".kt" -> {
                sb.append("import org.springframework.web.bind.annotation.*\n\n");
                sb.append("@RestController\n@RequestMapping(\"").append(route).append("\")\n");
                sb.append("class ").append(entity).append("Controller {\n");
                sb.append("    @GetMapping fun list() = emptyList<").append(entity).append(">()\n");
                sb.append("    @PostMapping fun save(@RequestBody e: ").append(entity).append(") {}\n");
                sb.append("    @DeleteMapping(\"/{id}\") fun delete(@PathVariable id: Long) {}\n");
                sb.append("}\n");
            }
            case ".cs" -> {
                sb.append("using Microsoft.AspNetCore.Mvc;\n\n");
                sb.append("[ApiController]\n[Route(\"").append(route).append("\")]\n");
                sb.append("public class ").append(entity).append("Controller : ControllerBase {\n");
                sb.append("    [HttpGet] public IActionResult List() => Ok(new object[0]);\n");
                sb.append("    [HttpPost] public IActionResult Save([FromBody] ").append(entity).append(" e) => Ok();\n");
                sb.append("}\n");
            }
            case ".go" -> {

                sb.append("import \"net/http\"\n\n");
                sb.append("func ").append(entity).append("List(w http.ResponseWriter, r *http.Request) {}\n");
                sb.append("func ").append(entity).append("Save(w http.ResponseWriter, r *http.Request) {}\n");
                sb.append("func ").append(entity).append("Delete(w http.ResponseWriter, r *http.Request) {}\n");
            }
            case ".cpp" -> {
                sb.append("// HTTP route handlers for ").append(entity).append("\n");
                sb.append("void ").append(entity.toLowerCase(Locale.ROOT)).append("_list() {}\n");
                sb.append("void ").append(entity.toLowerCase(Locale.ROOT)).append("_save() {}\n");
            }
            case ".c" -> {
                sb.append("// HTTP route handlers for ").append(entity).append("\n");
                sb.append("void ").append(entity.toLowerCase(Locale.ROOT)).append("_list() {}\n");
            }
            case ".js" -> {
                sb.append("const express = require('express');\n");
                sb.append("const router = express.Router();\n\n");
                sb.append("router.get('").append(route).append("', (req, res) => res.json([]));\n");
                sb.append("router.post('").append(route).append("', (req, res) => res.json({}));\n");
                sb.append("module.exports = router;\n");
            }
            case ".ts" -> {
                sb.append("import { Router } from 'express';\n");
                sb.append("const router = Router();\n\n");
                sb.append("router.get('").append(route).append("', (req, res) => res.json([]));\n");
                sb.append("router.post('").append(route).append("', (req, res) => res.json({}));\n");
                sb.append("export default router;\n");
            }
            case ".dart" -> {
                sb.append("import 'package:shelf/shelf.dart' as shelf;\n\n");
                sb.append("shelf.Response list(shelf.Request request) => shelf.Response.ok([]);\n");
                sb.append("shelf.Response save(shelf.Request request) => shelf.Response.ok(null);\n");
            }
            case ".m" -> {
                sb.append("#import <Foundation/Foundation.h>\n\n");
                sb.append("@interface ").append(entity).append("Controller : NSObject\n");
                sb.append("- (NSArray *)list;\n");
                sb.append("- (void)save:(id)e;\n");
                sb.append("@end\n");
            }
            case ".php" -> {
                sb.append("<?php\n// Route: ").append(route).append("\n");
                sb.append("header('Content-Type: application/json');\n");
                sb.append("echo json_encode([]);\n");
            }
            case ".py" -> {
                sb.append("class ").append(entity).append("Controller:\n");
                sb.append("    def list(self): return {\"code\": 0, \"data\": []}\n");
                sb.append("    def save(self, e): return {\"code\": 0}\n");
                sb.append("    def delete(self, id): return {\"code\": 0}\n");
            }
            case ".rb" -> {
                sb.append("class ").append(entity).append("Controller\n");
                sb.append("  def list; { code: 0, data: [] }; end\n");
                sb.append("  def save(e); { code: 0 }; end\n");
                sb.append("  def delete(id); { code: 0 }; end\n");
                sb.append("end\n");
            }
            case ".vb" -> {
                sb.append("Public Class ").append(entity).append("Controller\n");
                sb.append("    Public Function List() As Object\n        Return New Object() {}\n    End Function\n");
                sb.append("    Public Sub Save(e As Object)\n    End Sub\n");
                sb.append("End Class\n");
            }
            case ".st" -> {
                sb.append("FUNCTION_BLOCK ").append(entity).append("Controller\n");
                sb.append("VAR\n    request : STRING;\n    response : STRING;\nEND_VAR\n");
                sb.append("END_FUNCTION_BLOCK\n");
            }
            default -> sb.append("// controller placeholder\n");
        }
        closePackage(s, sb);
        return sb.toString();
    }

    // ===================== 包/命名空间辅助 =====================

    private static void appendPackage(LangSpec s, StringBuilder sb, String pkg) {
        if (s.packageKw().isEmpty()) return;
        if ("Namespace".equals(s.packageKw())) {
            sb.append(s.packageKw()).append(" ").append(pkg).append("\n");
        } else if ("namespace".equals(s.packageKw())) {
            sb.append(s.packageKw()).append(" ").append(pkg).append(" {\n");
        } else if ("package".equals(s.packageKw()) && ".go".equals(s.ext())) {
            sb.append("package ").append(pkg).append("\n");
        } else {
            sb.append(s.packageKw()).append(" ").append(pkg).append(";\n");
        }
    }

    private static void closePackage(LangSpec s, StringBuilder sb) {
        if ("namespace".equals(s.packageKw())) sb.append("}\n");
        else if ("Namespace".equals(s.packageKw())) sb.append("End Namespace\n");
    }

    // ===================== 类型映射 =====================

    private static String javaType(String f) {
        return "id".equals(f) ? "Long" : "String";
    }

    private static String tsType(String f) {
        return "id".equals(f) ? "number" : "string";
    }

    private static String dartType(String f) {
        return "id".equals(f) ? "int" : "String";
    }
}
