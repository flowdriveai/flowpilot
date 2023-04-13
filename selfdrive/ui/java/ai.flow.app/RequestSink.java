package ai.flow.app;

import ai.flow.common.ParamsInterface;
import com.badlogic.gdx.utils.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class RequestSink {
    static final OkHttpClient client = new OkHttpClient().newBuilder().cache(null).build();
    private static final String API_BASE_URL = "https://staging-api.flowdrive.ai";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ParamsInterface params = ParamsInterface.getInstance();
    public static Logger logger = LoggerFactory.getLogger(RequestSink.class);
    public static Boolean isConnectedToInternet() {
        String url = API_BASE_URL + "/health";

        logger.info("Internet check initiated");

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            logger.info("Internet check call made");
            return response.isSuccessful();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    public static void fetchUserInfo() {
        logger.info("Fetch User Info called");

        String url = API_BASE_URL + "/user/status";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + params.getString("UserToken"))
                .build();

        try (Response response = client.newCall(request).execute()) {
            logger.info("Fetched User Info");

            assert response.body() != null;
            JsonValue toJson = HttpUtils.parseGenericResponse(response.body().string());

            if (!toJson.get("success").asBoolean()) {
                logger.error(String.valueOf(toJson.get("message")));
            }

            String plan = toJson.get("message").getString("plan");
            String plan_expires_at = toJson.get("message").getString("plan_expires_at");

            params.put("Plan", plan);
            params.put("PlanExpiresAt", plan_expires_at);

        } catch (IOException e) {
            logger.error(e.getMessage());
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

        logger.info("Device Allowed called");

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
                logger.info("Device Info Fetched");
                assert response.body() != null;
                JsonValue toJson = HttpUtils.parseGenericResponse(response.body().string());
                if (!toJson.get("success").asBoolean()) {
                    logger.error(String.valueOf(toJson.get("message")));
                    return false;
                }

                params.put("DeviceRegId", toJson.get("message").getString("device_id"));
            } catch (IOException e) {
                logger.error(e.getMessage());
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
                logger.error(String.valueOf(toJson.get("message")));
                return false;
            }

            if (responseStr.contains(currDeviceRegId)) {
                return true;
            } else {
                params.deleteKey("DeviceRegId");
                return false;
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }
}
