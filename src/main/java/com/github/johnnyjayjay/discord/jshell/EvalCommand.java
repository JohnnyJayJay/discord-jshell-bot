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
            channel.sendMessage("This is not allowed!").queue();
            return;
        }
        
        String firstArg = args[0].toLowerCase();
        if (firstArg.matches("drop:\\d+")) {
            String id = firstArg.substring(5);
            if (discordJShell.drop(id)) {
                channel.sendMessage("Dropped snippet with id " + id).queue();
            } else {
                channel.sendMessage("A snippet with this id doesn't exist!").queue();
            }
        } else if (firstArg.equals("variables")) {
            String variables = discordJShell.getVariables().map((var) -> String.format("§%s\t%s : %s", var.id(), var.name(), var.typeName())).collect(Collectors.joining("\n"));
            channel.sendMessage("Variables: ```java\n" + variables + "\n```").queue();
        } else if (firstArg.equals("types")) {
            String types = discordJShell.getTypes().map((type) -> String.format("$%s\t%s", type.id(), type.name())).collect(Collectors.joining("\n"));
            channel.sendMessage("Types: ```java\n" + types + "\n```").queue();
        } else if (firstArg.equals("methods")) {
            String methods = discordJShell.getMethods().map((method) -> String.format("$%s\t%s : %s", method.id(), method.signature(), method.name())).collect(Collectors.joining("\n"));
            channel.sendMessage("Methods: ```java\n" + methods + "\n```").queue();
        } else if (firstArg.equals("imports")) {
            String imports = discordJShell.getImports().map((importSn) -> String.format("$%s\t%s", importSn.id(), importSn.fullname())).collect(Collectors.joining("\n"));
            channel.sendMessage("Imports: ```java\n" + imports + "\n```").queue();
        } else if (firstArg.matches("show:\\d+")) {
            Snippet snippet = discordJShell.getSnippet(firstArg.substring(5));
            if (snippet == null) {
                channel.sendMessage("A snippet with this id doesn't exist!").queue();
            } else {
                channel.sendMessage("Snippet " + snippet.id() + ": ```java\n" + snippet.source() + "``` Status: " + discordJShell.getStatus(snippet)).queue();
            }
        } else if (firstArg.equals("reset")) {
            discordJShell.resetJShell();
            channel.sendMessage("JShell was reset!").queue();
        } else if (firstArg.equals("stop")) {
            discordJShell.stopCurrentEval();
            channel.sendMessage("Stopped current evaluation!").queue();
        } else {
            Main.execute(() -> channel.sendMessage(discordJShell.eval(joinedArgs)).queue());
        }
    }
}
