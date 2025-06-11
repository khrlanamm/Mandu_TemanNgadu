package com.khrlanamm.mandu.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.khrlanamm.mandu.R

class ArticleAdapter(private val context: Context, private var articles: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    // Fungsi untuk memperbarui data di adapter
    fun updateData(newArticles: List<Article>) {
        articles = newArticles
        notifyDataSetChanged() // Memberi tahu RecyclerView untuk me-render ulang
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
            // Menangani klik pada setiap item
            itemView.setOnClickListener {
                // Pastikan posisi adapter valid
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val article = articles[adapterPosition]
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    context.startActivity(intent)
                }
            }
        }

        fun bind(article: Article) {
            title.text = article.title
            description.text = article.description
            image.setImageResource(article.image)
        }
    }
}
