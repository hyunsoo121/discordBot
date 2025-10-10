package com.discordBot.demo.domain.dto;

import lombok.*;
// ⭐ Jackson 어노테이션 import 추가
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class PlayerStatsDto {

    private Long discordUserId;

    // ⭐ AI가 보내는 필드 이름(gameName)을 DTO 필드(lolGameName)에 매핑
    @JsonProperty("gameName")
    private String lolGameName;

    // ⭐ AI가 보내는 필드 이름(tagLine)을 DTO 필드(lolTagLine)에 매핑
    @JsonProperty("tagLine")
    private String lolTagLine;

    // 이 필드들은 이름이 일치하므로 @JsonProperty("team")는 생략 가능
    private String team;
    private Integer kills;
    private Integer deaths;
    private Integer assists;
}