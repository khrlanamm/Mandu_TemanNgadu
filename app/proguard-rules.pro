# Aturan umum yang penting
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowshrinking class * {
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Aturan untuk Firebase
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }

# --- ATURAN BARU YANG PENTING ---
# Melindungi SEMUA kelas Model, ViewModel, Repository, dan Adapter di seluruh proyek
# Tanda '**' berarti melindungi semua kelas di dalam paket dan sub-paketnya

# Melindungi paket model utama
-keep class com.khrlanamm.mandu.model.** { *; }
-keepnames class com.khrlanamm.mandu.model.** { *; }
-keepclassmembers class com.khrlanamm.mandu.model.** { *; }

# Melindungi semua kelas di dalam paket 'ui' (termasuk detail, history, dll)
-keep class com.khrlanamm.mandu.ui.** { *; }
-keepnames class com.khrlanamm.mandu.ui.** { *; }
-keepclassmembers class com.khrlanamm.mandu.ui.** { *; }

# Melindungi kelas Service (untuk notifikasi)
-keep class com.khrlanamm.mandu.service.** { *; }
-keepnames class com.khrlanamm.mandu.service.** { *; }
-keepclassmembers class com.khrlanamm.mandu.service.** { *; }