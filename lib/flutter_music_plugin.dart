import 'dart:async';
import 'dart:math';

import 'package:flutter/services.dart';

typedef MusicStateListener = void Function(MusicState);
typedef MusicPlayUrlProvider = Future<String> Function(String songId);

class MusicState {
  int state;
  int position;
  int duration;
  int buffered;
  int error;
  String songId;

  MusicState(
      {this.state, this.position, this.duration, this.buffered, this.error});

  MusicState.formMap(dynamic map) {
    this.state = map['state'];
    this.position = map['position'];
    this.duration = map['duration'];
    this.buffered = map['buffered'];
    this.error = map['error'];
    this.songId = map['songId'];
    this.duration = max(0, duration);
    assert(duration >= 0);
    this.position = max(0, min(position, duration));
    assert(position >= 0 && position <= duration);
    this.buffered = max(0, min(buffered, duration));
    assert(buffered >= 0 && buffered <= duration);
  }
}

class MusicStateType {
  /// 默认播放状态，表示尚未添加媒体，或者表示已重置且无内容可播放
  static const int STATE_NONE = 0;

  /// 当前已停止
  static const int STATE_STOPPED = 1;

  /// 已暂停
  static const int STATE_PAUSED = 2;

  /// 正在播放
  static const int STATE_PLAYING = 3;

  /// 当前正在快进
  static const int STATE_FAST_FORWARDING = 4;

  /// 当前正在倒带
  static const int STATE_REWINDING = 5;

  /// 当前正在缓冲
  static const int STATE_BUFFERING = 6;

  /// 当前处于错误状态
  static const int STATE_ERROR = 7;

  /// 正在连接中
  static const int STATE_CONNECTING = 8;

  /// 正在转跳到上一首
  static const int STATE_SKIPPING_TO_PREVIOUS = 9;

  /// 正在转跳到下一首
  static const int STATE_SKIPPING_TO_NEXT = 10;

  /// 正在切歌
  static const int STATE_SKIPPING_TO_QUEUE_ITEM = 11;

  static const int PLAYBACK_POSITION_UNKNOWN = -1;
}

class MusicPlayMode {
  /// 顺序播放
  static const int REPEAT_MODE_NONE = 0;

  /// 单曲循环
  static const int REPEAT_MODE_ONE = 1;

  /// 列表循环
  static const int REPEAT_MODE_ALL = 2;
}

extension MusicStateProvider on int {
  canSeekTo() {
    return this == MusicStateType.STATE_PLAYING ||
        this == MusicStateType.STATE_PAUSED;
  }
}

class SongInfo {
  String songId;
  String songUrl;
  int duration;
  String singerName;
  String songName;
  String imgUrl;

  SongInfo(this.songId, this.songUrl,
      {this.duration = -1, this.singerName, this.songName, this.imgUrl});
}

class MusicWrapper {
  static MusicWrapper _wrapper;
  List<MusicStateListener> stateListeners = [];
  StreamController<MusicState> _controller;
  MusicPlayUrlProvider playUrlProvider;

  static MusicWrapper get singleton => _wrapper ??= MusicWrapper();

  MethodChannel _channel = const MethodChannel('music')
    ..setMethodCallHandler((methodCall) async {
      if ('onStateChange' == methodCall.method) {
        _wrapper._controller.add(MusicState.formMap(methodCall.arguments));
      } else if ('updatePlayingUrl' == methodCall.method) {
        if (_wrapper.playUrlProvider == null) {
          throw Exception('playUrlProvider must not be null');
        }
        return await _wrapper.playUrlProvider(methodCall.arguments);
      }
      return Future.value(true);
    });


  List<String> infoToList(SongInfo info) {
    return [
      info.songId,
      info.songUrl,
      info.duration.toString(),
      info.singerName,
      info.songName,
      info.imgUrl,
    ];
  }

  Future<String> playSongByInfo(SongInfo info) async {
    return await _channel.invokeMethod('playSong', infoToList(info));
  }

  void _registerStateListener() async {
    String result = await _channel.invokeMethod('registerStateListener');
    print(result);
  }

  void _unregisterStateListener() async {
    String result = await _channel.invokeMethod('unregisterStateListener');
    print(result);
  }

  void seekTo(int position) async {
    _channel.invokeMethod('seekTo', position);
  }

  void playOrPauseMusic({String songId}) {
    _channel.invokeMethod('playOrPauseMusic', songId);
  }

  void setPlayMusicMode(int mode) {
    _channel.invokeMethod('setPlayMusicMode', mode);
  }

  void loadMusicList({List<SongInfo> list, int index = 0, bool pause = false}) {
    final dataList = list.map<List<String>>((info) {
      return infoToList(info);
    }).toList();
    Map map = {'index': index, 'songList': dataList, 'pause': pause};
    _channel.invokeMethod('loadMusicList', map);
  }

  void appendMusicList({List<SongInfo> list, int index = 0}) {
    final dataList = list.map<List<String>>((info) {
      return infoToList(info);
    }).toList();
    Map map = {'index': index, 'songList': dataList};
    _channel.invokeMethod('appendMusicList', map);
  }

  Future removeMusicList() async {
    return await _channel.invokeMethod('removeMusicList');
  }

  void playMusicById(String songId) {
    _channel.invokeMethod('playMusicById', songId);
  }

  void playNextSong() {
    _channel.invokeMethod('playNebOrPreviousSong', true);
  }

  void playPreviousSong() {
    _channel.invokeMethod('playNebOrPreviousSong', false);
  }

  void prepareFromSongId({String songId}) {
    _channel.invokeMethod('prepareFromSongId', songId);
  }

  Future<int> getState() async {
    return await _channel.invokeMethod<int>('getState');
  }

  Future removeSongInfoById(String songId) async {
    return _channel.invokeMethod('removeSongInfoById', songId);
  }

  void downloadMusic(String songName) {
    _channel.invokeMethod('downloadMusic', songName);
  }

  void initState() {
    _controller?.close();
    _controller ??= StreamController.broadcast();
    _registerStateListener();
  }

  void dispose() {
    _unregisterStateListener();
    _controller?.close();
    _controller = null;
  }

  Future<List<dynamic>> getPlayListSongId() {
    return _channel.invokeMethod<List<dynamic>>('getPlayListSongId');
  }

  Stream<MusicState> getMusicStateStream() => _controller?.stream;
}
