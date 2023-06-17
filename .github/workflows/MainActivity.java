package com.rvm.gtr;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;
    private static final String[] PERMISSIONS = {Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE};
    private static final String ASSISTANT_APP_PACKAGE_NAME = "com.google.android.apps.googleassistant";

    private Handler handler;
    private Runnable dialRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!arePermissionsGranted()) {
            requestPermissions();
        } else {
            initialize();
        }
    }

    private boolean arePermissionsGranted() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                initialize();
            } else {
                // Handle permissions not granted
            }
        }
    }

    private void initialize() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new CustomPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

        handler = new Handler();
        dialRunnable = this::acceptInstitutionalCall;
    }

    private class CustomPhoneStateListener extends PhoneStateListener {

        private boolean isInstitutionalCall = false;
        private boolean isCallPatched = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    answerCall();
                    isInstitutionalCall = true;
                    handler.postDelayed(dialRunnable, 60000);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (isInstitutionalCall && !isCallPatched) {
                        patchCallToAssistantApp();
                        isCallPatched = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    isInstitutionalCall = false;
                    isCallPatched = false;
                    handler.removeCallbacks(dialRunnable);
                    break;
            }
        }

        private void answerCall() {
            Intent answerIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            answerIntent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
            sendOrderedBroadcast(answerIntent, null);
        }

        private void acceptInstitutionalCall() {
            Intent dialIntent = new Intent(Intent.ACTION_CALL);
            dialIntent.setData(Uri.parse("tel:1"));
            startActivity(dialIntent);
        }

        private void patchCallToAssistantApp() {
            Intent patchIntent = new Intent(Intent.ACTION_MAIN);
            patchIntent.setClassName(ASSISTANT_APP_PACKAGE_NAME, ASSISTANT_APP_PACKAGE_NAME + ".AssistantRequestHandlerActivity");
            patchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(patchIntent);
        }
    }
}
