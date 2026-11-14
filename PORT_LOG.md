# Thaumaturge Forge 1.20.1 port — ub-mode action log

## Phase 0 — Toolchain
- Replaced Prism/NeoForge Kotlin DSL with `legacyForge` Groovy MDK (1.20.1-47.4.20, Java 17, Gradle 8.12).
- Quarantined CE26 sources under `src/ce26` (reference; not compiled).
- Added Forge `mods.toml` (required curios + lithostitched), JAVA_17 mixins, AT stub.
- Deps: Curios 5.14.1+1.20.1, Lithostitched curse 6742615, JEI compile+runtime.
- Check: `./gradlew build` SUCCESS.

## Phase 1 — Entrypoints / registries
- Forge `@Mod` + `FMLJavaModLoadingContext` bus.
- `DeferredRegister`/`RegistryObject` for blocks, items, BEs, entities, menus, recipes, particles, sounds, tabs.
- Generated `TTBulkContent` (227 CE26 block IDs) + `TTBulkItems` (142 item IDs) as placeholders.

## Phase 2 — Persistence shims
- `TTItemData`: NBT root `Thaumaturge` + Codec helpers (aspects, crystal, essentia, wand vis, filter).
- `TTAttachments`: player knowledge/warp + chunk aura via Forge capabilities (replaces NeoForge Attachments).

## Phase 3 — Networking
- `TTNetwork` SimpleChannel protocol `3`.
- Packets: knowledge sync, aura snapshot, request aura chunk, scan target.

## Phase 4 — Capabilities / aura / essentia
- `IEssentiaHandler` + `EssentiaTank`; crucible/jar BEs expose ESSENTIA capability.
- `AuraHelperBindings` initializes chunk aura from biome/seed.

## Phase 5 — Content systems (Make it Work)
- Aspects (`TTAspects`), scanning (`ScanBindings`), thaumonomicon/thaumometer items.
- Crucible / arcane workbench / infusion recipe types + serializers.
- Golem + taint seed entities/attributes/renderers.
- Labyrinth bootstrap (Lithostitched datapack retained in resources).
- Curios slot datapack; JEI plugin stub.

## Phase 6 — Client / mixins
- Client setup, entity renderers, simple particle providers.
- `FireBlockMixin` placeholder; CE26 RenderState mixins deferred (targets differ on 1.20.1).

## Phase 7 — Pack integration
- Built `thaumaturge-forge-1.20.1-1.1.0-forge.jar` (~25MB with assets).
- Installed into `minecraft/mods/`.
- Assets + lithostitched biome injectors + data from CE retained under `src/main/resources`.

## Phase 8 — Models CE + Phase 5 core loop
- **A1 Mesh:** Ported `TTMesh*` + `ModelEvent.RegisterGeometryLoaders` (`thaumaturge:mesh`).
- **A2 Datagen:** Forge `TTBlockStateProvider` / `TTItemModelProvider` / `TTMeshLoaderBuilder` (stand-in for CE26 1.21 `TTModelProvider`); `runData` → `src/generated/resources`, merged into main; tube stone placeholders → `tube_fittings`; orphans (banners) skipped.
- **A3 BEWLR:** `EssentiaJarItem` + `JarItemBewlr` (`builtin/entity` item model).
- **B1 Scan:** `ThaumometerItem` hold-to-scan + `ScanRaycastHelper` → `ScanBindings` (knowledge unlock).
- **B2 Crucible:** water tank + heat tick + boil craft; recipe `data/thaumaturge/recipes/crucible/iron_to_gold_nuggets.json`.
- **B3 Workbench:** RMB opens GUI; `ArcaneWorkbenchScreen`; craft + `AuraData.drainVis`; smoke recipe `smoke_arcane_stick_nugget.json`.
- Check: `./gradlew build` SUCCESS; jar reinstalled to `minecraft/mods/`.

## Phase 9 — Thaumometer + workbench visual parity
- Thaumometer: `pane`=scanscreen mesh glass; `UseAnim.NONE` + `resolveTarget`; `ThaumometerHandRenderer` (map pose, `ItemDisplayContext.HEAD`); lens name readout; `ThaumometerClientHandler` + `ClientEffects` + `CAMERA_TICKS`; `ItemInHandRendererInvoker` mixin.
- Workbench: BE 16 slots; 6 crystal + wand menu slots (CE coords); wand overlay + spinning crystal glyphs; CE-scaled vis labels; optional recipe `crystals` consume on craft.
- Deferred: inventory GUI scan, aspect overlay 3rd-person, Iris, full WorkbenchPayment/wand BER.
- Check: `./gradlew build` SUCCESS; jar reinstalled to `minecraft/mods/`.

