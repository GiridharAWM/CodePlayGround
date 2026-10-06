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
 * Servlet handling user registration using JDBC and MySQL.
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register", "/RegisterServlet"})
public class RegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendRedirect("register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || "true".equalsIgnoreCase(request.getParameter("ajax"));

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            sendError(request, response, "All fields are required.", isAjax);
            return;
        }

        name = name.trim();
        email = email.trim().toLowerCase();

        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.getConnection();

            // Check if email already exists
            checkStmt = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
            checkStmt.setString(1, email);
            rs = checkStmt.executeQuery();
            if (rs.next()) {
                sendError(request, response, "An account with this email already exists.", isAjax);
                return;
            }

            // Insert new user
            insertStmt = conn.prepareStatement(
                "INSERT INTO users (name, email, password) VALUES (?, ?, ?)", 
                Statement.RETURN_GENERATED_KEYS
            );
            insertStmt.setString(1, name);
            insertStmt.setString(2, email);
            insertStmt.setString(3, password);
            insertStmt.executeUpdate();

            ResultSet genKeys = insertStmt.getGeneratedKeys();
            int newUserId = 0;
            if (genKeys.next()) {
                newUserId = genKeys.getInt(1);
            }

            // Establish authenticated session
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", newUserId);
            session.setAttribute("userName", name);
            session.setAttribute("userEmail", email);

            if (isAjax) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":true,\"message\":\"Registration successful!\",\"redirect\":\"playground.jsp\"}");
            } else {
                response.sendRedirect("playground.jsp");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(request, response, "Database error during registration: " + e.getMessage(), isAjax);
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (checkStmt != null) try { checkStmt.close(); } catch (Exception ignored) {}
            if (insertStmt != null) try { insertStmt.close(); } catch (Exception ignored) {}
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
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
