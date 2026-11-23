# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.minhtrong67.moody.**$$serializer { *; }
-keepclassmembers class com.minhtrong67.moody.** { *** Companion; }
-keepclasseswithmembers class com.minhtrong67.moody.** { kotlinx.serialization.KSerializer serializer(...); }
