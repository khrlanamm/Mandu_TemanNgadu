package com.khrlanamm.mandu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.khrlanamm.mandu.databinding.ActivityMainBinding
import com.khrlanamm.mandu.model.ArticleAdapter
import com.khrlanamm.mandu.model.HomeViewModel
import com.khrlanamm.mandu.service.MyFirebaseMessagingService
import com.khrlanamm.mandu.ui.history.HistoryActivity
import com.khrlanamm.mandu.ui.profile.ProfileActivity
import com.khrlanamm.mandu.ui.report.ReportActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var articleAdapter: ArticleAdapter
    private var backPressedTime: Long = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin notifikasi ditolak.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.appbar.setPadding(insets.left, insets.top, insets.right, 0)
            binding.swipeRefreshLayout.setPadding(0, 0, 0, insets.bottom)

            val fabMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
            binding.fab.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = insets.bottom + fabMargin
            }

            WindowInsetsCompat.CONSUMED
        }


        askNotificationPermission()
        checkUserAndSaveToken()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupViewModel()
        setupRecyclerView()
        observeViewModel()
        setupSwipeToRefresh()

        setupClickListeners()
        setupSearch()
        setupOnBackPressedCallback()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkUserAndSaveToken() {
        if (Firebase.auth.currentUser != null) {
            MyFirebaseMessagingService.saveTokenIfAdmin()
        }
    }

    private fun setupViewModel() {
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticleAdapter(this, emptyList())
        binding.ArticlesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = articleAdapter
        }
    }

    private fun observeViewModel() {
        homeViewModel.articles.observe(this) { articles ->
            articleAdapter.updateData(articles)
        }

        homeViewModel.noResultsFound.observe(this) { noResults ->
            if (noResults) {
                Toast.makeText(this, "Artikel yang anda cari belum tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        homeViewModel.isLoading.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            if (isLoading && articleAdapter.itemCount == 0) {
                binding.progressBar.visibility = View.VISIBLE
                binding.ArticlesRecyclerView.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.ArticlesRecyclerView.visibility = View.VISIBLE
            }
        }

        homeViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            homeViewModel.loadArticlesFromFirestore()
        }
    }

    private fun setupClickListeners() {
        val extras = intent.extras ?: Bundle()

        binding.fab.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java).apply {
                putExtras(extras)
            }
            startActivity(intent)
        }

        binding.cardReportBullying.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java).apply {
                putExtras(extras)
            }
            startActivity(intent)
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
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
        val extras = intent.extras ?: Bundle()

        return when (item.itemId) {
            R.id.action_account -> {
                val intent = Intent(this, ProfileActivity::class.java).apply {
                    putExtras(extras)
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
