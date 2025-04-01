package cn.treedeep.link.core.netty;

import io.netty.channel.Channel;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ChannelManager {
    private final ConcurrentMap<Integer, Channel> deviceChannels = new ConcurrentHashMap<>();

    public void addChannel(int deviceId, Channel channel) {
        deviceChannels.put(deviceId, channel);
    }

    public Channel getChannel(int deviceId) {
        return deviceChannels.get(deviceId);
    }


    public Channel removeChannel(int deviceId) {
        return deviceChannels.remove(deviceId);
    }

    public Integer removeChannel(Channel channel) {
        for (Integer deviceId : deviceChannels.keySet()) {
            if (deviceChannels.get(deviceId) == channel) {
                deviceChannels.remove(deviceId);
                return deviceId;
            }
        }

        return -1;
    }

    public Integer getDeviceId(Channel channel) {
        for (Integer deviceId : deviceChannels.keySet()) {
            if (deviceChannels.get(deviceId) == channel) {
                return deviceId;
            }
        }

        return -1;
    }


}