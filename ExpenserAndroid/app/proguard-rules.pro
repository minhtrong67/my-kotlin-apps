# Firebase, Credential Manager, Coil, WorkManager and Compose ship their own consumer rules.
-keepattributes Signature,*Annotation*,SourceFile,LineNumberTable
-keep class com.expenser.app.data.** { *; }
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
