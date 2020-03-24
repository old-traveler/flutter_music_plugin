package com.music

import android.Manifest
import android.app.Activity
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.Log
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.provider.SongInfo
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar

/** MusicPlugin */
@Suppress("UNCHECKED_CAST")
open class MusicPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    mChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "music")
    mChannel.setMethodCallHandler(MusicPlugin())
    Log.d("MusicPlugin", "onAttachedToEngine")
  }

  // flutter 1.12之前会自动调用registerWith，1.12之后走onAttachedToEngine
  companion object {
    protected var activity: Activity? = null
    lateinit var mChannel: MethodChannel
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "music")
      channel.setMethodCallHandler(MusicPlugin())
    }
  }

  private fun requestPermission(): Boolean {
    activity ?: return false
    val code =
      if (VERSION.SDK_INT >= VERSION_CODES.M) {
        activity!!.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) + activity!!.checkSelfPermission(
          Manifest.permission.READ_EXTERNAL_STORAGE
        )
      } else {
        // VERSION.SDK_INT < M
        0
      }
    if (code != 0) {
      if (VERSION.SDK_INT >= VERSION_CODES.M) {
        activity!!.requestPermissions(
          arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
          ), 1
        )
      }
    } else {
      return true
    }
    return false
  }

  private fun playSong(map: Map<String, String>) {
    val info = SongInfo()
    info.songId = map["songId"] ?: ""
    info.songUrl = map["songUrl"] ?: ""
    val sky = StarrySky.with()
    if (!sky.isCurrMusicIsPlayingMusic(info.songId)){
      sky.playMusicByInfo(info)
    } else if (sky.isIdea()){
      sky.removeSongInfo(info.songId)
      val originId = info.songId
      info.songId = info.songId + "copy"
      sky.playMusicByInfo(info)
      sky.stopMusic()
      sky.removeSongInfo(info.songId)
      info.songId = originId
      sky.playMusicByInfo(info)
    }
    Log.d("playSong","${info.songId} is playing ${sky.isIdea()}")

  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    Log.d("MusicPlugin", "onMethodCall ${call.method}")
    when (call.method) {
      "playSong" -> playSong(call.arguments as Map<String, String>)
      "registerStateListener" -> registerStateListener()
      "unregisterStateListener" -> unregisterStateListener()
      "seekTo" -> seekTo((call.arguments as Int).toLong())
      else -> result.notImplemented()
    }
    result.success("true")
  }

  private fun seekTo(position: Long) {
    val sky = StarrySky.with()
    if (sky.isPlaying()) {
      sky.seekTo(position)
    }
  }

  private fun registerStateListener() {
    StarrySky.init(activity!!.application)
    MusicStateManager.register()
  }

  private fun unregisterStateListener() {
    MusicStateManager.unregister()
    StarrySky.with().stopMusic()
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Log.d("MusicPlugin", "onDetachedFromEngine")
  }

  override fun onDetachedFromActivity() {
    Log.d("MusicPlugin", "onDetachedFromActivity")
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(p0: ActivityPluginBinding) {
  }

  override fun onAttachedToActivity(p0: ActivityPluginBinding) {
    Log.d("MusicPlugin", "onAttachedToActivity ${p0.activity.localClassName}")
    activity = p0.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
  }
}
