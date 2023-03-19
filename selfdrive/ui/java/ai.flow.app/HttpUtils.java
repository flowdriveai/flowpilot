package ai.flow.app;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class HttpUtils {
    public static class DefaultResponse {
        public Boolean success;
        public String message;
    }

    public static DefaultResponse parseDefaultResponse(String response) {
        Json json = new Json();
        DefaultResponse ret = json.fromJson(DefaultResponse.class, response);
        return ret;
    }

    public static JsonValue parseGenericResponse(String response) {
        JsonValue ret = new JsonReader().parse(response);
        return ret;
    }
}
