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
 * Servlet handling user authentication and session management.
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/LoginServlet"})
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || "true".equalsIgnoreCase(request.getParameter("ajax"));

        if (email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            sendError(request, response, "Please enter both email and password.", isAjax);
            return;
        }

        email = email.trim().toLowerCase();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();
            stmt = conn.prepareStatement("SELECT id, name, email FROM users WHERE email = ? AND password = ?");
            stmt.setString(1, email);
            stmt.setString(2, password);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String userName = rs.getString("name");

                HttpSession session = request.getSession(true);
                session.setAttribute("userId", userId);
                session.setAttribute("userName", userName);
                session.setAttribute("userEmail", email);

                if (isAjax) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":true,\"message\":\"Login successful!\",\"redirect\":\"playground.jsp\"}");
                } else {
                    response.sendRedirect("playground.jsp");
                }
            } else {
                sendError(request, response, "Invalid email or password. Please try again.", isAjax);
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(request, response, "Database error during login: " + e.getMessage(), isAjax);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            DBConnection.close(conn);
        }
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response, String message, boolean isAjax) 
            throws IOException, ServletException {
        if (isAjax) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"" + escape(message) + "\"}");
        } else {
            request.setAttribute("errorMessage", message);
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
