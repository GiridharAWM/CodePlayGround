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

/**
 * Servlet for deleting a user's saved program.
 */
@WebServlet(name = "DeleteProgramServlet", urlPatterns = {"/delete-program", "/DeleteProgramServlet"})
public class DeleteProgramServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        HttpSession session = request.getSession(false);
        Integer userId = (session != null) ? (Integer) session.getAttribute("userId") : null;

        if (userId == null) {
            response.getWriter().write("{\"success\":false,\"error\":\"Please log in to delete programs.\"}");
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

        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement("DELETE FROM programs WHERE id = ? AND user_id = ?");
            stmt.setInt(1, programId);
            stmt.setInt(2, userId);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                response.getWriter().write("{\"success\":true,\"message\":\"Program deleted successfully.\",\"id\":" + programId + "}");
            } else {
                response.getWriter().write("{\"success\":false,\"error\":\"Program not found or unauthorized access.\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("{\"success\":false,\"error\":\"Database error: " + escape(e.getMessage()) + "\"}");
        } finally {
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            DBConnection.close(conn);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
