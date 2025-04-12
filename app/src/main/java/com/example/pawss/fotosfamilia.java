package com.example.pawss;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.provider.MediaStore;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.Manifest;


public class fotosfamilia extends BaseActivity {

    // Views
    private ImageView addPostIcon;
    private ScrollView formContainer;
    private RecyclerView rvPosts;
    private EditText etPostContent;
    private Button btnSavePost, btnSelectImage;
    private ImageView ivPostImagePreview;
    private Spinner spPets;

    // Data
    private RequestQueue requestQueue;
    private AuthManager authManager;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();
    private List<Pet> availablePets = new ArrayList<>();
    private Uri imageUri;

    // Constants
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String DEFAULT_POST_TYPE = "UPDATE";
    private static final int REQUEST_IMAGE_CAMERA = 101;
    private static final int REQUEST_IMAGE_GALLERY = 102;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_fotosfamilia;
    }

    @Override
    protected int getCurrentNavItem() {
        return R.id.nav_fotosfamilia;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // NO pongas setContentView aquí

        // El resto está perfecto
        authManager = new AuthManager(this);
        requestQueue = Volley.newRequestQueue(this);

        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "Debe iniciar sesión primero", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        setupRecyclerView();
        loadPetsForSpinner();
        loadPosts();
    }

    private void initializeViews() {
        addPostIcon = findViewById(R.id.addPostIcon);
        formContainer = findViewById(R.id.formContainer);
        rvPosts = findViewById(R.id.rvPosts);
        etPostContent = findViewById(R.id.etPostContent);
        btnSavePost = findViewById(R.id.btnSavePost);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivPostImagePreview = findViewById(R.id.ivPostImagePreview);
        spPets = findViewById(R.id.spPets);
    }

    private void setupRecyclerView() {
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        postAdapter = new PostAdapter(postList, this::onPostClick, this::onDeletePost);
        rvPosts.setAdapter(postAdapter);
    }

    private void setupListeners() {
        addPostIcon.setOnClickListener(v -> toggleFormVisibility());
        btnSavePost.setOnClickListener(v -> savePost());
        btnSelectImage.setOnClickListener(v -> openImageChooser());
    }

    private void toggleFormVisibility() {
        if (formContainer.getVisibility() == View.VISIBLE) {
            formContainer.setVisibility(View.GONE);
            rvPosts.setVisibility(View.VISIBLE);
        } else {
            formContainer.setVisibility(View.VISIBLE);
            rvPosts.setVisibility(View.GONE);
            clearForm();
        }
    }

    private void clearForm() {
        etPostContent.setText("");
        spPets.setSelection(0);
        ivPostImagePreview.setVisibility(View.GONE);
        ivPostImagePreview.setImageResource(R.drawable.photoframe);
        imageUri = null;
    }

    private void openImageChooser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar imagen");
        builder.setItems(new CharSequence[]{"Tomar foto", "Elegir de galería"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    openCamera();
                    break;
                case 1:
                    openGallery();
                    break;
            }
        });
        builder.show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        photoFile
                );
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, REQUEST_IMAGE_CAMERA);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_GALLERY && data != null) {
                imageUri = data.getData();
            }

            if ((requestCode == REQUEST_IMAGE_CAMERA || requestCode == REQUEST_IMAGE_GALLERY) && imageUri != null) {
                ivPostImagePreview.setImageURI(imageUri);
                ivPostImagePreview.setVisibility(View.VISIBLE);
            }
        }
    }


    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            showToast("No se pudo crear el archivo de imagen");
            return null;
        }
    }

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showToast("Se necesita permiso de la cámara para tomar fotos");
            }
        }
    }


    private void loadPetsForSpinner() {
        String apiUrl = getString(R.string.apiUrl);
        String url = apiUrl + "pets/";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray petsArray = new JSONArray(response);
                        availablePets.clear();
                        List<String> petNames = new ArrayList<>();
                        petNames.add("Selecciona una mascota");

                        for (int i = 0; i < petsArray.length(); i++) {
                            JSONObject petJson = petsArray.getJSONObject(i);
                            Pet pet = new Pet(
                                    this,
                                    petJson.getInt("id"),
                                    petJson.getString("name"),
                                    petJson.getString("pet_type"),
                                    petJson.getInt("age"),
                                    petJson.getString("breed"),
                                    petJson.optString("adoption_date", ""),
                                    petJson.optString("photo_url", ""),
                                    petJson.optString("vaccines_url", "")
                            );
                            availablePets.add(pet);
                            petNames.add(pet.getName());
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_spinner_item,
                                petNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spPets.setAdapter(adapter);

                    } catch (JSONException e) {
                        Log.e("PetLoad", "Error parsing pets", e);
                        showToast("Error al cargar mascotas");
                    }
                },
                error -> {
                    Log.e("PetLoad", "Error loading pets", error);
                    showToast("Error al cargar mascotas");
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private void loadPosts() {
        String apiUrl = getString(R.string.apiUrl);
        String url = apiUrl + "posts/";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray postsArray = new JSONArray(response);
                        postList.clear();

                        for (int i = 0; i < postsArray.length(); i++) {
                            Post post = parsePostFromJson(postsArray.getJSONObject(i));
                            postList.add(post);
                        }

                        runOnUiThread(() -> {
                            postAdapter.notifyDataSetChanged();
                            if (postList.isEmpty()) {
                                showToast("No hay publicaciones aún");
                            }
                        });

                    } catch (JSONException e) {
                        Log.e("PostError", "Error parsing JSON", e);
                        showToast("Error al procesar publicaciones");
                    }
                },
                error -> {
                    Log.e("PostError", "Error loading posts", error);
                    showToast("Error al cargar publicaciones");
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

        private Post parsePostFromJson(JSONObject postJson) throws JSONException {
        int id = postJson.getInt("id");
        String content = postJson.getString("content");
        String postType = postJson.getString("post_type");
        String createdAt = postJson.getString("created_at");
        JSONArray imagesArray2 = postJson.getJSONArray("images");
        JSONObject firstImage = imagesArray2.getJSONObject(0);
        String authorName = firstImage.getString("author_name");

        // Pet info
        String petName = "";
        String petType = "";
        String petBreed = "";
        int petAge = 0;

        if (postJson.has("pet") && !postJson.isNull("pet")) {
            try {
                JSONObject petObj = postJson.getJSONObject("pet");
                petName = petObj.getString("name");
                petType = petObj.getString("pet_type");
                petBreed = petObj.getString("breed");
                petAge = petObj.getInt("age");
            } catch (JSONException e) {
                petName = "Mascota #" + postJson.getInt("pet");
            }
        }


        // Images
        List<String> imageUrls = new ArrayList<>();
        if (postJson.has("images")) {
            JSONArray imagesArray = postJson.getJSONArray("images");
            for (int j = 0; j < imagesArray.length(); j++) {
                JSONObject imageObj = imagesArray.getJSONObject(j);
                if (imageObj.has("photo")) {
                    imageUrls.add(imageObj.getString("photo"));
                }
            }
        }

        return new Post(
                this,
                id,
                content,
                postType,
                createdAt,
                authorName,
                petName,
                imageUrls,
                petType,
                petBreed,
                petAge
        );
    }

    private void savePost() {
        String content = etPostContent.getText().toString().trim();
        int selectedPetPosition = spPets.getSelectedItemPosition();

        if (!validatePostForm(content, selectedPetPosition)) {
            return;
        }

        Pet selectedPet = availablePets.get(selectedPetPosition - 1);

        VolleyMultipartRequest multipartRequest = createPostRequest(content, selectedPet.getId());
        requestQueue.add(multipartRequest);
    }


    private boolean validatePostForm(String content, int petPosition) {
        if (content.isEmpty()) {
            etPostContent.setError("El contenido es obligatorio");
            return false;
        }

        if (petPosition <= 0) {
            showToast("Selecciona una mascota");
            return false;
        }

        if (imageUri == null) {
            showToast("Selecciona una imagen");
            return false;
        }

        return true;
    }

    private VolleyMultipartRequest createPostRequest(String content, int petId) {
        String apiUrl = getString(R.string.apiUrl);

        return new VolleyMultipartRequest(
                Request.Method.POST,
                apiUrl + "posts/",
                response -> handlePostResponse(response),
                error -> handlePostError(error)
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createAuthHeaders();
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("content", content);
                params.put("post_type", DEFAULT_POST_TYPE);
                params.put("pet", String.valueOf(petId));
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    if (imageUri != null) {
                        InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        byte[] imageData = getBytes(imageStream);
                        params.put("photo", new DataPart("post_image.jpg", imageData, "image/jpeg"));
                    }
                } catch (Exception e) {
                    showToast("Error al leer la imagen");
                }
                return params;
            }
        };
    }

    private void handlePostResponse(NetworkResponse response) {
        try {
            String responseString = new String(response.data);
            JSONObject jsonResponse = new JSONObject(responseString);
            if (jsonResponse.has("id")) {
                showToast("Publicación creada correctamente");
                clearForm();
                toggleFormVisibility();
                loadPosts();
            }
        } catch (JSONException e) {
            showToast("Error al procesar respuesta");
        }
    }

    private void handlePostError(VolleyError error) {
        String errorMessage = "Error al crear publicación";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            errorMessage += ": " + new String(error.networkResponse.data);
        }
        showToast(errorMessage);
    }

    private void onPostClick(Post post) {
        showPostDetailsDialog(post);
    }

    private void onDeletePost(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar publicación")
                .setMessage("¿Estás seguro de que quieres eliminar esta publicación?")
                .setPositiveButton("Eliminar", (dialog, which) -> deletePost(post.getId()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletePost(int postId) {
        String apiUrl = getString(R.string.apiUrl);
        String url = apiUrl + "posts/" + postId + "/";

        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    showToast("Publicación eliminada");
                    loadPosts();
                },
                error -> {
                    String errorMsg = "Error al eliminar publicación";
                    if (error.networkResponse != null) {
                        errorMsg += ": " + new String(error.networkResponse.data);
                    }
                    showToast(errorMsg);
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return createAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private void showPostDetailsDialog(Post post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_post_details, null);
        builder.setView(view);

        // Initialize views
        TextView tvPetName = view.findViewById(R.id.tvPetName);
        TextView tvPetType = view.findViewById(R.id.tvPetType);
        TextView tvPetAge = view.findViewById(R.id.tvPetAge);
        TextView tvPetBreed = view.findViewById(R.id.tvPetBreed);
        TextView tvDate = view.findViewById(R.id.tvDate);
        TextView tvAuthor = view.findViewById(R.id.tvAuthor);
        TextView tvNotes = view.findViewById(R.id.tvNotes);
        ImageView ivPetPhoto = view.findViewById(R.id.ivPetPhoto);
        Button btnDownload = view.findViewById(R.id.btnDownload);
        Button btnEdit = view.findViewById(R.id.btnEdit);
        Button btnDelete = view.findViewById(R.id.btnDelete);

        // Set data
        tvPetName.setText(post.getPetName());
        tvPetType.setText("Tipo: " + post.getPetType());
        tvPetAge.setText("Edad: " + post.getPetAge() + " años");
        tvPetBreed.setText("Raza: " + post.getPetBreed());
        tvDate.setText("Fecha: " + post.getCreatedAt());
        tvAuthor.setText("Autor: " + post.getAuthorName());
        tvNotes.setText("Notas: " + post.getContent());

        // Load image
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            Picasso.get()
                    .load(post.getImageUrls().get(0))
                    .placeholder(R.drawable.photoframe)
                    .error(R.drawable.warning)
                    .into(ivPetPhoto);
        }

        // Set button actions
        btnDownload.setOnClickListener(v -> downloadImage(post));
        btnEdit.setOnClickListener(v -> editPost(post));
        btnDelete.setOnClickListener(v -> confirmDeletePost(post));

        builder.setPositiveButton("Cerrar", null);
        builder.create().show();
    }

    private void downloadImage(Post post) {
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            String imageUrl = post.getImageUrls().get(0);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
            request.setTitle("Descargando imagen de " + post.getPetName());
            request.setDescription("Imagen de mascota de Pawss");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_PICTURES,
                    "Pawss_" + System.currentTimeMillis() + ".jpg"
            );

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);

            showToast("Descarga iniciada");
        } else {
            showToast("No hay imagen para descargar");
        }
    }

    private void editPost(Post post) {
        // Implement edit functionality here
        showToast("Editar publicación: " + post.getId());
    }

    private void confirmDeletePost(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar publicación")
                .setMessage("¿Estás seguro de que quieres eliminar esta publicación?")
                .setPositiveButton("Eliminar", (dialog, which) -> deletePost(post.getId()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Helper methods
    private Map<String, String> createAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + authManager.getAccessToken());
        return headers;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    // VolleyMultipartRequest class
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