package com.example.pawss;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.BuildConfig;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class pets extends BaseActivity {

    private ImageView addPetIcon;
    private ScrollView formContainer;
    private RecyclerView rvPets;
    private EditText etPetName, etPetAge, etPetBreed, etAdoptionDate;
    private Spinner spPetType;
    private Button btnSavePet, btnSelectImage, btnSelectVaccineFile;
    private ImageView ivPetPhotoPreview;
    private TextView tvVaccineFileName;

    private RequestQueue requestQueue;
    private AuthManager authManager;
    private PetAdapter petAdapter;
    private List<Pet> petList = new ArrayList<>();
    private Uri imageUri, vaccineUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VACCINE_FILE_REQUEST = 2;


    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_pets;
    }

    @Override
    protected int getCurrentNavItem() {
        return R.id.nav_pets;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager(this);
        requestQueue = Volley.newRequestQueue(this);

        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Debe iniciar sesión primero", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupPetTypeSpinner();
        setupDatePicker();
        setupListeners();
        setupRecyclerView();
        loadPets();

        Picasso.setSingletonInstance(
                new Picasso.Builder(this)
                        .loggingEnabled(true)
                        .indicatorsEnabled(BuildConfig.DEBUG)
                        .build()
        );

    }

    private void initializeViews() {
        addPetIcon = findViewById(R.id.addPetIcon);
        formContainer = findViewById(R.id.formContainer);
        rvPets = findViewById(R.id.rvPets);
        etPetName = findViewById(R.id.etPetName);
        etPetAge = findViewById(R.id.etPetAge);
        etPetBreed = findViewById(R.id.etPetBreed);
        etAdoptionDate = findViewById(R.id.etAdoptionDate);
        spPetType = findViewById(R.id.spPetType);
        btnSavePet = findViewById(R.id.btnSavePet);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSelectVaccineFile = findViewById(R.id.btnSelectVaccineFile);
        ivPetPhotoPreview = findViewById(R.id.ivPetPhotoPreview);
        tvVaccineFileName = findViewById(R.id.tvVaccineFileName);
    }

    private void setupRecyclerView() {
        rvPets.setLayoutManager(new LinearLayoutManager(this));
        petAdapter = new PetAdapter(petList, this::onPetClick, this::onDeletePet);
        rvPets.setAdapter(petAdapter);
    }

    private int getFamilyId() {
        int familyId = authManager.getFamilyId();
        if (familyId == -1) {
            Log.e("PetLoad", "Family ID not found in AuthManager");
            Toast.makeText(this, "Error: No se encontró el ID de la familia", Toast.LENGTH_SHORT).show();
        }
        return familyId;
    }

    private void loadPets() {
        String apiUrl = getString(R.string.apiUrl);
        String url = apiUrl + "pets/";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray petsArray = new JSONArray(response);
                        petList.clear();

                        // Primero obtener la familia del usuario
                        int familyId = getFamilyId(); // Implementa este método según tu lógica

                        for (int i = 0; i < petsArray.length(); i++) {
                            JSONObject petJson = petsArray.getJSONObject(i);
                            JSONObject ownerJson = petJson.getJSONObject("owner");
                            int ownerFamilyId = ownerJson.getInt("family_id"); // Asume que el dueño tiene family_id

                            // Solo agregar mascotas de la misma familia
                            if (ownerFamilyId == familyId) {
                                Pet pet = new Pet(
                                        this,
                                        petJson.getInt("id"),
                                        petJson.getString("name"),
                                        petJson.getString("pet_type"),
                                        petJson.getInt("age"),
                                        petJson.getString("breed"),
                                        petJson.optString("adoption_date", ""),
                                        petJson.optString("photo", ""),
                                        petJson.optString("vaccines", "")
                                );
                                petList.add(pet);
                            }
                        }

                        petAdapter.notifyDataSetChanged();

                        if (petList.isEmpty()) {
                            Toast.makeText(this, "No hay mascotas registradas en tu familia", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("PetLoad", "Error parsing JSON", e);
                        Toast.makeText(this, "Error al procesar mascotas", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Manejo de errores
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }
    private void setupPetTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.pet_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPetType.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etAdoptionDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(),
                                "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        etAdoptionDate.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });
    }

    private void setupListeners() {
        addPetIcon.setOnClickListener(v -> toggleFormVisibility());
        btnSavePet.setOnClickListener(v -> savePet());
        btnSelectImage.setOnClickListener(v -> openImageChooser(PICK_IMAGE_REQUEST));
        btnSelectVaccineFile.setOnClickListener(v -> openFileChooser(PICK_VACCINE_FILE_REQUEST));
    }

    private void openImageChooser(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    private void openFileChooser(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                imageUri = data.getData();
                ivPetPhotoPreview.setImageURI(imageUri);
                ivPetPhotoPreview.setVisibility(View.VISIBLE);
            } else if (requestCode == PICK_VACCINE_FILE_REQUEST) {
                vaccineUri = data.getData();
                String fileName = getFileName(vaccineUri);
                tvVaccineFileName.setText(fileName);
                tvVaccineFileName.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void toggleFormVisibility() {
        if (formContainer.getVisibility() == View.VISIBLE) {
            formContainer.setVisibility(View.GONE);
            rvPets.setVisibility(View.VISIBLE);
        } else {
            formContainer.setVisibility(View.VISIBLE);
            rvPets.setVisibility(View.GONE);
            clearForm();
        }
    }

    private String getSelectedPetTypeValue() {
        String[] typeValues = getResources().getStringArray(R.array.pet_type_values);
        return typeValues[spPetType.getSelectedItemPosition()];
    }

    private void clearForm() {
        etPetName.setText("");
        etPetAge.setText("");
        etPetBreed.setText("");
        etAdoptionDate.setText("");
        spPetType.setSelection(0);
        ivPetPhotoPreview.setVisibility(View.GONE);
        ivPetPhotoPreview.setImageResource(R.drawable.photoframe);
        tvVaccineFileName.setVisibility(View.GONE);
        imageUri = null;
        vaccineUri = null;
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (etPetName.getText().toString().trim().isEmpty()) {
            etPetName.setError("Nombre es obligatorio");
            isValid = false;
        }

        try {
            int age = Integer.parseInt(etPetAge.getText().toString());
            if (age < 0 || age > 50) {
                etPetAge.setError("La edad debe ser entre 0 y 50");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            etPetAge.setError("Edad inválida");
            isValid = false;
        }

        if (etPetBreed.getText().toString().trim().isEmpty()) {
            etPetBreed.setError("Raza es obligatoria");
            isValid = false;
        }

        return isValid;
    }

    private void savePet() {
        if (!validateForm()) return;

        int ownerId = authManager.getUserId();
        if (ownerId == -1) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("owner", String.valueOf(ownerId));
        params.put("name", etPetName.getText().toString().trim());
        params.put("pet_type", getSelectedPetTypeValue());
        params.put("age", etPetAge.getText().toString().trim());
        params.put("breed", etPetBreed.getText().toString().trim());

        if (!etAdoptionDate.getText().toString().trim().isEmpty()) {
            params.put("adoption_date", etAdoptionDate.getText().toString().trim());
        }
        String apiUrl = getString(R.string.apiUrl);
        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(
                Request.Method.POST,
                apiUrl + "pets/",
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        try {
                            String responseString = new String(response.data);
                            JSONObject jsonResponse = new JSONObject(responseString);
                            if (jsonResponse.has("id")) {
                                Toast.makeText(pets.this, "Mascota guardada correctamente", Toast.LENGTH_SHORT).show();
                                clearForm();
                                toggleFormVisibility();
                                loadPets();
                            } else {
                                Toast.makeText(pets.this, "Error en la respuesta del servidor", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(pets.this, "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Error al guardar la mascota";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            errorMessage += ": " + new String(error.networkResponse.data);
                        }
                        Toast.makeText(pets.this, errorMessage, Toast.LENGTH_LONG).show();
                        Log.e("API_ERROR", "Error: " + errorMessage, error);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                return headers;
            }

            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    if (imageUri != null) {
                        InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        byte[] imageData = getBytes(imageStream);
                        params.put("photo", new DataPart("pet_photo.jpg", imageData, "image/jpeg"));
                    }

                    if (vaccineUri != null) {
                        InputStream vaccineStream = getContentResolver().openInputStream(vaccineUri);
                        byte[] vaccineData = getBytes(vaccineStream);
                        String mimeType = getContentResolver().getType(vaccineUri);
                        String fileName = getFileName(vaccineUri);
                        params.put("vaccines", new DataPart(fileName, vaccineData, mimeType));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(pets.this, "Error al leer los archivos", Toast.LENGTH_SHORT).show();
                }
                return params;
            }
        };

        requestQueue.add(multipartRequest);
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void onPetClick(Pet pet) {
        showPetDetailsDialog(pet);
    }

    private void onDeletePet(Pet pet) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar mascota")
                .setMessage("¿Estás seguro de que quieres eliminar a " + pet.getName() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> deletePet(pet.getId()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletePet(int petId) {
        String apiUrl = getString(R.string.apiUrl);
        String url = apiUrl + "pets/" + petId + "/";

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(this, "Mascota eliminada", Toast.LENGTH_SHORT).show();
                    loadPets();
                },
                error -> {
                    String errorMsg = "Error al eliminar mascota";
                    if (error.networkResponse != null) {
                        errorMsg += ": " + new String(error.networkResponse.data);
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void showPetDetailsDialog(Pet pet) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_pet_details, null);
        builder.setView(view);

        ImageView ivPetPhoto = view.findViewById(R.id.ivPetPhoto);
        TextView tvPetName = view.findViewById(R.id.tvPetName);
        TextView tvPetType = view.findViewById(R.id.tvPetType);
        TextView tvPetAge = view.findViewById(R.id.tvPetAge);
        TextView tvPetBreed = view.findViewById(R.id.tvPetBreed);
        TextView tvAdoptionDate = view.findViewById(R.id.tvAdoptionDate);
        Button btnVaccines = view.findViewById(R.id.btnVaccines);

        tvPetName.setText(pet.getName());
        tvPetType.setText("Tipo: " + pet.getType());
        tvPetAge.setText("Edad: " + pet.getAge() + " años");
        tvPetBreed.setText("Raza: " + pet.getBreed());

        String imageUrl = pet.getPhotoUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.photoframe)
                    .error(R.drawable.warning)
                    .fit()
                    .centerCrop()
                    .into(ivPetPhoto, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("PetDialog", "Imagen cargada: " + imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("PetDialog", "Error al cargar imagen: " + imageUrl, e);
                            ivPetPhoto.setImageResource(R.drawable.happy);
                        }
                    });
        } else {
            ivPetPhoto.setImageResource(R.drawable.happy);
        }

        if (pet.getVaccinesUrl() == null || pet.getVaccinesUrl().isEmpty()) {
            btnVaccines.setVisibility(View.GONE);
        } else {
            btnVaccines.setText("Descargar archivo de vacunas");
            btnVaccines.setOnClickListener(v -> {
                downloadVaccineFile(pet.getVaccinesUrl());
            });
        }

        builder.setPositiveButton("Cerrar", null);
        builder.create().show();
    }

    private void downloadVaccineFile(String fileUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle("Descargando archivo de vacunas");
        request.setDescription("Descargando...");

        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        Toast.makeText(this, "Descarga iniciada", Toast.LENGTH_SHORT).show();
    }

    public class VolleyMultipartRequest extends Request<NetworkResponse> {
        private final String twoHyphens = "--";
        private final String lineEnd = "\r\n";
        private final String boundary = "apiclient-" + System.currentTimeMillis();

        private Response.Listener<NetworkResponse> mListener;
        private Response.ErrorListener mErrorListener;
        private Map<String, String> mHeaders;
        private Map<String, String> mParams;
        private Map<String, DataPart> mByteData;

        public VolleyMultipartRequest(int method, String url,
                                      Response.Listener<NetworkResponse> listener,
                                      Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            this.mListener = listener;
            this.mErrorListener = errorListener;
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return (mHeaders != null) ? mHeaders : super.getHeaders();
        }

        public void setHeaders(Map<String, String> headers) {
            this.mHeaders = headers;
        }

        public void setParams(Map<String, String> params) {
            this.mParams = params;
        }

        public void setByteData(Map<String, DataPart> byteData) {
            this.mByteData = byteData;
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            return mParams;
        }

        protected Map<String, DataPart> getByteData() throws AuthFailureError {
            return mByteData;
        }

        @Override
        protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
            try {
                return Response.success(
                        response,
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new VolleyError(e));
            }
        }

        @Override
        protected void deliverResponse(NetworkResponse response) {
            mListener.onResponse(response);
        }

        @Override
        public void deliverError(VolleyError error) {
            mErrorListener.onErrorResponse(error);
        }

        @Override
        public String getBodyContentType() {
            return "multipart/form-data;boundary=" + boundary;
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);

            try {
                Map<String, String> params = getParams();
                if (params != null && params.size() > 0) {
                    textParse(dos, params);
                }

                Map<String, DataPart> data = getByteData();
                if (data != null && data.size() > 0) {
                    dataParse(dos, data);
                }

                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                return bos.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void textParse(DataOutputStream dataOutputStream, Map<String, String> params) throws IOException {
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    buildTextPart(dataOutputStream, entry.getKey(), entry.getValue());
                }
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException("Encoding not supported: " + "UTF-8", uee);
            }
        }

        private void dataParse(DataOutputStream dataOutputStream, Map<String, DataPart> data) throws IOException {
            for (Map.Entry<String, DataPart> entry : data.entrySet()) {
                buildDataPart(dataOutputStream, entry.getValue(), entry.getKey());
            }
        }

        private void buildTextPart(DataOutputStream dataOutputStream, String parameterName, String parameterValue) throws IOException {
            dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + lineEnd);
            dataOutputStream.writeBytes(lineEnd);
            dataOutputStream.writeBytes(parameterValue + lineEnd);
        }

        private void buildDataPart(DataOutputStream dataOutputStream, DataPart dataFile, String inputName) throws IOException {
            dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" +
                    inputName + "\"; filename=\"" + dataFile.getFileName() + "\"" + lineEnd);
            if (dataFile.getType() != null && !dataFile.getType().trim().isEmpty()) {
                dataOutputStream.writeBytes("Content-Type: " + dataFile.getType() + lineEnd);
            }
            dataOutputStream.writeBytes(lineEnd);

            ByteArrayInputStream fileInputStream = new ByteArrayInputStream(dataFile.getContent());
            int bytesAvailable = fileInputStream.available();

            int maxBufferSize = 1024 * 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];

            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {
                dataOutputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            dataOutputStream.writeBytes(lineEnd);
        }

        public class DataPart {
            private String fileName;
            private byte[] content;
            private String type;

            public DataPart(String name, byte[] data, String mimeType) {
                fileName = name;
                content = data;
                type = mimeType;
            }

            public String getFileName() {
                return fileName;
            }

            public byte[] getContent() {
                return content;
            }

            public String getType() {
                return type;
            }
        }
    }
}