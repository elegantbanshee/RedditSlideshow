package com.elegantbanshee;

import com.goebl.david.Webb;
import com.google.gson.Gson;
import spark.ModelAndView;

import static spark.Spark.*;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedditSlideshowServer {

    private static final String[] subreddits = {
            "wallpapers"
    };

    static void getGeneric(String path, String templatePath) {
        get(path, (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, templatePath));
        });
    }

    static void postApi(String path) {
        post(path, (request, response) -> {
            Webb webb = Webb.create();
            webb.setBaseUri("https://reddit.com");
            webb.setDefaultHeader(Webb.HDR_USER_AGENT, "com.ElegantBanshee.RedditSlideshow/1.0");
            webb.setDefaultHeader("NSFW-ON", "TRUE");

            String[] subreddits = request.body().split("[+\\s]");
            ArrayList<ArrayList<String>> urls = new ArrayList<>();
            urls.ensureCapacity(250);

            int index = 0;
            for (String subreddit : subreddits) {
                com.goebl.david.Response <String> html = webb.get("/r/" + subreddit).ensureSuccess().asString();
                //Pattern pattern = Pattern.compile("https:\\/\\/[A-Za-z\\.]*\\/[A-Za-z\\d]*\\.(?:jpeg|png|jpg|gif)", Pattern.MULTILINE);
                Pattern pattern = Pattern.compile("https:\\/\\/(?:i.redd.it)*\\/[A-Za-z\\d]*\\.(?:jpeg|png|jpg|gif)");
                Matcher matcher = pattern.matcher(html.getBody());

                if (urls.size() == index)
                    urls.add(new ArrayList<>());
                while (matcher.find()) {
                    urls.get(index).add(matcher.group());
                }
                index += 1;
            }

            ArrayList<String> mixedUrls = new ArrayList<>();
            int[] urls_index = new int[subreddits.length];

            int mixed_index = 0;
            while (mixed_index != subreddits.length) {
                for (ArrayList<String> urls_array : urls) {
                    if (urls_array.size() > urls_index[mixed_index]) {
                        if (urls_index[mixed_index] != -1) {
                            String url = urls_array.get(urls_index[mixed_index]);
                            mixedUrls.add(url);
                            urls_index[mixed_index] += 1;
                        }
                    }
                    else {
                        urls_index[mixed_index] = -1;
                    }

                    mixed_index += 1;
                    if (!shouldEndLoop(urls_index) && mixed_index == subreddits.length)
                        mixed_index = 0;
                    else {
                        if (urls.indexOf(urls_array) == urls.size() - 1)
                            mixed_index = subreddits.length;
                    }
                }
            }

            return new Gson().toJson(mixedUrls);
        });
    }

    private static boolean shouldEndLoop(int[] urls_index) {
        for (int urlsIndex : urls_index) {
            if (urlsIndex != -1)
                return false;
        }
        return true;
    }
}
