# The Keeper's Hour

A time-loop narrative game for Android.

Corr Sgeir lighthouse, the west coast, 21 November 1907. You are Mairead Vance, assistant
keeper. At eleven o'clock the light is out. At midnight the steamer *Ardnamurchan* strikes the
reef with two hundred people aboard.

Then you wake on the lamp-room floor, and it is eleven o'clock.

You get sixty minutes. Every action spends some of them, and the hour is never long enough. The
rock resets the storm, the oil, the locked cellar and the man downstairs — but it does not reset
what you have worked out. **Knowledge is the only thing that survives the loop**, and it is what
turns a five-minute search into a one-minute reach for a nail behind a door.

Seven endings. One of them stops the loop.

## Building

```
./gradlew :app:assembleDebug     # APK at app/build/outputs/apk/debug/
./gradlew build                  # compile, lint, detekt and the full test suite
```

Requires JDK 17 and an Android SDK with platform 36. Debug builds are signed with the standard
debug key and install by sideloading. No runtime permissions are requested.

## Layout

| Module   | What it is |
|----------|------------|
| `engine` | Pure JVM Kotlin: game state, the scene graph, the story, and the rules. No Android. |
| `app`    | Jetpack Compose UI, and saving the run to shared preferences. |

The split is deliberate. Because the engine is plain Kotlin with no Android types in it, the
entire game is drivable from JUnit — so the story is verified by tests rather than by tapping
through it on a handset.

## How the story is written

Scenes are declared with a small Kotlin DSL. A choice carries its cost in minutes, the state it
changes, and a predicate deciding whether it appears at all:

```kotlin
choice("Take the cellar key from the nail") {
    time(Cost.GLANCE)
    note("Behind the door, under the oilskins.")
    onlyIf { it.knows(Knowledge.CELLAR_KEY) && !it.has(Item.CELLAR_KEY) }
    take(Item.CELLAR_KEY)
    goto("beat_key_taken")
}
```

That is the whole shortcut mechanic: the same key, five minutes of searching the first time and
one minute of reaching for it once Mairead knows where it hangs.

## What the tests guarantee

`StoryValidator` runs static checks over the graph — no `goto` pointing at a scene that does not
exist, no duplicate choice ids, no orphaned or dead-end scenes, no fact that can never be learned.

On top of that, `Playthrough` drives the engine exactly as a player would, asserting each choice
was genuinely on offer before taking it. The suites use it to prove:

- every one of the seven endings is reachable, by playing to it;
- the game is beatable **starting from zero knowledge**, across real loops on the real clock;
- one hour is not enough to learn everything, so the loop is actually necessary;
- a fully informed run takes less than half the time of a blind one.

If those pass, the game is completable. That is the point of keeping the engine free of Android.
