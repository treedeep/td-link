package cn.treedeep.link.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class VideoTrimmer {

    /**
     * 截取视频的指定时间段
     *
     * @param inputFile  输入视频文件路径
     * @param outputFile 输出视频文件路径
     * @param startTime  开始时间（格式：hh:mm:ss）
     * @param duration   持续时间（格式：hh:mm:ss）
     * @return 是否成功
     */
    public static boolean trimVideo(String inputFile, String outputFile, String startTime, String duration) {
        try {
            // 构建 FFmpeg 命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-i", inputFile,
                    "-ss", startTime,
                    "-t", duration,
                    "-c:v", "libx264", // 使用 H.264 编码视频流
                    "-c:a", "aac",     // 使用 AAC 编码音频流
                    outputFile
            );

            // 设置错误流重定向到标准输出流，以便捕获错误信息
            processBuilder.redirectErrorStream(true);

            // 启动进程
            Process process = processBuilder.start();

            // 读取进程的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.error("视频截取失败", e);
            return false;
        }
    }

}
