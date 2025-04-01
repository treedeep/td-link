package cn.treedeep.link.core.util;

public class CRC16 {

    // CRC-16 CCITT
    public static int calculateCCITT(byte[] data) {
        int crc = 0xFFFF; // CCITT-FALSE初始值
        int polynomial = 0x1021;

        for (byte b : data) {
            int byteValue = b & 0xFF;
            crc ^= (byteValue << 8);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ polynomial;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return crc;
    }

}