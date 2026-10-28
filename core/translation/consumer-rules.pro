# Vosk usa JNA para llamar a su librería nativa: R8 no debe renombrar estas clases
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class org.vosk.** { *; }
-dontwarn java.awt.**
