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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.khrlanamm.mandu.MainActivity
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    var isDataReady = false

    companion object {
        const val EXTRA_FROM_LOGOUT = "extra_from_logout"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val fromLogout = intent.getBooleanExtra(EXTRA_FROM_LOGOUT, false)
        if (fromLogout) {
            setTheme(R.style.Theme_Mandu)
        }
        installSplashScreen()

        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            isDataReady = true
        }, 1000)

        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (isDataReady) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )

        auth = Firebase.auth

        if (auth.currentUser != null) {
            navigateToMain(auth.currentUser)
            return
        }

        configureGoogleSignIn()
        playAnimations()

        binding.buttonGoogleSignIn.setOnClickListener {
            signIn()
        }
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        updateUI(true) // Memulai loading
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("AuthActivity", "Google sign in failed", e)
                Toast.makeText(this, "Gagal masuk dengan Google: ${e.message}", Toast.LENGTH_SHORT).show()
                updateUI(false) // Menghentikan loading
            }
        } else {
            Log.w("AuthActivity", "Google sign in flow cancelled. Result code: ${result.resultCode}")
            Toast.makeText(this, "Login dengan Google dibatalkan", Toast.LENGTH_SHORT).show()
            updateUI(false) // Menghentikan loading
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("AuthActivity", "signInWithCredential_firebase:success")
                    navigateToMain(task.result.user)
                } else {
                    Log.w("AuthActivity", "signInWithCredential_firebase:failure", task.exception)
                    Toast.makeText(this, "Autentikasi Firebase gagal", Toast.LENGTH_SHORT).show()
                    updateUI(false) // Menghentikan loading
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
            }, (index * 400L))
        }
    }

    private fun updateUI(isLoading: Boolean) {
        binding.buttonGoogleSignIn.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}