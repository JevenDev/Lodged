## **Arrows should leave a mark.**

[![Available for NeoForge](https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/cozy/supported/neoforge_64h.png)](https://modrinth.com/mod/lodged/versions?l=neoforge)

[![Available on Modrinth](https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/compact-minimal/available/modrinth_46h.png)](https://modrinth.com/mod/lodged)
[![Available on GitHub](https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/compact-minimal/available/github_46h.png)](https://github.com/JevenDev/Lodged)

![Warden being struck by multiple arrows](https://i.imgur.com/HaMHMQS.png)

Arrows can lodge in bodies, shields, and armor, preserve their item data, drop from slain targets, break on impact, and be pulled out through inventory or in-world removal. The mod also adds optional bleeding, dizziness, leg-shot Slowness, and data-driven projectile support.

The goal is simple: arrows actually have some impact and are a bit more immersive.

* Track arrows after they hit living entities and render them on their model
* Preserve original arrow item stacks for tipped, spectral, and supported modded arrows
* Recover tracked arrows from killed targets, player bodies, shields, and armor
* Tune player, mob, Infinity, and creative arrow recovery separately
* Let arrows break on entity or block impact
* Pull body arrows from the inventory player preview or with in-world removal keybind
* Lodge blocked arrows into shields, render them on the shield, and remove them later
* Lodge stopped arrows into armor pieces, with armor penetration and durability penalties
* Classify body arrows as shallow, lodged, or deep lodged based on shot force, body part, crits, Power enchantment, crossbows, and armor coverage
* Make removal difficulty, animation time, bleeding risk, and bleeding duration respond to arrow depth
* Show hover details for body part, armor piece, depth, removal chance, and bleeding risk
* Apply mild dizziness when too many arrows are lodged
* Apply brief Slowness window from leg shots
* Apply bleeding from risky removal, broken arrow impacts, or tagged/enchantment-based weapons
* Configure blood particles, data-driven blood colors, stacked duration, late-stage damage, and Simple Blood ground decals
* Support modded `AbstractArrow` entity types through datapack projectile tags
* Configure armor, undead mobs, skeletons, denied entities, and bleeding-immune entities

![features](https://cdn.modrinth.com/data/cached_images/ec0e4dc78ec1a652eb11b233dd2926f7461fe770.png)

## What Changes In Game

![A player removing lodged arrows from their inventory preview](https://i.imgur.com/KIGrVCu.gif)

### Lodged Arrows

When supported arrows hit living entities, Lodged can remember the arrow, where it landed, what item it came from, whether it can be recovered, and whether it can cause bleeding. Non-player entities show synced lodged arrows to nearby players, while player arrows are kept for removal, recovery, dizziness, and bleeding logic.

By default, the mod tracks up to 8 body arrows per body part. Player-fired arrows can be recovered from killed targets with a 50% chance per tracked arrow.

Supported arrow types:

| Arrow type | Notes |
| --- | --- |
| Regular arrows | Supported for tracking, recovery, breakage, rendering, and removal |
| Spectral arrows | Supported and recoverable as spectral arrows |
| Tipped arrows | Preserved when original item stack recovery is enabled |
| Modded arrows | Supported when their `AbstractArrow` entity type is added to Lodged's projectile tags |
| Infinity arrows | Disabled for recovery by default, configurable |
| Mob-fired arrows | Enabled for recovery by default, configurable |
| Creative arrows | Disabled for recovery by default, configurable |

### Arrow Recovery

If arrow recovery is enabled, tracked body arrows can drop when the living entity they are lodged in dies. It preserves the original arrow item stack, allowing recovered tipped, spectral, and supported modded arrows to retain their data rather than becoming normal arrows.

Default recovery behaviour:

* Arrow recovery is enabled
* Player arrows can be recovered from entity death drops
* Each tracked arrow has a 50% recovery chance
* Mob arrows can be recovered by default
* Infinity arrows are not recovered unless enabled
* Creative arrows are not recovered unless enabled
* Original arrow item data is preserved
* Mob shield arrows and mob armor arrows are recoverable by default

### Arrow Removal

Players can remove lodged arrows from their own body, shields, and armor.

Open your inventory, rotate the player preview if needed, and click a lodged body, shield, or armor arrow. A safe removal gives the arrow back when that arrow type is recoverable; non-recoverable arrows can still be pulled safely without creating an item. A failed body removal can break the arrow, deal damage, and apply bleeding. Shield and armor removals can also break arrows and damage the item they were stuck in.

Removal can also happen in world. Body and armor removal chooses a priority arrow and takes time to complete. Shield removal works from the held shield and syncs to nearby players while the removal is in progress.

The removal UI shows useful risk information before you click: hit body part, armor piece, arrow depth, removal chance, and bleeding risk. Result feedback distinguishes successful recovery, broken arrows, full inventories, unsafe removals, and blocked removal attempts.

By default, carrying 4 or more lodged arrows, or reaching 50% of the configured body-arrow capacity, applies Lodged's custom Dizziness effect. It uses a subtle border vignette, slight red tint, and first-person screen wobble that gains one strength level for every two additional arrows. Removing arrows drops you below that threshold and lets the effect fade.

By default, arrows that hit a player's legs apply vanilla Slowness I for 40 ticks. Leg shot Slowness can be disabled, and both its duration and level are configurable.

Default removal chances:

| Body part | Default success chance |
| --- | ---: |
| Head | 50% |
| Chest | 72% |
| Arm | 90% |
| Leg | 90% |

Infinity-generated arrows have their removal success chance halved by default when Infinity recovery is enabled.

### Arrow Depth

Body arrows can land as shallow hits, normal lodged hits, or deep lodged hits.

Deep lodged arrows are more likely from fast, critical, high-damage shots to the head or chest, especially when that body area is unarmored. Power enchantment level, crossbow shots, arrow velocity, modified damage, body part, and armor coverage can all affect the final depth roll.

Depth changes how removal feels:

| Depth | Default effect |
| --- | --- |
| Shallow | Easier to remove, lower bleeding risk, shorter animation |
| Lodged | Standard removal behavior |
| Deep lodged | Harder to remove, higher bleeding risk, longer animation |

Depth tiers can be disabled if you prefer every body arrow to behave like a normal lodged arrow.

### Shields and Armor

Shield-blocked arrows can lodge into the shield item instead of disappearing. Lodged shield arrows render on the shield, including first-person shields, and can be removed from the inventory preview or through in-world shield removal. Removing shield arrows can damage the shield, and mob-fired shield arrows are recoverable by default.

Armor can also catch arrows. When an arrow hits an armored body part, Lodged can roll armor penetration. Arrows that fail to penetrate may lodge into the armor piece instead of the body. Armor arrows can render on equipped armor, be removed later, break during removal, and add extra durability loss while left in the armor.

Default dizziness behaviour:

* Lodged arrow dizziness is enabled
* Dizziness starts at 4 lodged arrows or 50% of max body arrow capacity
* Each additional lodged arrow increases the effect strength, up to a capped maximum
* Dizziness refreshes once per second with a short 80-tick custom effect duration

### Arrow Breakage

Arrows can break immediately on impact instead of sticking around forever.

Default breakage behavior:

* Entity impact breakage is enabled
* Block impact breakage is enabled
* Mob arrow breakage is enabled
* Regular player arrows have a 2% impact break chance
* Mob arrows have a 0% impact break chance by default
* Infinity arrows have a 0% impact break chance by default

![configuration](https://cdn.modrinth.com/data/cached_images/1252c11050b7daf8b8621712b58dd1005e7ba982.png)

## Bleeding

Lodged includes a configurable bleeding mob effect.

Bleeding can be applied by failed arrow removal, successful arrow removal, broken arrow impacts, or qualifying weapon hits. By default, enchantments in Lodged's bleeding enchantment tags can cause bleeding, while the item-tag weapon rule is disabled until you enable it.

Default bleeding behaviour:

* Bleeding is enabled
* Pulling lodged arrows can cause bleeding
* Enchantment-tagged weapons can cause bleeding
* Weapon-item-tag bleeding is disabled by default
* Full armor prevents normal bleeding by default
* Partial armor reduces bleeding chance by default
* Undead can bleed only if included in Lodged's undead bleeding tag
* Skeletons do not bleed by default
* Skeletons, golems, slimes, magma cubes, blazes, breezes, guardians, and armor stands are in the bleeding immune tag by default
* Lodged blood particles are enabled by default in world and on the inventory player preview
* Simple Blood compat ground decals are enabled by default when Simple Blood is installed
* Endermen, endermites, shulkers, and the Ender Dragon have purple blood by default
* Spiders have medium green blood by default, and wardens use teal blood by default
* Simple Blood compat decal chance and scale can be tuned

When Simple Blood is installed, Lodged can spawn Simple Blood ground decals from landed Lodged blood drops. The decals use the same data-driven blood color as Lodged's own particles, so custom mob blood colors carry across both systems. Lodged blood particles and Simple Blood compat decals can be disabled separately.

Default bleeding damage:

| Setting | Default |
| --- | ---: |
| Weapon or broken-arrow bleeding duration | 120 ticks |
| Arrow-removal bleeding duration | 160 ticks |
| Maximum bleeding duration | 1200 ticks |
| Strong bleeding threshold | 600 ticks |
| Normal bleed pulse | 1 damage every 100 ticks |
| Strong bleed pulse | 2 damage every 80 ticks |

## Configuration

Main config file:

* Singleplayer/client: `config/lodged.json5`
* Dedicated server: `<server root>/config/lodged.json5`

The in-game config screen is powered by owo-lib and Mod Menu integration.

Major systems can be tuned or disabled:

* Arrow recovery
* Recovery chance
* Maximum tracked arrows per body part
* Player, mob, Infinity, and creative arrow recovery
* Arrow item stack preservation
* Stuck-arrow despawn prevention
* Entity denylist
* Arrow breakage on entities and blocks
* Body, shield, and armor arrow removal
* Inventory and in-world removal timing
* Arrow removal animation enablement and speed
* Shield arrow lodging, removal timing, durability chance, first-person rendering, and mob-fired shield arrow recovery
* Armor arrow lodging, armor penetration, rendering, removal, break chance, durability penalties, and mob-fired armor arrow recovery
* Removal success chances by body part
* Arrow depth chances, Power/crit/velocity/armor modifiers, and shallow/deep removal and bleeding multipliers
* Removal failure damage
* Lodged arrow dizziness thresholds and duration
* Leg shot Slowness duration, level, and enablement
* Bleeding rules
* Bleeding duration, damage, Lodged particles, Simple Blood compat particles, Simple Blood decal chance and scale, data-driven blood colors, and armor behavior

## Tags and Pack Support

Lodged includes data-driven tags for pack makers and modpack authors.

Entity tags:

* `lodged:bleeding_immune`
* `lodged:bleeding_skeletons`
* `lodged:bleeding_undead`

Projectile entity type tags:

* `lodged:trackable_projectiles`
* `lodged:recoverable_projectiles`
* `lodged:bleeding_projectiles`
* `lodged:non_lodging_projectiles`

The default `lodged:bleeding_immune` tag includes entities like skeletons, golems, slimes, magma cubes, blazes, breezes, guardians, and armor stands. Datapacks can add to it or replace it like any normal Minecraft entity type tag.

The projectile tags apply to `AbstractArrow` entity types. `lodged:trackable_projectiles` opts an arrow entity into Lodged's impact handling, `lodged:recoverable_projectiles` allows its pickup item stack to be stored and returned, `lodged:bleeding_projectiles` allows broken impacts and arrow removal to apply bleeding, and `lodged:non_lodging_projectiles` prevents body and shield lodging while still letting the projectile remain trackable for other impact behavior. Vanilla arrows and spectral arrows are included in the first three projectile tags by default.

Blood color files live under `data/<namespace>/lodged/blood_colors/*.json`. Each file can target entity type IDs or entity type tags:

```json
{
  "color": "#B36BFF",
  "entity_types": [
    "minecraft:enderman",
    "#minecraft:raiders"
  ]
}
```

Blood colors apply to Lodged blood particles, inventory-preview blood, and Simple Blood ground decals spawned by Lodged.

Item tags:

* `lodged:bleeding_weapons`

Enchantment tags:

* `lodged:bleeding_living`
* `lodged:bleeding_undead`

These tags let packs decide which entities can bleed and which weapons or enchantments can trigger bleeding.

![A player bleeding after removing a lodged arrow](https://i.imgur.com/oEPffkX.gif)

## Compatibility

Lodged is designed to work with vanilla-style combat and projectile behaviour instead of replacing entity classes.

* Supports vanilla arrows and spectral arrows by default
* Supports modded `AbstractArrow` entity types through datapack projectile tags
* Uses synced arrow data for client visuals and arrow removal
* Integrates with Simple Blood when installed by spawning matching ground decals from Lodged blood drops
* Does not replace Minecraft's living entity classes

Compatibility may vary with mods that heavily replace projectile impact handling, living-entity rendering, inventory screens, particles, or stuck-arrow behaviour.

## Version and Loaders

* NeoForge 1.21.1 - active development
* Forge - not planned
* Fabric - not planned
* Older Minecraft versions - not planned

![credits & license](https://cdn.modrinth.com/data/cached_images/5fd3ad80e342e6985dd6ebda1f7afd9c48749fce.png)

## Modpacks

You may use this mod in modpacks, videos, servers, and other projects. A link back to the Modrinth page is appreciated.

## Credits

Created by me :D

## License

All Rights Reserved.

Feel free to use this mod in modpacks, videos, etc. Just provide a link back to this page if possible :)

Please don't port this mod without express permission from me.

For any general queries/unlisted questions, DM me on Twitter (@prodbyjvn) / Discord (ijvn).

***Warning: this mod ONLY exists on Modrinth & CurseForge as of May 2026. Any sites hosting this mod outside of Modrinth/CurseForge are not official releases.***
