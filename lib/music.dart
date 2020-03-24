import 'dart:async';

import 'package:flutter/services.dart';

class MusicWrapper {
  static MusicWrapper _wrapper;

  MethodChannel _channel = const MethodChannel('music');

  static MusicWrapper get singleton => _wrapper ??= MusicWrapper();

  Future<String> playSong(String songId, String songUrl) async {
    final String version = await _channel
        .invokeMethod('playSong', {'songId': songId, 'songUrl': songUrl});
    return version;
  }
}
