package com.discordBot.demo.domain.dto;

import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.Builder;
import lombok.Getter;

import java.util.List; // List 임포트 추가
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
    private String lolTagLine; // Riot API에서 가져온 TagLine을 저장할 필드 추가 (검색 결과 명확화를 위해)

    // ⭐ 1. 라이엇 공식 랭크 정보 (Riot API)
    private String soloRankTier;
    private String flexRankTier;

    // ⭐ 2. 내전 기본 통계 수치
    private Double kda;
    private Double gpm;
    private Double dpm;
    private Double winRate;
    private Double killParticipation;
    private Integer totalGames;

    // ⭐ 3. 지표별 서버 순위
    private Map<RankingCriterion, Integer> metricRanks;

    // ⭐ 4. 챔피언별 상세 통계 목록
    private List<ChampionSearchDto> championStatsList;
}