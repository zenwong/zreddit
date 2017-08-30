package com.zen.zreddit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.zen.zreddit.adapters.PostsAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val lm = LinearLayoutManager(this)
		lm.orientation = LinearLayoutManager.VERTICAL

		launch(UI) {
			val posts = Reddit.parsePosts("https://www.reddit.com/r/all/.json").await()

//			posts.forEach {
//				println("${it.comments}: ${it.title}")
//			}

			val postsAdapter = PostsAdapter()
			postsAdapter.setData(posts)
			rv.apply {
				setHasFixedSize(true)
				layoutManager = lm
				adapter = postsAdapter
			}

		}

	}


}
