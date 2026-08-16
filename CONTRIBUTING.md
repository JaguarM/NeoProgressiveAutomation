Working on this codebase
========================

Notes for whoever picks this up next, human or model. Written because most of what follows
cost real time to discover and none of it is guessable.

---

1. Do not trust your memory of the Minecraft API
------------------------------------------------

This targets **Minecraft 26.2 / NeoForge 26.2.0.59**. Large parts of the API were renamed or
replaced in the 26.x line, and almost every tutorial, Stack Overflow answer and model's
recollection describes the older shape. Code written from memory compiles about half the
time and is subtly wrong the rest.

**Read the real sources.** They are on disk after any build:

```
Minecraft (decompiled, NeoForge-patched):
  ~/.gradle/caches/neoformruntime/intermediate_results/mergeWithSources_*_output.jar
Vanilla assets and data (models, blockstates, recipes, loot tables):
  ~/.gradle/caches/neoformruntime/artifacts/minecraft_<version>_client.jar
NeoForge:
  ~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/<ver>/*/neoforge-<ver>-sources.jar
```

Extract with `jar xf <jar> path/to/Class.java` and read it. When several `mergeWithSources_*`
jars exist, the newest is the current version — the older ones are previous Minecraft
versions and will mislead you.

`https://docs.neoforged.net/` is worth reading too, and is version-current. Source tells you
what compiles; the docs tell you what is idiomatic. Check both.

### Renames and moves already discovered

| Older API | 26.x |
|---|---|
| `ResourceLocation` | **`Identifier`** |
| `LevelHeightAccessor.getMinBuildHeight()` | `getMinY()` |
| `BlockEntity.saveAdditional(CompoundTag, Provider)` | `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` |
| `DirectionProperty` | `EnumProperty<Direction>` |
| `Capabilities.ItemHandler.BLOCK`, `IItemHandler` | `Capabilities.Item.BLOCK`, `ResourceHandler<ItemResource>` |
| `IEnergyStorage` | `Capabilities.Energy.BLOCK`, `EnergyHandler` |
| `BlockEvent.BreakEvent` | `net.neoforged.neoforge.event.level.block.BreakBlockEvent` |
| `Screen.renderBg(...)` | `extractBackground(GuiGraphicsExtractor, ...)` |
| `RenderType.lines()` | `RenderTypes.lines()` |
| `ShapeRenderer.renderShape(...)` | `collector.submitShapeOutline(pose, shape, type, colour, width, afterTerrain)` |
| `Minecraft.screen` | `Minecraft.gui.screen()` |
| `RenderLevelStageEvent` for custom geometry | `SubmitCustomGeometryEvent` |
| `EnchantmentHelper` with static enchantments | `ResourceKey<Enchantment>` + a registry lookup |
| `data/<ns>/recipes/`, `loot_tables/` | **singular**: `recipe/`, `loot_table/` |
| recipe `result.item` | `result.id` |
| item models in `models/item/` only | also needs an `assets/<ns>/items/<name>.json` entry |

Two that fail **silently** rather than at compile time, so watch for them:

- A wrong loot-table folder name does not error. The block just drops nothing.
- `ItemStack.getBurnTime` needs `(RecipeType, FuelValues)`; `level.fuelValues()` supplies it.

### Things that look usable and are not

- **`Gizmos`** (`Gizmos.cuboid(aabb, style)`) looks like the obvious way to draw a box in
  world. It throws unless vanilla has installed a `GizmoCollector` on the current thread,
  and NeoForge exposes no hook to do that. Still true in 26.2. Use
  `SubmitCustomGeometryEvent` and `submitShapeOutline` instead — see `MinerAreaPreview`.
- **`SimpleEnergyHandler`'s extraction limit applies to its owner too.** A buffer built with
  `maxExtract = 0` to stop cables draining it cannot be spent by its own machine either. Keep
  the buffer unrestricted and expose a `LimitingEnergyHandler` to the capability.

---

2. The zero-byte artifact trap
------------------------------

The NeoForged maven mirror intermittently serves **empty files with HTTP 200**. It is a CDN
fault, not a local one: the identical URL returns 0 bytes from `maven.neoforged.net` and the
correct file from Central. It has hit this project three times, on different artifacts.

Symptoms, which look nothing like a dependency problem:

```
Content is not allowed in prolog              <- an empty .pom parsed as XML
zip file is empty / zip END header not found  <- an empty .jar
```

