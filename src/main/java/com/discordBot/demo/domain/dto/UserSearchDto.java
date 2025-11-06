package com.discordBot.demo.domain.dto;

import com.discordBot.demo.domain.enums.RankingCriterion;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * '/user-stats' 명령어의 최종 결과 DTO.
 */
@Getter
@Builder
public class UserSearchDto {

    // Discord 및 LoL 식별 정보
    private Long discordUserId;
    private String summonerName;
    private String lolTagLine;

    // ⭐ 추가: 연결된 모든 롤 계정 목록 (GameName#TagLine 형식)
    private List<String> linkedLolAccounts;

    // ⭐ 1. 라이엇 공식 랭크 정보 (내전 지표 검색에서는 N/A)
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