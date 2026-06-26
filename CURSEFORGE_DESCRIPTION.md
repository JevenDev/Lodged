## **Arrows should leave a mark.**

[![Available for NeoForge](https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/cozy/supported/neoforge_64h.png)](https://modrinth.com/mod/lodged/versions?l=neoforge)

[![Available on Modrinth](https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/compact-minimal/available/modrinth_46h.png)](https://modrinth.com/mod/lodged)
[![Available on GitHub](https://raw.githubusercontent.com/intergrav/devins-badges/refs/heads/v3/assets/compact-minimal/available/github_46h.png)](https://github.com/JevenDev/Lodged)

![Warden being struck by multiple arrows](https://i.imgur.com/HaMHMQS.png)

Lodged is a small Vanilla+ NeoForge mod that improves upon arrow rendering and impact.

Arrows can stay lodged in living entities, carry their original item data, drop from slain targets, break on impact, and even be pulled out of your own player model from the inventory screen. The mod also adds an optional bleeding system, so sharp hits and risky arrow removal can become more than a one-time damage number.

It is meant to make ranged combat a little grittier without replacing Minecraft's core combat loop.

* Arrows can be tracked after hitting living entities
* Tracked arrows can drop when the target dies
* Player, mob, Infinity, and creative arrows can each be controlled separately
* Spectral and tipped arrow data can be preserved for recovery
* Arrows can break on entity or block impact (configurable)
* Players can remove lodged arrows from their own inventory preview
* Shield-blocked arrows can lodge into shields and be removed from the inventory preview
* Removal success can vary by body part
* Having too many lodged arrows can cause mild dizziness until some are removed
* Arrows that hit player legs can briefly apply Slowness
* Failed removal can break the arrow, hurt the player, and apply bleeding
* Bleeding can come from arrow removal, broken arrow impacts, or tagged/enchantment-based weapons
* Bleeding supports configurable blood particles, data-driven blood colors, stacked duration, and stronger late-stage damage
* Optional Simple Blood compatibility can add matching ground decals when Lodged blood drops land
* Armour, undead mobs, skeletons, and immune entities can all be configured
* Slimes, magma cubes, and armour stands are denied by default
* Skeletons, golems, slimes, magma cubes, blazes, breezes, guardians, and armour stands are bleeding-immune by default

![features](https://cdn.modrinth.com/data/cached_images/ec0e4dc78ec1a652eb11b233dd2926f7461fe770.png)

## What Changes In Game

![A player removing lodged arrows from their inventory preview](https://i.imgur.com/KIGrVCu.gif)

### Lodged Arrows

When supported arrows hit living entities, Lodged can remember the arrow and where it landed. Non-player entities can show synced lodged arrows to nearby players, while players can keep arrow data for removal and recovery.

By default, the mod tracks up to 8 arrows per entity. Player-fired arrows can be recovered from killed targets with a 35% chance per tracked arrow.

Supported arrow types:

| Arrow type       | Notes                                                          |
| ---------------- | -------------------------------------------------------------- |
| Regular arrows   | Supported for tracking, recovery, breakage, and player removal |
| Spectral arrows  | Supported and recoverable as spectral arrows                   |
| Tipped arrows    | Preserved when original item stack recovery is enabled         |
| Infinity arrows  | Disabled for recovery by default, configurable                 |
| Mob-fired arrows | Enabled for recovery by default, configurable                  |
| Creative arrows  | Disabled for recovery by default, configurable                 |

### Arrow Recovery

If arrow recovery is enabled, tracked arrows can drop when the living entity they are lodged in dies. The mod can preserve the original arrow item stack, allowing recovered tipped and spectral arrows to retain their data rather than becoming normal arrows.

Default recovery behaviour:

* Arrow recovery is enabled
* Player arrows can be recovered from entity death drops
* Each tracked arrow has a 35% recovery chance
* Mob arrows can be recovered by default
* Infinity arrows are not recovered unless enabled
* Creative arrows are not recovered unless enabled
* Original arrow item data is preserved

### Player Arrow Removal

Players can remove arrows stuck in their own body from the inventory player preview.

Open your inventory, rotate the player preview if needed, and click a lodged arrow. A successful removal gives the arrow back when that arrow type is recoverable. A failed removal breaks the arrow, can deal damage, and can apply bleeding.

Shield arrows use the same inventory removal flow. Mob-fired arrows lodged in shields are recoverable by default and can be disabled separately from body arrow recovery.

By default, carrying 4 or more lodged arrows, or reaching 50% of the configured tracked-arrow limit, applies Lodged's custom Dizziness effect. It uses a subtle border vignette, slight red tint, and gentle first-person screen wobble that grows stronger as more arrows lodge. Removing arrows drops you below that threshold and lets the effect fade.

By default, arrows that hit a player's legs apply vanilla Slowness I for 40 ticks. Leg shot Slowness can be disabled, and both its duration and level are configurable.

Default removal chances:

| Body part | Default success chance |
| --------- | ---------------------: |
| Head      |                    35% |
| Chest     |                    65% |
| Arm       |                    85% |
| Leg       |                    85% |

Infinity-generated arrows have their removal success chance halved by default when Infinity recovery is enabled.

Default dizziness behaviour:

* Lodged arrow dizziness is enabled
* Dizziness starts at 4 lodged arrows or 50% of max tracked arrows
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
* Full armour prevents normal bleeding by default
* Partial armour reduces bleeding chance by default
* Undead can bleed only if included in Lodged's undead bleeding tag
* Skeletons do not bleed by default
* Skeletons, golems, slimes, magma cubes, blazes, breezes, guardians, and armour stands are in the bleeding immune tag by default
* Lodged blood particles are enabled by default in world and on the inventory player preview
* Simple Blood compat ground decals are enabled by default when Simple Blood is installed
* Endermen, endermites, shulkers, and the Ender Dragon have purple blood by default
* Spiders have medium green blood by default, and wardens use teal blood by default
* Simple Blood compat decal chance and scale can be tuned

When Simple Blood is installed, Lodged can spawn Simple Blood ground decals from landed Lodged blood drops. The decals use the same data-driven blood color as Lodged's own particles, so custom mob blood colors carry across both systems. Lodged blood particles and Simple Blood compat decals can be disabled separately.

Default bleeding damage:

| Setting                                  |                  Default |
| ---------------------------------------- | -----------------------: |
| Weapon or broken-arrow bleeding duration |                120 ticks |
| Arrow-removal bleeding duration          |                200 ticks |
| Maximum bleeding duration                |               1200 ticks |
| Strong bleeding threshold                |                600 ticks |
| Normal bleed pulse                       | 1 damage every 100 ticks |
| Strong bleed pulse                       |  2 damage every 80 ticks |

## Configuration

Main config file:

* Singleplayer/client: `config/lodged.json5`
* Dedicated server: `<server root>/config/lodged.json5`

The in-game config screen is powered by owo-lib and Mod Menu integration.

Major systems can be tuned or disabled:

* Arrow recovery
* Recovery chance
* Maximum tracked arrows per entity
* Player, mob, Infinity, and creative arrow recovery
* Arrow item stack preservation
* Stuck-arrow despawn prevention
* Entity denylist
* Arrow breakage on entities and blocks
* Player arrow removal
* Shield arrow lodging and mob-fired shield arrow recovery
* Removal success chances by body part
* Removal failure damage
* Lodged arrow dizziness thresholds and duration
* Leg shot Slowness duration, level, and enablement
* Bleeding rules
* Bleeding duration, damage, Lodged particles, Simple Blood compat particles, Simple Blood decal chance and scale, data-driven blood colors, and armour behaviour

## Tags and Pack Support

Lodged includes data-driven tags for pack makers and modpack authors.

Entity tags:

* `lodged:bleeding_immune`
* `lodged:bleeding_skeletons`
* `lodged:bleeding_undead`

The default `lodged:bleeding_immune` tag includes entities like skeletons, golems, slimes, magma cubes, blazes, breezes, guardians, and armour stands. Datapacks can add to it or replace it like any normal Minecraft entity type tag.

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

* Supports vanilla arrows and spectral arrows, have not tested modded arrows
* Uses synced arrow data for client visuals and player removal
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
