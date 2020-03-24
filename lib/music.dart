import 'dart:async';

import 'package:flutter/services.dart';

class Music {
  static const MethodChannel _channel = const MethodChannel('music');

  static Future<String> playSong(String songId, String songUrl) async {
    final String version = await _channel
        .invokeMethod('playSong', {'songId': songId, 'songUrl': songUrl});
    return version;
  }
}
