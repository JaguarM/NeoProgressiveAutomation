### The Mining Model

Two complaints, one cause: the drill leaves scars, and it has to be moved constantly.

Both fall out of a single decision — **the drill targets a volume, not a resource.** It is
handed a box and told to empty it. A box is finite, so the machine always exhausts and
always needs relocating, and emptying it necessarily rearranges the terrain. Every other
symptom follows from that one choice, so that is the choice worth revisiting.

---

### What the three examples actually teach

**BuildCraft's Quarry — volume targeting, taken to its conclusion.**
A frame, a rectangular pit, bedrock. Iconic and universally regarded as ugly. Worth noting
*why* it is remembered fondly anyway: it is completely legible. You know exactly what it
will do and when it will finish. That legibility is the thing to keep; the crater is not.

**IE's Excavator — the resource is data, not blocks.**
Sits over a mineral vein that exists as per-chunk information rather than as ore in the
ground. Never touches terrain, runs for thousands of items, and relocating is a rare,
deliberate event. It solves both our problems outright — but it stops being mining. The
world becomes scenery. We already rejected that direction once by choosing crumbling ore
over abstract patches.

**Mekanism's Digital Miner — configuration as the whole design.**
Large radius, filter list, replace-with block, silk toggle, min/max Y, inverse mode. The
lesson is not the filters themselves but that *you configure it once and stop thinking
about it*. Rarely moved because its reach is large, and it leaves no hole because you told
it what to leave alone. The cost is a genuinely large UI.

---

### The fork

| | Volume targeting | Resource targeting |
|---|---|---|
| What it does | empties a box | extracts what is worth having |
| Terrain | rearranged by definition | untouched |
| Lifetime | finite, always exhausts | as long as the deposit lasts |
| Siting | irrelevant, put it anywhere | **the decision** |
| Fails when | never | nothing valuable in range |

We already have both. The Filter module *is* resource targeting: it walks past everything
that is not ore, so no hole forms, no cobble is spent, and no stone reaches the output.
The problem is that it is an optional extra on one tier rather than the point of the
machine.

The strongest version of this mod is one where **siting matters**, because that is the
actual Factorio loop. Not "place drill, receive ore" but "where is the patch, and is it
worth putting a drill here". Right now you can drop a drill anywhere and it works, which
is why relocation feels like chores rather than decisions.

---

### Plans

**1. Filtered drills get a far larger radius.** *(small change, largest effect)*

A drill that only breaks ore can afford to reach much further, because reach no longer
means destruction. Make the Range module worth several blocks instead of one when a Filter
is installed, or give filtered mode its own range formula.

- Directly attacks relocation: a wide filtered drill works a whole ore field, not a shaft.
- Costs nothing in terrain, since stone is never touched.
- Trade-off: makes the Filter close to mandatory on the electric drill, which is arguably
  correct but does narrow the module choice.
- Trade-off: a big radius over sparse stone means long scans finding nothing. The per-tick
  scan budget already exists for this, but a wide drill will spend real time searching.

**2. Survey before placing.** *(medium change, makes siting a real decision)*

Show what is actually in range: a count of ore blocks, or a breakdown by type, in the GUI
and on the area preview. IE's core sample idea, without the multiblock.

- Turns placement from a guess into a judgement, which is the point of the whole direction.
- Pairs with plan 1: a big radius is only interesting if you can tell a good spot from a
  bad one.
- Trade-off: risks becoming an x-ray. Counting ore in a volume is different from showing
  where it is, and the first is fine while the second is a cheat. Keep it to totals.

**3. Mekanism-style filter configuration.** *(large change, most flexibility)*

Let the player define what counts as worth mining — tags, specific blocks, an inverse mode
— instead of hardcoding "ores".

- Enormous for pack authors, and the reason the Digital Miner is the one people keep.
- Trade-off: a real UI, with a scrolling list, add and remove, and persistence. This is the
  most expensive item here by a wide margin.
- Trade-off: fights "simple and feels great". The Digital Miner is many things; our pitch
  is one machine that is excellent.

**4. Leave the burner crude.** *(no change, stated for completeness)*

The burner drill has no module slots, so it cannot filter. It digs everything, backfills
cobble, exhausts, and must be moved. That is the scar and the chore — deliberately.

- Gives the electric drill something concrete to be better *at*, rather than merely faster.
- Trade-off: the early game is the ugly game. If a player never reaches electric, the mod
  they experienced is the bad one.

---

### Recommendation

**Plan 1, then plan 2. Skip plan 3 for now.**

Together they turn the electric drill into something closer to the Excavator in feel while
keeping ore as real blocks in the world: a machine you site deliberately over a patch,
which then works that patch for a long time without leaving a mark. Relocation becomes
rare and meaningful instead of constant and tedious, which was the actual complaint.

Plan 3 is the right idea in the wrong mod. "One miner that is simple and feels great" and
"a configuration screen with a filter list" pull in opposite directions, and the Digital
Miner already occupies that space well. A config-file list of extra tags gets most of the
benefit for none of the UI.

The honest risk in this direction: a filtered drill does nothing where there is no ore, and
that failure is silent unless the status line and the survey make it obvious. Plan 2 is not
decoration — it is what stops plan 1 feeling broken.
