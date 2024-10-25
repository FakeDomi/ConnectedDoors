package re.domi.doors;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class BlockPosFloodFillIterator
{
    private final LongOpenHashSet acceptedPositions = new LongOpenHashSet();

    /**
     * Flood-fill iterate outwards from a starting {@link BlockPos}. After this method returns, a {@link Set} of {@code long}-encoded positions accepted by the callback can be obtained from {@link BlockPosFloodFillIterator#getAcceptedPositions()}.
     *
     * @param pos                  The starting {@link BlockPos}.
     * @param maxDepth             The maximum depth to visit - no further positions will be queued at this depth.
     * @param maxAcceptedPositions The maximum amount of accepted positions to accumulate. When this limit is reached, no further positions will be checked.
     * @param nextQueuer           User supplied logic for selecting the next positions to add to the queue.
     * @param callback             A callback that's run once per unique queued position.
     *                             The return value of this callback controls whether the position is "accepted", allowing further positions to be queued from this position and making it count towards the limit.
     * @param state                A state object that is available in the callback and will be returned from this method.
     * @param <TState>             Any type for the state object.
     *
     * @return The {@link TState} passed in, which may have been mutated by the callback.
     */
    public <TState> TState iterate(BlockPos pos, int maxDepth, int maxAcceptedPositions, BiConsumer<BlockPos, Consumer<BlockPos>> nextQueuer, BiPredicate<BlockPos, TState> callback, TState state)
    {
        ArrayDeque<Pair<BlockPos, Integer>> queue = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        LongOpenHashSet acceptedPositions = this.acceptedPositions;

        visited.add(pos.asLong());
        nextQueuer.accept(pos, queuedPos -> queue.add(new Pair<>(queuedPos, 1)));

        while (!queue.isEmpty())
        {
            Pair<BlockPos, Integer> pair = queue.poll();
            BlockPos currentPos = pair.getLeft();
            int depth = pair.getRight();
            long longCurrentPos = currentPos.asLong();

            if (!visited.add(longCurrentPos) || !callback.test(currentPos, state)) continue;
            acceptedPositions.add(longCurrentPos);

            if (acceptedPositions.size() >= maxAcceptedPositions) return state;
            if (depth >= maxDepth) continue;

            nextQueuer.accept(currentPos, queuedPos -> queue.add(new Pair<>(queuedPos, depth + 1)));
        }

        return state;
    }

    public LongOpenHashSet getAcceptedPositions()
    {
        return this.acceptedPositions;
    }
}
