package com.discordBot.demo.service;

import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.dto.RiotAccountDto;

import com.discordBot.demo.domain.repository.LolAccountRepository;
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
    private final LolAccountRepository lolAccountRepository;
    private final RiotApiService riotApiService;

    @Override
    @Transactional
    public String registerLolNickname(Long discordUserId, String gameName, String tagLine) {

        String fullNickname = gameName + "#" + tagLine;

        Optional<RiotAccountDto> riotAccountOpt = riotApiService.verifyNickname(gameName, tagLine);

        if (riotAccountOpt.isEmpty()) {
            return "❌ 오류: 해당 롤 계정(" + fullNickname + ")은 존재하지 않습니다. Riot ID를 확인해 주세요.";
        }

        // Riot API 응답에서 받은 Account DTO
        RiotAccountDto riotAccount = riotAccountOpt.get(); // 검증 성공했으므로 Optional 값 추출

        // 2. Discord User 조회 또는 생성
        User user = userRepository.findByDiscordUserId(discordUserId)
                .orElseGet(() -> {
                    // 새 User 생성 (User 엔티티에 username이 없다고 가정하고 discordUserId만 설정)
                    User newUser = new User();
                    newUser.setDiscordUserId(discordUserId);
                    return userRepository.save(newUser);
                });

        // 3. 중복 등록 방지
        // 3-A. 현재 유저가 이미 이 계정을 등록했는지 확인
        if (lolAccountRepository.existsByUserAndGameNameAndTagLine(user, gameName, tagLine)) {
            return "⚠️ 경고: 당신의 롤 계정(" + fullNickname + ")은 이미 등록되어 있습니다.";
        }

        // 3-B. 다른 유저가 이 계정을 이미 등록했는지 확인
        Optional<LolAccount> existingLolAccount = lolAccountRepository.findByGameNameAndTagLine(gameName, tagLine);
        if (existingLolAccount.isPresent()) {
            // 3-A에서 걸러지지 않은, 다른 Discord 유저에게 등록된 경우
            return "⚠️ 경고: 해당 롤 계정(" + fullNickname + ")은 이미 다른 디스코드 계정에 등록되어 있습니다.";
        }


        // 4. 새로운 LolAccount 엔티티 생성 및 저장
        LolAccount newAccount = new LolAccount();
        newAccount.setUser(user);
        newAccount.setGameName(gameName);
        newAccount.setTagLine(tagLine);
        newAccount.setPuuid(riotAccount.getPuuid());

        lolAccountRepository.save(newAccount);

        // LolAccount 엔티티의 Getter를 사용하여 전체 계정명을 가져옵니다.
        return "✅ 성공: 롤 계정 '" + newAccount.getFullAccountName() + "'이(가) 성공적으로 등록되었습니다.";
    }
}