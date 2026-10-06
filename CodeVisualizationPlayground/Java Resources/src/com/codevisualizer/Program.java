package com.codevisualizer;

import java.sql.Timestamp;

/**
 * Program entity representing a saved Java program.
 */
public class Program {
    private int id;
    private int userId;
    private String programName;
    private String code;
    private Timestamp createdAt;

    public Program() {}

    public Program(int id, int userId, String programName, String code, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.programName = programName;
        this.code = code;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
