# player-wave-bar
 Custom bar for displaying track position.

 Add it in your settings.gradle.kts file:
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven ("https://jitpack.io") <-- add this
    }
}
```
After that, add dependencies to a module build.gradle.kts file:
```
dependencies {
    implementation 'com.github.Onixen:player-wave-bar:<library_version>'
}
```
Latest library version:

[![](https://jitpack.io/v/Onixen/player-wave-bar.svg)](https://jitpack.io/#Onixen/player-wave-bar)
