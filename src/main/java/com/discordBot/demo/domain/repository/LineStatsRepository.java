package com.discordBot.demo.domain.repository;


import com.discordBot.demo.domain.entity.LineStats;
import com.discordBot.demo.domain.entity.LineStatsId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineStatsRepository extends JpaRepository<LineStats, LineStatsId> {

    /**
     * 특정 유저, 서버, 라인의 통계 기록을 조회합니다.
     * findById 대신 사용하여 복합 키 매핑 오류를 방지합니다.
     */
    Optional<LineStats> findByUser_DiscordUserIdAndGuildServer_DiscordServerIdAndLine_LineId(
            Long discordUserId,
            Long discordServerId,
            Long lineId
    );

    List<LineStats> findAllByGuildServer_DiscordServerIdAndLine_LineId(Long serverId, Long lineId);
}