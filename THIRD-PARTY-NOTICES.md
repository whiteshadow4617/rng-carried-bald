# Third-party notices

## Feesh — Fishing quality-of-life behavior

This mod includes Forge 1.8.9 behavior ports inspired by Sleepy-Panda's [Feesh](https://github.com/Sleepy-Panda/Feesh), including:

- Session catch-efficiency and Fishing Festival personal-best concepts, based on [EfficiencyTracker.kt](https://github.com/Sleepy-Panda/Feesh/blob/develop/src/main/kotlin/com/github/sleepypanda/feesh/features/overlays/EfficiencyTracker.kt) and [FishingFestivalTracker.kt](https://github.com/Sleepy-Panda/Feesh/blob/develop/src/main/kotlin/com/github/sleepypanda/feesh/features/overlays/FishingFestivalTracker.kt). This mod restricts them to its already-supported wiki-Legendary catch announcements plus the user-requested Lord Jawbus exception; its Festival statistic counts Great Whites only.
- Optional rare sea-creature highlighting and alert behavior described by Feesh's [feature list](https://github.com/Sleepy-Panda/Feesh#features), implemented using this mod's existing 1.8.9 entity-name/chat parsing.

Feesh is licensed under the Apache License, Version 2.0. Its full license text is included as [`LICENSE-Feesh.txt`](LICENSE-Feesh.txt). This project does not bundle Feesh or depend on its Fabric/Kotlin code; the listed behavior has been adapted for Java and Forge 1.8.9, with local-only message detection and independent feature settings.

## Not Enough Fakepixel — M7 dragon areas and Fakepixel fishing features

The M7 relic/dragon-area positions, dimensions, colors, and final-phase cue are adapted from Not Enough Fakepixel 1.2.4's [`DragonCloseAlert.java`](https://github.com/im2pb/NotEnoughFakepixel/blob/1.2.4/src/main/java/com/nef/notenoughfakepixel/features/skyblock/dungeons/DragonCloseAlert.java) and [`M7RelicWaypoints.java`](https://github.com/im2pb/NotEnoughFakepixel/blob/1.2.4/src/main/java/com/nef/notenoughfakepixel/features/skyblock/dungeons/M7RelicWaypoints.java). This Forge port draws the five outlined regions during M7 final phase. A persisted option defaults to keeping them visible through dragon spawns; turning it off hides them after the first dragon appears.

The Fakepixel Catch Notifications controls and Legendary/Trophy message recognition are adapted from Not Enough Fakepixel's [`Fishing.java`](https://github.com/im2pb/NotEnoughFakepixel/blob/master/src/main/java/com/nef/notenoughfakepixel/config/features/Fishing.java), [`GreatCatchNotifier.java`](https://github.com/im2pb/NotEnoughFakepixel/blob/master/src/main/java/com/nef/notenoughfakepixel/features/skyblock/fishing/GreatCatchNotifier.java), and [`TrophyFishNotifier.java`](https://github.com/im2pb/NotEnoughFakepixel/blob/master/src/main/java/com/nef/notenoughfakepixel/features/skyblock/fishing/TrophyFishNotifier.java). This project does not bundle or depend on the upstream Fabric implementation; retained notifier behavior is adapted to Java/Forge 1.8.9 and is Fakepixel-only.

Not Enough Fakepixel is licensed under the GNU Lesser General Public License, version 3 or later. Its full license text is included as [`LICENSE-NotEnoughFakepixel.txt`](LICENSE-NotEnoughFakepixel.txt).
