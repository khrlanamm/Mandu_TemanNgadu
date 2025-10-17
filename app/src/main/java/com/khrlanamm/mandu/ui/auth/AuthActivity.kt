package com.khrlanamm.mandu.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.khrlanamm.mandu.MainActivity
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.databinding.ActivityAuthBinding
import kotlinx.coroutines.launch
import java.security.SecureRandom

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private var isDataReady = false

    companion object {
        const val EXTRA_FROM_LOGOUT = "extra_from_logout"
        private const val TAG = "AuthActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val fromLogout = intent.getBooleanExtra(EXTRA_FROM_LOGOUT, false)
        if (fromLogout) setTheme(R.style.Theme_Mandu)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({ isDataReady = true }, 1000)
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                return if (isDataReady) {
                    content.viewTreeObserver.removeOnPreDrawListener(this); true
                } else false
            }
        })

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        auth.currentUser?.let {
            navigateToMain(it)
            return
        }

        playAnimations()

        binding.buttonGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        updateUI(isLoading = true)
        val nonce = generateNonce()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@AuthActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        Log.d(TAG, "Google ID Token acquired")
                        firebaseAuthWithGoogle(idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                        Toast.makeText(
                            this@AuthActivity,
                            "Gagal mem-parsing token Google",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI(false)
                    }
                } else {
                    Log.w(TAG, "Unexpected credential type: ${credential.javaClass.name}")
                    Toast.makeText(
                        this@AuthActivity,
                        "Gagal masuk: kredensial tidak dikenali",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI(false)
                }
            } catch (e: GetCredentialException) {
                Log.w(TAG, "getCredential() failed", e)
                val userMsg = when (e) {
                    is NoCredentialException -> "Tidak ada akun yang dipilih untuk login."
                    is GetCredentialCancellationException -> "Proses login dibatalkan."
                    else -> "Gagal masuk: ${e.message ?: "Kesalahan tidak diketahui"}"
                }
                Toast.makeText(this@AuthActivity, userMsg, Toast.LENGTH_SHORT).show()
                updateUI(false)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign-in error", e)
                Toast.makeText(
                    this@AuthActivity,
                    "Terjadi kesalahan saat masuk",
                    Toast.LENGTH_SHORT
                ).show()
                updateUI(false)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential_firebase:success")
                    navigateToMain(task.result.user)
                } else {
                    Log.w(TAG, "signInWithCredential_firebase:failure", task.exception)
                    Toast.makeText(this, "Autentikasi Firebase gagal", Toast.LENGTH_SHORT).show()
                    updateUI(false)
                }
            }
    }

    private fun navigateToMain(user: FirebaseUser?) {
        if (user == null) return
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("USER_NAME", user.displayName)
            putExtra("USER_EMAIL", user.email)
            putExtra("USER_PHOTO_URL", user.photoUrl?.toString())
            putExtra("USER_UID", user.uid)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun playAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val viewsToAnimate = listOf(
            binding.textWelcome,
            binding.logoLandscape,
            binding.imageVintage,
            binding.textLongDescription,
            binding.buttonGoogleSignIn
        )
        viewsToAnimate.forEachIndexed { index, view ->
            Handler(Looper.getMainLooper()).postDelayed({
                view.visibility = View.VISIBLE
                view.startAnimation(fadeIn)
            }, index * 400L)
        }
    }

    private fun updateUI(isLoading: Boolean) {
        binding.buttonGoogleSignIn.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun generateNonce(length: Int = 16): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}