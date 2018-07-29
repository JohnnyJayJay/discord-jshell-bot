package com.github.johnnyjayjay.discord.jshell;

import com.github.johnnyjayjay.discord.commandapi.CommandEvent;
import com.github.johnnyjayjay.discord.commandapi.ICommand;
import jdk.jshell.Snippet;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.stream.Collectors;

public class EvalCommand implements ICommand {

    private DiscordJShell discordJShell;

    public EvalCommand(DiscordJShell discordJShell) {
        this.discordJShell = discordJShell;
    }

    @Override
    public void onCommand(CommandEvent event, Member member, TextChannel channel, String[] args) {
        if (args.length == 0)
            return;

        String joinedArgs = event.getCommand().getJoinedArgs();
        if (joinedArgs.matches("(?i).*System.exit\\(-?\\d+\\);?.*")) {
            channel.sendMessage("").queue();
            return;
        }
        
        String firstArg = args[0].toLowerCase();
        // TODO: 29.07.2018  
        if (firstArg.matches("drop:\\d+")) {
            String id = firstArg.substring(5);
            if (discordJShell.drop(id)) {
                channel.sendMessage("Dropped snippet with id " + id).queue();
            } else {
                channel.sendMessage("A snippet with this id doesn't exist!").queue();
            }
        } /*else if (firstArg.equals("variables")) {
            String variables = discordJShell.getVariables().map((var) -> String.format("%s : %s (%s)", var.name(), var.typeName(), var.id())).collect(Collectors.joining("\n"));
            channel.sendMessage("Variables (name : type (id)): ```java\n" + variables + "```").queue();
        } else if (firstArg.equals("types")) {
            String types = discordJShell.getTypes().map((type) -> String.format("%s (%s)", type.name(), type.id())).collect(Collectors.joining("\n"));
            channel.sendMessage("Types (name (id)): ```java\n" + types + "```").queue();
        } else if (firstArg.equals("methods")) {
            String methods = discordJShell.getMethods().map((method) -> String.format("%s (%s)", method.signature(), method.id())).collect(Collectors.joining("\n"));
            channel.sendMessage("Methods: ```java\n" + methods + "```").queue();
        }*/ else if (firstArg.matches("show:\\d+")) {
            Snippet snippet = discordJShell.getSnippet(firstArg.substring(5));
            if (snippet == null) {
                channel.sendMessage("This snippet does not exist.").queue();
            } else {
                channel.sendMessage("Snippet " + snippet.id() + ": ```java\n" + snippet.source() + "``` Status: " + discordJShell.getStatus(snippet)).queue();
            }
        } else if (firstArg.equals("reset")) {
            discordJShell.resetJShell();
            channel.sendMessage("JShell was reset").queue();
        } else {
            Main.execute(() -> channel.sendMessage(discordJShell.eval(joinedArgs)).queue());
        }
    }
}
