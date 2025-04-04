package com.example.pawss;

import android.os.Bundle;

public class pets extends BaseActivity {

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_pets;
    }

    @Override
    protected int getCurrentNavItem() {
        return R.id.nav_pets;
    }

}