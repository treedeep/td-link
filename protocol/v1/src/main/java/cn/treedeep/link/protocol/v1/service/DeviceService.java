package cn.treedeep.link.protocol.v1.service;

import cn.treedeep.link.core.model.DeviceInfo;
import cn.treedeep.link.core.service.CommandResult;

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
    CommandResult deviceBind(int deviceId, int taskId);

    /**
     * 设备解绑指令
     *
     * @param deviceId 目标设备ID
     * @param taskId   任务ID
     * @return 指令发送结果
     */
    CommandResult deviceUnBind(int deviceId, int taskId);

    /**
     * 下发开始录制指令
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult startRecording(int deviceId);

    /**
     * 下发停止录制指令
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult stopRecording(int deviceId);

    /**
     * 强制断开设备连接
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult forceDisconnect(int deviceId);

}