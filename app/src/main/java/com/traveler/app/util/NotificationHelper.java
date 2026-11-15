package com.traveler.app.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.traveler.app.R;
import com.traveler.app.view.MainActivity;

/**
 * NotificationHelper - Bildirim Yardımcı Sınıfı
 *
 * <p>Uygulama genelindeki bildirim gönderme işlemlerini merkezi olarak yönetir.
 * Farklı ekranlardan (AddTripFragment, TripDetailsFragment) çağrılabilir.</p>
 *
 * <p>Bildirim kanalı MainActivity'de oluşturulur; bu sınıf sadece bildirim gönderir.</p>
 *
 * @author Traveler App
 * @version 1.0
 */
public class NotificationHelper {

    /** Loglama için TAG */
    private static final String TAG = "NotificationHelper";

    /** Bildirim kanalı ID'si (MainActivity.NOTIFICATION_CHANNEL_ID ile aynı olmalı) */
    private static final String CHANNEL_ID = "traveler_notifications";

    /** Gezi kaydedildi bildirimi ID'si */
    private static final int NOTIF_ID_TRIP_SAVED = 1001;

    /** Gezi tamamlandı bildirimi ID'si */
    private static final int NOTIF_ID_TRIP_COMPLETED = 1002;

    /**
     * "Yeni gezi eklendi" bildirimi gönderir.
     * AddTripFragment tarafından gezi kaydedilince çağrılır.
     *
     * @param context   Uygulama context'i
     * @param tripTitle Kaydedilen gezinin başlığı
     */
    public static void sendTripSavedNotification(Context context, String tripTitle) {
        try {
            // Android 13+ için izin kontrolü
            if (!hasNotificationPermission(context)) {
                Log.w(TAG, "Bildirim izni yok, bildirim gönderilmedi.");
                return;
            }

            // Bildirime tıklandığında MainActivity'e git
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Bildirimi oluştur
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_location) // Bildirim ikonu
                    .setContentTitle("Yeni Gezi Eklendi! ✈️")
                    .setContentText(tripTitle + " gezin başarıyla planlandı.")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(tripTitle + " için rotanı oluşturduk. " +
                                    "Hadi maceraya başla!"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true) // Tıklanınca otomatik kapanır
                    .setColor(0xFF9F1C); // Bildirim vurgu rengi (turuncu)

            // Bildirimi gönder
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.notify(NOTIF_ID_TRIP_SAVED, builder.build());
                Log.d(TAG, "Bildirim gönderildi: " + tripTitle);
            }

        } catch (Exception e) {
            Log.e(TAG, "Bildirim gönderme hatası: " + e.getMessage());
        }
    }

    /**
     * "Gezi tamamlandı" bildirimi gönderir.
     * TripDetailsFragment tarafından gezi tamamlanınca çağrılır.
     *
     * @param context   Uygulama context'i
     * @param tripTitle Tamamlanan gezinin başlığı
     */
    public static void sendTripCompletedNotification(Context context, String tripTitle) {
        try {
            if (!hasNotificationPermission(context)) return;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_location)
                    .setContentTitle("Gezi Tamamlandı! 🎉")
                    .setContentText(tripTitle + " başarıyla tamamlandı.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setColor(0xFF2ECC71); // Yeşil - başarı rengi

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.notify(NOTIF_ID_TRIP_COMPLETED, builder.build());
            }

        } catch (Exception e) {
            Log.e(TAG, "Tamamlanma bildirimi hatası: " + e.getMessage());
        }
    }

    /**
     * Bildirim izninin verilip verilmediğini kontrol eder.
     * Android 13 (API 33) altında izin gerekmez.
     *
     * @param context Uygulama context'i
     * @return İzin varsa true
     */
    private static boolean hasNotificationPermission(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 13 altında izin gerekmiyor
    }
}
