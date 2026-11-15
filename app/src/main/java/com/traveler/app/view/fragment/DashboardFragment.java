package com.traveler.app.view.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.traveler.app.R;
import com.traveler.app.databinding.FragmentDashboardBinding;
import com.traveler.app.model.Trip;
import com.traveler.app.view.adapter.TripAdapter;
import com.traveler.app.viewmodel.DashboardViewModel;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * DashboardFragment - Ana Sayfa Ekranı
 *
 * <p>Kaydedilen tüm gezileri RecyclerView ile listeleyen ana ekrandır.
 * MVVM mimarisine uygun olarak:</p>
 * <ul>
 *   <li>ViewModel'den LiveData'yı observe eder</li>
 *   <li>Kullanıcı etkileşimlerini ViewModel'e iletir</li>
 *   <li>UI durumunu (boş liste, yükleme, hata) yönetir</li>
 * </ul>
 *
 * @author Traveler App
 * @version 1.0
 */
public class DashboardFragment extends Fragment {

    private final String[] quotes = {
            "\"Dünya bir kitaptır ve seyahat etmeyenler onun sadece bir sayfasını okurlar.\"\n— Aziz Augustinus",
            "\"Gerçek keşif; yeni yerler bulmak değil, yeni gözlerle bakabilmektir.\"\n— Marcel Proust",
            "\"Gittiğin yer her neresiyse, tüm kalbinle git.\"\n— Konfüçyüs",
            "\"Yolculuk, önce seni sözsüz bırakır, sonra da iyi bir hikayeci yapar.\"\n— İbn Battuta",
            "\"Kıyıyı gözden kaybetmeye cesaret edemeyenler, yeni okyanuslar keşfedemezler.\"\n— André Gide",
            "\"Seyahat etmek yaşamaktır.\"\n— Hans Christian Andersen",
            "\"Uzaklara gitmek, geri döndüğünde her şeyi aynı bulsan bile senin değişmiş olmandır.\"\n— Anita Desai",
            "\"Mesele bir yere varmak değil, yolun kendisidir.\"\n— Johann Wolfgang von Goethe",
            "\"Ne kadar uzağa gidersem kendime o kadar yakınlaşıyorum.\"\n— Andrew McCarthy",
            "\"Yolculuk etmek, her şeyin başında bir önyargı katilidir.\"\n— Mark Twain"
    };

    private int currentQuoteIndex = 0;
    private final android.os.Handler quoteHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable quoteRunnable;



    /** ViewBinding referansı - null kontrolü için @Nullable */
    private FragmentDashboardBinding binding;

    /** Dashboard için ViewModel */
    private DashboardViewModel viewModel;

