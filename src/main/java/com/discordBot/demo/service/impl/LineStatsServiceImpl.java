// com.discordBot.demo.service.impl.LineStatsServiceImpl.java

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
        // 1. Line 엔티티 조회 (로직 유지)
        Line line = lineRepository.findByName(playerStatsDto.getLaneName().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("❌ 라인 [" + playerStatsDto.getLaneName() + "] 정보를 찾을 수 없습니다."));

        // 2. 대상 유저 및 서버 엔티티 조회 (로직 유지)
        User user = userRepository.findByDiscordUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 사용자 ID를 찾을 수 없습니다: " + userId));

        GuildServer guildServer = serverManagementService.findOrCreateGuildServer(serverId);


        // 3. 기존 LineStats 레코드 조회 (로직 유지)
        LineStatsId id = new LineStatsId();
        id.setUser(userId);
        id.setGuildServer(serverId);
        id.setLineId(line.getLineId());

        LineStats stats = lineStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerIdAndLine_LineId(
                userId,
                serverId,
                line.getLineId() // Long lineId
        ).orElseGet(() -> createNewLineStats(user, guildServer, line));

        // 4. 통계 누적
        // LineStats 엔티티의 addStats 메서드도 teamTotalKills를 받도록 수정되어야 합니다.
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
     * 새로운 LineStats 레코드를 생성하고 복합 키를 설정합니다. (로직 유지)
     */
    private LineStats createNewLineStats(User user, GuildServer guildServer, Line line) {
        LineStatsId id = new LineStatsId();
        id.setUser(user.getDiscordUserId());
        id.setGuildServer(guildServer.getDiscordServerId());
        id.setLineId(line.getLineId());

        LineStats newStats = new LineStats();
        newStats.setId(id);
        newStats.setUser(user);
        newStats.setGuildServer(guildServer);
        newStats.setLine(line);

        return newStats;
    }
}