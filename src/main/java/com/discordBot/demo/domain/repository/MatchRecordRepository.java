package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {
    Optional<MatchRecord> findByGameDurationSecondsAndBlueTotalGoldAndRedTotalGoldAndGuildServer_DiscordServerId(
            int gameDurationSeconds,
            int blueTotalGold,
            int redTotalGold,
            Long discordServerId
    );
}
