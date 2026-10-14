package com.otomemo.app

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_update -> Updater.check(this)
                R.id.nav_sync -> showSyncDialog()
                else -> select(item.itemId)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        if (savedInstanceState == null) {
            select(R.id.nav_service)
        } else {
            val current = navView.checkedItem?.itemId ?: R.id.nav_service
            toolbar.title = titleFor(current)
        }
    }

    private fun select(itemId: Int) {
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

    private fun showSyncDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_sync, null)
        val inputUrl = view.findViewById<EditText>(R.id.inputUrl)
        val inputToken = view.findViewById<EditText>(R.id.inputToken)
        inputUrl.setText(SheetSync.url(this))
        inputToken.setText(SheetSync.token(this))
        AlertDialog.Builder(this)
            .setTitle("Sinkron Google Sheet")
            .setMessage(SheetSync.lastSyncLabel(this))
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                SheetSync.saveConfig(this, inputUrl.text.toString(), inputToken.text.toString())
                Toast.makeText(this, "Konfigurasi disimpan", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Kirim semua data") { _, _ ->
                SheetSync.saveConfig(this, inputUrl.text.toString(), inputToken.text.toString())
                pushAll()
            }
            .setNegativeButton("Tutup", null)
            .show()
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
