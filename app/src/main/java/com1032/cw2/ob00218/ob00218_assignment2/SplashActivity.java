package com1032.cw2.ob00218.ob00218_assignment2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Launch main activity after activity has loaded
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
