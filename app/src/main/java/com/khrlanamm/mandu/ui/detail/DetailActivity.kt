package com.khrlanamm.mandu.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.data.AdminUID
import com.khrlanamm.mandu.databinding.ActivityDetailBinding
import com.khrlanamm.mandu.ui.history.data.Report
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var report: Report
    private val auth = FirebaseAuth.getInstance()

    private val viewModel: DetailViewModel by viewModels {
        DetailViewModelFactory(DetailRepository())
    }

    companion object {
        const val EXTRA_REPORT = "extra_report"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        report = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_REPORT, Report::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_REPORT)
        } ?: return

        populateData()
        setupUserAccess() // Panggilan ini tetap di sini
        setupActionListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.action_detail)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun populateData() {
        binding.apply {
            Glide.with(this@DetailActivity)
                .load(report.urlBukti)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(ivProof)

            tvTimestampDetail.text = viewModel.formatTimestampToWIB(report.timestamp)
            tvDateOfIncidentDetail.text = report.tanggalBullying
            tvLocationDetail.text = report.lokasi
            chipFrequency.text = report.frekuensi
            tvDescriptionDetail.text = report.deskripsi
            tvWhatsappDetail.text = report.nomorWhatsapp

            updateStatusUI(report.status)
            updateRoleUI(report.peran)
        }
    }

    // --- PERUBAHAN UTAMA ADA DI FUNGSI INI ---
    private fun setupUserAccess() {
        val currentUser = auth.currentUser
        // Gunakan lifecycleScope untuk menjalankan suspend function
        lifecycleScope.launch {
            val isAdmin = AdminUID.isAdmin(currentUser?.uid)
            if (isAdmin) {
                // UI untuk Admin
                binding.switchHandled.visibility = View.VISIBLE
                binding.btnContactReporter.visibility = View.VISIBLE
                binding.btnCancelReport.visibility = View.GONE
            } else {
                // UI untuk pengguna biasa
                binding.switchHandled.visibility = View.GONE
                binding.btnContactReporter.visibility = View.GONE
                // Cek jika user adalah pemilik laporan dan statusnya masih "terlapor"
                if (currentUser?.uid == report.userId && report.status.equals("terlapor", true)) {
                    binding.btnCancelReport.visibility = View.VISIBLE
                } else {
                    binding.btnCancelReport.visibility = View.GONE
                }
            }
        }
    }


    private fun setupActionListeners() {
        binding.apply {
            switchHandled.setOnCheckedChangeListener(null)
            switchHandled.isChecked = report.status.equals("ditangani", true)
            switchHandled.setOnCheckedChangeListener { _, isChecked ->
                val newStatus = if (isChecked) "ditangani" else "terlapor"
                showConfirmationDialog(
                    "Ubah Status",
                    "Apakah Anda yakin ingin mengubah status laporan menjadi ${newStatus.uppercase()}?"
                ) {
                    viewModel.updateStatus(report.id, newStatus)
                }
            }

            btnContactReporter.setOnClickListener {
                val formattedNumber = viewModel.formatPhoneNumber(report.nomorWhatsapp)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formattedNumber"))
                startActivity(intent)
            }

            btnCancelReport.setOnClickListener {
                showConfirmationDialog(
                    "Batalkan Laporan",
                    "Apakah Anda yakin ingin membatalkan laporan ini? Tindakan ini tidak dapat diurungkan."
                ) {
                    viewModel.deleteReport(report.id)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) {
            binding.progressBarDetail.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.operationResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let { result ->
                result.onSuccess { type ->
                    when (type) {
                        "Update berhasil" -> {
                            Toast.makeText(this, "Status berhasil diperbarui", Toast.LENGTH_SHORT).show()
                            val newStatus = if (binding.switchHandled.isChecked) "ditangani" else "terlapor"
                            updateStatusUI(newStatus)
                            if (newStatus == "ditangani") {
                                binding.btnCancelReport.visibility = View.GONE
                            }
                        }
                        "Delete berhasil" -> {
                            Toast.makeText(this, "Laporan berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }.onFailure {
                    Toast.makeText(this, "Operasi gagal: ${it.message}", Toast.LENGTH_LONG).show()
                    binding.switchHandled.isChecked = report.status.equals("ditangani", true)
                }
            }
        }
    }

    private fun updateStatusUI(status: String) {
        report = report.copy(status = status)
        binding.tvStatusDetail.text = status.uppercase()
        val background = when (status.lowercase()) {
            "ditangani" -> R.drawable.bg_status_ditangani
            else -> R.drawable.bg_status_terlapor
        }
        binding.tvStatusDetail.background = ContextCompat.getDrawable(this, background)
    }

    private fun updateRoleUI(role: String) {
        binding.tvRoleDetail.text = role.uppercase()
        val background = when (role.lowercase()) {
            "saksi" -> R.drawable.bg_peran_saksi
            else -> R.drawable.bg_peran_korban
        }
        binding.tvRoleDetail.background = ContextCompat.getDrawable(this, background)
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ya") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                if (title == "Ubah Status") {
                    binding.switchHandled.isChecked = report.status.equals("ditangani", true)
                }
                dialog.dismiss()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
