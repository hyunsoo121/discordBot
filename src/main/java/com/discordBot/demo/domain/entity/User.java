package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "USERS")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_user_id", unique = true, nullable = false)
    private Long discordUserId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "USER_GUILD",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "server_id")
    )
    private Set<GuildServer> guildServers = new HashSet<>();
}