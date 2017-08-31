package com.zen.zreddit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class CommentsActivity: AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		intent?.let {
			println("Comments Activity ${intent.data}")
		}
	}
}