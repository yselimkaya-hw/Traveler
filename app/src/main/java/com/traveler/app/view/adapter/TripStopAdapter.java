package com.traveler.app.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.traveler.app.R;
import com.traveler.app.databinding.ItemTripStopBinding;
import com.traveler.app.model.TripStop;

import java.util.ArrayList;
import java.util.List;

/**
 * TripStopAdapter — Rota duraklarını (TripStop) RecyclerView'da gösteren adapter.
 *
 * <p>ViewBinding ile type-safe view erişimi sağlar. Durak ziyaret durumu
 * CheckBox ile toggle edilebilir; değişiklik {@link OnStopCheckedListener}
 * callback'i aracılığıyla Fragment'a iletilir.</p>
 *
 * @author  Traveler Dev Team
 * @version 1.0
 */
public class TripStopAdapter extends RecyclerView.Adapter<TripStopAdapter.TripStopViewHolder> {

    // -----------------------------------------------------------------------
    // Callback arayüzü
    // -----------------------------------------------------------------------

    /**
     * Bir durak ziyaret edildi / edilmedi olarak işaretlendiğinde tetiklenir.
     */
    public interface OnStopCheckedListener {
        /**
         * @param stop      Güncellenen durak nesnesi
         * @param isChecked Yeni ziyaret durumu
         */
        void onStopChecked(TripStop stop, boolean isChecked);
    }

    // -----------------------------------------------------------------------
    // Alanlar
    // -----------------------------------------------------------------------

    /** Gösterilecek durak listesi; adapter içinde kopyalanır. */
    private List<TripStop> stopList;

    /** Ziyaret durumu değişikliği için dış dinleyici. Null olabilir. */
    private OnStopCheckedListener checkedListener;

    // -----------------------------------------------------------------------
    // Kurucu
    // -----------------------------------------------------------------------

    /**
     * Adapter'ı başlangıçta boş bir liste ile oluşturur.
     */
    // -----------------------------------------------------------------------
    // Kurucu (Constructor) - Mevcut olanı bununla değiştir
    // -----------------------------------------------------------------------

    /**
     * Adapter'ı liste ve dinleyici ile oluşturur.
     */
    public TripStopAdapter(List<TripStop> stops, OnStopCheckedListener listener) {
        this.stopList = (stops != null) ? new ArrayList<>(stops) : new ArrayList<>();
        this.checkedListener = listener;
    }

    // -----------------------------------------------------------------------
    // Dinleyici atama
    // -----------------------------------------------------------------------

    /**
     * CheckBox değişim olayını dinlemek için listener atar.
     *
     * @param listener Durum değişimini işleyecek callback
     */
    public void setOnStopCheckedListener(OnStopCheckedListener listener) {
        this.checkedListener = listener;
    }

    // -----------------------------------------------------------------------
    // RecyclerView.Adapter metotları
    // -----------------------------------------------------------------------

    @NonNull
    @Override
    public TripStopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ViewBinding ile layout şişir
        ItemTripStopBinding binding = ItemTripStopBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TripStopViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TripStopViewHolder holder, int position) {
        TripStop stop = stopList.get(position);
        holder.bind(stop);
    }

    @Override
    public int getItemCount() {
        return stopList.size();
    }

    // -----------------------------------------------------------------------
    // Veri güncelleme
    // -----------------------------------------------------------------------

    /**
     * Adapter'ın durak listesini günceller ve RecyclerView'ı yeniden çizer.
     *
     * @param newStops Yeni durak listesi; null ise liste temizlenir
     */
    public void updateData(List<TripStop> newStops) {
        this.stopList = (newStops != null) ? new ArrayList<>(newStops) : new ArrayList<>();
        notifyDataSetChanged();
    }

    // -----------------------------------------------------------------------
    // ViewHolder
    // -----------------------------------------------------------------------

    /**
     * Her durak satırının görünümünü tutan ViewHolder.
     */
    class TripStopViewHolder extends RecyclerView.ViewHolder {

        /** ViewBinding referansı — tüm view erişimleri buradan yapılır. */
        private final ItemTripStopBinding binding;

        /**
         * @param binding Şişirilmiş item_trip_stop.xml binding'i
         */
        TripStopViewHolder(ItemTripStopBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * TripStop verisini view bileşenlerine bağlar.
         *
         * @param stop Gösterilecek durak nesnesi
         */
        void bind(TripStop stop) {
            // Sıra numarası + durak adı
            String orderLabel = stop.getOrderIndex() + ". Durak";
            binding.tvStopOrder.setText(orderLabel);
            binding.tvStopName.setText(stop.getStopName());

            // Notlar doluysa göster, boşsa gizle
            if (stop.getNotes() != null && !stop.getNotes().isEmpty()) {
                binding.tvStopNotes.setVisibility(View.VISIBLE);
                binding.tvStopNotes.setText(stop.getNotes());
            } else {
                binding.tvStopNotes.setVisibility(View.GONE);
            }

//            // Tahmini süre (dakika) 0'dan büyükse göster
//            if (stop.getEstimatedDurationMinutes() > 0) {
//                binding.tvStopDuration.setVisibility(View.VISIBLE);
//                String durationText = "⏱ " + stop.getEstimatedDurationMinutes() + " dk";
//                binding.tvStopDuration.setText(durationText);
//            } else {
//                binding.tvStopDuration.setVisibility(View.GONE);
//            }

            // CheckBox'ı mevcut ziyaret durumuna göre ayarla.
            // Listener'ı geçici kaldırarak istemeden tetiklenmeyi önle.
            binding.checkboxVisited.setOnCheckedChangeListener(null);
            binding.checkboxVisited.setChecked(stop.isVisited());

            // Ziyaret durumuna göre kart görünümünü güncelle
            updateCardVisuals(stop.isVisited());

            // Kullanıcı durum değiştirdiğinde callback'i çağır
            binding.checkboxVisited.setOnCheckedChangeListener((buttonView, isChecked) -> {
                stop.setVisited(isChecked);
                updateCardVisuals(isChecked);
                if (checkedListener != null) {
                    checkedListener.onStopChecked(stop, isChecked);
                }
            });
        }

        /**
         * Ziyaret durumuna göre kart alpha değerini ve metin stilini ayarlar.
         *
         * @param isVisited true ise kart soluklaştırılır (tamamlandı hissi)
         */
        private void updateCardVisuals(boolean isVisited) {
            // Ziyaret edilmişse kartı %60 saydamlığa düşür
            binding.getRoot().setAlpha(isVisited ? 0.6f : 1.0f);
            // Durak adı üstü çizili görünümü
            binding.tvStopName.setPaintFlags(
                    isVisited
                            ? binding.tvStopName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                            : binding.tvStopName.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            );
        }
    }
}
