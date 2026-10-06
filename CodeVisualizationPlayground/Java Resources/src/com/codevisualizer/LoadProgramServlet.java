package com.codevisualizer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Servlet for retrieving a specific saved program belonging to the authenticated user.
 */
@WebServlet(name = "LoadProgramServlet", urlPatterns = {"/load-program", "/LoadProgramServlet"})
public class LoadProgramServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        Integer userId = (session != null) ? (Integer) session.getAttribute("userId") : null;

        if (userId == null) {
            response.getWriter().write("{\"success\":false,\"error\":\"Please log in to load saved programs.\"}");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            response.getWriter().write("{\"success\":false,\"error\":\"Program ID is required.\"}");
            return;
        }

        int programId;
        try {
            programId = Integer.parseInt(idParam.trim());
        } catch (NumberFormatException e) {
            response.getWriter().write("{\"success\":false,\"error\":\"Invalid program ID format.\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement("SELECT id, program_name, code FROM programs WHERE id = ? AND user_id = ?");
            stmt.setInt(1, programId);
            stmt.setInt(2, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("program_name");
                String code = rs.getString("code");

                StringBuilder sb = new StringBuilder("{");
                sb.append("\"success\":true,");
                sb.append("\"id\":").append(programId).append(",");
                sb.append("\"name\":").append(escapeJson(name)).append(",");
                sb.append("\"code\":").append(escapeJson(code));
                sb.append("}");

                response.getWriter().write(sb.toString());
            } else {
                response.getWriter().write("{\"success\":false,\"error\":\"Program not found or unauthorized access.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            DBConnection.close(conn);
        }
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
}
