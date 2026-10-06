package com.codevisualizer;

/**
 * Encapsulates an execution step event captured during Java program execution.
 * Easily serializable to JSON without external libraries.
 */
public class TraceEvent {
    private int step;
    private int line;
    private String type;
    private String name;
    private String value;
    private String prevValue;
    private String description;
    private int callDepth;
    private String methodName;
    private Integer index;

    public TraceEvent() {}

    public TraceEvent(int step, int line, String type, String description) {
        this.step = step;
        this.line = line;
        this.type = type;
        this.description = description;
    }

    public TraceEvent(int step, int line, String type, String name, String value, String description) {
        this.step = step;
        this.line = line;
        this.type = type;
        this.name = name;
        this.value = value;
        this.description = description;
    }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getPrevValue() { return prevValue; }
    public void setPrevValue(String prevValue) { this.prevValue = prevValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCallDepth() { return callDepth; }
    public void setCallDepth(int callDepth) { this.callDepth = callDepth; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public Integer getIndex() { return index; }
    public void setIndex(Integer index) { this.index = index; }

    /**
     * Converts this event into a clean JSON object representation.
     */
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
