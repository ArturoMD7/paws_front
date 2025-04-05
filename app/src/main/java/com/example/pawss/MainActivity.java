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

public class MainActivity extends AppCompatActivity {
    private EditText email, password;
    private Button loginButton, registerButton;
    private AuthManager authManager;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(this);
        requestQueue = Volley.newRequestQueue(this);

        // Verificar si ya está logueado
        if (authManager.isLoggedIn()) {
            navigateToHome();
            return;
        }

        email = findViewById(R.id.emailEditText);
        password = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(v -> performLogin());
        registerButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, register.class));
        });
    }

    private void performLogin() {
        String emailText = email.getText().toString().trim();
        String passwordText = password.getText().toString().trim();

        if (emailText.isEmpty() || passwordText.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "http://192.168.1.64:8000/api/users/login/";
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
                                loginResponse.getUser().getId()
                        );

                        Toast.makeText(MainActivity.this,
                                "Bienvenido " + loginResponse.getUser().getFirstName(),
                                Toast.LENGTH_SHORT).show();
                        navigateToHome();
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

    private void navigateToHome() {
        Intent intent = new Intent(this, fotos.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}