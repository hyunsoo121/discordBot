package com.discordBot.demo.domain.dto;

import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * '/search' 명령어의 최종 결과 DTO.
 * Riot 랭크 정보와 내전 통계 및 지표별 순위를 통합합니다.
 */
@Getter
@Builder
public class UserSearchDto {

    // Discord 및 LoL 식별 정보
    private Long discordUserId;
    private String summonerName;

    // ⭐ 1. 라이엇 공식 랭크 정보 (Riot API)
    private String soloRankTier;
    private String flexRankTier;

    // ⭐ 2. 내전 기본 통계 수치 (UserRankDto에서 복사)
    private Double kda;
    private Double gpm;
    private Double dpm;
    private Double winRate;
    private Double killParticipation;
    private Integer totalGames;

    // ⭐ 3. 지표별 서버 순위 (Map<Criterion, Rank>)
    // KDA, GPM 등 각 지표에 대한 해당 서버 내의 순위를 저장합니다.
    private Map<RankingCriterion, Integer> metricRanks;
}