package cn.treedeep.link.device.client;

import cn.treedeep.link.device.client.codec.DeviceFrameDecoder;
import cn.treedeep.link.device.client.codec.DeviceFrameEncoder;
import cn.treedeep.link.device.protocol.model.report.ReportDeviceConnectionRequest;
import cn.treedeep.link.device.protocol.model.report.ReportFileFrameUpload;
import cn.treedeep.link.device.protocol.model.report.ReportFileUploadEnd;
import cn.treedeep.link.device.protocol.model.report.ReportHeartbeatPacket;
import cn.treedeep.link.protocol.v1.Protocol;
import cn.treedeep.link.simulator.DeviceSimulator;
import cn.treedeep.link.simulator.SimulatorStatus;
import cn.treedeep.link.util.DatetimeUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>协议V1模拟设备</p>
 *
 * @author 周广明
 * @since 2025/4/5 20:59
 */
@Slf4j
public class Pv1Device extends DeviceSimulator {

    public Pv1Device(int deviceId) {
        super(deviceId);
    }

    private volatile CountDownLatch frameAckLatch;
    private volatile int lastAckedFrameSeq = 0;
    private volatile boolean uploading = false;

    private long startTime;
    private CountDownLatch uploadLatch;
    private final Random random = new Random();

    @Override
    public void connect(String host, int port) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    Protocol.lengthFieldBasedFrameDecoder(),
                                    new DeviceFrameDecoder(),
                                    new DeviceFrameEncoder(),
                                    new SimulatorHandler(Pv1Device.this)
                            );
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            status = SimulatorStatus.CONNECTING;

            // 发送注册请求
            sendRegisterRequest();

        } catch (Exception e) {
            log.error("设备【{}】连接/注册失败", deviceId, e);
            disconnect();
        }
    }

    @Override
    protected void sendRegisterRequest() {
        ReportDeviceConnectionRequest request = new ReportDeviceConnectionRequest();
        request.setDeviceId(deviceId);
        request.setVersion(request.getVersion());
        channel.writeAndFlush(request);
    }

    @Override
    public void sendHeartbeat() {
        if (channel != null && channel.isActive()) {
            int battery = random.nextInt(100) + 1;
            ReportHeartbeatPacket heartbeat = new ReportHeartbeatPacket((byte) battery, (byte) 1);
            heartbeat.setDeviceId(deviceId);
            heartbeat.setSessionId(sessionId);
            heartbeat.setTaskId(taskId);
            channel.writeAndFlush(heartbeat);
        }
    }

    @Override
    public void uploadFile(String filePath) {
        if (status != SimulatorStatus.CONNECTED) {
            log.warn("设备【{}】未连接，无法上传文件", deviceId);
            return;
        }
        if (uploading) {
            log.warn("设备【{}】正在上传文件，请等待完成", deviceId);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                uploading = true;
                uploadLatch = new CountDownLatch(1);
                File videoFile = new File(filePath);
                if (!videoFile.exists()) {
                    log.error("设备【{}】文件不存在: {}", deviceId, filePath);
                    return;
                }

                log.info("设备【{}】开始上传文件: {}", deviceId, videoFile.getName());
                uploadVideoFile(videoFile);
                uploadLatch.await(60, TimeUnit.SECONDS);
                log.info("设备【{}】文件上传完成", deviceId);
            } catch (Exception e) {
                log.error("设备【{}】文件上传异常", deviceId, e);
            } finally {
                uploading = false;
            }
        });
    }

    @Override
    public void disconnect() {
        stopHeartbeat();
        if (channel != null) {
            channel.close();
            channel = null;
        }
        group.shutdownGracefully();
        status = SimulatorStatus.DISCONNECTED;
    }

    private void uploadVideoFile(File videoFile) throws IOException, InterruptedException {
        startTime = System.currentTimeMillis();

        try (FileInputStream fis = new FileInputStream(videoFile)) {

            // 对于局域网或高速网络，可以考虑使用128KB或256KB
            // 对于互联网传输，64KB通常是个不错的平衡点
            // 如果设备内存有限，可以使用较小的值如32KB

            // 8192
            int bufferSize = 8192;
            ByteBuf buffer = channel.alloc().directBuffer(bufferSize);
            int frameSeq = 0;
            byte[] readBuffer = new byte[bufferSize];
            int bytesRead;

            log.info("设备【{}】开始读取文件: {}，文件大小: {}字节", deviceId, videoFile.getName(), videoFile.length());

            while ((bytesRead = fis.read(readBuffer)) != -1) {
                frameSeq++;

                // 创建新的等待锁
                frameAckLatch = new CountDownLatch(1);

                buffer.clear();
                buffer.writeBytes(readBuffer, 0, bytesRead);

                ReportFileFrameUpload frameUpload = new ReportFileFrameUpload(frameSeq, buffer);
                frameUpload.setDeviceId(deviceId);
                frameUpload.setSessionId(sessionId);
                frameUpload.setTaskId(taskId);

                channel.writeAndFlush(frameUpload);
                log.debug("设备【{}】已发送第{}帧数据，等待确认", deviceId, frameSeq);

                // 等待确认，超时时间5秒
                if (!frameAckLatch.await(5, TimeUnit.SECONDS)) {
                    throw new IOException("帧确认超时：" + frameSeq);
                }
            }

            buffer.release();

            ReportFileUploadEnd uploadEnd = new ReportFileUploadEnd(frameSeq);
            uploadEnd.setDeviceId(deviceId);
            uploadEnd.setSessionId(sessionId);
            uploadEnd.setTaskId(taskId);
            channel.writeAndFlush(uploadEnd).sync();

            log.info("设备【{}】发送完成，共发送{}帧数据", deviceId, frameSeq);
        } catch (Exception e) {
            log.error("设备【{}】上传文件过程中发生异常: {}", deviceId, e.getMessage(), e);
            throw e;
        }
    }

    public void notifyFrameAcked(int frameSeq) {
        if (frameSeq - lastAckedFrameSeq == 1) {
            log.debug("设备【{}】已收到第{}帧数据确认", deviceId, frameSeq);

            lastAckedFrameSeq = frameSeq;
            if (frameAckLatch != null) {
                frameAckLatch.countDown();
            }
        } else {
            log.warn("设备【{}】收到重复的帧确认：{}，上次帧序号：{}", deviceId, frameSeq, lastAckedFrameSeq);
        }
    }

    public void notifyUploadComplete(int totalFrames) {
        if (uploadLatch != null) {
            uploadLatch.countDown();
        }
        // 重置帧序号
        lastAckedFrameSeq = 0;

        long duration = System.currentTimeMillis() - startTime;
        String formattedDuration = DatetimeUtil.formatDuration(duration);
        log.info("设备【{}】文件上传完成，总帧数：{}，耗时：{}", deviceId, totalFrames, formattedDuration);
    }

}