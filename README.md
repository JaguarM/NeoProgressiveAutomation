Neo Progressive Automation
==========================

Two mining drills, and nothing else.

| | |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.95 |

The drills
----------

**Burner Mining Drill** — solid fuel, one module slot. Crafted from stone, a furnace and a
stone pickaxe.

**Electric Mining Drill** — FE, four module slots, and it accepts power from any cable.
Crafted from a burner drill, so the progression is literal.

Both need a pickaxe and a shovel, dig straight down beneath themselves to bedrock, and push
what they find into any container placed against them. No hopper required.

Modes
-----

A button in the drill's screen cycles what it is willing to break:

| | |
|---|---|
| **Ore Only** | Takes ore, leaves the terrain standing. No holes, no fill needed. The default. |
| **Clear + Fill** | Takes everything and fills the hole back in, one block of fill per block dug. |
| **Clear** | Takes everything and leaves the pit. |

In Clear + Fill the drill refills itself from the stone it digs, so it only needs a starter
stack of cobblestone. Cobbled deepslate counts as fill too, and it places whichever it is
holding.

Modules
-------

Modules trade one thing for another rather than simply being better. One per slot.

| | |
|---|---|
| **Speed** | 0.80x mining time, 1.40x running cost |
| **Efficiency** | 0.60x running cost, 1.15x mining time |
| **Range** | +1 radius |

Effects compound, so three speed modules are 0.8³ rather than a flat cut.

Details that matter
-------------------

Drills mine at **exactly vanilla speed** for the tool they hold — an iron pickaxe takes as
long on stone as it would in your hand. `miner.speedMultiplier` in the config changes that
if you want, without breaking how tools and Efficiency relate to each other.

They **mine as the player who placed them**, so land protection and claim mods treat a drill
as its owner and can refuse it.

They show you **the block they are working**, using the same crack overlay a player's
mining does, and **the area they cover** when you look at one. A status line says why a
drill has stopped rather than leaving you to guess.

They cooperate with mods that intercept mining: if something else handles a break, the
drill collects what was dropped and carries on. [Crumbling Ore][crumbling] is the obvious
companion, but nothing here depends on it.

[crumbling]: https://github.com/JaguarM/CrumblingOre

Config
------

```
miner.speedMultiplier      = 1.0     # 1.0 is exactly player speed
miner.mineFloor            = -64     # lowest Y a drill will reach
range.initialRange         = 1       # radius with no modules
energy.capacity            = 40000   # FE buffer on the electric drill
energy.perTick             = 40      # FE while actually mining; idle costs nothing
```

Building
--------

```
./gradlew build      # jar lands in build/libs
./gradlew runClient  # dev client
```
