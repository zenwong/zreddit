package com.zen.zreddit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.util.Base64
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit


class Post {
	var title = ""
	var preview = ""
	var subreddit = ""
	var permalink = ""
	var comments = 0
	var score = 0
	var created = 0L
}

class Header {
	var id = ""
	var author = ""
	var selftext = ""
	var embed = ""
	var preview = ""
	var mp4 = ""
	var url = ""
	var title = ""
	var created = 0L
	var comments = 0
	var score = 0
}

class Comment {
	var depth = 0
	var body = ""
	var body_html = ""
	var author = ""
	var name = ""
	var parent = ""
	var score = 0
	var created_utc = 0L
}

class Media {
	var embed = ""
	var html = ""
	var title = ""
	var thumbnail = ""
	var provider = ""
}

class Preview {
	var source = ""
	var mp4 = ""
	var thumb = ""
	var gif = ""
}

object Reddit {
	var postAfter = ""
	val CLIENTID = "f-A-UqH0oTkkeA"
	val REDIRECT = "http://zreddit"
	val REDDIT_AUTH_TOKEN = "https://ssl.reddit.com/api/v1/access_token"
	val REDDIT_FRONT = "https://oauth.reddit.com?limit=50"
	val BASIC_AUTH = Base64.encodeToString("$CLIENTID:".toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
	val timeout = 3L
	val useragent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"
	val jsonFactory = JsonFactory()
	var isOnline = true
	val cacheSize = 10 * 1024 * 1024L
	var cacheDir = ""
	val cache = Cache(File(cacheDir), cacheSize)
	val client = OkHttpClient.Builder()
		.authenticator(RedditOauthAuthenticator())
		.connectTimeout(timeout, TimeUnit.SECONDS).readTimeout(timeout, TimeUnit.SECONDS).build()
	val cachedClient = OkHttpClient.Builder().cache(cache)
		.addInterceptor { chain ->
			var request = chain.request()
			request = if (isOnline) {
				request.newBuilder().header("Cache-Control", "public, max-age=" + 60).build()
			} else {
				request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
			}
			chain.proceed(request)
		}
		.build()

	fun parseComments(url: String, header: Header): Deferred<ArrayList<Comment>> {
		return async(CommonPool) {
			val comments = ArrayList<Comment>()
			//val json = get(url)
			val json = getOrEmpty(url)
			val jp = jsonFactory.createParser(json)
			while (jp.nextToken() !== null) {
				if ("selftext".equals(jp.currentName)) {
					header.selftext = jp.nextTextValue()

					loop@ while (jp.nextToken() != JsonToken.END_OBJECT) {
						val key = jp.currentName
						when (key) {
							"user_reports" -> jp.skipChildren()
							"secure_media" -> {
								if (jp.nextToken() == JsonToken.START_OBJECT) {
									val media = Media()
									parseMedia(jp, media)
									header.embed = media.html
								}
							}
							"id" -> header.id = jp.nextTextValue()
							"author" -> header.author = jp.nextTextValue()
							"media" -> jp.skipChildren()
							"score" -> header.score = jp.nextIntValue(0)
							"preview" -> {
								val local = Preview()
								parsePreview(jp, local, 320)
								header.preview = local.source
								header.mp4 = local.mp4
							}
							"mod_reports" -> jp.skipChildren()
							"secure_media_embed" -> jp.skipChildren()
							"url" -> header.url = jp.nextTextValue()
							"title" -> header.title = jp.nextTextValue()
							"created_utc" -> {
								jp.nextToken()
								header.created = jp.getValueAsLong(0L)
							}
							"num_comments" -> header.comments = jp.nextIntValue(0)
							"before" -> {
								println("inside before")
								break@loop
							}
						}
					}

				}

				if ("replies" == jp.currentName) {
					val comment = Comment()
					val replies = jp.nextValue()

					if (replies == JsonToken.START_OBJECT) parseReplies(jp)

					while (jp.nextToken() != JsonToken.END_OBJECT) {
						when (jp.currentName) {
							"user_reports" -> jp.skipChildren()
							"author" -> comment.author = jp.nextTextValue()
							"parent_id" -> comment.parent = jp.nextTextValue()
							"score" -> comment.score = jp.nextIntValue(0)
							"body" -> {
								val body = jp.nextTextValue().trim()

								val clean = body.split(" ").map {
									if (it.startsWith("/u")) it.replace("/r", "https://reddit.com/r") else it
								}.joinToString(" ")

								comment.body = clean
							}
							"body_html" -> comment.body_html = Html.fromHtml(jp.nextTextValue()).toString()
							"depth" -> comment.depth = jp.nextIntValue(0)
						}
					}
					comments.add(comment)
					//println("${comment.depth} - ${comment.author}) ${comment.body}")
				}

			}
			comments
		}
	}


	fun parseReplies(jp: JsonParser) {

	}

	fun parsePosts(url: String): Deferred<ArrayList<Post>> {
		return async(CommonPool) {
			//val json = get(url)
			var local = url
			if(postAfter.isNotBlank()) {
				local = url + "?limit=50&after=$postAfter"
			}
			val json = getOrEmpty(local)
			val posts = ArrayList<Post>()
			val jp = jsonFactory.createParser(json)
			while (jp.nextToken() !== null) {
				if ("domain" == jp.currentName) {
					val post = Post()

					while (jp.nextToken() != JsonToken.END_OBJECT) {
						when (jp.currentName) {
							"media_embed" -> jp.skipChildren()
							"subreddit" -> post.subreddit = jp.nextTextValue()
							"user_reports" -> jp.skipChildren()
							"secure_media" -> jp.skipChildren()
							"title" -> post.title = jp.nextTextValue()
							"mod_reports" -> jp.skipChildren()
							"score" -> post.score = jp.nextIntValue(0)
							"preview" -> parsePostPreview(jp, post)
							"secure_media_embed" -> jp.skipChildren()
							"created_utc" -> post.created = jp.getValueAsLong(0L)
							"media" -> jp.skipChildren()
							"num_comments" -> post.comments = jp.nextIntValue(0)
							"permalink" -> post.permalink = jp.nextTextValue()
						}
					}

					posts.add(post)
				}

				if ("after" == jp.currentName) {
					postAfter = jp.nextTextValue()
				}

			}
			posts
		}
	}

	fun parsePostPreview(jp: JsonParser, post: Post, width: Int = 320) {
		val thumbWidth = "w=$width"
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			when (jp.currentName) {
				"source" -> jp.skipChildren()
				"resolutions" -> {
					while (jp.nextToken() != JsonToken.END_ARRAY) {
						if ("url" == jp.currentName) {
							val thumb = jp.nextTextValue()
							if (thumb.contains(thumbWidth)) {
								post.preview = thumb.replace("amp;", "")
							}
						}
					}
				}
				"variants" -> jp.skipChildren()
				"id" -> {
					jp.skipChildren()
					jp.nextToken()
					jp.nextToken()
					jp.nextToken()
					jp.nextToken()
				}
			}
		}
	}


