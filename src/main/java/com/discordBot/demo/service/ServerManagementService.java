package com.discordBot.demo.service;

import com.discordBot.demo.domain.entity.GuildServer;

public interface ServerManagementService {

    /**
     * 서버 ID를 통해 GuildServer를 찾거나, 없으면 새로운 트랜잭션 내에서 생성합니다.
     * 이 메서드는 읽기 전용(readOnly)으로 동작하여 트랜잭션 충돌을 최소화합니다.
     * @param serverId Discord 서버 ID
     * @return GuildServer 엔티티
     */
    GuildServer findOrCreateGuildServer(Long serverId);
}
