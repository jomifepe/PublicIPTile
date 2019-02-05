package com.jomifepe.publiciptile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, R.string.label_powered_by, Toast.LENGTH_LONG).show();
        finish();
    }
}
