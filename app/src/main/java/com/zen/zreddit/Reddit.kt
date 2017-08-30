package com.zen.zreddit

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.OkHttpClient
import okhttp3.Request
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
	val jsonFactory = JsonFactory()
	val comments = ArrayList<Comment>()

	fun parseComments(json: String, header: Header) {
		val jp = jsonFactory.createParser(json)
		try {
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
							"body" -> comment.body = jp.nextTextValue().replace("\n", "")
							"depth" -> comment.depth = jp.nextIntValue(0)
						}
					}
					comments.add(comment)
					//println("${comment.depth} - ${comment.author}) ${comment.body}")
				}

//				if ("author" == jp.currentName) {
//					val comment = Comment()
//					comment.author = jp.nextTextValue()
//
//					while (jp.nextToken() != JsonToken.END_OBJECT) {
//						val key = jp.currentName
//						when (key) {
//							"replies" -> parseReplies(jp)
//							"score" -> comment.score = jp.nextIntValue(0)
//							"body" -> {
//								comment.body = jp.nextTextValue()
//								println(comment.body)
//							}
//							"name" -> comment.name = jp.nextTextValue().replace("t1_", "")
//							"created_utc" -> {
//								jp.nextToken()
//								comment.created_utc = jp.valueAsLong
//							}
//						}
//					}
//
//				}

			}
		} catch (ex: Exception) {
			println(ex.message)
		}

//		comments.sortedBy { it.depth }.forEach {
//			println("${it.depth} - ${it.author}) ${it.body}")
//		}


		println("num comments: ${comments.size}")
	}

	fun parseReplies(jp: JsonParser) {

	}

	fun parsePosts(url: String): Deferred<ArrayList<Post>> {
		return async(CommonPool) {
			val json = get(url)
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
						//"permalink" -> post.permalink = jp.nextTextValue()
						}
					}

					posts.add(post)
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

	fun get(url: String): String {
		val timeout = 3L
		val useragent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"
		val client = OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS).readTimeout(timeout, TimeUnit.SECONDS).build()
		try {
			return client.newCall(Request.Builder().url(url).removeHeader("User-Agent").addHeader("User-Agent", useragent).build()).execute().body()!!.string()
		} catch (ex: Exception) {
			println(ex.message)
		}
		return ""
	}
}