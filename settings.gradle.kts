pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

   /* plugins {
        // 1. KHAI BÁO PHIÊN BẢN KGP TẠI ĐÂY
        id("org.jetbrains.kotlin.android") version "1.9.22" apply false

        id("org.jetbrains.kotlin.kapt") version "2.0.21"

        // 2. Các plugin khác cũng cần phiên bản
        id("com.android.application") version "8.2.2" apply false
        id("com.google.gms.google-services") version "4.4.0" apply false
        id("com.google.dagger.hilt.android") version "2.50" apply false

        // kotlin-kapt KHÔNG được khai báo ở đây!
    }

    */
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NewsSpeechAuto"
include(":app")
