package com.mgstudio.vediodecode;


// base class for video encoder/decoder configuration //用于视频编码器/解码器配置的基类
public interface VideoCodecConstants {


    // video codec//视频编解码器
    String VIDEO_CODEC = "video/avc";

    // frame per seconds//每秒帧数
    int VIDEO_FPS = 30;

    // i frame interval //我的帧间隔
    int VIDEO_FI = 2;

    // video bitrate  // 视频比特率
    int VIDEO_BITRATE = 3000 * 1000;
}
