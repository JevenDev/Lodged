# Changelog

## Unreleased

### Added

- Added optional animated model-part projectile collision for players and spiders, including F3 + B visualization, shield/piercing compatibility, tests, and benchmarks
- Changed model-accurate collision to be enabled by default and made arrows follow the exact animated part they struck
- Expanded model-part collision from players and spiders to generated server-safe cuboids for the full vanilla living-entity model roster
- Made F3 + B model cuboids follow vanilla's exact live part poses, visibility, scaling, and model-specific animations
- Added horse armor arrow lodging, rendering, inventory-preview tooltips, and removal
- Added optional Ride On load-order compatibility
- Added timed in-world arrow removal for nearby tamed mobs and ridden mounts, with player-first priority and configurable particles, bleeding, success chances, a one-block default range, and timing

### Changed

- Rebalanced default recovery, body removal, deep-lodged, bleeding, shield, and armor risks to reduce stacked bad-luck penalties while keeping meaningful choices
- Made Dizziness gain strength every two arrows beyond its threshold instead of every arrow
- Made non-recoverable arrows removable without forcing an automatic failed-removal penalty
- Added a config v2 migration that updates unchanged old defaults while preserving customized values
- Added config migrations for tamed mob arrow removal settings, including allowing any tamed mob by default
- Added scroll/drag rotation, the turn indicator and tooltip, arrow tooltips, and highlighting to the mount inventory preview
- Stored the traced humanoid model part with each arrow so shoulder, hip, and overlapping limb hits remain stable
- Snapped humanoid arrow anchors to the client model's real cubes and aligned inventory hit-testing with animated parts, including slim arms
- Tracked each active bleeding source separately so a bandage patches only one wound instead of clearing all bleeding

## 2.0.0 - 2026-06-28

## Added

- Added shield arrow lodging: arrows blocked by shields can now lodge into that shield item and render uniquely per shield
- Added shield arrow removal through the inventory/player preview flow
- Added in-world body and shield arrow removal
- Added armor arrow lodging: arrows can lodge into armor pieces instead of the body when armor blocks penetration, while still dealing damage to the player
- Added armor arrow removal, including recovery chance, break chance, particles, durability penalties, and removal state
- Added extra armor durability loss risk when armor has lodged arrows that are not removed
- Added arrow depth tiers: `shallow`, `lodged`, and `deep lodged`
- Added depth-based removal difficulty, bleeding chance, bleeding duration, and removal animation timing
- Added depth calculation based on velocity, critical hits, Power level, crossbow shots, modified damage, hit body part, and armor coverage
- Added support for modded `AbstractArrow` projectile types via datapack tags:
    - `lodged:trackable_projectiles`
    - `lodged:recoverable_projectiles`
    - `lodged:bleeding_projectiles`
    - `lodged:non_lodging_projectiles`
- Added storage of rendered arrow item stacks so lodged arrows can visually preserve arrow type and item data
- Added removal result UI states for success, failure, inventory full, too risky, and cannot remove now
- Added inventory UI hover details for body part, armor piece, arrow depth, removal chance, and bleeding risk for better UX and visual feedback
- Added configurable in-world removal animations and animation speed

## Changed

- Changed “max tracked arrows per entity” behavior to “max tracked arrows per body part”, defaulting to 8 per body part
- Changed mob-fired arrow recovery to be enabled by default
- Changed dizziness threshold logic to use total body-arrow capacity
- Changed player arrow removal scoring into a shared scoring helper
- Changed supported projectile detection from hardcoded vanilla arrows and spectral arrows to datapack projectile tags
- Changed bleeding behavior so projectile tags determine whether lodged or broken arrows can cause bleeding
- Changed inventory preview controls and tooltip text for clarity
- Changed failed arrow removal knockback to be configurable and disabled by default
- Changed bleeding base drip interval from `8` to `10`
