package com.jaguarm.neoprogressiveautomation.world.crumble;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * How much is left in each ore block currently being crumbled.
 *
 * <p>Held per dimension rather than on the blocks themselves, because the whole point is
 * that this works on any ore, including ones from other mods. We cannot add a property or
 * a block entity to somebody else's block, but we can remember what we have taken out of
 * a position.
 *
 * <p>Only partially-worked ore is tracked. An untouched block has no entry, and an
 * exhausted one is removed, so the map stays proportional to how much mining is in flight
 * rather than to how much ore exists in the world.
 */
public class OreCrumbleState extends SavedData {

    private record Entry(BlockPos pos, int remaining) {}

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
            Codec.INT.fieldOf("remaining").forGetter(Entry::remaining)
    ).apply(instance, Entry::new));

    private static final Codec<OreCrumbleState> CODEC = ENTRY_CODEC.listOf().xmap(
            entries -> {
                OreCrumbleState state = new OreCrumbleState();
                entries.forEach(entry -> state.remaining.put(entry.pos(), entry.remaining()));
                return state;
            },
            state -> state.remaining.entrySet().stream()
                    .map(e -> new Entry(e.getKey(), e.getValue()))
                    .toList());

    public static final SavedDataType<OreCrumbleState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("neoprogressiveautomation", "ore_crumble"),
            OreCrumbleState::new,
            CODEC);

    private final Map<BlockPos, Integer> remaining = new HashMap<>();

    /** Harvests left at this position, or {@code fullValue} if it has not been touched. */
    public int remainingAt(BlockPos pos, int fullValue) {
        return remaining.getOrDefault(pos.immutable(), fullValue);
    }

    public void set(BlockPos pos, int value) {
        remaining.put(pos.immutable(), value);
        setDirty();
    }

    public void clear(BlockPos pos) {
        if (remaining.remove(pos.immutable()) != null) {
            setDirty();
        }
    }

    /** Forgets positions that are no longer the ore we were working, e.g. after a rebuild. */
    public void forgetIf(java.util.function.Predicate<BlockPos> stale) {
        if (remaining.keySet().removeIf(stale)) {
            setDirty();
        }
    }

    public List<BlockPos> trackedPositions() {
        return List.copyOf(remaining.keySet());
    }
}
