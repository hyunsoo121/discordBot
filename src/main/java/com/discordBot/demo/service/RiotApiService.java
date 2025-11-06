package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import com.discordBot.demo.domain.entity.Champion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RiotApiService {
    Optional<RiotAccountDto> verifyNickname(String gameName, String tagLine);

    String getLatestGameVersion();

    Map<String, Champion> getLatestChampionDataAsChampionEntity();

    String getSmiteIconUrl();

    List<String> getSupportItemIconUrls();
}
