package com.example.cyberqrscan;

import com.example.cyberqrscan.ui.settings.AppInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SafeBrowsingUpdater{
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppInfo.getContext());

    private void updateThreatList(){
        String UPDATE_URL = "https://safebrowsing.googleapis.com/v4/threatListUpdates:fetch?key=";

        OkHttpClient client = new OkHttpClient();
        String jsonBody = buildJsonRequest();

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
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    System.out.println("Error: " + response.code());
                    return;
                }

                String jsonResponse = response.body().string();
                handleResponse(jsonResponse);
            }
        });
    }

    private void handleResponse(String jsonResponse){
        try {
            JSONObject obj = new JSONObject(jsonResponse);

            // The API always returns "listUpdateResponses"
            JSONArray listUpdates = obj.optJSONArray("listUpdateResponses");
            if (listUpdates == null) {
//                No updates found in response.
                return;
            }

            int listUpdateLength = listUpdates.length();
            for (int i = 0; i < listUpdateLength; i++) {
                JSONObject update = listUpdates.getJSONObject(i);

                // Save the new client state for future incremental updates
                String newClientState = update.optString("newClientState", "");
                if (!newClientState.isEmpty()) {
                    saveClientState(newClientState);
                }

                // Handle additions (new hashes)
                if (update.has("additions")) {
                    JSONArray additions = update.getJSONArray("additions");
                    for (int j = 0; j < additions.length(); j++) {
                        JSONObject rawHashesObj = additions.getJSONObject(j)
                                .getJSONObject("rawHashes");

                        String rawHashes = rawHashesObj.getString("rawHashes");
                        int prefixSize = rawHashesObj.getInt("prefixSize");

                        saveHashesToDatabase(rawHashes, prefixSize);
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
                            removeHashesFromDatabase(indices);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveHashesToDatabase(String rawHashes, int prefixSize){
        try {
            // Step 1: Decode Base64 into raw bytes
            byte[] decodedBytes = Base64.decode(rawHashes, Base64.DEFAULT);

            // Step 2: Get current timestamp and expiration time
            long addedAt = System.currentTimeMillis() / 1000; // in seconds
            long expirationTime = addedAt + 300; // Example: 5 min (300 sec) validity

            // Step 3: Split into prefixes and insert into DB
            SQLiteDatabase db = QRDatabase.getWritableDatabase();
            db.beginTransaction();
            try {
                for (int i = 0; i < decodedBytes.length; i += prefixSize) {
                    byte[] prefix = Arrays.copyOfRange(decodedBytes, i, i + prefixSize);

                    ContentValues values = new ContentValues();
                    values.put("hash_prefix", prefix);

                    db.insert("malware_hash_prefixes", null, values);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
                db.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeHashesFromDatabase(JSONArray indices) {
    }

    private String buildJsonRequest(){
        try {

            // Root JSON object
                JSONObject root = new JSONObject();

                // ---- Client section ----
                JSONObject client = new JSONObject();
                client.put("clientId", R.string.projectid);
                client.put("clientVersion", AppInfo.getVersionName());
                root.put("client", client);

                // ---- Update request ----
                JSONArray listUpdateRequests = new JSONArray();
                JSONObject updateRequest = new JSONObject();

                updateRequest.put("threatType", "MALWARE");          // can also be SOCIAL_ENGINEERING, etc.
                updateRequest.put("platformType", "ANDROID");        // or ANDROID, ANY_PLATFORM
                updateRequest.put("threatEntryType", "URL");         // URL, EXECUTABLE, etc.

                // Client state from last update (empty for first request)
                String clientState = getSavedClientState();
                updateRequest.put("state", clientState);

                // ---- Constraints ----
                JSONObject constraints = new JSONObject();
                constraints.put("maxUpdateEntries", 2048);
                constraints.put("maxDatabaseEntries", 4096);
                constraints.put("region", "INDIA");

                JSONArray supportedCompressions = new JSONArray();
                supportedCompressions.put("RAW");
                constraints.put("supportedCompressions", supportedCompressions);

                updateRequest.put("constraints", constraints);

                // Add to listUpdateRequests
                listUpdateRequests.put(updateRequest);
                root.put("listUpdateRequests", listUpdateRequests);

                // Return JSON as String
                return root.toString();

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
    }

    private void saveClientState(String clientState) {
        prefs.edit().putString("safe_browsing_client_state", clientState).apply();
    }

    private String getSavedClientState() {
        return prefs.getString("safe_browsing_client_state", "");
    }

}