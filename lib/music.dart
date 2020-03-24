import 'dart:async';

import 'package:flutter/services.dart';

typedef MusicStateListener = void Function(MusicState);

class MusicState {
  String type;
  dynamic data;

  MusicState({this.data, this.type});
}

class MusicStateType {
  static String progress = "progress";
}

class MusicWrapper {
  static MusicWrapper _wrapper;
  List<MusicStateListener> stateListeners = [];
  StreamController<MusicState> _controller;

  static MusicWrapper get singleton => _wrapper ??= MusicWrapper();

  MethodChannel _channel = const MethodChannel('music')
    ..setMethodCallHandler((methodCall) {
      if ('onStateChange' == methodCall.method) {
        final map = methodCall.arguments;
        _wrapper._controller
            .add(MusicState(type: map['type'], data: map['data']));
      }
      return Future.value(true);
    });

  Future<String> playSong(String songId, String songUrl) async {
    final String version = await _channel
        .invokeMethod('playSong', {'songId': songId, 'songUrl': songUrl});
    return version;
  }

  void _registerStateListener() async {
    String result = await _channel.invokeMethod("registerStateListener");
    print(result);
  }

  void _unregisterStateListener() async {
    String result = await _channel.invokeMethod("unregisterStateListener");
    print(result);
  }

  void seekTo(int position) async {
    _channel.invokeMethod('seekTo',position);
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

  Stream<MusicState> getMusicStateStream() => _controller?.stream;
}
