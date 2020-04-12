package com.plugin

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.StarrySkyBuilder
import com.lzx.starrysky.StarrySkyConfig
import com.lzx.starrysky.provider.SongInfo
import com.lzx.starrysky.registry.StarrySkyRegistry
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File

/** MusicPlugin */
@Suppress("UNCHECKED_CAST")
open class MusicPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    mChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "music")
    mChannel.setMethodCallHandler(MusicPlugin())
    flutterPluginBinding.platformViewRegistry.registerViewFactory(
      "flutterWebView",
      FlutterWebViewFactory(flutterPluginBinding.binaryMessenger)
    )
  }

  // flutter 1.12之前会自动调用registerWith，1.12之后走onAttachedToEngine
  companion object {
    var activity: Activity? = null
    lateinit var mChannel: MethodChannel
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "music")
      channel.setMethodCallHandler(MusicPlugin())
      registrar.platformViewRegistry()
        .registerViewFactory("flutterWebView", FlutterWebViewFactory(registrar.messenger()))
    }
  }

  private fun hasPermission(): Boolean {
    val code =
      if (VERSION.SDK_INT >= VERSION_CODES.M) {
        activity!!.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) + activity!!.checkSelfPermission(
          Manifest.permission.READ_EXTERNAL_STORAGE
        )
      } else {
        // VERSION.SDK_INT < M
        0
      }
    return code == 0
  }

  private fun requestPermission(listener: (grant: Boolean) -> Unit): Boolean {
    activity ?: return false
    if (!hasPermission()) {
      if (VERSION.SDK_INT >= VERSION_CODES.M) {
        (activity)?.let {
          var fragment = it.fragmentManager.findFragmentByTag("request")
          fragment ?: let { fr ->
            fragment = PermissionFragment()
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
              it.fragmentManager.beginTransaction().add(fragment!!, "request").commitNow()
            } else {
              it.fragmentManager.beginTransaction().add(fragment!!, "request").commit()
            }
          }
          (fragment as? PermissionFragment)?.requestStoragePermission(listener)
        }
      }
    } else {
      return true
    }
    return false
  }

  private fun playSong(map: Map<String, Any?>) {
    val info = SongInfo()
    info.songId = map["songId"] as String? ?: ""
    info.songUrl = map["songUrl"] as String? ?: ""
    info.duration = (map["duration"] as Int? ?: -1).toLong()
    val sky = StarrySky.with()
    if (!sky.isCurrMusicIsPlayingMusic(info.songId)) {
      sky.playMusicByInfo(info)
    } else if (sky.isIdea()) {
      sky.removeSongInfo(info.songId)
      val originId = info.songId
      info.songId = info.songId + "copy"
      sky.playMusicByInfo(info)
      sky.stopMusic()
      sky.removeSongInfo(info.songId)
      info.songId = originId
      sky.playMusicByInfo(info)
    }
    Log.d("playSong", "${info.songId} is playing ${sky.isIdea()}")

  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    Log.d("MusicPlugin", "onMethodCall ${call.method}")
    var resultData: Any? = "true"
    when (call.method) {
      "playSong" -> playSong(call.arguments as Map<String, Any?>)
      "registerStateListener" -> registerStateListener()
      "unregisterStateListener" -> unregisterStateListener()
      "seekTo" -> seekTo((call.arguments as Int).toLong())
      "playOrPauseMusic" -> playOrPauseMusic(call.arguments as? String?)
      "playMusicById" -> playMusicById(call.arguments as String)
      "loadMusicList" -> loadMusicList(call.arguments as Map<String, Any>)
      "playNebOrPreviousSong" -> playNebOrPreviousSong(call.arguments as Boolean)
      "getState" -> resultData = StarrySky.with().getState()
      "prepareFromSongId" -> prepareFromSongId(call.arguments as? String?)
      "getPlayListSongId" -> resultData = getPlayListSongId()
      "setPlayMusicMode" -> setPlayMusicMode(call.arguments as Int)
      "removeSongInfoById" -> removeSongInfoById(call.arguments as String?)
      "downloadMusic" -> downloadMusic(call.arguments as? String?)
      "appendMusicList" -> appendMusicList(call.arguments as Map<String, Any>)
      "removeMusicList" -> removeMusicList()
      else -> result.notImplemented()
    }
    result.success(resultData)
  }

  private fun removeMusicList() {
    StarrySky.with()?.apply {
      val keyList = getPlayList().map { it.songId }
      keyList.forEach {
        this.removeSongInfo(it)
      }
    }
  }

  private fun downloadMusic(songName: String?) {
    if (!requestPermission {
        if (it) {
          downloadMusic(songName)
        }
      }) return
    val playUrl = StarrySky.with().getNowPlayingSongInfo()?.songUrl
    val realSongName = "${songName ?: StarrySky.with().getNowPlayingSongInfo()?.songId}.mp3"
    if (playUrl.isNullOrEmpty()) return
    val downloadManager =
      activity?.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    if (downloadManager == null) {
      Toast.makeText(activity, "下载失败，系统不支持", Toast.LENGTH_SHORT).show()
      return
    }
    val path =
      Environment.getExternalStorageDirectory().path + "/music/" + realSongName
    val file = File(path)
    if (file.exists()) {
      file.delete()
    }
    downloadManager
      .enqueue(
        Request(Uri.parse(playUrl))
          .setDestinationInExternalPublicDir("/music/", realSongName)
          .setTitle(realSongName)
          .setNotificationVisibility(
            Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
          )
      )
  }

  private fun removeSongInfoById(songId: String?) {
    songId ?: return
    val sky = StarrySky.with()
    if (sky.isCurrMusicIsPlayingMusic(songId)) {
      sky.skipToNext()
    }
    StarrySky.with().removeSongInfo(songId)
  }

  private fun setPlayMusicMode(mode: Int) {
    return StarrySky.with().setRepeatMode(mode)
  }

  private fun getPlayListSongId(): List<String>? {
    return StarrySky.with().getPlayList().map { it.songId }
  }

  private fun prepareFromSongId(songId: String?) {
    if (songId.isNullOrEmpty()) {
      StarrySky.with().prepare()
    } else {
      StarrySky.with().prepareFromSongId(songId)
    }
  }

  private fun playNebOrPreviousSong(next: Boolean) {
    if (next) {
      StarrySky.with().skipToNext()
    } else {
      StarrySky.with().skipToPrevious()
    }
  }

  private fun playMusicById(songId: String) {
    StarrySky.with().playMusicById(songId)
  }

  private fun loadMusicList(map: Map<String, Any>) {
    val index = map["index"] as Int
    val songList = map["songList"] as List<List<String?>>
    val songInfoList: List<SongInfo> = songList.map {
      SongInfo(songId = it[0]!!, songUrl = it[1] ?: "", duration = it[2]!!.toLong())
    }
    StarrySky.with().playMusic(songInfoList, index)
    val pause = map["pause"] as? Boolean ?: false
    if (pause) {
      Log.d("loadMusicList", "pauseMusic")
      StarrySky.with().pauseMusic()
    }
  }

  private fun appendMusicList(map: Map<String, Any>) {
    val index = map["index"] as Int
    val songList = map["songList"] as List<List<String?>>
    val songInfoList: List<SongInfo> = songList.map {
      SongInfo(songId = it[0]!!, songUrl = it[1] ?: "", duration = it[2]!!.toLong())
    }
    val newPlayList = mutableListOf<SongInfo>()
    newPlayList.addAll(songInfoList)
    newPlayList.addAll(StarrySky.with().getPlayList())
    StarrySky.with().playMusic(newPlayList, index)
  }

  private fun playOrPauseMusic(songId: String?) {
    val sky = StarrySky.with()
    Log.d(
      "MusicPlugin",
      "isPause :${sky.isPaused()}   isPlaying :${sky.isPlaying()}  ${sky.getState()}"
    )
    if (sky.isPaused()) {
      sky.restoreMusic()
    } else if (sky.isPlaying()) {
      sky.pauseMusic()
    } else if (sky.isIdea() && sky.getNowPlayingSongInfo() != null) {
      val info = sky.getNowPlayingSongInfo()!!
      sky.removeSongInfo(info.songId)
      val originId = info.songId
      info.songId = info.songId + "copy"
      sky.playMusicByInfo(info)
      sky.stopMusic()
      sky.removeSongInfo(info.songId)
      info.songId = originId
      sky.playMusicByInfo(info)
    } else if (sky.getState() == PlaybackStateCompat.STATE_STOPPED && sky.getNowPlayingSongInfo() != null) {
      sky.playMusicById(sky.getNowPlayingSongInfo()?.songId!!)
    } else if (sky.isIdea() && !songId.isNullOrEmpty()) {
      playMusicById(songId)
    }
  }

  private fun seekTo(position: Long) {
    val sky = StarrySky.with()
    if (sky.isPlaying() || sky.isPaused()) {
      sky.seekTo(position)
    }
  }

  private fun registerStateListener() {
    if (!requestPermission {
        if (it || hasPermission()) {
          registerStateListener()
        } else {
          Toast.makeText(activity!!, "未给予读写权限,无法正常运行", Toast.LENGTH_SHORT).show()
          activity!!.finish()
        }
      }) {
      return
    }
    StarrySky.init(activity!!.application, object : StarrySkyConfig() {
      override fun applyOptions(context: Context, builder: StarrySkyBuilder) {
        super.applyOptions(context, builder)
        builder.setOpenCache(true)
        val destFileDir = Environment.getExternalStorageDirectory().absolutePath + "/music/cache/"
        builder.setCacheDestFileDir(destFileDir)
        builder.setOpenNotification(true)
        Log.d("MusicPlugin", destFileDir)
      }

      override fun applyStarrySkyRegistry(context: Context, registry: StarrySkyRegistry?) {
        super.applyStarrySkyRegistry(context, registry)
        registry?.appendValidRegistry(RequestSongInfoValid())
      }

    })
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
