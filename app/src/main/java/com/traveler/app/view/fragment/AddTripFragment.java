package com.traveler.app.view.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.traveler.app.R;
import com.traveler.app.databinding.FragmentAddTripBinding;
import com.traveler.app.model.Trip;
import com.traveler.app.model.TripStop;
import com.traveler.app.repository.TripRepository;
import com.traveler.app.util.GeminiApiHelper;
import com.traveler.app.util.NotificationHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * AddTripFragment - Yeni Gezi Ekleme Ekranı
 *
 * <p>Kullanıcının yeni gezi ekleyebildiği ekrandır. İki modda çalışır:</p>
 * <ol>
 *   <li><b>Manuel mod:</b> Kullanıcı durakları kendisi girer</li>
 *   <li><b>AI mod:</b> Gemini API'den asenkron rota önerisi alır,
 *       EditText'lere otomatik doldurur. Her durak için not önerisi de gelir.</li>
 * </ol>
 *
 * <p>Kaydet butonuna basılınca Trip ve TripStop nesneleri SQLite'a yazılır,
 * kullanıcıya bildirim gönderilir.</p>
 *
 * @author Traveler App
 * @version 1.0
 */
public class AddTripFragment extends Fragment {
    private final List<TextInputEditText> stopFields = new ArrayList<>();
    // YENİ: Her durağın not alanını takip eder
    private final List<TextInputEditText> noteFields = new ArrayList<>();

    /** ViewBinding referansı */
    private FragmentAddTripBinding binding;

    /** Veri katmanı erişimi */
    private TripRepository repository;

    /** Gemini AI yardımcı sınıfı */
    private GeminiApiHelper geminiHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddTripBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new TripRepository(requireContext());
        geminiHelper = new GeminiApiHelper();

        setupBackButton();
        setupDatePickers();
        setupAiSwitch();
        setupSaveButton();

