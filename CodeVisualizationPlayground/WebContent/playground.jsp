<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    Integer userId = (Integer) session.getAttribute("userId");
    String userName = (String) session.getAttribute("userName");
    boolean isLoggedIn = (userId != null);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Playground - Code Visualization Platform</title>
    <link rel="stylesheet" href="css/style.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500;700&display=swap" rel="stylesheet">
</head>
<body style="overflow: hidden;">

    <!-- Navigation Header -->
    <header>
        <nav class="navbar" style="padding: 0.5rem 1.5rem;">
            <a href="index.html" class="brand">
                <div class="brand-icon">&lt;/&gt;</div>
                <span>Code Visualizer</span>
            </a>
            <ul class="nav-links">
                <li><a href="index.html">Home</a></li>
                <li><a href="playground.jsp" class="active">Playground</a></li>
                <li><a href="concepts.jsp">Concepts</a></li>
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

    <!-- Main Playground Workspace -->
    <main class="playground-layout">
        <!-- Top Toolbar with Examples and Settings -->
        <div class="playground-header">
            <div class="sample-selector-group">
                <label for="sampleSelector">Load Example:</label>
                <select id="sampleSelector" class="select-input">
                    <option value="sample1">1. Variables &amp; Sum (Arithmetic)</option>
                    <option value="sample2">2. Array Iteration &amp; Loops</option>
                    <option value="sample3">3. Conditional If / Else Branching</option>
                    <option value="sample4">4. Recursion (Factorial Call Stack)</option>
                    <option value="sample5">5. While Loop with State Counter</option>
                    <option value="sample6">6. Object-Oriented Class &amp; Method</option>
                    <option value="sample7">7. Exception Handling (try-catch)</option>
                </select>
            </div>
            <div class="toolbar-group">
                <label for="speedSelect" style="font-size: 0.85rem; color: var(--text-secondary); margin-right: 0.25rem;">Speed:</label>
                <select id="speedSelect" class="select-input" style="padding: 0.3rem 0.5rem;">
                    <option value="500">Fast (0.5s)</option>
                    <option value="1000" selected>Normal (1.0s)</option>
                    <option value="2000">Slow (2.0s)</option>
                </select>
                <% if (isLoggedIn) { %>
                    <button id="saveProgramButton" class="btn btn-secondary btn-sm" title="Save this Java program">
                        💾 Save Program
                    </button>
                <% } else { %>
                    <a href="login.jsp" class="btn btn-secondary btn-sm" title="Sign in to save programs">
                        💾 Login to Save
                    </a>
                <% } %>
            </div>
        </div>

        <!-- Center Workspace: Split Left (Editor + Terminal) / Right (Visualization Engine) -->
        <div class="playground-main">
            <!-- Left Column: Source Code Editor & Controls & Terminal -->
            <div style="display: flex; flex-direction: column; gap: 0.75rem; min-height: 0;">
                <div class="panel" style="flex: 1; min-height: 0;">
                    <div class="panel-header">
                        <div class="panel-title">
                            <span>📄</span>
                            <span>Source Code: <strong>Main.java</strong></span>
                        </div>
                        <span class="badge badge-info">Arbitrary Java Execution</span>
                    </div>

                    <!-- Editor with Line Numbers -->
                    <div class="editor-container">
                        <div id="lineNumbersGutter" class="line-numbers"></div>
                        <textarea id="codeEditor" class="code-textarea" spellcheck="false" placeholder="// Enter any valid Java code containing class Main..."></textarea>
                    </div>

                    <!-- Step Controls Toolbar -->
                    <div class="control-toolbar">
                        <div class="toolbar-group">
                            <button id="runButton" class="btn btn-primary" title="Compile and Execute on Server" onclick="handleRunCode()">
                                ▶ RUN
                            </button>
                            <button id="stepButton" class="btn btn-secondary btn-sm" title="Advance one trace step" onclick="stepForward()">
                                ⏭ STEP
                            </button>
                            <button id="prevButton" class="btn btn-secondary btn-sm" title="Step backward" onclick="stepBackward()">
                                ◀ PREV
                            </button>
                            <button id="nextButton" class="btn btn-secondary btn-sm" title="Step forward" onclick="stepForward()">
                                NEXT ▶
                            </button>
                            <button id="autoPlayButton" class="btn btn-secondary btn-sm" title="Auto-advance steps" onclick="toggleAutoPlay()">
                                ▶ AUTO
                            </button>
                            <button id="resetButton" class="btn btn-secondary btn-sm" title="Reset execution to beginning" onclick="resetExecution()">
                                🔄 RESET
                            </button>
                        </div>
                        <div class="step-tracker" id="stepTracker">
                            Current Step: 0 / 0
                        </div>
                    </div>
                </div>

                <!-- Bottom Terminal Console -->
                <div class="terminal-panel">
                    <div class="terminal-header">
                        <div class="terminal-dots">
                            <div class="terminal-dot dot-red"></div>
                            <div class="terminal-dot dot-yellow"></div>
                            <div class="terminal-dot dot-green"></div>
                        </div>
                        <span>CONSOLE OUTPUT &bull; TERMINAL</span>
                        <span style="font-size: 0.75rem;">JVM 21 Sandbox</span>
                    </div>
                    <div id="outputPanel" class="terminal-body">&gt; Ready. Click RUN to compile and execute program.</div>
                </div>
            </div>

            <!-- Right Column: Visualization Panels -->
            <div class="panel" id="visualizationPanel" style="min-height: 0;">
                <div class="panel-header">
                    <div class="panel-title">
                        <span>🔍</span>
                        <span>EXECUTION VISUALIZATION</span>
                    </div>
                    <span class="badge badge-success">Trace-Driven Engine</span>
                </div>

                <div class="visualization-board">
                    <!-- Chronicle Narrative Ticker -->
                    <div id="chronicleContainer">
                        <div class="chronicle-card">
                            <span class="chronicle-step">Ready</span>
                            <span class="chronicle-text">Click RUN to compile and trace the program.</span>
                        </div>
                    </div>

                    <!-- Local Variables Panel -->
                    <div class="vis-card">
                        <div class="vis-card-title">
                            <span>📦</span>
                            <span>Variables (Memory State)</span>
                        </div>
                        <div id="variablesContainer">
                            <span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>
                        </div>
                    </div>

                    <!-- Array Memory Representation -->
                    <div class="vis-card">
                        <div class="vis-card-title">
                            <span>📊</span>
                            <span>Arrays (Indexed Storage)</span>
                        </div>
                        <div id="arraysContainer">
                            <span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>
                        </div>
                    </div>

                    <!-- Method Call Stack & Recursion -->
                    <div class="vis-card">
                        <div class="vis-card-title">
                            <span>📞</span>
                            <span>Call Stack &amp; Recursion Frames</span>
                        </div>
                        <div id="callStackContainer">
                            <span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>
                        </div>
                    </div>

                    <!-- Conditions & Branch Evaluation -->
                    <div class="vis-card">
                        <div class="vis-card-title">
                            <span>🔀</span>
                            <span>Condition Evaluation</span>
                        </div>
                        <div id="conditionContainer">
                            <span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>
                        </div>
                    </div>

                    <!-- Loop Iterations -->
                    <div class="vis-card">
                        <div class="vis-card-title">
                            <span>🔁</span>
                            <span>Loop Progression</span>
                        </div>
                        <div id="loopContainer">
                            <span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- Save Program Modal Dialog -->
    <div id="saveModal" class="modal-overlay">
        <div class="modal-card">
            <div class="modal-header">
                <h3 class="modal-title">💾 Save Java Program</h3>
                <button type="button" class="btn-icon" onclick="closeSaveProgramModal()">✕</button>
            </div>
            <div class="form-group">
                <label for="saveProgramName" class="form-label">Program Name</label>
                <input type="text" id="saveProgramName" class="form-input" placeholder="e.g., Binary Search Algorithm" value="My Java Program">
            </div>
            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
                <button type="button" class="btn btn-secondary" onclick="closeSaveProgramModal()">Cancel</button>
                <button type="button" class="btn btn-primary" id="confirmSaveButton" onclick="submitSaveProgram()">Save to MySQL</button>
            </div>
        </div>
    </div>

    <script src="js/main.js"></script>
    <script src="js/visualizer.js"></script>
</body>
</html>
