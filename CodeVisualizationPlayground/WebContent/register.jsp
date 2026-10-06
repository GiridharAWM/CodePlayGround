<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String error = (String) request.getAttribute("errorMessage");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register - Code Visualization Playground</title>
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
                <a href="login.jsp" class="btn btn-secondary">Login</a>
            </div>
        </nav>
    </header>

    <main class="auth-wrapper">
        <div class="auth-card">
            <div class="auth-header">
                <h1 class="auth-title">Create Account</h1>
                <p class="auth-subtitle">Join Code Visualization Playground to save &amp; share code</p>
            </div>

            <% if (error != null && !error.isEmpty()) { %>
                <div class="alert alert-danger" id="registerAlert">
                    <span>⚠️ <%= error %></span>
                </div>
            <% } %>

            <form action="register" method="POST" id="registerForm">
                <div class="form-group">
                    <label for="name" class="form-label">Full Name</label>
                    <input type="text" id="name" name="name" class="form-input" placeholder="Ada Lovelace" required autocomplete="name">
                </div>

                <div class="form-group">
                    <label for="email" class="form-label">Email Address</label>
                    <input type="email" id="email" name="email" class="form-input" placeholder="developer@example.com" required autocomplete="email">
                </div>

                <div class="form-group">
                    <label for="password" class="form-label">Password</label>
                    <input type="password" id="password" name="password" class="form-input" placeholder="••••••••" required autocomplete="new-password">
                </div>

                <button type="submit" id="registerButton" class="btn btn-primary" style="width: 100%; margin-top: 1rem; padding: 0.75rem;">
                    Register
                </button>
            </form>

            <div class="form-footer">
                Already registered? <a href="login.jsp">Sign in here</a>
            </div>
        </div>
    </main>

    <script src="js/main.js"></script>
</body>
</html>
