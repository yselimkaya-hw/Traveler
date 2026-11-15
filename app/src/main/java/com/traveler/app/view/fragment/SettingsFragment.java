package com.traveler.app.view.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.traveler.app.databinding.FragmentSettingsBinding;
import com.traveler.app.repository.TripRepository;

import static android.content.Context.MODE_PRIVATE;

/**
 * SettingsFragment - Ayarlar ve İstatistikler Ekranı
 *
 * <p>İki ana bölüm içerir:</p>
 * <ol>
 *   <li><b>Tema Kontrolü:</b> Dark Mode Switch - SharedPreferences'a kaydeder</li>
 *   <li><b>İstatistikler:</b> Toplam ve tamamlanan gezi sayıları</li>
 * </ol>
 *
 * @author Traveler App
 * @version 1.0
 */
public class SettingsFragment extends Fragment {

    /** ViewBinding referansı */
    private FragmentSettingsBinding binding;

    /** SharedPreferences referansı */
    private SharedPreferences prefs;

    /** Veri erişim katmanı */
    private TripRepository repository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireActivity().getSharedPreferences("TravelerPrefs", MODE_PRIVATE);
        repository = new TripRepository(requireContext());

        setupBackButton();
        setupDarkModeSwitch();
        loadStatistics();
        loadUserInfo();
        setupAvatarPicker();
        setupEditNameButton();
    }

    /** Geri butonunu yapılandırır */
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack()
        );
    }

    /**
     * Dark Mode switch'ini yapılandırır.
     * Mevcut değeri SharedPreferences'dan okur; değişince kaydeder ve temayı uygular.
     */
    private void setupDarkModeSwitch() {
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);
        binding.switchDarkMode.setChecked(isDarkMode);

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Activity'yi yeniden başlat — navigation stack'ini koru
            requireActivity().recreate();
        });
    }

    /**
     * Veritabanından istatistikleri yükler ve UI'ya yansıtır.
     */
    private void loadStatistics() {
        try {
            int totalTrips = repository.getTotalTripCount();
            int completedTrips = repository.getCompletedTripCount();
            int activeTrips = totalTrips - completedTrips;

            binding.tvTotalTrips.setText(String.valueOf(totalTrips));
            binding.tvCompletedTrips.setText(String.valueOf(completedTrips));
            binding.tvActiveTrips.setText(String.valueOf(activeTrips));

        } catch (Exception e) {
            binding.tvTotalTrips.setText("0");
            binding.tvCompletedTrips.setText("0");
            binding.tvActiveTrips.setText("0");
        }
    }

    /**
     * Kullanıcı bilgilerini SharedPreferences'tan yükler.
     */
    private void loadUserInfo() {
        String userName = prefs.getString("user_name", "Gezgin");
        String avatar = prefs.getString("user_avatar", "🧭");
        binding.tvUserName.setText(userName);
        binding.tvUserInitial.setText(avatar);
    }
    private void setupEditNameButton() {
        binding.btnEditName.setOnClickListener(v -> {
            // Mevcut ismi al
            String currentName = prefs.getString("user_name", "Gezgin");

            // EditText içeren dialog aç
            android.widget.EditText editText = new android.widget.EditText(requireContext());
            editText.setText(currentName);
            editText.setSelection(currentName.length()); // İmleci sona götür

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("İsmi Düzenle")
                    .setView(editText)
                    .setPositiveButton("Kaydet", (dialog, which) -> {
                        String newName = editText.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            // SharedPreferences'a kaydet
                            prefs.edit().putString("user_name", newName).apply();
                            // Ekrandaki ismi güncelle
                            binding.tvUserName.setText(newName);
                            binding.tvUserInitial.setText(String.valueOf(newName.charAt(0)).toUpperCase());
                        }
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        });
    }

    private void setupAvatarPicker() {
        binding.tvUserInitial.setOnClickListener(v -> showAvatarDialog());
    }

    private void showAvatarDialog() {
        String[] emojis = {
                "🧭", "✈️", "🏔️", "🏖️", "🗺️", "🎒", "🚂", "⛵",
                "🏕️", "🌍", "🦁", "🐘", "🌺", "🍜", "📸", "🎨",
                "🦅", "🏛️", "🌊", "🎿", "🚴", "🧗", "🛶", "🌋"
        };

        // Grid görünümü için adapter
        android.widget.GridView gridView = new android.widget.GridView(requireContext());
        gridView.setNumColumns(6);
        gridView.setPadding(24, 24, 24, 24);
        gridView.setVerticalSpacing(8);
        gridView.setHorizontalSpacing(8);

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                emojis) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                android.widget.TextView tv = new android.widget.TextView(requireContext());
                tv.setText(emojis[position]);
                tv.setTextSize(28);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(8, 8, 8, 8);
                return tv;
            }
        };
        gridView.setAdapter(adapter);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Avatar Seç")
                .setView(gridView)
                .setNegativeButton("İptal", null)
                .create();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedEmoji = emojis[position];
            // SharedPreferences'a kaydet
            prefs.edit().putString("user_avatar", selectedEmoji).apply();
            // UI'ı güncelle
            binding.tvUserInitial.setText(selectedEmoji);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
