package com.discordBot.demo.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class MatchRegistrationDto {

    private Long serverId;

    // 승리 팀 ("RED" 또는 "BLUE")
    private String winnerTeam;

    // 10명의 선수 통계 목록
    private List<PlayerStatsDto> playerStatsList;
}