package com.khrlanamm.mandu

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
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

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setup ViewModel, RecyclerView, dan Observers
        setupViewModel()
        setupRecyclerView()
        observeViewModel() // Mengganti nama fungsi observer

        // Setup listener untuk tombol, card, dan search
        setupClickListeners()
        setupSearch() // Menambahkan setup untuk search

        // Setup logika tombol kembali
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
            // isNestedScrollingEnabled sudah diatur di XML, jadi tidak perlu di sini
        }
    }

    /**
     * Mengamati perubahan data dari ViewModel.
     * 1. Mengamati daftar artikel untuk diperbarui di RecyclerView.
     * 2. Mengamati status 'tidak ditemukan' untuk menampilkan Toast.
     */
    private fun observeViewModel() {
        homeViewModel.articles.observe(this) { articles ->
            articleAdapter.updateData(articles)
        }

        homeViewModel.noResultsFound.observe(this) { noResults ->
            if (noResults) {
                Toast.makeText(this, "Artikel yang anda cari belum tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        // Mendapatkan extras dari intent yang memulai MainActivity
        val extras = intent.extras ?: Bundle()

        binding.fab.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java).apply {
                putExtras(extras) // Meneruskan data pengguna
            }
            startActivity(intent)
        }

        binding.cardReportBullying.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java).apply {
                putExtras(extras) // Meneruskan data pengguna
            }
            startActivity(intent)
        }
    }

    /**
     * Menyiapkan listener untuk SearchView untuk memfilter artikel secara real-time.
     */
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Tidak perlu aksi khusus saat submit, karena pencarian sudah live
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Panggil fungsi search di ViewModel setiap kali teks berubah
                homeViewModel.searchArticles(newText.orEmpty())
                return true
            }
        })
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
        // Mendapatkan extras dari intent yang memulai MainActivity
        val extras = intent.extras ?: Bundle()

        return when (item.itemId) {
            R.id.action_account -> {
                val intent = Intent(this, ProfileActivity::class.java).apply {
                    putExtras(extras) // Meneruskan data pengguna
                }
                startActivity(intent)
                true
            }
            R.id.action_history -> {
                val intent = Intent(this, HistoryActivity::class.java).apply {
                    putExtras(extras)
                }
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
