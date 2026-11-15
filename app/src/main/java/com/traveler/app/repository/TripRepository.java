package com.traveler.app.repository;

import android.content.Context;
import android.util.Log;

import com.traveler.app.database.TravelerDatabaseHelper;
import com.traveler.app.model.Trip;
import com.traveler.app.model.TripStop;

import java.util.List;

/**
 * TripRepository - Veri Erişim Katmanı
 *
 * <p>MVVM mimarisinde Repository, ViewModel ile veri kaynakları (SQLite, API)
 * arasındaki köprü görevi görür. ViewModel doğrudan veritabanına erişmez;
 * her zaman Repository üzerinden erişir.</p>
 *
 * <p>Bu tasarım deseninin avantajları:</p>
 * <ul>
 *   <li>ViewModel, veri kaynağından bağımsız kalır</li>
 *   <li>Test yazımı kolaylaşır (mock repository kullanılabilir)</li>
 *   <li>Gelecekte Room veya API entegrasyonu kolayca yapılabilir</li>
 * </ul>
 *
 * @author Traveler App
 * @version 1.0
 */
public class TripRepository {

    /** Loglama için TAG */
    private static final String TAG = "TripRepository";

    /** Veritabanı helper referansı */
    private final TravelerDatabaseHelper dbHelper;

    /**
     * Repository constructor'ı
     *
     * @param context Uygulama context'i (DatabaseHelper için gerekli)
     */
    public TripRepository(Context context) {
        this.dbHelper = TravelerDatabaseHelper.getInstance(context);
    }

    // ===================== TRIP İŞLEMLERİ =====================

    /**
     * Yeni bir gezi kaydeder ve oluşturulan ID'yi döner.
     *
     * @param trip Kaydedilecek gezi nesnesi
     * @return Yeni gezi ID'si, başarısızsa -1
     */
    public long saveTrip(Trip trip) {
        try {
            long id = dbHelper.insertTrip(trip);
            Log.d(TAG, "Gezi başarıyla kaydedildi. ID: " + id);
            return id;
        } catch (Exception e) {
            Log.e(TAG, "Gezi kaydetme hatası: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Tüm gezileri listeler.
     *
     * @return Gezi listesi (boş olabilir, null değil)
     */
    public List<Trip> getAllTrips() {
        try {
            return dbHelper.getAllTrips();
        } catch (Exception e) {
            Log.e(TAG, "Tüm geziler getirme hatası: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Belirli bir geziyi ID ile getirir.
     *
     * @param tripId Getirilecek gezi ID'si
     * @return Trip nesnesi, bulunamazsa null
     */
    public Trip getTripById(int tripId) {
        try {
            return dbHelper.getTripById(tripId);
        } catch (Exception e) {
            Log.e(TAG, "Gezi getirme hatası: " + e.getMessage());
            return null;
        }
    }

    /**
     * Bir geziyi veritabanından siler.
     *
     * @param tripId Silinecek gezi ID'si
     * @return Silme başarılıysa true
     */
    public boolean deleteTrip(int tripId) {
        try {
            int result = dbHelper.deleteTrip(tripId);
            return result > 0;
        } catch (Exception e) {
            Log.e(TAG, "Gezi silme hatası: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gezinin tamamlanma durumunu günceller.
     *
     * @param tripId      Güncellenecek gezi ID'si
     * @param isCompleted Yeni durum
     * @return Güncelleme başarılıysa true
     */
    public boolean updateTripCompletion(int tripId, boolean isCompleted) {
        try {
            int result = dbHelper.updateTripCompletionStatus(tripId, isCompleted);
            return result > 0;
        } catch (Exception e) {
            Log.e(TAG, "Gezi güncelleme hatası: " + e.getMessage());
            return false;
        }
    }

    // ===================== İSTATİSTİK İŞLEMLERİ =====================

    /**
     * Toplam gezi sayısını döner.
     *
     * @return Toplam gezi sayısı
     */
    public int getTotalTripCount() {
        try {
            return dbHelper.getTripCount();
        } catch (Exception e) {
            Log.e(TAG, "Sayım hatası: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Tamamlanan gezi sayısını döner.
     *
     * @return Tamamlanan gezi sayısı
     */
    public int getCompletedTripCount() {
        try {
            return dbHelper.getCompletedTripCount();
        } catch (Exception e) {
            Log.e(TAG, "Tamamlanan gezi sayımı hatası: " + e.getMessage());
            return 0;
        }
    }

    // ===================== DURAK İŞLEMLERİ =====================

    /**
     * Bir geziye yeni durak ekler.
     *
     * @param stop Eklenecek TripStop nesnesi
     * @return Eklenen durak ID'si, başarısızsa -1
     */
    public long saveStop(TripStop stop) {
        try {
            return dbHelper.insertTripStop(stop);
        } catch (Exception e) {
            Log.e(TAG, "Durak ekleme hatası: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Bir geziye toplu durak ekler. Gemini AI'dan gelen öneriler için kullanılır.
     *
     * @param stops Eklenecek durak listesi
     */
    public void saveAllStops(List<TripStop> stops) {
        try {
            dbHelper.insertAllStops(stops);
        } catch (Exception e) {
            Log.e(TAG, "Toplu durak ekleme hatası: " + e.getMessage());
        }
    }

    /**
     * Belirli bir gezinin tüm duraklarını getirir.
     *
     * @param tripId Durakları istenen gezi ID'si
     * @return Durak listesi
     */
    public List<TripStop> getStopsForTrip(int tripId) {
        try {
            return dbHelper.getStopsForTrip(tripId);
        } catch (Exception e) {
            Log.e(TAG, "Durak listesi hatası: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Bir durağın ziyaret durumunu günceller.
     *
     * @param stopId    Güncellenecek durak ID'si
     * @param isVisited Ziyaret edildi mi?
     * @return Güncelleme başarılıysa true
     */
    public boolean updateStopVisited(int stopId, boolean isVisited) {
        try {
            int result = dbHelper.updateStopVisitedStatus(stopId, isVisited);
            return result > 0;
        } catch (Exception e) {
            Log.e(TAG, "Durak güncelleme hatası: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTrip(Trip trip) {
        try {
            return dbHelper.updateTrip(trip) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateStop(TripStop stop) {
        try {
            return dbHelper.updateStop(stop) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteStop(int stopId) {
        try {
            return dbHelper.deleteStop(stopId) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
