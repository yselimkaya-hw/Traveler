package com.traveler.app.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.traveler.app.model.Trip;
import com.traveler.app.repository.TripRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DashboardViewModel - Dashboard Ekranı İş Mantığı Katmanı
 *
 * <p>MVVM mimarisinde ViewModel, View (Fragment/Activity) ile Repository
 * arasındaki köprüdür. UI ile ilgili verileri {@link LiveData} olarak tutar
 * ve yaşam döngüsü değişikliklerinden (ekran döndürme vb.) etkilenmez.</p>
 *
 * <p>{@link AndroidViewModel}'i extends ediyoruz çünkü Application context'ine
 * ihtiyacımız var. Normal ViewModel, context'e erişemez.</p>
 *
 * <p><b>LiveData kullanımının avantajı:</b> Fragment, LiveData'yı observe eder.
 * Veri değişince Fragment otomatik güncellenir; manuel yenileme gerekmez.</p>
 *
 * @author Traveler App
 * @version 1.0
 */
public class DashboardViewModel extends AndroidViewModel {

    /** Loglama için TAG */
    private static final String TAG = "DashboardViewModel";

    /** Veri katmanına erişim için repository referansı */
    private final TripRepository repository;

    /**
     * Gezi listesi için LiveData.
     * MutableLiveData: ViewModel içinden değiştirilebilir.
     * Fragment'a LiveData (immutable) olarak sunulur.
     */
    private final MutableLiveData<List<Trip>> tripsLiveData = new MutableLiveData<>();

    /**
     * Yükleme durumu LiveData'sı.
     * true = yükleniyor (ProgressBar görünür), false = tamamlandı
     */
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);

    /**
     * Hata mesajı LiveData'sı.
     * null = hata yok, string = hata var
     */
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>(null);

    /**
     * Toplam gezi sayısı LiveData'sı. İstatistikler için.
     */
    private final MutableLiveData<Integer> totalTripCountLiveData = new MutableLiveData<>(0);

    /**
     * Tamamlanan gezi sayısı LiveData'sı.
     */
    private final MutableLiveData<Integer> completedTripCountLiveData = new MutableLiveData<>(0);

    /**
     * Arka plan thread işlemleri için ExecutorService.
     * SQLite işlemleri main thread'de yapılmamalı - ANR'ı önler.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * ViewModel constructor'ı. AndroidViewModel tarafından otomatik çağrılır.
     *
     * @param application Uygulama nesnesi (context için)
     */
    public DashboardViewModel(@NonNull Application application) {
        super(application);
        repository = new TripRepository(application.getApplicationContext());
        // ViewModel oluşturulunca veriyi hemen yükle
        loadTrips();
    }

    // ===================== PUBLIC METODLAR (Fragment tarafından çağrılır) =====================

    /**
     * Tüm gezileri veritabanından asenkron olarak yükler.
     * LiveData güncellenir → Fragment otomatik yenilenir.
     */
    public void loadTrips() {
        isLoadingLiveData.setValue(true); // Yükleme başladı

        // SQLite işlemini arka planda yap - UI thread'i bloklamaz
        executor.execute(() -> {
            try {
                List<Trip> trips = repository.getAllTrips();
                int totalCount = repository.getTotalTripCount();
                int completedCount = repository.getCompletedTripCount();

                // LiveData güncellemeleri main thread'de yapılmalı
                tripsLiveData.postValue(trips);
                totalTripCountLiveData.postValue(totalCount);
                completedTripCountLiveData.postValue(completedCount);
                isLoadingLiveData.postValue(false);

                Log.d(TAG, "Geziler yüklendi: " + trips.size() + " adet");

            } catch (Exception e) {
                Log.e(TAG, "Gezi yükleme hatası: " + e.getMessage());
                errorLiveData.postValue("Geziler yüklenirken hata oluştu: " + e.getMessage());
                isLoadingLiveData.postValue(false);
            }
        });
    }

    /**
     * Belirtilen geziyi siler ve listeyi yeniler.
     *
     * @param tripId Silinecek gezi ID'si
     * @param onResult Silme sonucu callback (true=başarılı)
     */
    public void deleteTrip(int tripId, OnOperationResult onResult) {
        executor.execute(() -> {
            try {
                boolean success = repository.deleteTrip(tripId);
                if (success) {
                    // Silme başarılı, listeyi yenile
                    List<Trip> updatedTrips = repository.getAllTrips();
                    tripsLiveData.postValue(updatedTrips);
                    totalTripCountLiveData.postValue(repository.getTotalTripCount());
                    completedTripCountLiveData.postValue(repository.getCompletedTripCount());
                    Log.d(TAG, "Gezi silindi. ID: " + tripId);
                }
                onResult.onResult(success);

            } catch (Exception e) {
                Log.e(TAG, "Gezi silme hatası: " + e.getMessage());
                onResult.onResult(false);
            }
        });
    }

    /**
     * Verileri yeniler. Pull-to-refresh veya döndükten sonra çağrılabilir.
     */
    public void refreshData() {
        loadTrips();
    }

    // ===================== LIVEDATA GETTER'LAR (Fragment gözlemler) =====================

    /**
     * Gezi listesi LiveData'sını döner.
     * Fragment bu LiveData'yı observe ederek RecyclerView'ı günceller.
     *
     * @return Gezi listesi LiveData'sı
     */
    public LiveData<List<Trip>> getTripsLiveData() {
        return tripsLiveData;
    }

    /**
     * Yükleme durumu LiveData'sını döner.
     *
     * @return Boolean LiveData (true=yükleniyor)
     */
    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    /**
     * Hata mesajı LiveData'sını döner.
     *
     * @return String LiveData (null=hata yok)
     */
    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    /**
     * Toplam gezi sayısı LiveData'sını döner.
     *
     * @return Integer LiveData
     */
    public LiveData<Integer> getTotalTripCountLiveData() {
        return totalTripCountLiveData;
    }

    /**
     * Tamamlanan gezi sayısı LiveData'sını döner.
     *
     * @return Integer LiveData
     */
    public LiveData<Integer> getCompletedTripCountLiveData() {
        return completedTripCountLiveData;
    }

    // ===================== YAŞAM DÖNGÜSÜ =====================

    /**
     * ViewModel temizlenirken çağrılır. ExecutorService'i kapatır.
     * Bellek sızıntısını önler.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
        Log.d(TAG, "ViewModel temizlendi, ExecutorService kapatıldı.");
    }

    // ===================== CALLBACK INTERFACE =====================

    /**
     * Asenkron operasyonların sonucunu Fragment'a bildirmek için callback arayüzü.
     */
    public interface OnOperationResult {
        /**
         * Operasyon tamamlandığında çağrılır.
         * @param success İşlem başarılı mı?
         */
        void onResult(boolean success);
    }
}
