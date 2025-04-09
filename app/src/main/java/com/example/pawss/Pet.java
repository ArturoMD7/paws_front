package com.example.pawss;

import android.content.Context;

public class Pet {
    private int id;
    private String name;
    private String type;
    private int age;
    private String breed;
    private String adoptionDate;
    private String photoUrl;
    private String vaccinesUrl;
    private String urlApi;
    private int ownerId;  // Nuevo campo para el ID del dueño

    public Pet(Context context, int id, String name, String type, int age, String breed,
               String adoptionDate, String photoUrl, String vaccinesUrl) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.age = age;
        this.breed = breed;
        this.adoptionDate = adoptionDate;
        this.photoUrl = photoUrl;
        this.vaccinesUrl = vaccinesUrl;
        this.urlApi = context.getString(R.string.Url);
    }

    // Getters mejorados para manejar URLs
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getAge() { return age; }
    public String getBreed() { return breed; }
    public String getAdoptionDate() { return adoptionDate; }
    public int getOwnerId() { return ownerId; }

    public String getPhotoUrl() {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            if (!photoUrl.startsWith("http")) {
                String cleanPath = photoUrl.startsWith("/") ? photoUrl.substring(1) : photoUrl;
                return urlApi + "media/" + cleanPath;
            }
            return photoUrl;
        }
        return null;
    }

    public String getVaccinesUrl() {
        if (vaccinesUrl != null && !vaccinesUrl.isEmpty()) {
            if (!vaccinesUrl.startsWith("http")) {
                String cleanPath = vaccinesUrl.startsWith("/") ? vaccinesUrl.substring(1) : vaccinesUrl;
                return urlApi + "media/" + cleanPath;
            }
            return vaccinesUrl;
        }
        return null;
    }
}