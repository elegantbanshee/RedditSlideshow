package com.elegantbanshee.util;

import com.goebl.david.Response;
import com.goebl.david.Webb;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RedditBot {

    public static String bearerToken = "";

    public static void login(String redditUsername, String redditPassword, String redditClientId, Object redditClientSecret) {
        Webb webb = Webb.create();
        webb.setDefaultHeader(Webb.HDR_USER_AGENT, "com.ElegantBanshee.RedditSlideshow/1.0");
        webb.setDefaultHeader("Authentication",
                Base64.getEncoder().encodeToString(String.format("%s:%s", redditClientId,
                        redditClientSecret).getBytes(StandardCharsets.UTF_8)));
        webb.setBaseUri("https://www.reddit.com/api/v1");
        Response<JSONObject> json = webb.post("/access_token")
                .body(String.format("grant_type=password&username=%s&password=%s",
                        redditUsername, redditPassword))
                .ensureSuccess().asJsonObject();
        bearerToken = (String) json.getBody().get("access_token");
        System.out.println();
    }
}
