package com.example.pawss;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    EditText email, password;
    Button clinician, registrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        email = findViewById(R.id.emailEditText);
        password = findViewById(R.id.passwordEditText);
        clinician = findViewById(R.id.loginButton);
        registrar = findViewById(R.id.registerButton); // Asegúrate de tener este botón

        clinician.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, fotos.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        registrar.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, register.class));
        });
    }
}