package com.example.pawss;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;
    private final Map<Integer, Class<?>> activityMap = createActivityMap();
    private static Integer lastSelectedItem = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
        initNavigation();
    }

    private void initNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            // Configuración inicial silenciosa
            bottomNav.post(() -> {
                int currentItem = getCurrentNavItem();
                if (lastSelectedItem == null || lastSelectedItem != currentItem) {
                    bottomNav.setSelectedItemId(currentItem);
                    lastSelectedItem = currentItem;
                }
            });

            setupBottomNavigation();
        }
    }

    protected void setupBottomNavigation() {
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            Class<?> targetActivity = activityMap.get(item.getItemId());

            if (targetActivity != null && !this.getClass().equals(targetActivity)) {
                lastSelectedItem = item.getItemId();
                navigateToActivity(targetActivity);
                return true;
            }
            return false;
        });
    }

    private void navigateToActivity(Class<?> activity) {
        Intent intent = new Intent(this, activity);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            updateNavigationSelection();
        }
    }


    private void updateNavigationSelection() {
        if (bottomNav != null) {
            try {
                bottomNav.setSelectedItemId(getCurrentNavItem());
            } catch (Exception e) {
                e.printStackTrace(); // evita crash silencioso
            }
        }
    }


    protected abstract int getLayoutResourceId();
    protected abstract int getCurrentNavItem();

    private Map<Integer, Class<?>> createActivityMap() {
        Map<Integer, Class<?>> map = new HashMap<>();
        map.put(R.id.nav_fotos, fotos.class);
        map.put(R.id.nav_fotosfamilia, fotosfamilia.class);
        map.put(R.id.nav_pets, pets.class);
        map.put(R.id.nav_recordatorios, recordatorios.class);
        return map;
    }
}