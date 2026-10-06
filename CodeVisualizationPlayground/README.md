# CODE VISUALIZATION PLAYGROUND
### Web-based Java Code Execution and Visualization Platform

---

## 1. Project Objective
**Code Visualization Playground** is a comprehensive Java compilation, execution, and execution-tracing web application. It empowers students, educators, and software engineers to enter **any arbitrary valid Java program**, compile and execute it on a secure server, and explore its execution step-by-step through synchronized, interactive visualizations in the browser.

Unlike superficial visualizers that rely on client-side regex or pattern matching, this platform features an authentic **Java Trace-Driven Architecture**:
1. Arbitrary Java source code is submitted via AJAX.
2. The server instruments the code with non-intrusive trace hooks that preserve program semantics.
3. The server compiles the code using `javac` in an isolated sandbox.
4. The compiled class runs as an isolated subprocess with an execution timeout guard.
5. The execution generates structured runtime events (`VARIABLE`, `ARRAY`, `LOOP`, `CONDITION`, `CALL_STACK`, `OUTPUT`, `EXCEPTION`).
6. The frontend visualization engine reconstitutes the exact state of variables, memory arrays, and stack frames for each step.

---

## 2. Technologies Used

| Layer | Strict Technology Stack | Purpose |
|---|---|---|
| **Frontend Structure** | Pure HTML5 | Semantic layouts, modal dialogues, line gutters, terminal shells |
| **Frontend Styling** | Pure Vanilla CSS3 | Modern dark developer IDE aesthetic, glassmorphism, glowing accents (Zero frameworks, No Bootstrap, No Tailwind) |
| **Frontend Logic** | Vanilla JavaScript (ES6+) | AJAX communication, trace event replay, memory state reconstruction, stepping engine |
| **Server Engine** | Java Servlets (Jakarta EE / Servlet 6.0) | Compilation management, sandboxed execution, authentication, CRUD APIs |
| **Server Pages** | JavaServer Pages (JSP 3.1) | Dynamic server-side rendering for auth, dashboard, and XML concepts |
| **Database** | MySQL 8.0+ | Relational persistence for developers and saved programs |
| **Database Access** | Java Database Connectivity (JDBC) | Secure SQL operations via `PreparedStatement` (Zero ORM, No Hibernate) |
| **Data Configuration** | XML & W3C XML DOM Parser | Pedagogical concept definitions parsed natively via `javax.xml.parsers.DocumentBuilder` |
| **Automated Testing** | Selenium WebDriver & XPath | End-to-end browser automation testing using strict XPath selectors |

---

## 3. Software Requirements
- **Java Development Kit (JDK)**: Version 17 or Version 21 LTS (with `javac` and `java` on system `PATH`).
- **Apache Tomcat**: Version 10.1.x (Jakarta EE 10 support).
- **MySQL Server**: Version 8.0+.
- **IDE**: Eclipse IDE for Enterprise Java and Web Developers (or standalone Tomcat deployment).
- **Google Chrome**: For browser usage and Selenium automated testing.

---

## 4. Tomcat Configuration
1. Ensure Tomcat 10.1 is installed (e.g. `C:\Program Files\Apache Software Foundation\Tomcat 10.1`).
2. Verify that Tomcat's `webapps` directory is accessible.
3. The project context root is configured as `/CodeVisualizationPlayground`.
4. Tomcat port default: `http://localhost:8080`.

---

## 5. Java / JDK Requirement
Ensure JDK 21 or 17 is configured:
```powershell
java -version
javac -version
```
Both `java` and `javac` must be runnable from the terminal, as `RunCodeServlet` invokes `javac` and `java` directly during sandboxed code execution.

---

## 6. MySQL Setup
1. Log in to MySQL CLI or MySQL Workbench:
```powershell
mysql -u root -p
```
2. Execute the provided `database.sql` script:
```sql
CREATE DATABASE IF NOT EXISTS code_visualizer;
USE code_visualizer;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS programs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    program_name VARCHAR(150) NOT NULL,
    code TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```
3. A pre-seeded test account is provided:
   - **Email**: `demo@visualizer.com`
   - **Password**: `password123`

---

## 7. JDBC Driver Setup
The MySQL JDBC Connector (`mysql-connector-j-8.3.0.jar`) is bundled in:
```
WebContent/WEB-INF/lib/mysql-connector-j-8.3.0.jar
```
This enables automatic runtime classloading for `com.mysql.cj.jdbc.Driver` on Tomcat.

---

## 8. Eclipse Project Setup
1. Open Eclipse IDE for Enterprise Java and Web Developers.
2. Select **File** &rarr; **Import...** &rarr; **General** &rarr; **Existing Projects into Workspace**.
3. Browse and select the directory:
   `e:\Giridhar\VISUAL\CodeVisualizationPlayground`
4. Click **Finish**.
5. Ensure the Server runtime is targeted to **Apache Tomcat v10.1**:
   - Right-click project &rarr; **Properties** &rarr; **Targeted Runtimes** &rarr; Check **Apache Tomcat v10.1**.
6. The project facets are pre-configured:
   - Dynamic Web Module: 6.0
   - Java: 21
   - JavaScript: 1.0

