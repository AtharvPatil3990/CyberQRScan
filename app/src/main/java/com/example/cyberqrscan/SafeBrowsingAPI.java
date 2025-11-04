package com.example.cyberqrscan;

import com.example.cyberqrscan.ui.settings.AppInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import static java.nio.charset.StandardCharsets.UTF_8;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SafeBrowsingAPI{
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppInfo.getContext());

    public static void checkURLSafety(String url, SafeCheckCallback callback) {
        String hash = getUrlHashPrefix(url);

        // 1. First check if prefix exists in local DB
        if (!checkURLInDatabase(hash)) {
            // If not in DB → it's safe (no known threats)
            callback.onResult(true);
        }

        // 2. If found in DB, confirm with full-hash lookup
        postReportForFullUrl(hash, callback);
    }

    public void updateThreatList(Context context){
        String UPDATE_URL = "https://safebrowsing.googleapis.com/v4/threatListUpdates:fetch?key="+R.string.API_Key;

        OkHttpClient client = new OkHttpClient();
        String jsonBody = buildJsonRequestBodyForHash(context);
        if(jsonBody == null) {
            System.out.println("Returned null from update url function");
            return;
        }
        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(UPDATE_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("Response failed in updateThreatList function");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    System.out.println("Response code : " + response.code());
                    System.out.println("Response Body : " + response.body());
                    System.out.println("Response Not successful");
                    return;
                }

                assert response.body() != null;
                String jsonResponse = response.body().toString();
                handleResponse(jsonResponse);
                System.out.println("Response successful");
            }
        });

        // Save the current time as the last update timestamp
        SharedPreferences prefs = AppInfo.getContext().getSharedPreferences("SafeBrowsingPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_URLdb_update_time", System.currentTimeMillis());
        editor.apply();
    }

    private void handleResponse(String jsonResponse){
        try {
            JSONObject obj = new JSONObject(jsonResponse);

            // The API always returns "listUpdateResponses"
            JSONArray listUpdates = obj.optJSONArray("listUpdateResponses");
            if (listUpdates == null) {
//                No updates found in response.
                System.out.println("No updates found in response.");
                return;
            }
            System.out.println("Updates found in response.");
            int listUpdateLength = listUpdates.length();
            for (int i = 0; i < listUpdateLength; i++) {
                JSONObject update = listUpdates.getJSONObject(i);
                QRDatabase db = new QRDatabase(AppInfo.getContext());

                String threatType = update.getString("threatType");

                // Save the new client state for future incremental updates
                String newClientState = update.optString("newClientState", "");
                if (!newClientState.isEmpty())
                    saveClientState(newClientState, threatType);

                // Handle additions (new hashes)
                if (update.has("additions")) {
                    JSONArray additions = update.getJSONArray("additions");
                    for (int j = 0; j < additions.length(); j++) {
                        JSONObject rawHashesObj = additions.getJSONObject(j)
                                .getJSONObject("rawHashes");

                        String rawHashes = rawHashesObj.getString("rawHashes");

                        int prefixSize = rawHashesObj.getInt("prefixSize");


                        db.insertHashesToDatabase(rawHashes, prefixSize, threatType);
                    }
                }

                // Handle removals (hashes no longer valid)
                if (update.has("removals")) {
                    JSONArray removals = update.getJSONArray("removals");
                    for (int j = 0; j < removals.length(); j++) {
                        JSONObject rawIndicesObj = removals.getJSONObject(j)
                                .getJSONObject("rawIndices");

                        JSONArray indices = rawIndicesObj.optJSONArray("indices");
                        if (indices != null) {
                            db.removeHashesFromDatabase(indices);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            System.out.println( e.getMessage() + " " + e.getCause());
        }
    }

    //    JSON body for full hash
    private static String buildJsonRequestBodyForFullHash(String hash, String threatType) {
        JSONObject json = new JSONObject();
        try {
            // Client info
            JSONObject clientObj = new JSONObject();
            String projectId = String.valueOf(R.string.projectid);
            clientObj.put("clientId", projectId);
            clientObj.put("clientVersion", AppInfo.getVersionName());

            // Threat entries (just one hash)
            JSONArray threatEntriesArr = new JSONArray();
            threatEntriesArr.put(new JSONObject().put("hash", hash));

            // Threat info (only one type passed)
            JSONObject threatInfoObj = new JSONObject();
            threatInfoObj.put("threatTypes", new JSONArray().put(threatType));
            threatInfoObj.put("platformTypes", new JSONArray().put("ANDROID")); // you can parameterize this too
            threatInfoObj.put("threatEntryTypes", new JSONArray().put("URL"));  // fixed to URL
            threatInfoObj.put("threatEntries", threatEntriesArr);

            // Combine all
            json.put("client", clientObj);
            json.put("threatInfo", threatInfoObj);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json.toString();
    }

    private String buildJsonRequestBodyForHash(Context context){
        try {
            JSONObject root = new JSONObject();
            // ---- Client section ----
            JSONObject client = new JSONObject();

            String projectId = context.getString(R.string.projectid);
            client.put("clientId", projectId); // You might need getString() here
            client.put("clientVersion", AppInfo.getVersionName());
            root.put("client", client);

            // ---- Update request list ----
            JSONArray listUpdateRequests = new JSONArray();

            // Threat types you want to include
            String[] threatTypes = {"MALWARE", "SOCIAL_ENGINEERING"};

//            for (String threatType : threatTypes) {
                JSONObject updateRequest = new JSONObject();

//                updateRequest.put("threatType", threatType);
                updateRequest.put("threatType", "MALWARE");
                updateRequest.put("platformType", "ANDROID");
                updateRequest.put("threatEntryType", "URL");

                // Client state from last update (empty for first request)
//                String clientState = getSavedClientState(threatType);
                String clientState = getSavedClientState("MALWARE");
                updateRequest.put("state", clientState);

                // ---- Constraints ----
                JSONObject constraints = new JSONObject();
                constraints.put("maxUpdateEntries", 2048);
                constraints.put("maxDatabaseEntries", 4096);
                constraints.put("region", "IN");
                JSONArray supportedCompressions = new JSONArray();
                supportedCompressions.put("RAW");
                constraints.put("supportedCompressions", supportedCompressions);

                updateRequest.put("constraints", constraints);

                // Add this threat type request to list
                listUpdateRequests.put(updateRequest);
//            }
            root.put("listUpdateRequests", listUpdateRequests);

            return root.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getUrlHashPrefix(String url){
        try {
            // Normalize URL (remove fragments, lowercase host, etc.) — skipping full normalization for now
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(url.getBytes(UTF_8)); // StandardCharsets.UTF_8;

            // Let's say prefixSize is 4 bytes (common for Safe Browsing)
            int prefixSize = 4;
            byte[] prefixBytes = Arrays.copyOfRange(hashBytes, 0, prefixSize);

            // Convert to hex string for storage/lookup
            StringBuilder sb = new StringBuilder();
            for (byte b : prefixBytes) {
                sb.append(String.format("%02x", b)); // convert byte to its hexadecimal representation
            }

            return sb.toString();

        } catch (Exception e) {
            return null;
        }
    }

    public static void postReportForFullUrl(String hash, SafeCheckCallback callback) {
        OkHttpClient client = new OkHttpClient();

        QRDatabase db = new QRDatabase(AppInfo.getContext());
        String threatType = db.getUrlThreatType(hash);
        db.close();

        if (threatType == null) {
            callback.onResult(true);
            return;
        }

        String json = buildJsonRequestBodyForFullHash(hash, threatType);

        if (json != null) {
            RequestBody body = RequestBody.create(
                    json,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://safebrowsing.googleapis.com/v4/fullHashes:find?key=" + R.string.API_Key)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    System.out.println(e.getMessage() + " " + e.getCause());
                    callback.onResult(true); // If network fails → assume safe
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();

                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            // ✅ If matches exist → unsafe
                            if (jsonResponse.has("matches")) {
                                callback.onResult(false);
                            } else {
                                callback.onResult(true);
                            }

                        } catch (JSONException e) {
                            System.out.println(e.getMessage() + " " + e.getCause());
                            callback.onResult(true); // default safe on error
                        }
                    } else {
                        System.out.println("Failed to send report: " + response.code());
                        callback.onResult(true); // default safe on server error
                    }
                }
            });
        } else {
            System.out.println("Failed to build JSON body for hash: " + hash);
            callback.onResult(true);
        }
    }


    public static boolean checkURLInDatabase(String hashPrefix){
        QRDatabase db = new QRDatabase(AppInfo.getContext());
        boolean exists = db.isHashPrefixInDatabase(hashPrefix);
        db.close();
        return exists;
    }

    private void saveClientState(String clientState, String threatType) {
        prefs.edit().putString("safe_browsing_client_state_"+threatType, clientState).apply();
    }

    private String getSavedClientState(String threatType) {
        if(threatType.equals("MALWARE"))
            return prefs.getString("safe_browsing_client_state_MALWARE", "");
        else
            return prefs.getString("safe_browsing_client_state_SOCIAL_ENGINEERING", "");
    }

    public interface SafeCheckCallback{
        void onResult(boolean isSafe);
    }

}