package com.rvm.gtr;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.telecom.Call;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class CustomPhoneStateListener extends PhoneStateListener {

    private static final int AUTO_ACCEPT_DELAY = 60000; // 1 minute delay
    private static final String ASSISTANT_APP_PACKAGE_NAME = "com.google.android.googlequicksearchbox";
    private Context context;
    private boolean isCallAnswered = false;

    public CustomPhoneStateListener(Context context) {
        this.context = context;
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                // Answer the call if it is ringing
                if (!isCallAnswered) {
                    answerCall();
                    isCallAnswered = true;
                }
                // Wait for 1 minute and automatically dial "1"
                new Handler().postDelayed(this::autoDialOne, AUTO_ACCEPT_DELAY);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                // Connection established, patch the call to Google Assistant
                patchCallToAssistant();
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                // Call ended, reset the call answered flag
                isCallAnswered = false;
                break;
        }
    }

    private void answerCall() {
        try {
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null && telecomManager.isRinging()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    PhoneAccountHandle phoneAccountHandle = telecomManager.getDefaultOutgoingPhoneAccount(Uri.fromParts("tel", "1", null));
                    if (phoneAccountHandle != null) {
                        Call currentCall = telecomManager.acceptRingingCall(phoneAccountHandle, 0);
                        if (currentCall != null) {
                            currentCall.registerCallback(new Call.Callback() {
                                @Override
                                public void onStateChanged(Call call, int state) {
                                    if (state == Call.STATE_DISCONNECTED) {
                                        isCallAnswered = false;
                                        currentCall.unregisterCallback(this);
                                    }
                                }
                            });
                        }
                    }
                } else {
                    telecomManager.acceptRingingCall();
                }
            }
        } catch (Exception e) {
            // Handle exception
            e.printStackTrace();
        }
    }

    private void autoDialOne() {
        try {
            Intent dialIntent = new Intent(Intent.ACTION_CALL);
            dialIntent.setData(Uri.parse("tel:1"));
            context.startActivity(dialIntent);
        } catch (Exception e) {
            // Handle exception
            e.printStackTrace();
        }
    }

    private void patchCallToAssistant() {
        try {
            Intent intent = new Intent(Intent.ACTION_ASSIST);
            intent.setPackage(ASSISTANT_APP_PACKAGE_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // Handle exception
            e.printStackTrace();
        }
    }
}
