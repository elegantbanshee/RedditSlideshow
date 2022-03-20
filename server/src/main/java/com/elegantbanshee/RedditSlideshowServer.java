package com.elegantbanshee;

import com.elegantbanshee.util.LoginThread;
import com.elegantbanshee.util.RedisUtil;
import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.goebl.david.WebbException;
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

            String[] subredditsPage = request.body().split("[;]");
            String pageString = subredditsPage.length == 2 ? subredditsPage[1] : "";
            String subredditsString = subredditsPage.length >= 1 ? subredditsPage[0] : "";
            if (subredditsString.isEmpty())
                subredditsString = "popular";
            if (pageString.isEmpty())
                pageString = "[]";

            String[] subreddits = subredditsString.split("[+\\s]");
            JSONArray pages = new JSONArray(pageString);
            ArrayList<JSONArray> subredditsJson = new ArrayList<>();

            JSONArray pagesNew = new JSONArray();

            for (String subreddit : subreddits) {
                int index = Arrays.asList(subreddits).indexOf(subreddit);
                String page = pages.getString(index);
                JSONObject imagesObject = getSubredditJson(subreddit, page);
                JSONArray images = imagesObject.getJSONArray("images");
                String newPage = imagesObject.getString("page");
                pagesNew.put(newPage);

                subredditsJson.add(images);
            }

            JSONArray mixedImages = mixImages(subredditsJson);

            JSONObject returnJson = new JSONObject();
            returnJson.put("data", mixedImages);
            returnJson.put("after", pagesNew);

            response.header("Access-Control-Allow-Origin", "https://rolando.org");

            return returnJson.toString();
        });
    }

    private static JSONArray mixImages(ArrayList<JSONArray> subredditsJson) {
        JSONArray ret = new JSONArray();
        while (hasObjects(subredditsJson)) {
            for (JSONArray images : subredditsJson) {
                if (images.length() > 0) {
                    if (!containsUrl(ret, ((JSONObject) images.get(0)).getString("url")))
                        ret.put(images.get(0));
                    images.remove(0);
                }
            }
        }
        return ret;
    }

    private static boolean hasObjects(ArrayList<JSONArray> subredditsJson) {
        for (JSONArray jsonArray : subredditsJson) {
            if (jsonArray.length() > 0)
                return true;
        }
        return false;
    }

    private static boolean containsUrl(JSONArray json, String url) {
        for (Object object : json) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.getString("url").equals(url))
                return true;
        }
        return false;
    }

    private static JSONObject getSubredditJson(String subreddit, String page) {
        JSONObject ret = new JSONObject();
        ret.put("images", new JSONArray());
        ret.put("page", "");

        if (page.equals("none")) {
            ret.put("page", "none");
            return ret;
        }

        Webb webb = Webb.create();
        webb.setBaseUri("https://reddit.com");
        webb.setDefaultHeader(Webb.HDR_USER_AGENT, "com.ElegantBanshee.RedditSlideshow/1.0");
        webb.setDefaultHeader("NSFW-ON", "ON");

        if (!LoginThread.bearerToken.isEmpty())
            webb.setDefaultHeader("Authorization", "Bearer " + LoginThread.bearerToken);

        com.goebl.david.Response <JSONObject> json;

        try {
            json = webb.get(
                            String.format("/r/%s.json?after=%s", subreddit, page))
                    .ensureSuccess().asJsonObject();
        }
        catch (WebbException e) {
            ret.put("page", "none");
            return ret;
        }

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
        ret.put("page", json.getBody().getJSONObject("data").getString("after"));
        ret.put("images", urls);
        return ret;
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

    public static void postRemoteClient(String path) {
        post(path, (request, response) -> {
            String[] codeAndCommand = request.body().split(";");
            RedisUtil.storeCommand(codeAndCommand[0], codeAndCommand[1]);
            return "";
        });
    }

    public static void getRemoteServer(String path) {
        get(path, (request, response) -> {
            String command =  RedisUtil.getCommand(request.queryParams("code"));
            JSONObject json = new JSONObject();
            json.put("command", command);
            return json.toString();
        });
    }
}
