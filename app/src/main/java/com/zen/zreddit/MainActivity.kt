package com.zen.zreddit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.row_post.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {
	var postUrl = "https://www.reddit.com/r/all/.json"

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val lm = LinearLayoutManager(this)
		lm.orientation = LinearLayoutManager.VERTICAL

		intent?.data?.let {
			if (it.pathSegments.size > 2) {
				startActivity(Intent(this, CommentsActivity::class.java))
				finish()
			} else {
				postUrl = it.toString() + ".json"
			}
		}

		launch(UI) {
			val posts = Reddit.parsePosts(postUrl).await()
			val postsAdapter = PostsAdapter()
			postsAdapter.setData(posts)
			rv.apply {
				setHasFixedSize(true)
				layoutManager = lm
				adapter = postsAdapter
			}
		}

	}

	inner class PostsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
		val IMG_POST = 0
		val TXT_POST = 1
		val data = ArrayList<Post>()

		fun setData(list: ArrayList<Post>) {
			//val start = data.size
			data.addAll(list)
			//notifyItemRangeInserted(start + 1, list.size)
			notifyDataSetChanged()
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			if (viewType == IMG_POST)
				return PostsViewHolder(parent.inflate(R.layout.row_post))
			return TxtViewHolder(parent.inflate(R.layout.row_post_txt))
		}

		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, idx: Int) {
			when (holder.itemViewType) {
				IMG_POST -> {
					holder as PostsViewHolder
					holder.bind(data[idx])
				}
				TXT_POST -> {
					holder as TxtViewHolder
					holder.bind((data[idx]))
				}
			}
		}

		override fun getItemViewType(idx: Int): Int {
			if (data[idx].preview.isNotBlank()) {
				return IMG_POST
			}
			return TXT_POST
		}


		override fun getItemCount() = data.size
	}

	inner class PostsViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

		fun bind(post: Post) {
			itemView.tvTitle.text = post.title
			itemView.tvSubreddit.text = post.subreddit
			itemView.tvCreated.text = post.comments.toString()
			itemView.imgPreview.loadUrl(post.preview)

			itemView.setOnClickListener {
				val url = "http://reddit.com" + post.permalink
				Log.d("TEST", "url: $url")
				val intent = Intent(baseContext, CommentsActivity::class.java)
				intent.data = Uri.parse(url)
				startActivity(intent)
			}
		}

	}

	inner class TxtViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

		fun bind(post: Post) {
			itemView.tvTitle.text = post.title
			itemView.tvSubreddit.text = post.subreddit
			itemView.tvCreated.text = post.comments.toString()

			itemView.setOnClickListener {
				val url = "https://reddit.com" + post.permalink
				val intent = Intent(baseContext, CommentsActivity::class.java)
				intent.data = Uri.parse(url)
				startActivity(intent)
			}
		}

	}


}
