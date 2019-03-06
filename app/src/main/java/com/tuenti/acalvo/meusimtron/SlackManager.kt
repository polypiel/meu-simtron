package com.tuenti.acalvo.meusimtron

import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SlackInfo(val token: String, val channel: String, val debugChannel: String)

// TODO factory
class SlackAttachment(private val text: String? = null, private val icon: String? = null, private val fields: List<Pair<String, String>>? = null) {
    override fun toString(): String {
        val fieldsJson = fields?.joinToString(",", "[", "]") { "{\"title\":\"${it.first}\",\"value\":\"${it.second}\",\"short\":true}" }
        val attrs = mapOf(
                "text" to text,
                "author_name" to icon,
                "color" to "#d3d3d3",
                "fields" to fieldsJson
        )
        val str = attrs.entries.filter { it.value != null }.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" }
        Log.i("DEBUG", str)
        return str
    }
}

fun List<SlackAttachment>.toJson() =
        if (isEmpty()) ""
        else joinToString(",", "[", "]")

class SlackManager private constructor() {
    private object Holder { val INSTANCE = SlackManager() }

    fun send(token: String, channel: String, text: String, attachments: List<SlackAttachment> = emptyList()) {
        val requestBody = FormBody.Builder()
                .add("token", token)
                .add("channel", channel)
                .add("text", text)
                .add("attachments", attachments.toJson())
                .add("as_user", "true")
                .build()

        val request = Request.Builder()
                .url("https://slack.com/api/chat.postMessage")
                .post(requestBody)
                .build()

        val client = OkHttpClient()

        try {
            Log.d(LOG_TAG, "Slack: $text < ${attachments.toJson()}")
            val response = client.newCall(request).execute()
            response.body()!!.string()

        } catch (ex: IOException) {
            Log.e(LOG_TAG, "Error sending Slack message", ex)
        }
    }

    fun rtm(token: String, listener: SlackListener) {
        Log.i(LOG_TAG, "Starting Slack RTM...")
        val client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()

        val authRequestBody = FormBody.Builder()
                .add("token", token)
                .build()
        val authRequest = Request.Builder()
                .url("https://slack.com/api/rtm.connect")
                .post(authRequestBody)
                .build()
        val httpResponse = client.newCall(authRequest).execute()
        val response = JSONObject(httpResponse.body()!!.string())
        Log.i(LOG_TAG, "RTM auth $response")

        if ("true" == response["ok"].toString()) {
            val rtmUrl = response["url"].toString()
            val request = Request.Builder()
                    .url(rtmUrl)
                    .build()
            client.newWebSocket(request, listener)
        }
        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown()
    }

    companion object {
        val INSTANCE: SlackManager by lazy { Holder.INSTANCE }
        const val LOG_TAG = "MEU-SLACK"
    }
}