package cn.treedeep.link.service;

import cn.treedeep.link.event.DeviceEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public SseEmitter createEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            log.info("SSE连接完成: {}", clientId);
            emitters.remove(clientId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE连接超时: {}", clientId);
            emitter.complete();
            emitters.remove(clientId);
        });

        emitter.onError((ex) -> {
            log.error("SSE连接错误: {}", clientId, ex);
            emitter.complete();
            emitters.remove(clientId);
        });

        emitters.put(clientId, emitter);
        return emitter;
    }

    public void sendEventToAll(DeviceEvent event) {
        String eventData;
        try {
            eventData = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("事件序列化失败", e);
            return;
        }

        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType())
                        .data(eventData));
            } catch (IOException e) {
                log.error("向客户端 {} 发送事件失败", clientId, e);
                emitter.completeWithError(e);
                emitters.remove(clientId);
            }
        });
    }

    public void sendEventToClient(String clientId, DeviceEvent event) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            return;
        }

        try {
            String eventData = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                    .name(event.getType())
                    .data(eventData));
        } catch (IOException e) {
            log.error("向客户端 {} 发送事件失败", clientId, e);
            emitter.completeWithError(e);
            emitters.remove(clientId);
        }
    }
}