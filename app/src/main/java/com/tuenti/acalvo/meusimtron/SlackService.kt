package com.tuenti.acalvo.meusimtron

import android.content.Context
import android.content.res.Resources
import android.util.Log
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.security.AccessControlContext
import java.util.concurrent.TimeUnit

class SlackInfo(val token: String, val channel: String)

class SlackAttachment(private val text: String) {
    override fun toString(): String {
        return "{\"text\":\"$text\",\"color\":\"#d3d3d3\"}"
    }
}

fun List<SlackAttachment>.toJson() =
        if (isEmpty()) ""
        else joinToString(",", "[", "]")

class SlackService private constructor() {
    private object Holder { val INSTANCE = SlackService() }
    private var listener: SlackListener? = null

    fun send(slackInfo: SlackInfo, text: String, attachments: List<SlackAttachment> = emptyList()) {
        val requestBody = FormBody.Builder()
                .add("token", slackInfo.token)
                .add("channel", slackInfo.channel)
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
            Log.d("TestLog", "slack : $text < ${attachments.toJson()}")
            val response = client.newCall(request).execute()
            val body = response.body()!!.string()

            Log.d("TestLog", body)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    fun rtm(slackInfo: SlackInfo) {
        if (listener == null) {
            listener = SlackListener(slackInfo)
        }
        if (listener?.listening ?: false) {
            Log.w("RTM", "Already running")
            return
        }

        val client = OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()

        val authRequestBody = FormBody.Builder()
                .add("token", slackInfo.token)
                .build()
        val authRequest = Request.Builder()
                .url("https://slack.com/api/rtm.connect")
                .post(authRequestBody)
                .build()
        val httpResponse = client.newCall(authRequest).execute()
        val response = JSONObject(httpResponse.body()!!.string())
        Log.i("RTM", "RTM auth $response")

        if ("true" == response["ok"].toString()) {
            val rtmUrl = response["url"].toString()
            Log.i("RTM", "URL: $rtmUrl")
            val request = Request.Builder()
                    .url(rtmUrl)
                    .build()
            client.newWebSocket(request, listener)
        }
        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown()
    }

    companion object {
        val instance: SlackService by lazy { Holder.INSTANCE }
    }
}