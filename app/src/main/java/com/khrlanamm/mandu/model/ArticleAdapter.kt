package com.khrlanamm.mandu.model

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.ui.article.ArticleDetailActivity

@Suppress("DEPRECATION")
class ArticleAdapter(private val context: Context, private var articles: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    fun updateData(newArticles: List<Article>) {
        articles = newArticles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.bind(article)
    }

    override fun getItemCount(): Int = articles.size

    inner class ArticleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.articleTitle)
        private val description: TextView = view.findViewById(R.id.articleDescription)
        private val image: ImageView = view.findViewById(R.id.articleImage)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val article = articles[adapterPosition]

                    if (article.url.startsWith("http://") || article.url.startsWith("https://")) {
                        val intent = Intent(context, ArticleDetailActivity::class.java).apply {
                            putExtra(ArticleDetailActivity.EXTRA_URL, article.url)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }

        fun bind(article: Article) {
            title.text = article.title
            description.text = article.description

            Glide.with(context)
                .load(article.image)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(image)
        }
    }
}
