package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.GuildServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildServerRepository extends JpaRepository<GuildServer, Long> {
}
