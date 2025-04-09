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
import com.android.volley.Response;
import com.android.volley.VolleyError;
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
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_setup);

        authManager = new AuthManager(this);

        // Initialize views
        familyOptionsGroup = findViewById(R.id.familyOptionsGroup);
        familyNameEditText = findViewById(R.id.familyNameEditText);
        familyCodeEditText = findViewById(R.id.familyCodeEditText);
        confirmButton = findViewById(R.id.confirmButton);

        requestQueue = Volley.newRequestQueue(this);

        // Show/hide fields based on selected option
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
        boolean isCreateFamily = selectedId == R.id.createFamilyRadio;

        String name = familyNameEditText.getText().toString().trim();
        String code = familyCodeEditText.getText().toString().trim();

        // Validate inputs
        if (isCreateFamily && name.isEmpty()) {
            Toast.makeText(this, "Please enter a family name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isCreateFamily && code.isEmpty()) {
            Toast.makeText(this, "Please enter a family code", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String url = getString(R.string.apiUrl);
            JSONObject jsonBody = new JSONObject();

            if (isCreateFamily) {
                url += "families/";
                jsonBody.put("name", name);
            } else {
                url += "families/join/";
                jsonBody.put("codeFam", code);
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    isCreateFamily ? Request.Method.POST : Request.Method.PUT,
                    url,
                    jsonBody,
                    response -> handleSuccessResponse(response),
                    error -> handleErrorResponse(error)
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "Bearer " + authManager.getAccessToken());
                    return headers;
                }
            };

            requestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSuccessResponse(JSONObject response) {
        try {
            // Check if response contains family data
            if (response.has("id") || response.has("family")) {
                Toast.makeText(this, "Family setup successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Unexpected response from server", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error processing response", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleErrorResponse(VolleyError error) {
        String errorMessage = "Error configuring family";
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                JSONObject errorResponse = new JSONObject(new String(error.networkResponse.data));
                if (errorResponse.has("error")) {
                    errorMessage = errorResponse.getString("error");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

}