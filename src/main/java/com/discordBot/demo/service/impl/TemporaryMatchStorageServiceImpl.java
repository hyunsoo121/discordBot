package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.service.TemporaryMatchStorageService;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TemporaryMatchStorageServiceImpl implements TemporaryMatchStorageService {

    private final Map<Long, MatchRegistrationDto> storage = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Long saveTemporaryMatch(MatchRegistrationDto dto) {
        Long id = idCounter.getAndIncrement();
        storage.put(id, dto);
        return id;
    }

    @Override
    public MatchRegistrationDto getTemporaryMatch(Long id) {
        return storage.get(id);
    }

    @Override
    public void removeTemporaryMatch(Long id) {
        storage.remove(id);
    }

    @Override
    public void updateTemporaryMatch(Long id, MatchRegistrationDto updatedDto) {
        if (storage.containsKey(id)) {
            storage.put(id, updatedDto);
        }
    }
}