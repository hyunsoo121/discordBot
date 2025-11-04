package com.discordBot.demo.domain.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "CHAMPION")
public class Champion {

    // PK: Riot API 챔피언 고유 ID
    @Id
    @Column(name = "champion_id")
    @JsonProperty("key") //
    private Long championId;

    // 챔피언의 현재 이름 (표시용)
    @Column(name = "name", nullable = false, length = 50)
    @JsonProperty("name")
    private String name;

    // 챔피언의 영문 고유 식별자 (불변 키)
    @Column(name = "champion_key", nullable = false, length = 50, unique = true)
    private String championKey;

}