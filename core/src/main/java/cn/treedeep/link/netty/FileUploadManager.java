package cn.treedeep.link.netty;

import cn.treedeep.link.config.LinkConfig;
import cn.treedeep.link.util.FileHashUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>文件管理器，负责文件帧的缓存和保存</p>
 *
 * @author 周广明
 * @since 2025/3/30 08:20
 */
@Slf4j
@Component("td_link_FileUploadManager")
public class FileUploadManager {

    private final LinkConfig appConfig;

    @Autowired
    public FileUploadManager(LinkConfig appConfig) {
        this.appConfig = appConfig;

        // 创建文件保存目录
        this.videoSaveDir = appConfig.getUploadPath();
        this.tempDir = videoSaveDir + "/temp";
        createDirectories();
    }


    // 帧索引缓存，只存储帧序号和对应的临时文件路径
    private final Map<String, SortedMap<Integer, String>> frameIndexCache = new ConcurrentHashMap<>();
    // 文件保存目录
    private final String videoSaveDir;
    // 临时文件目录
    private final String tempDir;
    // 内存缓存阈值（字节），超过此大小的帧将直接写入临时文件
    private static final int MEMORY_THRESHOLD = 1024 * 1024; // 1MB
    // 小帧内存缓存
    private final Map<String, Map<Integer, byte[]>> smallFramesCache = new ConcurrentHashMap<>();


    /**
     * 创建所需的目录结构
     * 此方法旨在确保视频保存目录和临时文件目录存在如果不存在，则创建它们
     * 这是为了避免在上传文件或保存临时文件时出现因目录不存在而导致的错误
     */
    private void createDirectories() {
        try {
            // 创建文件保存目录
            Path videoDirPath = Paths.get(videoSaveDir);
            if (!Files.exists(videoDirPath)) {
                Files.createDirectories(videoDirPath);
                log.info("创建文件保存目录: {}", videoSaveDir);
            }

            // 创建临时文件目录
            Path tempDirPath = Paths.get(tempDir);
            if (!Files.exists(tempDirPath)) {
                Files.createDirectories(tempDirPath);
                log.info("创建临时文件目录: {}", tempDir);
            }
        } catch (IOException e) {
            log.error("创建目录失败", e);
        }
    }

    /**
     * 生成缓存键
     * <p>
     * 该方法用于根据设备ID和任务ID生成一个唯一的缓存键
     * 缓存键用于在缓存系统中唯一标识每个设备的任务信息
     *
     * @param deviceId 设备ID，标识一个特定的设备
     * @param taskId   任务ID，标识一个特定的任务
     * @return 返回生成的缓存键，格式为 "deviceId_taskId"
     */
    private String getCacheKey(int deviceId, int taskId) {
        return deviceId + "_" + taskId;
    }

    /**
     * 缓存文件帧（使用ByteBuf）
     *
     * @param deviceId 设备ID
     * @param taskId   任务ID
     * @param frameSeq 帧序号
     * @param frameBuf 帧数据ByteBuf
     */
    public void cacheFileFrame(int deviceId, int taskId, int frameSeq, ByteBuf frameBuf) {
        // 生成缓存键
        String cacheKey = getCacheKey(deviceId, taskId);

        int dataLength = frameBuf.readableBytes();

        // 根据帧大小决定存储方式
        if (dataLength <= MEMORY_THRESHOLD) {
            // 小帧存储在内存中 - 从ByteBuf中读取数据到byte数组
            byte[] frameData = new byte[dataLength];
            // 保存当前读索引
            int readerIndex = frameBuf.readerIndex();
            // 读取数据到数组
            frameBuf.getBytes(readerIndex, frameData);

            smallFramesCache.computeIfAbsent(cacheKey, k -> new ConcurrentHashMap<>()).put(frameSeq, frameData);

            log.debug("内存缓存文件帧(ByteBuf)：【设备ID：{}, 任务ID：{}, 帧序号：{}, 数据长度：{} 字节】",
                    deviceId, taskId, frameSeq, dataLength);
        } else {
            // 大帧存储在临时文件中
            String tempFilePath = saveTempFrame(deviceId, taskId, frameSeq, frameBuf);
            if (tempFilePath != null) {
                frameIndexCache.computeIfAbsent(cacheKey, k -> new TreeMap<>()).put(frameSeq, tempFilePath);
                log.debug("文件缓存文件帧(ByteBuf)：【设备ID：{}, 任务ID：{}, 帧序号：{}, 数据长度：{} 字节, 临时文件：{}】",
                        deviceId, taskId, frameSeq, dataLength, tempFilePath);
            }
        }
    }

