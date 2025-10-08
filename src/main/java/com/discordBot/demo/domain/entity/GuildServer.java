package com.discordBot.demo.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "GUILD_SERVER")
public class GuildServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_server_id", unique = true, nullable = false)
    private Long discordServerId;

    @Column(name = "server_name", nullable = false, length = 100)
    private String serverName;

    @ManyToMany(mappedBy = "guildServers")
    private Set<User> users = new HashSet<>();
}