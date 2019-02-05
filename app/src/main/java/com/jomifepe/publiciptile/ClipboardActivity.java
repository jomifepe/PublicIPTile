package com.jomifepe.publiciptile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class ClipboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (QSTileService.currentIp != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("ip", QSTileService.currentIp);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.label_clipboard_copied, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.label_clipboard_no_ip, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
