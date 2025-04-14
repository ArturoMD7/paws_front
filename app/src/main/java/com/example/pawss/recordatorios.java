package com.example.pawss;

import static android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
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

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

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

    private static final int REQUEST_REMINDER_PHOTO = 1001;
    private static final int REQUEST_CAMERA_PERMISSION = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1003;
    private Integer pendingReminderId = null;


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


        if (getIntent() != null && "COMPLETE_REMINDER".equals(getIntent().getAction())) {
            int reminderId = getIntent().getIntExtra("reminderId", -1);
            if (reminderId != -1) {
                pendingReminderId = reminderId;
            }
            getIntent().removeExtra("reminderId");
            getIntent().setAction(null);
        }




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

            @Override
            public void onCompleteReminder(Reminder reminder) {
                showCompleteReminderDialog(reminder.getId());
            }
        });
        rvReminders.setAdapter(reminderAdapter); // Faltaba esta línea
    }

    private void completeReminder(int reminderId) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Completando recordatorio...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String url = getString(R.string.apiUrl) + "reminders/" + reminderId + "/complete/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Recordatorio completado", Toast.LENGTH_SHORT).show();
                    loadReminders();
                },
                error -> {
                    progressDialog.dismiss();
                    String errorMsg = "Error al completar recordatorio";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg += ": " + new String(error.networkResponse.data);
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
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

    private void setupRecurrenceSpinner() {
        String[] recurrenceTypes = {"Día(s)", "Semana(s)", "Mes(es)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, recurrenceTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRecurrenceType.setAdapter(adapter);
    }

    private void showCompleteReminderDialog(int reminderId) {
        getIntent().setAction("");
        getIntent().removeExtra("reminderId");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Completar recordatorio");

        View view = getLayoutInflater().inflate(R.layout.dialog_complete_reminder, null);
        ImageView ivPhoto = view.findViewById(R.id.ivPhoto);
        Button btnTakePhoto = view.findViewById(R.id.btnTakePhoto);

        builder.setView(view);

        AlertDialog dialog = builder.create();

        btnTakePhoto.setOnClickListener(v -> {
            try {
                openCameraForReminder(reminderId);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openCameraForReminder(int reminderId) throws IOException {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        photoFile
                );

                // Guardar temporalmente el reminderId y la URI
                SharedPreferences prefs = getSharedPreferences("TempReminder", MODE_PRIVATE);
                prefs.edit()
                        .putInt("currentReminderId", reminderId)
                        .putString("photoUri", photoUri.toString())
                        .apply();

                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, REQUEST_REMINDER_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_REMINDER_PHOTO && resultCode == RESULT_OK) {
            SharedPreferences prefs = getSharedPreferences("TempReminder", MODE_PRIVATE);
            int reminderId = prefs.getInt("currentReminderId", -1);
            String photoUriStr = prefs.getString("photoUri", null);

            if (reminderId != -1 && photoUriStr != null) {
                Uri photoUri = Uri.parse(photoUriStr);
                uploadReminderCompletion(reminderId, photoUri);
            }

            // Limpiar preferencias
            prefs.edit().clear().apply();
        }
    }

    private void uploadReminderCompletion(int reminderId, Uri photoUri) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Completando recordatorio...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 1. Crear el mapa de parámetros (si tu backend los requiere)
        Map<String, String> params = new HashMap<>();
        params.put("user_id", String.valueOf(authManager.getUserId()));

        // 2. Preparar los datos de la imagen
        Map<String, VolleyMultipartRequest.DataPart> byteData = new HashMap<>();
        try {
            InputStream imageStream = getContentResolver().openInputStream(photoUri);
            byte[] imageData = getBytes(imageStream);

            // Usar un nombre de archivo único
            String fileName = "reminder_" + reminderId + "_" + System.currentTimeMillis() + ".jpg";
            byteData.put("photo", new VolleyMultipartRequest.DataPart(
                    fileName,
                    imageData,
                    "image/jpeg"
            ));
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error al procesar la imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Configurar la petición
        VolleyMultipartRequest request = new VolleyMultipartRequest(
                Request.Method.POST,
                getString(R.string.apiUrl) + "reminders/" + reminderId + "/complete/",
                response -> {
                    progressDialog.dismiss();
                    try {
                        String responseString = new String(response.data, "UTF-8");
                        Log.d("API_RESPONSE", "Respuesta: " + responseString);
                        Toast.makeText(this, "¡Recordatorio completado!", Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(this::loadReminders, 1000);
                        cancelReminderAlarm(reminderId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    String errorMsg = "Error al completar";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg += ": " + new String(error.networkResponse.data);
                    }
                    Log.e("API_ERROR", errorMsg, error);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                return headers;
            }
        };

        // 4. Asignar los datos a la petición
        request.setParams(params);
        request.setByteData(byteData);

        // 5. Agregar timeout y reintentos
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,  // 10 segundos timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        try {
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e("Reminder", "Error al cerrar el stream", e);
            }
            try {
                byteBuffer.close();
            } catch (IOException e) {
                Log.e("Reminder", "Error al cerrar el buffer", e);
            }
        }
    }


    private void loadFamilyMembers() {
        String url = getString(R.string.apiUrl) + "families";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                this::handleFamilyMembersResponse,
                this::handleFamilyMembersError) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private void handleFamilyMembersResponse(JSONArray response) {
        try {
            availableFamilyMembers.clear();
            List<String> memberNames = new ArrayList<>();
            memberNames.add("Selecciona un familiar (Todos verán el recordatorio)");

            // Agregar opción "Todos"
            Reminder.FamilyMember todos = new Reminder.FamilyMember();
            todos.id = -1;
            todos.name = "Todos los miembros";
            availableFamilyMembers.add(todos);
            memberNames.add(todos.name);

            if (response.length() > 0) {
                JSONObject family = response.getJSONObject(0);
                JSONArray members = family.getJSONArray("members");

                // Agregar usuario actual
                Reminder.FamilyMember currentUser = new Reminder.FamilyMember();
                currentUser.id = authManager.getUserId();
                currentUser.name = "Yo (" + authManager.getUserName() + ")";
                availableFamilyMembers.add(currentUser);
                memberNames.add(currentUser.name);

                // Agregar otros miembros
                for (int i = 0; i < members.length(); i++) {
                    JSONObject member = members.getJSONObject(i);
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
            spFamilyMembers.setSelection(1);
        } catch (JSONException e) {
            Log.e("Recordatorios", "Error parsing family members", e);
            Toast.makeText(this, "Error al procesar familiares", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleFamilyMembersError(VolleyError error) {
        String errorMsg = "Error al cargar familiares";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            errorMsg += ": " + new String(error.networkResponse.data);
        }
        Log.e("Recordatorios", errorMsg, error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

    private void loadPets() {
        String url = getString(R.string.apiUrl) + "pets/";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                this::handlePetsResponse,
                this::handlePetsError) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private void handlePetsResponse(JSONArray response) {
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
    }

    private void handlePetsError(VolleyError error) {
        String errorMsg = "Error al cargar mascotas";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            errorMsg += ": " + new String(error.networkResponse.data);
        }
        Log.e("Recordatorios", errorMsg, error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

    private void loadReminders() {
        String url = getString(R.string.apiUrl) + "reminders/";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                this::handleRemindersResponse,
                this::handleRemindersError) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createAuthHeaders();
            }
        };

        requestQueue.add(request);
        if (pendingReminderId != null) {
            showCompleteReminderDialog(pendingReminderId);
            pendingReminderId = null;
        }
    }

    private void handleRemindersResponse(JSONArray response) {
        try {
            reminderList.clear();
            for (int i = 0; i < response.length(); i++) {
                JSONObject reminderJson = response.getJSONObject(i);
                Reminder reminder = parseReminderFromJson(reminderJson);
                if (reminder != null) {
                    reminderList.add(reminder);
                }
            }
            reminderAdapter.notifyDataSetChanged();
            if (pendingReminderId != null) {
                showCompleteReminderDialog(pendingReminderId);
                pendingReminderId = null;
            }
        } catch (JSONException e) {
            Log.e("Recordatorios", "Error parsing reminders", e);
            Toast.makeText(this, "Error al procesar recordatorios", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRemindersError(VolleyError error) {
        String errorMsg = "Error al cargar recordatorios";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            errorMsg += ": " + new String(error.networkResponse.data);
        }
        Log.e("Recordatorios", errorMsg, error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

    private Reminder parseReminderFromJson(JSONObject json) throws JSONException {
        Reminder reminder = new Reminder();
        try {
            reminder.id = json.getInt("id");
            reminder.title = json.getString("title");
            reminder.description = json.getString("description");
            reminder.isRecurring = json.getBoolean("is_recurring");
            reminder.recurrenceType = json.getString("recurrence_type");
            reminder.recurrenceValue = json.getInt("recurrence_value");
            reminder.status = json.optString("status", "PENDING");
            String dueDateRaw = json.getString("due_date");
            reminder.dueDate = dueDateRaw;

            // Programar alarma si es una fecha futura
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(dueDateRaw);
                if (date != null && date.getTime() > System.currentTimeMillis()) {
                    scheduleReminderAlarm(
                            reminder.id,
                            reminder.title,
                            reminder.description,
                            date.getTime()
                    );
                }
            } catch (Exception e) {
                Log.e("Recordatorios", "Error al parsear fecha", e);
            }

            // Manejar mascota
            if (!json.isNull("pet")) {
                if (json.get("pet") instanceof JSONObject) {
                    JSONObject petJson = json.getJSONObject("pet");
                    Reminder.Pet pet = new Reminder.Pet();
                    pet.id = petJson.getInt("id");
                    pet.name = petJson.getString("name");
                    pet.photoUrl = petJson.optString("photo_url", null);
                    reminder.pet = pet;
                } else {
                    int petId = json.getInt("pet");
                    for (Reminder.Pet pet : availablePets) {
                        if (pet.id == petId) {
                            reminder.pet = pet;
                            break;
                        }
                    }
                }
            }

            // Manejar usuario asignado
            if (!json.isNull("assigned_to")) {
                int assignedToId = json.getInt("assigned_to");
                for (Reminder.FamilyMember member : availableFamilyMembers) {
                    if (member.id == assignedToId) {
                        reminder.assignedTo = member;
                        break;
                    }
                }
            } else if (!json.isNull("family")) {
                Reminder.FamilyMember todos = new Reminder.FamilyMember();
                todos.id = -1;
                todos.name = "Todos los miembros";
                reminder.assignedTo = todos;
            }

        } catch (JSONException e) {
            Log.e("Recordatorios", "Error parsing reminder JSON", e);
            return null;
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
        final String title = etReminderTitle.getText().toString().trim();
        final String description = etReminderDescription.getText().toString().trim();
        final boolean isRecurring = rbRecurring.isChecked();

        if (title.isEmpty()) {
            etReminderTitle.setError("El título es obligatorio");
            return;
        }

        if (isRecurring && etRecurrenceValue.getText().toString().isEmpty()) {
            etRecurrenceValue.setError("Ingresa un valor");
            return;
        }

        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        apiDateFormat.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
        final String dueDate = apiDateFormat.format(selectedDateTime.getTime());

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("title", title);
            requestBody.put("description", description);
            requestBody.put("due_date", dueDate);
            requestBody.put("is_recurring", isRecurring);
            requestBody.put("user", authManager.getUserId());

            if (isRecurring) {
                String recurrenceType = spRecurrenceType.getSelectedItem().toString();
                requestBody.put("recurrence_type", recurrenceType.equals("Día(s)") ? "DAILY" :
                        recurrenceType.equals("Semana(s)") ? "WEEKLY" : "MONTHLY");
                requestBody.put("recurrence_value", Integer.parseInt(etRecurrenceValue.getText().toString()));
            }

            if (spPets.getSelectedItemPosition() > 0) {
                requestBody.put("pet", availablePets.get(spPets.getSelectedItemPosition() - 1).id);
            }

            int selectedPosition = spFamilyMembers.getSelectedItemPosition();
            if (selectedPosition > 0) {
                Reminder.FamilyMember selected = availableFamilyMembers.get(selectedPosition - 1);
                if (selected.id != -1) {
                    requestBody.put("assigned_to", selected.id);
                }
            }

            int familyId = authManager.getFamilyId();
            if (familyId > 0) {
                requestBody.put("family", familyId);
            }

            String url = getString(R.string.apiUrl) + "reminders/";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                    response -> {
                        try {
                            int reminderId = response.getInt("id");
                            scheduleReminderAlarm(reminderId, title, description, selectedDateTime.getTimeInMillis());
                            Toast.makeText(this, "Recordatorio creado", Toast.LENGTH_SHORT).show();
                            loadReminders();
                            toggleFormVisibility();
                        } catch (JSONException e) {
                            Log.e("Recordatorios", "Error al parsear respuesta", e);
                            Toast.makeText(this, "Error al crear recordatorio", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        String errorMsg = "Error al crear recordatorio";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMsg += ": " + new String(error.networkResponse.data);
                        }
                        Log.e("Recordatorios", errorMsg, error);
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
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

    private void scheduleReminderAlarm(int reminderId, String title, String description, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderAlarmReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("description", description);
        intent.putExtra("reminderId", reminderId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                setExactAlarm(alarmManager, triggerAtMillis, pendingIntent);
            } else {
                requestExactAlarmPermission();
            }
        } else {
            setExactAlarm(alarmManager, triggerAtMillis, pendingIntent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestExactAlarmPermission() {
        Intent intent = new Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        startActivity(intent);
        Toast.makeText(this, "Por favor permite alarmas exactas en configuración", Toast.LENGTH_LONG).show();
    }

    private void setExactAlarm(AlarmManager alarmManager, long triggerAtMillis, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }
        Log.d("Recordatorios", "Alarma programada para: " + new Date(triggerAtMillis));
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

                    StringRequest request = new StringRequest(Request.Method.DELETE, url,
                            response -> {
                                Toast.makeText(this, "Recordatorio eliminado", Toast.LENGTH_SHORT).show();
                                loadReminders();
                            },
                            error -> {
                                String errorMsg = "Error al eliminar recordatorio";
                                if (error.networkResponse != null && error.networkResponse.data != null) {
                                    errorMsg += ": " + new String(error.networkResponse.data);
                                }
                                Log.e("Recordatorios", errorMsg, error);
                                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                            }
                    ) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            return createAuthHeaders();
                        }
                    };

                    requestQueue.add(request);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private Map<String, String> createAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + authManager.getAccessToken());
        return headers;
    }

    private void cancelReminderAlarm(int reminderId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderAlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        Log.d("Recordatorios", "Alarma cancelada para el recordatorio ID: " + reminderId);
    }

}