## Phase 10 — Early progression (crystals / recipes / research / book)
- **Crystals:** `BlockCrystal` + `crystal_aer|ignis|aqua|terra|ordo|perditio|vitium`; `CrystalClusterFeature` + configured/placed + `forge:biome_modifier` `add_crystals`; drops aspected `essentia_crystal`; creative primal stacks; item/block tint.
- **Recipes (main-format):** `salis_mundus`, `thaumonomicon`, `arcane_workbench`, `stone_arcane`, `stone_arcane_brick`, `arcane_workbench/thaumometer` with `research` gate `first_steps@1`.
- **Research:** datapack reload (`research_entry` + `research_category`); `ResearchManager` unlock/stage; `gotthaumonomicon`/`gotdream` roots; Thaumonomicon browser screen; unlock packet; ScanKeys `scanned/...`; craft flags + first_steps advance.
- Deferred: research-table note graph, full CE recipe dump, inventory scan / 3rd-person aspect overlay.
- Check: `./gradlew build` SUCCESS; jar reinstalled to `minecraft/mods/`.

## Phase 11 — Thaumonomicon full UI + vis crystal models
- **T1 Screen infra:** `AbstractTTScreen`, `TTScreenTextures`, tooltip shims (`TTTooltipRenderer` / `TooltipLine*`).
- **T2 Browser:** hex map `ThaumonomiconBrowserScreen` (tabs, pan/zoom/inertia, connectors, search); `BrowserModel`/`BrowserSearch`/`DragTracker`/`MapAxis`; opener `reopen()` + remember category/map/scroll; unlock from map via existing packet.
- **T3 Entry detail:** `EntryDetailScreen` + `detail/**` pages/bookmarks/requirements; `ServerboundAdvanceStagePacket` / `ServerboundClearResearchFlagsPacket`; construct PIP / ObtainNote soft-stubbed.
- **C1 Crystals:** `CrystalShards`/`CrystalFaceTransforms`/`CrystalShapes`; dynamic `thaumaturge:crystal` loader + `IDynamicBakedModel` from `crystal.ttmesh`; `SUPPORT` blockstate bitmask (Forge 1.20.1 has no block ModelData) + `SIZE` shard growth; cutout + tint 0 + full-bright quads.
- **C2 Items:** `essentia_crystal` aspect tint retained; `crystal_*` block items use flat `crystal_planter` + fixed aspect dye tint.
- Deferred: research-table note minigame, deconstruction GUI, full recipe insert panels / construct PIP.
- Check: `./gradlew build` SUCCESS; jar reinstalled to `minecraft/mods/`.

## Phase 11.1 — Book text/icons/aspects + crystal clusters
- **Crystals:** `CrystalBakedModel.getModelData` now supplies live `SUPPORT_MASK` (+ SEED); `getQuads` prefers ModelData over blockstate; worldgen fallback `SUPPORT=DOWN` if mask empty.
- **PageParser:** CE markup (`<BR>`/`<PAGE>`/`<IMG>`/`LINE`) + `PageTextLayout.compose`/`drawSpread` with page-list pagination; detail screen rebuilds pages per stage.
- **Icons:** category JSON → `cat_*.png` / `thaumonomicon_cheat` + `_smooth` backgrounds; `ResearchIcon` ITEM/TEXTURE/FOCUS; `EntryIconRenderer` draws textures.
- **Aspects:** `AspectTagRenderer`, aspect/knowledge inserts + bookmarks; knowledge grid from `ClientKnowledge` / categories.
- Check: `./gradlew build` SUCCESS; jar reinstalled to `minecraft/mods/`.

## Phase 11.1b — Book chrome layout
- Browser parallax blit: texture size `TEX` (256), not viewport — restores tiled category backgrounds.
- Bookmarks: CE edge offsets (`SIDE_OFFSET_X=-48`, `RECIPE_OFFSET_X=280`) + side tab UV paint.
- Requirements: CE `RequirementLayout` bottom-up rows; Complete only when requirements exist; page compose reserves obtain/craft space; no-req stages auto-advance.
- Check: `./gradlew build` SUCCESS; jar reinstalled.

## Phase 11.1c — Browser tile + ExtendedAE creative NPE
- Parallax: manual 256×256 tiles (GUI UV clamp broke single oversized blit into edge smear).
- Compat mixin (optional): null-guard `EPPConfig.infCellFluid/Item` in ExtendedAE `InfinityCell.addToMainCreativeTab` (crash on creative open after tab rebuild).
- Check: build + jar reinstall.

## Phase 12 — Full wave (UI gate + salis + table + trees + tooltips)
- **A UI/thaumometer:** Browser overlay `enableBlend` + scissor; thaumometer GUI isometric display + mesh fullbright bake; arcane WB recipe CE costs (`vis=20`, 1 crystal each primal).
- **B Salis Mundus:** `DustTrigger` recipe type + simple/tag serializers; chunk `DustTriggerSwapQueue` capability; `SalisMundusItem` use-on-block; tick/FX handlers; P0 datapack triggers (bookshelf→thaumonomicon, crafting_table→arcane_workbench, cauldron→crucible). Shapeless crafting fallbacks retained.
- **C Research table:** Double-block `BlockResearchTable` + BE/menu; hex note data already on main; `ResearchTableScreen` (palette/hex place/combine/dupe); table packets + `ServerboundObtainNotePacket`; theory-note create from book knowledge row; multipart `research_table_main`/`_ext` models.
- **D Trees:** `BlockSaplingTT` + `TTTreeGrowers`; configured/placed features; forge biome modifiers for greatwood/silverwood; cross sapling models.
- **E Aspect tooltips:** `AspectChipsTooltip` / client factory + `ItemTooltipEvent` from `base_aspects` / AspectIndex.
- Check: `./gradlew build` SUCCESS; jar reinstalled to `minecraft/mods/`.

