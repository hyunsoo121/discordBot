package com.discordBot.demo.domain.enums;

public enum RankingCriterion {
    // 랭킹 정렬 기준
    WIN_RATE("승률"), // 현재 기본 정렬
    KDA("KDA"),
    GAMES("총 게임 수"),

    // 상세 보기에서 사용될 수 있는 추가 지표
    GPM("분당 골드"),
    DPM("분당 피해량"),
    KP("킬 관여율");

    private final String displayName;

    RankingCriterion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}