package com.mrrobot.batterystatsreset

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.google.android.material.button.MaterialButton

class WebViewActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var languageToggleButton: MaterialButton
    private var isRussianSelected = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        webView = findViewById(R.id.webview)
        languageToggleButton = findViewById(R.id.languageToggleButton)

        webView.settings.javaScriptEnabled = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val errorMessage = getString(R.string.webview_load_error, error?.description ?: "Unknown error")
                Toast.makeText(this@WebViewActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
            isRussianSelected = savedInstanceState.getBoolean("isRussianSelected", false)
        } else {
            loadPageBasedOnLanguage()
        }

        updateButtonText()

        languageToggleButton.setOnClickListener {
            isRussianSelected = !isRussianSelected
            loadPageBasedOnLanguage()
            updateButtonText()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.destroy()
                finish()
            }
        })
    }

    private fun loadPageBasedOnLanguage() {
        val htmlFileName = if (isRussianSelected) {
            "instruction_page_ru.html"
        } else {
            "instruction_page_en.html"
        }
        webView.loadUrl("file:///android_asset/$htmlFileName")
    }

    private fun updateButtonText() {
        languageToggleButton.text = if (isRussianSelected) {
            getString(R.string.switch_to_english)
        } else {
            getString(R.string.switch_to_russian)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
        outState.putBoolean("isRussianSelected", isRussianSelected)
    }
}