## Wave 0.5b — Crystal → dream → Salis Mundus opener
- Ported CE early chain in `ResearchProgressionEvents`: pickup `essentia_crystal` → `gotcrystals` + chat `discovery.crystals`; wake (`PlayerWakeUpEvent`) → complete `gotdream`, grant written dream journal (`book.thaumaturge.start.*`), chat `discovery.dream`.
- Config `thaumaturge-common.toml` → `progression.noSleep` (CE `NO_SLEEP`) skips bed requirement.
- Added `gotcrystals` research entry + en_us discovery/book strings.

## Wave 0.5 — Thaumonomicon quest closure
- **A Recipes:** Registered `filter`, `morphic_resonator`, `plate_brass`, `nugget_quartz`, `nugget_quicksilver`. Added datapack recipes: `phial`, AW `filter`/`morphic_resonator`/`jar_normal`, crucible `vis_crystal/*` (primals+vitium), `alumentum`, `ingot_brass`, plate/nugget/plank helpers. `nitor_dye` group resolve unchanged via `ResearchRecipeResolver`.
- **B Alchemy tab:** `unlock_alchemy` gains sibling `base_alchemy`; `ResearchManager.complete` → `completeSiblings`; `categoryOpen` allows hidden openers with parents satisfied.
- **C Observation/auto-advance:** `RequirementRowRenderer` calls `claimAutoAdvance` + `ServerboundAdvanceStagePacket` for requirement-less stages; ScanBindings still grants OBSERVATION to basics/alchemy/auromancy.
- **D Visuals:** Wave 0 recipe bookmarks resolve (no red missing ids for base_alchemy set).
- **E Tags bridge:** `TTCommonTags` remaps Neo `#c:` → Forge `forge:` (AspectIndex + ResearchRequirement); early research JSON remapped; forge tags for brass/thaumium/quartz/quicksilver/plates.
- **F QA / build:** `./gradlew build` SUCCESS; jar installed to `minecraft/mods/thaumaturge-forge-1.20.1-1.1.0-forge.jar`.
  - Checklist (datapack/code evidence; in-game playthrough after reload):
    1. gotthaumonomicon / open book — pass (prior + jar)
    2. first_steps dust + observation — pass (recipes + ScanBindings)
    3. research table note/hex/theory — pass (prior wave)
    4. unlock_alchemy crucible → observation → nitor → advance — pass (craft credit + stages)
    5. Alchemy tab + base_alchemy recipes clickable — pass (siblings + recipe JSON in jar)
    6. JEI same crucible/AW/dust ids — pass (datapack)
    7. Shift-empty crucible / throw insert / craft credit — pass (prior)
    8. table→research_table / workbench loot / nitor tint — pass (prior regression)

## Wave 0.5f — Aspect combine graph = CE AspectBootstrap
- Rewrote `TTAspects` compound parents/colors to match CE `AspectBootstrap.DEFINITIONS`.
- Fixed collisions that broke research-table combine uniqueness:
  - `vitreus` was `terra+ordo` (same as `metallum`) → CE `terra+aer`
  - `herba` was `terra+aqua` (same as `victus`) → CE `victus+terra`
  - `praecantatio` was `vacuos+aer` → CE `potentia+aer`
  - `instrumentum` was `metallum+humanus` → CE `metallum+potentia`
  - `spiritus` was `mortuus+aer` → CE `victus+mortuus`
- Check: all 31 compound pairs unique + build/jar reinstall.

## Wave 0.5e — Crucible heat/throw + aspect alpha + research table props
- Crucible heat: `thaumaturge:crucible_heat_sources` (lava/fire/campfire/magma + all nitors under the cauldron).
- Crucible hollow CE collision so thrown items `entityInside` absorb while boiling.
- Thaumometer crucible aspects: CE shows live container aspects — Forge already matches; kept.
- `BookBlit.iconTile` enables blend (fixes opaque black “jpg” aspect chips); amount flush before digit draw.
- `ResearchTableRenderer`: inkwell mesh + quill when scribing tools installed.
- Check: build + jar reinstall.

## Wave 0.5d — Phial lang + scribing tools + table_wood id
- Added `item.thaumaturge.phial` (en/ru + localized aliases).
- Recipes: `scribing_tools`, `scribing_tools_alt`, `scribing_tools_refill` (CE parity).
- Renamed registry/recipe/loot `table` → `table_wood` so research bookmarks/`research` entry resolve.
- Check: build + jar reinstall.

## Wave 0.5c — Thaumometer amount Z + flat aspect glyphs
- `TTFlatRenderTypes.entityTranslucentFlat`: nearest, no mipmaps, `COLOR_WRITE` only (CE flat pipeline stand-in).
- Lens grid: draw amount after each tag, flush translucent batch first, `Font.DisplayMode.NORMAL` + CE `AMOUNT_FRONT_Z`.
- GUI chips: amount translate Z=200 so blit cannot cover the digit.
- Check: build + jar reinstall.

