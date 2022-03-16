package com.elegantbanshee;

import com.elegantbanshee.util.LoginThread;
import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.ModelAndView;

import static spark.Spark.*;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedditSlideshowServer {

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
            webb.setDefaultHeader("NSFW-ON", "ON");

            if (!LoginThread.bearerToken.isEmpty())
                webb.setDefaultHeader("Authorization", "Bearer " + LoginThread.bearerToken);

            String[] subredditsPage = request.body().split("[;]");
            String pageString = subredditsPage.length == 2 ? subredditsPage[1] : "";
            String subreddits = subredditsPage[0];

            com.goebl.david.Response <JSONObject> json = webb.get(
                    String.format("/r/%s.json?after=%s", subreddits, pageString))
                    .ensureSuccess().asJsonObject();

            JSONArray urls = new JSONArray();
            JSONArray jsonUrls = json.getBody().getJSONObject("data").getJSONArray("children");

            for (Object jsonUrlObj : jsonUrls) {
                JSONObject jsonUrl = (JSONObject) jsonUrlObj;
                String url = jsonUrl.getJSONObject("data").getString("url");

                Pattern pattern = Pattern.compile("https:\\/\\/(?:i.redd.it|i.imgur.com)*\\/[A-Za-z\\d]*\\.(?:jpeg|png|jpg|gifv|gif)");
                Matcher matcher = pattern.matcher(url);

                if (matcher.find()) {
                    url = convertGifvToMp4Url(url);

                    JSONObject urlJsonObj = new JSONObject();
                    urlJsonObj.put("url", url);
                    urlJsonObj.put("title", jsonUrl.getJSONObject("data").getString("title"));
                    urlJsonObj.put("subreddit", jsonUrl.getJSONObject("data").getString("subreddit"));
                    urls.put(urlJsonObj);
                }
            }

            JSONObject returnJson = new JSONObject();
            returnJson.put("data", urls);
            returnJson.put("after", json.getBody().getJSONObject("data").getString("after"));

            response.header("Access-Control-Allow-Origin", "https://rolando.org");

            return returnJson.toString();
        });
    }

    public static void refreshAccessToken() {
        Webb webb = Webb.create();
        webb.setDefaultHeader(Webb.HDR_USER_AGENT, "com.ElegantBanshee.RSlideshow/1.0");

        String passwordString = String.format("%s:%s", System.getenv("REDDIT_CLIENT_ID"),
                System.getenv("REDDIT_CLIENT_SECRET"));
        String encodedAuth = Base64.getEncoder().encodeToString(passwordString.getBytes(StandardCharsets.UTF_8));
        webb.setDefaultHeader("Authorization", "Basic " + encodedAuth);

        webb.setBaseUri("https://www.reddit.com/api/v1");
        webb.setDefaultHeader("Content-Type", "application/x-www-form-urlencoded");
        Response<JSONObject> json = webb.post("/access_token")
                .body(String.format("grant_type=refresh_token&refresh_token=%s", LoginThread.refreshToken))
                //.ensureSuccess()
                .asJsonObject();
        LoginThread.bearerToken = (String) json.getBody().get("access_token");
        LoginThread.refreshToken = (String) json.getBody().get("refresh_token");

        LoginThread.lastRefreshTime = System.currentTimeMillis();
    }

    private static String convertGifvToMp4Url(String url) {
        return url.replace(".gifv", ".mp4");
    }

    public static void getBotAuth(String path) {
        get(path, (request, response) -> {
            if (!LoginThread.bearerToken.isEmpty())
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
            LoginThread.bearerToken = (String) json.getBody().get("access_token");
            LoginThread.refreshToken = (String) json.getBody().get("refresh_token");

            LoginThread.lastRefreshTime = System.currentTimeMillis();
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