    /** RecyclerView adapter'ı */
    private TripAdapter tripAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // ViewBinding ile layout'u bağla
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModel'i başlat
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Bileşenleri kur
        setupWelcomeMessage();
        setupRecyclerView();
        setupFabButton();
        observeViewModel();
        setupSettingsButton();
        setupQuoteRotation();
    }

    /**
     * SharedPreferences'dan kullanıcı adını okuyarak karşılama mesajını günceller.
     */
    private void setupWelcomeMessage() {
        try {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("TravelerPrefs", MODE_PRIVATE);
            String userName = prefs.getString("user_name", "Gezgin");
            binding.tvWelcome.setText("Merhaba, " + userName + "! 👋");
        } catch (Exception e) {
            binding.tvWelcome.setText("Merhaba, Gezgin! 👋");
        }
    }

    private void setupQuoteRotation() {
        // Rastgele bir sözden başla
        currentQuoteIndex = new java.util.Random().nextInt(quotes.length);
        binding.tvQuote.setText(quotes[currentQuoteIndex]);

        quoteRunnable = new Runnable() {
            @Override
            public void run() {
                currentQuoteIndex = (currentQuoteIndex + 1) % quotes.length;

                // Fade out
                binding.tvQuote.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction(() -> {
                            binding.tvQuote.setText(quotes[currentQuoteIndex]);
                            // Fade in
                            binding.tvQuote.animate()
                                    .alpha(1f)
                                    .setDuration(400)
                                    .start();
                        }).start();

                quoteHandler.postDelayed(this, 4000);
            }
        };

        quoteHandler.postDelayed(quoteRunnable, 4000);
    }

    /**
     * RecyclerView'ı konfigüre eder.
     * LinearLayoutManager ve TripAdapter ile başlatır.
     * Adapter callback'leri ile tıklama ve silme işlemlerini yönetir.
     */
    private void setupRecyclerView() {
        tripAdapter = new TripAdapter(new ArrayList<>(),
            // Gezi kartına tıklanınca → Detay ekranına git
            trip -> {
                Bundle args = new Bundle();
                args.putInt("tripId", trip.getId());
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_dashboard_to_tripDetails, args);
            },
            // Uzun basınca → Silme dialog'u göster
            trip -> showDeleteConfirmationDialog(trip)
        );

        // LinearLayoutManager: dikey liste
        binding.rvTrips.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTrips.setAdapter(tripAdapter);

        // RecyclerView animasyonları için item animator zaten varsayılan olarak aktif
    }

    /**
     * FAB (Floating Action Button) tıklamasını ayarlar.
     * Yeni gezi ekleme ekranına navigate eder.
     */
    private void setupFabButton() {
        binding.fabAddTrip.setOnClickListener(v ->
            Navigation.findNavController(v).navigate(R.id.action_dashboard_to_addTrip)
        );
    }
    private void setupSettingsButton() {
        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_dashboard_to_settings)
        );
    }
    /**
     * ViewModel'deki LiveData'ları observe eder.
     * Veri değişince UI otomatik güncellenir.
     */
    private void observeViewModel() {
        // Gezi listesi değişince RecyclerView'ı güncelle
        viewModel.getTripsLiveData().observe(getViewLifecycleOwner(), trips -> {
            updateTripList(trips);
        });

        // Yükleme durumu değişince ProgressBar'ı göster/gizle
        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Hata mesajı gelirse Toast göster
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Toplam gezi sayısını güncelle
        viewModel.getTotalTripCountLiveData().observe(getViewLifecycleOwner(), count -> {
            binding.tvTripCount.setText(count + " Gezi");
        });
    }

    /**
     * Gezi listesine göre RecyclerView veya boş durum görünümünü gösterir.
     *
     * @param trips Güncel gezi listesi
     */
    private void updateTripList(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            // Boş durum: "Henüz bir gezi eklenmedi" mesajını ve ikonu göster
            binding.rvTrips.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            // Liste dolu: RecyclerView'ı göster, boş durumu gizle
            binding.rvTrips.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
            tripAdapter.updateData(trips);
        }
    }

    /**
     * Silme onayı için AlertDialog gösterir.
     * Kullanıcı onaylarsa ViewModel üzerinden silme işlemi başlatılır.
     *
     * @param trip Silinecek gezi nesnesi
     */
    private void showDeleteConfirmationDialog(Trip trip) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Geziyi Sil")
                .setMessage("\"" + trip.getTitle() + "\" gezisini silmek istediğinizden emin misiniz?")
                .setPositiveButton("Evet, Sil", (dialog, which) -> {
                    // Silme işlemini ViewModel üzerinden başlat
                    viewModel.deleteTrip(trip.getId(), success -> {
                        requireActivity().runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(requireContext(),
                                        "Gezi silindi ✓", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Silme işlemi başarısız.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("İptal", null) // İptal: dialog kapanır, işlem yapılmaz
                .setIcon(R.drawable.ic_delete) // Silme ikonu
                .show();
    }

    /**
     * Fragment görünür hale gelince veriyi yeniler.
     * Başka fragmenttan dönünce liste güncel olur.
     */
    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshData();
        setupWelcomeMessage();
    }

    /**
     * Fragment yok edilince binding'i null yaparak bellek sızıntısını önler.
     * ViewBinding kullanırken bu adım kritik öneme sahiptir.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        quoteHandler.removeCallbacks(quoteRunnable);
        binding = null; // Bellek sızıntısını önle
    }
}