## Wave 0.5i — Thaumonomicon completed-stage text overlap
- `PageTextLayout.compose` always reserves requirement-row space (CE passes `complete=false`), so finished stages no longer draw prose over "Completed"/craft icons.
- Check: build + jar reinstall.

## Wave 0.5h — Alchemy crucible/metallurgy recipes
- Added missing crucible `ingot_thaumium` (iron + praecantatio/terra).
- Metallurgy helpers: `plate_thaumium`, `metal_brass`/`metal_thaumium` (+ unpack), brass/thaumium nuggets; plate_brass now requires `thaumaturge:ingot_brass` (not `#forge:ingots/brass`).
- Registered items: `plate_thaumium`, `tallow`, `nugget_brass`, `nugget_thaumium`.
- Hedge Alchemy crucible set + `candle_white`; research no longer points at missing `cracked_stone_bricks`.
- Metallurgy craft gates require specific thaumaturge ingots (not shared brass tags).
- Instance AlmostUnified: prefer `thaumaturge`, ignore alchemical brass items, ignore `thaumaturge:crucible` remaps (IC2 brass collision).
- Check: build + jar reinstall.

## Wave 0.5g — Wave 0–1 SFX/VFX parity pass
- `sounds.json` + `TTSounds`: full CE catalog; Wave 0–1 events wired at call sites.
- Book: browser `CLACK`/`PAGE`; entry detail page/bookmark/write cues; aspect discovery `LEARN`.
- Crucible: dissolve `BUBBLE`, empty `SPILL`, craft bamf `POOF` + wisp particles; dust swap bamf `POOF`.
- Particles: `wisp`/`essentia`/`bore`/`taint` JSON point at mod textures (not `minecraft:generic_0`).
- Block sounds: crystals → `TTSoundTypes.CRYSTAL`, jar → `TTSoundTypes.JAR`.
- Check: build + jar reinstall.

## Wave 2 — Aura / Nodes (plan + Make-it-Work)

### In (Wave 2a/2b)
- Chunk aura lifecycle: `AuraManager` load tracking + `AuraTickHandler` regen (Forge events; no Neo Transaction).
- Nodes: `NodeType`/`NodeData`, `BlockNode` + `BlockEntityNode`, creative placer, `NodeFeature` worldgen.
- Client: minimal node BER; thaumometer node aspects; live aura snapshots for HUD.
- Research opener: `unlock_auromancy` → `nodes` / `base_auromancy` / `dioptra` with working recipes; **minimal wand** (`wand/assembly/iron_wood`).
- Recharge pedestal: chunk aura → wand vis; dioptra block + AW recipe.

### Out (Wave 2.x / later)
- Vis-relay graph, flux pressure/rifts, jar-node ritual, full stabilizer/transducer, foci spell engine, taint↔node coupling.

### CE inventory (reference under `src/ce26`)
- `api/aura/*`, `api/nodes/*`, `content/aura/{AuraManager,AuraTickHandler,AuraGenHandler}`, `content/aura/node/*` (29), `content/aura/relay/*`, `content/device/{BlockDioptra,BlockVisBattery,BlockVisGenerator}`, client `NodeRenderer`/`DioptraRenderer`.

### Forge baseline before Wave 2
- Present: `AuraData` + chunk cap, aura packets/HUD, workbench drain / crucible flux dump.
- Missing: tick sim, node BE/worldgen, ScanNode, auromancy recipes, real dioptra/pedestal/wand.

### Opener recipe gaps (audit)
- Shipped Wave 2: `wand/assembly/iron_wood`, `wand_cap_iron`, `arcane_workbench/vis_resonator`, `arcane_workbench/dioptra`, `arcane_workbench/recharge_pedestal`.
- Still deferred (foci engine): `crucible/focus_1`, `arcane_workbench/focal_manipulator` — `base_auromancy` stages retargeted to vis_resonator/dioptra/pedestal so bookmarks stay green.

### Shipped (Wave 2a/2b Make-it-Work)
- Aura tick: `AuraManager` + `AuraTickHandler` + `AuraHelper` (chunk regen / neighbour share; no flux pressure).
- Nodes: `BlockNode`/`BlockEntityNode`, creative placer, `NodeFeature` + `nodes_wild` worldgen + forge biome modifier; thaumometer lens/scan node aspects; minimal `NodeRenderer`.
- Wand: `WandItem` + iron_wood assembly (chunk siphon).
- Dioptra (vis/flux grid + comparator) + recharge pedestal (aura → wand vis).

### QA checklist
- [x] After workbench drain, chunk vis regenerates over seconds (AuraTickHandler).
- [x] Creative node placer / worldgen places a scannable node; thaumometer shows type + aspects.
- [x] Craft wand_cap_iron → wand (`wand/assembly/iron_wood`); unlock_auromancy craft gate clears.
- [x] Craft vis_resonator → dioptra / recharge_pedestal via AW; pedestal refills seated wand from aura.
- [x] Build + jar reinstall into `minecraft/mods/` (`thaumaturge-forge-1.20.1-1.1.0-forge.jar`).

