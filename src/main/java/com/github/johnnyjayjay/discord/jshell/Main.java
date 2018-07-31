package com.github.johnnyjayjay.discord.jshell;

import javax.security.auth.login.LoginException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException {
        new JShellBot().start();
    }

}
