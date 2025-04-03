package cn.treedeep.link.device.client;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.device.protocol.model.command.CmdDeviceBind;
import cn.treedeep.link.device.protocol.model.command.CmdDeviceUnBind;
import cn.treedeep.link.device.protocol.model.command.CmdStartRecording;
import cn.treedeep.link.device.protocol.model.command.CmdStopRecording;
import cn.treedeep.link.device.protocol.model.report.ReportDeviceBindResponse;
import cn.treedeep.link.device.protocol.model.report.ReportHeartbeatPacket;
import cn.treedeep.link.device.protocol.model.report.ReportStartRecordingResponse;
import cn.treedeep.link.device.protocol.model.report.ReportStopRecordingResponse;
import cn.treedeep.link.device.protocol.model.response.*;
import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.simulator.SimulatorStatus;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SimulatorHandler extends SimpleChannelInboundHandler<BaseFrame> {

    private final Pv1Device simulator;

    public SimulatorHandler(Pv1Device simulator) {
        this.simulator = simulator;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BaseFrame frame) {
        switch (frame.getCommand()) {
            case V1.RESP_DEVICE_CONNECTION:
                handleDeviceConnectionResponse((RespDeviceConnection) frame);
                break;
            case V1.RESP_HEARTBEAT:
                handleHeartbeatResponse((RespHeartbeat) frame);
                break;
            case V1.RESP_KEYFRAME_MARK:
                RespKeyframeMark keyframeResp = (RespKeyframeMark) frame;
                log.debug("模拟器 => 设备【{}】收到关键帧标记响应，帧序号：{}", simulator.getDeviceId(), keyframeResp.getFrameSeq());
                break;
            case V1.RESP_FILE_FRAME_UPLOAD:
                RespFileFrameUpload frameResp = (RespFileFrameUpload) frame;
                simulator.notifyFrameAcked(frameResp.getFrameSeq());
                log.debug("模拟器 => 设备【{}】收到文件帧上传响应，帧序号：{}", simulator.getDeviceId(), frameResp.getFrameSeq());
                break;
            case V1.RESP_FILE_UPLOAD_END:
                RespFileUploadEnd uploadEndResp = (RespFileUploadEnd) frame;
                simulator.notifyUploadComplete(uploadEndResp.getTotalFrames());
                break;
            case V1.CMD_DEVICE_BIND:
                CmdDeviceBind bindCmd = (CmdDeviceBind) frame;
                handleDeviceBind(ctx, bindCmd);
                break;
            case V1.CMD_DEVICE_UNBIND:
                CmdDeviceUnBind unbindCmd = (CmdDeviceUnBind) frame;
                handleDeviceUnbind(ctx, unbindCmd);
                break;
            case V1.CMD_START_RECORDING:
                CmdStartRecording startCmd = (CmdStartRecording) frame;
                handleStartRecording(ctx, startCmd);
                break;
            case V1.CMD_STOP_RECORDING:
                CmdStopRecording stopCmd = (CmdStopRecording) frame;
                handleStopRecording(ctx, stopCmd);
                break;
            case V1.CMD_HEARTBEAT:
                handleHeartbeatCommand(ctx);
                break;
            case V1.CMD_FORCE_DISCONNECT:
                log.info("模拟器 => 设备【{}】收到强制断开命令", simulator.getDeviceId());
                ctx.close();
                break;
            case V1.RESP_FRAME_EXCEPTION:
                RespFrameError error = (RespFrameError) frame;
                log.warn("模拟器 => 设备【{}】收到异常帧响应", simulator.getDeviceId());
                break;
            default:
                log.warn("模拟器 => 设备【{}】收到未支持的命令类型: 0x{}",
                        simulator.getDeviceId(),
                        Integer.toHexString(frame.getCommand() & 0xFF));
        }
    }

    private void handleDeviceBind(ChannelHandlerContext ctx, CmdDeviceBind cmd) {
        log.info("模拟器 => 设备【{}】收到绑定命令，任务ID：{}", simulator.getDeviceId(), cmd.getTaskId());
        simulator.setTaskId(cmd.getTaskId());
        ReportDeviceBindResponse response = new ReportDeviceBindResponse((short) 1);
        response.setDeviceId(simulator.getDeviceId());
        response.setSessionId(simulator.getSessionId());
        response.setTaskId(simulator.getTaskId());
        ctx.writeAndFlush(response);
    }

    private void handleDeviceUnbind(ChannelHandlerContext ctx, CmdDeviceUnBind cmd) {
        log.info("模拟器 => 设备【{}】收到解绑命令，任务ID：{}", simulator.getDeviceId(), cmd.getTaskId());
        simulator.setTaskId(0);
        ReportDeviceBindResponse response = new ReportDeviceBindResponse((byte) 1);
        response.setDeviceId(simulator.getDeviceId());
        response.setSessionId(simulator.getSessionId());
        response.setTaskId(simulator.getTaskId());
        ctx.writeAndFlush(response);
    }

    private void handleStartRecording(ChannelHandlerContext ctx, CmdStartRecording cmd) {
        log.info("模拟器 => 设备【{}】收到开始录制命令", simulator.getDeviceId());
        ReportStartRecordingResponse response = new ReportStartRecordingResponse((byte) 1);
        response.setDeviceId(simulator.getDeviceId());
        response.setSessionId(simulator.getSessionId());
        response.setTaskId(simulator.getTaskId());
        ctx.writeAndFlush(response);
    }

    private void handleStopRecording(ChannelHandlerContext ctx, CmdStopRecording cmd) {
        log.info("模拟器 => 设备【{}】收到停止录制命令", simulator.getDeviceId());
        ReportStopRecordingResponse response = new ReportStopRecordingResponse((byte) 1);
        response.setDeviceId(simulator.getDeviceId());
        response.setSessionId(simulator.getSessionId());
        response.setTaskId(simulator.getTaskId());
        ctx.writeAndFlush(response);
    }

    private void handleHeartbeatCommand(ChannelHandlerContext ctx) {
        // 服务器要求发送心跳

        // 生成1-100随机数
        int battery = simulator.getRandom().nextInt(100) + 1;

        ReportHeartbeatPacket response = new ReportHeartbeatPacket((byte) battery, (byte) 1);
        response.setDeviceId(simulator.getDeviceId());
        response.setSessionId(simulator.getSessionId());
        response.setTaskId(simulator.getTaskId());
        ctx.writeAndFlush(response);
    }

    private void handleDeviceConnectionResponse(RespDeviceConnection response) {
        simulator.setSessionId(response.getSessionId());
        simulator.setTaskId(response.getTaskId());
        simulator.setStatus(SimulatorStatus.CONNECTED);  // 更新设备状态为已连接
        simulator.startHeartbeat();
        log.info("模拟器 => 设备【{}】连接成功，会话ID：{}", simulator.getDeviceId(), response.getSessionId());
    }

    private void handleHeartbeatResponse(RespHeartbeat response) {
        log.debug("模拟器 => 设备【{}】收到心跳响应，TaskId：{}", simulator.getDeviceId(), response.getTaskId());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        simulator.setStatus(SimulatorStatus.DISCONNECTED);
        simulator.stopHeartbeat();
        log.info("模拟器 => 设备【{}】连接断开", simulator.getDeviceId());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("模拟器 => 设备【{}】发生异常", simulator.getDeviceId(), cause);
        ctx.close();
    }
}