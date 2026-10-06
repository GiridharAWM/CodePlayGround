<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.io.InputStream, java.util.List, com.codevisualizer.XMLDOMParser, com.codevisualizer.XMLDOMParser.Concept" %>
<%
    Integer userId = (Integer) session.getAttribute("userId");
    String userName = (String) session.getAttribute("userName");
    boolean isLoggedIn = (userId != null);

    // Read and parse visualization.xml using standard W3C XML DOM Parser (Requirement 19 & 20)
    InputStream xmlStream = application.getResourceAsStream("/xml/visualization.xml");
    List<Concept> concepts = XMLDOMParser.parseConcepts(xmlStream);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Visualization Concepts (XML DOM) - Code Visualization Playground</title>
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
                <li><a href="concepts.jsp" class="active">Concepts (XML DOM)</a></li>
                <% if (isLoggedIn) { %>
                    <li><a href="saved-programs.jsp">Saved Programs</a></li>
                <% } %>
            </ul>
            <div class="nav-actions">
                <button id="themeToggleBtn" class="btn-icon" title="Toggle Theme">☀️</button>
                <% if (isLoggedIn) { %>
                    <div class="user-pill">
                        <div class="user-avatar"><%= userName != null ? userName.substring(0, 1).toUpperCase() : "U" %></div>
                        <span><%= userName %></span>
                    </div>
                    <a href="logout" class="btn btn-secondary btn-sm" id="logoutButton">Logout</a>
                <% } else { %>
                    <a href="login.jsp" class="btn btn-secondary btn-sm">Login</a>
                    <a href="register.jsp" class="btn btn-primary btn-sm">Register</a>
                <% } %>
            </div>
        </nav>
    </header>

    <main class="section-container">
        <div class="section-header">
            <div class="hero-badge">XML DOM Parser Architecture (Section 19 &amp; 20)</div>
            <h1 class="section-title">Core Visualization Concepts</h1>
            <p class="section-desc">
                Parsed at runtime from <code>/xml/visualization.xml</code> using Java's standard W3C Document Object Model (DOM) API without any external XML libraries.
            </p>
        </div>

        <div class="grid-3">
            <%
                if (concepts != null && !concepts.isEmpty()) {
                    for (Concept c : concepts) {
            %>
                <div class="feature-card" style="display: flex; flex-direction: column; justify-content: space-between;">
                    <div>
                        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.75rem;">
                            <span class="badge badge-info"><%= c.getCategory() %></span>
                            <span style="font-family: var(--font-mono); font-size: 0.8rem; color: var(--text-muted);">&lt;<%= c.getName() %>></span>
                        </div>
                        <h3 style="font-size: 1.2rem; margin-bottom: 0.5rem;"><%= c.getTitle() %></h3>
                        <p style="font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 1.25rem;">
                            <%= c.getDescription() %>
                        </p>
                        
                        <div style="background-color: var(--bg-terminal); border-radius: var(--radius-sm); padding: 0.75rem; border: 1px solid var(--border-color); font-family: var(--font-mono); font-size: 0.8rem; color: #a7f3d0; white-space: pre-wrap; margin-bottom: 1.25rem;">
<%= c.getSampleCode() %>
                        </div>
                    </div>

                    <a href="playground.jsp" class="btn btn-secondary btn-sm" style="width: 100%; text-align: center;">
                        🚀 Try in Playground
                    </a>
                </div>
            <%
                    }
                } else {
            %>
                <div style="grid-column: 1 / -1; text-align: center; color: var(--text-muted); padding: 3rem;">
                    No concepts loaded from visualization.xml.
                </div>
            <%
                }
            %>
        </div>
    </main>

    <footer style="border-top: 1px solid var(--border-color); padding: 2rem; text-align: center; color: var(--text-muted); font-size: 0.85rem; margin-top: auto;">
        <p>Demonstrates W3C XML DOM Parsing &bull; Native <code>javax.xml.parsers.DocumentBuilder</code> &bull; Pure Java EE.</p>
    </footer>

    <script src="js/main.js"></script>
</body>
</html>
