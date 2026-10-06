package com.codevisualizer.tracer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intelligent Java Source Code Instrumenter with brace-depth tracking.
 * Preserves program semantics, tracks variables, arrays, conditions,
 * loops, and method call stacks.
 */
public class JavaSourceInstrumenter {

    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "^\\s*(?:public|protected|private|static|final|synchronized|\\s)*\\s+([A-Za-z0-9_<>\\[\\]]+)\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(([^)]*)\\)\\s*(?:throws [^\\{]+)?\\s*\\{\\s*$"
    );

    private static final Pattern VAR_DECL_PATTERN = Pattern.compile(
        "^\\s*(?:(?:int|long|double|float|boolean|char|byte|short|String|var|[A-Za-z0-9_<>]+)(?:\\[\\])*)\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*=\\s*(.+?);\\s*$"
    );

    private static final Pattern VAR_ASSIGN_PATTERN = Pattern.compile(
        "^\\s*([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*(=|\\+=|-=|\\*=|/=|%=)\\s*(.+?);\\s*$"
    );

    private static final Pattern INC_DEC_PATTERN = Pattern.compile(
        "^\\s*([a-zA-Z_$][a-zA-Z0-9_$]*)(?:\\+\\+|--);\\s*$"
    );

    public static String instrument(String originalSource) {
        if (originalSource == null || originalSource.trim().isEmpty()) {
            return originalSource;
        }

        StringBuilder out = new StringBuilder();
        out.append("import com.codevisualizer.tracer.Tracer;\n");

        String[] lines = originalSource.split("\r?\n", -1);
        String currentMethod = null;
        int braceDepth = 0;
        int mainMethodDepth = -1;
        boolean inBlockComment = false;

        for (int i = 0; i < lines.length; i++) {
            int lineNum = i + 1;
            String line = lines[i];
            String trimmed = line.trim();

            // Comment handling
            if (inBlockComment) {
                if (trimmed.contains("*/")) inBlockComment = false;
                out.append(line).append("\n");
                continue;
            }
            if (trimmed.startsWith("/*")) {
                if (!trimmed.contains("*/")) inBlockComment = true;
                out.append(line).append("\n");
                continue;
            }
            if (trimmed.startsWith("//") || trimmed.startsWith("import ") || trimmed.startsWith("package ")) {
                out.append(line).append("\n");
                continue;
            }

            // 1. Method definitions
            Matcher methodMatcher = METHOD_PATTERN.matcher(trimmed);
            if (methodMatcher.matches()) {
                String methodName = methodMatcher.group(2);
                currentMethod = methodName;
                braceDepth++;

                out.append(line).append("\n");
                if ("main".equals(methodName)) {
                    mainMethodDepth = braceDepth;
                    out.append("        Tracer.start(").append(lineNum).append(");\n");
                } else {
                    String paramSummary = summarizeParams(methodMatcher.group(3));
                    out.append("        Tracer.methodEntry(").append(lineNum).append(", \"")
                       .append(methodName).append("\", ").append(paramSummary).append(");\n");
                }
                continue;
            }

            // Track braces
            int openBraces = countOccurrences(trimmed, '{');
            int closeBraces = countOccurrences(trimmed, '}');

            // 2. Conditionals: if (...)
            if (trimmed.startsWith("if ") || trimmed.startsWith("if(")) {
                String instrumentedIf = instrumentCondition(line, lineNum, "if");
                out.append(instrumentedIf).append("\n");
                braceDepth += (openBraces - closeBraces);
                continue;
            }

            // 3. Loops: while (...)
            if (trimmed.startsWith("while ") || trimmed.startsWith("while(")) {
                String instrumentedWhile = instrumentCondition(line, lineNum, "while");
                out.append(instrumentedWhile).append("\n");
                braceDepth += (openBraces - closeBraces);
                continue;
            }

            // 4. Loops: for (...)
            if (trimmed.startsWith("for ") || trimmed.startsWith("for(")) {
                out.append(line).append("\n");
                if (trimmed.endsWith("{")) {
                    out.append("        Tracer.loopIteration(").append(lineNum).append(", \"for\", \"line ").append(lineNum).append("\");\n");
                }
                braceDepth += (openBraces - closeBraces);
                continue;
            }

            // 5. Method returns
            if (trimmed.startsWith("return ") && trimmed.endsWith(";") && currentMethod != null && !"main".equals(currentMethod)) {
                String expr = trimmed.substring(7, trimmed.length() - 1).trim();
                if (!expr.isEmpty()) {
                    out.append("        return Tracer.methodExit(").append(lineNum).append(", \"")
                       .append(currentMethod).append("\", (").append(expr).append("));\n");
                    braceDepth += (openBraces - closeBraces);
                    continue;
                }
            }

            // 6. Variable declarations
            Matcher varDecl = VAR_DECL_PATTERN.matcher(trimmed);
            if (varDecl.matches() && !trimmed.startsWith("return")) {
                String varName = varDecl.group(1);
                out.append(line).append("\n");
                if (trimmed.contains("[]")) {
                    out.append("        Tracer.array(").append(lineNum).append(", \"")
                       .append(varName).append("\", ").append(varName).append(");\n");
                } else {
                    out.append("        Tracer.var(").append(lineNum).append(", \"")
                       .append(varName).append("\", ").append(varName).append(");\n");
                }
                braceDepth += (openBraces - closeBraces);
                continue;
            }

            // 7. Variable assignment
            Matcher varAssign = VAR_ASSIGN_PATTERN.matcher(trimmed);
            if (varAssign.matches() && !trimmed.contains("==")) {
                String varName = varAssign.group(1);
                out.append(line).append("\n");
                out.append("        Tracer.var(").append(lineNum).append(", \"")
                   .append(varName).append("\", ").append(varName).append(");\n");
                braceDepth += (openBraces - closeBraces);
                continue;
            }

            // 8. Increment / Decrement
            Matcher incDec = INC_DEC_PATTERN.matcher(trimmed);
            if (incDec.matches()) {
                String varName = incDec.group(1);
                out.append(line).append("\n");
                out.append("        Tracer.var(").append(lineNum).append(", \"")
                   .append(varName).append("\", ").append(varName).append(");\n");
                braceDepth += (openBraces - closeBraces);
                continue;
            }

            // Check if this line is closing main method
            if (closeBraces > 0 && mainMethodDepth != -1 && (braceDepth - closeBraces < mainMethodDepth)) {
                out.append("        Tracer.end(").append(lineNum).append(");\n");
                mainMethodDepth = -1;
                currentMethod = null;
            }

            braceDepth += (openBraces - closeBraces);
            out.append(line).append("\n");
        }

        return out.toString();
    }

    private static String instrumentCondition(String line, int lineNum, String keyword) {
        int kwIdx = line.indexOf(keyword);
        if (kwIdx < 0) return line;

        int openParen = line.indexOf('(', kwIdx);
        if (openParen < 0) return line;

        int closeParen = findMatchingParen(line, openParen);
        if (closeParen < 0) return line;

        String conditionExpr = line.substring(openParen + 1, closeParen).trim();
        if (conditionExpr.isEmpty()) return line;

        String escapedExpr = conditionExpr.replace("\\", "\\\\").replace("\"", "\\\"");
        String prefix = line.substring(0, openParen + 1);
        String suffix = line.substring(closeParen);

        return prefix + "Tracer.condition(" + lineNum + ", \"" + escapedExpr + "\", (" + conditionExpr + "))" + suffix;
    }

    private static int findMatchingParen(String str, int openPos) {
        int depth = 0;
        for (int i = openPos; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static String summarizeParams(String paramsDecl) {
        if (paramsDecl == null || paramsDecl.trim().isEmpty()) {
            return "\"\"";
        }
        String[] parts = paramsDecl.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            String[] tokens = p.split("\\s+");
            if (tokens.length >= 2) {
                String pName = tokens[tokens.length - 1].replace("[]", "");
                if (sb.length() > 0) sb.append(" + \", \" + ");
                sb.append("\"").append(pName).append("=\" + ").append(pName);
            }
        }
        return sb.length() > 0 ? sb.toString() : "\"\"";
    }

    private static int countOccurrences(String str, char ch) {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"' && (i == 0 || str.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            }
            if (!inQuotes && c == ch) {
                count++;
            }
        }
        return count;
    }
}
