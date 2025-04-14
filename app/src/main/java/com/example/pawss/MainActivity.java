package com.example.pawss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private EditText email, password;
    private Button loginButton, registerButton;
    private AuthManager authManager;
    private RequestQueue requestQueue;
    private static final int REQUEST_PERMISSIONS_CODE = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        requestQueue = Volley.newRequestQueue(this);

        // Verificar si ya está logueado
        if (authManager.isLoggedIn()) {
            checkFamilyStatus();
            return;
        }

        email = findViewById(R.id.emailEditText);
        password = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        requestNecessaryPermissions();

        loginButton.setOnClickListener(v -> performLogin());
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, register.class));
        });
    }

    private void requestNecessaryPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES
        };

        // Lista de permisos que aún no han sido concedidos
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_CODE);
        }
    }


    private void performLogin() {
        String emailText = email.getText().toString().trim();
        String passwordText = password.getText().toString().trim();

        if (emailText.isEmpty() || passwordText.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String apiUrl = getString(R.string.apiUrl);
        String url = apiUrl + "users/login/";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", emailText);
            jsonBody.put("password", passwordText);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, jsonBody,
                response -> {
                    try {
                        Gson gson = new Gson();
                        LoginResponse loginResponse = gson.fromJson(response.toString(), LoginResponse.class);

                        // Guardar tokens y datos del usuario
                        authManager.saveAuthTokens(
                                loginResponse.getAccess(),
                                loginResponse.getRefresh(),
                                loginResponse.getUser().getId(),
                                loginResponse.getUser().getFamilyId(),
                                loginResponse.getUser().getFirstName() + " " + loginResponse.getUser().getLastName()
                        );

                        // Check if user has a family
                        checkFamilyStatus();
                    } catch (Exception e) {
                        Log.e("LoginError", "Error parsing response", e);
                        Toast.makeText(MainActivity.this, "Error procesando la respuesta", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    String errorMessage = "Error de conexión";
                    if (error.networkResponse != null) {
                        switch (error.networkResponse.statusCode) {
                            case 400:
                                errorMessage = "Credenciales inválidas";
                                break;
                            case 401:
                                errorMessage = "No autorizado";
                                break;
                        }
                    }
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e("LoginError", error.toString());
                }
        );

        requestQueue.add(request);
    }

    private void checkFamilyStatus() {
        String apiUrl = getString(R.string.apiUrl);
        String url = apiUrl + "users/check-family/";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean hasFamily = response.getBoolean("has_family");
                        if (hasFamily) {
                            navigateToHome();
                        } else {
                            navigateToFamilySetup();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error verificando estado de familia", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(MainActivity.this, "Error verificando familia", Toast.LENGTH_SHORT).show();
                    Log.e("FamilyCheckError", error.toString());
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, fotos.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToFamilySetup() {
        Intent intent = new Intent(this, FamilySetupActivity.class);
        startActivity(intent);
        finish();
    }
}