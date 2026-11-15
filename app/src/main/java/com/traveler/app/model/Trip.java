package com.traveler.app.model;

/**
 * Trip (Gezi) Model Sınıfı
 *
 * <p>Bu sınıf, SQLite veritabanındaki "trips" tablosunun Java nesne karşılığıdır.
 * MVVM mimarisinde "Model" katmanını temsil eder.</p>
 *
 * <p>One-to-Many ilişkisi: Bir Trip, birden fazla TripStop içerebilir.</p>
 *
 * @author Traveler App
 * @version 1.0
 */
public class Trip {

    /** Veritabanı birincil anahtarı */
    private int id;

    /** Gezinin başlığı (örn: "İstanbul Turu") */
    private String title;

    /** Hedef şehir adı */
    private String city;

    /** Gezi başlangıç tarihi (String formatında: "YYYY-MM-DD") */
    private String startDate;

    /** Gezi bitiş tarihi */
    private String endDate;

    /** Kullanıcı notları veya gezi açıklaması */
    private String description;

    /** Gezinin tamamlanıp tamamlanmadığını gösteren bayrak (0=hayır, 1=evet) */
    private boolean isCompleted;

    /** Kaydın oluşturulma zamanı (timestamp) */
    private long createdAt;

    // ===================== CONSTRUCTOR'LAR =====================

    /**
     * Boş constructor - Room veya manuel nesne oluşturma için
     */
    public Trip() {}

    /**
     * Yeni gezi oluşturmak için tam parametreli constructor
     *
     * @param title     Gezi başlığı
     * @param city      Hedef şehir
     * @param startDate Başlangıç tarihi
     * @param endDate   Bitiş tarihi
     * @param description Açıklama
     */
    public Trip(String title, String city, String startDate, String endDate, String description) {
        this.title = title;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
    }

    // ===================== GETTER / SETTER'LAR =====================

    /** @return Gezi ID'sini döner */
    public int getId() { return id; }

    /** @param id Veritabanından gelen ID'yi set eder */
    public void setId(int id) { this.id = id; }

    /** @return Gezi başlığını döner */
    public String getTitle() { return title; }

    /** @param title Gezi başlığını set eder */
    public void setTitle(String title) { this.title = title; }

    /** @return Hedef şehri döner */
    public String getCity() { return city; }

    /** @param city Hedef şehri set eder */
    public void setCity(String city) { this.city = city; }

    /** @return Başlangıç tarihini döner */
    public String getStartDate() { return startDate; }

    /** @param startDate Başlangıç tarihini set eder */
    public void setStartDate(String startDate) { this.startDate = startDate; }

    /** @return Bitiş tarihini döner */
    public String getEndDate() { return endDate; }

    /** @param endDate Bitiş tarihini set eder */
    public void setEndDate(String endDate) { this.endDate = endDate; }

    /** @return Açıklamayı döner */
    public String getDescription() { return description; }

    /** @param description Açıklamayı set eder */
    public void setDescription(String description) { this.description = description; }

    /** @return Gezi tamamlandıysa true döner */
    public boolean isCompleted() { return isCompleted; }

    /** @param completed Tamamlanma durumunu set eder */
    public void setCompleted(boolean completed) { isCompleted = completed; }

    /** @return Oluşturma zamanını (timestamp) döner */
    public long getCreatedAt() { return createdAt; }

    /** @param createdAt Oluşturma zamanını set eder */
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /**
     * Debug ve loglama için nesnenin okunabilir string temsili
     * @return Trip nesnesinin string gösterimi
     */
    @Override
    public String toString() {
        return "Trip{id=" + id + ", title='" + title + "', city='" + city + "'}";
    }
}
