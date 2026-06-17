# Lodged

Lodged is a small NeoForge mod for Minecraft `1.21.1`.

It turns arrows stuck in living entities into readable combat feedback and a small recovery mechanic. Player-fired arrows can be tracked on hit, then each tracked arrow has a configurable chance to drop when the target dies.

## Build

On Windows:

```bat
gradlew.bat build
```

On macOS/Linux:

```bash
./gradlew build
```

Useful run tasks:

```bat
gradlew.bat runClient
gradlew.bat runServer
```

## Configuration

The common NeoForge config includes:

- `enableArrowRecovery`
- `recoveryChance`
- `maxTrackedArrowsPerEntity`
- `recoverPlayerArrowsOnly`
- `recoverMobArrows`
- `recoverInfinityArrows`
- `recoverCreativeArrows`
- `enableArrowBreakOnEntityHit`
- `enableArrowBreakOnBlockHit`
- `enableMobArrowBreak`
- `regularArrowImpactBreakChance`
- `mobArrowImpactBreakChance`
- `infinityArrowImpactBreakChance`
- `preserveArrowItemStack`
- `preventPlayerArrowDespawn`
- `preventNonPlayerArrowDespawn`
- `playerArrowRemovalHeadSuccessChance`
- `playerArrowRemovalChestSuccessChance`
- `playerArrowRemovalArmSuccessChance`
- `playerArrowRemovalLegSuccessChance`
- `entityDenylist`
