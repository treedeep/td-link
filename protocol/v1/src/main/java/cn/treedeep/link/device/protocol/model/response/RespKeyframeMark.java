package cn.treedeep.link.device.protocol.model.response;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.device.protocol.V1;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关键帧标记响应指令。
 * 用于服务端响应设备的关键帧标记请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class RespKeyframeMark extends BaseFrame {

    private int frameSeq;   // 帧序号(4B)
    private byte ackStatus; // 确认状态(1B)

    @Override
    public byte getCommand() {
        return V1.RESP_KEYFRAME_MARK;
    }
}
