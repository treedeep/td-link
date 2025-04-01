package cn.treedeep.link.controller;

import cn.treedeep.link.core.service.CommandResult;
import cn.treedeep.link.core.model.DeviceInfo;
import cn.treedeep.link.service.SseService;
import cn.treedeep.link.protocol.v1.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>DeviceController</p>
 *
 * @author 周广明
 * @since 2025/3/30 10:01
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService commandService;
    private final SseService sseService;

    @Autowired
    public DeviceController(DeviceService commandService, SseService sseService) {
        this.commandService = commandService;
        this.sseService = sseService;
    }

        /**
     * 获取所有在线设备列表
     *
     * @return 设备列表，包含设备ID、会话ID、连接状态等信息
     */
    @GetMapping("/list")
    public ResponseEntity<List<DeviceInfo>> getDeviceList() {
        return ResponseEntity.ok(commandService.getConnectedDevices());
    }

    /**
     * 绑定设备到指定任务
     *
     * @param deviceId 设备ID，唯一标识一个设备
     * @param taskId 任务ID，设备将被绑定到此任务
     * @return 命令执行结果，包含成功/失败状态和消息
     */
    @GetMapping("/bind")
    public ResponseEntity<CommandResult> deviceBind(@RequestParam int deviceId, @RequestParam int taskId) {
        return ResponseEntity.ok(commandService.deviceBind(deviceId, taskId));
    }

    /**
     * 解除设备与任务的绑定关系
     *
     * @param deviceId 设备ID，唯一标识一个设备
     * @param taskId 任务ID，设备将与此任务解除绑定
     * @return 命令执行结果，包含成功/失败状态和消息
     */
    @GetMapping("/unbind")
    public ResponseEntity<CommandResult> deviceUnBind(@RequestParam int deviceId, @RequestParam int taskId) {
        return ResponseEntity.ok(commandService.deviceUnBind(deviceId, taskId));
    }

    /**
     * 开始设备录制
     *
     * @param deviceId 设备ID，唯一标识一个设备
     * @return 命令执行结果，包含成功/失败状态和消息
     */
    @GetMapping("/start")
    public ResponseEntity<CommandResult> startRecording(@RequestParam int deviceId) {
        return ResponseEntity.ok(commandService.startRecording(deviceId));
    }

    /**
     * 停止设备录制
     *
     * @param deviceId 设备ID，唯一标识一个设备
     * @return 命令执行结果，包含成功/失败状态和消息
     */
    @GetMapping("/stop")
    public ResponseEntity<CommandResult> stopRecording(@RequestParam int deviceId) {
        return ResponseEntity.ok(commandService.stopRecording(deviceId));
    }

    /**
     * 强制断开设备连接
     *
     * @param deviceId 设备ID，唯一标识一个设备
     * @return 命令执行结果，包含成功/失败状态和消息
     */
    @GetMapping("/disconnect")
    public ResponseEntity<CommandResult> forceDisconnect(@RequestParam int deviceId) {
        return ResponseEntity.ok(commandService.forceDisconnect(deviceId));
    }


    /**
     * 创建SSE连接，用于接收所有设备的事件通知
     * 客户端可以通过此接口订阅服务器推送的实时事件
     *
     * @return SSE发射器，用于向客户端推送事件流
     */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToEvents() {
        String clientId = UUID.randomUUID().toString();
        return sseService.createEmitter(clientId);
    }

    /**
     * 创建特定设备的SSE连接，只接收指定设备的事件通知
     * 客户端可以通过此接口订阅特定设备的实时事件
     *
     * @param deviceId 设备ID，唯一标识一个设备，只接收此设备的事件
     * @return SSE发射器，用于向客户端推送指定设备的事件流
     */
    @GetMapping(path = "/{deviceId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToDeviceEvents(@PathVariable int deviceId) {
        String clientId = "DEVICE:" + deviceId;
        return sseService.createEmitter(clientId);
    }
}