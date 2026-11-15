package com.traveler.app.view.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.traveler.app.R;
import com.traveler.app.databinding.FragmentTripDetailsBinding;
import com.traveler.app.model.Trip;
import com.traveler.app.model.TripStop;
import com.traveler.app.repository.TripRepository;
import com.traveler.app.util.NotificationHelper;
import com.traveler.app.view.adapter.TripStopAdapter;



import java.util.List;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import java.util.ArrayList;

/**
 * TripDetailsFragment - Gezi Detay Ekranı
 *
 * <p>Seçilen gezinin durak listesini gösterir. Her durak için
 * "ziyaret edildi" checkbox'ı içerir. Gezi tamamlama butonu ile
 * tüm gezi tamamlandı olarak işaretlenebilir.</p>
 *
 * <p>Nav argument: {@code tripId} - DashboardFragment'tan geçirilir</p>
 *
 * @author Traveler App
 * @version 1.0
 */
public class TripDetailsFragment extends Fragment {

    /** ViewBinding referansı */
    private FragmentTripDetailsBinding binding;

    /** Veri erişim katmanı */
    private TripRepository repository;

    /** Gösterilen gezi ID'si (nav argument'tan gelir) */
    private int tripId = -1;

    /** Durak listesi adapter'ı */
    private TripStopAdapter stopAdapter;
    private Trip currentTrip;
    private List<TripStop> currentStops;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTripDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new TripRepository(requireContext());

        // Navigation argument'tan tripId'yi al
        if (getArguments() != null) {
            tripId = getArguments().getInt("tripId", -1);
        }

