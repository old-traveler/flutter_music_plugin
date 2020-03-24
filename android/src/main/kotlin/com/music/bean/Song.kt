package com.music.bean

/**
 * @author: 贺宇成
 * @date: 2020/3/24 11:59 AM
 * @desc:
 */
class Song {
  /**
   * 歌手
   */
  var singer: String? = null
  /**
   * 歌曲名
   */
  var song: String? = null
  /**
   * 歌曲的地址
   */
  var path: String? = null
  /**
   * 歌曲长度
   */
  var duration = 0
  /**
   * 歌曲的大小
   */
  var size: Long = 0
  /**
   * 歌曲在集合中的position
   */
  var position = 0

}