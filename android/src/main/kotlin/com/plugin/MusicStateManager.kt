package com.plugin

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.control.OnPlayerEventListener
import com.lzx.starrysky.control.PlayerControl
import com.lzx.starrysky.provider.SongInfo

/**
 * @author: 贺宇成
 * @date: 2020/3/24 5:48 PM
 * @desc:
 */
object MusicStateManager : OnPlayerEventListener {
  private var hasRegister = false
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
    val sky = StarrySky.with()
    if (sky.getPlayingPosition() > 0 && sky.getPlayingPosition() > sky.getRealDuration()) {
      if (sky.isSkipToNextEnabled() && sky.getPlayList().size > 1) {
        sky.skipToNext()
      } else {
        sky.stopMusic()
      }
    }
    sendStateMessage()
    if (hasRegister && StarrySky.with().isPlaying()) {
      startProgress()
    }
  }

  private fun sendStateMessage() {
    val arguments = mutableMapOf<String, Any?>()
    StarrySky.with().apply {
      arguments["state"] = getState()
      arguments["position"] = getPlayingPosition()
      arguments["duration"] = getRealDuration()
      arguments["buffered"] = getBufferedPosition()
      arguments["error"] = getErrorCode()
      arguments["songId"] = getNowPlayingSongInfo()?.songId
    }
    MusicPlugin.mChannel.invokeMethod("onStateChange", arguments)
  }

  private fun PlayerControl.getRealDuration(): Long {
    var duration = getNowPlayingSongInfo()?.duration ?: -1
    if (duration < 0) {
      duration = getDuration()
    }
    return duration
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
    sendStateMessage()
  }

  override fun onError(errorCode: Int, errorMsg: String) {
    sendStateMessage()
  }

  override fun onMusicSwitch(songInfo: SongInfo) {
    sendStateMessage()
  }

  override fun onPlayCompletion(songInfo: SongInfo) {
    sendStateMessage()
  }

  override fun onPlayerPause() {
    sendStateMessage()
  }

  override fun onPlayerStart() {
    sendStateMessage()
    startProgress()
  }

  private fun startProgress() {
    mHandler.sendEmptyMessageDelayed(PROGRESS, 1000)
  }

  override fun onPlayerStop() {
    sendStateMessage()
  }

}