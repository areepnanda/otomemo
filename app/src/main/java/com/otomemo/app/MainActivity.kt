package com.otomemo.app

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private var currentNavId = R.id.nav_service

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && !isProtectionAvailable()) {
            // Tidak ada sidik jari/kunci layar terdaftar -> buka langsung
            initMainUi()
            return
        }
        if (savedInstanceState == null) {
            showBiometricGate()
        } else {
            // Reorientasi/recreate: UI tetap tampil tanpa prompt ulang
            initMainUi()
        }
    }

    private fun isProtectionAvailable(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return BiometricManager.from(this)
            .canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricGate() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (isFinishing || isDestroyed) return
                    initMainUi()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(this@MainActivity, errString, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("OtoMemo Terkunci")
            .setSubtitle("Gunakan sidik jari atau kunci layar perangkat")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    private fun initMainUi() {
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        SheetSync.flushPending(this)
        SheetSync.pull(this) { ok -> if (ok) select(currentNavId) }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_update -> Updater.check(this)
                R.id.nav_sync -> onSyncMenu()
                else -> select(item.itemId)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        if (supportFragmentManager.findFragmentById(R.id.container) == null) {
            select(R.id.nav_service)
        } else {
            val current = navView.checkedItem?.itemId ?: R.id.nav_service
            currentNavId = current
            toolbar.title = titleFor(current)
        }
    }

    private fun select(itemId: Int) {
        currentNavId = itemId
        val fragment: Fragment = when (itemId) {
            R.id.nav_vehicles -> VehiclesFragment()
            R.id.nav_items -> ItemsFragment()
            R.id.nav_workshops -> WorkshopsFragment()
            else -> RecordsFragment()
        }
        toolbar.title = titleFor(itemId)
        navView.setCheckedItem(itemId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    private fun titleFor(itemId: Int): String = when (itemId) {
        R.id.nav_vehicles -> "Kendaraan"
        R.id.nav_items -> "Item"
        R.id.nav_workshops -> "Bengkel"
        else -> "Service"
    }

    private fun onSyncMenu() {
        if (SheetSync.isConfigured(this)) {
            showSyncDialog()
        } else {
            showConfigDialog()
        }
    }

    private fun showSyncDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sinkron Google Sheet")
            .setMessage(SheetSync.lastSyncLabel(this))
            .setPositiveButton("Tarik data") { _, _ -> pullFromSheet() }
            .setNeutralButton("Kirim semua data") { _, _ -> pushAll() }
            .setNegativeButton("Ubah konfigurasi") { _, _ -> showConfigDialog() }
            .show()
    }

    private fun showConfigDialog() {
        val configured = SheetSync.isConfigured(this)
        val view = layoutInflater.inflate(R.layout.dialog_sync, null)
        val inputUrl = view.findViewById<EditText>(R.id.inputUrl)
        val inputToken = view.findViewById<EditText>(R.id.inputToken)
        inputUrl.setText(SheetSync.url(this))
        inputToken.setText(SheetSync.token(this))
        val builder = AlertDialog.Builder(this)
            .setTitle(if (configured) "Ubah Konfigurasi Google Sheet" else "Konfigurasi Google Sheet")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                SheetSync.saveConfig(this, inputUrl.text.toString(), inputToken.text.toString())
                Toast.makeText(this, "Konfigurasi disimpan", Toast.LENGTH_SHORT).show()
                pullFromSheet()
            }
        if (configured) builder.setNegativeButton("Batal", null)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(configured)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (inputUrl.text.toString().trim().isEmpty() || inputToken.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "URL & token wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SheetSync.saveConfig(this, inputUrl.text.toString(), inputToken.text.toString())
            Toast.makeText(this, "Konfigurasi disimpan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            pullFromSheet()
        }
    }

    private fun pullFromSheet() {
        if (!SheetSync.isConfigured(this)) {
            Toast.makeText(this, "Isi URL & token dulu", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Menarik data dari sheet...", Toast.LENGTH_SHORT).show()
        SheetSync.pull(this) { ok ->
            if (ok) {
                select(currentNavId)
                Toast.makeText(this, "Data dari sheet berhasil ditarik", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Gagal menarik data (cek URL/token, deploy ulang script, atau sheet kosong)",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun pushAll() {
        if (!SheetSync.isConfigured(this)) {
            Toast.makeText(this, "Isi URL & token dulu", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Mengirim semua data ke sheet...", Toast.LENGTH_SHORT).show()
        SheetSync.replaceAll(this) { ok ->
            Toast.makeText(
                this,
                if (ok) "Semua data terkirim ke sheet"
                else "Gagal mengirim, data masuk antrean (dicoba ulang otomatis)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
