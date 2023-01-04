package ai.flow.app;

import com.badlogic.gdx.utils.Json;

public class HttpUtils {
    public static class Response {
        public Boolean success;
        public String message;
    }

    public static Response parseResponse(String response) {
        Json json = new Json();
        Response ret = json.fromJson(Response.class, response);
        return ret;
    }
}
