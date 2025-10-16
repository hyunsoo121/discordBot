package com.discordBot.demo.domain.dto;

import com.discordBot.demo.domain.entity.PlayerStats;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchRegistrationDto {

    private Long serverId;

    // ⭐ ImageAnalysisServiceImpl에서 자동 확정된 값
    private String winnerTeam;

    // ⭐ ImageAnalysisServiceImpl에서 추가로 확정된 값 (1팀이 BLUE인지 RED인지)
    private String team1Side;

    // ⭐⭐ 중복 검사 및 DB 저장을 위한 추가 필드 ⭐⭐

    // 1. 경기 지속 시간 (초)
    private int gameDurationSeconds;

    // 2. 블루팀 총 골드 합산
    private int blueTotalGold;

    // 3. 레드팀 총 골드 합산
    private int redTotalGold;

    // 선수별 상세 통계
    private List<PlayerStatsDto> playerStatsList;
}