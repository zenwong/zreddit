package com.zen.zreddit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.comments.*
import kotlinx.android.synthetic.main.row_post.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class CommentsActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.comments)
		val lm = LinearLayoutManager(this)
		lm.orientation = LinearLayoutManager.VERTICAL
		val commentsAdapter = CommentsAdapter()

		intent?.data?.let {

			launch(UI) {
				val header = Header()
				val comments = Reddit.parseComments(it.toString() + ".json", header).await()
				comments.forEach { println(it.author) }
				commentsAdapter.setData(comments)

				rvComment.apply {
					setHasFixedSize(true)
					layoutManager = lm
					adapter = commentsAdapter
				}
			}
		}
	}

	inner class CommentsAdapter : RecyclerView.Adapter<CommentsViewholder>() {
		val data = ArrayList<Comment>()

		fun setData(list: ArrayList<Comment>) {
			data.addAll(list)
			notifyDataSetChanged()
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewholder {
			return CommentsViewholder(parent.inflate(R.layout.row_post_txt))
		}

		override fun onBindViewHolder(holder: CommentsViewholder, idx: Int) {
			holder.bind(data[idx])
		}

		override fun getItemCount() = data.size
	}

	inner class CommentsViewholder(iv: View) : RecyclerView.ViewHolder(iv) {

		fun bind(comment: Comment) {
			itemView.tvTitle.text = comment.body
			itemView.tvSubreddit.text = comment.author
			//itemView.tvCreated.text = comment.created_utc.toString()
		}

	}
}