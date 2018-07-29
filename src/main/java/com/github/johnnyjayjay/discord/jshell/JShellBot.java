package com.github.johnnyjayjay.discord.jshell;

import com.github.johnnyjayjay.discord.commandapi.CommandSettings;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;

import javax.security.auth.login.LoginException;

public class JShellBot {

    public void start() throws LoginException, InterruptedException {
        var jda = new JDABuilder(AccountType.BOT)
                .setToken("NOPE")
                .buildBlocking();
        var jShell = new DiscordJShell();
        var evalCommand = new EvalCommand(jShell);
        new CommandSettings(".", jda, true).put(evalCommand, "j", "eval", "jshell").activate();
    }

}
