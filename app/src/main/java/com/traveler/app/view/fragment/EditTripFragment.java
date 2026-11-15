package com.traveler.app.view.fragment;
import com.traveler.app.R;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.traveler.app.databinding.FragmentEditTripBinding;
import com.traveler.app.model.Trip;
import com.traveler.app.model.TripStop;
import com.traveler.app.repository.TripRepository;

import java.util.ArrayList;
import java.util.List;



public class EditTripFragment extends Fragment {

    private FragmentEditTripBinding binding;
    private TripRepository repository;
    private int tripId = -1;
    private Trip currentTrip;
    private List<TripStop> currentStops;

    // Her durak satırı için iki alanı takip etmek amacıyla basit bir yapı
    private static class StopRow {
        TripStop stop;          // null ise yeni eklenen durak
        TextInputEditText etName;
        TextInputEditText etNotes;
        boolean isDeleted = false;
    }

    private final List<StopRow> stopRows = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditTripBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new TripRepository(requireContext());

        if (getArguments() != null) {
            tripId = getArguments().getInt("tripId", -1);
        }

        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        loadAndFill();
        setupAddStopButton();
        setupSaveButton();
    }

    /** Mevcut veriyi çekip formu doldurur */
    private void loadAndFill() {
        if (tripId == -1) return;

        currentTrip = repository.getTripById(tripId);
        if (currentTrip == null) return;

        binding.etTripTitle.setText(currentTrip.getTitle());
        binding.etTripCity.setText(currentTrip.getCity());
        binding.etStartDate.setText(currentTrip.getStartDate());
        binding.etEndDate.setText(currentTrip.getEndDate());

        currentStops = repository.getStopsForTrip(tripId);
        for (TripStop stop : currentStops) {
            addStopRow(stop);
        }
    }

    /** Var olan veya yeni durak için bir satır ekler */
    private void addStopRow(@Nullable TripStop existingStop) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        // Durak adı alanı
        TextInputLayout tilName = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        tilName.setHint(existingStop != null
                ? existingStop.getOrderIndex() + ". Durak"
                : (stopRows.size() + 1) + ". Durak");
        tilName.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        tilName.setLayoutParams(params);

        TextInputEditText etName = new TextInputEditText(requireContext());
        etName.setTextColor(getResources().getColor(R.color.text_primary, null));
        if (existingStop != null) etName.setText(existingStop.getStopName());
        tilName.addView(etName);

        // Notlar alanı
        TextInputLayout tilNotes = new TextInputLayout(requireContext(), null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        tilNotes.setHint("Not (isteğe bağlı)");
        tilNotes.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        LinearLayout.LayoutParams paramsNotes = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        paramsNotes.setMargins(0, 0, 0, 4);
        tilNotes.setLayoutParams(paramsNotes);

        TextInputEditText etNotes = new TextInputEditText(requireContext());
        etNotes.setTextColor(getResources().getColor(R.color.text_primary, null));
        if (existingStop != null && existingStop.getNotes() != null)
            etNotes.setText(existingStop.getNotes());
        tilNotes.addView(etNotes);

        // Sil butonu
        com.google.android.material.button.MaterialButton btnDelete =
                new com.google.android.material.button.MaterialButton(requireContext(), null,
                        com.google.android.material.R.attr.borderlessButtonStyle);
        btnDelete.setText("✕ Bu durağı sil");
        btnDelete.setTextColor(getResources().getColor(R.color.status_error, null));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        deleteParams.setMargins(0, 0, 0, 16);
        btnDelete.setLayoutParams(deleteParams);

        // Satırı gruba topla
        LinearLayout group = new LinearLayout(requireContext());
        group.setOrientation(LinearLayout.VERTICAL);
        group.addView(tilName);
        group.addView(tilNotes);
        group.addView(btnDelete);
        binding.layoutStops.addView(group);

        StopRow row = new StopRow();
        row.stop = existingStop;
        row.etName = etName;
        row.etNotes = etNotes;
        stopRows.add(row);

        // Sil butonuna tıklanınca satırı gizle ve işaretle
        btnDelete.setOnClickListener(v -> {
            group.setVisibility(View.GONE);
            row.isDeleted = true;
        });
    }

    private void setupAddStopButton() {
        binding.btnAddStop.setOnClickListener(v -> addStopRow(null));
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> {
            String title = binding.etTripTitle.getText() != null
                    ? binding.etTripTitle.getText().toString().trim() : "";
            String city = binding.etTripCity.getText() != null
                    ? binding.etTripCity.getText().toString().trim() : "";

            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(city)) {
                Toast.makeText(requireContext(),
                        "Başlık ve şehir boş olamaz", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gezi bilgilerini güncelle
            currentTrip.setTitle(title);
            currentTrip.setCity(city);
            currentTrip.setStartDate(binding.etStartDate.getText() != null
                    ? binding.etStartDate.getText().toString().trim() : "");
            currentTrip.setEndDate(binding.etEndDate.getText() != null
                    ? binding.etEndDate.getText().toString().trim() : "");
            repository.updateTrip(currentTrip);

            // Durakları işle
            int order = 1;
            for (StopRow row : stopRows) {
                if (row.isDeleted) {
                    // Mevcut durağı sil
                    if (row.stop != null) repository.deleteStop(row.stop.getId());
                } else {
                    String name = row.etName.getText() != null
                            ? row.etName.getText().toString().trim() : "";
                    String notes = row.etNotes.getText() != null
                            ? row.etNotes.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(name)) continue;

                    if (row.stop != null) {
                        // Var olan durağı güncelle
                        row.stop.setStopName(name);
                        row.stop.setNotes(notes);
                        row.stop.setOrderIndex(order);
                        repository.updateStop(row.stop);
                    } else {
                        // Yeni durak ekle
                        TripStop newStop = new TripStop();
                        newStop.setTripId(tripId);
                        newStop.setStopName(name);
                        newStop.setNotes(notes);
                        newStop.setOrderIndex(order);
                        repository.saveStop(newStop);
                    }
                    order++;
                }
            }

            Toast.makeText(requireContext(), "Değişiklikler kaydedildi ✓",
                    Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}