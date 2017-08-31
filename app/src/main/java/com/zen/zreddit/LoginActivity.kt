package com.zen.zreddit

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.login.*

class LoginActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.login)

		browser.loadUrl(Reddit.getAuthUrl())
		browser.webViewClient = object : WebViewClient() {
			override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
				when {
					url.startsWith(Reddit.REDIRECT) -> Reddit.getAccessToken(this@LoginActivity, url)
					else -> return false
				}
				return true
			}

			@TargetApi(Build.VERSION_CODES.HONEYCOMB)
			override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
				if (url.matches(""".*compact.*.css""".toRegex())) {
					return WebResourceResponse("text/css", "UTF-8", css.byteInputStream())
				}

				return null
			}
		}
	}

	val css = """
li {
	list-style-type: none;
}
div.icon, div.infobar, div.mobile-web-redirect-bar, div#topbar {
	display: none;
	visibility: collapse;
	height: 0px;
	padding: 0px;
	margin:0px;
}
input {
  margin-bottom: 10px;
  margin-left: 10px;
}
body {
	background-color: #FFF;
	height: 1000px;
}
input.newbutton {
	background-color: #888;
	font-size: 20pt;
	margin: 10px;
	border-image-source: none;
	color: #FFF;
	border: none;
	padding-left:10px;
	padding-right:10px;
	padding-top:6px;
	padding-bottom:6px;
}
button {
	background-color: #888;
	font-size: 15pt;
	border-image-source: none;
	color: #FFF;
	border: none;
	padding-left:10px;
	padding-right:10px;
	padding-top:6px;
	padding-bottom:6px;
}
input.allow {
	background-color: #0A0;
}
input.allow:active, input.allow:hover {
	background-color: #0F0;
}
input.decline {
	background-color: #A00;
}
input.decline:active, input.decline:hover {
	background-color: #F00;
}
form.pretty-form {
	float: left;
}"""

}

