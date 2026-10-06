package com.codevisualizer;

import com.codevisualizer.tracer.JavaSourceInstrumenter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Servlet handling Java source compilation, sandboxed execution,
 * trace generation, and returning structured trace events via AJAX.
 */
@WebServlet(name = "RunCodeServlet", urlPatterns = {"/run-code", "/RunCodeServlet"})
public class RunCodeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final long TIMEOUT_SECONDS = 5;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        // Read source code from request body or parameter
        String sourceCode = request.getParameter("code");
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String body = sb.toString().trim();
            if (body.startsWith("{") && body.contains("\"code\"")) {
                // Simple JSON extraction to avoid external dependencies
                int idx = body.indexOf("\"code\":");
                if (idx != -1) {
                    int start = body.indexOf("\"", idx + 7);
                    int end = body.lastIndexOf("\"");
                    if (start != -1 && end > start) {
                        sourceCode = unescapeJson(body.substring(start + 1, end));
                    }
                }
            } else if (!body.isEmpty()) {
                sourceCode = body;
            }
        }

        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            response.getWriter().write("{\"success\":false,\"error\":\"Source code is required.\",\"events\":[]}");
            return;
        }

        // Security check for forbidden operations
        if (containsDangerousCalls(sourceCode)) {
            response.getWriter().write("{\"success\":false,\"error\":\"Security Violation: Access to system processes or exit calls is restricted.\",\"events\":[]}");
            return;
        }

        File tempDir = null;
        try {
            // Create unique isolated workspace
            tempDir = new File(System.getProperty("java.io.tmpdir"), "cvp_" + UUID.randomUUID().toString());
            if (!tempDir.mkdirs()) {
                response.getWriter().write("{\"success\":false,\"error\":\"Failed to create execution workspace.\",\"events\":[]}");
                return;
            }

            // Copy Tracer source code to tempDir so javac can compile it
            prepareTracerSource(tempDir);

            // 1. Try instrumented execution
            String instrumentedCode = JavaSourceInstrumenter.instrument(sourceCode);
            File mainJavaFile = new File(tempDir, "Main.java");
            try (FileWriter writer = new FileWriter(mainJavaFile)) {
                writer.write(instrumentedCode);
            }

            // Compile instrumented code
            ProcessBuilder compilePb = new ProcessBuilder(
                "javac", 
                "-cp", tempDir.getAbsolutePath(), 
                "com" + File.separator + "codevisualizer" + File.separator + "tracer" + File.separator + "Tracer.java", 
                "Main.java"
            );
            compilePb.directory(tempDir);
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            String compileOutput = readProcessOutput(compileProcess.getInputStream());
            boolean compileSuccess = compileProcess.waitFor(10, TimeUnit.SECONDS) && compileProcess.exitValue() == 0;

            // Fallback: If instrumented compilation fails, try compiling original code
            boolean isInstrumented = true;
            if (!compileSuccess) {
                isInstrumented = false;
                try (FileWriter writer = new FileWriter(mainJavaFile)) {
                    writer.write(sourceCode);
                }
                ProcessBuilder fallbackPb = new ProcessBuilder("javac", "Main.java");
                fallbackPb.directory(tempDir);
                fallbackPb.redirectErrorStream(true);
                Process fallbackProcess = fallbackPb.start();
                compileOutput = readProcessOutput(fallbackProcess.getInputStream());
                compileSuccess = fallbackProcess.waitFor(10, TimeUnit.SECONDS) && fallbackProcess.exitValue() == 0;

                if (!compileSuccess) {
                    // Compilation genuinely failed in original code
                    String sanitizedCompileError = sanitizeCompilerOutput(compileOutput, tempDir.getAbsolutePath());
                    response.getWriter().write("{\"success\":false,\"compilationError\":" + escapeJson(sanitizedCompileError) + ",\"events\":[]}");
                    return;
                }
            }

            // 2. Execute compiled class
            ProcessBuilder runPb = new ProcessBuilder("java", "-cp", tempDir.getAbsolutePath(), "Main");
            runPb.directory(tempDir);
            runPb.redirectErrorStream(false);
            Process runProcess = runPb.start();

            // Asynchronously read stdout and stderr
            StreamCollector stdoutCollector = new StreamCollector(runProcess.getInputStream());
            StreamCollector stderrCollector = new StreamCollector(runProcess.getErrorStream());
            stdoutCollector.start();
            stderrCollector.start();

            boolean finished = runProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                runProcess.destroyForcibly();
                response.getWriter().write("{\"success\":false,\"error\":\"Execution Timed Out (" + TIMEOUT_SECONDS + " seconds limit). Possible infinite loop detected.\",\"events\":[]}");
                return;
            }

            stdoutCollector.join(1000);
            stderrCollector.join(1000);

            String stdout = stdoutCollector.getOutput();
            String stderr = stderrCollector.getOutput();

            // 3. Read trace events
            File traceFile = new File(tempDir, "trace.json");
            String eventsJson;
            if (isInstrumented && traceFile.exists() && traceFile.length() > 0) {
                eventsJson = new String(Files.readAllBytes(traceFile.toPath()), "UTF-8").trim();
            } else {
                eventsJson = synthesizeEvents(stdout, stderr, sourceCode);
            }

            // Format response
            StringBuilder json = new StringBuilder("{");
            json.append("\"success\":true,");
            json.append("\"output\":").append(escapeJson(stdout)).append(",");
            if (stderr != null && !stderr.trim().isEmpty()) {
                json.append("\"runtimeError\":").append(escapeJson(stderr)).append(",");
            }
            json.append("\"events\":").append(eventsJson != null && !eventsJson.isEmpty() ? eventsJson : "[]");
            json.append("}");

            response.getWriter().write(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"error\":" + escapeJson("Server Execution Error: " + e.getMessage()) + ",\"events\":[]}");
        } finally {
            // Clean up temporary workspace
            if (tempDir != null && tempDir.exists()) {
                deleteDirectory(tempDir);
            }
        }
    }

    private void prepareTracerSource(File workspace) throws IOException {
        File tracerDir = new File(workspace, "com" + File.separator + "codevisualizer" + File.separator + "tracer");
        tracerDir.mkdirs();
        File tracerFile = new File(tracerDir, "Tracer.java");

        // Always write complete Tracer.java source so javac compiles all inner classes cleanly
        String tracerSource = getTracerSourceCode();
        try (FileWriter fw = new FileWriter(tracerFile)) {
            fw.write(tracerSource);
        }
    }

    private String getTracerSourceCode() {
        // Embedded minimal self-contained Tracer source for execution sandbox
        return "package com.codevisualizer.tracer;\n" +
            "import java.io.*;\n" +
            "import java.util.*;\n" +
            "import java.util.concurrent.atomic.AtomicInteger;\n" +
            "public class Tracer {\n" +
            "    public static class Event {\n" +
            "        public int step, line, callDepth;\n" +
            "        public String type, name, value, prevValue, description, methodName;\n" +
            "        public Integer index;\n" +
            "        public Event(int s, int l, String t, String d) { step = s; line = l; type = t; description = d; }\n" +
            "        public String toJson() {\n" +
            "            StringBuilder b = new StringBuilder(\"{\");\n" +
            "            b.append(\"\\\"step\\\":\").append(step).append(\",\");\n" +
            "            b.append(\"\\\"line\\\":\").append(line).append(\",\");\n" +
            "            b.append(\"\\\"type\\\":\\\"\").append(type).append(\"\\\",\");\n" +
            "            if (name != null) b.append(\"\\\"name\\\":\\\"\").append(esc(name)).append(\"\\\",\");\n" +
            "            if (value != null) b.append(\"\\\"value\\\":\\\"\").append(esc(value)).append(\"\\\",\");\n" +
            "            if (prevValue != null) b.append(\"\\\"prevValue\\\":\\\"\").append(esc(prevValue)).append(\"\\\",\");\n" +
            "            if (methodName != null) b.append(\"\\\"methodName\\\":\\\"\").append(esc(methodName)).append(\"\\\",\");\n" +
            "            if (index != null) b.append(\"\\\"index\\\":\").append(index).append(\",\");\n" +
            "            b.append(\"\\\"callDepth\\\":\").append(callDepth).append(\",\");\n" +
            "            b.append(\"\\\"description\\\":\\\"\").append(esc(description)).append(\"\\\"\");\n" +
            "            b.append(\"}\");\n" +
            "            return b.toString();\n" +
            "        }\n" +
            "        private String esc(String s) {\n" +
            "            if (s == null) return \"\";\n" +
            "            return s.replace(\"\\\\\", \"\\\\\\\\\").replace(\"\\\"\", \"\\\\\\\"\").replace(\"\\n\", \"\\\\n\").replace(\"\\r\", \"\");\n" +
            "        }\n" +
            "    }\n" +
            "    private static final List<Event> events = Collections.synchronizedList(new ArrayList<>());\n" +
            "    private static final AtomicInteger counter = new AtomicInteger(0);\n" +
            "    private static final Map<String, String> vars = Collections.synchronizedMap(new HashMap<>());\n" +
            "    private static final AtomicInteger depth = new AtomicInteger(0);\n" +
            "    private static volatile String activeMethod = \"main\";\n" +
            "    public static synchronized void init() {\n" +
            "        Runtime.getRuntime().addShutdownHook(new Thread(Tracer::dump));\n" +
            "    }\n" +
            "    public static void start(int l) {\n" +
            "        init();\n" +
            "        Event e = new Event(counter.incrementAndGet(), l, \"PROGRAM_START\", \"Program execution started\");\n" +
            "        e.callDepth = depth.get(); e.methodName = \"main\"; events.add(e);\n" +
            "    }\n" +
            "    public static void end(int l) {\n" +
            "        Event e = new Event(counter.incrementAndGet(), l, \"PROGRAM_END\", \"Program execution finished\");\n" +
            "        e.callDepth = depth.get(); e.methodName = activeMethod; events.add(e); dump();\n" +
            "    }\n" +
            "    public static void var(int l, String n, Object v) {\n" +
            "        String s = String.valueOf(v);\n" +
            "        String prev = vars.put(n, s);\n" +
            "        String type = (prev == null) ? \"VARIABLE\" : \"VARIABLE_CHANGE\";\n" +
            "        String desc = (prev == null) ? \"Variable '\" + n + \"' initialized to \" + s : \"Variable '\" + n + \"' updated from \" + prev + \" to \" + s;\n" +
            "        Event e = new Event(counter.incrementAndGet(), l, type, desc);\n" +
            "        e.name = n; e.value = s; e.prevValue = prev; e.callDepth = depth.get(); e.methodName = activeMethod;\n" +
            "        if (events.size() < 600) events.add(e);\n" +
            "    }\n" +
            "    public static void array(int l, String n, Object arr) {\n" +
            "        String s = (arr instanceof int[]) ? Arrays.toString((int[])arr) : String.valueOf(arr);\n" +
            "        String prev = vars.put(n, s);\n" +
            "        Event e = new Event(counter.incrementAndGet(), l, (prev == null) ? \"ARRAY_CREATE\" : \"ARRAY_CHANGE\", \"Array '\" + n + \"' = \" + s);\n" +
            "        e.name = n; e.value = s; e.callDepth = depth.get(); e.methodName = activeMethod;\n" +
            "        if (events.size() < 600) events.add(e);\n" +
            "    }\n" +
            "    public static boolean condition(int l, String expr, boolean res) {\n" +
            "        Event e = new Event(counter.incrementAndGet(), l, \"CONDITION\", \"Condition (\" + expr + \") evaluated to \" + (res ? \"TRUE\" : \"FALSE\"));\n" +
            "        e.name = expr; e.value = res ? \"TRUE\" : \"FALSE\"; e.callDepth = depth.get(); e.methodName = activeMethod;\n" +
            "        if (events.size() < 600) events.add(e); return res;\n" +
            "    }\n" +
            "    public static void loopIteration(int l, String type, String info) {\n" +
            "        Event e = new Event(counter.incrementAndGet(), l, \"LOOP_ITERATION\", \"Loop iteration at line \" + l);\n" +
            "        e.name = type; e.value = info; e.callDepth = depth.get(); e.methodName = activeMethod;\n" +
            "        if (events.size() < 600) events.add(e);\n" +
            "    }\n" +
            "    public static void methodEntry(int l, String m, String p) {\n" +
            "        activeMethod = m;\n" +
            "        int d = depth.incrementAndGet();\n" +
            "        Event e = new Event(counter.incrementAndGet(), l, \"METHOD_ENTRY\", \"Entered method \" + m + \"(\" + p + \")\");\n" +
            "        e.name = m; e.value = p; e.methodName = m; e.callDepth = d;\n" +
            "        if (events.size() < 600) events.add(e);\n" +
            "    }\n" +
            "    public static <T> T methodExit(int l, String m, T ret) {\n" +
            "        String s = String.valueOf(ret);\n" +
            "        int d = depth.getAndDecrement();\n" +
            "        Event e = new Event(counter.incrementAndGet(), l, \"METHOD_EXIT\", \"Method \" + m + \" returned \" + s);\n" +
            "        e.name = m; e.value = s; e.methodName = m; e.callDepth = Math.max(0, d);\n" +
            "        if (events.size() < 600) events.add(e); return ret;\n" +
            "    }\n" +
            "    public static synchronized void dump() {\n" +
            "        try (FileWriter w = new FileWriter(\"trace.json\")) {\n" +
            "            w.write(\"[\\n\");\n" +
            "            for (int i = 0; i < events.size(); i++) {\n" +
            "                w.write(events.get(i).toJson());\n" +
            "                if (i < events.size() - 1) w.write(\",\\n\");\n" +
            "            }\n" +
            "            w.write(\"\\n]\");\n" +
            "        } catch (Exception ignored) {}\n" +
            "    }\n" +
            "}\n";
    }

    private String synthesizeEvents(String stdout, String stderr, String code) {
        List<String> list = new ArrayList<>();
        int step = 1;
        list.add("{\"step\":" + (step++) + ",\"line\":1,\"type\":\"PROGRAM_START\",\"description\":\"Program execution started\",\"callDepth\":0}");

        if (stdout != null && !stdout.isEmpty()) {
            String[] lines = stdout.split("\r?\n");
            for (String l : lines) {
                if (!l.trim().isEmpty()) {
                    list.add("{\"step\":" + (step++) + ",\"line\":0,\"type\":\"OUTPUT\",\"value\":" + escapeJson(l) + ",\"description\":" + escapeJson("Console output: " + l) + ",\"callDepth\":0}");
                }
            }
        }

        if (stderr != null && !stderr.isEmpty()) {
            list.add("{\"step\":" + (step++) + ",\"line\":0,\"type\":\"EXCEPTION\",\"value\":" + escapeJson(stderr) + ",\"description\":" + escapeJson("Runtime Error: " + stderr) + ",\"callDepth\":0}");
        }

        list.add("{\"step\":" + (step) + ",\"line\":0,\"type\":\"PROGRAM_END\",\"description\":\"Program execution completed\",\"callDepth\":0}");

        return "[" + String.join(",", list) + "]";
    }

    private boolean containsDangerousCalls(String code) {
        String lower = code.toLowerCase();
        return lower.contains("processbuilder") 
            || lower.contains("runtime.getruntime().exec") 
            || lower.contains("system.exit");
    }

    private String readProcessOutput(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private String sanitizeCompilerOutput(String output, String tempPath) {
        if (output == null) return "";
        return output.replace(tempPath + File.separator, "")
                     .replace(tempPath + "/", "")
                     .replace("com/codevisualizer/tracer/", "");
    }

    private static void deleteDirectory(File dir) {
        try {
            Files.walk(dir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(java.nio.file.Path::toFile)
                .forEach(File::delete);
        } catch (Exception ignored) {}
    }

    private static String escapeJson(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String hex = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int k = 0; k < 4 - hex.length(); k++) sb.append('0');
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private static class StreamCollector extends Thread {
        private final InputStream is;
        private final StringBuilder sb = new StringBuilder();

        public StreamCollector(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        }

        public String getOutput() {
            return sb.toString();
        }
    }
}
