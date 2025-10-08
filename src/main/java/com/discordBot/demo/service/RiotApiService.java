package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.RiotAccountDto;

import java.util.Optional;

public interface RiotApiService {
    Optional<RiotAccountDto> verifyNickname(String gameName, String tagLine);

}
