package com.example.pawss;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class FamilySetupActivity extends AppCompatActivity {

    private RadioGroup familyOptionsGroup;
    private EditText familyNameEditText, familyCodeEditText;
    private Button confirmButton;
    private RequestQueue requestQueue;
    private int userId;
    private final String API_URL = "http://192.168.1.64:8000/api/families/setup/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_setup);

        // Obtener user_id del intent
        userId = getIntent().getIntExtra("user_id", -1);

        // Inicializar vistas
        familyOptionsGroup = findViewById(R.id.familyOptionsGroup);
        familyNameEditText = findViewById(R.id.familyNameEditText);
        familyCodeEditText = findViewById(R.id.familyCodeEditText);
        confirmButton = findViewById(R.id.confirmButton);

        requestQueue = Volley.newRequestQueue(this);

        // Mostrar/ocultar campos según opción seleccionada
        familyOptionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.createFamilyRadio) {
                familyNameEditText.setVisibility(android.view.View.VISIBLE);
                familyCodeEditText.setVisibility(android.view.View.GONE);
            } else {
                familyNameEditText.setVisibility(android.view.View.GONE);
                familyCodeEditText.setVisibility(android.view.View.VISIBLE);
            }
        });

        confirmButton.setOnClickListener(v -> setupFamily());
    }

    private void setupFamily() {
        int selectedId = familyOptionsGroup.getCheckedRadioButtonId();
        String action = (selectedId == R.id.createFamilyRadio) ? "create" : "join";
        String name = familyNameEditText.getText().toString().trim();
        String code = familyCodeEditText.getText().toString().trim();

        if ((action.equals("create") && name.isEmpty()) ||
                (action.equals("join") && code.isEmpty())) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("user_id", userId);
            jsonBody.put("action", action);
            if (action.equals("create")) {
                jsonBody.put("name", name);
            } else {
                jsonBody.put("code", code);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                API_URL,
                jsonBody,
                response -> {
                    // Familia configurada, ir a pantalla principal
                    Intent intent = new Intent(FamilySetupActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                },
                error -> Toast.makeText(FamilySetupActivity.this, "Error al configurar familia", Toast.LENGTH_SHORT).show()
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