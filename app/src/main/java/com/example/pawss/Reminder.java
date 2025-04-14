package com.example.pawss;

import com.google.gson.annotations.SerializedName;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Reminder {
    @SerializedName("id")
    int id;

    @SerializedName("title")
    String title;

    @SerializedName("description")
    String description;

    @SerializedName("due_date")
    String dueDate;

    @SerializedName("is_recurring")
    boolean isRecurring;

    @SerializedName("recurrence_type")
    String recurrenceType;

    @SerializedName("recurrence_value")
    int recurrenceValue;

    @SerializedName("pet")
    Pet pet;

    @SerializedName("family_member_details")
    FamilyMember assignedTo;

    @SerializedName("status") // Nuevo campo añadido
    String status;

    // Formateador de fecha para mostrar en la UI
    private static final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat apiFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public boolean isRecurring() { return isRecurring; }
    public String getRecurrenceType() { return recurrenceType; }
    public int getRecurrenceValue() { return recurrenceValue; }
    public Pet getPet() { return pet; }
    public FamilyMember getAssignedTo() { return assignedTo; }
    public String getStatus() {
        return status;
    }


    // Método para obtener la fecha formateada para mostrar
    public String getFormattedDateTime() {
        try {
            Date date = apiFormat.parse(dueDate);
            return displayFormat.format(date);
        } catch (ParseException e) {
            return dueDate; // Si hay error, devolver el valor original
        }
    }

    public String getAssignedToName() {
        if (assignedTo != null) {
            return assignedTo.name;
        }
        return "Todos los miembros";
    }

    public String getPetName() {
        if (pet != null) {
            return pet.name;
        }
        return "Sin mascota";
    }

    // Clase interna para mascotas
    public static class Pet {
        @SerializedName("id")
        int id;
        @SerializedName("name")
        String name;
        @SerializedName("photo_url")
        String photoUrl;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getPhotoUrl() { return photoUrl; }
    }

    // Clase interna para miembros de familia
    public static class FamilyMember {
        @SerializedName("id")
        int id;
        @SerializedName("name")
        String name;

        public int getId() { return id; }
        public String getName() { return name; }
    }
}