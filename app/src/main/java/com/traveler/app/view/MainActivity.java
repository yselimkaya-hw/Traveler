package com.traveler.app.view;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.traveler.app.R;
import com.traveler.app.databinding.ActivityMainBinding;

/**
 * MainActivity - Tek Aktivite (Single Activity) Mimarisi
 *
 * <p>Tüm uygulama bu tek Activity içinde yönetilir. Farklı ekranlar Fragment olarak
 * Navigation Component ile yönetilir. Bu mimari şunları sağlar:</p>
 * <ul>
 *   <li>Fragment geçişleri arası animasyonlar</li>
 *   <li>Back stack yönetimi</li>
 *   <li>Shared ViewModel kullanımı</li>
 * </ul>
 *
 * <p>İlk çalışmada onboarding durumunu kontrol eder:</p>
 * <ul>
 *   <li>Onboarding tamamlandıysa → Dashboard'a yönlendir</li>
 *   <li>İlk kullanımsa → Onboarding Fragment'ı göster</li>
 * </ul>
 *
 * @author Traveler App
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    /** Loglama için TAG */
    private static final String TAG = "MainActivity";

    /** ViewBinding referansı - tüm view'lara type-safe erişim */
    private ActivityMainBinding binding;

    /** Navigation Component controller'ı */
    private NavController navController;

    /** Bildirim izni istek kodu */
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    /** Bildirim kanalı ID'si - uygulama genelinde kullanılır */
    public static final String NOTIFICATION_CHANNEL_ID = "traveler_notifications";

    /** SharedPreferences dosya adı */
    public static final String PREFS_NAME = "TravelerPrefs";

    /** Onboarding tamamlandı anahtarı */
    public static final String PREF_ONBOARDING_DONE = "onboarding_done";

    /** Kullanıcı adı anahtarı */
    public static final String PREF_USER_NAME = "user_name";

    /** Koyu tema aktif anahtarı */
    public static final String PREF_DARK_MODE = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kaydedilen temayı uygula
        SharedPreferences prefs = getSharedPreferences("TravelerPrefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", true);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // ViewBinding ile layout'u bağla
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Bildirim kanalını oluştur (Android 8.0+ için zorunlu)
        createNotificationChannel();

        // Bildirim iznini iste (Android 13+ için zorunlu)
        requestNotificationPermission();

        // Navigation Component'ı kur
        setupNavigation();

        // Onboarding durumunu kontrol et ve yönlendir
        checkOnboardingStatus();
    }

    /**
     * Navigation Component'ı NavHostFragment ile bağlar.
     * Fragment geçişleri nav_graph.xml'deki action'larla yönetilir.
     */
    private void setupNavigation() {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);

            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                Log.d(TAG, "Navigation kuruldu.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Navigation kurulum hatası: " + e.getMessage());
        }
    }

    /**
     * Kullanıcının onboarding'i tamamlayıp tamamlamadığını kontrol eder.
     * SharedPreferences'dan okur; tamamlandıysa direkt Dashboard'a yönlendirir.
     */
    private void checkOnboardingStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean onboardingDone = prefs.getBoolean(PREF_ONBOARDING_DONE, false);

        if (onboardingDone && navController != null) {
            // Onboarding tamamlandı, direkt Dashboard'a git
            navController.navigate(R.id.dashboardFragment);
            Log.d(TAG, "Onboarding tamamlanmış, Dashboard'a yönlendiriliyor.");
        }
        // onboardingDone = false ise nav_graph'daki startDestination (OnboardingFragment) gösterilir
    }

    /**
     * Android 8.0 (API 26) ve üzeri için bildirim kanalı oluşturur.
     * Kanal olmadan bildirim gönderilemez.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence channelName = "Traveler Bildirimleri";
            String channelDescription = "Gezi kaydetme ve güncelleme bildirimleri";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    channelName,
                    importance
            );
            channel.setDescription(channelDescription);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Bildirim kanalı oluşturuldu.");
            }
        }
    }

    /**
     * Android 13 (API 33) ve üzeri için POST_NOTIFICATIONS iznini ister.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }

    /**
     * İzin istek sonucunu işler.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Bildirim izni verildi.");
            } else {
                Log.w(TAG, "Bildirim izni reddedildi.");
            }
        }
    }

    /**
     * Sistem geri tuşuna basılınca Navigation Component'ın back stack'ini kullan.
     */
    @Override
    public void onBackPressed() {
        if (navController != null && !navController.popBackStack()) {
            super.onBackPressed();
        }
    }
}
