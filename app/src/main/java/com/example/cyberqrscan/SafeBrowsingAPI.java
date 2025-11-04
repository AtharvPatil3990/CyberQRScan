package com.example.cyberqrscan;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.example.cyberqrscan.ui.settings.AppInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SafeBrowsingAPI {

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient client;
    // FIX: Hardcoded API key is a security risk. For testing, it's okay, but consider moving it to a secure place.
    private static final String API_KEY = "Your_api";

    public SafeBrowsingAPI(Context context) {
        this.context = context.getApplicationContext(); // FIX: Use application context to avoid memory leaks.
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        this.client = new OkHttpClient();
    }

    // --- Public API Methods ---

    /**
     * Checks if a URL is safe. This is the main entry point.
     * It first checks a local database of hash prefixes and then queries the full hash from the API if needed.
     */
    public void checkURLSafety(String url, SafeCheckCallback callback) {
        String hashPrefix = getUrlHashPrefix(url);
        if (hashPrefix == null) {
            callback.onResult(true); // Assume safe if hashing fails.
            return;
        }

        // 1. First, check if the prefix exists in the local database.
        else if (!checkURLInDatabase(hashPrefix)) {
            // If not in the local DB, it's considered safe (no known threats).
            System.out.println("Hash prefix not found locally. URL is safe.");
            callback.onResult(true);
            return;
        }

        // 2. If the prefix is in the DB, we must confirm the full hash with the API.
        System.out.println("Hash prefix found locally. Verifying full hash with Google Safe Browsing API.");
        postReportForFullUrl(hashPrefix, callback);
    }

    /**
     * Fetches the latest threat list updates from the Safe Browsing API.
     */
    public void updateThreatList() {
        String updateUrl = "https://safebrowsing.googleapis.com/v4/threatListUpdates:fetch?key=" + API_KEY;

        String jsonBody = buildJsonRequestBodyForUpdate();
        if (jsonBody == null) {
            System.out.println("Failed to build JSON body for threat list update.");
            return;
        }

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(updateUrl).post(body)
                .header("X-Android-Package", "com.example.cyberqrscan")
                .header("X-Android-Cert", getSha1Fingerprint())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("API call failed in updateThreatList: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // FIX: Use a try-with-resources block to safely handle the response body.
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        // FIX: Read the response body to get the actual error message from Google.
                        if (responseBody != null) {
                            System.out.println("Error Body from Google: " + responseBody.string());
                        }
                        return;
                    }

                    if (responseBody == null) {
                        return;
                    }

                    // FIX: Read the body ONCE and pass it to the handler.
                    String jsonResponse = responseBody.string();
                    handleUpdateResponse(jsonResponse);

                    // FIX: Move this to be inside a successful response.
                    saveLastUpdateTime();
                }
            }
        });
    }

    // --- Private Helper Methods for API Calls ---

    /**
     * Queries the fullHashes:find endpoint to confirm if a hash prefix matches a full malicious hash.
     */
    private void postReportForFullUrl(String hash, SafeCheckCallback callback) {
        QRDatabase db = new QRDatabase(context);
        String threatType = db.getUrlThreatType(hash);
        db.close();
        System.out.println("Entering the postReportForFullUrl method.");

        if (threatType == null) {
            // This can happen if the DB is cleared between the prefix check and here.
            System.out.println("Threat type for hash not found, assuming safe.");
            callback.onResult(true);
            return;
        }

        String findUrl = "https://safebrowsing.googleapis.com/v4/fullHashes:find?key=" + API_KEY;
        String jsonBody = buildJsonRequestBodyForFullHash(hash, threatType);

        if (jsonBody == null) {
            System.out.println("Failed to build JSON body for full hash lookup.");
            callback.onResult(true); // Default to safe.
            return;
        }

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(findUrl).post(body)
                .header("X-Android-Package", "com.example.cyberqrscan")
                .header("X-Android-Cert", getSha1Fingerprint())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("API call failed in postReportForFullUrl: " + e.getMessage());
                callback.onResult(true); // If network fails, default to safe.
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // FIX: Correctly handle the response.
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        System.out.println("Failed to send full hash report: " + response.code());
                        if (responseBody != null) {
                            System.out.println("Error Body: " + responseBody.string());
                        }
                        callback.onResult(true);
                        System.out.println("site is safe");// Default to safe.
                        return;
                    }

                    if (responseBody == null) {
                        callback.onResult(true);
                        System.out.println("no body in response of postReportForFullUrl");// No body means no matches, so it's safe.
                        return;
                    }

                    String bodyString = responseBody.string();
                    try {
                        JSONObject jsonResponse = new JSONObject(bodyString);
                        // If the "matches" array exists and is not empty, the URL is unsafe.
                        if (jsonResponse.has("matches")) {
                            System.out.println("Full hash match found. URL is UNSAFE.");
                            callback.onResult(false);
                        } else {
                            System.out.println("No full hash matches. URL is safe.");
                            callback.onResult(true);
                        }
                    } catch (JSONException e) {
                        System.out.println("JSON parsing error in postReportForFullUrl: " + e.getMessage());
                        callback.onResult(true); // Default to safe.
                    }
                }
            }
        });
    }
    private String getSha1Fingerprint(){
        return "C7714C304854968F1846476627D4CF0D69A51959";
    }

    /**
     * Processes the JSON response from the threatListUpdates fetch call.
     */
    private void handleUpdateResponse(String jsonResponse) {
        try {
            JSONObject obj = new JSONObject(jsonResponse);
            JSONArray listUpdates = obj.optJSONArray("listUpdateResponses");

            if (listUpdates == null || listUpdates.length() == 0) {
                System.out.println("No threat list updates found in the response.");
                return;
            }

            System.out.println("Processing " + listUpdates.length() + " list update(s).");
            QRDatabase db = new QRDatabase(context);
            for (int i = 0; i < listUpdates.length(); i++) {
                JSONObject update = listUpdates.getJSONObject(i);
                String threatType = update.getString("threatType");
                String newClientState = update.optString("newClientState", "");
                if (!newClientState.isEmpty()) {
                    saveClientState(newClientState, threatType);
                }

                if (update.has("additions")) {
                    JSONArray additions = update.getJSONArray("additions");
                    for (int j = 0; j < additions.length(); j++) {
                        JSONObject rawHashesObj = additions.getJSONObject(j).getJSONObject("rawHashes");
                        String rawHashes = rawHashesObj.getString("rawHashes");
                        int prefixSize = rawHashesObj.getInt("prefixSize");
                        System.out.println("raw hashes: "+rawHashes+", threat type: "+ threatType);
                        db.insertHashesToDatabase(rawHashes, prefixSize, threatType);
                    }
                }

                if (update.has("removals")) {
                    JSONObject removals = update.getJSONArray("removals").getJSONObject(0);
                    if (removals.has("rawIndices")) {
                        // This part of your original code was incorrect, this logic is complex.
                        // For now, we are skipping removals. A full implementation requires careful index management.
                        System.out.println("Hash removals are present but not yet implemented.");
                    }
                }
            }
            db.close();
        } catch (JSONException e) {
            System.out.println("JSON parsing error in handleUpdateResponse: " + e.getMessage());
        }
    }

    // --- Private Helper Methods for Building JSON Bodies ---

    private String buildJsonRequestBodyForUpdate() {
        try {
            JSONObject root = new JSONObject();
            JSONObject clientInfo = new JSONObject();
            clientInfo.put("clientId", context.getString(R.string.projectid));
            clientInfo.put("clientVersion", AppInfo.getVersionName()); // Assuming AppInfo gives the correct version
            root.put("client", clientInfo);

            JSONArray listUpdateRequests = new JSONArray();
            // FIX: You can request multiple threat types in one go.
            String[] threatTypes = {"MALWARE", "SOCIAL_ENGINEERING"};
            for (String threatType : threatTypes) {
                JSONObject updateRequest = new JSONObject();
                updateRequest.put("threatType", threatType);
                updateRequest.put("platformType", "ANDROID");
                updateRequest.put("threatEntryType", "URL");
                updateRequest.put("state", getSavedClientState(threatType));

                JSONObject constraints = new JSONObject();
                constraints.put("maxUpdateEntries", 2048);
                constraints.put("supportedCompressions", new JSONArray().put("RAW"));
                updateRequest.put("constraints", constraints);

                listUpdateRequests.put(updateRequest);
            }
            root.put("listUpdateRequests", listUpdateRequests);
            return root.toString();
        } catch (JSONException e) {
            System.out.println("Could not build JSON for update request: " + e.getMessage());
            return null;
        }
    }

    private String buildJsonRequestBodyForFullHash(String hash, String threatType) {
        try {
            JSONObject root = new JSONObject();
            JSONObject clientInfo = new JSONObject();
            clientInfo.put("clientId", context.getString(R.string.projectid));
            root.put("client", clientInfo);

            JSONObject threatInfo = new JSONObject();
            threatInfo.put("threatTypes", new JSONArray().put(threatType));
            threatInfo.put("platformTypes", new JSONArray().put("ANDROID"));
            threatInfo.put("threatEntryTypes", new JSONArray().put("URL"));

            JSONObject threatEntry = new JSONObject().put("hash", hash);
            threatInfo.put("threatEntries", new JSONArray().put(threatEntry));

            root.put("threatInfo", threatInfo);
            return root.toString();
        } catch (JSONException e) {
            System.out.println("Could not build JSON for full hash lookup: " + e.getMessage());
            return null;
        }
    }

    // --- Other Utility Methods ---

    private boolean checkURLInDatabase(String hashPrefix) {
        QRDatabase db = new QRDatabase(context);
        boolean exists = db.isHashPrefixInDatabase(hashPrefix);
        db.close();
        return exists;
    }

    private String getUrlHashPrefix(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(url.getBytes(UTF_8));
            int prefixSize = 4; // 4 bytes = 32 bits
            byte[] prefixBytes = Arrays.copyOfRange(hashBytes, 0, prefixSize);
            // Convert to Base64, which is what the API expects for hashes.
            return android.util.Base64.encodeToString(prefixBytes, android.util.Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("SHA-256 algorithm not found.");
            return null;
        }
    }

    private void saveClientState(String clientState, String threatType) {
        prefs.edit().putString("safe_browsing_client_state_" + threatType, clientState).apply();
    }

    private String getSavedClientState(String threatType) {
        return prefs.getString("safe_browsing_client_state_" + threatType, "");
    }

    private void saveLastUpdateTime() {
        SharedPreferences timePrefs = context.getSharedPreferences("SafeBrowsingPrefs", Context.MODE_PRIVATE);
        timePrefs.edit().putLong("last_URLdb_update_time", System.currentTimeMillis()).apply();
    }

    // --- Callback Interface ---

    public interface SafeCheckCallback {
        void onResult(boolean isSafe);
    }
}
