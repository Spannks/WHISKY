# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class app.whiskyjournal.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class app.whiskyjournal.data.model.** { *; }
# Credential Manager / Google ID
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
