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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "LINE")
public class Line {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    // 라인 이름 (예: "TOP", "JUNGLE" - DB에서 사용하는 고유 키)
    @Column(name = "name", nullable = false, length = 20, unique = true)
    private String name;

    // 사용자에게 보여줄 이름 (예: "탑", "정글")
    @Column(name = "display_name", nullable = false, length = 20)
    private String displayName;
}