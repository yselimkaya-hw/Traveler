package com.traveler.app.model;

/**
 * TripStop (Gezi Durağı) Model Sınıfı
 *
 * <p>Bir gezinin bireysel duraklarını temsil eder. Trip ile One-to-Many ilişkisi vardır:
 * Her {@link Trip} nesnesine ait birden fazla TripStop olabilir.</p>
 *
 * <p>SQLite'daki "trip_stops" tablosunun Java karşılığıdır.
 * Foreign key olarak {@code tripId} alanı kullanılır.</p>
 *
 * @author Traveler App
 * @version 1.0
 */
public class TripStop {

    /** Veritabanı birincil anahtarı */
    private int id;

    /**
     * Bu durağın bağlı olduğu gezi ID'si (Foreign Key).
     * trips.id'ye referans verir.
     */
    private int tripId;

    /** Durağın adı (örn: "Ayasofya", "Kapalıçarşı") */
    private String stopName;

    /** Durak hakkında kısa açıklama veya yapılacaklar */
    private String notes;

    /** Durak sıralaması (1, 2, 3...) */
    private int orderIndex;

    /** Bu durak ziyaret edildi mi? (0=hayır, 1=evet) */
    private boolean isVisited;

    /** Durağın tahmini ziyaret süresi (dakika cinsinden) */
    private int estimatedDurationMinutes;

    // ===================== CONSTRUCTOR'LAR =====================

    /**
     * Boş constructor
     */
    public TripStop() {}

    /**
     * Yeni durak oluşturmak için constructor
     *
     * @param tripId     Bağlı olduğu gezi ID'si
     * @param stopName   Durak adı
     * @param notes      Notlar/açıklama
     * @param orderIndex Sıralama indeksi
     */
    public TripStop(int tripId, String stopName, String notes, int orderIndex) {
        this.tripId = tripId;
        this.stopName = stopName;
        this.notes = notes;
        this.orderIndex = orderIndex;
        this.isVisited = false;
        this.estimatedDurationMinutes = 60; // Varsayılan 1 saat
    }

    // ===================== GETTER / SETTER'LAR =====================

    /** @return Durak ID'sini döner */
    public int getId() { return id; }

    /** @param id Durak ID'sini set eder */
    public void setId(int id) { this.id = id; }

    /** @return Bağlı gezi ID'sini döner */
    public int getTripId() { return tripId; }

    /** @param tripId Bağlı gezi ID'sini set eder */
    public void setTripId(int tripId) { this.tripId = tripId; }

    /** @return Durak adını döner */
    public String getStopName() { return stopName; }

    /** @param stopName Durak adını set eder */
    public void setStopName(String stopName) { this.stopName = stopName; }

    /** @return Notları döner */
    public String getNotes() { return notes; }

    /** @param notes Notları set eder */
    public void setNotes(String notes) { this.notes = notes; }

    /** @return Sıralama indeksini döner */
    public int getOrderIndex() { return orderIndex; }

    /** @param orderIndex Sıralama indeksini set eder */
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    /** @return Durak ziyaret edildiyse true döner */
    public boolean isVisited() { return isVisited; }

    /** @param visited Ziyaret durumunu set eder */
    public void setVisited(boolean visited) { isVisited = visited; }

    /** @return Tahmini süreyi dakika cinsinden döner */
    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }

    /** @param estimatedDurationMinutes Tahmini süreyi set eder */
    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    /**
     * Debug için string gösterimi
     * @return TripStop nesnesinin okunabilir gösterimi
     */
    @Override
    public String toString() {
        return "TripStop{id=" + id + ", tripId=" + tripId +
                ", stopName='" + stopName + "', order=" + orderIndex + "}";
    }
}
