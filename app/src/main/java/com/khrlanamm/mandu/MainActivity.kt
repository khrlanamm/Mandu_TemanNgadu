package com.khrlanamm.mandu

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.khrlanamm.mandu.databinding.ActivityMainBinding
import com.khrlanamm.mandu.ui.history.HistoryActivity
import com.khrlanamm.mandu.ui.profile.ProfileActivity
import com.khrlanamm.mandu.ui.report.ReportActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.fab.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_account -> {
                val intent = Intent(this, ProfileActivity::class.java).apply {
                    putExtras(intent.extras ?: Bundle())
                }
                startActivity(intent)
                true
            }
            R.id.action_history -> {
                val intent = Intent(this, HistoryActivity::class.java).apply {
                    putExtras(intent.extras ?: Bundle())
                }
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
