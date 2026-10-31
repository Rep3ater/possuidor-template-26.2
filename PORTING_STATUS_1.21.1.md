# NeoForge 1.21.1 port checkpoint

Target tuple: Minecraft 1.21.1, NeoForge 21.1.259, ModDevGradle 2.0.148, Gradle 8.14.3, Java 21.

| Area | Status | Notes |
|---|---|---|
| Build configuration and metadata | Adapted | Minecraft/NeoForge versions, Java toolchain, archive name, loader metadata, and resource pack format target 1.21.1. Gradle resolves target Minecraft artifacts. |
| Identifier and common package migration | Partial | Migrated broad 26.2 naming/package differences; replaced later entity-tag, dimension-key, sound-event, player-inventory and cross-dimension teleport APIs with target equivalents. Compilation still reports API errors throughout the project. |
| Persistence and data components | Partial | Some entity/block saves use CompoundTag and registry-aware codecs. The Cardinal Components bridge now uses 1.21.1 SavedData.Factory and tag-based attachment serializer signatures, but still needs compile/runtime verification. |
| Game rules/content registration | Partial | Gamerule names preserved, but enum rules are encoded as integer rules for 1.21.1. Content registration and behavior still need compilation/runtime validation. |
| Screens and client rendering | Partial | Migrated selected screens, particle renderers, homunculus models, and construct rendering to 1.21.1 APIs. Player-shell rendering now extends the target `PlayerRenderer`; replaced the later render-state modifier system with entity-based `LivingEntityRenderer` mixins and 1.21.1 render events. Compile passes these paths, but runtime visual behavior remains unverified. |
| Loot conditions | Adapted | Registered the four existing condition IDs as 1.21.1 `LootItemConditionType` entries; migrated referenced context parameters and lookups. Datapack loading and loot behavior still need runtime validation. |
| Networking, reload listeners, and data formats | Partial | Updated payload handler registration to the 1.21.1 `PayloadRegistrar` signatures and reload listener event signatures. Network behavior and datapack schemas still need runtime validation. |
| Construct rendering | Adapted | Replaced the 26.2 deferred render-state renderer with the 1.21.1 immediate `EntityRenderer` path while retaining voxel transforms and walk/punch animation. Visual parity is unverified. |
| Dimension data and client startup | Adapted | Rewrote the Nucleus and Construct Workshop dimension type files to the 1.21.1 flat dimension type schema. Added the 1.21.1 item model for the cloning machine. A new integrated world now loads and the local player joins. Nucleus/workshop travel and visuals still need targeted checks. |
| Audio and subtitles | Partial | Added a vanilla villager-trade sound event fallback for the Mortician and added missing English/Portuguese subtitle translations. The source tree contains no custom OGG/WAV assets, so custom sound references reported missing at runtime remain unresolved. |

## Validation

`gradlew compileJava --no-daemon` succeeds against Minecraft 1.21.1 / NeoForge 21.1.259 with 0 compiler errors (the previous full compile had 68). `gradlew jar --no-daemon` succeeds and produces `build/libs/requiem-1.21.1-neoforge-2.0.0-beta.16.jar`. A client run first exposed a stale `Camera.update` mixin target; after migrating that hook to the 1.21.1 `Camera.setup` signature, the client reached the title screen. A subsequent world creation exposed newer-format dimension type JSON; after conversion, a fresh integrated world loaded, prepared its spawn area, and logged the player joining. The cloning-machine model warning is resolved. The Mortician trade event now uses vanilla villager audio; custom OGG/WAV files are absent from the supplied source, so other custom sound references still need source assets or an intentional replacement decision. World validation did not exercise Nucleus or Construct Workshop travel, entity behavior, persistence across restart, or visual feature parity. These gaps remain before release readiness.
