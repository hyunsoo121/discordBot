package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.service.TemporaryMatchStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemporaryMatchStorageServiceImpl implements TemporaryMatchStorageService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String MATCH_PREFIX = "temp:match:";
    private static final String ID_KEY = "temp:match:id_counter";
    private static final Duration TTL = Duration.ofMinutes(30);

    private String createKey(Long id) {
        return MATCH_PREFIX + id;
    }

    @Override
    public Long saveTemporaryMatch(MatchRegistrationDto dto) {
        // Redis의 INCR 명령으로 고유 ID 발급
        Long id = Optional.ofNullable(redisTemplate.opsForValue().increment(ID_KEY))
                .orElseThrow(() -> new IllegalStateException("Redis ID counter failed."));

        String key = createKey(id);

        // DTO를 저장하고 30분 TTL 적용
        redisTemplate.opsForValue().set(key, dto, TTL);

        return id;
    }

    @Override
    public MatchRegistrationDto getTemporaryMatch(Long id) {
        String key = createKey(id);
        Object matchObj = redisTemplate.opsForValue().get(key);

        if (matchObj instanceof MatchRegistrationDto) {
            return (MatchRegistrationDto) matchObj;
        }
        return null;
    }

    @Override
    public void removeTemporaryMatch(Long id) {
        redisTemplate.delete(createKey(id));
    }

    @Override
    public void updateTemporaryMatch(Long id, MatchRegistrationDto updatedDto) {
        String key = createKey(id);
        Boolean exists = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(exists)) {
            // 객체 업데이트 및 TTL 재설정
            redisTemplate.opsForValue().set(key, updatedDto, TTL);
        }
    }
}