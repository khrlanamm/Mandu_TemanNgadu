package com.khrlanamm.mandu.ui.report

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.databinding.ActivityReportBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportBinding

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var auth: FirebaseAuth
    private lateinit var functions: FirebaseFunctions

    private var imageUri: Uri? = null
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.ivPreviewBukti.setImageURI(imageUri)
                binding.ivPreviewBukti.visibility = View.VISIBLE
            } else {
                Log.d("PhotoPicker", "Tidak ada media yang dipilih")
            }
        }

    private val requestCameraPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher: ActivityResultLauncher<Uri> =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                binding.ivPreviewBukti.setImageURI(imageUri)
                binding.ivPreviewBukti.visibility = View.VISIBLE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.appbar.setPadding(insets.left, insets.top, insets.right, 0)

            val layoutParams =
                binding.buttonKirimLaporan.layoutParams as android.view.ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin =
                insets.bottom + resources.getDimensionPixelSize(R.dimen.fab_margin)
            binding.buttonKirimLaporan.layoutParams = layoutParams

            WindowInsetsCompat.CONSUMED
        }

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        functions = FirebaseFunctions.getInstance("us-central1")

        setupToolbar()
        setupDropdown()
        setupDatePicker()
        setupActionListeners()
    }

    private fun setupActionListeners() {
        binding.buttonUnggahBukti.setOnClickListener {
            showImageSourceDialog()
        }

        binding.buttonKirimLaporan.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(
                    this,
                    "Anda harus masuk untuk dapat mengirim laporan.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (validateInput()) {
                showSendConfirmationDialog()
            }
        }

        binding.fabWhatsapp.setOnClickListener {
            showWhatsAppConfirmationDialog()
        }
    }

    private fun showWhatsAppConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Hubungi Bimbingan Konseling")
            .setMessage("Anda akan diarahkan ke WhatsApp Bimbingan Konseling SMAN 1 Sukodadi.")
            .setIcon(R.drawable.app_logo)
            .setPositiveButton("Lanjutkan") { dialog, _ ->
                openWhatsApp()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSendConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Kirim Laporan?")
            .setMessage("Pastikan semua data yang Anda masukkan sudah benar.")
            .setIcon(R.drawable.app_logo)
            .setPositiveButton("Kirim") { dialog, _ ->
                uploadImageAndSaveReport()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Laporan Berhasil Terkirim")
            .setMessage("Laporan Anda sudah kami terima. Ingat, Anda tidak sendirian dan Anda telah berani melangkah maju. Kami akan segera menindaklanjuti laporan ini. Kerahasiaan anda akan kami jaga.")
            .setIcon(R.drawable.app_logo)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun openWhatsApp() {
        val phoneNumber = "+6285731774570"
        val url = "https://wa.me/$phoneNumber"
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = url.toUri()
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp tidak terpasang.", Toast.LENGTH_SHORT).show()
            Log.e("ReportActivity", "Error opening WhatsApp", e)
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Buka Kamera", "Pilih dari Galeri")
        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    1 -> {
                        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }
            .show()
    }

    private fun openCamera() {
        val newImageUri = createImageUri()
        imageUri = newImageUri
        takePictureLauncher.launch(newImageUri)
    }

    private fun createImageUri(): Uri {
        val imageFile = File(cacheDir, "temp_image_${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            imageFile
        )
    }

    private fun saveReportToFirestore(imageUrl: String?) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            Toast.makeText(this, "Gagal mengidentifikasi pengguna.", Toast.LENGTH_LONG).show()
            return
        }

        val selectedRole = if (binding.radioKorban.isChecked) "Korban" else "Saksi"
        val description = binding.etDeskripsi.text.toString()
        val location = binding.etLokasi.text.toString()
        val bullyingDate = binding.etTanggal.text.toString()
        val frequency = binding.actvFrekuensi.text.toString()
        val whatsappNumber = binding.etWhatsapp.text.toString()

        val report = hashMapOf(
            "id" to UUID.randomUUID().toString(),
            "userId" to userId,
            "peran" to selectedRole,
            "tanggalBullying" to bullyingDate,
            "lokasi" to location,
            "frekuensi" to frequency,
            "deskripsi" to description,
            "nomorWhatsapp" to whatsappNumber,
            "urlBukti" to imageUrl,
            "timestamp" to Date(),
            "status" to "terlapor"
        )

        firestore.collection("reports")
            .add(report)
            .addOnSuccessListener {
                val data = hashMapOf(
                    "peran" to selectedRole,
                    "deskripsi" to description,
                    "lokasi" to location,
                    "tanggalBullying" to bullyingDate,
                    "urlBukti" to imageUrl
                )

                functions
                    .getHttpsCallable("sendReportNotification")
                    .call(data)
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            val e = task.exception
                            Log.w("ReportActivity", "Pemanggilan fungsi gagal", e)
                        } else {
                            Log.d("ReportActivity", "Notifikasi berhasil dipicu.")
                        }
                        showLoading(false)
                        showSuccessDialog()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Gagal mengirim laporan: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun uploadImageAndSaveReport() {
        showLoading(true)
        if (imageUri != null) {
            val fileName = "bukti_${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("laporan_bukti/$fileName")

            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        saveReportToFirestore(imageUrl)
                    }.addOnFailureListener {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Gagal mendapatkan URL gambar: ${it.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "Gagal mengunggah gambar: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            saveReportToFirestore(null)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.action_report)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupDropdown() {
        val frekuensiOptions = resources.getStringArray(R.array.frekuensi_bullying_options)
        val adapter = ArrayAdapter(this, R.layout.list_item_frekuensi, frekuensiOptions)
        binding.actvFrekuensi.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView(calendar)
        }

        binding.etTanggal.setOnClickListener {
            DatePickerDialog(
                this,
                datePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateInView(calendar: Calendar) {
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        binding.etTanggal.setText(sdf.format(calendar.time))
    }

    private fun validateInput(): Boolean {
        if (binding.radioGroupRole.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Pilih peran Anda (Korban/Saksi)", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.etTanggal.text.isNullOrEmpty()) {
            binding.tilTanggal.error = "Tanggal tidak boleh kosong"
            return false
        } else {
            binding.tilTanggal.error = null
        }
        if (binding.etLokasi.text.isNullOrEmpty()) {
            binding.tilLokasi.error = "Lokasi tidak boleh kosong"
            return false
        } else {
            binding.tilLokasi.error = null
        }
        if (binding.actvFrekuensi.text.isNullOrEmpty()) {
            binding.tilFrekuensi.error = "Frekuensi tidak boleh kosong"
            return false
        } else {
            binding.tilFrekuensi.error = null
        }
        if (binding.etDeskripsi.text.isNullOrEmpty()) {
            binding.tilDeskripsi.error = "Deskripsi tidak boleh kosong"
            return false
        } else {
            binding.tilDeskripsi.error = null
        }
        if (binding.etWhatsapp.text.isNullOrEmpty()) {
            binding.tilWhatsapp.error = "Nomor WhatsApp tidak boleh kosong"
            return false
        } else {
            binding.tilWhatsapp.error = null
        }
        return true
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonKirimLaporan.isEnabled = !isLoading
        binding.buttonUnggahBukti.isEnabled = !isLoading
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