---

## 9. How to Run the Project
### Option A: Running inside Eclipse
1. In the Eclipse **Servers** view, right-click Tomcat 10.1 &rarr; **Add and Remove...**.
2. Add `CodeVisualizationPlayground` to the configured deployments.
3. Start the Tomcat server.
4. Open your browser and navigate to:
   `http://localhost:8080/CodeVisualizationPlayground/index.html`

### Option B: Deploying directly to Tomcat Webapps
1. Copy the `WebContent` folder contents to `C:\Program Files\Apache Software Foundation\Tomcat 10.1\webapps\CodeVisualizationPlayground` (or export as WAR).
2. Start the Tomcat service.
3. Access `http://localhost:8080/CodeVisualizationPlayground/index.html`.

---

## 10. How to Use the Playground
1. **Load or Write Code**: Select an example from the dropdown (Variables, Arrays, If-Else, Recursion, Loops, Objects, Exceptions) or write custom Java code with a `public class Main`.
2. **Click RUN**: The server compiles and executes the program. If compilation errors occur, the compiler output is shown in the terminal.
3. **Inspect Execution Steps**:
   - **STEP / NEXT**: Advance one execution step.
   - **PREV**: Rewind one execution step (state is reconstituted backward).
   - **RESET**: Return to the beginning.
   - **AUTO**: Automatically steps through execution at selected speed (0.5s, 1s, 2s).
4. **Visual Panels**:
   - **Active Line Highlight**: Highlights the executing source line in real-time.
   - **Variables**: Displays variable values, marking recent changes.
   - **Arrays**: Visualizes memory blocks with element access highlighting.
   - **Call Stack**: Displays active method frames and recursion trees.
   - **Condition Evaluation**: Displays boolean conditions and evaluated branches.
   - **Terminal**: Progressively renders output as reached by the execution trace.
5. **Save Program**: Click **Save Program** to save your code to MySQL via AJAX.
6. **Saved Programs**: Visit `saved-programs.jsp` to review, reload into the editor, or delete saved code.

---

## 11. How Execution Tracing Works
```
                     +---------------------------+
                     |  Arbitrary Java Source    |
                     +---------------------------+
                                   |
                                   v
                     +---------------------------+
                     |  JavaSourceInstrumenter   |
                     |  - Detects declarations   |
                     |  - Wraps conditions       |
                     |  - Hooks method entries   |
                     |  - Preserves semantics    |
                     +---------------------------+
                                   |
                                   v
                     +---------------------------+
                     |     Server 'javac'        |
                     |  Isolated Temp Workspace  |
                     +---------------------------+
                                   |
                                   v
                     +---------------------------+
                     |      JVM Execution        |
                     |  - 5-second timeout       |
                     |  - Tracer emits events    |
                     +---------------------------+
                                   |
                                   v
                     +---------------------------+
                     |        trace.json         |
                     |  Returned via AJAX JSON   |
                     +---------------------------+
                                   |
                                   v
                     +---------------------------+
                     | visualizer.js Replayer    |
                     | Synchronized DOM Stepping |
                     +---------------------------+
```
* **Fallback Guarantee**: If complex constructs cannot be instrumented, the system automatically falls back to compiling and executing the original code and safely captures stdout/stderr events.

---

## 12. How to Run Selenium Tests
The automated test suite (`CodeVisualizationTest.java`) covers all 18 test steps required in Section 27:
1. Open home page
2. Verify page title
3. Open login page
4. Enter email
5. Enter password
6. Login
7. Verify playground page
8. Enter Java code
9. Click RUN
10. Verify output
11. Verify visualization
12. Click STEP
13. Verify execution step changes
14. Save a program
15. Open saved programs
16. Load a program
17. Delete a program
18. Logout

To compile and execute the test:
```powershell
javac -cp ".;lib/*;C:\Users\kgoku\.m2\repository\org\seleniumhq\selenium\selenium-api\4.20.0\selenium-api-4.20.0.jar;..." Selenium/CodeVisualizationTest.java
java -cp "..." com.codevisualizer.test.CodeVisualizationTest
```

---

## 13. Common Errors & Solutions

| Error | Root Cause | Solution |
|---|---|---|
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | JDBC driver missing from classpath | Ensure `mysql-connector-j-8.3.0.jar` is in `WebContent/WEB-INF/lib/` and Tomcat server library path. |
| `Access denied for user 'root'@'localhost'` | MySQL password mismatch | Verify MySQL credentials in `DBConnection.java` match your MySQL root setup. |
| `javac : The term 'javac' is not recognized` | JDK not configured on PATH | Add JDK `bin/` directory to Windows environment variable `PATH`. |
| `Execution Timed Out (5 seconds limit)` | User code contains `while(true)` or infinite loop | The sandbox safety guard successfully prevented a freeze. Fix the loop termination condition. |
| `HTTP 404 on JSP pages` | Context path mismatch | Ensure application is deployed under `/CodeVisualizationPlayground`. |

---
*Created for Code Visualization Playground &bull; Dynamic Web Architecture with Pure Vanilla Technologies.*
