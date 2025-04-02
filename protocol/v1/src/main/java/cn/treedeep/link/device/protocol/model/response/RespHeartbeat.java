package cn.treedeep.link.device.protocol.model.response;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.device.protocol.V1;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 心跳检测响应指令。
 * 用于服务端响应设备的心跳检测请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class RespHeartbeat extends BaseFrame {

    private long serverTimestamp;   // 时间戳(8B)

    @Override
    public byte getCommand() {
        return V1.RESP_HEARTBEAT;
    }
}
