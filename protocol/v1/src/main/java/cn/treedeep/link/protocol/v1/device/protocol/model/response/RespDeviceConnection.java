package cn.treedeep.link.protocol.v1.device.protocol.model.response;

import cn.treedeep.link.protocol.v1.device.protocol.V1;
import cn.treedeep.link.core.protocol.v1.BaseFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备连接响应指令。
 * 用于服务端响应设备的连接请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class RespDeviceConnection extends BaseFrame {

    private long serverTimestamp;   // 时间戳(8B)

    @Override
    public byte getCommand() {
        return V1.RESP_DEVICE_CONNECTION;
    }
}

