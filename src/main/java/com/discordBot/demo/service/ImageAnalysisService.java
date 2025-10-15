package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.entity.LolAccount;
import java.util.List;

public interface ImageAnalysisService {
    MatchRegistrationDto analyzeAndStructureData(String imageUrl, Long serverId, List<LolAccount> registeredAccounts) throws Exception;
}