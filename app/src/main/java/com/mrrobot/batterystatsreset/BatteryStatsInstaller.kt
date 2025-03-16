package com.mrrobot.batterystatsreset

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class BatteryStatsInstaller(private val context: Context) {

    fun installMultipleApksShizuku(fileNames: List<String>) {
        val assetManager = context.assets

        fileNames.forEach { fileName ->
            val outputFile = File(context.getExternalFilesDir(null), fileName)
            try {
                assetManager.open(fileName).use { input ->
                    FileOutputStream(outputFile).use { output ->
                        copyFile(input, output)
                    }
                }
                installApkViaShizuku(outputFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.error_apk_not_found), Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun installApkViaShizuku(apkPath: String) {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(context, context.getString(R.string.shizuku_not_available), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val fileName = File(apkPath).name
            val destinationPath = "/data/local/tmp/$fileName"
            val moveCommand = "mv $apkPath $destinationPath"
            val moveResult = executeAdbCommand(moveCommand)

            if (moveResult.first == 0) {
                val installCommand = if (fileName.contains("FactoryTestLauncher")) {
                    "pm install -i PrePackageInstaller $destinationPath"
                } else {
                    "pm install $destinationPath"
                }
                val installResult = executeAdbCommand(installCommand)

                if (installResult.first == 0) {
                    val appName = if (fileName.contains("FactoryTestLauncher")) "FactoryTestLauncher" else "RepairCal"
                    val successMessage = context.getString(R.string.apk_install_success, appName)
                    Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                } else {
                    val appName = if (fileName.contains("FactoryTestLauncher")) "FactoryTestLauncher" else "RepairCal"
                    val errorMessage = context.getString(R.string.apk_install_error, appName) + ": ${installResult.second}"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            } else {
                val moveErrorMessage = context.getString(R.string.apk_move_error, moveResult.second)
                Toast.makeText(context, moveErrorMessage, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.adb_command_error, e.message), Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun executeAdbCommand(command: String): Pair<Int, String> {
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val exitCode = process.waitFor()
            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
            val output = process.inputStream.bufferedReader().use { it.readText() }
            Pair(exitCode, errorOutput.ifEmpty { output })
        } catch (e: Exception) {
            Pair(-1, e.message ?: "Unknown error")
        }
    }

    private fun copyFile(inputStream: InputStream, outputStream: OutputStream) {
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
    }
}