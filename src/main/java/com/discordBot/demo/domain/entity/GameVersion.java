package com.discordBot.demo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Riot Data Dragon의 현재 게임 버전을 저장하여 업데이트 여부를 추적합니다.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "GAME_VERSION")
public class GameVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 현재 저장된 Riot Game 버전 (예: "14.21.1")
    @Column(name = "version", nullable = false, length = 20, unique = true)
    private String version;

    // 마지막 업데이트 시간 (버전 비교가 어려울 때 최신 레코드를 찾기 위해 사용)
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate = LocalDateTime.now();
}
