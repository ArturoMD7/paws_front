package com.example.pawss;

import org.json.JSONArray;

public class User {
    public static JSONArray objects;
    private int id;
    private String email;
    private String first_name;
    private String last_name;
    private String phone;
    private String address;

    // Getters y setters
    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return first_name; }
    public String getLastName() { return last_name; }
}