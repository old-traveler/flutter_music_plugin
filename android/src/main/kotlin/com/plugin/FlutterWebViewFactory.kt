package com.plugin

import android.content.Context
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

/**
 * @author: 贺宇成
 * @date: 2020/4/1 9:04 PM
 * @desc:
 */
@Suppress("UNCHECKED_CAST")
class FlutterWebViewFactory(private val binaryMessenger: BinaryMessenger) : PlatformViewFactory(
  StandardMessageCodec.INSTANCE
) {
  override fun create(context: Context?, viewId: Int, args: Any?): PlatformView {
    val params =
      args as? Map<String?, Any?>
    return FlutterWebView(context, binaryMessenger, params)
  }
}