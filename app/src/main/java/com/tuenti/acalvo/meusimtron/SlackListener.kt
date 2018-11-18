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
    private var listening: Boolean = false
    private var lastPing: Long = 0

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
        val slackMsg = JSONObject(text!!)
        handleMessage(slackMsg).forEach {
            Timer().schedule(1000L) {
                SlackService.instance.send(slackInfo.token, slackMsg["channel"].toString(), it)
            }
        }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        Log.d(LOG_TAG, "onMessageBytes: ${bytes?.hex()}")
    }

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
        Log.i(LOG_TAG, "onClosed: $code $reason")
    }

    fun isAlive() = listening && (currentTime() - lastPing) < ALIVENESS

    private fun handleMessage(slackMsg: JSONObject): List<String> =
            when {
                slackMsg.isPong() -> pong()
                slackMsg.isPublicMsg() && slackMsg.isSimtronCmd() -> {
                    Directory.instance.getAllSimInfo().asSequence()
                            .filter { it.hasProviderInfo() }
                            .map { it.toSlack() }
                            .toList()
                }
                else -> emptyList()
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

    private fun pong(): List<String> {
        lastPing = currentTime()
        return emptyList()
    }

    companion object {
        const val LOG_TAG = "RTM"
        const val PING_DELAY = 5000L
        const val PING = """{"type":"ping"}"""
        const val ALIVENESS = 300
    }

    private fun currentTime() = System.currentTimeMillis() / 1000

    fun JSONObject.isPong() =
            this["type"].toString() == "pong"

    fun JSONObject.isPublicMsg(): Boolean {
        val channel = this["channel"].toString()
        return this["type"].toString() == "message"
                && (channel == slackInfo.channel || channel == slackInfo.debugChannel)
    }

    fun JSONObject.isSimtronCmd(): Boolean {
        val text = this["text"].toString().trim()
        return text.toLowerCase() == "simtron" || text == "@simtron"
    }
}
