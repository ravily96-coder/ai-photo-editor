// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  id("org.jetbrains.kotlin.android") version "1.9.0" apply false
  id("com.android.application") version "8.2.0" apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
