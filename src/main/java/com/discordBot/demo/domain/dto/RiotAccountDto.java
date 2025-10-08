package com.discordBot.demo.domain.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RiotAccountDto {

    /**
     * Riot Games 계정의 고유 식별자 (Encrypted PUUID).
     * Riot API의 여러 엔드포인트에서 사용자 식별의 핵심 키로 사용됩니다.
     */
    private String puuid;

    /**
     * Riot Games 계정의 현재 게임 이름 (예: "Hide on bush").
     */
    private String gameName;

    /**
     * Riot Games 계정의 현재 태그 라인 (예: "KR1").
     * 고유성을 확보하는 데 사용됩니다.
     */
    private String tagLine;
}