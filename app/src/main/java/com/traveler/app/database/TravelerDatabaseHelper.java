package com.traveler.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.traveler.app.model.Trip;
import com.traveler.app.model.TripStop;

import java.util.ArrayList;
import java.util.List;

/**
 * TravelerDatabaseHelper - SQLite Veritabanı Yönetici Sınıfı
 *
 * <p>Bu sınıf {@link SQLiteOpenHelper}'ı genişleterek uygulamanın tüm
 * veritabanı işlemlerini yönetir. İki tablo içerir:</p>
 *
 * <ul>
 *   <li><b>trips:</b> Gezilerin ana bilgileri (başlık, şehir, tarihler...)</li>
 *   <li><b>trip_stops:</b> Her geziye ait duraklar (One-to-Many ilişkisi)</li>
 * </ul>
 *
 * <p><b>İlişki:</b> trips.id → trip_stops.trip_id (Foreign Key ile bağlı)</p>
 *
 * @author Traveler App
 * @version 1.0
 */
public class TravelerDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "TravelerDB";

    private static final String DATABASE_NAME = "traveler.db";

    private static final int DATABASE_VERSION = 1;

    // ===================== TABLO: trips =====================

    public static final String TABLE_TRIPS = "trips";

    /** trips tablosu sütun adları */
    public static final String COL_TRIP_ID = "id";
    public static final String COL_TRIP_TITLE = "title";
    public static final String COL_TRIP_CITY = "city";
    public static final String COL_TRIP_START_DATE = "start_date";
    public static final String COL_TRIP_END_DATE = "end_date";
    public static final String COL_TRIP_DESCRIPTION = "description";
    public static final String COL_TRIP_IS_COMPLETED = "is_completed";
    public static final String COL_TRIP_CREATED_AT = "created_at";

    /**
     * trips tablosunu oluşturan SQL ifadesi.
     * PRIMARY KEY AUTOINCREMENT: ID'ler otomatik atanır.
     */
    private static final String CREATE_TABLE_TRIPS =
            "CREATE TABLE " + TABLE_TRIPS + " (" +
            COL_TRIP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_TRIP_TITLE + " TEXT NOT NULL, " +
            COL_TRIP_CITY + " TEXT NOT NULL, " +
            COL_TRIP_START_DATE + " TEXT, " +
            COL_TRIP_END_DATE + " TEXT, " +
            COL_TRIP_DESCRIPTION + " TEXT, " +
            COL_TRIP_IS_COMPLETED + " INTEGER DEFAULT 0, " +
            COL_TRIP_CREATED_AT + " INTEGER DEFAULT 0" +
            ");";

    // ===================== TABLO: trip_stops =====================

    /** Durak tablosu adı */
    public static final String TABLE_TRIP_STOPS = "trip_stops";

    /** trip_stops tablosu sütun adları */
    public static final String COL_STOP_ID = "id";
    public static final String COL_STOP_TRIP_ID = "trip_id";      // Foreign Key
    public static final String COL_STOP_NAME = "stop_name";
    public static final String COL_STOP_NOTES = "notes";
    public static final String COL_STOP_ORDER = "order_index";
    public static final String COL_STOP_IS_VISITED = "is_visited";
    public static final String COL_STOP_DURATION = "estimated_duration_minutes";

    /**
     * trip_stops tablosunu oluşturan SQL ifadesi.
     * FOREIGN KEY: trip_id → trips.id bağlantısı. CASCADE ile parent silinince
     * child kayıtları da otomatik silinir.
     */
    private static final String CREATE_TABLE_TRIP_STOPS =
            "CREATE TABLE " + TABLE_TRIP_STOPS + " (" +
            COL_STOP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_STOP_TRIP_ID + " INTEGER NOT NULL, " +
            COL_STOP_NAME + " TEXT NOT NULL, " +
            COL_STOP_NOTES + " TEXT, " +
            COL_STOP_ORDER + " INTEGER DEFAULT 0, " +
            COL_STOP_IS_VISITED + " INTEGER DEFAULT 0, " +
            COL_STOP_DURATION + " INTEGER DEFAULT 60, " +
            "FOREIGN KEY(" + COL_STOP_TRIP_ID + ") REFERENCES " +
            TABLE_TRIPS + "(" + COL_TRIP_ID + ") ON DELETE CASCADE" +
            ");";

    // ===================== SINGLETON PATTERN =====================

    /** Tek örnek (Singleton) için static referans */
    private static TravelerDatabaseHelper instance;

    /**
     * Singleton getInstance metodu. Çoklu veritabanı bağlantısı açılmasını önler.
     *
     * @param context Uygulama context'i
     * @return TravelerDatabaseHelper tek örneği
     */
    public static synchronized TravelerDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TravelerDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Private constructor - Singleton için dışarıdan new ile oluşturma engellenir
     *
     * @param context Uygulama context'i
     */
    private TravelerDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ===================== YAŞAM DÖNGÜSÜ METODLARI =====================

    /**
     * Veritabanı ilk kez oluşturulduğunda çağrılır.
     * Tüm tabloları burada CREATE TABLE ile oluşturuyoruz.
     *
     * @param db Yeni oluşturulan SQLiteDatabase örneği
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Foreign key desteğini etkinleştir
            db.execSQL("PRAGMA foreign_keys=ON;");

            // Tabloları oluştur
            db.execSQL(CREATE_TABLE_TRIPS);
            Log.d(TAG, "Tablo oluşturuldu: " + TABLE_TRIPS);

            db.execSQL(CREATE_TABLE_TRIP_STOPS);
            Log.d(TAG, "Tablo oluşturuldu: " + TABLE_TRIP_STOPS);

        } catch (Exception e) {
            Log.e(TAG, "Tablo oluşturma hatası: " + e.getMessage());
        }
    }

    /**
     * Veritabanı versiyonu değiştiğinde çağrılır.
     * Mevcut tabloları silip yeniden oluşturur.
     *
     * @param db         Mevcut veritabanı
     * @param oldVersion Eski versiyon numarası
     * @param newVersion Yeni versiyon numarası
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Veritabanı güncelleniyor: v" + oldVersion + " → v" + newVersion);
        try {
            // Child tablo önce silinmeli (Foreign Key kısıtı nedeniyle)
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIP_STOPS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
            onCreate(db);
        } catch (Exception e) {
            Log.e(TAG, "Güncelleme hatası: " + e.getMessage());
        }
    }

    /**
     * Her veritabanı bağlantısı açıldığında çağrılır.
     * Foreign key'leri her açılışta etkinleştirmek gerekir.
     *
     * @param db Açılan veritabanı
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Foreign key desteği - Android'de varsayılan olarak kapalıdır
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    // ===================== TRIP CRUD OPERASYONLARI =====================

    /**
     * Yeni bir gezi veritabanına ekler.
     *
     * @param trip Eklenecek Trip nesnesi
     * @return Eklenen satırın ID'si, hata durumunda -1
     */
    public long insertTrip(Trip trip) {
        SQLiteDatabase db = this.getWritableDatabase();
        long insertedId = -1;

        try {
            // ContentValues: Sütun-değer çiftleri
            ContentValues values = new ContentValues();
            values.put(COL_TRIP_TITLE, trip.getTitle());
            values.put(COL_TRIP_CITY, trip.getCity());
            values.put(COL_TRIP_START_DATE, trip.getStartDate());
            values.put(COL_TRIP_END_DATE, trip.getEndDate());
            values.put(COL_TRIP_DESCRIPTION, trip.getDescription());
            values.put(COL_TRIP_IS_COMPLETED, trip.isCompleted() ? 1 : 0);
            values.put(COL_TRIP_CREATED_AT, System.currentTimeMillis());

            insertedId = db.insert(TABLE_TRIPS, null, values);
            Log.d(TAG, "Gezi eklendi. ID: " + insertedId);

        } catch (Exception e) {
            Log.e(TAG, "Gezi ekleme hatası: " + e.getMessage());
        }
        return insertedId;
    }

    /**
     * Tüm gezileri veritabanından çeker.
     * En son eklenenler başa gelecek şekilde sıralar.
     *
     * @return Trip nesnelerinin listesi
     */
    public List<Trip> getAllTrips() {
        List<Trip> tripList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // En yeni geziler başa gelsin
        String query = "SELECT * FROM " + TABLE_TRIPS +
                       " ORDER BY " + COL_TRIP_CREATED_AT + " DESC";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);

            // Cursor'ı iterate et ve her satırı Trip nesnesine dönüştür
            if (cursor.moveToFirst()) {
                do {
                    Trip trip = cursorToTrip(cursor);
                    tripList.add(trip);
                } while (cursor.moveToNext());
            }
            Log.d(TAG, "Toplam " + tripList.size() + " gezi yüklendi.");

        } catch (Exception e) {
            Log.e(TAG, "Gezi listesi hatası: " + e.getMessage());
        } finally {
            // Cursor'ı her zaman kapat - bellek sızıntısı önleme
            if (cursor != null) cursor.close();
        }
        return tripList;
    }

    /**
     * Belirli bir gezinin bilgilerini ID'ye göre getirir.
     *
     * @param tripId Sorgulanacak gezi ID'si
     * @return Bulunan Trip nesnesi, bulunamazsa null
     */
    public Trip getTripById(int tripId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Trip trip = null;
        Cursor cursor = null;

        try {
            cursor = db.query(
                TABLE_TRIPS,
                null,
                COL_TRIP_ID + " = ?",
                new String[]{String.valueOf(tripId)},
                null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                trip = cursorToTrip(cursor);
            }

        } catch (Exception e) {
            Log.e(TAG, "Gezi getirme hatası (ID:" + tripId + "): " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return trip;
    }

    /**
     * Gezinin tamamlanma durumunu günceller.
     *
     * @param tripId      Güncellenecek gezi ID'si
     * @param isCompleted Yeni tamamlanma durumu
     * @return Güncellenen satır sayısı (başarılıysa 1)
     */
    public int updateTripCompletionStatus(int tripId, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;

        try {
            ContentValues values = new ContentValues();
            values.put(COL_TRIP_IS_COMPLETED, isCompleted ? 1 : 0);

            rowsAffected = db.update(
                TABLE_TRIPS,
                values,
                COL_TRIP_ID + " = ?",
                new String[]{String.valueOf(tripId)}
            );
            Log.d(TAG, "Gezi durumu güncellendi. ID: " + tripId + ", Tamamlandı: " + isCompleted);

        } catch (Exception e) {
            Log.e(TAG, "Durum güncelleme hatası: " + e.getMessage());
        }
        return rowsAffected;
    }

    /**
     * Bir geziyi ID'ye göre siler. CASCADE nedeniyle bağlı tüm duraklar da silinir.
     *
     * @param tripId Silinecek gezi ID'si
     * @return Silinen satır sayısı (başarılıysa 1)
     */
    public int deleteTrip(int tripId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = 0;

        try {
            rowsDeleted = db.delete(
                TABLE_TRIPS,
                COL_TRIP_ID + " = ?",
                new String[]{String.valueOf(tripId)}
            );
            Log.d(TAG, "Gezi silindi. ID: " + tripId + ", Silinen satır: " + rowsDeleted);

        } catch (Exception e) {
            Log.e(TAG, "Gezi silme hatası: " + e.getMessage());
        }
        return rowsDeleted;
    }

    /**
     * Toplam gezi sayısını döner. İstatistik ekranı için kullanılır.
     *
     * @return Veritabanındaki toplam gezi sayısı
     */
    public int getTripCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRIPS, null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Sayım hatası: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    /**
     * Tamamlanan gezi sayısını döner.
     *
     * @return Tamamlanan gezi sayısı
     */
    public int getCompletedTripCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_TRIPS +
                " WHERE " + COL_TRIP_IS_COMPLETED + " = 1",
                null
            );
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Tamamlanan gezi sayımı hatası: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return count;
    }

    // ===================== TRIP STOP CRUD OPERASYONLARI =====================

    /**
     * Bir geziye yeni durak ekler.
     *
     * @param stop Eklenecek TripStop nesnesi
     * @return Eklenen satırın ID'si, hata durumunda -1
     */
    public long insertTripStop(TripStop stop) {
        SQLiteDatabase db = this.getWritableDatabase();
        long insertedId = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(COL_STOP_TRIP_ID, stop.getTripId());
            values.put(COL_STOP_NAME, stop.getStopName());
            values.put(COL_STOP_NOTES, stop.getNotes());
            values.put(COL_STOP_ORDER, stop.getOrderIndex());
            values.put(COL_STOP_IS_VISITED, stop.isVisited() ? 1 : 0);
            values.put(COL_STOP_DURATION, stop.getEstimatedDurationMinutes());

            insertedId = db.insert(TABLE_TRIP_STOPS, null, values);
            Log.d(TAG, "Durak eklendi. ID: " + insertedId + ", Gezi: " + stop.getTripId());

        } catch (Exception e) {
            Log.e(TAG, "Durak ekleme hatası: " + e.getMessage());
        }
        return insertedId;
    }

    /**
     * Belirli bir geziye ait tüm durakları sıralı şekilde getirir.
     *
     * @param tripId Durakları getirilecek gezi ID'si
     * @return Sıralı TripStop listesi
     */
    public List<TripStop> getStopsForTrip(int tripId) {
        List<TripStop> stopList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                TABLE_TRIP_STOPS,
                null,
                COL_STOP_TRIP_ID + " = ?",
                new String[]{String.valueOf(tripId)},
                null,
                null,
                COL_STOP_ORDER + " ASC" // Sıralama indeksine göre artan sırala
            );

            if (cursor.moveToFirst()) {
                do {
                    TripStop stop = cursorToTripStop(cursor);
                    stopList.add(stop);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "Durak listesi hatası (TripID:" + tripId + "): " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
        return stopList;
    }

    /**
     * Bir durağın ziyaret edilme durumunu günceller.
     *
     * @param stopId    Güncellenecek durak ID'si
     * @param isVisited Yeni ziyaret durumu
     * @return Güncellenen satır sayısı
     */
    public int updateStopVisitedStatus(int stopId, boolean isVisited) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;

        try {
            ContentValues values = new ContentValues();
            values.put(COL_STOP_IS_VISITED, isVisited ? 1 : 0);

            rowsAffected = db.update(
                TABLE_TRIP_STOPS,
                values,
                COL_STOP_ID + " = ?",
                new String[]{String.valueOf(stopId)}
            );

        } catch (Exception e) {
            Log.e(TAG, "Durak durumu güncelleme hatası: " + e.getMessage());
        }
        return rowsAffected;
    }

    /**
     * Belirli bir geziye ait tüm durakları toplu ekler.
     * Gemini API'den gelen rota önerilerini kaydetmek için kullanılır.
     *
     * @param stops Eklenecek durak listesi
     */
    public void insertAllStops(List<TripStop> stops) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Transaction ile toplu ekleme - performans için önemli
        db.beginTransaction();
        try {
            for (TripStop stop : stops) {
                ContentValues values = new ContentValues();
                values.put(COL_STOP_TRIP_ID, stop.getTripId());
                values.put(COL_STOP_NAME, stop.getStopName());
                values.put(COL_STOP_NOTES, stop.getNotes());
                values.put(COL_STOP_ORDER, stop.getOrderIndex());
                values.put(COL_STOP_IS_VISITED, 0);
                values.put(COL_STOP_DURATION, stop.getEstimatedDurationMinutes());
                db.insert(TABLE_TRIP_STOPS, null, values);
            }
            db.setTransactionSuccessful(); // Tüm işlemler başarılıysa commit et
            Log.d(TAG, stops.size() + " durak toplu eklendi.");

        } catch (Exception e) {
            Log.e(TAG, "Toplu durak ekleme hatası: " + e.getMessage());
            // Hata durumunda transaction rollback edilir
        } finally {
            db.endTransaction();
        }
    }

    // ===================== YARDIMCI METODLAR =====================

    /**
     * Cursor'daki mevcut satırı Trip nesnesine dönüştürür.
     * Kod tekrarını önlemek için tüm SELECT sorgularında kullanılır.
     *
     * @param cursor Veritabanı cursor'ı (moveToFirst/moveToNext çağrılmış olmalı)
     * @return Doldurulmuş Trip nesnesi
     */
    private Trip cursorToTrip(Cursor cursor) {
        Trip trip = new Trip();
        trip.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRIP_ID)));
        trip.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRIP_TITLE)));
        trip.setCity(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRIP_CITY)));
        trip.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRIP_START_DATE)));
        trip.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRIP_END_DATE)));
        trip.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRIP_DESCRIPTION)));
        trip.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRIP_IS_COMPLETED)) == 1);
        trip.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_TRIP_CREATED_AT)));
        return trip;
    }

    /**
     * Cursor'daki mevcut satırı TripStop nesnesine dönüştürür.
     *
     * @param cursor Veritabanı cursor'ı
     * @return Doldurulmuş TripStop nesnesi
     */
    private TripStop cursorToTripStop(Cursor cursor) {
        TripStop stop = new TripStop();
        stop.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STOP_ID)));
        stop.setTripId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STOP_TRIP_ID)));
        stop.setStopName(cursor.getString(cursor.getColumnIndexOrThrow(COL_STOP_NAME)));
        stop.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COL_STOP_NOTES)));
        stop.setOrderIndex(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STOP_ORDER)));
        stop.setVisited(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STOP_IS_VISITED)) == 1);
        stop.setEstimatedDurationMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(COL_STOP_DURATION)));
        return stop;
    }

    // Gezi bilgilerini günceller (başlık, şehir, tarihler, açıklama)
    public int updateTrip(Trip trip) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(COL_TRIP_TITLE, trip.getTitle());
        values.put(COL_TRIP_CITY, trip.getCity());
        values.put(COL_TRIP_START_DATE, trip.getStartDate());
        values.put(COL_TRIP_END_DATE, trip.getEndDate());
        values.put(COL_TRIP_DESCRIPTION, trip.getDescription());
        return db.update(TABLE_TRIPS, values, COL_TRIP_ID + "=?",
                new String[]{String.valueOf(trip.getId())});
    }

    // Durak adını ve notunu günceller
    public int updateStop(TripStop stop) {
        SQLiteDatabase db = getWritableDatabase();
        android.content.ContentValues values = new android.content.ContentValues();
        values.put(COL_STOP_NAME, stop.getStopName());
        values.put(COL_STOP_NOTES, stop.getNotes());
        values.put(COL_STOP_ORDER, stop.getOrderIndex());
        return db.update(TABLE_TRIP_STOPS, values, COL_STOP_ID + "=?",
                new String[]{String.valueOf(stop.getId())});
    }

    // Durağı siler
    public int deleteStop(int stopId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_TRIP_STOPS, COL_STOP_ID + "=?",
                new String[]{String.valueOf(stopId)});
    }
}
