package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {

    /**
     * 중복 검사를 위한 최종 메서드 (규칙 기반 쿼리)
     * - 동일 서버, 동일 경기 시간, 동일 팀별 골드 합산 여부를 확인합니다.
     * @param gameDurationSeconds 경기 지속 시간 (초)
     * @param blueTotalGold 블루팀 총 골드 합산
     * @param redTotalGold 레드팀 총 골드 합산
     * @param discordServerId 경기가 진행된 디스코드 서버 ID
     * @return 일치하는 MatchRecord가 있으면 Optional<MatchRecord> 반환
     */
    Optional<MatchRecord> findByGameDurationSecondsAndBlueTotalGoldAndRedTotalGoldAndGuildServer_DiscordServerId(
            int gameDurationSeconds,
            int blueTotalGold,
            int redTotalGold,
            Long discordServerId
    );
}