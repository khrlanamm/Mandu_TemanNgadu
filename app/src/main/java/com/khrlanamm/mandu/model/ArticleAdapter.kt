package com.khrlanamm.mandu.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.facebook.shimmer.ShimmerFrameLayout
import com.khrlanamm.mandu.R
import com.khrlanamm.mandu.ui.article.ArticleDetailActivity

@Suppress("DEPRECATION")
class ArticleAdapter(private val context: Context, private var articles: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
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
        private val shimmerLayout: ShimmerFrameLayout = view.findViewById(R.id.shimmer_view_container)

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

            // Mulai efek shimmer
            shimmerLayout.startShimmer()

            // Atur opsi permintaan, termasuk timeout 10 detik
            val requestOptions = RequestOptions()
                .timeout(10000) // 10000 milidetik = 10 detik

            Glide.with(context)
                .load(article.image)
                .apply(requestOptions) // Terapkan opsi timeout
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hentikan dan sembunyikan shimmer drawable saat gagal.
                        shimmerLayout.hideShimmer()
                        return false // Mengembalikan false agar Glide dapat menampilkan gambar error
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Hentikan dan sembunyikan shimmer drawable saat gambar siap.
                        shimmerLayout.hideShimmer()
                        return false // Mengembalikan false agar Glide dapat menampilkan gambar yang sudah dimuat
                    }
                })
                .error(R.drawable.placeholder_image) // Menampilkan gambar placeholder jika terjadi error
                .into(image)
        }
    }
}
