# Keep application classes
-keep class com.ubuntu.terminal.** {
    *;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve Kotlin metadata
-keep class kotlin.Metadata {
    *;
}
