package com.codevisualizer.tracer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance Java Execution Tracer runtime.
 * Injected into the user's execution environment to capture step-by-step
 * execution events (variables, arrays, loops, call stacks, conditions, output).
 */
public class Tracer {

    public static class Event {
        public int step;
        public int line;
        public String type;
        public String name;
        public String value;
        public String prevValue;
        public String description;
        public int callDepth;
        public String methodName;
        public Integer index;

        public Event(int step, int line, String type, String description) {
            this.step = step;
            this.line = line;
            this.type = type;
            this.description = description;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"step\":").append(step).append(",");
            sb.append("\"line\":").append(line).append(",");
            sb.append("\"type\":").append(quote(type)).append(",");
            if (name != null) sb.append("\"name\":").append(quote(name)).append(",");
            if (value != null) sb.append("\"value\":").append(quote(value)).append(",");
            if (prevValue != null) sb.append("\"prevValue\":").append(quote(prevValue)).append(",");
            if (methodName != null) sb.append("\"methodName\":").append(quote(methodName)).append(",");
            if (index != null) sb.append("\"index\":").append(index).append(",");
            sb.append("\"callDepth\":").append(callDepth).append(",");
            sb.append("\"description\":").append(quote(description));
            sb.append("}");
            return sb.toString();
        }

        private static String quote(String string) {
            if (string == null) return "null";
            StringBuilder sb = new StringBuilder("\"");
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
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
    }

    private static final List<Event> events = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger stepCounter = new AtomicInteger(0);
    private static final Map<String, String> varStore = Collections.synchronizedMap(new HashMap<>());
    private static final AtomicInteger currentCallDepth = new AtomicInteger(0);
    private static volatile String activeMethod = "main";
    private static final int MAX_EVENTS = 800;
    private static boolean initialized = false;
    private static PrintStream originalOut;
    private static ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        originalOut = System.out;

        // Custom PrintStream to capture console output and mirror to stdout
        OutputStream teeOut = new OutputStream() {
            private StringBuilder currentLine = new StringBuilder();

            @Override
            public void write(int b) {
                originalOut.write(b);
                capturedOut.write(b);
                if (b == '\n') {
                    String lineText = currentLine.toString().replace("\r", "");
                    currentLine.setLength(0);
                    output(0, lineText);
                } else {
                    currentLine.append((char) b);
                }
            }
        };

        System.setOut(new PrintStream(teeOut, true));

