package com.plugin

import android.annotation.SuppressLint
import android.util.Log
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.delayaction.Valid
import com.lzx.starrysky.delayaction.Valid.ValidCallback
import com.lzx.starrysky.provider.SongInfo
import io.flutter.plugin.common.MethodChannel.Result
import java.text.SimpleDateFormat
import java.util.Date

/**
 * @author: 贺宇成
 * @date: 2020/4/7 8:36 PM
 * @desc:
 */
class RequestSongInfoValid : Valid {

  companion object {
    const val baseWebUrl = "https://webfs.yun.kugou.com/"
  }

  override fun doValid(songInfo: SongInfo?, callback: ValidCallback) {
    songInfo ?: return
    if (songInfo.songUrl.isNotEmpty() && validPlayUrl(songInfo.songUrl)) {
      callback.finishValid()
    } else {
      MusicPlugin.mChannel.invokeMethod("updatePlayingUrl", songInfo.songId, object : Result {
        override fun notImplemented() {
          throw RuntimeException("notImplemented")
        }

        override fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?) {

        }

        override fun success(result: Any?) {
          if (result is String) {
            result.split("@").let {
              val url = it[0]
              val duration = it[1]
              val sizableCover = it[2]
              songInfo.songUrl = url
              songInfo.duration = duration.toLong()
              songInfo.songCover = sizableCover
              callback.finishValid()
            }

          }
        }

      })
    }
  }

  @SuppressLint("SimpleDateFormat")
  private fun validPlayUrl(playUrl: String): Boolean {
    if (playUrl.startsWith(baseWebUrl)) {
      val time = playUrl.substring(baseWebUrl.length, baseWebUrl.length + 12).toLong()
      val curTime = SimpleDateFormat("yyyyMMddHHmm").format(Date()).toLong()
      Log.d("validPlayUrl", "time：$time curTime：$curTime")
      if (curTime - time > 10000) {
        // 链接失效
        Log.d("validPlayUrl", "链接失效")
        return false
      }
    }
    return true
  }

}