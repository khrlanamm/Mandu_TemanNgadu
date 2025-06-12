package com.khrlanamm.mandu.model

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    private val db = Firebase.firestore

    private var originalArticles: List<Article> = emptyList()

    private val _articles = MutableLiveData<List<Article>>()
    val articles: LiveData<List<Article>> get() = _articles

    private val _noResultsFound = MutableLiveData<Boolean>()
    val noResultsFound: LiveData<Boolean> get() = _noResultsFound

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        loadArticlesFromFirestore()
    }

    fun loadArticlesFromFirestore() {
        _isLoading.value = true
        db.collection("articles")
            .orderBy("id")
            .get()
            .addOnSuccessListener { result ->
                val articleList = result.toObjects(Article::class.java)

                originalArticles = articleList
                _articles.value = articleList
                _isLoading.value = false
                _errorMessage.value = null // Bersihkan pesan error jika berhasil
            }
            .addOnFailureListener { exception ->
                // Jika gagal, kirim pesan error ke UI melalui LiveData
                _errorMessage.value = "Gagal memuat artikel. Periksa koneksi internet Anda."
                Log.w("HomeViewModel", "Error getting documents.", exception)
                _isLoading.value = false
                _articles.value = emptyList()
            }
    }

    fun searchArticles(query: String) {
        val filteredList = if (query.isBlank()) {
            originalArticles
        } else {
            originalArticles.filter { article ->
                article.title.contains(query, ignoreCase = true)
            }
        }

        _articles.value = filteredList
        _noResultsFound.value = filteredList.isEmpty() && query.isNotEmpty()
    }
}