        // Hook for shutdown / exceptions
        Runtime.getRuntime().addShutdownHook(new Thread(Tracer::dump));
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            exception(e);
            dump();
        });
    }

    public static void start(int line) {
        init();
        Event ev = new Event(stepCounter.incrementAndGet(), line, "PROGRAM_START", "Program execution started in main()");
        ev.callDepth = currentCallDepth.get();
        ev.methodName = "main";
        addEvent(ev);
    }

    public static void end(int line) {
        Event ev = new Event(stepCounter.incrementAndGet(), line, "PROGRAM_END", "Program execution completed successfully");
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
        dump();
    }

    public static void line(int line, String snippet) {
        Event ev = new Event(stepCounter.incrementAndGet(), line, "LINE_EXECUTION", "Executing: " + snippet);
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
    }

    public static void var(int line, String name, Object val) {
        String strVal = formatValue(val);
        String prev = varStore.get(name);
        String eventType = (prev == null) ? "VARIABLE" : "VARIABLE_CHANGE";
        String desc = (prev == null) 
            ? "Variable '" + name + "' initialized to " + strVal
            : "Variable '" + name + "' changed from " + prev + " to " + strVal;

        varStore.put(name, strVal);
        Event ev = new Event(stepCounter.incrementAndGet(), line, eventType, desc);
        ev.name = name;
        ev.value = strVal;
        ev.prevValue = prev;
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
    }

    public static void array(int line, String name, Object arr) {
        String strVal = formatArray(arr);
        String prev = varStore.get(name);
        String eventType = (prev == null) ? "ARRAY_CREATE" : "ARRAY_CHANGE";
        String desc = (prev == null)
            ? "Array '" + name + "' initialized with values " + strVal
            : "Array '" + name + "' updated to " + strVal;

        varStore.put(name, strVal);
        Event ev = new Event(stepCounter.incrementAndGet(), line, eventType, desc);
        ev.name = name;
        ev.value = strVal;
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
    }

    public static void arrayAccess(int line, String name, int index, Object val) {
        String strVal = formatValue(val);
        Event ev = new Event(stepCounter.incrementAndGet(), line, "ARRAY_ACCESS", "Accessed " + name + "[" + index + "] = " + strVal);
        ev.name = name;
        ev.index = index;
        ev.value = strVal;
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
    }

    public static boolean condition(int line, String expr, boolean result) {
        Event ev = new Event(stepCounter.incrementAndGet(), line, "CONDITION", 
            "Condition (" + expr + ") evaluated to " + (result ? "TRUE" : "FALSE"));
        ev.name = expr;
        ev.value = result ? "TRUE" : "FALSE";
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
        return result;
    }

    public static void loopIteration(int line, String loopType, String info) {
        Event ev = new Event(stepCounter.incrementAndGet(), line, "LOOP_ITERATION", "Loop iteration: " + info);
        ev.name = loopType;
        ev.value = info;
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
    }

    public static void methodEntry(int line, String methodName, String params) {
        activeMethod = methodName;
        int depth = currentCallDepth.incrementAndGet();
        Event ev = new Event(stepCounter.incrementAndGet(), line, "METHOD_ENTRY", 
            "Invoked method " + methodName + "(" + (params != null ? params : "") + ")");
        ev.name = methodName;
        ev.value = params;
        ev.methodName = methodName;
        ev.callDepth = depth;
        addEvent(ev);
    }

    public static <T> T methodExit(int line, String methodName, T returnVal) {
        String retStr = formatValue(returnVal);
        int depth = currentCallDepth.getAndDecrement();
        Event ev = new Event(stepCounter.incrementAndGet(), line, "METHOD_EXIT", 
            "Method " + methodName + " returned " + retStr);
        ev.name = methodName;
        ev.value = retStr;
        ev.methodName = methodName;
        ev.callDepth = Math.max(0, depth);
        addEvent(ev);
        return returnVal;
    }

    public static void objectCreate(int line, String name, Object obj) {
        String strVal = formatValue(obj);
        Event ev = new Event(stepCounter.incrementAndGet(), line, "OBJECT_CREATE", "Instantiated object: " + name + " (" + strVal + ")");
        ev.name = name;
        ev.value = strVal;
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
    }

    public static void output(int line, String text) {
        if (text == null || text.trim().isEmpty()) return;
        Event ev = new Event(stepCounter.incrementAndGet(), line, "OUTPUT", "Console Output: " + text);
        ev.value = text;
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
    }

    public static void exception(Throwable t) {
        if (t == null) return;
        int line = 0;
        StackTraceElement[] trace = t.getStackTrace();
        if (trace != null && trace.length > 0) {
            for (StackTraceElement elem : trace) {
                if ("Main.java".equalsIgnoreCase(elem.getFileName()) || elem.getClassName().contains("Main")) {
                    line = elem.getLineNumber();
                    break;
                }
            }
            if (line == 0 && trace.length > 0) {
                line = trace[0].getLineNumber();
            }
        }
        String type = t.getClass().getSimpleName();
        String msg = t.getMessage();
        Event ev = new Event(stepCounter.incrementAndGet(), line, "EXCEPTION", 
            "Exception occurred: " + type + " (" + (msg != null ? msg : "no message") + ")");
        ev.name = type;
        ev.value = msg != null ? msg : type;
        ev.callDepth = currentCallDepth.get();
        ev.methodName = activeMethod;
        addEvent(ev);
    }

    private static void addEvent(Event ev) {
        if (events.size() < MAX_EVENTS) {
            events.add(ev);
        }
    }

    public static synchronized void dump() {
        try {
            File outFile = new File("trace.json");
            try (FileWriter writer = new FileWriter(outFile)) {
                writer.write("[\n");
                for (int i = 0; i < events.size(); i++) {
                    writer.write(events.get(i).toJson());
                    if (i < events.size() - 1) writer.write(",\n");
                }
                writer.write("\n]");
            }
        } catch (Exception e) {
            // Ignore during shutdown
        }
    }

    private static String formatValue(Object val) {
        if (val == null) return "null";
        if (val.getClass().isArray()) {
            return formatArray(val);
        }
        if (val instanceof String) {
            return "\"" + val + "\"";
        }
        if (val instanceof Character) {
            return "'" + val + "'";
        }
        return String.valueOf(val);
    }

    private static String formatArray(Object arr) {
        if (arr == null) return "null";
        if (!arr.getClass().isArray()) return String.valueOf(arr);
        int length = Array.getLength(arr);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            Object elem = Array.get(arr, i);
            sb.append(formatValue(elem));
            if (i < length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
