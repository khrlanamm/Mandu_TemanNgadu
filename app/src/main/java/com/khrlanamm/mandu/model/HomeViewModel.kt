package com.khrlanamm.mandu.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.khrlanamm.mandu.R

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private var originalArticles: List<Article> = emptyList()

    private val _articles = MutableLiveData<List<Article>>()
    val articles: LiveData<List<Article>> get() = _articles

    private val _noResultsFound = MutableLiveData<Boolean>()
    val noResultsFound: LiveData<Boolean> get() = _noResultsFound

    init {
        loadArticles()
    }

    private fun loadArticles() {
        val data = mutableListOf<Article>()
        val articleTitles = context.resources.getStringArray(R.array.tempdata_article_title)
        val articleDescriptions = context.resources.getStringArray(R.array.tempdata_article_description)
        val articleUrls = context.resources.getStringArray(R.array.tempdata_article_url)
        val articleImages = context.resources.getStringArray(R.array.tempdata_article_image)

        for (i in articleTitles.indices) {
            data.add(
                Article(
                    title = articleTitles[i],
                    description = articleDescriptions[i],
                    image = articleImages.getOrNull(i) ?: "",
                    url = articleUrls.getOrNull(i) ?: ""
                )
            )
        }
        originalArticles = data
        _articles.value = originalArticles
    }

    fun searchArticles(query: String) {
        val filteredList = if (query.isEmpty()) {
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