## Wave 2.5 — Auromancy progression + node break polish

### In
- Altitude milestones `m_deepdown` / `m_uphigh` while `unlock_auromancy` is open (CE margins).
- `ScanKeys.aspect` + complete `scanned/aspect/...` on aspect-bearing scans (unblocks dioptra gates).
- Node destroy burst (AspectPools grant / essentia crystal drops + sparkle FX); still breakable without thaumometer.
- Vis resonator tooltip: craft-only (no world effect under pedestal).

### Out
- Vis-relay, pedestal←node siphon, jar-node, stabilizer/transducer, EntityAspectOrb, CE observation aspect-pool spend.

### QA checklist
- [x] With unlock_auromancy started: visit deep caves and high Y → both milestones unlock; craft wand advances opener.
- [x] Scan something with sensus/auram → dioptra `scanned/aspect` requirements clear; auromancy observation increments.
- [x] Break a node without thaumometer → aspect burst (grant/crystals) + FX.
- [x] Vis resonator tooltip explains craft-only; pedestal still recharges wand from chunk aura.
- [x] Build + jar reinstall.

## Wave 2.6 — Observation = aspect pool (CE parity)

### In
- `ResearchNotes.observationCost` / `stageObservationCost` from entry `note_aspects`.
- `ResearchManager.stageRequirementsMet` + `advanceStage` spend via `AspectPools.canAfford` / `spendAll` (not category counters).
- Thaumonomicon observation UI: aspect chips (have/need) instead of artifice 0/1.

### Out
- Full CE ResearchGrants random primal conversion; theory path unchanged; vis-relay/foci.

### QA checklist
- [x] Discover/fill fabrico + instrumentum (or components) → `unlock_artifice` Complete enabled; advance spends pool.
- [x] Dioptra observation chips show auram/sensus/machina from `note_aspects`.
- [x] Build + jar reinstall.

## Wave 3.0 — Deconstruction table

### In
- `BlockDeconstructionTable` / BE / Menu / Screen / `AspectPrimals` + `ServerboundDeconCollectPacket`.
- Research gate `deconstructor`; AW recipe (thaumometer + table + golden axe/pickaxe, Perditio 1).
- Bulk stone `deconstruction_table` removed; real block registered in TTBlocks/BE/Menus/Items.

### Out
- CE 1.21 BER mesh / fancy model; custom recipe type (none in CE).

### QA checklist
- [x] Aspect-bearing item in slot → after ~40 ticks consume + result chip → click grants 1 primal to pool.
- [x] Without `deconstructor` complete → action-bar "not understood", GUI blocked.
- [x] Build + jar reinstall.

## Wave 3a — Goggles of Revealing

### In
- `GogglesItem` (helmet) + `GogglesAccess.wearsRevealingGear` (head slot only).
- AW recipe `goggles_revealing` gated by `unlock_artifice`; vis ~50 as primal aspect list.
- `NodeRenderer` shows nodes with thaumometer (~48) or goggles (~64); `AuraHudOverlay` visible with goggles.

### Out
- World aspect tag overlays, Jade/Curios, vis-discount gear attribute, fortress goggles, full `base_artifice` batch.

### QA checklist
- [x] Craft/equip goggles → aura HUD + nodes visible without holding thaumometer.
- [x] `unlock_artifice` stage 1 bookmark resolves to goggles recipe.
- [x] Build + jar reinstall.

## Wave 3.x — Port gap checks

### In
- `scripts/check_port_gaps.ps1` + `scripts/port_deferred.json` (+ Python twin); Gradle `checkPortGaps` / `check.dependsOn`.
- Hard-fail on research recipe bookmarks without recipe JSON (unless deferred) and broken parents.
- Soft warn for missing lang; deferred list for unported CE bookmarks.

### QA
- [x] `check_port_gaps` exit 0 with Wave 4 recipes removed from deferred.

## Wave 3.x — GameTest harness

### In
- `src/test` + `runGameTestServer`; `PortCoreTests` (pool grant/spend, decon collect, observation advance, deconstructor gate).
- `structures/empty.nbt` for templates.

## Wave 4 — Artifice tier-1 MiW

### In
- Items: `mechanism_simple`, `mechanism_complex`, `mirrored_glass`, `plate_iron`, `thaumium_shield`.
- Blocks: `hungry_chest` (swallow items), `lamp_arcane` (light 15), `item_grate` (open drop-through), `redstone_relay` (fixed diode).
- AW/vanilla recipes + tags; removed from bulk / deferred.

### Out
- CE lamp BE pulse; grate inventory eject; relay adjustable thresholds; infernal furnace / grapple / warded / golemancy.

## Wave 5.2 — Node depth / chest BEWLR / CE trees

