package com.elegantbanshee.util;

import com.elegantbanshee.RedditSlideshowServer;

public class LoginThread extends Thread {
    public static String bearerToken = "";
    public static String refreshToken = "";
    public static long lastRefreshTime = 0;

    public LoginThread() {
        super();
        setName("Login-Thread");
    }

    @Override
    public void run() {
        super.run();
        while (true) {
            if (shouldRefreshAccessToken()) {
                try {
                    RedditSlideshowServer.refreshAccessToken();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(60 * 1000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean shouldRefreshAccessToken() {
        return !LoginThread.refreshToken.isEmpty() && System.currentTimeMillis() - LoginThread.lastRefreshTime > 30 * 60 * 1000;
    }
}
