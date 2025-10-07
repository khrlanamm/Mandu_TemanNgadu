package com.khrlanamm.mandu.ui.article

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.khrlanamm.mandu.databinding.ActivityArticleDetailBinding

class ArticleDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArticleDetailBinding

    companion object {
        const val EXTRA_URL = "extra_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- PERUBAHAN 1: Aktifkan Edge-to-Edge ---
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityArticleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- PERUBAHAN 2: Tambahkan Insets Listener ---
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Terapkan padding pada root view agar WebView tidak tertutup status/navigation bar
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }


        val url = intent.getStringExtra(EXTRA_URL)

        if (url != null) {
            setupWebView(url)
        }
    }

    private fun setupWebView(url: String) {
        binding.webView.apply {
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    binding.progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                }
            }
            settings.javaScriptEnabled = true
            loadUrl(url)
        }
    }
}
