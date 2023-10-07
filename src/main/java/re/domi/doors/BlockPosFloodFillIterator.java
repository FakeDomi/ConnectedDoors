package re.domi.doors;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class BlockPosFloodFillIterator
{
    private final LongOpenHashSet acceptedPositions = new LongOpenHashSet();

    public <TState> TState iterate(BlockPos pos, int maxDepth, int maxIterations, BiConsumer<BlockPos, Consumer<BlockPos>> nextQueuer, BiPredicate<BlockPos, TState> callback, TState state)
    {
        ArrayDeque<Pair<BlockPos, Integer>> queue = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        int iterations = 0;

        visited.add(pos.asLong());
        nextQueuer.accept(pos, queuedPos -> queue.add(new Pair<>(queuedPos, 1)));

        while (!queue.isEmpty())
        {
            Pair<BlockPos, Integer> pair = queue.poll();
            BlockPos currentPos = pair.getLeft();
            int depth = pair.getRight();
            long longCurrentPos = currentPos.asLong();

            if (!visited.add(longCurrentPos) || !callback.test(currentPos, state)) continue;
            this.acceptedPositions.add(longCurrentPos);

            if (++iterations >= maxIterations) return state;
            if (depth >= maxDepth) continue;

            nextQueuer.accept(currentPos, queuedPos -> queue.add(new Pair<>(queuedPos, depth + 1)));
        }

        return state;
    }

    public LongOpenHashSet getAcceptedPositions()
    {
        return acceptedPositions;
    }
}
