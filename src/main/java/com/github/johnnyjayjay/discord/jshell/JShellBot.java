package com.github.johnnyjayjay.discord.jshell;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;

import javax.security.auth.login.LoginException;

public class JShellBot {

    public static final String PREFIX = "jshell:";

    public void start() throws LoginException, InterruptedException {
        var jShell = new DiscordJShell();
        new JDABuilder(AccountType.BOT)
                .setToken("NOPE")
                .setGame(Game.playing("Ping me for help!"))
                .addEventListener(new EvalCommand(jShell))
                .buildBlocking();
    }

}
