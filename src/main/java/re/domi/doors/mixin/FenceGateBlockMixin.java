package re.domi.doors.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.domi.doors.BlockPosFloodFillIterator;
import re.domi.doors.Config;
import re.domi.doors.ConnectedDoors;
import re.domi.doors.ConnectedDoorsClient;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(FenceGateBlock.class)
public class FenceGateBlockMixin
{
    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void activateConnectedFenceGates(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir)
    {
        if (!Config.connectFenceGates
            || player.isSneaking()
            || world.isClient() && ConnectedDoorsClient.serverBlacklisted)
        {
            return;
        }

        boolean open = state.get(Properties.OPEN); // Attention: the original block state has already been updated at this point
        Direction facing = state.get(HorizontalFacingBlock.FACING);
        Direction.Axis axis = facing.getAxis();
        Block block = state.getBlock();

        new BlockPosFloodFillIterator().iterate(pos, Config.connectedFenceGateLimit, Config.connectedFenceGateLimit, new Queuer(axis), (blockPos, o) ->
        {
            BlockState checkState = world.getBlockState(blockPos);

            if (checkState.isOf(block) && checkState.get(HorizontalFacingBlock.FACING).getAxis() == axis && checkState.get(Properties.OPEN) != open)
            {
                if (!ConnectedDoors.serverModPresent)
                {
                    Vec3d adjustedHitPos = hit.getPos().add(blockPos.getX() - pos.getX(), blockPos.getY() - pos.getY(), blockPos.getZ() - pos.getZ());
                    BlockHitResult adjustedHitResult = new BlockHitResult(adjustedHitPos, hit.getSide(), blockPos, hit.isInsideBlock());

                    ConnectedDoorsClient.sendUsePacket(world, hand, adjustedHitResult);
                }
                else
                {
                    world.setBlockState(blockPos, checkState.with(HorizontalFacingBlock.FACING, facing).with(Properties.OPEN, open), Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
                }

                return true;
            }

            return false;
        }, null);
    }

    @Inject(method = "neighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"), cancellable = true)
    private void redstoneUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify, CallbackInfo ci)
    {
        if (!Config.connectFenceGates) return;

        boolean isPowered = world.isReceivingRedstonePower(pos);
        boolean open = state.get(Properties.OPEN);

        if (open == isPowered) return; // Only process updates where the "open" state changes

        Direction facing = state.get(HorizontalFacingBlock.FACING);
        Direction.Axis axis = facing.getAxis();
        Block block = state.getBlock();
        BlockPosFloodFillIterator iterator = new BlockPosFloodFillIterator();

        boolean isAnyConnectedBlockPowered = iterator.iterate(pos, Config.connectedFenceGateLimit, Config.connectedFenceGateLimit, new Queuer(axis), (blockPos, isAnyPowered) ->
        {
            BlockState checkState = world.getBlockState(blockPos);

            if (checkState.isOf(block) && checkState.get(HorizontalFacingBlock.FACING).getAxis() == axis && checkState.get(Properties.OPEN) == open)
            {
                if (world.isReceivingRedstonePower(blockPos))
                {
                    isAnyPowered.setTrue();
                }

                return true;
            }

            return false;
        }, new MutableBoolean(isPowered)).booleanValue();

        if (!isPowered && isAnyConnectedBlockPowered) // Cancel vanilla logic as a connected gate is still powered
        {
            ci.cancel();
            return;
        }

        BlockPos.Mutable p = new BlockPos.Mutable();

        for (long longPos : iterator.getAcceptedPositions())
        {
            p.set(longPos);
            world.setBlockState(p, world.getBlockState(p).with(Properties.OPEN, isAnyConnectedBlockPowered).with(HorizontalFacingBlock.FACING, facing));
        }
    }

    @SuppressWarnings("MixinInnerClass")
    private static class Queuer implements BiConsumer<BlockPos, Consumer<BlockPos>>
    {
        private final Direction.Axis axis;

        public Queuer(Direction.Axis axis)
        {
            this.axis = axis;
        }

        @Override
        public void accept(BlockPos blockPos, Consumer<BlockPos> blockPosConsumer)
        {
            blockPosConsumer.accept(blockPos.up());
            blockPosConsumer.accept(blockPos.down());

            if (this.axis == Direction.Axis.X)
            {
                blockPosConsumer.accept(blockPos.north());
                blockPosConsumer.accept(blockPos.south());
            }
            else
            {
                blockPosConsumer.accept(blockPos.west());
                blockPosConsumer.accept(blockPos.east());
            }
        }
    }
}
