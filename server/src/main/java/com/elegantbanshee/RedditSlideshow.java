package com.elegantbanshee;

import com.elegantbanshee.data.Constants;
import com.elegantbanshee.util.Logger;
import com.elegantbanshee.util.RedditBot;

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
        // Reddit Login
        String redditUsername = System.getenv("REDDIT_USERNAME");
        String redditPassword = System.getenv("REDDIT_PASSWORD");
        String redditClientId = System.getenv("REDDIT_CLIENT_ID");
        String redditClientSecret = System.getenv("REDDIT_CLIENT_SECRET");
        RedditBot.login(redditUsername, redditPassword, redditClientId, redditClientSecret);
        // Set values
        port(port);
        staticFiles.location("/static/");
        staticFiles.expireTime(604800); // One Week cache
        // Web
        RedditSlideshowServer.getGeneric("/", "index.hbs");
        RedditSlideshowServer.getGeneric("/r/:reddits", "index.hbs");
        RedditSlideshowServer.postApi("/api/data");
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