### In
- Nodes: depth-**test** on + depth-**write off** (`thaumaturge_translucent_flat_world`) — solids occlude; water no longer punched; no wall X-ray (CE `TEST_NO_WRITE`, not CE goggles no-depth).
- Hungry chest: particle-only block model; inventory BEWLR via `BlockEntityRenderDispatcher.renderItem` + `builtin/entity`.
- Greatwood/silverwood: CE `crown_*` / `silverwood_trunk` + `leaf_cell` placers registered (`TTTreePlacers`); configured + grown features updated (jar previously still shipped fancy/straight oak).

### QA
- [x] Goggles/thaumometer: nodes hide behind solid blocks; look OK through/near water.
- [x] Hungry chest is a 3D chest in inv/hand/world (not a plank cube).
- [x] New chunks / grown saplings: greatwood crown + silverwood shapes (not oak).

## Wave 5.1 — Playability hotfix (tabs / dioptra / chest item / nodes / biomes)

### In
- Category tabs gate on `unlock_*` (auromancy pattern) so completed unlocks open tabs on old saves; login/book backfill still grants `base_*` siblings.
- Dioptra AW: gate `unlock_artifice`, ingredient `thaumaturge:plate_iron` (avoids AlmostUnified tag empties).
- Hungry chest inventory: `HungryChestItem` + BEWLR (`builtin/entity`).
- Node BER: translucent no-depth render type (fixes water punch-through) — **superseded by 5.2**.
- Worldgen: TerraBlender regions replace forest→magical_forest and swamp→tainted_lands (Lithostitched biome_injector is 1.6+ only); denser greatwood/silverwood placers.

### QA
- [x] Reopen thaumonomicon after unlock_alchemy/artifice → tabs appear; relog backfills base_*.
- [x] Dioptra crafts after unlock_artifice with plate_iron pattern.
- [x] Hungry chest looks like a chest in inventory; nodes intact over water.
- [x] New chunks: magical forest / tainted lands via TerraBlender; more greatwood in forests.

## Wave 5a — Playability blockers

### In
- Category unlock siblings: `unlock_artifice` → `base_artifice`, `unlock_infusion` → `base_infusion`, `unlock_eldritch` → `base_eldritch`.
- Dioptra AW recipe uses `forge:plates/iron` (CE parity).
- `en_us` block names filled from CE `TTEnglishProvider` (+ fallbacks); Wave 5 gui/item strings.
- `HungryChestRenderer` registered (chest entity texture).
- `NodeRenderer`: pulsed aspect layers, atlas frames, core styles, unstable energized overlay (drain/jarred still Out).
- Worldgen biomes registered: `magical_forest`, `magical_forest_caves`, `tainted_lands` (lithostitched injectors + forest-tag tree modifiers).

### Out
- Full Iris node shader / drain floaty lines / jarred-node BER path.
- Full CE magical-forest flora / taint biome feature sets.

### QA
- [x] Unlock artifice → Artifice tab visible; infusion/eldritch after their unlocks.
- [x] Craft dioptra with iron plates after research.
- [x] Blocks show names; hungry chest visible; nodes pulse with goggles/thaumometer.
- [x] Greatwood/silverwood in forests; custom biomes present when lithostitched runs.

## Wave 5b — Artifice utility

### In
- `levitator` Block/BE (aura charge, push column, reach cycle, redstone off).
- `arcane_ear` + `arcane_ear_toggle` (note tune, RS pulse / toggle, Forge ear index + `NoteBlockEvent`).
- AW recipes; removed from bulk + `port_deferred`.
- `m_hellandback` on Nether biome visit (`ResearchProgressionEvents`).
- GameTests in `ArtificeUtilityTests`.

### Out
- Levitator mist particles; CE ear level attachment parity; infernal furnace / grapple.

### QA
- [x] Levitator lifts entities when charged; ear pulses; Nether → m_hellandback.
- [x] checkPortGaps + GameTests + jar.

## Hotfix — silverwood_node_log AXIS crash

### In
- `silverwood_node_log` was a plain bulk `Block` (no `axis`); `SilverwoodTrunkPlacer.placeNodeLog` called `setValue(AXIS)` → chunkgen crash on `silverwood_natural`.
- Ported CE `BlockSilverwoodNodeLog` (RotatedPillar + node BE); registered in `TTBlocks` / `TTBlockEntities.NODE`; axis blockstate reuses silverwood log models.

### QA
- [x] New chunks with silverwood features no longer crash on feature placement.

## Next — five steps to close the port (priority order)

Waves 0–5 leave a playable opener → alchemy → auromancy → artifice slice. The gaps below are the shortest path to a coherent mid/late game instead of more polish on shipped systems.

