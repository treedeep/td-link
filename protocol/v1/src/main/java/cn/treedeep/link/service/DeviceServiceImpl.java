package cn.treedeep.link.service;

import cn.treedeep.link.device.protocol.model.command.*;
import cn.treedeep.link.model.DeviceInfo;
import cn.treedeep.link.netty.ChannelManager;
import cn.treedeep.link.netty.SessionManager;
import cn.treedeep.link.protocol.v1.Pv1BaseFrame;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("p_v1_DeviceService")
public class DeviceServiceImpl implements DeviceService {

    private final ChannelManager channelManager;
    private final SessionManager sessionManager;

    @Autowired
    public DeviceServiceImpl(ChannelManager channelManager, SessionManager sessionManager) {
        this.channelManager = channelManager;
        this.sessionManager = sessionManager;
    }


    @Override
    public List<DeviceInfo> getConnectedDevices() {
        return sessionManager.getAllSessions().stream()
                .map(session -> {
                    DeviceInfo deviceInfo = new DeviceInfo();
                    deviceInfo.setDeviceId(session.getDeviceId());
                    deviceInfo.setSessionId(session.getSessionId());
                    deviceInfo.setConnectedTime(session.getCreateTime());
                    deviceInfo.setLastActiveTime(session.getLastActiveTime());
                    deviceInfo.setTaskId(session.getTaskId());

                    // 获取设备远程地址
                    String remoteAddress = channelManager.getChannel(session.getDeviceId()) != null ?
                            channelManager.getChannel(session.getDeviceId()).remoteAddress().toString() : "未知";
                    deviceInfo.setRemoteAddress(remoteAddress);

                    // 设置设备状态
                    deviceInfo.setStatus(session.getStatus().name());

                    return deviceInfo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CommandResult deviceBind(int deviceId, int taskId) {
        return sendCommand(deviceId, ctx -> {
            CmdDeviceBind cmd = new CmdDeviceBind();
            cmd.setTaskId(taskId);
            cmd.setDeviceId(deviceId);
            cmd.setSessionId(sessionManager.getSessionId(deviceId));
            return cmd;
        });
    }

    @Override
    public CommandResult deviceUnBind(int deviceId, int taskId) {
        return sendCommand(deviceId, ctx -> {
            CmdDeviceUnBind cmd = new CmdDeviceUnBind();
            cmd.setTaskId(taskId);
            cmd.setDeviceId(deviceId);
            cmd.setSessionId(sessionManager.getSessionId(deviceId));
            return cmd;
        });
    }

    @Override
    public CommandResult startRecording(int deviceId) {
        return sendCommand(deviceId, ctx -> {
            CmdStartRecording cmd = new CmdStartRecording();
            cmd.setDeviceId(deviceId);
            cmd.setSessionId(sessionManager.getSessionId(deviceId));
            return cmd;
        });
    }

    @Override
    public CommandResult stopRecording(int deviceId) {
        return sendCommand(deviceId, ctx -> {
            CmdStopRecording cmd = new CmdStopRecording();
            cmd.setDeviceId(deviceId);
            cmd.setSessionId(sessionManager.getSessionId(deviceId));
            return cmd;
        });
    }

    @Override
    public CommandResult forceDisconnect(int deviceId) {
        CommandResult commandResult = sendCommand(deviceId, ctx -> {
            CmdForceDisconnect cmd = new CmdForceDisconnect();
            cmd.setDeviceId(deviceId);
            cmd.setSessionId(sessionManager.getSessionId(deviceId));
            return cmd;
        });

        Channel channel = channelManager.getChannel(deviceId);
        if (channel == null) {
            return CommandResult.failure("设备未连接");
        }

        channel.disconnect();

        channelManager.removeChannel(deviceId);
        sessionManager.removeSession(deviceId);

        return commandResult;
    }

    private CommandResult sendCommand(int deviceId, Function<ChannelHandlerContext, Pv1BaseFrame> commandBuilder) {
        Channel channel = channelManager.getChannel(deviceId);
        if (channel == null) {
            return CommandResult.failure("设备未连接");
        }

        if (!channel.isActive()) {
            channelManager.removeChannel(deviceId);
            sessionManager.removeSession(deviceId);
            return CommandResult.failure("通道已失效");
        }

        try {
            Pv1BaseFrame command = commandBuilder.apply(channel.pipeline().context(ChannelHandler.class));
            channel.writeAndFlush(command);
            return CommandResult.success();
        } catch (Exception e) {
            return CommandResult.failure("指令发送失败: " + e.getMessage());
        }
    }
}