# PlayerWaveBar
 Custom bar for displaying audio track position.

## Implementation
 Add it in your settings.gradle.kts file:
```kotlin copy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven ("https://jitpack.io") //<-- add this
    }
}
```
After that, add dependencies to a module build.gradle.kts file:
```kotlin copy
dependencies {
    implementation 'com.github.Onixen:player-wave-bar:<library_version>'
}
```
Latest library version:

[![](https://jitpack.io/v/Onixen/player-wave-bar.svg)](https://jitpack.io/#Onixen/player-wave-bar)

## Usage
1. Before using it, you need to call the ```prepare()``` method from the PlayerWaveBar instance, to which you need to pass the duration of the track and the current position.
2. To start the animation, the ```startAnimation()``` method is called. In this case, the animation of the movement of the track's lost time lane and the position indicator will be started.
3. When calling the ``pause Track()`` method, all animations will be stopped.
4. When calling the ``stop Animation()`` method, the track position for the animation is reset to 0 and all animations are updated. The next time the `startAnimation()` method is called, animations will start playing from the start position of the track.

Player Wave Bar with default settings:

<img width="283" alt="Снимок экрана 2023-11-12 в 20 58 49" src="https://github.com/Onixen/player-wave-bar/assets/47987147/c4e70f5f-8230-4013-bc61-f4b395f2f50c">

Player Wave Bar with property `filled In` with a value of 40:

<img width="284" alt="Снимок экрана 2023-11-12 в 21 00 44" src="https://github.com/Onixen/player-wave-bar/assets/47987147/4cea4838-bf89-4f39-94b7-5b04e247ed22">

## XML Properties
The description of each property can be viewed directly in the XML code:
- waveColor
- waveStrokeWidth
- renderingStep
- amplitude
- frequency
- offset
- filledIn
- indicatorRadius

## Synchronize MediPlayer & PlayerWaveBar
To visualize the current track time, the values obtained from Flow are used. The Flow itself is obtained from the `PlayerWabeBar.getCurrentTimeFlow()` method.

To get the current position of the track after the user rewound it, you need to call the ``PlayerWaveBar.getRewindTimeFlow()``.
