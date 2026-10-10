# Keep all application code, services, receivers, activities, models, and UI
-keep class com.example.** { *; }
-keepclassmembers class com.example.** { *; }

# Keep JavaScript Interfaces for WebView HTML5 bridge
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-dontwarn **
