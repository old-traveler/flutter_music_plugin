package com.plugin

import android.Manifest
import android.app.Fragment
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES

/**
 * @author: 贺宇成
 * @date: 2020/4/6 9:27 PM
 * @desc:
 */
class PermissionFragment : Fragment() {

  var requestListener: ((grant: Boolean) -> Unit)? = null

  fun requestStoragePermission(listener: (grant: Boolean) -> Unit) {
    this.requestListener = listener
    if (VERSION.SDK_INT >= VERSION_CODES.M) {
      requestPermissions(
        arrayOf(
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.READ_EXTERNAL_STORAGE
        ), 1
      )
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == 1 && grantResults.sum() == 0) {
      requestListener?.invoke(true)
    }
    requestListener?.invoke(false)
  }

}