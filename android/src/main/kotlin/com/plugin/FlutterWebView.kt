package com.plugin

import android.content.Context
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import com.just.agentweb.AgentWeb
import com.just.agentweb.WebChromeClient
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.platform.PlatformView

/**
 * @author: 贺宇成
 * @date: 2020/4/1 8:51 PM
 * @desc:
 */
class FlutterWebView(
  context: Context?,
  messenger: BinaryMessenger?,
  params: Map<String?, Any?>?
) : PlatformView, MethodCallHandler {

  private val mLinearLayout: LinearLayout = LinearLayout(context)

  private val mChannel = MethodChannel(messenger, "web_view_plugin").apply {
    setMethodCallHandler(this@FlutterWebView)
  }

  private val mWebChromeClient: WebChromeClient = object : WebChromeClient() {

    override fun onReceivedTitle(view: WebView?, title: String?) {
      super.onReceivedTitle(view, title)
      mChannel.invokeMethod("onReceivedTitle", title)
    }

  }

  private val mWebView: AgentWeb = AgentWeb.with(MusicPlugin.activity!!)
    .setAgentWebParent(mLinearLayout, LinearLayout.LayoutParams(-1, -1))
    .useDefaultIndicator()
    .setWebChromeClient(mWebChromeClient)
    .createAgentWeb()
    .ready()
    .go(params?.get("url") as String)

  override fun getView(): View {
    return mLinearLayout
  }

  override fun dispose() {
    mWebView.destroy()
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "back" -> result.success(mWebView.back())
      "reload" -> result.success(mWebView.urlLoader.reload())
    }
    result.notImplemented()
  }

}