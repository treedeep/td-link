package cn.treedeep.link.protocol.v1.device.netty;

import cn.treedeep.link.core.event.DeviceEvent;
import cn.treedeep.link.core.event.DeviceEventPublisher;

import cn.treedeep.link.core.netty.ChannelManager;
import cn.treedeep.link.core.netty.DeviceSession;
import cn.treedeep.link.core.netty.FileUploadManager;
import cn.treedeep.link.core.netty.SessionManager;
import cn.treedeep.link.core.protocol.v1.BaseFrame;
import cn.treedeep.link.protocol.v1.device.protocol.V1;
import cn.treedeep.link.protocol.v1.device.protocol.model.report.*;
import cn.treedeep.link.protocol.v1.device.protocol.model.response.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>ServerHandler</p>
 *
 * @author 周广明
 * @since 2025/3/30 08:20
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelInboundHandler<BaseFrame> {

    private final SessionManager sessionManager;
    private final ChannelManager channelManager;
    private final FileUploadManager fileUploadManager;
    private final DeviceEventPublisher eventPublisher;

    @Autowired
    public ServerHandler(SessionManager sessionManager,
                         ChannelManager channelManager,
                         FileUploadManager fileUploadManager,
                         DeviceEventPublisher eventPublisher) {
        this.sessionManager = sessionManager;
        this.channelManager = channelManager;
        this.fileUploadManager = fileUploadManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseFrame frame) {

        switch (frame.getCommand()) {
            case V1.REPORT_DEVICE_CONNECTION_REQUEST:
                handleDeviceConnect(ctx, (ReportDeviceConnectionRequest) frame);
                break;
            case V1.REPORT_DEVICE_BIND_RESPONSE:
                handleDeviceBindResponse(ctx, (ReportDeviceBindResponse) frame);
                break;
            case V1.REPORT_START_RECORDING_RESPONSE:
                handleStartRecordingResponse(ctx, (ReportStartRecordingResponse) frame);
                break;
            case V1.REPORT_STOP_RECORDING_RESPONSE:
                handleStopRecordingResponse(ctx, (ReportStopRecordingResponse) frame);
                break;
            case V1.REPORT_HEARTBEAT_PACKET:
                handleHeartbeat(ctx, (ReportHeartbeatPacket) frame);
                break;
            case V1.REPORT_KEYFRAME_MARK:
                handleKeyframeMark(ctx, (ReportKeyframeMark) frame);
                break;
            case V1.REPORT_FILE_FRAME_UPLOAD:
                handleFileFrameUpload(ctx, (ReportFileFrameUpload) frame);
                break;
            case V1.REPORT_FILE_UPLOAD_END:
                handleFileUploadEnd(ctx, (ReportFileUploadEnd) frame);
                break;

            default:
                log.warn("未知命令：0x{}", Integer.toHexString(frame.getCommand() & 0xFF).toUpperCase());
        }
    }

    private void handleDeviceBindResponse(ChannelHandlerContext ctx, ReportDeviceBindResponse response) {
        log.info("设备绑定/解绑定响应：【设备ID：{}, 状态：{}】", response.getDeviceId(), response.getStatus());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("status", response.getStatus());

        DeviceEvent event = new DeviceEvent(
                "DEVICE_BIND",
                response.getDeviceId(),
                response.getTaskId(),
                eventData
        );
        eventPublisher.publishEvent(event);
    }

    private void handleStartRecordingResponse(ChannelHandlerContext ctx, ReportStartRecordingResponse response) {
        log.info("开始录制响应：【设备ID：{}, 状态：{}】", response.getDeviceId(), response.getStatus());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("status", response.getStatus());

        DeviceEvent event = new DeviceEvent(
                "START_RECORDING",
                response.getDeviceId(),
                response.getTaskId(),
                eventData
        );
        eventPublisher.publishEvent(event);
    }

    private void handleStopRecordingResponse(ChannelHandlerContext ctx, ReportStopRecordingResponse response) {
        log.info("停止录制响应：【设备ID：{}, 状态：{}】", response.getDeviceId(), response.getStatus());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("status", response.getStatus());

        DeviceEvent event = new DeviceEvent(
                "STOP_RECORDING",
                response.getDeviceId(),
                response.getTaskId(),
                eventData
        );
        eventPublisher.publishEvent(event);
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, ReportHeartbeatPacket heartbeat) {
        // 更新会话最后活动时间
        sessionManager.updateLastActive(heartbeat.getDeviceId(), heartbeat.getTaskId());

        // 自动响应心跳
        RespHeartbeat response = new RespHeartbeat(System.currentTimeMillis());
        response.setDeviceId(heartbeat.getDeviceId());
        response.setSessionId(heartbeat.getSessionId());
        response.setTaskId(heartbeat.getTaskId());

        ctx.writeAndFlush(response);
        log.debug("心跳响应：【设备ID：{}】", heartbeat.getDeviceId());

        // 发布心跳事件
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("timestamp", System.currentTimeMillis());
        eventData.put("battery", heartbeat.getBattery());
        eventData.put("status", heartbeat.getStatus());

        DeviceEvent event = new DeviceEvent(
                "HEARTBEAT",
                heartbeat.getDeviceId(),
                heartbeat.getTaskId(),
                eventData
        );
        eventPublisher.publishEvent(event);
    }

    private void handleKeyframeMark(ChannelHandlerContext ctx, ReportKeyframeMark keyframe) {
        // 响应关键帧标记
        RespKeyframeMark response = new RespKeyframeMark(0, (byte) 1);
        response.setDeviceId(keyframe.getDeviceId());
        response.setSessionId(keyframe.getSessionId());

        ctx.writeAndFlush(response);
        log.info("关键帧标记：【设备ID：{}, 帧序号：{}, 时间戳：{}】",
                keyframe.getDeviceId(),
                keyframe.getFrameSeq(),
                keyframe.getTimestamp());

        // 发布关键帧事件
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("frameSeq", keyframe.getFrameSeq());
        eventData.put("timestamp", keyframe.getTimestamp());

        DeviceEvent event = new DeviceEvent(
                "KEYFRAME_MARK",
                keyframe.getDeviceId(),
                keyframe.getTaskId(),
                eventData
        );
        eventPublisher.publishEvent(event);
    }

    private void handleFileFrameUpload(ChannelHandlerContext ctx, ReportFileFrameUpload frame) {
        ByteBuf frameBuf = frame.getFrameData();

        int deviceId = frame.getDeviceId();
        int taskId = frame.getTaskId();
        int frameSeq = frame.getFrameSeq();

        // 使用FileUploadManager缓存文件帧
        fileUploadManager.cacheFileFrame(deviceId, taskId, frameSeq, frameBuf);

        // 响应文件帧上传
        RespFileFrameUpload response = new RespFileFrameUpload(frameSeq, (byte) 1);
        response.setDeviceId(deviceId);
        response.setSessionId(frame.getSessionId());

        ctx.writeAndFlush(response);
        log.debug("文件帧上传：【设备ID：{}, 任务ID：{}, 帧序号：{}, 数据长度：{}】", deviceId, taskId, frameSeq, frameBuf.readableBytes());
    }

    private void handleFileUploadEnd(ChannelHandlerContext ctx, ReportFileUploadEnd end) {
        int deviceId = end.getDeviceId();
        int taskId = end.getTaskId();
        int totalFrames = end.getTotalFrames();

        // 使用FileUploadManager保存文件
        FileUploadManager.FileSaveResult result = fileUploadManager.saveFile(deviceId, taskId, totalFrames);

        // 响应文件上传结束
        RespFileUploadEnd response = new RespFileUploadEnd(result.frameCount(), result.getFileHash());
        response.setDeviceId(deviceId);
        response.setSessionId(end.getSessionId());

        ctx.writeAndFlush(response);
        log.info("文件上传结束：【设备ID：{}, 任务ID：{}, 总帧数：{}, 文件名：{}】", deviceId, taskId, result.frameCount(), result.fileName());

        // 发布文件上传完成事件
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("totalFrames", result.frameCount());
        eventData.put("fileName", result.fileName());
        eventData.put("fileHash", result.getFileHash());

        DeviceEvent event = new DeviceEvent(
                "FILE_UPLOAD_COMPLETE",
                deviceId,
                taskId,
                eventData
        );
        eventPublisher.publishEvent(event);
    }

    private void handleDeviceConnect(ChannelHandlerContext ctx, ReportDeviceConnectionRequest request) {
        int deviceId = request.getDeviceId();

        // 生成会话，缓存通道
        DeviceSession session = sessionManager.createSession(deviceId);
        channelManager.addChannel(deviceId, ctx.channel());

        // 构建响应
        RespDeviceConnection response = new RespDeviceConnection(System.currentTimeMillis());
        response.setDeviceId(deviceId);
        response.setSessionId(session.getSessionId());

        ctx.writeAndFlush(response);
        log.info("设备连接：【{}】", deviceId);

        // 发布设备连接事件
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("sessionId", session.getSessionId());
        eventData.put("timestamp", System.currentTimeMillis());
        eventData.put("remoteAddress", ctx.channel().remoteAddress().toString());

        DeviceEvent event = new DeviceEvent(
                "DEVICE_CONNECTED",
                deviceId,
                request.getTaskId(),
                eventData
        );
        eventPublisher.publishEvent(event);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("设备通道已连接：【{}】", ctx.channel().remoteAddress());
        log.info("服务器启动成功，等待设备注册...");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("设备通道已断开：【{}】", channel.remoteAddress());
        int deviceId = channelManager.removeChannel(channel);

        // 更新会话状态
        DeviceSession session = sessionManager.removeSession(deviceId);
        session.setStatus(DeviceSession.SessionStatus.CLOSED);

        super.channelInactive(ctx);

        // 发布设备断开连接事件
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("timestamp", System.currentTimeMillis());
        eventData.put("remoteAddress", channel.remoteAddress().toString());

        DeviceEvent event = new DeviceEvent(
                "DEVICE_DISCONNECTED",
                deviceId,
                eventData
        );
        eventPublisher.publishEvent(event);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("设备通道异常：【{}】, 异常信息：{}", ctx.channel().remoteAddress(), cause.getMessage());

        // 发布设备异常事件
        Channel channel = ctx.channel();
        int deviceId = channelManager.getDeviceId(channel);

        // 更新会话状态
        DeviceSession session = sessionManager.getSession(deviceId);
        session.setStatus(DeviceSession.SessionStatus.INACTIVE);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("timestamp", System.currentTimeMillis());
        eventData.put("error", cause.getMessage());
        eventData.put("remoteAddress", channel.remoteAddress().toString());

        DeviceEvent event = new DeviceEvent(
                "DEVICE_ERROR",
                deviceId,
                eventData
        );
        eventPublisher.publishEvent(event);

        ctx.close();
    }
}