package com.otomemo;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

public class AuthActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";
    private static final int REQUEST_CODE_FINGERPRINT = 1001;
    private static final int REQUEST_CODE_PERMISSION = 1002;

    private FingerprintManager fingerprintManager;
    private Executor executor;
    private CancellationSignal cancellationSignal;

    private MaterialButton btnLogin;
    private TextInputEditText etPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Initialize views
        btnLogin = findViewById(R.id.btn_login);
        etPin = findViewById(R.id.et_pin);

        // Initialize fingerprint manager
        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        executor = Executors.newSingleThreadExecutor();
        cancellationSignal = new CancellationSignal();

        // Set up login button
        btnLogin.setOnClickListener(v -> {
            String pin = etPin.getText().toString().trim();
            if (pin.isEmpty()) {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if device supports fingerprint
            if (!fingerprintManager.isHardwareDetected()) {
                Toast.makeText(this, "Fingerprint hardware not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if fingerprint is already enrolled
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(this, "No fingerprints enrolled. Please enroll first.", Toast.LENGTH_LONG).show();
                return;
            }

            // Start fingerprint authentication
            authenticateWithFingerprint(pin);
        });
    }

    private void authenticateWithFingerprint(String pin) {
        // Check if user has granted permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT}, REQUEST_CODE_PERMISSION);
            return;
        }

        // Create fingerprint auth request
        FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(
                new FingerprintManager.CryptoObject.CryptoObject.Builder(
                        new Cipher(true) // Use AES cipher
                ).build()
        );

        // Start authentication
        fingerprintManager.authenticate(cryptoObject, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.e(TAG, "Authentication error: " + errString);
                Toast.makeText(AuthActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSuccess(FingerprintManager.AuthenticationResult result) {
                super.onAuthenticationSuccess(result);
                Log.d(TAG, "Fingerprint authentication successful!");
                // Navigate to main activity after successful authentication
                Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.w(TAG, "Authentication failed!");
                Toast.makeText(AuthActivity.this, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationCanceled() {
                super.onAuthenticationCanceled();
                Log.w(TAG, "Authentication canceled by user!");
                Toast.makeText(AuthActivity.this, "Authentication canceled by user.", Toast.LENGTH_SHORT).show();
            }
        }, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with authentication
                authenticateWithFingerprint("default");
            } else {
                Toast.makeText(this, "Fingerprint permission denied. Please enable it in settings.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
        }
    }
}