	fun parsePreview(jp: JsonParser, preview: Preview, width: Int = 320) {
		val thumbWidth = "w=$width"
		var selectedPreview = ""
		while (jp.nextToken() !== JsonToken.END_OBJECT) {
			val key = jp.currentName

			when (key) {
				"source" -> {
					while (jp.nextToken() != JsonToken.END_OBJECT) {
						if ("url".equals(jp.currentName)) {
							preview.source = jp.nextTextValue().replace("amp;", "")
						}
					}
				}
				"resolutions" -> {
					while (jp.nextToken() != JsonToken.END_ARRAY) {
						if ("url".equals(jp.currentName)) {
							val thumb = jp.nextTextValue()
							selectedPreview = thumb
							if (thumb.contains(thumbWidth)) {
								preview.thumb = thumb.replace("amp;", "")
							}
						}
					}
				}
				"variants" -> {
					while (jp.nextToken() != JsonToken.END_OBJECT) {
						val k = jp.currentName
						when (k) {
							"gif" -> {
								while (jp.nextToken() != JsonToken.END_ARRAY) {
									if ("url".equals(jp.currentName)) {
										val thumb = jp.nextTextValue()
										if (thumb.contains(thumbWidth)) {
											preview.gif = thumb.replace("amp;", "")
										}
									}
								}
								jp.nextToken() // move to start of next object mp4
							}
							"mp4" -> {
								while (jp.nextToken() != JsonToken.END_ARRAY) {
									if ("url".equals(jp.currentName)) {
										val thumb = jp.nextTextValue()
										if (thumb.contains(thumbWidth)) {
											preview.mp4 = thumb.replace("amp;", "")
										}
									}
								}
								jp.nextToken()
								jp.nextToken()
							}
						}
					}
				}
				"id" -> {
					//println("inside id")
					jp.skipChildren()
					jp.nextToken()
					jp.nextToken()
					jp.nextToken()
					//println(jp.currentName)
				}
			}

		}
		// if desired preview width not available select next best width
		if (preview.thumb.isBlank()) preview.thumb = selectedPreview
	}

