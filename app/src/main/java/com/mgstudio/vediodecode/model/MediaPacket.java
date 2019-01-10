package com.mgstudio.vediodecode.model;
//媒体包
class MediaPacket {


    public enum Type {

        VIDEO((byte) 1), AUDIO((byte) 0);

        private byte type;

        Type(byte type) {
            this.type = type;
        }

        public byte getType() {
            return type;
        }

        public static Type getType(byte value) {
            for (Type type : Type.values()) {
                if (type.getType() == value) {
                    return type;
                }
            }

            return null;
        }
    }

    public Type type;
}
