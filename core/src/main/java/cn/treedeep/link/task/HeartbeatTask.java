package cn.treedeep.link.task;

import cn.treedeep.link.netty.ChannelManager;
import cn.treedeep.link.netty.ServerHeartbeat;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeartbeatTask {

    private final ChannelManager channelManager;

    public HeartbeatTask(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void scheduleHeartbeatInterval(ServerHeartbeat heartbeat) {
        channelManager.getDeviceChannels().values().forEach(channel -> channel.writeAndFlush(heartbeat));
    }

}
