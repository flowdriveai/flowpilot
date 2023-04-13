package ai.flow.app;

import ai.flow.common.ParamsInterface;
import com.badlogic.gdx.utils.JsonValue;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class RequestSink {
    static final OkHttpClient client = new OkHttpClient();
    private static final String API_BASE_URL = "https://staging-api.flowdrive.ai";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ParamsInterface params = ParamsInterface.getInstance();
    public static Boolean isConnectedToInternet() {
        String url = API_BASE_URL + "/health";

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void fetchUserInfo() {
        String url = API_BASE_URL + "/user/status";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + params.getString("UserToken"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            JsonValue toJson = HttpUtils.parseGenericResponse(response.body().string());

            if (!toJson.get("success").asBoolean()) {
                System.out.println(toJson.get("message"));
            }

            params.put("Plan", toJson.get("message").getString("plan"));
            params.put("PlanExpiresAt", toJson.get("message").getString("plan_expires_at"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isPlanStillValid() {
        // Requires Android Oreo and above
        String planExpiry = params.getString("PlanExpiresAt");
        LocalDateTime dateTime = LocalDateTime.parse(planExpiry, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"));
        ZonedDateTime utcDateTime = ZonedDateTime.of(dateTime, ZoneId.of("UTC"));
        ZonedDateTime planExpiryZdTime = utcDateTime.withZoneSameInstant(ZoneId.systemDefault());
        ZonedDateTime zdtNow = ZonedDateTime.now();

        return zdtNow.isBefore(planExpiryZdTime) && Objects.equals(params.getString("Plan"), "f3_beta");
    }

    public static boolean isDeviceAllowed() {
        // Checks for whether the current plan supports this device

        String url = API_BASE_URL + "/device";

        if (!isConnectedToInternet() && params.exists("DeviceRegId")) {
            // Use old device id if internet is not connected
            return true;
        }

        if (!params.exists("DeviceRegId")) {
            // Register device

            String dongleId = params.getString("DongleId");
            String modelName = params.getString("DeviceManufacturer") + " " + params.getString("DeviceModel");
            String requestJson = "{\n\t\"dongle_id\": \"" + dongleId + "\",\n\t\"model_name\": \"" + modelName +"\"\n}";

            RequestBody body = RequestBody.create(requestJson, JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + params.getString("UserToken"))
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                assert response.body() != null;
                JsonValue toJson = HttpUtils.parseGenericResponse(response.body().string());
                if (!toJson.get("success").asBoolean()) {
                    System.out.println(toJson.get("message"));
                    return false;
                }

                params.put("DeviceRegId", toJson.get("message").getString("device_id"));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // Check if current device is in device list
        String currDeviceRegId = params.getString("DeviceRegId");

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + params.getString("UserToken"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String responseStr = response.body().string();
            JsonValue toJson = HttpUtils.parseGenericResponse(responseStr);
            if (!toJson.get("success").asBoolean()) {
                System.out.println(toJson.get("message"));
                return false;
            }

            if (responseStr.contains(currDeviceRegId)) {
                return true;
            } else {
                params.deleteKey("DeviceRegId");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