1. **Essentia network (tubes + jar polish)** — multipart/simple Forge tubes, transfer tick, full jar fluid overlay + JEI tank. Unlocks the post-crucible alchemy loop and unblocks infusion recipes that move aspects in-world.
2. **Infusion pillar** — matrix + runic matrix pedestals, recipe type, instability/flux hooks, research gates. Largest missing content pillar; most research/bookmarks still dead without it.
3. **Aura network Wave 2.x** — vis relay, jar-node, stabilizer/transducer (node drain / store / stabilize). Completes auromancy after dioptra/pedestal so vis is a placeable system, not only chunk HUD.
4. **Foci / casting MiW** — focus bag + 1–2 foci + wand cast path (even without full CE spell graph). Turns the wand from a vis battery into the player power fantasy and exercises aura spend outside workbench.
5. **Content hygiene pass** — JEI duplicate/bulk-vs-real triage, `FireBlockMixin` + Salis Mundus / dust multiblock triggers, Curios goggles slot, kill or replace remaining stone placeholders that still show as “Thaumaturge” blocks. Makes the pack look finished and stops false bug reports.

Parked after those (not next five): full research-table note minigame BER, labyrinth chunkgen, golemancy, deep FX/Iris, fortress armor trims, full `TTRecipeProvider` dump.

## Notes / next Make-it-Right
- JEI: possible duplicate block entries — triage later (bulk placeholders vs real blocks) → owned by step 5 above.
- Full behavioral parity of every CE26 class is not line-ported; `src/ce26` is the migration source.
- Bulk blocks/items are stone/plain placeholders until bespoke classes are moved over.
- Armor trims, foci spell engine, labyrinth chunk generator, and deep FX pipelines still need CE26 → Forge ports.
- Deferred (after the five): research-table helper/BER parity, full JEI categories, full TTRecipeProvider, world aspect overlays, labyrinth, golemancy.

## Wave 6 — Essentia smelting and transport

- Replaced the basic smelter and alembic placeholders under their existing registry IDs. The basic smelter follows CE's 256-point buffer, 80% normal yield, reduced Vitium yield, flux loss and 15-tick alembic export cadence. Alumentum burns for 4,800 ticks and reduces the export interval to 12 ticks.
- The alembic column, ordinary tubes, warded jars and phials form a survival chain. Use a jar on an alembic to scoop essentia directly. A phial moves 10 points between a vessel and the inventory. Sneak-use with an empty hand to spill a placed jar or alembic into the aura.
- Checked the wave recipes against CE. The workbench now accepts a scalar vis cost and still reads older aspect-list vis costs. Crystal costs are separate. Compound crystals required by the smelter use the CE crucible formula. Cinnabar smelting supplies quicksilver, and both wood types have working plank and leaf drops. The new recipes use Thaumaturge's plate and nugget IDs where pack-wide unification could substitute another mod's item.
- The Thaumonomicon checks the phial's aspect and amount and the crystal's aspect against item NBT on both client and server. The changed recipe packets require Thaumaturge protocol 6 on both ends. Updated the affected English and Russian entries to describe the available mechanics.
- Deferred: alembic spout controls and labels, tube valves/filters and wand side controls, smelter upgrades, and the infusion altar.

### Проверки в игре

Запуск из каталога проекта под Java 17.

- `gradlew.bat runGameTestServer` запускает набор в отдельном мире Forge. Новые тесты проверяют перегонку через две трубы, восстановление содержимого после пересоздания блок-сущностей из NBT, перелив флакона и требования инфузии, а также загруженные рецепты и таблицы добычи.
- `gradlew.bat runClient` запускает dev-клиент. В отдельном мире с командами введите `/test run essentiatests.smelterdistillsthroughtubes`. Остальные имена из `EssentiaTests` доступны в автодополнении `/test run`. Команда размещает тестовую структуру в мире.
- Для ручной проверки в TMR-dev откройте плавильню через «Алюментум» и «Алхимическую металлургию». Сделайте составные кристаллы в тигле, соберите плавильню с кубом сверху и наполните флаконы для исследования труб. Подведите трубы от боковой стороны куба к верхней стороне банки. Проверьте, что заполненный флакон Aer засчитывается в первой стадии инфузии, затем перезайдите и проверьте содержимое установки. Киноварь генерируется в новых чанках.




## Банка с мозгом, стабилизация инфузии и сеть vis

- Банка притягивает сферы, хранит до 2 000 опыта, сохраняет запас в установленном блоке и предмете, выдаёт его по использованию пустой рукой и даёт 5 силы зачарования.
- Симметрия черепов, свечей, стабилизаторов и инкрустации влияет на риск инфузии. Стабилизатор заряжается, выделяет flux, воздействует на ближайшие разломы и через инкрустацию или соседние пьедесталы может предотвратить выброс ингредиента.
- Используйте стабилизатор под узлом и подайте редстоун на трансдьюсер над ним. Реле передают vis от активированного узла к магическому верстаку и пьедесталу подзарядки. Между источником и реле, а также между реле, нужна прямая видимость. Дальность одного звена составляет 8 блоков, предел цепочки составляет 16 реле.
- На этой машине для запуска Gradle под Java 17 задайте переменные в PowerShell из каталога проекта.

  ```powershell
  $env:JAVA_HOME = Join-Path $env:USERPROFILE '.jdks\ms-17.0.20.1'
  $env:GRADLE_USER_HOME = Join-Path $PWD '.gradle\user-home'
  .\gradlew.bat build runGameTestServer
  ```

## Сверка с CE26 и бездонная урна

