1### The Mining Logic Matrix

Inside your `BlockEvent.BreakEvent`, intercept the block break and evaluate these four scenarios:

#### 1. Pristine Node + Silk Touch (The Fix)

* **The Condition:** `!SavedData.contains(pos)` AND `hasSilkTouch`.
* **The Action:** **Do absolutely nothing.** Do not cancel the event.
* **Why:** By ignoring the event, vanilla Minecraft takes over. The game will completely destroy the block, remove it from the world, and drop the pristine Ore block item. No `SavedData` is created, and no damaged block is left behind. The player has successfully moved the fresh node.

#### 2. Pristine Node + Normal Pickaxe (The First Hit)

* **The Condition:** `!SavedData.contains(pos)` AND `!hasSilkTouch`.
* **The Action:** Cancel the break event. Manually spawn the standard drops (raw ore). Add the `BlockPos` to your `SavedData` at Damage Stage 1.
* **Why:** This initiates the crumble mechanic. The player gets one raw ore, the block stays in the world, and it is now permanently tracked.

#### 3. Damaged Node + Silk Touch (The Crumble Lock)

* **The Condition:** `SavedData.contains(pos)` AND `hasSilkTouch`.
* **The Action:** Cancel the break event. Manually query the loot table with a spoofed "Iron Pickaxe" (no Silk Touch) and spawn the raw ore. Increment the damage stage in `SavedData`. Play the "clunk" sound and send the warning message.
* **Why:** The node is already fractured. They tried to Silk Touch it, but the Crumble Lock denies them, punishing them with raw ore and advancing the block's destruction.

#### 4. Damaged Node + Normal Pickaxe (Standard Manual Mining)

* **The Condition:** `SavedData.contains(pos)` AND `!hasSilkTouch`.
* **The Action:** Cancel the break event. Manually spawn the standard drops. Increment the damage stage in `SavedData`.
* **Why:** The player is brute-forcing the node block-by-block.

---

### Handling the Final Break

For Scenarios 2, 3, and 4, you are incrementing the damage. You just need one final check at the end of your custom logic:

```java
if (newDamageStage >= MAX_YIELD) {
    // The node is finally exhausted!
    level.destroyBlock(pos, false); // False because we already spawned our custom drops
    savedData.remove(pos);
}

```

By explicitly bailing out of your custom logic in Scenario 1, you let vanilla handle the relocation of fresh nodes, completely patching the dupe while perfectly maintaining your out-of-band architecture!


Some Notes:
**Memory Cleanup (Crucial)**
Since your progress map scales with mining in-flight, you must ensure you aren't leaving "ghost data" behind when the player breaks the block.

* In the break event, immediately `remove()` that `BlockPos` from your `SavedData` and mark the data as dirty so the server saves the removal.

---

### The Two Critical Edge Cases

Because your architecture maps progress strictly by `BlockPos`, the physical block and your virtual data are decoupled. You must guard against mechanics that move or break the block without player interaction.

* **The Piston Exploit:** If a player uses a piston to push a crumbling ore block one block to the side, the physical block is now at `X+1`, but your `SavedData` is still at `X`. The player has successfully reset the ore to a pristine state (and left an orphaned data point behind).
* *Fix:* Listen to the `PistonEvent.Pre` event. If the piston is attempting to push a block that exists in your `SavedData`, cancel the piston movement.


* **The Creeper Problem:** If an explosion destroys a crumbling ore block, the block disappears, but the break event you are listening to might not fire depending on how the modloader handles explosion drops.
* *Fix:* Listen to the `ExplosionEvent.Detonate` event. Check the list of affected blocks. If any of them exist in your `SavedData`, remove them from the map to prevent memory leaks.