package com.zen.zreddit.adapters

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.zen.zreddit.Post
import com.zen.zreddit.R
import com.zen.zreddit.inflate
import com.zen.zreddit.loadUrl
import kotlinx.android.synthetic.main.row_post.view.*

class PostsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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

class PostsViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

	fun bind(post: Post) {
		itemView.tvTitle.text = post.title
		itemView.tvSubreddit.text = post.subreddit
		itemView.tvCreated.text = post.comments.toString()
		itemView.imgPreview.loadUrl(post.preview)
	}

}

class TxtViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

	fun bind(post: Post) {
		itemView.tvTitle.text = post.title
		itemView.tvSubreddit.text = post.subreddit
		itemView.tvCreated.text = post.comments.toString()
	}

}