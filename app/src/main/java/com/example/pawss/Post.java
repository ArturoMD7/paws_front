package com.example.pawss;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Post {
    private int id;
    private String content;
    private String postType;
    private String createdAt;
    private String authorName;
    private String petName;
    private List<String> imageUrls;  // Esta lista ya contiene las URLs completas
    private String petType;
    private String petBreed;
    private int petAge;

    public static class PostImage {
        private String photoUrl;

        public PostImage(String photoUrl) {
            this.photoUrl = photoUrl;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }
    }

    public Post(Context context, int id, String content, String postType,
                String createdAt, String authorName, String petName,
                List<String> imageUrls, String petType, String petBreed, int petAge) {
        this.id = id;
        this.content = content;
        this.postType = postType;
        this.createdAt = createdAt;
        this.authorName = authorName;
        this.petName = petName;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.petType = petType;
        this.petBreed = petBreed;
        this.petAge = petAge;
    }

    // Getters
    public String getPetType() { return petType; }
    public String getPetBreed() { return petBreed; }
    public int getPetAge() { return petAge; }
    public int getId() { return id; }
    public String getContent() { return content; }
    public String getPostType() { return postType; }
    public String getCreatedAt() {
        try {
            SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = apiFormat.parse(createdAt);
            return displayFormat.format(date);
        } catch (Exception e) {
            return createdAt;
        }
    }

    public String getAuthorName() { return authorName; }
    public String getPetName() { return petName; }
    public List<String> getImageUrls() {
        return imageUrls;  // Simplemente retorna la lista de URLs
    }
}