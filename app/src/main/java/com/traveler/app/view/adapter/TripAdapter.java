package com.traveler.app.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.traveler.app.databinding.ItemTripCardBinding;
import com.traveler.app.model.Trip;

import java.util.List;

/**
 * TripAdapter - Gezi Kartları RecyclerView Adapter'ı
 *
 * <p>RecyclerView için adapter sınıfı. Her gezi için {@link ItemTripCardBinding}
 * ile MaterialCardView'ı bağlar. ViewBinding kullanarak view'lara erişir.</p>
 *
 * <p>İki tür callback destekler:</p>
 * <ul>
 *   <li>{@link OnTripClickListener}: Normal tıklama → Detay ekranı</li>
 *   <li>{@link OnTripLongClickListener}: Uzun basma → Silme dialog'u</li>
 * </ul>
 *
 * @author Traveler App
 * @version 1.0
 */
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    /** Gösterilecek gezi listesi */
    private List<Trip> trips;

    /** Tıklama callback'i */
    private final OnTripClickListener clickListener;

    /** Uzun basma callback'i */
    private final OnTripLongClickListener longClickListener;

    /**
     * Adapter constructor'ı
     *
     * @param trips             Gezi listesi
     * @param clickListener     Tıklama callback'i
     * @param longClickListener Uzun basma callback'i
     */
    public TripAdapter(List<Trip> trips,
                       OnTripClickListener clickListener,
                       OnTripLongClickListener longClickListener) {
        this.trips = trips;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ViewBinding ile her item için binding oluştur
        ItemTripCardBinding itemBinding = ItemTripCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new TripViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = trips.get(position);
        holder.bind(trip);
    }

    @Override
    public int getItemCount() {
        return trips != null ? trips.size() : 0;
    }

    /**
     * Listeyi yeni veriyle günceller ve RecyclerView'ı yeniler.
     *
     * @param newTrips Yeni gezi listesi
     */
    public void updateData(List<Trip> newTrips) {
        this.trips = newTrips;
        notifyDataSetChanged(); // Basit güncelleme; DiffUtil ile optimize edilebilir
    }

    // ===================== VIEWHOLDER =====================

    /**
     * TripViewHolder - Her gezi kartı için ViewHolder
     *
     * <p>RecyclerView performansı için view'ları burada cache'leriz.
     * ViewBinding kullanarak type-safe erişim sağlarız.</p>
     */
    class TripViewHolder extends RecyclerView.ViewHolder {

        /** Item layout binding'i */
        private final ItemTripCardBinding itemBinding;

        /**
         * ViewHolder constructor'ı
         * @param binding Item layout binding'i
         */
        TripViewHolder(ItemTripCardBinding binding) {
            super(binding.getRoot());
            this.itemBinding = binding;
        }

        /**
         * Trip nesnesinin verilerini view'lara bağlar.
         *
         * @param trip Gösterilecek gezi nesnesi
         */
        void bind(Trip trip) {
            // Temel bilgileri set et
            itemBinding.tvTripTitle.setText(trip.getTitle());
            itemBinding.tvTripCity.setText("📍 " + trip.getCity());

            // Tarih bilgisi
            if (trip.getStartDate() != null && !trip.getStartDate().isEmpty()) {
                itemBinding.tvTripDate.setText(trip.getStartDate() + " → " + trip.getEndDate());
            } else {
                itemBinding.tvTripDate.setText("Tarih belirtilmedi");
            }

            // Tamamlanma durumuna göre badge rengini ayarla
            if (trip.isCompleted()) {
                itemBinding.tvStatusBadge.setText("Tamamlandı ✓");
                itemBinding.tvStatusBadge.setBackgroundResource(
                        com.traveler.app.R.drawable.badge_completed);
            } else {
                itemBinding.tvStatusBadge.setText("Planlandı");
                itemBinding.tvStatusBadge.setBackgroundResource(
                        com.traveler.app.R.drawable.badge_planned);
            }

            // Tıklama olayı: Detay ekranına git
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTripClick(trip);
                }
            });

            // Uzun basma olayı: Silme seçeneği
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onTripLongClick(trip);
                }
                return true; // true: event tüketildi, başka listener çağrılmaz
            });
        }
    }

    // ===================== CALLBACK INTERFACE'LER =====================

    /**
     * Gezi kartına normal tıklama callback'i
     */
    public interface OnTripClickListener {
        /**
         * @param trip Tıklanan gezi nesnesi
         */
        void onTripClick(Trip trip);
    }

    /**
     * Gezi kartına uzun basma callback'i
     */
    public interface OnTripLongClickListener {
        /**
         * @param trip Uzun basılan gezi nesnesi
         */
        void onTripLongClick(Trip trip);
    }
}
