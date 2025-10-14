package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.repository.GuildServerRepository;
import com.discordBot.demo.service.ServerManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServerManagementServiceImpl implements ServerManagementService {

    private final GuildServerRepository guildServerRepository;

    /**
     * GuildServer를 찾고, 없으면 생성 트랜잭션으로 위임합니다.
     * 기본 트랜잭션은 readOnly=true로 설정하여 조회된 객체가 Managed 상태가 되는 것을 방지합니다.
     */
    @Transactional(readOnly = true) // ⭐ 기본적으로 읽기 전용 트랜잭션
    @Override
    public GuildServer findOrCreateGuildServer(Long serverId) {
        // 1. 읽기 전용 트랜잭션 내에서 서버를 찾습니다.
        return guildServerRepository.findById(serverId)
                // 2. 서버가 없으면, 쓰기 작업이 필요한 생성 메서드를 REQUIRES_NEW 트랜잭션으로 호출
                .orElseGet(() -> createNewGuildServer(serverId));
    }

    /**
     * GuildServer를 생성하고 저장하는 트랜잭션 (REQUIRES_NEW).
     * 이 메서드는 findOrCreateGuildServer 트랜잭션과 완전히 분리되어 충돌을 일으키지 않습니다.
     */
    @Transactional
    public GuildServer createNewGuildServer(Long serverId) {
        log.info("새로운 GuildServer 등록: ID={}", serverId);
        GuildServer newServer = new GuildServer();

        // ⭐ 수정: Discord ID를 올바른 필드에 매핑
        newServer.setDiscordServerId(serverId);

        // ⭐ 필수: serverName이 nullable=false이므로 임시 값 설정 (실제로는 JDA로 조회해야 함)
        newServer.setServerName("Unknown Server - " + serverId);

        // newServer.setId()는 생략되어 DB가 자동으로 생성하도록 둡니다.
        return guildServerRepository.save(newServer);
    }
}
