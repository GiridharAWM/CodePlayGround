-- ==============================================================
-- CODE VISUALIZATION PLAYGROUND - DATABASE SETUP SCRIPT
-- Database: code_visualizer
-- ==============================================================

CREATE DATABASE IF NOT EXISTS code_visualizer;
USE code_visualizer;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- 2. Programs Table (Saved Java Programs)
CREATE TABLE IF NOT EXISTS programs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    program_name VARCHAR(150) NOT NULL,
    code TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Sample seed user for testing
INSERT INTO users (id, name, email, password) 
VALUES (1, 'Demo Developer', 'demo@visualizer.com', 'password123')
ON DUPLICATE KEY UPDATE name='Demo Developer';

-- Sample seed programs
INSERT INTO programs (id, user_id, program_name, code, created_at)
VALUES 
(1, 1, 'Sum of Two Numbers', 'public class Main {\n    public static void main(String[] args) {\n        int a = 10;\n        int b = 20;\n        int sum = a + b;\n        System.out.println(sum);\n    }\n}', NOW()),
(2, 1, 'Array Iteration', 'public class Main {\n    public static void main(String[] args) {\n        int[] numbers = {10, 20, 30, 40, 50};\n        for(int i = 0; i < numbers.length; i++) {\n            System.out.println(numbers[i]);\n        }\n    }\n}', NOW()),
(3, 1, 'Factorial Recursion', 'public class Main {\n    static int factorial(int n) {\n        if(n <= 1) {\n            return 1;\n        }\n        return n * factorial(n - 1);\n    }\n    public static void main(String[] args) {\n        int result = factorial(5);\n        System.out.println(result);\n    }\n}', NOW())
ON DUPLICATE KEY UPDATE program_name=VALUES(program_name);
