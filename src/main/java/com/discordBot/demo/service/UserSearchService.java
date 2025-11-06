package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.UserSearchDto;
import java.util.Optional;

public interface UserSearchService {

    Optional<UserSearchDto> searchUserInternalStatsByDiscordId(Long discordUserId, Long serverId);

    Optional<UserSearchDto> searchUserInternalStatsByRiotId(String summonerName, String tagLine, Long serverId);
}