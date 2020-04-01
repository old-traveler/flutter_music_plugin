package com.plugin

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.just.agentweb.AgentWeb
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.platform.PlatformView

/**
 * @author: 贺宇成
 * @date: 2020/4/1 8:51 PM
 * @desc:
 */
class FlutterWebView(
  context: Context?,
  val messenger: BinaryMessenger?,
  id: Int,
  params: Map<String?, Any?>?
) : PlatformView {

  private val mLinearLayout: LinearLayout = LinearLayout(context)

  private val mWebView: AgentWeb = AgentWeb.with(MusicPlugin.activity!!)
    .setAgentWebParent(mLinearLayout, LinearLayout.LayoutParams(-1, -1))
    .useDefaultIndicator()
    .createAgentWeb()
    .ready()
    .go(params?.get("url") as String)

  override fun getView(): View {
    return mLinearLayout
  }

  override fun dispose() {
    mWebView.destroy()
  }

}