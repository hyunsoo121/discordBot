package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.LineStats;
import com.discordBot.demo.domain.entity.LineStatsId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LineStatsRepository extends JpaRepository<LineStats, LineStatsId> {

    /**
     * 특정 유저, 서버, 라인의 통계 기록을 조회합니다.
     * LineStatsId 복합 키를 구성하는 필드를 기반으로 조회 메서드를 정의합니다.
     */
    Optional<LineStats> findByUser_DiscordUserIdAndGuildServer_DiscordServerIdAndLine_LineId(
            Long discordUserId,
            Long discordServerId,
            Long lineId
    );

    // 이외에도 필요에 따라 라인별 전체 통계를 가져오는 메서드 등을 추가할 수 있습니다.
}