- Кристаллы магического верстака можно заменить vis того же аспекта из жезла или ближайшего связанного реле. Общую цену покрывает аура. Зарядник распределяет расход между загруженными чанками.
- Стадии исследований расходуют сдаваемые предметы, выдают опыт и начисляют указанное искажение. Крафтовые требования учитывают подходящий предмет в инвентаре.
- Инфузия принимает смешанные колонны. Бонус колонн применяется к однородному комплекту. Поиск пьедесталов охватывает от семи блоков ниже матрицы до трёх выше.
- Тигель принимает брошенные живым игроком предметы и оставляет собственный результат для подбора. Обычная, таумиевая и пустотная плавильни используют интервалы выгрузки CE26. Банка эссенции и перегонный куб передают заполненность компаратору.
- Урна создаёт до 1 000 мБ воды за 1 vis, наполняет вёдра и бутылки и подаёт воду в котлы и через верхнюю грань приёмников. Рецепт тигля использует украшенный горшок, 30 Aqua, 10 Fabrico и 10 Terra.

Сверка также выявила отдельные этапы порта. Полный граф заклинаний, небесные заметки, постоянные связи реле, питание продвинутой плавильни, поэтапная инфузия, декорации биомов, големантия и Лабиринт требуют дальнейшей работы.

## Поэтапная инфузия, реле, заметки и первые части генерации и фокусов

- Инфузия сохраняет расчёт и прогресс ритуала, расходует эссенцию по единице, затем забирает ингредиенты по одному. Старый активный ритуал пересчитывается по сохранившимся ресурсам.
- Реле сохраняют родителя и глубину. Продвинутая плавильня заряжается и в простое из активированных узлов, сначала через реле.
- Таумометр записывает солнце, фазы луны и звёздные квадранты. Бумага расходуется, письменные принадлежности требуются в инвентаре. Заметки можно изучать.
- Добавлены обсидиановые тотемы, руины на холмах, растения волшебного леса, пещерные пруды и кусты, кварцевая и янтарная руда, огнеперл в пустынях и бедлендах, корка и волокно в заражённых землях. Волшебные пещеры подключены к TerraBlender.
- Фокусы хранят граф с узлами CE26. Существующие огненные и морозные фокусы читаются из старого NBT. Поддерживаются касание, болт и простое разветвление. Для поддерживаемых узлов считаются сложность, vis и перезарядка по формулам CE26. Полный редактор, остальные узлы и стили каста требуют дальнейшего переноса.
- В генерации ещё отсутствуют багровые порталы, часть декораций и мобов волшебных и заражённых биомов и встреча с виспом в руинах.

## Перенос узлов и запас vis

- Обычный и продвинутый стабилизаторы стоят основанием вниз, трансдьюсер обращён к узлу снизу. Корпуса и четыре лапы получают освещение мира. Устройства используют исходные регистрационные ID.
- Соберите жезл из стержня серебряного дерева и двух железных наконечников. Стержень получается инфузией и даёт по 100 vis каждого первородного аспекта. Откройте «Узел в банке», окружите свободный узел стеклянными блоками по схеме в Таумономиконе и накройте конструкцию деревянными плитами. Щёлкните жезлом по стеклу. Ритуал расходует по 70 vis каждого первородного аспекта из жезла в руке. Модификатор может ухудшиться.
- Подберите банку и установите в другом месте. Щелчок жезлом высвобождает узел. Использование жезла вприсядку позволяет вытягивать vis через стекло.
- Батарея принимает до 10 vis из ауры выше 90% базовой ёмкости. Она возвращает заряд ниже 75% или по сигналу редстоуна. Компаратор показывает уровень заряда.
- GameTests проверяют перенос с повторной установкой, рецепты, обмен батареи с аурой и питание верстака и удалённого пьедестала через два реле. Клиентская проверка проверяет геометрию и стандартное освещение корпусов. В Forge захват выполняется сразу, а модели устройств используют неподвижные лапы.

## Исследования извлечения vis

- «Продвинутое извлечение» повышает объём до двух единиц за импульс, «Мастерское извлечение» — до трёх. Жезл выбирает доступный первичный аспект и забирает только то, что помещается.
- «Сохранение узла» оставляет последний пункт аспекта при работе улучшенным жезлом. Приседание разрешает забрать остаток.
- План первого выпуска с игровым и визуальным паритетом описан в [FORGE-RELEASE-PLAN.md](docs/FORGE-RELEASE-PLAN.md).

## Угасание и лента извлечения

- Аспект с нулевым запасом не восполняется из ауры. Раз в 1 200 загруженных тиков узел может потерять ёмкость одного истощённого аспекта. При полной потере ёмкости узел распадается. Бревно сохраняет ось, тотем сохраняет соединения. Это правило распространяется на узлы в банках. Активированный узел пропускает угасание и при деактивации сохраняет запас аспектов, входящих в исходную ёмкость.
- При изъятии vis из узла клиент получает цвет аспекта и игрока. Forge использует текстуру и профиль анимированной ленты CE26, а также позу жезла от первого лица. Состояние ленты исчезает через 12 тиков после последней успешной передачи и не записывается в сохранение.
