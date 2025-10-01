package com.discordBot.demo.listener;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class SlashCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()){
            case "my-info":
                event.reply("**아직 구현이 되지 않았습니다**").queue();
                break;
            case "rank-check":
                event.reply("**아직 구현이 되지 않았습니다**").queue();
                break;
            default:
                event.reply("오류").queue();
                break;
        }
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandDataList = new ArrayList<>();
        commandDataList.add(
                Commands.slash("my-info", "내 정보를 보여줍니다")
        );
        commandDataList.add(
                Commands.slash("rank-check", "내전 랭킹을 확인합니다")
        );

        event.getGuild().updateCommands().addCommands(commandDataList).queue();
    }
}
