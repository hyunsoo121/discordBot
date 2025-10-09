package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.entity.MatchRecord;

public interface MatchRecordService {

    MatchRecord registerMatch(MatchRegistrationDto matchDto);
}
