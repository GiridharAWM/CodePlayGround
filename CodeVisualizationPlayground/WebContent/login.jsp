<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String error = (String) request.getAttribute("errorMessage");
    String logoutMsg = request.getParameter("logout");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Code Visualization Playground</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;700&display=swap" rel="stylesheet">
</head>
<body>

    <header>
        <nav class="navbar">
            <a href="index.html" class="brand">
                <div class="brand-icon">&lt;/&gt;</div>
                <span>Code Visualizer</span>
            </a>
            <ul class="nav-links">
                <li><a href="index.html">Home</a></li>
                <li><a href="playground.jsp">Playground</a></li>
                <li><a href="concepts.jsp">Concepts</a></li>
            </ul>
            <div class="nav-actions">
                <button id="themeToggleBtn" class="btn-icon" title="Toggle Theme">☀️</button>
                <a href="register.jsp" class="btn btn-primary">Register</a>
            </div>
        </nav>
    </header>

    <main class="auth-wrapper">
        <div class="auth-card">
            <div class="auth-header">
                <h1 class="auth-title">Welcome Back</h1>
                <p class="auth-subtitle">Sign in to access your saved Java programs</p>
            </div>

            <% if (error != null && !error.isEmpty()) { %>
                <div class="alert alert-danger" id="loginAlert">
                    <span>⚠️ <%= error %></span>
                </div>
            <% } %>

            <% if ("true".equals(logoutMsg)) { %>
                <div class="alert alert-success" id="logoutAlert">
                    <span>✓ You have been logged out successfully.</span>
                </div>
            <% } %>

            <form action="login" method="POST" id="loginForm">
                <div class="form-group">
                    <label for="email" class="form-label">Email Address</label>
                    <input type="email" id="email" name="email" class="form-input" placeholder="developer@example.com" required autocomplete="email">
                </div>

                <div class="form-group">
                    <label for="password" class="form-label">Password</label>
                    <input type="password" id="password" name="password" class="form-input" placeholder="••••••••" required autocomplete="current-password">
                </div>

                <button type="submit" id="loginButton" class="btn btn-primary" style="width: 100%; margin-top: 1rem; padding: 0.75rem;">
                    Login
                </button>
            </form>

            <div class="form-footer">
                Don't have an account? <a href="register.jsp">Create an account</a>
            </div>
        </div>
    </main>

    <script src="js/main.js"></script>
</body>
</html>
