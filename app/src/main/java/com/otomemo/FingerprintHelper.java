package com.otomemo;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class FingerprintHelper {

    private static final String TAG = "FingerprintHelper";
    private static final int REQUEST_CODE_FINGERPRINT = 1001;
    private static final int REQUEST_CODE_PERMISSION = 1002;

    private Context context;
    private FingerprintManager fingerprintManager;
    private Executor executor;
    private CancellationSignal cancellationSignal;

    public FingerprintHelper(Context context) {
        this.context = context;
        this.fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        this.executor = new Handler(Looper.getMainLooper()).getLooper();
        this.cancellationSignal = new CancellationSignal();
    }

    public boolean isFingerprintAvailable() {
        if (fingerprintManager == null) {
            return false;
        }
        return fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints();
    }

    public boolean requestFingerprintAuthentication() {
        if (!isFingerprintAvailable()) {
            Toast.makeText(context, "Fingerprint not available or enrolled", Toast.LENGTH_LONG).show();
            return false;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Permission not granted, request it
                // This would typically be handled in the UI
                return false;
            }
        }

        // Start fingerprint authentication
        if (fingerprintManager != null) {
            // In a real app, you would create a crypto object and authenticate
            // This is a simplified version
            fingerprintManager.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Log.e(TAG, "Authentication error: " + errString);
                    Toast.makeText(context, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationSuccess(FingerprintManager.AuthenticationResult result) {
                    super.onAuthenticationSuccess(result);
                    Log.d(TAG, "Fingerprint authentication successful!");
                    // Mark authentication as successful in shared preferences
                    SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("is_authenticating_success", true).apply();
                    // Navigate to main activity
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Log.w(TAG, "Authentication failed!");
                    Toast.makeText(context, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationCanceled() {
                    super.onAuthenticationCanceled();
                    Log.w(TAG, "Authentication canceled by user!");
                    Toast.makeText(context, "Authentication canceled by user.", Toast.LENGTH_SHORT).show();
                }
            }, null);
            return true;
        }
        return false;
    }
}