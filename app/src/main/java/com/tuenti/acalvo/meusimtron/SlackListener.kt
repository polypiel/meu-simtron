package com.tuenti.acalvo.meusimtron

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

class SlackListener(private val slackInfo: SlackInfo): WebSocketListener() {
    var listening: Boolean = false

    override fun onOpen(webSocket: WebSocket?, response: Response?) {
        Log.i(LOG_TAG, "onOpen")
        listening = true
        webSocket?.send("ping")
    }

    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
        Log.i(LOG_TAG, "onFailure: ${t?.message}")
        closeAndRestart(webSocket)
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        Log.i(LOG_TAG, "onClosing: $code $reason")
        closeAndRestart(webSocket)
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        Log.i(LOG_TAG, "onMessageText: $text")
        handleMessage(text!!).forEach { SlackService.instance.send(slackInfo, it) }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        Log.i(LOG_TAG, "onMessageBytes: ${bytes?.hex()}")
    }

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
        Log.i(LOG_TAG, "onClosed: $code $reason")
    }

    private fun handleMessage(msg: String): List<String> {
        val slackMsg = JSONObject(msg)
        return if (slackMsg["type"].toString() == "message"
                && slackMsg["text"].toString().contains("simtron")
                && slackMsg["channel"].toString() == slackInfo.channel) {
            Directory.instance.getAllSimInfo().map { it.toSlackStatus() }
        } else {
            emptyList()
        }
    }

    private fun closeAndRestart(webSocket: WebSocket?) {
        listening = false
        webSocket?.close(1000, null)
        SlackService.instance.rtm(slackInfo)
    }

    companion object {
        const val LOG_TAG = "RTM"
    }
}
