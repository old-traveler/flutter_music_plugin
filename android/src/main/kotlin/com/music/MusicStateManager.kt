package com.music

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.control.OnPlayerEventListener
import com.lzx.starrysky.provider.SongInfo

/**
 * @author: 贺宇成
 * @date: 2020/3/24 5:48 PM
 * @desc:
 */
object MusicStateManager : OnPlayerEventListener {
  var hasRegister = false
  const val PROGRESS: Int = 1
  private val mHandler: Handler = @SuppressLint("HandlerLeak")
  object : Handler() {
    override fun handleMessage(msg: Message?) {
      super.handleMessage(msg)
      when (msg?.what) {
        PROGRESS -> onProgressChange()
      }
    }
  }

  fun onProgressChange() {
    val arguments = mutableMapOf<String, Any>()
    arguments["type"] = "progress"
    val map = mutableMapOf<String, Any>()
    map["position"] = StarrySky.with().getPlayingPosition()
    map["duration"] = StarrySky.with().getDuration()
    map["buffered"] = StarrySky.with().getBufferedPosition()
    arguments["data"] = map
    MusicPlugin.mChannel.invokeMethod("onStateChange", arguments)
    if (hasRegister && StarrySky.with().isPlaying()) {
      startProgress()
    }
  }

  fun register() {
    hasRegister = true
    StarrySky.with().addPlayerEventListener(this)
  }

  fun unregister() {
    hasRegister = false
    mHandler.removeMessages(PROGRESS)
    StarrySky.with().removePlayerEventListener(this)
  }

  override fun onBuffering() {
  }

  override fun onError(errorCode: Int, errorMsg: String) {
  }

  override fun onMusicSwitch(songInfo: SongInfo) {
  }

  override fun onPlayCompletion(songInfo: SongInfo) {
  }

  override fun onPlayerPause() {
  }

  override fun onPlayerStart() {
    startProgress()
  }

  private fun startProgress() {
    mHandler.sendEmptyMessageDelayed(PROGRESS, 1000)
  }

  override fun onPlayerStop() {
  }

}