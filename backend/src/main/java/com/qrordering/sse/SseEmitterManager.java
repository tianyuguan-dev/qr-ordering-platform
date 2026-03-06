package com.qrordering.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manage SSE connections by tenant. TECH_DESIGN_V2 §6.1
 */
@Component
@Slf4j
public class SseEmitterManager {

    private final Map<String, Set<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String tenantId, String clientId) {
        SseEmitter emitter = new SseEmitter(0L);
        tenantEmitters.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        log.info("Client {} subscribed to tenant {}, total connections: {}",
                clientId, tenantId, tenantEmitters.get(tenantId).size());

        emitter.onCompletion(() -> removeEmitter(tenantId, emitter));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitter));
        emitter.onError(e -> removeEmitter(tenantId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("Connected to tenant " + tenantId));
        } catch (IOException e) {
            removeEmitter(tenantId, emitter);
        }
        return emitter;
    }

    public void broadcast(String tenantId, SseMessage message) {
        Set<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            log.info("SSE broadcast skipped: no subscribers for tenant {} (event: {})", tenantId, message.getType());
            return;
        }
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(message.getType()).data(message.getData()));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        dead.forEach(e -> removeEmitter(tenantId, e));
        log.info("SSE broadcast {} to {} client(s) in tenant {}", message.getType(), emitters.size(), tenantId);
    }

    private void removeEmitter(String tenantId, SseEmitter emitter) {
        Set<SseEmitter> set = tenantEmitters.get(tenantId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) tenantEmitters.remove(tenantId);
        }
    }

    public int getConnectionCount(String tenantId) {
        Set<SseEmitter> set = tenantEmitters.get(tenantId);
        return set == null ? 0 : set.size();
    }

    /** Total SSE connections across all tenants (for metrics). */
    public int getTotalConnections() {
        return tenantEmitters.values().stream().mapToInt(Set::size).sum();
    }
}
