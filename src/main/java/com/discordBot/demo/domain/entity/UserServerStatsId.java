package com.discordBot.demo.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode // 복합 키 비교를 위해 필수
public class UserServerStatsId implements Serializable {

    private static final long serialVersionUID = 1L;

    // User 엔티티의 ID (Discord User ID)
    private Long user;

    // GuildServer 엔티티의 ID (Discord Server ID)
    private Long guildServer;
}