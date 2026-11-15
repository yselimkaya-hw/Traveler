package com.traveler.app.util;

import android.util.Log;
import androidx.annotation.NonNull; // Eksik olabilir, ekledik

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
// ÖNEMLİ: Kendi paket adındaki BuildConfig'i import etmelisin
import com.traveler.app.BuildConfig;

public class GeminiApiHelper {

    private static final String TAG = "GeminiApiHelper";

    // local.properties -> build.gradle üzerinden gelen güvenli anahtar
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    public GeminiApiHelper() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private String getBaseUrl() {
        // Model ismini panelde gördüğümüz 'gemini-3-flash-preview' ile değiştiriyoruz
        return "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + API_KEY;
    }

    public void getRouteForCity(String city, RouteCallback callback) {
        // API Anahtarı kontrolü
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.equals("unused")) {
            Log.e(TAG, "HATA: API anahtarı yüklenemedi. local.properties kontrol edilmeli.");
            callback.onSuccess(getMockStopsForCity(city));
            return;
        }

        try {
            String prompt = buildPrompt(city);
            String requestBody = buildRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(getBaseUrl())
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();

            Log.d(TAG, city + " için API isteği gönderiliyor...");

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Ağ Hatası: " + e.getMessage());
                    // Kullanıcıya hissettirmeden mock veri dönüyoruz
                    callback.onSuccess(getMockStopsForCity(city));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (Response res = response) {
                        if (!res.isSuccessful()) {
                            String errorBody = res.body() != null ? res.body().string() : "Gövde yok";
                            Log.e(TAG, "API Hatası (" + res.code() + "): " + errorBody);
                            callback.onSuccess(getMockStopsForCity(city));
                            return;
                        }

                        String responseBody = res.body() != null ? res.body().string() : "";
                        List<String> stops = parseApiResponse(responseBody);

                        if (stops.isEmpty()) {
                            callback.onSuccess(getMockStopsForCity(city));
                        } else {
                            callback.onSuccess(stops);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse hatası: " + e.getMessage());
                        callback.onSuccess(getMockStopsForCity(city));
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Beklenmeyen Hata: " + e.getMessage());
            callback.onSuccess(getMockStopsForCity(city));
        }
    }

    /**
     * Belirtilen durak için ziyaretçi notu önerisi üretir.
     *
     * @param stopName Durak adı
     * @param city     Şehir adı
     * @param callback Sonuç callback'i
     */
    public void getNoteForStop(String stopName, String city, NoteCallback callback) {
        // API anahtarı yoksa mock not döndür
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.equals("unused")) {
            callback.onSuccess(getMockNoteForStop(stopName, city));
            return;
        }

        try {
            String prompt = "Sen bir seyahat rehberisin. " + city + " şehrindeki "
                    + stopName + " için ziyaretçilere 1 cümlelik kısa ve pratik bir ipucu ver. "
                    + "Sadece ipucunu yaz, başka hiçbir şey ekleme.";

            String requestBody = buildRequestBody(prompt);

            Request request = new Request.Builder()
                    .url(getBaseUrl())
                    .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onSuccess(getMockNoteForStop(stopName, city));
                    return;
                }
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                String text = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim();
                callback.onSuccess(text);
            }
        } catch (Exception e) {
            callback.onSuccess(getMockNoteForStop(stopName, city));
        }
    }

    private String buildPrompt(String city) {
        return "Sen bir seyahat uzmanısın. " + city + " şehri için 3 önemli turistik durak öner. " +
                "SADECE durak adlarını ver. Giriş cümlesi, açıklama veya sonuç cümlesi yazma. " +
                "Her durağı yeni bir satıra yaz ve başına sadece rakam ve nokta koy. " +
                "Örn:\n1. Durak Adı\n2. Durak Adı\n3. Durak Adı";
    }

    private String buildRequestBody(String prompt) {
        try {
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(textPart);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);

            return requestBody.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<String> parseApiResponse(String responseBody) {
        List<String> stops = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray candidates = jsonResponse.getJSONArray("candidates");
            if (candidates.length() > 0) {
                String text = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                String[] lines = text.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    // Satır boş değilse ve sadece açıklama cümlesi (örneğin "Madrid, tarihi...") değilse al
                    if (!line.isEmpty() && line.matches("^\\d+[\\.\\-\\s].*")) {
                        String stopName = line.replaceFirst("^\\d+[\\.\\-\\s]*", "").trim();
                        if (!stopName.isEmpty()) {
                            stops.add(stopName);
                        }
                    }
                    if (stops.size() >= 3) break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse hatası: " + e.getMessage());
        }
        return stops;
    }

    private List<String> getMockStopsForCity(String city) {
        List<String> stops = new ArrayList<>();
        String lowerCity = city.toLowerCase();

        if (lowerCity.contains("istanbul")) {
            stops.add("Ayasofya Camii"); stops.add("Kapalıçarşı"); stops.add("Boğaz Turu");
        } else if (lowerCity.contains("madrid")) {
            stops.add("Prado Müzesi"); stops.add("Retiro Parkı"); stops.add("Kraliyet Sarayı");
        } else {
            stops.add(city + " Tarihi Merkez");
            stops.add(city + " Şehir Müzesi");
            stops.add(city + " Yerel Pazarı");
        }
        return stops;
    }

    private String getMockNoteForStop(String stopName, String city) {
        String lower = stopName.toLowerCase();

        if (lower.contains("ayasofya"))      return "Sabah erken gidin, kuyruk kısa olur.";
        if (lower.contains("kapalıçarşı") || lower.contains("kapali")) return "Pazarlık yapmayı unutmayın, fiyatlar sabittir.";
        if (lower.contains("boğaz") || lower.contains("bogaz"))        return "Gün batımı turları en güzel manzarayı sunar.";
        if (lower.contains("anıtkabir") || lower.contains("anitkabir")) return "Değişim töreni için öğlen saatlerinde gidin.";
        if (lower.contains("göreme") || lower.contains("goreme"))      return "Sabah 8'den önce girin, kalabalıktan kaçının.";
        if (lower.contains("müze") || lower.contains("muze"))          return "Müze kartı ile ücretsiz giriş yapabilirsiniz.";
        if (lower.contains("kale"))          return "Gün batımında manzara muhteşem olur.";
        if (lower.contains("çarşı") || lower.contains("carsi") || lower.contains("pazar")) return "Yerel ürünleri tatmayı ihmal etmeyin.";
        if (lower.contains("şelale") || lower.contains("selale"))      return "Yağmur mevsiminde daha etkileyici görünür.";
        if (lower.contains("antik") || lower.contains("tarihi"))       return "Sesli rehber kiralayarak tarihi daha iyi öğrenebilirsiniz.";

        return city + "'deki bu durağı öğleden önce ziyaret etmenizi öneririz.";
    }

    public interface RouteCallback {
        void onSuccess(List<String> stops);
        void onError(String error);
    }

    /**
     * Durak notu callback'i.
     */
    public interface NoteCallback {
        void onSuccess(String note);
    }
}