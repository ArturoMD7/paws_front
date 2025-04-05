package com.example.pawss;

public class Pet {
    private int id;
    private String name;
    private String type;
    private int age;
    private String breed;
    private String adoptionDate;
    private String photoUrl;
    private String vaccinesUrl;

    public Pet(int id, String name, String type, int age, String breed,
               String adoptionDate, String photoUrl, String vaccinesUrl) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.age = age;
        this.breed = breed;
        this.adoptionDate = adoptionDate;
        this.photoUrl = photoUrl;
        this.vaccinesUrl = vaccinesUrl;
    }

    // Getters mejorados para manejar URLs
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getAge() { return age; }
    public String getBreed() { return breed; }
    public String getAdoptionDate() { return adoptionDate; }

    public String getPhotoUrl() {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            if (!photoUrl.startsWith("http")) {
                // Asegurar que no tenga barras duplicadas
                String cleanPath = photoUrl.startsWith("/") ? photoUrl.substring(1) : photoUrl;
                return "http://192.168.1.64:8000/media/" + cleanPath;
            }
            return photoUrl;
        }
        return null;
    }

    public String getVaccinesUrl() {
        if (vaccinesUrl != null && !vaccinesUrl.isEmpty()) {
            if (!vaccinesUrl.startsWith("http")) {
                String cleanPath = vaccinesUrl.startsWith("/") ? vaccinesUrl.substring(1) : vaccinesUrl;
                return "http://192.168.1.64:8000/media/" + cleanPath;
            }
            return vaccinesUrl;
        }
        return null;
    }
}