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
        Log.i(LOG_TAG, "RTM onOpen")
        listening = true
        ping(webSocket)
    }

    override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
        Log.i(LOG_TAG, "RTM onFailure: ${t?.message}", t)
        closeAndRestart(webSocket)
    }

    override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
        Log.i(LOG_TAG, "RTM onClosing: $code $reason")
        closeAndRestart(webSocket)
    }

    override fun onMessage(webSocket: WebSocket?, text: String?) {
        Log.d(LOG_TAG, "RTM onMessageText: $text")
        val slackMsg = JSONObject(text!!.filterNot { it.isISOControl() })
        handleMessage(slackMsg).forEach {
            Timer().schedule(1000L) {
                SlackManager.INSTANCE.send(slackInfo.token, slackMsg.getStr("channel")!!, it)
            }
        }
    }

    override fun onMessage(webSocket: WebSocket?, bytes: ByteString?) {
        Log.d(LOG_TAG, "RTM onMessageBytes: ${bytes?.hex()}")
    }

    override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
        Log.i(LOG_TAG, "RTM onClosed: $code $reason")
    }

    fun isAlive(): Boolean = listening && (currentTime() - lastPing) < LIFESPAN

    fun close() {
        listening = false
    }

    private fun handleMessage(slackMsg: JSONObject): List<String> =
            when {
                slackMsg.isPong() -> pong()
                slackMsg.isPublicMsg() && slackMsg.isSimtronCmd() -> {
                    val debug = slackMsg.isDebug()
                    Directory.instance.getAllSimInfo().asSequence()
                            .filter { debug || it.hasProviderInfo() }
                            .map { it.toSlack(debug) }
                            .toList()
                }
                else -> emptyList()
            }

    private fun closeAndRestart(webSocket: WebSocket?) {
        listening = false
        webSocket?.close(1000, null)
        Timer().schedule(15000L) {
            SlackManager.INSTANCE.rtm(slackInfo.channel, this@SlackListener)
        }
    }

    private fun ping(webSocket: WebSocket?) {
        Timer().schedule(0L, PING_DELAY) {
            if (listening) {
                webSocket?.send(PING)
            } else {
                webSocket?.send(BYE)
                cancel()
            }
        }
    }

    private fun pong(): List<String> {
        lastPing = currentTime()
        return emptyList()
    }

    companion object {
        const val LOG_TAG = "MEU-SLACK"
        const val PING_DELAY = 5000L
        const val PING = """{"type":"ping"}"""
        const val LIFESPAN = 300
        const val BYE = """{"type":"bye"}"""
    }

    private fun currentTime() = System.currentTimeMillis() / 1000

    private fun JSONObject.getStr(prop: String): String? =
            if (has(prop)) this[prop].toString() else null

    private fun JSONObject.isPong() =
            getStr("type") == "pong"

    private fun JSONObject.isPublicMsg(): Boolean {
        val channel = getStr("channel")
        return getStr("type") == "message"
                && (channel == slackInfo.channel || channel == slackInfo.debugChannel)
    }

    private fun JSONObject.isDebug(): Boolean =
            getStr("type") == "message" && getStr("channel") == slackInfo.debugChannel

    private fun JSONObject.isSimtronCmd(): Boolean {
        val text = getStr("text")?.trim()
        return text?.toLowerCase() == "simtron" || text == "@simtron"
    }


}