The giveaway is a Gradle cache directory named `da39a3ee5e6b4b0d3255bfef95601890afd80709`,
which is the SHA-1 of the empty string.

Both `build.gradle` files pin the known-affected groups to Central with `exclusiveContent`.
Repository *ordering* is not enough, because the moddev plugin registers its repositories
first and wins. If it happens for a new group:

```bash
# 1. find and delete the empty artifacts
find ~/.gradle/caches/modules-2/files-2.1 -type d -name da39a3ee5e6b4b0d3255bfef95601890afd80709 -exec rm -rf {} +
# 2. add the group to the exclusiveContent filter in build.gradle
# 3. rebuild, refreshing resolution as well as files
./gradlew build --refresh-dependencies
```

`--refresh-dependencies` matters: deleting the file alone leaves Gradle resolving to the
path you just deleted.

---

3. Generated art
----------------

Textures are **generated, not drawn**. The scripts are the source of truth; editing a PNG by
hand will be silently overwritten.

- `texture-workshop/make_miner_textures.py` — the drill block textures. Holds three 16x16
  ASCII maps and one palette per drill, so both drills change together and cannot drift
  apart. `--preview` writes a contact sheet.
- The GUI panel and the module icons are generated by throwaway Java in the session
  scratchpad. If you change GUI layout, regenerate the panel **and** update the matching
  coordinates in `MinerMenu` and `MinerScreen`. They are three separate files that must
  agree, and nothing checks that they do.

`texture-workshop/README.md` explains the style. The short version: vanilla block textures
use six to thirteen colours, no gradients, no anti-aliasing. Start from a vanilla texture
rather than a blank canvas.

---

4. Design decisions, and why
----------------------------

Reversing one of these without knowing why it was made will reintroduce a bug that was
already fixed.

**Two drills, not four tiers.** Burner (fuel, 1 module slot) and Electric (FE, 4 slots). A
tier ladder was built and then deleted before release, because the modules already carried
the progression and the middle tiers were only a slower path to the same place.

**Ore-only by default, via a button.** Three modes: Ore Only, Clear + Fill, Clear. This was
briefly two modules and is deliberately not: a mode you flip is not a trade-off you commit
to, and it costs no slot. Ore Only is first, and therefore the default, because a machine
whose first act is to bury a new player in cobblestone does not get played long enough to be
judged on anything else.

**Fill is only required in Clear + Fill.** Requiring it in Ore Only starves the machine: it
never breaks stone, so it never produces the cobblestone that refills it.

**Vanilla mining maths.** `speed / hardness / 30` with the correct tool, `/ 100` without,
Efficiency as `level² + 1`. The 1.12.2 mod's approximation had no wrong-tool penalty at all
and was 3.3x too fast bare-handed. `miner.speedMultiplier` layers on top rather than
replacing it.

**Mining goes through a `FakePlayer`** carrying the placer's UUID. Without it the machine is
invisible to the rest of the game: land protection cannot refuse it, and it mines through
claims. `Level#mayInteract` is the check that actually gives protection mods their say.

**The drill absorbs item entities at a block whose break was cancelled.** This is how it
cooperates with any mod that intercepts mining, Crumbling Ore included. Whether anything
*dropped* decides "somebody handled it" versus "somebody refused it" — **not** whether the
blockstate changed. Crumbling ore keeps the same blockstate throughout, so the blockstate
test read every hit as a refusal and the drill walked away after one.

**The drill steps down after taking a block**, rather than letting the next scan notice the
position is empty. A position can refill: lava meeting water makes cobblestone, and the drill
mined that same spot forever.

**No dependency on Crumbling Ore.** They are separate mods on purpose, and neither compiles
against the other. Do not add a dependency to "make integration easier" — the event-based
cooperation is the integration, and it works for mods nobody has written yet.

**The drill does not draw break progress on ore.** Ore is where other mods most often take
over, and two crack overlays on one block fight.

---

5. Testing
----------

`./gradlew runClient` and play it. There is no automated coverage, and the bugs that mattered
were all found by playing: the Silk Touch duplication, the piston pull, ore being mined once,
the lava cobblestone loop. A green build proves the APIs exist, nothing more.

Checking a log for `Exception` after a run catches loading failures cheaply, but a machine
that loads and behaves wrongly looks identical to one that works.
