package re.domi.doors.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.domi.doors.ConnectedDoorsClient;
import re.domi.doors.config.EffectiveConfig;

import static net.minecraft.block.DoorBlock.*;

@Mixin(DoorBlock.class)
public class DoorBlockMixin extends Block
{
    public DoorBlockMixin(Settings settings)
    {
        super(settings);
    }

    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;cycle(Lnet/minecraft/state/property/Property;)Ljava/lang/Object;"))
    public void activateConnectedDoor(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir)
    {
        if (!EffectiveConfig.connectDoors || player.isSneaking())
        {
            return;
        }

        this.forConnectedDoor(state, world, pos, (connectedDoorPos, connectedDoorState, isOriginOpen) ->
        {
            world.setBlockState(connectedDoorPos, connectedDoorState.with(OPEN, !isOriginOpen), Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);

            if (!EffectiveConfig.serverModPresent)
            {
                Vec3d adjustedHitPos = hit.getPos().add(connectedDoorPos.getX() - pos.getX(), connectedDoorPos.getY() - pos.getY(), connectedDoorPos.getZ() - pos.getZ());
                BlockHitResult neighborHitResult = new BlockHitResult(adjustedHitPos, hit.getSide(), connectedDoorPos, hit.isInsideBlock());

                ConnectedDoorsClient.sendUsePacket(world, player.getActiveHand(), neighborHitResult);
            }

            return true;
        });
    }

    @Inject(method = "neighborUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify, CallbackInfo ci)
    {
        if (EffectiveConfig.connectDoors && !world.isClient())
        {
            this.forConnectedDoor(state, world, pos, (connectedDoorPos, connectedDoorState, isOriginOpen) ->
            {
                boolean receivingPower = world.isReceivingRedstonePower(pos);
                return world.setBlockState(connectedDoorPos, connectedDoorState.with(OPEN, receivingPower).with(POWERED, receivingPower), Block.NOTIFY_LISTENERS);
            });
        }
    }

    @ModifyVariable(method = "neighborUpdate", ordinal = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/DoorBlock;getDefaultState()Lnet/minecraft/block/BlockState;"))
    public boolean neighborUpdateReceivingPower(boolean orig, BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify)
    {
        if (!EffectiveConfig.connectDoors) return orig;

        return orig || this.forConnectedDoor(state, world, pos, (connectedDoorPos, connectedDoorState, isOriginOpen) ->
            world.isReceivingRedstonePower(connectedDoorPos) || world.isReceivingRedstonePower(connectedDoorPos.offset(connectedDoorState.get(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN)));
    }

    @Inject(method = "setOpen", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void connectForVillagers(@Nullable Entity entity, World world, BlockState state, BlockPos pos, boolean newOpen, CallbackInfo ci)
    {
        if (!EffectiveConfig.connectDoors) return;

        this.forConnectedDoor(state, world, pos, (connectedDoorPos, connectedDoorState, isOriginOpen) ->
            world.setBlockState(connectedDoorPos, connectedDoorState.with(OPEN, newOpen), NOTIFY_LISTENERS));
    }

    @Unique
    private boolean forConnectedDoor(BlockState state, World world, BlockPos pos, ForConnectedDoorFunc func)
    {
        Direction facing = state.get(FACING);
        DoorHinge hinge = state.get(HINGE);
        boolean open = state.get(OPEN);

        BlockPos neighborPos = pos.offset(hinge == DoorHinge.LEFT ? facing.rotateClockwise(Direction.Axis.Y) : facing.rotateCounterclockwise(Direction.Axis.Y));
        BlockState neighborState = world.getBlockState(neighborPos);

        if (neighborState.getBlock() == this
            && neighborState.get(FACING) == facing
            && neighborState.get(HINGE) != hinge
            && neighborState.get(HALF) == state.get(HALF)
            && neighborState.get(OPEN) == open)
        {
            return func.invoke(neighborPos, neighborState, open);
        }

        return false;
    }

    @SuppressWarnings("MixinInnerClass")
    private interface ForConnectedDoorFunc
    {
        /**
         * Callback that is run for a connected door half, if present.
         * Top and bottom half blocks will call this separately.
         *
         * @param connectedDoorPos   the {@link BlockPos} of the connected door half
         * @param connectedDoorState the {@link BlockState} of the connected door half
         * @param isOriginOpen       whether the origin door block is open
         *
         * @return a boolean value to be passed through the {@link DoorBlockMixin#forConnectedDoor} call
         */
        boolean invoke(BlockPos connectedDoorPos, BlockState connectedDoorState, boolean isOriginOpen);
    }
}
