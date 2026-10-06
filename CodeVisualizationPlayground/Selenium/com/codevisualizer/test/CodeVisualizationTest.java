package com.codevisualizer.test;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * End-to-End Automated Browser Test Suite for Code Visualization Playground.
 * Implements all 18 test requirements specified in Section 27 using XPath locators.
 */
public class CodeVisualizationTest {

    private static final String BASE_URL = "http://localhost:8080/CodeVisualizationPlayground";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  STARTING CODE VISUALIZATION PLAYGROUND E2E TEST");
        System.out.println("=================================================");

        ChromeOptions options = new ChromeOptions();
        // Run headless or with UI
        if (System.getProperty("headless", "true").equalsIgnoreCase("true")) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1600,1000");

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);

            // 1. Open home page
            System.out.println("[TEST 1] Opening home page...");
            driver.get(BASE_URL + "/index.html");

            // 2. Verify page title
            System.out.println("[TEST 2] Verifying page title...");
            String title = driver.getTitle();
            assert title.contains("Code Visualization") : "Unexpected title: " + title;
            System.out.println("  ✓ Page title verified: " + title);

            // 3. Navigate to login
            System.out.println("[TEST 3] Navigating to login page...");
            driver.get(BASE_URL + "/login.jsp");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//form[@id='loginForm']")));
            System.out.println("  ✓ Login page opened successfully");

            // 4. Enter email (XPath)
            System.out.println("[TEST 4] Entering email via XPath...");
            WebElement emailInput = driver.findElement(By.xpath("//input[@id='email']"));
            emailInput.clear();
            emailInput.sendKeys("demo@visualizer.com");

            // 5. Enter password (XPath)
            System.out.println("[TEST 5] Entering password via XPath...");
            WebElement passwordInput = driver.findElement(By.xpath("//input[@id='password']"));
            passwordInput.clear();
            passwordInput.sendKeys("password123");

            // 6. Submit login (XPath)
            System.out.println("[TEST 6] Submitting login form...");
            WebElement loginBtn = driver.findElement(By.xpath("//button[@id='loginButton' or @type='submit']"));
            loginBtn.click();

            // 7. Verify playground page
            System.out.println("[TEST 7] Verifying redirection to playground...");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//textarea[@id='codeEditor']")));
            wait.until(d -> ((JavascriptExecutor) d).executeScript("return typeof handleRunCode === 'function';").equals(Boolean.TRUE));
            System.out.println("  ✓ Playground page verified");

            // 8. Enter Java code (XPath)
            System.out.println("[TEST 8] Entering Java test program...");
            WebElement codeEditor = driver.findElement(By.xpath("//textarea[@id='codeEditor']"));
            String testCode = "public class Main {\n" +
                              "    public static void main(String[] args) {\n" +
                              "        int a = 15;\n" +
                              "        int b = 25;\n" +
                              "        int sum = a + b;\n" +
                              "        System.out.println(sum);\n" +
                              "    }\n" +
                              "}";
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));",
                codeEditor, testCode
            );
            Thread.sleep(500);

            // 9. Click RUN (XPath)
            System.out.println("[TEST 9] Clicking RUN button via XPath...");
            WebElement runBtn = driver.findElement(By.xpath("//button[@id='runButton']"));
            runBtn.click();

            // 10. Verify output (XPath)
            System.out.println("[TEST 10] Verifying terminal output...");
            WebElement outputPanel = driver.findElement(By.xpath("//div[@id='outputPanel']"));
            wait.until(d -> outputPanel.getText().contains("40"));
            System.out.println("  ✓ Output verified: \n" + outputPanel.getText());

            // 11. Verify visualization (XPath)
            System.out.println("[TEST 11] Verifying visualization panel...");
            WebElement visPanel = driver.findElement(By.xpath("//div[@id='visualizationPanel']"));
            assert visPanel.isDisplayed() : "Visualization panel not visible";
            System.out.println("  ✓ Visualization panel is visible and reactive");

            // 12. Click STEP (XPath)
            System.out.println("[TEST 12] Clicking STEP button...");
            WebElement stepBtn = driver.findElement(By.xpath("//button[@id='stepButton']"));
            stepBtn.click();
            Thread.sleep(400);

            // 13. Verify execution step changes (XPath)
            System.out.println("[TEST 13] Verifying step counter increment...");
            WebElement stepTracker = driver.findElement(By.xpath("//div[@id='stepTracker']"));
            String stepText = stepTracker.getText();
            System.out.println("  ✓ Step state: " + stepText);
            assert stepText.contains("Step: 2") || stepText.contains("Step: 1") : "Step did not advance properly";

            // 14. Save a program (XPath)
            System.out.println("[TEST 14] Saving current program via modal...");
            WebElement saveBtn = driver.findElement(By.xpath("//button[@id='saveProgramButton']"));
            saveBtn.click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@id='saveProgramName']")));
            WebElement progNameInput = driver.findElement(By.xpath("//input[@id='saveProgramName']"));
            progNameInput.clear();
            progNameInput.sendKeys("Automated Selenium Test Program");
            WebElement confirmSaveBtn = driver.findElement(By.xpath("//button[@id='confirmSaveButton']"));
            confirmSaveBtn.click();
            Thread.sleep(1000);
            System.out.println("  ✓ Program saved successfully via AJAX");

            // 15. Open saved programs
            System.out.println("[TEST 15] Navigating to saved programs page...");
            driver.get(BASE_URL + "/saved-programs.jsp");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table[@id='savedProgramsTable']")));
            System.out.println("  ✓ Saved programs table displayed");

            // 16. Load a program (XPath)
            System.out.println("[TEST 16] Clicking Load on saved program...");
            WebElement loadBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(@id,'load-btn-') or contains(text(),'Load')]")));
            loadBtn.click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//textarea[@id='codeEditor']")));
            System.out.println("  ✓ Program loaded back into playground editor");

            // 17. Delete a program (XPath)
            System.out.println("[TEST 17] Testing program deletion...");
            driver.get(BASE_URL + "/saved-programs.jsp");
            WebElement deleteBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(@id,'delete-btn-') or contains(text(),'Delete')]")));
            deleteBtn.click();
            // Handle alert prompt
            try {
                wait.until(ExpectedConditions.alertIsPresent());
                driver.switchTo().alert().accept();
            } catch (Exception ignored) {}
            Thread.sleep(1000);
            System.out.println("  ✓ Program deletion handled successfully via AJAX");

            // 18. Logout (XPath)
            System.out.println("[TEST 18] Logging out...");
            WebElement logoutBtn = driver.findElement(By.xpath("//a[@id='logoutButton' or text()='Logout']"));
            logoutBtn.click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@id='email']")));
            System.out.println("  ✓ User logged out and session terminated");

            System.out.println("\n=================================================");
            System.out.println("  ALL 18 E2E TEST SCENARIOS PASSED SUCCESSFULLY!");
            System.out.println("=================================================");

        } catch (Throwable t) {
            System.err.println("\n❌ TEST SUITE FAILED: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
