package org.example.Server.controller;

import org.example.DB.read.DbReadService;
import org.example.ETL.entities.Indicator;
import org.example.Events.AddEvent.NewIndicatorsIdsEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
public class SseController {

    // Ссылка на READ сервис
    private final DbReadService dbRead;

    // Список активных клиентов
    private final List<SseEmitter> clients = new CopyOnWriteArrayList<>();

    public SseController(DbReadService dbRead) {
        this.dbRead = dbRead;
    }

    @GetMapping("/stream")
    public SseEmitter subscribe() {
        // Таймаут 5 минут
        SseEmitter emitter = new SseEmitter(300_000L);
        clients.add(emitter);

        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> clients.remove(emitter));
        emitter.onError((e) -> clients.remove(emitter));

        return emitter;
    }

    @EventListener
    public void handleNewData(NewIndicatorsIdsEvent event) {
        // 1. Получаем ID из события
        List<Integer> ids = event.getIndicatorIds();

        // 2. Делаем SELECT через Read Service
        List<Indicator> data = dbRead.getIndicatorsByIds(ids);

        if (data.isEmpty()) return;

        // 3. Рассылаем
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter client : clients) {
            try {
                client.send(SseEmitter.event().name("message").data(data));
            } catch (Exception e) {
                deadEmitters.add(client);
            }
        }
        clients.removeAll(deadEmitters);
    }
}