        setupBackButton();
        loadTripDetails();
        setupCompleteButton();
        setupEditButton();
        setupMap();
    }

    /** Geri butonunu yapılandırır */
    private void setupBackButton() {
        binding.btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack()
        );
    }

    /**
     * Gezi bilgilerini ve durak listesini yükler.
     */
    private void loadTripDetails() {
        if (tripId == -1) {
            Toast.makeText(requireContext(), "Gezi bulunamadı", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Ana gezi bilgilerini yükle
            currentTrip = repository.getTripById(tripId);
            Trip trip = currentTrip;
            if (trip != null) {
                binding.tvTripTitle.setText(trip.getTitle());
                binding.tvTripCity.setText("📍 " + trip.getCity());

                String dateRange = (trip.getStartDate() != null && !trip.getStartDate().isEmpty())
                        ? trip.getStartDate() + " → " + trip.getEndDate()
                        : "Tarih belirtilmedi";
                binding.tvTripDate.setText(dateRange);

                // Tamamlanmış ise butonu güncelle
                if (trip.isCompleted()) {
                    binding.btnCompleteTrip.setText("Tamamlandı ✓");
                    binding.btnCompleteTrip.setEnabled(false);
                }
            }

            // Durak listesini yükle
            currentStops = repository.getStopsForTrip(tripId);
            List<TripStop> stops = currentStops;
            setupStopsRecyclerView(stops);

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Yükleme hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Durak listesi RecyclerView'ını kurar.
     *
     * @param stops Gösterilecek durak listesi
     */
    private void setupStopsRecyclerView(List<TripStop> stops) {
        stopAdapter = new TripStopAdapter(stops,
                // Durak checkbox tıklandığında veritabanı güncelle
                (stop, isChecked) -> {
                    repository.updateStopVisited(stop.getId(), isChecked);
                    if (isChecked) {
                        Toast.makeText(requireContext(),
                                stop.getStopName() + " ziyaret edildi ✓",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        binding.rvStops.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStops.setAdapter(stopAdapter);

        // Boş durum kontrolü
        if (stops.isEmpty()) {
            binding.layoutNoStops.setVisibility(View.VISIBLE);
            binding.rvStops.setVisibility(View.GONE);
        }
    }

    /**
     * "Geziyi Tamamla" butonunu yapılandırır.
     * Onay dialog'u sonrası veritabanını günceller ve bildirim gönderir.
     */
    private void setupCompleteButton() {
        binding.btnCompleteTrip.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Geziyi Tamamla")
                    .setMessage("Bu geziyi tamamlandı olarak işaretlemek istiyor musunuz?")
                    .setPositiveButton("Evet, Tamamla", (dialog, which) -> {
                        completeTrip();
                    })
                    .setNegativeButton("İptal", null)
                    .show();
        });
    }
    private void setupEditButton() {
        binding.btnEditTrip.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("tripId", tripId);
            Navigation.findNavController(v)
                    .navigate(R.id.action_tripDetails_to_editTrip, args);
        });
    }
    /**
     * Geziyi tamamlandı olarak işaretler, bildirim gönderir.
     */
    private void completeTrip() {
        try {
            boolean success = repository.updateTripCompletion(tripId, true);
            if (success) {
                Trip trip = repository.getTripById(tripId);
                String tripTitle = trip != null ? trip.getTitle() : "Gezi";

                // Tamamlandı bildirimi gönder
                NotificationHelper.sendTripCompletedNotification(requireContext(), tripTitle);

                // UI güncelle
                binding.btnCompleteTrip.setText("Tamamlandı ✓");
                binding.btnCompleteTrip.setEnabled(false);

                Toast.makeText(requireContext(),
                        "Tebrikler! " + tripTitle + " tamamlandı 🎉",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) binding.mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
//    private void setupMap() {
//        // OSMDroid yapılandırması
//        Configuration.getInstance().setUserAgentValue(
//                requireContext().getPackageName());
//
//        MapView map = binding.mapView;
//        map.setTileSource(TileSourceFactory.MAPNIK);
//        map.setMultiTouchControls(true);
//        map.getController().setZoom(13.0);
//
//        // Şehir merkezini al ve haritayı oraya konumlandır
//        GeoPoint cityCenter = getCityCoordinates(currentTrip != null ? currentTrip.getCity() : null);
//        map.getController().setCenter(cityCenter);
//
//        // Duraklar yoksa sadece şehir merkezini göster
//        if (currentStops == null || currentStops.isEmpty()) {
//            Marker cityMarker = new Marker(map);
//            cityMarker.setPosition(cityCenter);
//            cityMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//            cityMarker.setTitle(currentTrip != null ? currentTrip.getCity() : "Şehir");
//            map.getOverlays().add(cityMarker);
//            return;
//        }
//
//        // Her durak için pin ekle (şehir merkezine yakın konumlar)
//        List<GeoPoint> routePoints = new ArrayList<>();
////        double latOffset = -0.012;
////        double lngOffset = -0.020;
//
//        for (int i = 0; i < currentStops.size(); i++) {
//            TripStop stop = currentStops.get(i);
//            // Durakları şehir merkezinin etrafına dağıt
//            double angle = (2 * Math.PI * i) / currentStops.size();
//            double radius = 0.018;
//            double lat = cityCenter.getLatitude()  + radius * Math.sin(angle);
//            double lng = cityCenter.getLongitude() + radius * Math.cos(angle) * 1.4;
//
//            GeoPoint point = new GeoPoint(lat, lng);
//            routePoints.add(point);
//
//            Marker marker = new Marker(map);
//            marker.setPosition(point);
//            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//            marker.setTitle((i + 1) + ". " + stop.getStopName());
//            marker.setSnippet(stop.isVisited() ? "✓ Ziyaret edildi" : "Henüz ziyaret edilmedi");
//            map.getOverlays().add(marker);
//        }
//
//        // Durakları birbirine bağlayan rota çizgisi
//        if (routePoints.size() > 1) {
//            Polyline polyline = new Polyline();
//            polyline.setPoints(routePoints);
//            polyline.getOutlinePaint().setColor(0xFFFF9F1C); // turuncu
//            polyline.getOutlinePaint().setStrokeWidth(5f);
//            map.getOverlays().add(polyline);
//        }
//
//        // Haritayı tüm pinleri gösterecek şekilde ayarla
//        map.invalidate();
//    }

private void setupMap() {
    Configuration.getInstance().setUserAgentValue(
            requireContext().getPackageName());

    MapView map = binding.mapView;
    map.setTileSource(TileSourceFactory.MAPNIK);
    map.setMultiTouchControls(true);

    GeoPoint cityCenter = getCityCoordinates(
            currentTrip != null ? currentTrip.getCity() : null);
    map.getController().setZoom(13.0);
    map.getController().setCenter(cityCenter);

    if (currentStops == null || currentStops.isEmpty()) {
        Marker cityMarker = new Marker(map);
        cityMarker.setPosition(cityCenter);
        cityMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        cityMarker.setTitle(currentTrip != null ? currentTrip.getCity() : "Şehir");
        map.getOverlays().add(cityMarker);
        return;
    }

    // Her durak için Nominatim'den gerçek koordinat çek
    fetchAndPlaceMarkers(map, cityCenter);
}
    private void fetchAndPlaceMarkers(MapView map, GeoPoint cityCenter) {
        List<GeoPoint> routePoints = new ArrayList<>();

        // Tüm durakları sırayla işle — bir önceki bitmeden diğeri başlamasın
        new Thread(() -> {
            for (int i = 0; i < currentStops.size(); i++) {
                TripStop stop = currentStops.get(i);
                final int index = i;

                // Nominatim rate limit: istekler arası en az 1.2 saniye bekle
                if (i > 0) {
                    try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                }

                GeoPoint point = geocodeStopName(
                        stop.getStopName(),
                        currentTrip != null ? currentTrip.getCity() : "");

                routePoints.add(point);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (binding == null) return;

                    // Pin ekle
                    Marker marker = new Marker(map);
                    marker.setPosition(point);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setTitle((index + 1) + ". " + stop.getStopName());
                    marker.setSnippet(stop.isVisited()
                            ? "✓ Ziyaret edildi" : "Henüz ziyaret edilmedi");
                    map.getOverlays().add(marker);

                    // Son durak eklendiyse çizgiyi çiz
                    if (routePoints.size() == currentStops.size() && currentStops.size() > 1) {
                        Polyline polyline = new Polyline();
                        polyline.setPoints(new ArrayList<>(routePoints));
                        polyline.getOutlinePaint().setColor(0xFFFF9F1C);
                        polyline.getOutlinePaint().setStrokeWidth(5f);
                        map.getOverlays().add(polyline);
                    }

                    map.invalidate();
                });
            }
        }).start();
    }
//    private void fetchAndPlaceMarkers(MapView map, GeoPoint cityCenter) {
//        List<GeoPoint> routePoints = new ArrayList<>();
//        int total = currentStops.size();
//
//        // Her durak için ayrı thread'de geocoding yap
//        for (int i = 0; i < total; i++) {
//            TripStop stop = currentStops.get(i);
//            int index = i;
//
////            new Thread(() -> {
////                GeoPoint point = geocodeStopName(stop.getStopName(),
////                        currentTrip != null ? currentTrip.getCity() : "");
//
//            new Thread(() -> {
//                try {
//                    // Her durak arasında 1.2 saniye bekle
//                    Thread.sleep(index * 1200L);
//                } catch (InterruptedException ignored) {}
//
//                GeoPoint point = geocodeStopName(stop.getStopName(),
//                        currentTrip != null ? currentTrip.getCity() : "");
//
//
//                // UI thread'e dön
//                if (getActivity() == null) return;
//                getActivity().runOnUiThread(() -> {
//                    if (binding == null) return;
//
//                    routePoints.add(point);
//
//                    Marker marker = new Marker(map);
//                    marker.setPosition(point);
//                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//                    marker.setTitle((index + 1) + ". " + stop.getStopName());
//                    marker.setSnippet(stop.isVisited()
//                            ? "✓ Ziyaret edildi" : "Henüz ziyaret edilmedi");
//                    map.getOverlays().add(marker);
//
//                    // Son durak eklendiyse çizgiyi çiz
//                    if (routePoints.size() == total && total > 1) {
//                        Polyline polyline = new Polyline();
//                        polyline.setPoints(routePoints);
//                        polyline.getOutlinePaint().setColor(0xFFFF9F1C);
//                        polyline.getOutlinePaint().setStrokeWidth(5f);
//                        map.getOverlays().add(polyline);
//                    }
//
//                    map.invalidate();
//                });
//            }).start();
//        }
//    }

    private GeoPoint geocodeStopName(String stopName, String city) {
        try {
            // Şehir adını da ekleyerek daha doğru sonuç al
            String query = stopName + ", " + city + ", Turkey";
            String encoded = java.net.URLEncoder.encode(query, "UTF-8");
            String url = "https://nominatim.openstreetmap.org/search?q="
                    + encoded + "&format=json&limit=1";

            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", requireContext().getPackageName())
                    .build();

            okhttp3.Response response = client.newCall(request).execute();
            if (response.body() != null) {
                String body = response.body().string();
                org.json.JSONArray arr = new org.json.JSONArray(body);
                if (arr.length() > 0) {
                    org.json.JSONObject obj = arr.getJSONObject(0);
                    double lat = obj.getDouble("lat");
                    double lon = obj.getDouble("lon");
                    return new GeoPoint(lat, lon);
                }
            }
        } catch (Exception e) {
            // Geocoding başarısız olursa şehir merkezine düş
        }

        // Fallback: şehir merkezi
        return getCityCoordinates(city);
    }
    private GeoPoint getCityCoordinates(String city) {
        if (city == null) return new GeoPoint(39.9334, 32.8597);
        String c = city.toLowerCase().trim();
        if (c.contains("istanbul") || c.contains("İstanbul")) return new GeoPoint(41.0082, 28.9784);
        if (c.contains("ankara"))  return new GeoPoint(39.9334, 32.8597);
        if (c.contains("izmir") || c.contains("İzmir"))   return new GeoPoint(38.4192, 27.1287);
        if (c.contains("antalya")) return new GeoPoint(36.8969, 30.7133);
        if (c.contains("bursa"))   return new GeoPoint(40.1826, 29.0665);
        if (c.contains("kapadokya") || c.contains("nevşehir")) return new GeoPoint(38.6431, 34.8289);
        if (c.contains("trabzon")) return new GeoPoint(41.0015, 39.7178);
        if (c.contains("konya"))   return new GeoPoint(37.8746, 32.4932);
        if (c.contains("bodrum"))  return new GeoPoint(37.0344, 27.4305);
        if (c.contains("pamukkale") || c.contains("denizli")) return new GeoPoint(37.9214, 29.1200);
        return new GeoPoint(39.9334, 32.8597); // varsayılan: Ankara
    }
}
