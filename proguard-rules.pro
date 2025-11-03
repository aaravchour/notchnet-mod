-keep class aaravchour.notchnet.NotchNet { *; }
-keep class aaravchour.notchnet.NotchNetClient { *; }
-keep class aaravchour.notchnet.mixin.** { *; }
-keepclasseswithmembers class * {
    @net.fabricmc.api.Environment *;
}
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}