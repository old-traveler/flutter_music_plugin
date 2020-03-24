package com.music.util;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.util.Log;

import com.music.bean.Song;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

/**
 * @author: 贺宇成
 * @date: 2020/3/24 12:03 PM
 * @desc:
 */
public class MusicUtils {
  /**
   * 播放模式
   */
  private int pattern;
  /**
   * 顺序播放
   */
  public static final int TYPE_ORDER = 4212;
  /**
   * 随机播放
   */
  public static final int TYPE_RANDOM = 4313;
  /**
   * 单曲循环
   */
  public static final int TYPE_SINGLE = 4414;
  /**
   * 播放器
   */
  private MediaPlayer mediaPlayer;
  /**
   * 音乐工具类
   */
  private static MusicUtils musicUtils;
  /**
   * 当前播放的歌曲数据集合
   */
  public List<Song> songs;
  /**
   * 原始的歌曲数据集合（与上者内容相同，但是排列的顺序不同，为了实现多种播放类型而设）
   */
  private List<Song> initialSongs;
  /**
   * 当前播放歌曲的下标
   */
  private int currentSongPosition;
  /**
   * 播放监听
   */
  private OnPlayListener onPlayListener;
  /**
   * 判断播放器是否处于准备阶段，如果处于准备阶段，就不会做其他操作
   * 这样的目的是为了防止快速点击时，播放器转化不及时而奔溃
   */
  private boolean isPrepare = false;

  public void setOnPlayListener(OnPlayListener onPlayListener) {
    this.onPlayListener = onPlayListener;
  }


  public void setPattern(int pattern) {
    //如果是顺序播放并且和之前设置的一样，就不进行播放模式的转化，节省一些操作
    if (this.pattern == pattern && pattern == TYPE_ORDER) {
      return;
    }
    this.pattern = pattern;
    changeSongsByPattern();
  }

  /**
   * 播放监听
   */
  public interface OnPlayListener {
    void OnPlaying(int duration);
  }

  public static synchronized MusicUtils getIntance() {
    if (musicUtils == null) {
      musicUtils = new MusicUtils();
    }
    return musicUtils;
  }

  private boolean hasInit = false;

  /**
   * 初始化
   */
  public void init(Context context) {
    if (hasInit) return;
    hasInit = true;
    songs = getMusicData(context);
  }

  public String getSongListJson(){
    return String.valueOf(songs.size());
  }

  /**
   * 扫描系统里面的音频文件，返回一个list集合
   */
  public List<Song> getMusicData(Context context) {
    List<Song> list = new ArrayList<Song>();
    // 媒体库查询语句（写一个工具类MusicUtils）
    Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null,
        null, MediaStore.Audio.AudioColumns.IS_MUSIC);
    int position = 0;
    if (cursor != null) {
      while (cursor.moveToNext()) {
        Song song = new Song();
        song.setSong(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)));
        song.setSinger(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
        song.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
        song.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
        song.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)));
        if (song.getSong().endsWith("mp3")){
          Log.d("MusicUtil",song.getSong());
        }
        if (song.getSize() > 1000 * 800) {
          // 注释部分是切割标题，分离出歌曲名和歌手 （本地媒体库读取的歌曲信息不规范）
          if (song.getSong().contains("-")) {
            String[] str = song.getSong().split("-");
            song.setSinger(str[0]);
            String name = str[1].toString().split(".mp3")[0].indexOf("[mqms2]") > 0
                ? str[1].toString().split(".mp3")[0].substring(0, str[1].toString().split(".mp3")[0].indexOf("[mqms2]")) : str[1].toString().split(".mp3")[0];
            song.setSong(name);

          }
          //给歌曲赋上一个位置下标，方便播放模式时的操作
          song.setPosition(position);
          position++;
          list.add(song);
        }
      }
      // 释放资源
      cursor.close();
    }

    if (initialSongs == null) {
      //初始化原始播放循序的数据集（也就是顺序播放时的排列方式）
      initialSongs = list;
    }
    return list;
  }

  /**
   * 定义一个方法用来格式化获取到的时间
   */
  public static String formatTime(int time) {
    if (time / 1000 % 60 < 10) {
      return time / 1000 / 60 + ":0" + time / 1000 % 60;

    } else {
      return time / 1000 / 60 + ":" + time / 1000 % 60;
    }

  }

  /**
   * 播放音乐
   *
   * @param song
   */
  public void playMusic(Song song) {
    if (isPrepare) {
      return;
    }
    if (mediaPlayer == null)
      mediaPlayer = new MediaPlayer();
    setPlayCompletionListener();
    if (mediaPlayer.isPlaying()) {
      mediaPlayer.stop();
    }
    try {
      mediaPlayer.reset();
      mediaPlayer.setDataSource(song.getPath());
      isPrepare = true;
      mediaPlayer.prepare();
    } catch (IOException e) {
      e.printStackTrace();
    }
    mediaPlayer.start();
    isPrepare = false;
    onPlayListener.OnPlaying(song.getDuration());
    currentSongPosition = song.getPosition();
  }

  /**
   * 暂停和播放音乐
   */
  public void playOrPause() {
    if (mediaPlayer == null) {
      playMusic(songs.get(0));
      currentSongPosition = 0;
      return;
    }
    if (mediaPlayer.isPlaying()) {
      mediaPlayer.pause();
    } else {
      mediaPlayer.start();
    }
  }

  /**
   * 播放上一曲
   */
  public void playPrevious() {
    if (pattern == TYPE_SINGLE) {
      playMusic(songs.get(currentSongPosition));
      return;
    }
    Log.d("TAG", "currentPosition" + currentSongPosition);
    if (mediaPlayer == null) {
      playMusic(songs.get(0));
      currentSongPosition = 0;
      return;
    }
    if (currentSongPosition == 0) {
      currentSongPosition = songs.size() - 1;
      Log.d("TAG", "1111currentPosition" + currentSongPosition);
      playMusic(songs.get(currentSongPosition));
      return;
    }
    currentSongPosition--;
    playMusic(songs.get(currentSongPosition));

  }

  /**
   * 播放下一曲
   */
  public void playNext() {
    if (pattern == TYPE_SINGLE) {
      playMusic(songs.get(currentSongPosition));
      return;
    }
    Log.d("TAG", "currentPosition" + currentSongPosition);
    if (mediaPlayer == null) {
      playMusic(songs.get(0));
      currentSongPosition = 0;
      return;
    }
    if (currentSongPosition == songs.size() - 1) {
      currentSongPosition = 0;
      playMusic(songs.get(currentSongPosition));
      return;
    }
    currentSongPosition++;
    playMusic(songs.get(currentSongPosition));

  }
  //设置播放完成后的监听，让其按照当前播放模式进行下一曲的切换
  public void setPlayCompletionListener() {
    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        playNext();
      }
    });
  }

  /**
   * 清理资源
   */
  public void clean() {
    mediaPlayer.stop();
    mediaPlayer.release();
  }

  /**
   * 根据播放模式改变songs
   */
  public void changeSongsByPattern() {
    switch (pattern) {
      case TYPE_ORDER:
        songs = initialSongs;
        break;
      case TYPE_RANDOM:
        Collections.shuffle(songs);
        break;
    }
    for (Song s : songs) {
      Log.d("TAG", "song.position is " + s.getPosition());
    }
  }

}