	fun parseMedia(jp: JsonParser, media: Media) {
		while (jp.nextToken() !== JsonToken.END_OBJECT) {
			val key = jp.currentName
			when (key) {
				"title" -> media.title = jp.nextTextValue()
				"html" -> media.html = jp.nextTextValue()
				"provider_name" -> media.provider = jp.nextTextValue()
				"thumbnail_url" -> media.thumbnail = jp.nextTextValue()
			}
		}
		jp.nextToken()
		jp.nextToken()
		jp.nextToken()
	}

	fun getAuthUrl(clientid: String = CLIENTID, state: String = "NONCE", redirect: String = "http://zreddit", scope: String = "read identity privatemessages"): String {
		//println("https://ssl.reddit.com/api/v1/authorize.compact?client_id=$clientid&response_type=code&state=$state&redirect_uri=$redirect&duration=permanent&scope=$scope")
		return "https://ssl.reddit.com/api/v1/authorize.compact?client_id=$clientid&response_type=code&state=$state&redirect_uri=$redirect&duration=permanent&scope=$scope"
	}

	fun getAccessToken(activity: Activity, url: String) {
		val uri = Uri.parse(url)
		val error = uri.getQueryParameter("error")
		if (error !== null) {
			println(error)
			if ("access_denied" == error) {
				//EventBus.getDefault().post(Navigation(BROWSER))
			}
		} else {
			val code = uri.getQueryParameter("code")
			val body = FormBody.Builder().add("code", code).add("redirect_uri", REDIRECT).add("grant_type", "authorization_code").build()
			val req = Request.Builder().url(REDDIT_AUTH_TOKEN).addHeader("Authorization", "Basic $BASIC_AUTH").post(body).build()

			client.newCall(req).enqueue(object : Callback {
				override fun onFailure(call: Call?, e: IOException?) {
				}

				override fun onResponse(call: Call?, response: Response) {
					val jp = jsonFactory.createParser(response.body()!!.string())

					while (jp.nextToken() != JsonToken.END_OBJECT) {
						when (jp.currentName) {
							"access_token" -> {
								jp.nextToken()
								prefs.accessToken = jp.valueAsString
								//println("ACCESS TOKEN ${prefs.accessToken}")
								activity.startActivity(Intent(activity, MainActivity::class.java))
							}
							"refresh_token" -> {
								jp.nextToken()
								prefs.refreshToken = jp.valueAsString
							}
						}
					}
				}
			})
		}

	}

	fun refreshAccessToken(): String {
		val body = FormBody.Builder().add("grant_type", "refresh_token").add("refresh_token", prefs.refreshToken).build()
		val req = Request.Builder().url(REDDIT_AUTH_TOKEN).addHeader("Authorization", "Basic $BASIC_AUTH").post(body).build()
		val json = client.newCall(req).execute().body()!!.string()
		val jp = jsonFactory.createParser(json)

		var local = ""
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			when (jp.currentName) {
				"access_token" -> {
					local = jp.nextTextValue()
					prefs.accessToken = local
				}
			}
		}
		return local
	}

	fun normalizeCommentsUrl(url: String, limit: Int = 10): String {
		val uri = Uri.parse(url)
		if(uri.pathSegments.last().equals(".json"))
			return  "$REDDIT_FRONT${uri.path}?limit=$limit"
		return "$REDDIT_FRONT${uri.path}.json?limit=$limit"
	}

	fun getOrEmpty(url: String): String {
		val resp = client.newCall(Request.Builder().url(url).addHeader("Authorization", "Bearer ${prefs.accessToken}").build()).execute()
		return resp.body()!!.string()
	}


	fun get(url: String): String {
		try {
			return client.newCall(Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent", useragent).build()).execute().body()!!.string()
		} catch (ex: Exception) {
			println(ex.message)
		}
		return ""
	}

	fun getCached(url: String): String {
		try {
			return cachedClient.newCall(Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent", useragent).build()).execute().body()!!.string()
		} catch (ex: Exception) {
			println(ex.message)
		}
		return ""
	}

	suspend fun getPostsAfter(limit: Int = 5) : Deferred<ArrayList<Post>> {
		return parsePosts(getOrEmpty("$REDDIT_FRONT?limit=$limit&after=$postAfter"))
	}
}

class RedditOauthAuthenticator : Authenticator {
	override fun authenticate(route: Route, response: Response): Request {
		return response.request().newBuilder().header("Authorization", "Bearer ${prefs.refreshToken}").build()
	}
}