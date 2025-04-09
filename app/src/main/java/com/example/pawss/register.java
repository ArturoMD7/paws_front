package com.example.pawss;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    private EditText firstNameEditText, lastNameEditText, emailEditText,
            phoneEditText, addressEditText, passwordEditText;
    private Button registerButton;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;
    private String apiUrl;
    private String API_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializar vistas
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        requestQueue = Volley.newRequestQueue(this);
        String apiUrl  = getString(R.string.apiUrl);

        registerButton.setOnClickListener(v -> registerUser());

    }

    private void registerUser() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validaciones básicas
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor complete los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso y deshabilitar botón
        registerButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Crear objeto JSON para la solicitud
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("first_name", firstName);
            jsonBody.put("last_name", lastName);
            jsonBody.put("email", email);
            jsonBody.put("phone", phone);
            jsonBody.put("address", address);
            jsonBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
            registerButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            return;
        }
        apiUrl = getString(R.string.apiUrl);
        API_URL = apiUrl + "users/signup/";


        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                API_URL,
                jsonBody,
                response -> {
                    registerButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);

                    Log.d("REGISTER_RESPONSE", "Respuesta: " + response.toString());

                    // Verificar si el registro fue exitoso
                    if (response.has("id")) {
                        Toast.makeText(register.this, "Registro exitoso. Por favor inicie sesión", Toast.LENGTH_LONG).show();

                        // Go back to login screen
                        Intent intent = new Intent(register.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(register.this, "Registro exitoso pero falta ID en la respuesta", Toast.LENGTH_LONG).show();
                        Log.e("REGISTER_ERROR", "La respuesta no contiene ID de usuario");
                    }
                },
                error -> {
                    registerButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);

                    String errorMessage = "Error en el registro";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMessage = new String(error.networkResponse.data);
                    }
                    Toast.makeText(register.this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("REGISTER_ERROR", "Error: " + errorMessage);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }
}