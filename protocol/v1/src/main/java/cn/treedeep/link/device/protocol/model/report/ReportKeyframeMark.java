package cn.treedeep.link.device.protocol.model.report;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.Pv1BaseFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关键帧标记指令。
 * 用于设备向服务端发送关键帧标记。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ReportKeyframeMark extends Pv1BaseFrame {

    private int frameSeq;   // 帧序号(4B)
    private long timestamp; // 时间戳(8B)

    @Override
    public byte getCommand() {
        return V1.REPORT_KEYFRAME_MARK;
    }
}
