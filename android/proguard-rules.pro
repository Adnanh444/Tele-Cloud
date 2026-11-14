# Debug logging is a user-facing setting, so Log calls must survive R8.
# Never add -assumenosideeffects for android.util.Log here.
-keep class com.adnan.memoracloud.core.common.SafeLog { *; }
-keep class android.util.Log { *; }

# TDLib is reached through JNI, so its classes and native callbacks must stay.
-keep class org.drinkless.tdlib.** { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Room entities are constructed reflectively by generated code.
-keep class com.adnan.memoracloud.data.local.entity.** { *; }
-keep class com.adnan.memoracloud.data.local.dao.** { *; }

# kotlinx.serialization keeps generated serializers on the classes themselves.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.adnan.memoracloud.data.remote.telegram.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.adnan.memoracloud.data.remote.telegram.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager stores a worker's class name in its database and re-creates it by that name on the
# next launch. Jobs queued by 1.0.6 reference classes whose features were removed, so the names must
# survive obfuscation for the empty stand-ins to be found.
-keepnames class com.adnan.memoracloud.core.ai.PhotoLabelWorker
-keepnames class com.adnan.memoracloud.core.people.FaceScanWorker
-keep class com.adnan.memoracloud.core.ai.PhotoLabelWorker { <init>(...); }
-keep class com.adnan.memoracloud.core.people.FaceScanWorker { <init>(...); }
