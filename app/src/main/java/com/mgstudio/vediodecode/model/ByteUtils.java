package com.mgstudio.vediodecode.model;

import java.math.BigInteger;
import java.nio.ByteBuffer;

//字节工具
public class ByteUtils {

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / 8);
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        return new BigInteger(bytes).longValue();
    }

    public static byte[] intToBytes(int x) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / 8);
        buffer.putInt(0, x);
        return buffer.array();
    }

    public static int bytesToInt(byte[] bytes) {
        return new BigInteger(bytes).intValue();
    }
}
