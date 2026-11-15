package com.traveler.app.view.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.traveler.app.R;
import com.traveler.app.databinding.FragmentOnboardingBinding;

import static android.content.Context.MODE_PRIVATE;

/**
 * OnboardingFragment - Karşılama ve İsim Kayıt Ekranı
 *
 * <p>Uygulamanın ilk çalışmasında gösterilir. Kullanıcıdan isim alır ve
 * SharedPreferences'a kaydeder. Onboarding tamamlandıktan sonra bir daha
 * gösterilmez (MainActivity'de kontrol edilir).</p>
 *
 * <p>Kaydedilen veriler:</p>
 * <ul>
 *   <li>{@code user_name}: Kullanıcı adı</li>
 *   <li>{@code onboarding_done}: Onboarding tamamlandı bayrağı (true)</li>
 * </ul>
 *
 * @author Traveler App
 * @version 1.0
 */
public class OnboardingFragment extends Fragment {

    /** ViewBinding referansı */
    private FragmentOnboardingBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupStartButton();
    }

    /**
     * "Başla" butonunu yapılandırır.
     * İsim validasyonu yaparak SharedPreferences'a kaydeder ve Dashboard'a geçer.
     */
    private void setupStartButton() {
        binding.btnStart.setOnClickListener(v -> {
            String userName = binding.etUserName.getText().toString().trim();

            // Validasyon: İsim boş bırakılamaz
            if (TextUtils.isEmpty(userName)) {
                binding.tilUserName.setError("Lütfen adınızı girin");
                Toast.makeText(requireContext(),
                        "Adını girmeden devam edemezsin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hata mesajını temizle
            binding.tilUserName.setError(null);

            // SharedPreferences'a kaydet
            saveUserData(userName);

            // Hoş geldin mesajı
            Toast.makeText(requireContext(),
                    "Hoş geldin, " + userName + "! 🎉", Toast.LENGTH_SHORT).show();

            // Dashboard'a navigate et (Onboarding back stack'ten kaldırılır)
            Navigation.findNavController(v)
                    .navigate(R.id.action_onboarding_to_dashboard);
        });

        // EditText'e yazılınca hata mesajını kaldır
        binding.etUserName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) binding.tilUserName.setError(null);
        });
    }

    /**
     * Kullanıcı verilerini SharedPreferences'a kaydeder.
     *
     * @param userName Kullanıcının girdiği isim
     */
    private void saveUserData(String userName) {
        SharedPreferences.Editor editor = requireActivity()
                .getSharedPreferences("TravelerPrefs", MODE_PRIVATE)
                .edit();

        editor.putString("user_name", userName);
        editor.putBoolean("onboarding_done", true);
        editor.putBoolean("dark_mode", true); // Varsayılan: koyu tema
        editor.apply(); // apply() asenkron; commit() senkron ama yavaş
    }

    /** Binding'i temizle - bellek sızıntısını önle */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