        // Başlangıçta 3 boş durak alanı göster
        addStopField(null);
        addStopField(null);
        addStopField(null);
        setupAddStopButton();
    }

    /**
     * Geri butonunu yapılandırır.
     */
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack()
        );
    }

    /**
     * Tarih seçici dialog'larını yapılandırır.
     * EditText'e tıklanınca DatePickerDialog açılır.
     */
    private void setupDatePickers() {
        binding.etStartDate.setOnClickListener(v -> showDatePicker(true));
        binding.etEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    /**
     * DatePickerDialog gösterir ve seçilen tarihi ilgili EditText'e yazar.
     *
     * @param isStartDate true=başlangıç tarihi, false=bitiş tarihi
     */
    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(requireContext(),
                (datePicker, selectedYear, selectedMonth, selectedDay) -> {
                    // Ay 0 tabanlı olduğu için +1 ekliyoruz
                    String date = String.format("%04d-%02d-%02d",
                            selectedYear, selectedMonth + 1, selectedDay);

                    if (isStartDate) {
                        binding.etStartDate.setText(date);
                    } else {
                        binding.etEndDate.setText(date);
                    }
                }, year, month, day);

        picker.show();
    }

    /**
     * AI planlama switch'ini yapılandırır.
     * Switch açılınca şehir girilmişse AI çağrısı başlatılır.
     */
    private void setupAiSwitch() {
        binding.switchAiPlan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String city = binding.etCity.getText().toString().trim();

                if (TextUtils.isEmpty(city)) {
                    // Şehir girilmemişse switch'i geri kapat ve uyar
                    binding.switchAiPlan.setChecked(false);
                    Toast.makeText(requireContext(),
                            "Önce hedef şehri girin", Toast.LENGTH_SHORT).show();
                    binding.etCity.requestFocus();
                    return;
                }
                // AI çağrısını başlat
                fetchAiRoute(city);
            }
        });
    }

    /**
     * Gemini AI'dan şehir için rota önerisi alır.
     * Arka planda asenkron çalışır; sonuç UI thread'inde işlenir.
     *
     * @param city Rota oluşturulacak şehir
     */
    private void fetchAiRoute(String city) {
        // Yükleniyor göstergesini göster
        binding.layoutAiLoading.setVisibility(View.VISIBLE);
        binding.btnSaveTrip.setEnabled(false);

        // API çağrısı arka planda yapılır
        new Thread(() -> {
            geminiHelper.getRouteForCity(city, new GeminiApiHelper.RouteCallback() {

                @Override
                public void onSuccess(List<String> stops) {
                    // UI güncellemeleri mutlaka main thread'de!
                    requireActivity().runOnUiThread(() -> {
                        // YENİ: city parametresi eklendi (notlar için)
                        fillStopsFromAi(stops, city);
                        binding.layoutAiLoading.setVisibility(View.GONE);
                        binding.btnSaveTrip.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "✨ AI rotanı oluşturdu!", Toast.LENGTH_SHORT).show();

                        // Eğer gezi başlığı boşsa şehri otomatik doldur
                        if (TextUtils.isEmpty(binding.etTripTitle.getText())) {
                            binding.etTripTitle.setText(city + " Turu");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        binding.layoutAiLoading.setVisibility(View.GONE);
                        binding.btnSaveTrip.setEnabled(true);
                        binding.switchAiPlan.setChecked(false);
                        Toast.makeText(requireContext(),
                                "AI hatası: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        }).start();
    }

    /**
     * AI'dan gelen durak önerilerini EditText'lere doldurur.
     * YENİ: city parametresi eklendi — her durak için not önerisi alınır.
     *
     * @param stops AI tarafından önerilen durak adları
     * @param city  Hedef şehir (not önerileri için)
     */
    private void fillStopsFromAi(List<String> stops, String city) {
        // Mevcut alanları temizle
        binding.layoutStops.removeAllViews();
        stopFields.clear();
        noteFields.clear(); // YENİ

        // AI'dan gelen her durak için alan oluştur
        for (String stop : stops) {
            addStopField(stop);
        }

        // AI 3'ten az önerdi ise kalan alanları boş ekle
        while (stopFields.size() < 3) {
            addStopField(null);
        }

        // YENİ: Her durak için AI'dan not önerisi al
        for (int i = 0; i < stops.size(); i++) {
            final int index = i;
            final String stopName = stops.get(i);
            if (stopName.isEmpty()) continue;

            new Thread(() -> {
                geminiHelper.getNoteForStop(stopName, city, note -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        if (binding == null) return;
                        if (noteFields.size() > index) {
                            noteFields.get(index).setText(note);
                        }
                    });
                });
            }).start();
        }
    }

    /**
     * Kaydet butonunu yapılandırır. Validasyon yaparak veriyi SQLite'a yazar.
     */
    private void setupSaveButton() {
        binding.btnSaveTrip.setOnClickListener(v -> {
            if (validateForm()) {
                saveTrip();
            }
        });
    }

    /**
     * Form validasyonu. Zorunlu alanların dolu olup olmadığını kontrol eder.
     *
     * @return Form geçerliyse true
     */
    private boolean validateForm() {
        String city = binding.etCity.getText().toString().trim();
        String tripTitle = binding.etTripTitle.getText().toString().trim();

        if (TextUtils.isEmpty(city)) {
            binding.tilCity.setError("Şehir zorunludur");
            return false;
        }
        binding.tilCity.setError(null);

        if (TextUtils.isEmpty(tripTitle)) {
            binding.tilTripTitle.setError("Gezi başlığı zorunludur");
            return false;
        }
        binding.tilTripTitle.setError(null);

        return true;
    }

    /**
     * Trip ve TripStop nesnelerini oluşturup veritabanına kaydeder.
     * Başarılı kayıt sonrası bildirim gönderilir ve önceki ekrana dönülür.
     */
    private void saveTrip() {
        try {
            // Trip nesnesini oluştur
            Trip trip = new Trip(
                    binding.etTripTitle.getText().toString().trim(),
                    binding.etCity.getText().toString().trim(),
                    binding.etStartDate.getText().toString().trim(),
                    binding.etEndDate.getText().toString().trim(),
                    binding.etDescription.getText().toString().trim()
            );

            // Veritabanına kaydet, dönen ID ile durakları bağla
            long tripId = repository.saveTrip(trip);

            if (tripId == -1) {
                Toast.makeText(requireContext(),
                        "Kayıt başarısız. Tekrar deneyin.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Durakları oluştur ve kaydet
            List<TripStop> stops = buildStopsList((int) tripId);
            if (!stops.isEmpty()) {
                repository.saveAllStops(stops);
            }

            // Başarılı kayıt bildirimi gönder
            NotificationHelper.sendTripSavedNotification(
                    requireContext(), trip.getTitle()
            );

            // Başarı Toast mesajı
            Toast.makeText(requireContext(),
                    "Gezi kaydedildi! ✓", Toast.LENGTH_SHORT).show();

            // Dashboard'a geri dön
            Navigation.findNavController(requireView()).popBackStack();

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * EditText'lerdeki durak bilgilerinden TripStop listesi oluşturur.
     * YENİ: noteFields de okunarak not veritabanına yazılır.
     *
     * @param tripId Bağlı gezi ID'si
     * @return TripStop listesi
     */
    private List<TripStop> buildStopsList(int tripId) {
        List<TripStop> stops = new ArrayList<>();
        int order = 1;
        for (int i = 0; i < stopFields.size(); i++) {
            String name = stopFields.get(i).getText() != null
                    ? stopFields.get(i).getText().toString().trim() : "";
            // YENİ: Not alanını oku (yoksa boş string)
            String note = (noteFields.size() > i && noteFields.get(i).getText() != null)
                    ? noteFields.get(i).getText().toString().trim() : "";
            if (!TextUtils.isEmpty(name)) {
                stops.add(new TripStop(tripId, name, note, order++));
            }
        }
        return stops;
    }

    private void addStopField(String initialValue) {
        // Durak adı alanı
        TextInputLayout til = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint((stopFields.size() + 1) + ". Durak");
        til.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 6);
        til.setLayoutParams(params);

        TextInputEditText et = new TextInputEditText(requireContext());
        et.setTextColor(getResources().getColor(R.color.text_primary, null));
        if (initialValue != null) et.setText(initialValue);
        til.addView(et);

        binding.layoutStops.addView(til);
        stopFields.add(et);

        // YENİ: Durak adının hemen altına not alanı ekle
        TextInputLayout tilNote = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        tilNote.setHint("Not");
        tilNote.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        noteParams.setMargins(0, 0, 0, 16);
        tilNote.setLayoutParams(noteParams);

        TextInputEditText etNote = new TextInputEditText(requireContext());
        etNote.setTextColor(getResources().getColor(R.color.text_primary, null));
        tilNote.addView(etNote);

        binding.layoutStops.addView(tilNote);
        noteFields.add(etNote);
    }

    private void setupAddStopButton() {
        binding.btnAddStop.setOnClickListener(v -> addStopField(null));
    }

    /** Bellek sızıntısını önlemek için binding'i temizle */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}