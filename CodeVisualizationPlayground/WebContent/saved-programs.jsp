<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.Connection, java.sql.PreparedStatement, java.sql.ResultSet, com.codevisualizer.DBConnection" %>
<%
    Integer userId = (Integer) session.getAttribute("userId");
    String userName = (String) session.getAttribute("userName");

    if (userId == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Saved Programs - Code Visualization Playground</title>
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
                <li><a href="saved-programs.jsp" class="active">Saved Programs</a></li>
            </ul>
            <div class="nav-actions">
                <button id="themeToggleBtn" class="btn-icon" title="Toggle Theme">☀️</button>
                <div class="user-pill">
                    <div class="user-avatar"><%= userName != null ? userName.substring(0, 1).toUpperCase() : "U" %></div>
                    <span><%= userName %></span>
                </div>
                <a href="logout" class="btn btn-secondary btn-sm" id="logoutButton">Logout</a>
            </div>
        </nav>
    </header>

    <main class="section-container" style="max-width: 960px;">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 2rem;">
            <div>
                <h1 style="font-size: 2rem; font-weight: 700;">My Saved Programs</h1>
                <p style="color: var(--text-secondary); font-size: 0.95rem;">Manage your saved Java algorithms and exercises</p>
            </div>
            <a href="playground.jsp" class="btn btn-primary">
                + New Program
            </a>
        </div>

        <table class="programs-table" id="savedProgramsTable">
            <thead>
                <tr>
                    <th style="width: 45%;">Program Name</th>
                    <th style="width: 25%;">Created Date</th>
                    <th style="width: 30%; text-align: right;">Actions</th>
                </tr>
            </thead>
            <tbody>
                <%
                    boolean hasPrograms = false;
                    try {
                        conn = DBConnection.getConnection();
                        stmt = conn.prepareStatement("SELECT id, program_name, created_at FROM programs WHERE user_id = ? ORDER BY created_at DESC");
                        stmt.setInt(1, userId);
                        rs = stmt.executeQuery();

                        while (rs.next()) {
                            hasPrograms = true;
                            int progId = rs.getInt("id");
                            String progName = rs.getString("program_name");
                            java.sql.Timestamp created = rs.getTimestamp("created_at");
                %>
                    <tr id="program-row-<%= progId %>">
                        <td style="font-weight: 600; color: #93c5fd;">
                            <span>☕ <%= progName %></span>
                        </td>
                        <td style="color: var(--text-muted); font-size: 0.85rem;">
                            <%= created != null ? created.toString().substring(0, 16) : "N/A" %>
                        </td>
                        <td style="text-align: right;">
                            <a href="playground.jsp?loadId=<%= progId %>" class="btn btn-secondary btn-sm" id="load-btn-<%= progId %>" style="margin-right: 0.4rem;">
                                📂 Load
                            </a>
                            <button type="button" class="btn btn-danger btn-sm" id="delete-btn-<%= progId %>" data-id="<%= progId %>" onclick="handleDelete(this)">
                                🗑️ Delete
                            </button>
                        </td>
                    </tr>
                <%
                        }
                    } catch (Exception e) {
                %>
                    <tr>
                        <td colspan="3" style="text-align: center; color: var(--accent-danger); padding: 2rem;">
                            Database Error: <%= e.getMessage() %>
                        </td>
                    </tr>
                <%
                    } finally {
                        if (rs != null) try { rs.close(); } catch (Exception ignored) {}
                        if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
                        DBConnection.close(conn);
                    }
                    if (!hasPrograms) {
                %>
                    <tr id="emptyRow">
                        <td colspan="3" style="text-align: center; color: var(--text-muted); padding: 3rem;">
                            No saved programs found yet. Go to <a href="playground.jsp" style="color: var(--accent-cyan);">Playground</a> to write and save your first program!
                        </td>
                    </tr>
                <%
                    }
                %>
            </tbody>
        </table>
    </main>

    <script src="js/main.js"></script>
    <script>
        function handleDelete(btn) {
            if (btn) {
                const progId = btn.getAttribute('data-id');
                if (progId) {
                    deleteProgram(progId);
                }
            }
        }

        async function deleteProgram(programId) {
            if (!confirm('Are you sure you want to delete this saved program?')) {
                return;
            }

            try {
                const response = await fetch('delete-program', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ id: programId })
                });

                const data = await response.json();
                if (data.success) {
                    showToast('Program deleted successfully.', 'success');
                    const row = document.getElementById('program-row-' + programId);
                    if (row) {
                        row.style.opacity = '0';
                        row.style.transition = 'opacity 0.3s ease';
                        setTimeout(() => {
                            row.remove();
                            const tbody = document.querySelector('#savedProgramsTable tbody');
                            if (tbody && tbody.children.length === 0) {
                                tbody.innerHTML = '<tr id="emptyRow"><td colspan="3" style="text-align: center; color: var(--text-muted); padding: 3rem;">No saved programs found.</td></tr>';
                            }
                        }, 300);
                    }
                } else {
                    showToast(data.error || 'Failed to delete program.', 'error');
                }
            } catch (err) {
                showToast('Network error while deleting program.', 'error');
            }
        }
    </script>
</body>
</html>
