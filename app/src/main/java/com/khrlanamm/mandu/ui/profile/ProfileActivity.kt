package com.khrlanamm.mandu.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.data.AdminUID
import com.khrlanamm.mandu.databinding.ActivityProfileBinding
import com.khrlanamm.mandu.ui.auth.AuthActivity
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.action_account)
            setDisplayHomeAsUpEnabled(true)
        }

        displayUserData()
        setupUserRole() // Panggil fungsi untuk set role

        binding.buttonEditProfile.setOnClickListener {
            Toast.makeText(
                this,
                "Profil hanya bisa diedit melalui akun Google Anda",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.buttonLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun displayUserData() {
        val name = intent.getStringExtra("USER_NAME")
        val email = intent.getStringExtra("USER_EMAIL")
        val photoUrl = intent.getStringExtra("USER_PHOTO_URL")
        val uid = intent.getStringExtra("USER_UID")

        binding.textName.text = name
        binding.textEmail.text = email
        binding.textUserId.text = uid

        Glide.with(this)
            .load(photoUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .circleCrop()
            .into(binding.profileImage)
    }

    private fun setupUserRole() {
        val uid = intent.getStringExtra("USER_UID")
        lifecycleScope.launch {
            val isAdmin = AdminUID.isAdmin(uid)
            binding.textUserRole.apply {
                if (isAdmin) {
                    text = getString(R.string.admin_role)
                    setBackgroundResource(R.drawable.bg_peran_saksi)
                } else {
                    text = getString(R.string.user_role)
                    setBackgroundResource(R.drawable.bg_status_terlapor)
                }
                visibility = View.VISIBLE
            }
        }
    }


    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Konfirmasi Keluar")
            setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
            setPositiveButton("Ya, Keluar") { _, _ ->
                logoutUser()
            }
            setNegativeButton("Batal", null)
            create()
            show()
        }
    }

    private fun logoutUser() {
        Firebase.auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, AuthActivity::class.java)
            intent.putExtra(AuthActivity.EXTRA_FROM_LOGOUT, true)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
