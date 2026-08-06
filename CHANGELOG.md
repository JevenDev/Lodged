# Changelog

## 3.0.0 - 2026-08-06

### Added

- Added model-accurate projectile collision for tracked arrows across the all living-entities. Hits use live animated model cuboids, respect shields and piercing, fall back to vanilla collision for unsupported models, and include configurable inflation plus F3 + B visualization
- Added horse-armor arrow lodging, rendering, durability effects, hover targeting, tooltips, outlines, and removal from the horse inventory, with optional "Ride On" (unreleased) compatibility
- Added timed body-arrow removal from owned tamed mobs and ridden mounts in the world, with player-first targeting, plus body-arrow removal from mount inventory previews. Ownership, range, timing, success, particles, and bleeding are configurable
- Added craftable bandages. Craft two from three string and one paper, holding Use bandage (default: V) finds one anywhere in the inventory, plays synchronized first/third-person wrapping animations for two seconds, and patches one bleeding wound
- Added configurable bleeding-duration multipliers for head, chest, arm, and leg arrow wounds

### Changed

- Rebalanced default recovery, body-removal, deep-lodged, bleeding, shield, and armor risks to reduce stacked bad luck while preserving meaningful consequences
- Made Dizziness gain strength every two arrows beyond its threshold instead of every arrow
- Made non-recoverable arrows safely removable instead of treating them as automatic failures
- Reworked first and third-person body, armor, and shield removal animations with staged reach, grip, extraction, depth-based effort, and improved shield/bow clearance
- Improved mount inventory previews with scroll/drag rotation, reset controls, a turn hint, arrow highlighting, and body/armor risk tooltips
- Tracked bleeding sources independently so each bandage removes only one wound and stacked wounds require multiple bandages
- Showed lodged-arrow counts on shields as well as armor and grouped both Lodged keybinds under a localized Lodged category

### Fixed

- Fixed piercing arrows duplicating recoverable lodged-arrow data across multiple targets
- Fixed recovered arrow stacks retaining the intangible projectile marker, which could make returned arrows behave incorrectly

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
