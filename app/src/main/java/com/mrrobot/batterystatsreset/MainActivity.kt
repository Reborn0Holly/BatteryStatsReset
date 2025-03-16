package com.mrrobot.batterystatsreset

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import rikka.shizuku.Shizuku
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageButton
import androidx.browser.customtabs.CustomTabsIntent

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private lateinit var batteryStatsInstaller: BatteryStatsInstaller
    private lateinit var togglePermission: SwitchMaterial
    private var isPermissionCheckedManually = false

    @SuppressLint("StringFormatInvalid")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        togglePermission = findViewById(R.id.togglePermission)

        Shizuku.addBinderReceivedListener {
            try {
                updateToggleState()
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.shizuku_error, e.message), Toast.LENGTH_SHORT).show()
            }
        }

        if (Shizuku.pingBinder()) {
            updateToggleState()
        }

        togglePermission.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isPermissionCheckedManually) {
                    checkPermission(0)
                }
            } else {
                if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.shizuku_or_permission_missing), Toast.LENGTH_SHORT).show()
                    togglePermission.isChecked = false
                } else {
                    togglePermission.isChecked = true
                }
            }
        }

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)

        val openHtmlButton: MaterialButton = findViewById(R.id.readButton)
        openHtmlButton.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        batteryStatsInstaller = BatteryStatsInstaller(this)

        val installButton: MaterialButton = findViewById(R.id.installButton)
        installButton.setOnClickListener {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                val apkFiles = listOf("com.samsung.android.FactoryTestLauncher.apk", "com.samsung.android.app.repaircal.apk")
                batteryStatsInstaller.installMultipleApksShizuku(apkFiles)
            } else {
                Toast.makeText(this, getString(R.string.shizuku_not_available), Toast.LENGTH_SHORT).show()
            }
        }

        val launchButton: MaterialButton = findViewById(R.id.launchButton)
        launchButton.setOnClickListener {
            launchSamsungFactoryTest()
        }

        val uninstallButton: MaterialButton = findViewById(R.id.uninstallButton)
        uninstallButton.setOnClickListener {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                val packageNames = listOf("com.samsung.android.FactoryTestLauncher", "com.samsung.android.app.repaircal")
                uninstallMultiplePackagesShizuku(packageNames, this)
            } else {
                Toast.makeText(this, getString(R.string.shizuku_not_available), Toast.LENGTH_SHORT).show()
            }
        }

        val exitButton: MaterialButton = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            finish()
        }

        val openWebsiteButton: ImageButton = findViewById(R.id.openWebsiteButton)
        openWebsiteButton.setOnClickListener {
            val websiteUrl = "https://4pda.to/forum/index.php?showtopic=943849&view=findpost&p=132524914"
            val uri = Uri.parse(websiteUrl)
            try {
                val customTabsIntent = CustomTabsIntent.Builder().build()
                customTabsIntent.launchUrl(this, uri)
            } catch (e: Exception) {
                val errorMessage = getString(R.string.error_cannot_open_website)
                val toastMessage = getString(R.string.toast_error, errorMessage)
                Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchSamsungFactoryTest() {
        try {
            if (Shizuku.pingBinder()) {
                val checkAppCommand = "pm list packages | grep 'com.samsung.android.FactoryTestLauncher'"
                val checkProcess = Shizuku.newProcess(arrayOf("sh", "-c", checkAppCommand), null, null)
                checkProcess.waitFor()
                val checkOutput = checkProcess.inputStream.bufferedReader().use { it.readText() }
                checkProcess.destroy()

                if (checkOutput.isNotEmpty()) {
                    val launchCommand = "am start -n com.samsung.android.FactoryTestLauncher/.ui.Main"
                    val launchProcess = Shizuku.newProcess(arrayOf("sh", "-c", launchCommand), null, null)
                    val launchResult = launchProcess.waitFor()
                    val errorStream = launchProcess.errorStream.bufferedReader().use { it.readText() }
                    launchProcess.destroy()

                    if (launchResult == 0) {
                        Toast.makeText(this, getString(R.string.command_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.command_error, errorStream), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.app_not_found_error), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.shizuku_not_available), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.command_error, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallMultiplePackagesShizuku(packageNames: List<String>, context: Context) {
        for (packageName in packageNames) {
            val command = "pm uninstall $packageName"
            try {
                val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    val successMessage = context.getString(R.string.uninstall_success, packageName)
                    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                } else {
                    val failureMessage = context.getString(R.string.uninstall_failure, packageName)
                    Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
                }
                process.destroy()
            } catch (e: Exception) {
                val errorMessage = context.getString(R.string.uninstall_error, e.message)
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }

    private fun updateToggleState() {
        val isShizukuRunning = Shizuku.pingBinder()
        val hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        togglePermission.isChecked = isShizukuRunning && hasPermission
    }

    private fun checkPermission(requestCode: Int): Boolean {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, getString(R.string.shizuku_not_running), Toast.LENGTH_SHORT).show()
            togglePermission.isChecked = false
            return false
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            return false
        } else {
            isPermissionCheckedManually = true
            Shizuku.requestPermission(requestCode)
            return false
        }
    }

    private val REQUEST_PERMISSION_RESULT_LISTENER =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            isPermissionCheckedManually = false
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.permissions_granted), Toast.LENGTH_SHORT).show()
                togglePermission.isChecked = true
            } else {
                Toast.makeText(this, getString(R.string.permissions_denied), Toast.LENGTH_SHORT).show()
                togglePermission.isChecked = false
            }
        }
}