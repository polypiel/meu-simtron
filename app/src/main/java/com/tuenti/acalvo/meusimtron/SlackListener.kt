package com.tuenti.acalvo.meusimtron

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.schedule

class SlackListener(private val slackInfo: SlackInfo): WebSocketListener() {
    var listening: Boolean = false

    override fun onOpen(webSocket: WebSocket?, response: Response?) {
        Log.i(LOG_TAG, "onOpen")
        listening = true
        ping(webSocket)
    }

    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
        Log.i(LOG_TAG, "onFailure: ${t?.message}", t)
        closeAndRestart(webSocket)
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        Log.i(LOG_TAG, "onClosing: $code $reason")
        closeAndRestart(webSocket)
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        Log.d(LOG_TAG, "onMessageText: $text")
        handleMessage(text!!).forEach { SlackService.instance.send(slackInfo, it) }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        Log.d(LOG_TAG, "onMessageBytes: ${bytes?.hex()}")
    }

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
        Log.i(LOG_TAG, "onClosed: $code $reason")
    }

    private fun handleMessage(msg: String): List<String> {
        val slackMsg = JSONObject(msg)
        val text = slackMsg["text"].toString().trim()
        return if (slackMsg["type"].toString() == "message"
                && (text == "simtron" || text == "@simtron")
                && slackMsg["channel"].toString() == slackInfo.channel) {
            Directory.instance.getAllSimInfo().asSequence()
                    .filter { it.hasProviderInfo() }
                    .map { it.toSlackStatus() }
                    .toList()
        } else {
            emptyList()
        }
    }

    private fun closeAndRestart(webSocket: WebSocket?) {
        listening = false
        webSocket?.close(1000, null)
        Timer().schedule(15000L) {
            SlackService.instance.rtm(slackInfo)
        }
    }

    private fun ping(webSocket: WebSocket?) {
        Timer().schedule(0L, PING_DELAY) {
            if (listening) {
                Log.d(LOG_TAG, "-> ping")
                webSocket?.send(PING)
            } else {
                cancel()
            }
        }
    }

    companion object {
        const val LOG_TAG = "RTM"
        const val PING_DELAY = 5000L
        const val PING = """{"type":"ping"}"""
    }
}
