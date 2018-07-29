package com.github.johnnyjayjay.discord.jshell;

import net.dv8tion.jda.core.entities.MessageEmbed;

public interface Evaluator {

    MessageEmbed eval(String input);

}
