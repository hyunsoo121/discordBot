package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByDiscordUserId(Long discordUserId);
}
