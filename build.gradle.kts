plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.clawd.pet"
    compileSdk = 34
    defaultConfig { applicationId = "com.clawd.pet"; minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
