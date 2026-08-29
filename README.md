# The Keeper's Hour

A time-loop game for Android. You walk the lighthouse as its assistant keeper.

Corr Sgeir lighthouse, the west coast, 21 November 1907. You are Mairead Vance, assistant
keeper. At eleven o'clock the light is out. At midnight the steamer *Ardnamurchan* strikes the
reef with two hundred people aboard.

Then you wake on the lamp-room floor, and it is eleven o'clock.

You get sixty minutes. Every action spends some of them, and the hour is never long enough. The
rock resets the storm, the oil, the locked cellar and the man downstairs — but it does not reset
what you have worked out. **Knowledge is the only thing that survives the loop**, and it is what
turns a five-minute search into a one-minute reach for a nail behind a door.

Seven endings. One of them stops the loop.

## Playing

Drag the thumbstick to walk. When something is within reach its name appears and the ACT button
lights up. Stairs and doors are things you walk up to and open; the sea chest, the lens, the loose
flagstone and Ezra Kell are things you walk up to and deal with.

Walking is free. Only the acting costs minutes — searching a room, forcing a lock, climbing a
floor — so the hour is spent on decisions rather than on footwork.

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
| `engine` | Pure JVM Kotlin: game state, the scene graph, the story, the rules, and the walkable floor plans. No Android. |
| `app`    | Compose: the room renderer, the controls, and saving the run to shared preferences. |

The split is deliberate. Because the engine is plain Kotlin with no Android types in it, the
entire game is drivable from JUnit — so the story is verified by tests rather than by tapping
through it on a handset.

## How the world relates to the story

The rooms are a *view* over the story graph, not a second copy of it. Every object in the
lighthouse names choice ids that already exist in that room's scene, and pressing ACT hands them
straight to the same `GameEngine`:

```kotlin
Interactable(
    id = "chest",
    label = "Sea chest",
    kind = PropKind.CHEST,
    footprint = tiles(11 to 11),
    choiceIds = listOf("force_the_sea_chest", "open_the_chest_and_take_the_money"),
)
```

So walking up to the chest and forcing it is the identical state transition the text version made
from a menu — same cost, same suspicion, same knowledge. Nothing about the game's rules lives in
the UI.

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

`WorldValidator` then checks that the world and the story cannot drift apart:

- every object names choices its room's scene actually has;
- **every choice in every room scene is exposed by some object** — the silent failure when you put
  a story behind a character is an action that still exists in the content but has nothing in the
  world to trigger it;
- no two objects offer the same action;
- every room's floor is one connected piece, every object can be reached by a body rather than a
  point, and every arrival spot is somewhere the keeper can actually stand.

If all of that passes, the game is completable and every action in it is reachable on foot. That
is the point of keeping the engine free of Android.
