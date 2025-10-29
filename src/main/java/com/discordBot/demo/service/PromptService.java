package com.discordBot.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class PromptService {

    @Value("classpath:prompts/match_data_prompt.txt")
    private Resource matchDataPromptResource;

    private String matchDataPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            this.matchDataPromptTemplate = StreamUtils.copyToString(
                    matchDataPromptResource.getInputStream(),
                    StandardCharsets.UTF_8
            );

            System.out.println("✅ TXT 파일 로드 완료: matchDataPromptTemplate에 저장됨.");

        } catch (IOException e) {
            System.err.println("❌ 리소스 파일 로드 실패: " + matchDataPromptResource.getFilename());
            e.printStackTrace();
            throw new RuntimeException("Prompt file initialization failed.", e);
        }
    }

    public String getMatchDataPromptTemplate() {
        return matchDataPromptTemplate;
    }
}
