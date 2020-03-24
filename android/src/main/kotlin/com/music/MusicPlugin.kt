package com.music

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.os.Build.VERSION_CODES
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
open class MusicPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    val channel = MethodChannel(flutterPluginBinding.binaryMessenger, "music")
    channel.setMethodCallHandler(MusicPlugin())
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    protected var activity: Activity? = null
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "music")
      channel.setMethodCallHandler(MusicPlugin())
    }
  }

  @TargetApi(VERSION_CODES.M) fun requestPermission(): Boolean {
    activity ?: return false
    val code =
      activity!!.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) + activity!!.checkSelfPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE
      )
    if (code != 0) {
      activity!!.requestPermissions(
        arrayOf(
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.READ_EXTERNAL_STORAGE
        ), 1
      )
    } else {
      return true
    }
    return false
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    StarrySky.init(activity!!.application)
    if (call.method == "getPlatformVersion") {
      if (requestPermission()) {
        val info = SongInfo()
        info.songId = "111"
        info.songUrl =
          "https://webfs.yun.kugou.com/202003241405/b115cd03e8cc3e479f7b5a2158546f1a/G164/M01/1F/09/RIcBAF1FXz6AImQhAC0SISFl4Mw962.mp3"
        StarrySky.with().playMusicByInfo(info)
        result.success("New Android 12")
      } else {
        result.success("not Permission")
      }
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
  }

  override fun onDetachedFromActivity() {
  }

  override fun onReattachedToActivityForConfigChanges(p0: ActivityPluginBinding) {
  }

  override fun onAttachedToActivity(p0: ActivityPluginBinding) {
    activity = p0.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
  }
}
