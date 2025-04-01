package cn.treedeep.link.controller;

import cn.treedeep.link.protocol.v1.device.client.DeviceSimulator;
import cn.treedeep.link.protocol.v1.device.client.SimulatorManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>设备模拟器</p>
 *
 * @author 周广明
 * @since 2025/3/30 11:42
 */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulatorManager simulatorManager;

    @Autowired
    public SimulatorController(SimulatorManager simulatorManager) {
        this.simulatorManager = simulatorManager;
    }

    /**
     * 创建新的设备模拟器
     */
    @GetMapping("/create")
    public ResponseEntity<DeviceSimulator> createSimulator(@RequestParam int deviceId) {
        DeviceSimulator simulator = simulatorManager.createSimulator(deviceId);
        return ResponseEntity.ok(simulator);
    }

    /**
     * 获取所有模拟器列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<DeviceSimulator>> listSimulators() {
        return ResponseEntity.ok(simulatorManager.getAllSimulators());
    }

    /**
     * 连接设备到服务器
     */
    @GetMapping("/{deviceId}/connect")
    public ResponseEntity<String> connectDevice(@PathVariable int deviceId) {
        simulatorManager.connectDevice(deviceId);
        return ResponseEntity.ok("设备连接指令已发送");
    }

    /**
     * 断开设备连接
     */
    @GetMapping("/{deviceId}/disconnect")
    public ResponseEntity<String> disconnectDevice(@PathVariable int deviceId) {
        simulatorManager.disconnectDevice(deviceId);
        return ResponseEntity.ok("设备断开指令已发送");
    }

    /**
     * 触发文件上传
     */
    @GetMapping("/{deviceId}/upload")
    public ResponseEntity<String> uploadFile(@PathVariable int deviceId, @RequestParam String filePath) {
        simulatorManager.startFileUpload(deviceId, filePath);
        return ResponseEntity.ok("文件上传指令已发送");
    }

       /**
     * 获取模拟器状态
     */
    @GetMapping("/{deviceId}/status")
    public ResponseEntity<Map<String, Object>> getSimulatorStatus(@PathVariable int deviceId) {
        return ResponseEntity.ok(simulatorManager.getSimulatorStatus(deviceId));
    }

    /**
     * 批量创建模拟器
     */
    @PostMapping("/batch-create")
    public ResponseEntity<List<DeviceSimulator>> batchCreateSimulators(
            @RequestParam int startDeviceId,
            @RequestParam int count) {
        List<DeviceSimulator> simulators = simulatorManager.batchCreateSimulators(startDeviceId, count);
        return ResponseEntity.ok(simulators);
    }

    /**
     * 删除模拟器
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<String> removeSimulator(@PathVariable int deviceId) {
        simulatorManager.removeSimulator(deviceId);
        return ResponseEntity.ok("模拟器已删除");
    }

    /**
     * 删除所有模拟器
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> removeAllSimulators() {
        simulatorManager.removeAllSimulators();
        return ResponseEntity.ok("所有模拟器已删除");
    }
}
