package com.example.pawss;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class fotos extends BaseActivity {

    // Views
    private RecyclerView rvPosts;

    // Data
    private RequestQueue requestQueue;
    private AuthManager authManager;
    private PostAdapter postAdapter;
    private List<Post> postList = new ArrayList<>();

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_fotos;
    }

    @Override
    protected int getCurrentNavItem() {
        return R.id.nav_fotos;
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
        setupRecyclerView();
        loadPosts();
    }

    private void initializeViews() {
        rvPosts = findViewById(R.id.rvPets); // Asegúrate que este ID existe en activity_fotos.xml
        if (rvPosts == null) {
            throw new RuntimeException("RecyclerView con ID rvPosts no encontrado en el layout");
        }
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(postList, this::onPostClick, this::onDeletePost);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);
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
                            JSONObject postJson = postsArray.getJSONObject(i);
                            JSONArray imagesArray = postJson.getJSONArray("images");
                            if (imagesArray.length() > 0) {
                                JSONObject firstImage = imagesArray.getJSONObject(0);
                                int authorId = firstImage.getInt("author");
                                if (authorId == authManager.getUserId()) {
                                    Post post = parsePostFromJson(postJson);
                                    postList.add(post);
                                }
                            }
                        }

                        runOnUiThread(() -> {
                            postAdapter.notifyDataSetChanged();
                            if (postList.isEmpty()) {
                                showToast("No tienes publicaciones aún");
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
        JSONArray imagesArray = postJson.getJSONArray("images");
        JSONObject firstImage = imagesArray.getJSONObject(0);
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
            JSONArray imagesArray2 = postJson.getJSONArray("images");
            for (int j = 0; j < imagesArray2.length(); j++) {
                JSONObject imageObj = imagesArray2.getJSONObject(j);
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
}