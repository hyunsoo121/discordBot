package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.ChampionSearchDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;

import java.util.List;

public interface ChampionStatsService {

    /**
     * 매치 등록 시 호출되어 유저의 챔피언별 통계를 누적 업데이트합니다.
     * @param championName 추출된 챔피언 이름
     * @param assumedLine 유저가 플레이했다고 입력한 라인 (TOP, JUNGLE 등)
     * @param userId Discord User ID
     * @param serverId Discord Server ID
     * @param playerStatsDto 해당 유저의 경기 상세 통계 (K/D/A, 골드, 피해량)
     * @param isWin 해당 경기의 승리 여부
     * @param gameDurationSeconds 경기의 총 시간 (초 단위)
     * @param teamTotalKills 해당 유저 팀의 총 킬 수
     */
    void updateChampionStats(
            String championName,
            Long userId,
            Long serverId,
            PlayerStatsDto playerStatsDto,
            boolean isWin,
            long gameDurationSeconds,
            int teamTotalKills
    );

    /**
     * 유저 전적 검색 시 호출되어 해당 유저의 모든 챔피언 통계를 조회합니다.
     * @param userId Discord User ID
     * @param serverId Discord Server ID
     * @return ChampionSearchDto 목록 (계산된 KDA, GPM 등 포함)
     */
    List<ChampionSearchDto> getUserChampionStats(Long userId, Long serverId);
}