package com.khrlanamm.mandu.ui.article

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.khrlanamm.mandu.databinding.ActivityArticleDetailBinding

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    companion object {
        const val EXTRA_URL = "extra_url"
        private const val TAG = "ArticleDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val url = intent.getStringExtra(EXTRA_URL)

        if (isUrlValid(url)) {
            setupWebView(url!!)
        } else {
            Log.e(TAG, "Invalid or null URL received: $url")
            Toast.makeText(this, "Invalid article URL.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        binding.webView.apply {
            webViewClient = SecureWebViewClient()
            settings.apply {
                javaScriptEnabled = true
                allowFileAccess = false
                allowContentAccess = false
            }
            loadUrl(url)
        }
    }

    private inner class SecureWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding.progressBar.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.progressBar.visibility = View.GONE
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val urlToLoad = request?.url.toString()
            return if (urlToLoad.startsWith("https://")) {
                false
            } else {
                Log.w(TAG, "Blocked loading of non-HTTPS URL: $urlToLoad")
                Toast.makeText(
                    this@ArticleDetailActivity,
                    "Blocked insecure link",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
        }
    }

    private fun isUrlValid(url: String?): Boolean {
        if (url.isNullOrEmpty()) {
            return false
        }
        return url.toUri().scheme == "https"
    }
}
