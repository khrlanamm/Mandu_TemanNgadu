package com.khrlanamm.mandu.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.khrlanamm.mandu.R

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Mengambil data dummy dari resources
    private val articleTitles: Array<String> = context.resources.getStringArray(R.array.tempdata_article_title)
    private val articleDescriptions: Array<String> = context.resources.getStringArray(R.array.tempdata_article_description)
    private val articleUrls: Array<String> = context.resources.getStringArray(R.array.tempdata_article_url)
    private val articleImages: IntArray = context.resources.obtainTypedArray(R.array.tempdata_article_image).let { typedArray ->
        val array = IntArray(typedArray.length()) { index -> typedArray.getResourceId(index, -1) }
        typedArray.recycle()
        array
    }

    // LiveData yang akan diamati oleh Activity
    private val _articles = MutableLiveData<List<Article>>()
    val articles: LiveData<List<Article>> get() = _articles

    init {
        // Memuat data saat ViewModel dibuat
        loadArticles()
    }

    private fun loadArticles() {
        val data = mutableListOf<Article>()
        for (i in articleTitles.indices) {
            data.add(
                Article(
                    title = articleTitles[i],
                    description = articleDescriptions[i],
                    image = articleImages.getOrNull(i) ?: 0, // Fallback jika ada masalah
                    url = articleUrls.getOrNull(i) ?: ""
                )
            )
        }
        _articles.value = data
    }
}
