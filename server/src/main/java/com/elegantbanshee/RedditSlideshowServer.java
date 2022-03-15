package com.elegantbanshee;

import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.google.gson.Gson;
import org.json.JSONObject;
import spark.ModelAndView;

import static spark.Spark.*;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedditSlideshowServer {

    private static String bearerToken = "";
    private static String refreshToken = "";
    private static long lastRefreshTime = 0;

    static void getGeneric(String path, String templatePath) {
        get(path, (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, templatePath));
        });
    }

    static void postApi(String path) {
        post(path, (request, response) -> {
            if (shouldRefreshAccessToken()) {
                refreshAccessToken();
            }

            Webb webb = Webb.create();
            webb.setBaseUri("https://reddit.com");
            webb.setDefaultHeader(Webb.HDR_USER_AGENT, "com.ElegantBanshee.RedditSlideshow/1.0");
            webb.setDefaultHeader("NSFW-ON", "ON");

            if (!bearerToken.isEmpty())
                webb.setDefaultHeader("Authorization", "Bearer " + bearerToken);

            String[] subreddits = request.body().split("[+\\s]");
            ArrayList<ArrayList<String>> urls = new ArrayList<>();
            urls.ensureCapacity(250);

            int index = 0;
            for (String subreddit : subreddits) {
                com.goebl.david.Response <String> html = webb.get("/r/" + subreddit).ensureSuccess().asString();
                //Pattern pattern = Pattern.compile("https:\\/\\/[A-Za-z\\.]*\\/[A-Za-z\\d]*\\.(?:jpeg|png|jpg|gif)", Pattern.MULTILINE);
                Pattern pattern = Pattern.compile("https:\\/\\/(?:i.redd.it|i.imgur.com)*\\/[A-Za-z\\d]*\\.(?:jpeg|png|jpg|gifv|gif)");
                Matcher matcher = pattern.matcher(html.getBody());

                if (urls.size() == index)
                    urls.add(new ArrayList<>());
                while (matcher.find()) {
                    String cleanUrl = convertGifvToMp4Url(matcher.group());
                    urls.get(index).add(cleanUrl);
                }
                index += 1;
            }

            ArrayList<String> mixedUrls = mixUrls(urls);

            return new Gson().toJson(mixedUrls);
        });
    }

    private static void refreshAccessToken() {
        Webb webb = Webb.create();
        webb.setDefaultHeader(Webb.HDR_USER_AGENT, "com.ElegantBanshee.RSlideshow/1.0");

        String passwordString = String.format("%s:%s", System.getenv("REDDIT_CLIENT_ID"),
                System.getenv("REDDIT_CLIENT_SECRET"));
        String encodedAuth = Base64.getEncoder().encodeToString(passwordString.getBytes(StandardCharsets.UTF_8));
        webb.setDefaultHeader("Authorization", "Basic " + encodedAuth);

        webb.setBaseUri("https://www.reddit.com/api/v1");
        webb.setDefaultHeader("Content-Type", "application/x-www-form-urlencoded");
        Response<JSONObject> json = webb.post("/access_token")
                .body(String.format("grant_type=refresh_token&refresh_token=%s", refreshToken))
                //.ensureSuccess()
                .asJsonObject();
        bearerToken = (String) json.getBody().get("access_token");
        refreshToken = (String) json.getBody().get("refresh_token");

        lastRefreshTime = System.currentTimeMillis();
    }

    private static boolean shouldRefreshAccessToken() {
        return !refreshToken.isEmpty() && System.currentTimeMillis() - lastRefreshTime > 1 * 60 * 1000;
    }

    private static ArrayList<String> mixUrls(ArrayList<ArrayList<String>> urls) {
        ArrayList<String> mixedUrls = new ArrayList<>();
        int index = 0;
        while (hasUrls(urls)) {
            ArrayList<String> urlz = urls.get(index);
            if (urlz.size() > 0) {
                String url = urlz.remove(0);
                if (!mixedUrls.contains(url))
                    mixedUrls.add(url);
            }
            index = (index + 1) % urls.size();
        }
        return mixedUrls;
    }

    private static boolean hasUrls(ArrayList<ArrayList<String>> urls) {
        for (ArrayList<String> urlz : urls) {
            if (urlz.size() > 0)
                return true;
        }
        return false;
    }

    private static String convertGifvToMp4Url(String url) {
        return url.replace(".gifv", ".mp4");
    }

    public static void getBotAuth(String path) {
        get(path, (request, response) -> {
            if (!bearerToken.isEmpty())
                return "Already logged in";

            Webb webb = Webb.create();
            webb.setDefaultHeader(Webb.HDR_USER_AGENT, "com.ElegantBanshee.RSlideshow/1.0");


            String passwordString = String.format("%s:%s", System.getenv("REDDIT_CLIENT_ID"),
                    System.getenv("REDDIT_CLIENT_SECRET"));
            String encodedAuth = Base64.getEncoder().encodeToString(passwordString.getBytes(StandardCharsets.UTF_8));
            webb.setDefaultHeader("Authorization", "Basic " + encodedAuth);


            webb.setBaseUri("https://www.reddit.com/api/v1");
            webb.setDefaultHeader("Content-Type", "application/x-www-form-urlencoded");
            Response<JSONObject> json = webb.post("/access_token")
                    .body(String.format("grant_type=authorization_code&code=%s&redirect_uri=http://localhost:5000/bot",
                            request.queryParams("code")))
                    //.ensureSuccess()
                    .asJsonObject();
            bearerToken = (String) json.getBody().get("access_token");
            refreshToken = (String) json.getBody().get("refresh_token");

            lastRefreshTime = System.currentTimeMillis();
            return "Logged in";
        });
    }

    public static void getLogin(String path, String templatePath) {
        get(path, (request, response) -> {
            Map<String, Object> model = new HashMap<>();
            model.put("CLIENT_ID", System.getenv("REDDIT_CLIENT_ID"));
            return new HandlebarsTemplateEngine().render(new ModelAndView(model, templatePath));
        });
    }
}
