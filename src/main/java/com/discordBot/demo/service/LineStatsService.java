package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.PlayerStatsDto;

public interface LineStatsService {

    /**
     * 경기 후 유저의 라인별 통계 (LineStats)를 업데이트합니다.
     */
    void updateLineStats(
            Long userId,
            Long serverId,
            PlayerStatsDto playerStatsDto,
            boolean isWin,
            long gameDurationSeconds,
            int teamTeamKills
    );
}