package com.discordBot.demo.service;

import com.discordBot.demo.domain.entity.LolNickname;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.dto.RiotAccountDto;

import com.discordBot.demo.domain.repository.LolNicknameRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LolNicknameRepository lolNicknameRepository;
    private final RiotApiService riotApiService;

    @Override
    @Transactional
    public String registerLolNickname(Long discordUserId, String gameName, String tagLine) {

        String fullNickname = gameName + "#" + tagLine;

        // 1. Riot API 검증
        Optional<RiotAccountDto> riotAccountOpt = riotApiService.verifyNickname(gameName, tagLine);

        if (riotAccountOpt.isEmpty()) {
            return "❌ 오류: 해당 롤 닉네임(" + fullNickname + ")은 존재하지 않습니다.";
        }

        // 2. Discord User 조회 또는 생성
        User user = userRepository.findByDiscordUserId(discordUserId)
                .orElseGet(() -> {
                    // (Optional) 디스코드 봇에서 유저 정보를 가져와서 User 엔티티 생성
                    User newUser = new User();
                    newUser.setDiscordUserId(discordUserId);
                    // Riot API 응답에서 받은 gameName을 기본으로 사용
                    newUser.setUsername(riotAccountOpt.get().getGameName());
                    return userRepository.save(newUser);
                });

        // 3. 닉네임 등록 (중복 방지 로직)
        // 해당 닉네임이 DB에 이미 등록되어 있는지 확인
        if (lolNicknameRepository.existsByNickname(fullNickname)) {
            // 해당 닉네임을 소유한 유저의 디스코드 ID를 가져와서
            // 현재 등록하려는 유저와 비교하는 로직을 추가하여 '본인 재등록'은 허용하고,
            // '타인 중복 등록'만 막는 것이 더 정확합니다. (현재는 타인 중복만 가정하고 막음)
            return "⚠️ 경고: 해당 롤 닉네임은 이미 다른 디스코드 계정에 등록되어 있습니다.";
        }

        // 4. 새로운 LolNickname 엔티티 생성 및 저장
        LolNickname newNickname = new LolNickname();
        newNickname.setUser(user);
        newNickname.setNickname(fullNickname);

        // 🚩 대표 닉네임 (isMain) 관련 로직은 모두 제거됨

        lolNicknameRepository.save(newNickname);

        return "✅ 성공: 롤 닉네임 '" + newNickname.getNickname() + "'이(가) 성공적으로 등록되었습니다.";
    }
}