package com.discordBot.demo.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "CHAMPION")
public class Champion {
    @Id
    private Long id; // Riot Games 챔피언 ID (고유 키)
    private String name; // 챔피언의 현재 이름 (예: Lux)
    private String key;  // 챔피언의 영문 Key (예: Lux)
    private String primaryRole; // 주 포지션
}