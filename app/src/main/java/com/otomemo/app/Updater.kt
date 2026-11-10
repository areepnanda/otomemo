package com.otomemo.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object Updater {

    private const val REPO = "areepnanda/otomemo"

    fun check(activity: Activity) {
        Toast.makeText(activity, "Memeriksa update...", Toast.LENGTH_SHORT).show()
        thread {
            try {
                val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
                    .openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == 404) {
                    ui(activity) {
                        Toast.makeText(activity, "Belum ada rilis tersedia", Toast.LENGTH_SHORT).show()
                    }
                    return@thread
                }

                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val latest = json.getString("tag_name").removePrefix("v")
                val current = BuildConfig.VERSION_NAME

                if (latest == current) {
                    ui(activity) {
                        Toast.makeText(activity, "Sudah versi terbaru (v$latest)", Toast.LENGTH_SHORT).show()
                    }
                    return@thread
                }

                val asset = json.getJSONArray("assets").getJSONObject(0)
                val downloadUrl = asset.getString("browser_download_url")

                ui(activity) {
                    try {
                        AlertDialog.Builder(activity)
                            .setTitle("Update tersedia")
                            .setMessage("Versi v$latest tersedia (versi kamu v$current).\nDownload dan install sekarang?")
                            .setPositiveButton("Update") { _, _ -> downloadAndInstall(activity, downloadUrl) }
                            .setNegativeButton("Nanti", null)
                            .show()
                    } catch (_: Exception) {
                    }
                }
            } catch (e: Exception) {
                ui(activity) {
                    Toast.makeText(activity, "Gagal cek update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadAndInstall(activity: Activity, url: String) {
        val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
        }
        val progress = try {
            AlertDialog.Builder(activity)
                .setTitle("Mengunduh update")
                .setView(bar)
                .setCancelable(false)
                .create()
        } catch (_: Exception) {
            null
        }
        progress?.show()

        thread {
            try {
                val dir = activity.getExternalFilesDir(null) ?: activity.filesDir
                val file = File(dir, "otomemo-update.apk")
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 120000
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val read = input.read(buf)
                            if (read == -1) break
                            output.write(buf, 0, read)
                            done += read
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                ui(activity) { bar.progress = pct }
                            }
                        }
                    }
                }
                ui(activity) {
                    progress?.dismiss()
                    install(activity, file)
                }
            } catch (e: Exception) {
                ui(activity) {
                    progress?.dismiss()
                    Toast.makeText(activity, "Gagal download: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun install(activity: Activity, file: File) {
        try {
            if (activity.packageManager.canRequestPackageInstalls()) {
                launchInstaller(activity, file)
            } else {
                Toast.makeText(
                    activity,
                    "Izinkan \"Install unknown apps\" untuk OtoMemo, lalu cek update lagi",
                    Toast.LENGTH_LONG
                ).show()
                activity.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activity.packageName}")
                    )
                )
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Gagal buka installer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchInstaller(activity: Activity, file: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    private fun ui(activity: Activity, block: () -> Unit) {
        activity.runOnUiThread(block)
    }
}
