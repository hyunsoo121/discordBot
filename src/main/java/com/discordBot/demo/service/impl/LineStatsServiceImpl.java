package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.*;
import com.discordBot.demo.domain.repository.LineRepository;
import com.discordBot.demo.domain.repository.LineStatsRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.service.LineStatsService;
import com.discordBot.demo.service.ServerManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LineStatsServiceImpl implements LineStatsService {

    private final LineRepository lineRepository;
    private final LineStatsRepository lineStatsRepository;
    private final UserRepository userRepository;
    private final ServerManagementService serverManagementService;


    @Override
    public void updateLineStats(
            Long userId,
            Long serverId,
            PlayerStatsDto playerStatsDto,
            boolean isWin,
            long gameDurationSeconds,
            int teamTotalKills
    ) {
        // 1. Line 엔티티 조회
        Line line = lineRepository.findByName(playerStatsDto.getLaneName().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("❌ 라인 [" + playerStatsDto.getLaneName() + "] 정보를 찾을 수 없습니다."));

        // 2. 대상 유저 및 서버 엔티티 조회
        User user = userRepository.findByDiscordUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 사용자 ID를 찾을 수 없습니다: " + userId));

        GuildServer guildServer = serverManagementService.findOrCreateGuildServer(serverId);


        // 3. 기존 LineStats 레코드 조회
        // Repository에 정의된 복합 키 조회 메서드를 사용합니다.
        LineStats stats = lineStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerIdAndLine_LineId(
                userId,
                serverId,
                line.getLineId()
        ).orElseGet(() -> createNewLineStats(user, guildServer, line));

        // 4. 통계 누적
        // LineStats 엔티티의 addStats 메서드가 모든 파라미터를 정확히 받도록 가정합니다.
        stats.addStats(
                playerStatsDto.getKills(),
                playerStatsDto.getDeaths(),
                playerStatsDto.getAssists(),
                isWin,
                playerStatsDto.getTotalGold(),
                playerStatsDto.getTotalDamage(),
                teamTotalKills,
                gameDurationSeconds
        );

        lineStatsRepository.save(stats);
    }

    /**
     * 새로운 LineStats 레코드를 생성하고 복합 키를 설정합니다.
     */
    private LineStats createNewLineStats(User user, GuildServer guildServer, Line line) {
        // LineStatsId를 생성하여 PK 필드를 설정
        LineStatsId id = new LineStatsId();
        id.setUser(user.getDiscordUserId()); // User ID 설정
        id.setGuildServer(guildServer.getDiscordServerId()); // GuildServer ID 설정
        id.setLineId(line.getLineId()); // Line ID 설정

        LineStats newStats = new LineStats();
        // ID 필드 및 관계 엔티티 설정
        newStats.setId(id);
        newStats.setUser(user);
        newStats.setGuildServer(guildServer);
        newStats.setLine(line);

        return newStats;
    }
}