package com.mgstudio.vediodecode.model;
//视频包

import java.nio.ByteBuffer;

public class VideoPacket extends MediaPacket {

    public enum Flag {

        FRAME((byte) 0), KEY_FRAME((byte) 1), CONFIG((byte) 2), END((byte) 4);

        private byte type;

        Flag(byte type) {
            this.type = type;
        }

        public byte getFlag() {
            return type;
        }

        public static Flag getFlag(byte value) {
            for (Flag type : Flag.values()) {
                if (type.getFlag() == value) {
                    return type;
                }
            }

            return null;
        }
    }

    public Flag flag;

    public long presentationTimeStamp;

    public byte[] data;

    public VideoPacket() {
    }

    public VideoPacket(Type type, Flag flag, long presentationTimeStamp, byte[] data) {
        this.type = type;
        this.flag = flag;
        this.presentationTimeStamp = presentationTimeStamp;
        this.data = data;
    }

    public byte[] toByteArray() {
        return toArray(type, flag, presentationTimeStamp, data);
    }

    // create packet from byte array
    public static VideoPacket fromArray(byte[] values) {
        VideoPacket videoPacket = new VideoPacket();

        // should be a type value - 1 byte
        //应该是一个类型值 -  1个字节
        byte typeValue = values[0];
        // should be a flag value - 1 byte
        //应该是一个标志值 -  1个字节
        byte flagValue = values[1];

        videoPacket.type = Type.getType(typeValue);
        videoPacket.flag = Flag.getFlag(flagValue);

        // should be 8 bytes for timestamp//应该是8个字节的时间戳
        byte[] timeStamp = new byte[8];
        System.arraycopy(values, 2, timeStamp, 0, 8);
        videoPacket.presentationTimeStamp = ByteUtils.bytesToLong(timeStamp);

        // all other bytes is data//所有其他字节都是数据
        int dataLength = values.length - 10;
        byte[] data = new byte[dataLength];
        System.arraycopy(values, 10, data, 0, dataLength);
        videoPacket.data = data;

        return videoPacket;
    }

    // create byte array//创建字节数组
    public static byte[] toArray(Type type, Flag flag, long presentationTimeStamp, byte[] data) {

        // should be 4 bytes for packet size//数据包大小应为4个字节
        byte[] bytes = ByteUtils.intToBytes(10 + data.length);

        int packetSize = 14 + data.length; // 4 - inner packet size 1 - type + 1 - flag + 8 - timeStamp + data.length
        byte[] values = new byte[packetSize];

        System.arraycopy(bytes, 0, values, 0, 4);

        // set type value//设置类型值
        values[4] = type.getType();
        // set flag value//设置标志值
        values[5] = flag.getFlag();
        // set timeStamp//设置时间戳
        byte[] longToBytes = ByteUtils.longToBytes(presentationTimeStamp);
        System.arraycopy(longToBytes, 0, values, 6, longToBytes.length);

        // set data array//设置数据数组
        System.arraycopy(data, 0, values, 14, data.length);
        return values;
    }

    // should call on inner packet//应该调用内部数据包
    public static boolean isVideoPacket(byte[] values) {
        return values[0] == Type.VIDEO.getType();
    }

    public static StreamSettings getStreamSettings(byte[] buffer) {
        byte[] sps, pps;

        ByteBuffer spsPpsBuffer = ByteBuffer.wrap(buffer);
        if (spsPpsBuffer.getInt() == 0x00000001) {
            System.out.println("parsing sps/pps");
        } else {
            System.out.println("something is amiss?");
        }
        int ppsIndex = 0;
        while (!(spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x01)) {

        }
        ppsIndex = spsPpsBuffer.position();
        sps = new byte[ppsIndex - 4];
        System.arraycopy(buffer, 0, sps, 0, sps.length);
        ppsIndex -= 4;
        pps = new byte[buffer.length - ppsIndex];
        System.arraycopy(buffer, ppsIndex, pps, 0, pps.length);

        // sps buffer
        ByteBuffer spsBuffer = ByteBuffer.wrap(sps, 0, sps.length);

        // pps buffer
        ByteBuffer ppsBuffer = ByteBuffer.wrap(pps, 0, pps.length);

        StreamSettings streamSettings = new StreamSettings();
        streamSettings.sps = spsBuffer;
        streamSettings.pps = ppsBuffer;

        return streamSettings;
    }

    public static class StreamSettings {
        public ByteBuffer pps;
        public ByteBuffer sps;
    }
}
