package com.example.pawss;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class recordatorios extends BaseActivity {

    // Views
    private ImageView addReminderIcon;
    private ScrollView formContainer;
    private RecyclerView rvReminders;
    private EditText etReminderTitle, etReminderDescription, etRecurrenceValue;
    private Button btnSaveReminder, btnSelectDate, btnSelectTime;
    private Spinner spFamilyMembers, spPets, spRecurrenceType;
    private RadioGroup rgReminderType;
    private RadioButton rbOneTime, rbRecurring;
    private LinearLayout llRecurringOptions;

    // Data
    private ReminderAdapter reminderAdapter;
    private List<Reminder> reminderList = new ArrayList<>();
    private List<Reminder.Pet> availablePets = new ArrayList<>();
    private List<Reminder.FamilyMember> availableFamilyMembers = new ArrayList<>();

    // Date & Time
    private Calendar selectedDateTime = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    // Network
    private AuthManager authManager;
    private RequestQueue requestQueue;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_recordatorios;
    }

    @Override
    protected int getCurrentNavItem() {
        return R.id.nav_recordatorios;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager(this);
        requestQueue = Volley.newRequestQueue(this);

        initializeViews();
        setupListeners();
        setupRecyclerView();
        setupRecurrenceSpinner();
        loadFamilyMembers();
        loadPets();
        loadReminders();
    }

    private void initializeViews() {
        addReminderIcon = findViewById(R.id.addReminderIcon);
        formContainer = findViewById(R.id.formContainer);
        rvReminders = findViewById(R.id.rvReminders);
        etReminderTitle = findViewById(R.id.etReminderTitle);
        etReminderDescription = findViewById(R.id.etReminderDescription);
        etRecurrenceValue = findViewById(R.id.etRecurrenceValue);
        btnSaveReminder = findViewById(R.id.btnSaveReminder);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnSelectTime = findViewById(R.id.btnSelectTime);
        spFamilyMembers = findViewById(R.id.spFamilyMembers);
        spPets = findViewById(R.id.spPets);
        spRecurrenceType = findViewById(R.id.spRecurrenceType);
        rgReminderType = findViewById(R.id.rgReminderType);
        rbOneTime = findViewById(R.id.rbOneTime);
        rbRecurring = findViewById(R.id.rbRecurring);
        llRecurringOptions = findViewById(R.id.llRecurringOptions);

        updateDateTimeButtons();
    }

    private void setupListeners() {
        addReminderIcon.setOnClickListener(v -> toggleFormVisibility());
        btnSaveReminder.setOnClickListener(v -> saveReminder());
        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnSelectTime.setOnClickListener(v -> showTimePicker());

        rgReminderType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRecurring) {
                llRecurringOptions.setVisibility(View.VISIBLE);
            } else {
                llRecurringOptions.setVisibility(View.GONE);
            }
        });
    }

    private void setupRecyclerView() {
        rvReminders.setLayoutManager(new LinearLayoutManager(this));
        reminderAdapter = new ReminderAdapter(reminderList, new ReminderAdapter.OnReminderClickListener() {
            @Override
            public void onReminderClick(Reminder reminder) {
                showReminderDetails(reminder);
            }

            @Override
            public void onDeleteReminder(Reminder reminder) {
                deleteReminder(reminder);
            }
        });
        rvReminders.setAdapter(reminderAdapter);
    }

    private void setupRecurrenceSpinner() {
        String[] recurrenceTypes = {"Día(s)", "Semana(s)", "Mes(es)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, recurrenceTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRecurrenceType.setAdapter(adapter);
    }

    private void loadFamilyMembers() {
        String url = getString(R.string.apiUrl) + "families";

        Log.d("Recordatorios", "Loading family members from: " + url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("Recordatorios", "Family members response: " + response.toString());
                    try {
                        availableFamilyMembers.clear();
                        List<String> memberNames = new ArrayList<>();
                        memberNames.add("Selecciona un familiar (Todos verán el recordatorio)");

                        // Agregar opción "Todos"
                        Reminder.FamilyMember todos = new Reminder.FamilyMember();
                        todos.id = -1; // Usamos -1 para representar "Todos"
                        todos.name = "Todos los miembros";
                        availableFamilyMembers.add(todos);
                        memberNames.add(todos.name);

                        // Procesar la respuesta de la API
                        if (response.length() > 0) {
                            JSONObject family = response.getJSONObject(0); // Primera familia
                            JSONArray members = family.getJSONArray("members");

                            // Agregar usuario actual primero
                            Reminder.FamilyMember currentUser = new Reminder.FamilyMember();
                            currentUser.id = authManager.getUserId();
                            currentUser.name = "Yo (" + authManager.getUserName() + ")";
                            availableFamilyMembers.add(currentUser);
                            memberNames.add(currentUser.name);

                            // Agregar otros miembros de la familia
                            for (int i = 0; i < members.length(); i++) {
                                JSONObject member = members.getJSONObject(i);
                                // Verificar que no sea el usuario actual
                                if (member.getInt("id") != authManager.getUserId()) {
                                    Reminder.FamilyMember familyMember = new Reminder.FamilyMember();
                                    familyMember.id = member.getInt("id");
                                    familyMember.name = member.getString("first_name") + " " + member.getString("last_name");
                                    availableFamilyMembers.add(familyMember);
                                    memberNames.add(familyMember.name);
                                }
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, memberNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spFamilyMembers.setAdapter(adapter);

                        // Seleccionar "Todos" por defecto
                        spFamilyMembers.setSelection(1);
                    } catch (JSONException e) {
                        Log.e("Recordatorios", "Error parsing family members", e);
                        Toast.makeText(this, "Error al procesar familiares", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMsg = "Error al cargar familiares";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg += ": " + new String(error.networkResponse.data);
                    }
                    Log.e("Recordatorios", errorMsg, error);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }


    private void loadPets() {
        String url = getString(R.string.apiUrl) + "pets/";

        Log.d("Recordatorios", "Loading pets from: " + url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("Recordatorios", "Pets response: " + response.toString());
                    try {
                        availablePets.clear();
                        List<String> petNames = new ArrayList<>();
                        petNames.add("Selecciona una mascota");

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject pet = response.getJSONObject(i);
                            Reminder.Pet petObj = new Reminder.Pet();
                            petObj.id = pet.getInt("id");
                            petObj.name = pet.getString("name");
                            petObj.photoUrl = pet.optString("photo_url", null);
                            availablePets.add(petObj);
                            petNames.add(petObj.name);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, petNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spPets.setAdapter(adapter);
                    } catch (JSONException e) {
                        Log.e("Recordatorios", "Error parsing pets", e);
                        Toast.makeText(this, "Error al procesar mascotas", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMsg = "Error al cargar mascotas";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg += ": " + new String(error.networkResponse.data);
                    }
                    Log.e("Recordatorios", errorMsg, error);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void loadReminders() {
        String url = getString(R.string.apiUrl) + "reminders/";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        reminderList.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject reminderJson = response.getJSONObject(i);
                            Reminder reminder = parseReminderFromJson(reminderJson);
                            reminderList.add(reminder);
                        }
                        reminderAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Log.e("Recordatorios", "Error parsing reminders", e);
                        Toast.makeText(this, "Error al procesar recordatorios", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMsg = "Error al cargar recordatorios";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg += ": " + new String(error.networkResponse.data);
                    }
                    Log.e("Recordatorios", errorMsg, error);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private Reminder parseReminderFromJson(JSONObject json) throws JSONException {
        Reminder reminder = new Reminder();
        reminder.id = json.getInt("id");
        reminder.title = json.getString("title");
        reminder.description = json.getString("description");
        reminder.dueDate = json.getString("due_date");
        reminder.isRecurring = json.getBoolean("is_recurring");
        reminder.recurrenceType = json.getString("recurrence_type");
        reminder.recurrenceValue = json.getInt("recurrence_value");

        // Handle pet data
        if (!json.isNull("pet")) {
            try {
                // Pet might be just an ID or an object
                if (json.get("pet") instanceof JSONObject) {
                    JSONObject petJson = json.getJSONObject("pet");
                    Reminder.Pet pet = new Reminder.Pet();
                    pet.id = petJson.getInt("id");
                    pet.name = petJson.getString("name");
                    pet.photoUrl = petJson.optString("photo_url", null);
                    reminder.pet = pet;
                } else {
                    // If pet is just an ID, find it in availablePets
                    int petId = json.getInt("pet");
                    for (Reminder.Pet pet : availablePets) {
                        if (pet.id == petId) {
                            reminder.pet = pet;
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e("Recordatorios", "Error parsing pet data", e);
            }
        }

        // Handle assigned user
        if (!json.isNull("assigned_to")) {
            int assignedToId = json.getInt("assigned_to");
            for (Reminder.FamilyMember member : availableFamilyMembers) {
                if (member.id == assignedToId) {
                    reminder.assignedTo = member;
                    break;
                }
            }
        } else if (!json.isNull("family")) {
            // If no assigned_to but has family, it's for all members
            Reminder.FamilyMember todos = new Reminder.FamilyMember();
            todos.id = -1;
            todos.name = "Todos los miembros";
            reminder.assignedTo = todos;
        }

        return reminder;
    }

    private void toggleFormVisibility() {
        if (formContainer.getVisibility() == View.VISIBLE) {
            formContainer.setVisibility(View.GONE);
            rvReminders.setVisibility(View.VISIBLE);
        } else {
            formContainer.setVisibility(View.VISIBLE);
            rvReminders.setVisibility(View.GONE);
            clearForm();
        }
    }

    private void clearForm() {
        etReminderTitle.setText("");
        etReminderDescription.setText("");
        etRecurrenceValue.setText("");
        spFamilyMembers.setSelection(0);
        spPets.setSelection(0);
        rbOneTime.setChecked(true);
        selectedDateTime = Calendar.getInstance();
        updateDateTimeButtons();
    }

    private void updateDateTimeButtons() {
        btnSelectDate.setText(dateFormat.format(selectedDateTime.getTime()));
        btnSelectTime.setText(timeFormat.format(selectedDateTime.getTime()));
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                this::onDateSet,
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        selectedDateTime.set(Calendar.YEAR, year);
        selectedDateTime.set(Calendar.MONTH, month);
        selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateDateTimeButtons();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                this::onTimeSet,
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }

    private void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        selectedDateTime.set(Calendar.MINUTE, minute);
        updateDateTimeButtons();
    }

    private void saveReminder() {
        String title = etReminderTitle.getText().toString().trim();
        String description = etReminderDescription.getText().toString().trim();
        boolean isRecurring = rbRecurring.isChecked();

        if (title.isEmpty()) {
            etReminderTitle.setError("El título es obligatorio");
            return;
        }

        if (isRecurring && etRecurrenceValue.getText().toString().isEmpty()) {
            etRecurrenceValue.setError("Ingresa un valor");
            return;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("title", title);
            requestBody.put("description", description);
            requestBody.put("due_date", apiDateFormat.format(selectedDateTime.getTime()));
            requestBody.put("is_recurring", isRecurring);
            requestBody.put("user", authManager.getUserId()); // Changed from created_by to user

            if (isRecurring) {
                String recurrenceType = spRecurrenceType.getSelectedItem().toString();
                requestBody.put("recurrence_type", recurrenceType.equals("Día(s)") ? "DAILY" :
                        recurrenceType.equals("Semana(s)") ? "WEEKLY" : "MONTHLY");
                requestBody.put("recurrence_value", Integer.parseInt(etRecurrenceValue.getText().toString()));
            }

            if (spPets.getSelectedItemPosition() > 0) {
                requestBody.put("pet", availablePets.get(spPets.getSelectedItemPosition() - 1).id);
            }

            // Handle family member selection
            int selectedPosition = spFamilyMembers.getSelectedItemPosition();
            if (selectedPosition > 0) {
                Reminder.FamilyMember selected = availableFamilyMembers.get(selectedPosition - 1);
                if (selected.id != -1) { // Specific member
                    requestBody.put("assigned_to", selected.id);
                }
            }

            // Only send family ID if user actually has a family
            int familyId = authManager.getFamilyId();
            if (familyId > 0) { // Changed from != -1 to > 0
                requestBody.put("family", familyId);
            }

            Log.d("Recordatorios", "Enviando recordatorio: " + requestBody.toString());

            String url = getString(R.string.apiUrl) + "reminders/";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                    response -> {
                        Log.d("Recordatorios", "Recordatorio creado: " + response.toString());
                        Toast.makeText(this, "Recordatorio creado", Toast.LENGTH_SHORT).show();
                        loadReminders();
                        toggleFormVisibility();
                    },
                    error -> {
                        try {
                            String errorMsg = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            Log.e("Recordatorios", "Error al crear recordatorio: " + errorMsg);
                            Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("Recordatorios", "Error al crear recordatorio", e);
                            Toast.makeText(this, "Error al crear recordatorio", Toast.LENGTH_SHORT).show();
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            requestQueue.add(request);
        } catch (Exception e) {
            Log.e("Recordatorios", "Error al preparar la solicitud", e);
            Toast.makeText(this, "Error al preparar la solicitud: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showReminderDetails(Reminder reminder) {
        StringBuilder details = new StringBuilder();
        details.append("Título: ").append(reminder.getTitle()).append("\n\n");
        details.append("Descripción: ").append(reminder.getDescription()).append("\n\n");

        try {
            Date date = apiDateFormat.parse(reminder.getDueDate());
            details.append("Fecha y hora: ").append(dateFormat.format(date)).append(" ").append(timeFormat.format(date)).append("\n");
        } catch (Exception e) {
            details.append("Fecha y hora: ").append(reminder.getDueDate()).append("\n");
        }

        if (reminder.getAssignedTo() != null) {
            details.append("Asignado a: ").append(reminder.getAssignedTo().name).append("\n");
        } else {
            details.append("Asignado a: Todos\n");
        }

        if (reminder.getPet() != null) {
            details.append("Mascota: ").append(reminder.getPet().name).append("\n");
        }

        if (reminder.isRecurring()) {
            String recurrenceText = "Repetición: Cada " + reminder.getRecurrenceValue() + " ";
            switch (reminder.getRecurrenceType()) {
                case "DAILY": recurrenceText += "día(s)"; break;
                case "WEEKLY": recurrenceText += "semana(s)"; break;
                case "MONTHLY": recurrenceText += "mes(es)"; break;
            }
            details.append(recurrenceText).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Detalles del recordatorio")
                .setMessage(details.toString())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void deleteReminder(Reminder reminder) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar recordatorio")
                .setMessage("¿Estás seguro de que quieres eliminar este recordatorio?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    String url = getString(R.string.apiUrl) + "reminders/" + reminder.getId() + "/";

                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, url, null,
                            response -> {
                                Toast.makeText(recordatorios.this, "Recordatorio eliminado", Toast.LENGTH_SHORT).show();
                                loadReminders();
                            },
                            error -> Toast.makeText(recordatorios.this, "Error al eliminar recordatorio", Toast.LENGTH_SHORT).show()
                    ) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                            return headers;
                        }
                    };

                    requestQueue.add(request);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}