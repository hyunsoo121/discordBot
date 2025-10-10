package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor; // NoArgsConstructor 추가

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor // Lombok의 NoArgsConstructor 추가
@Table(name = "GUILD_SERVER")
public class GuildServer {

    // ⭐ 1. DB 자동 생성 ID 필드를 제거합니다.
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    // private Long id;

    // ⭐ 2. Discord 서버 ID를 Primary Key로 사용합니다.
    @Id // Discord ID를 PK로 지정
    @Column(name = "id", unique = true, nullable = false) // 컬럼명을 'id'로 변경
    private Long discordServerId;

    // ⭐ 3. serverName은 필수 필드로 유지합니다. (실제 사용 시)
    @Column(name = "server_name", nullable = false, length = 100)
    private String serverName = "Unknown Server"; // 기본값 설정으로 NOT NULL 충족

    @ManyToMany(mappedBy = "guildServers", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    // 편의상 Getter/Setter 이름을 유지하면서 PK에 매핑되도록 생성자를 추가합니다.
    public void setId(Long discordServerId) {
        this.discordServerId = discordServerId;
    }
    public Long getId() {
        return this.discordServerId;
    }
}
