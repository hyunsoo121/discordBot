package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.UserServerStats;

public interface UserServerStatsService {

    /**
     * 특정 경기의 결과를 기반으로 유저의 누적 통계를 업데이트하거나 새로 생성합니다.
     * 이 메서드는 MatchRecordService 트랜잭션 내에서 호출되어야 합니다.
     * * @param userId 통계를 업데이트할 디스코드 유저 ID
     * @param serverId 통계가 기록될 디스코드 서버 ID
     * @param playerStatsDto 해당 유저의 경기 상세 통계 (K/D/A)
     * @param isWin 해당 경기의 승리 여부
     * @return 업데이트되거나 새로 생성된 UserServerStats 엔티티
     */
    UserServerStats updateStatsAfterMatch(
            Long userId,
            Long serverId,
            PlayerStatsDto playerStatsDto,
            boolean isWin,
            long gameDurationSeconds,
            int teamTotalKils

    );
}