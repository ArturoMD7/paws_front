package com.example.pawss;

public class LoginResponse {
    private String access;
    private String refresh;
    private User user;

    // Getters y setters
    public String getAccess() { return access; }
    public String getRefresh() { return refresh; }
    public User getUser() { return user; }
}