package com.github.johnnyjayjay.discord.jshell;

import javax.security.auth.login.LoginException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws LoginException, InterruptedException {
        new JShellBot().start();
    }

    public static void execute(Runnable task) {
        executorService.execute(task);
    }

}
