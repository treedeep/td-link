package cn.treedeep.link.service;

import cn.treedeep.link.annotation.Remark;
import cn.treedeep.link.model.DeviceInfo;

import java.util.List;

public interface DeviceService {

    /**
     * 获取所有连接的设备列表
     *
     * @return 设备信息列表
     */
    List<DeviceInfo> getConnectedDevices();

    /**
     * 设备绑定指令
     *
     * @param deviceId 目标设备ID
     * @param taskId   任务ID
     * @return 指令发送结果
     */
    CommandResult deviceBind(@Remark("deviceId") int deviceId, @Remark("taskId") int taskId);

    /**
     * 设备解绑指令
     *
     * @param deviceId 目标设备ID
     * @param taskId   任务ID
     * @return 指令发送结果
     */
    CommandResult deviceUnBind(@Remark("deviceId") int deviceId, @Remark("taskId") int taskId);

    /**
     * 下发开始录制指令
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult startRecording(@Remark("deviceId") int deviceId);

    /**
     * 下发停止录制指令
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult stopRecording(@Remark("deviceId") int deviceId);

    /**
     * 强制断开设备连接
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult forceDisconnect(@Remark("deviceId") int deviceId);

}