    /**
     * 将ByteBuf帧数据保存到临时文件
     *
     * @param deviceId 设备ID
     * @param taskId   任务ID
     * @param frameSeq 帧序列号
     * @param frameBuf 帧数据ByteBuf
     * @return 成功保存时返回临时文件的绝对路径，否则返回null
     */
    private String saveTempFrame(int deviceId, int taskId, int frameSeq, ByteBuf frameBuf) {
        // 根据设备ID、任务ID和帧序列号生成临时文件名
        String tempFileName = String.format("%d_%d_%d.tmp", deviceId, taskId, frameSeq);
        // 在指定的临时目录中创建文件对象
        File tempFile = new File(tempDir, tempFileName);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // 保存当前读索引
            int readerIndex = frameBuf.readerIndex();

            // 将ByteBuf数据写入到临时文件中
            byte[] buffer = new byte[8192]; // 8KB缓冲区
            int remaining = frameBuf.readableBytes();
            int offset = readerIndex;

            while (remaining > 0) {
                int length = Math.min(buffer.length, remaining);
                frameBuf.getBytes(offset, buffer, 0, length);
                fos.write(buffer, 0, length);
                offset += length;
                remaining -= length;
            }

            // 返回临时文件的绝对路径
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            // 记录保存临时帧文件失败的日志信息
            log.error("保存临时帧文件失败", e);
            return null;
        }
    }

    /**
     * 保存文件文件并清理缓存
     *
     * @param deviceId    设备ID
     * @param taskId      任务ID
     * @param totalFrames 预期总帧数
     * @return 保存的文件名和实际帧数
     */
    public FileSaveResult saveFile(int deviceId, int taskId, int totalFrames) {
        String cacheKey = getCacheKey(deviceId, taskId);

        // 获取内存中的小帧缓存
        Map<Integer, byte[]> smallFrames = smallFramesCache.getOrDefault(cacheKey, new ConcurrentHashMap<>());

        // 获取文件中的大帧索引
        SortedMap<Integer, String> frameIndex = frameIndexCache.getOrDefault(cacheKey, new TreeMap<>());

        // 计算总帧数
        int memoryFrameCount = smallFrames.size();
        int fileFrameCount = frameIndex.size();
        int totalReceivedFrames = memoryFrameCount + fileFrameCount;

        if (totalReceivedFrames == 0) {
            log.error("未找到设备 {} 任务 {} 的文件帧缓存", deviceId, taskId);
            return new FileSaveResult(null, 0, false);
        }

        // 检查帧数是否匹配
        if (totalReceivedFrames != totalFrames) {
            log.warn("文件帧数不匹配：期望 {} 帧，实际接收 {} 帧", totalFrames, totalReceivedFrames);
        }

        // 合并所有帧并保存文件文件
        File file = mergeAndSaveFile(deviceId, taskId, smallFrames, frameIndex);

        // 清理缓存和临时文件
        cleanupResources(cacheKey, frameIndex);

        boolean success = file != null;
        return new FileSaveResult(file, totalReceivedFrames, success);
    }

    /**
     * 合并所有帧并保存为文件文件
     *
     * @param deviceId    设备ID
     * @param taskId      任务ID
     * @param smallFrames 小帧的集合，键为帧序号，值为帧数据
     * @param frameIndex  大帧的集合，键为帧序号，值为帧数据的文件路径
     * @return 保存的文件名，保存失败则返回null
     */
    private File mergeAndSaveFile(int deviceId, int taskId, Map<Integer, byte[]> smallFrames, SortedMap<Integer, String> frameIndex) {
        // 生成文件名：设备ID_任务ID_时间戳.mp4
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%d_%d_%s.mp4", deviceId, taskId, timestamp);
        File file = new File(videoSaveDir, fileName);

        // 确保保存目录存在
        createDirectories();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            // 创建合并的帧序号列表

            // 添加内存中的小帧
            SortedMap<Integer, Object> allFrames = new TreeMap<>(smallFrames);

            // 添加文件中的大帧
            allFrames.putAll(frameIndex);

            // 按序号顺序写入文件
            for (Map.Entry<Integer, Object> entry : allFrames.entrySet()) {
                int frameSeq = entry.getKey();
                Object frameObj = entry.getValue();

                if (frameObj instanceof byte[]) {
                    // 直接写入内存中的帧数据
                    fos.write((byte[]) frameObj);
                } else if (frameObj instanceof String tempFilePath) {
                    // 从临时文件读取并写入帧数据
                    try (FileInputStream fis = new FileInputStream(tempFilePath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }

            fos.flush();
            log.info("文件文件保存成功：{}", file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            log.error("保存文件文件失败", e);
            return null;
        }
    }

    /**
     * 清理缓存和临时文件
     * <p>
     * 该方法用于清理存储在缓存中的小帧图像数据和帧索引，以及删除之前生成的临时文件
     * 它主要在处理视频帧时，为了释放资源和清理存储空间而调用
     *
     * @param cacheKey   缓存键，用于标识特定的缓存数据
     * @param frameIndex 排序映射，包含帧索引和对应的临时文件路径
     */
    private void cleanupResources(String cacheKey, SortedMap<Integer, String> frameIndex) {
        // 清理内存缓存
        smallFramesCache.remove(cacheKey);
        frameIndexCache.remove(cacheKey);

        // 删除临时文件
        for (String tempFilePath : frameIndex.values()) {
            try {
                Files.deleteIfExists(Paths.get(tempFilePath));
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", tempFilePath, e);
            }
        }
    }

    /**
     * 文件保存结果
     */
    public record FileSaveResult(File file, int frameCount, boolean success) {

        /**
         * 获取文件的哈希值
         * <p>
         * 此方法用于获取文件名的字节表示，作为文件的简单哈希值
         * 如果文件名为空，则返回null，表示没有文件名或不支持哈希计算
         *
         * @return 文件名的字节数组如果文件名为空，则返回null
         */
        public byte[] getFileHash() {
            if (file == null) {
                return new byte[0];
            }
            // return file.getName().getBytes();
            try {
                return FileHashUtil.calculateFileHash(file.getAbsolutePath(), FileHashUtil.HashAlgorithm.MD5);
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

}