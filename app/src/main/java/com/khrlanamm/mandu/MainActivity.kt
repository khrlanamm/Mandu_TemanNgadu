package com.khrlanamm.mandu

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.khrlanamm.mandu.databinding.ActivityMainBinding
import com.khrlanamm.mandu.model.ArticleAdapter
import com.khrlanamm.mandu.model.HomeViewModel
import com.khrlanamm.mandu.ui.history.HistoryActivity
import com.khrlanamm.mandu.ui.profile.ProfileActivity
import com.khrlanamm.mandu.ui.report.ReportActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var articleAdapter: ArticleAdapter
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupViewModel()
        setupRecyclerView()
        observeArticles()

        setupClickListeners()

        setupOnBackPressedCallback()
    }

    private fun setupViewModel() {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticleAdapter(this, emptyList())
        binding.ArticlesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = articleAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeArticles() {
        homeViewModel.articles.observe(this) { articles ->
            articleAdapter.updateData(articles)
        }
    }

    private fun setupClickListeners() {
        binding.fab.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }

        binding.cardReportBullying.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupOnBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finishAffinity()
                } else {
                    Toast.makeText(this@MainActivity, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_account -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
