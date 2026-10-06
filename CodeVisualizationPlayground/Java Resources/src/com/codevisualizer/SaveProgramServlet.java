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
import java.sql.Statement;

/**
 * Servlet handling saving user programs via AJAX.
 */
@WebServlet(name = "SaveProgramServlet", urlPatterns = {"/save-program", "/SaveProgramServlet"})
public class SaveProgramServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        Integer userId = (session != null) ? (Integer) session.getAttribute("userId") : null;

        if (userId == null) {
            response.getWriter().write("{\"success\":false,\"error\":\"Please log in to save programs.\"}");
            return;
        }

        String programName = request.getParameter("program_name");
        String code = request.getParameter("code");

        if (programName == null || programName.trim().isEmpty()) {
            programName = "Untitled Program (" + System.currentTimeMillis() % 10000 + ")";
        }
        if (code == null || code.trim().isEmpty()) {
            response.getWriter().write("{\"success\":false,\"error\":\"Code cannot be empty.\"}");
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement(
                "INSERT INTO programs (user_id, program_name, code) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            stmt.setInt(1, userId);
            stmt.setString(2, programName.trim());
            stmt.setString(3, code);
            stmt.executeUpdate();

            rs = stmt.getGeneratedKeys();
            int newId = 0;
            if (rs.next()) {
                newId = rs.getInt(1);
            }

            response.getWriter().write("{\"success\":true,\"message\":\"Program saved successfully.\",\"id\":" + newId + "}");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"error\":\"Database error: " + escape(e.getMessage()) + "\"}");
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            DBConnection.close(conn);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
