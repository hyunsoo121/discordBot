package com.discordBot.demo.domain.dto;

import lombok.Data;

@Data
public class PlayerStatsDto {

    private Long discordUserId;

    private String lolGameName;
    private String lolTagLine;

    private String team; // "RED" 또는 "BLUE"
    private Integer kills;
    private Integer deaths;
    private Integer assists;
}