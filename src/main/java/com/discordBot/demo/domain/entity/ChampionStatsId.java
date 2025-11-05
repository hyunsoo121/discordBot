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
@EqualsAndHashCode
public class ChampionStatsId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long user;             // User 엔티티의 ID (Discord User ID)
    private Long guildServer;      // GuildServer 엔티티의 ID (Discord Server ID)
    private Long champion;   // 챔피언 이름 (고유 키의 일부)
}