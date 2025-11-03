package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Riot Games의 공식 챔피언 데이터를 저장하는 엔티티.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "CHAMPION")
public class Champion {

    // PK: Riot API 챔피언 고유 ID
    @Id
    @Column(name = "champion_id")
    private Long championId;

    // 챔피언의 현재 이름 (표시용 ex: 아트록스)
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // 챔피언의 영문 고유 식별자 (불변 키)
    @Column(name = "champion_key", nullable = false, length = 50, unique = true)
    private String championKey;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "CHAMPION_LINE", // 중간 테이블 이름
            joinColumns = @JoinColumn(name = "champion_id"),
            inverseJoinColumns = @JoinColumn(name = "line_id")
    )
    private Set<Line> lines; // 챔피언이 갈 수 있는 라인 목록
}