package com.github.johnnyjayjay.discord.jshell;

import jdk.jshell.Snippet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class EvalCommand extends ListenerAdapter {

    private final Logger logger;
    private final ExecutorService executorService; // mehrere Threads mit einem cache, der id und zugehöriges future speichert
    private final MessageEmbed help;
    private DiscordJShell discordJShell;
    private Future<?> running;

    public EvalCommand(DiscordJShell discordJShell) {
        logger = LoggerFactory.getLogger(getClass());
        executorService = Executors.newSingleThreadExecutor();
        help = new EmbedBuilder().setColor(Color.ORANGE).setDescription("With me, you can evaluate Java code like never before!")
                .setTitle("Help").addField("How to", "Evaluate code by just typing `" + JShellBot.PREFIX + " <code>`." +
                        "You will then get a response whether it was successful.\nYou may execute multiple statements by putting them in { curly braces }.\n" +
                        "You may also put your code in a code block: \n\\```java\n// your code here\n\\```\n" +
                        "You can declare and initialize variables, declare types, define methods - everything that is possible in Java 10.\n" +
                        "You are given access to the standard Java SE libraries. You may also add imports from these modules.\n" +
                        "Snippets are saved as long as the bot runs.\n" +
                        "This bot supports the newest Java Version, Java 10, but as usual everything below is compatible as well.", false)
                .addField("Commands", "```\ndrop:<id> - Drops the snippet with the id provided\nshow:<id> Shows the snippet with the id provided\n" +
                        "variables - Show all variable snippets\ntypes - Show all type snippets\nimports - Show all import snippets\nmethods - Show all method snippets\n" +
                        "stop - Aborts the current evaluation```To execute a command, type `" + JShellBot.PREFIX + "[command]`.", false).build();
        this.discordJShell = discordJShell;
        running = executorService.submit(() -> {});
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        String user = format("%#s", event.getAuthor());
        String msg = event.getMessage().getContentDisplay();
        if (msg.startsWith(JShellBot.PREFIX)) {
            msg = msg.replaceFirst(JShellBot.PREFIX + "(\\s+)?", "").replaceAll("[`]+(java)?", "");
            String firstArg = msg.split("\\s+")[0].toLowerCase();
            if (firstArg.matches("drop:\\d+")) {
                String id = firstArg.replaceFirst("drop:", "");
                if (discordJShell.drop(id)) {
                    logger.info(user + " dropped snippet " + id);
                    channel.sendMessage("Dropped snippet with id " + id).queue();
                } else {
                    channel.sendMessage("A snippet with this id doesn't exist!").queue();
                }
            } else if (firstArg.equals("variables")) {
                String variables = discordJShell.getVariables().map((var) -> format("§%s\t%s : %s", var.id(), var.name(), var.typeName())).collect(Collectors.joining("\n"));
                channel.sendMessage("Variables: ```java\n" + variables + "\n```").queue();
            } else if (firstArg.equals("types")) {
                String types = discordJShell.getTypes().map((type) -> format("$%s\t%s", type.id(), type.name())).collect(Collectors.joining("\n"));
                channel.sendMessage("Types: ```java\n" + types + "\n```").queue();
            } else if (firstArg.equals("methods")) {
                String methods = discordJShell.getMethods().map((method) -> format("$%s\t%s : %s", method.id(), method.signature(), method.name())).collect(Collectors.joining("\n"));
                channel.sendMessage("Methods: ```java\n" + methods + "\n```").queue();
            } else if (firstArg.equals("imports")) {
                String imports = discordJShell.getImports().map((importSn) -> format("$%s\t%s", importSn.id(), importSn.fullname())).collect(Collectors.joining("\n"));
                channel.sendMessage("Imports: ```java\n" + imports + "\n```").queue();
            } else if (firstArg.matches("show:\\d+")) {
                Snippet snippet = discordJShell.getSnippet(firstArg.replaceFirst("show:", ""));
                if (snippet == null) {
                    channel.sendMessage("A snippet with this id doesn't exist!").queue();
                } else {
                    channel.sendMessage("Snippet " + snippet.id() + ": ```java\n" + snippet.source() + "``` Status: " + discordJShell.getStatus(snippet)).queue();
                }
            } /*else if (firstArg.equals("reset")) {
                logger.info(user + " reset JShell");
                executorService.shutdownNow();
                discordJShell.resetJShell();
                channel.sendMessage("Reset JShell").queue();
            }*/ else if (firstArg.equals("stop")) {
                if (!running.isCancelled() && !running.isDone()) {
                    logger.info(user + " tried stopping evaluation");
                    running.cancel(true);
                    channel.sendMessage("Stopped current evaluation!").queue();
                } else {
                    channel.sendMessage("There is no evaluation to stop at the moment.").queue();
                }
            } else {
                String finalMsg = msg;
                logger.info(user + " evaluated code: " + finalMsg.replaceAll("\\s+", " "));
                if (running.isDone() || running.isCancelled())
                    running = executorService.submit(() -> channel.sendMessage(discordJShell.eval(finalMsg)).queue());
                else
                    channel.sendMessage("Please wait until the current evaluation has finished or stop it with `" + JShellBot.PREFIX + "stop`").queue();
            }
        } else {
            List<Member> mentionedMembers = event.getMessage().getMentionedMembers();
            if (!mentionedMembers.isEmpty() && mentionedMembers.get(0) == event.getGuild().getSelfMember()) {
                channel.sendMessage("Hello, " + event.getAuthor().getAsMention() + "!").queue();
                channel.sendMessage(help).queue();
            }
        }
    }

}
