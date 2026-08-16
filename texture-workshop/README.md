Making block textures
=====================

How a block gets its look
-------------------------

Three files, each pointing at the next:

```
assets/neoprogressiveautomation/blockstates/miner_wood.json
    picks a model per blockstate  (lit=false -> block/miner_wood)
        |
assets/neoprogressiveautomation/models/block/miner_wood.json
    picks a texture per face      ("front", "side", "top")
        |
assets/neoprogressiveautomation/textures/block/miner_wood_front.png
    the actual 16x16 image
```

Swapping in your own art means only changing the strings in the model JSON to
`neoprogressiveautomation:block/<name>` and dropping the PNG in `textures/block/`.
Nothing in the Java changes. The miners used to point at `minecraft:block/furnace_front`
and friends, which is why they looked like furnaces; they now point at our own art.

The `parent` in the model does the 3D work for you. `minecraft:block/orientable` means
"a cube with a distinct front face"; `cube_all` means "same texture on all six sides".
You rarely need to define geometry yourself.

What the file actually is
-------------------------

- **16x16 PNG, RGBA.** Higher resolutions work (32, 64...) but must be a power of two, and
  mixing resolutions in one mod looks inconsistent.
- Transparency is supported, but for a solid block your model's `parent` assumes an opaque
  cube. Cut-out shapes need a different parent and a render type.
- Animation is a **vertical strip** plus a `.mcmeta` file next to it. A 16x64 PNG with
  `{"animation": {"frametime": 4}}` is four frames. This is how a lit machine face flickers.

The one lesson from vanilla
---------------------------

Open `reference-sheet.png`. Those are vanilla textures blown up with a pixel grid, and the
colour counts are the thing worth internalising:

| Texture | Distinct colours |
|---|---|
| cobblestone | **6** |
| oak_planks | 7 |
| furnace_top | 7 |
| iron_block | 11 |
| furnace_front | 13 |

Six colours for an entire rocky surface. Vanilla textures are not shaded drawings — they
are a handful of flat tones scattered to make noise. No gradients, no anti-aliasing, no
soft edges. If you find yourself picking a twelfth shade of grey, you have left the style.

Look at how `furnace_front` reads as a machine at all: a dark recessed rectangle, a lighter
frame around it, one highlight row along the top. Three ideas. That is the whole face.

Practical workflow
------------------

1. **Start from a vanilla texture, do not draw from scratch.** Copy the closest one out of
   `reference/`, recolour it, then change what makes your block different. This keeps your
   palette and noise density consistent with the game automatically.
2. **Build a palette first.** Pick 4-6 tones for the material and use only those. Sample
   them from vanilla if unsure.
3. **Never pure black or pure white.** Vanilla's darkest greys sit around `#2b2b2b`.
4. **Check it tiled and in-game early.** A texture that looks good alone often shows an
   obvious seam or a repeating blemish once it is a wall of blocks.

Tools
-----

- **Blockbench** — free, purpose-built for Minecraft, edits models *and* textures, previews
  the block in 3D as you paint. This is the one to learn. The MDK's `.gitignore` already
  excludes `.bbmodel` project files, so it expects you to use it.
- **Aseprite** (paid) or **LibreSprite** (free fork) — best pure pixel-art editors if you
  want to draw rather than model.
- **GIMP / Krita / Paint.NET** — fine, but turn off anti-aliasing on every tool, and zoom
  with nearest-neighbour, or you will get blurry half-tones that break the style.

For our miners
--------------

The miners are burner drills: Minecraft's furnace grammar (recessed firebox, one bright
plate seam, heavy pixel noise) carrying Factorio's machine silhouette (a tapering drill
bit and a warm trim band that wraps the chassis).

`make_miner_textures.py` draws all sixteen of them:

```
python texture-workshop/make_miner_textures.py            # write the PNGs
python texture-workshop/make_miner_textures.py --preview  # also write preview.png
```

It holds three 16x16 ASCII maps — front, side, top — and one five-tone palette per tier,
then renders every combination. That is the family rule made mechanical: edit a map and
all four tiers change together, so they cannot drift apart. Only the palettes differ, and
each is sampled off the vanilla block the tier is made of (`oak_planks`, `cobblestone`,
`iron_block`, `diamond_block`), extended one step darker where vanilla has no tone dark
enough to read as a recess.

Three things stay identical across all four tiers, and they are what make the set read as
one machine family:

- the **firebox**, cold at `#191919` and lit with vanilla `furnace_front_on`'s own two
  fire tones, `#ff8f00` and `#ffd800`
- the **trim band**, at the same row on the front and the side so the yellow wraps the
  corner instead of stopping at it
- the **drill bit**, a segmented cone with a highlight down its left edge

Each texture lands between 6 and 9 colours, which is cobblestone-to-furnace_front
territory. If a change pushes one past about 13, something has gone soft.

The 3D preview is worth trusting more than the flat one — check the trim band still lines
up across the front/side corner after any edit to those rows.

`reference/` holds vanilla textures extracted from the client jar for study. They are
Mojang's assets, so that folder is git-ignored and must not ship with the mod.
