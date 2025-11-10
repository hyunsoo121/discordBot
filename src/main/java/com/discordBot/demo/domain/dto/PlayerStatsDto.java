package com.discordBot.demo.domain.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class PlayerStatsDto {

    private Long discordUserId;

    @JsonProperty("gameName")
    private String lolGameName;

    @JsonProperty("tagLine")
    private String lolTagLine;

    private String team;

    @JsonProperty("championName")
    private String championName;

    private String laneName;

    private int kills;
    private int deaths;
    private int assists;

    private int totalGold;
    private int totalDamage;

    private long durationSeconds;
}