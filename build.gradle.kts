// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    kotlin("android") version "1.8.20" apply false
    kotlin("kapt") version "1.8.20" apply false
    alias(libs.plugins.google.gms.google.services) apply false

}