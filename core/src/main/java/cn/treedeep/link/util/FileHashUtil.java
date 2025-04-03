package cn.treedeep.link.util;

import lombok.Getter;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHashUtil {

    /**
     * 计算文件的哈希值
     *
     * @param filePath  文件路径
     * @param algorithm 哈希算法枚举
     * @return 哈希值的十六进制字符串
     * @throws IOException              如果文件读取失败
     * @throws NoSuchAlgorithmException 如果算法不可用
     */
    public static byte[] calculateFileHash(String filePath, HashAlgorithm algorithm) throws IOException, NoSuchAlgorithmException {
        // 创建 MessageDigest 实例
        MessageDigest digest = MessageDigest.getInstance(algorithm.getAlgorithmName());

        // 使用文件输入流读取文件内容
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192]; // 8KB 缓冲区
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        // 计算哈希值
        return digest.digest();
    }

    public static String calculateFileHashStr(String filePath, HashAlgorithm algorithm) throws IOException, NoSuchAlgorithmException {
        return HexUtil.bytesToHex(calculateFileHash(filePath, algorithm));
    }

    @Getter
    public enum HashAlgorithm {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256");

        private final String algorithmName;

        HashAlgorithm(String algorithmName) {
            this.algorithmName = algorithmName;
        }

    }

}
