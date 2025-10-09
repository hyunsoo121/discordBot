package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.MatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRecordRepository extends JpaRepository<MatchRecord, Long> {
}
