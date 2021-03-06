package com.elegantbanshee;

import com.elegantbanshee.data.Constants;
import com.elegantbanshee.util.Logger;
import com.elegantbanshee.util.LoginThread;
import com.elegantbanshee.util.RedisUtil;

import java.util.logging.Level;

import static spark.Spark.*;

public class RedditSlideshow {

	public static void main(String[] args) {
		// Init logger
        Logger.setLevel(Level.parse("INFO"));
        Logger.info("Starting RedditSlideshow %s", Constants.VERSION);
        // Parse port
        int port = 5000;
        String portString = System.getenv("PORT");
        try {
            if (portString != null && !portString.isEmpty())
                port = Integer.parseInt(portString);
        }
        catch (NumberFormatException e) {
            Logger.warn("Failed to parse PORT env var: %s", portString);
        }
        // Set values
        port(port);
        staticFiles.location("/static/");
        staticFiles.expireTime(60 * 60); // One Week cache
        // Web
        RedditSlideshowServer.getGeneric("/", "index.hbs");
        RedditSlideshowServer.getGeneric("/r/:reddits", "index.hbs");
        RedditSlideshowServer.postApi("/api/data");
        //RedditSlideshowServer.getLogin("/login", "login.hbs");
        //RedditSlideshowServer.getBotAuth("/bot");
        RedditSlideshowServer.getGeneric("/remote/:code", "remote.hbs");
        RedditSlideshowServer.postRemoteClient("/api//remote/client");
        RedditSlideshowServer.getRemoteServer("/api//remote/server");
        // Login thread
        LoginThread loginThread = new LoginThread();
        loginThread.start();
        // REDIS
        String redisUrl = System.getenv("REDIS_URL");
        RedisUtil.start(redisUrl);
	}

	/**
     * Get an environment variable or log and die
     * @param name env var
     */
    private static String getenv(String name) {
        String env = System.getenv(name);
        if (env == null || env.isEmpty()) {
            Logger.warn("Missing required environment variable: %s", name);
            System.exit(1);
        }
        return env;
    }
}
