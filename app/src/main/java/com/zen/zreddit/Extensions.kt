package com.zen.zreddit

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso


fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
	return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun ImageView.loadUrl(url: String) {
	Picasso.with(context).load(url).fit().centerCrop().into(this)
}

class Prefs (context: Context) {
	val PREFS_FILENAME = "com.zen.zreddit.prefs"
	val ACCESS_TOKEN = "access_token"
	val REFRESH_TOKEN = "refresh_token"
	val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

	var accessToken: String
		get() = prefs.getString(ACCESS_TOKEN, "")
		set(value) = prefs.edit().putString(ACCESS_TOKEN, value).apply()

	var refreshToken: String
		get() = prefs.getString(REFRESH_TOKEN, "")
		set(value) = prefs.edit().putString(REFRESH_TOKEN, value).apply()
}