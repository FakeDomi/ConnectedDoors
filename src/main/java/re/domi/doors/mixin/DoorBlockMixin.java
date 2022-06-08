package re.domi.doors.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import re.domi.doors.ConnectedDoors;

import static net.minecraft.block.DoorBlock.*;

@Mixin(DoorBlock.class)
public class DoorBlockMixin extends Block
{
    public DoorBlockMixin(Settings settings)
    {
        super(settings);
    }

    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;cycle(Lnet/minecraft/state/property/Property;)Ljava/lang/Object;"))
    public void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir)
    {
        this.forConnectedDoor(state, world, pos, (neighborPos, neighborState, open) ->
        {
            world.setBlockState(neighborPos, neighborState.with(OPEN, !open), Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);

            if (!ConnectedDoors.modPresent)
            {
                Vec3d adjustedHitPos = hit.getPos().add(neighborPos.getX() - pos.getX(), neighborPos.getY() - pos.getY(), neighborPos.getZ() - pos.getZ());
                BlockHitResult neighborHitResult = new BlockHitResult(adjustedHitPos, hit.getSide(), neighborPos, hit.isInsideBlock());

                sendUsePacket(world, hand, neighborHitResult, neighborPos);
            }

            return true;
        });
    }

    @Inject(method = "neighborUpdate", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci, boolean receivingPower)
    {
        if (!world.isClient())
        {
            this.forConnectedDoor(state, world, pos, (neighborPos, neighborState, open) ->
                world.setBlockState(neighborPos, neighborState.with(OPEN, receivingPower).with(POWERED, receivingPower), Block.NOTIFY_LISTENERS));
        }
    }

    @ModifyVariable(method = "neighborUpdate", ordinal = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/DoorBlock;getDefaultState()Lnet/minecraft/block/BlockState;"))
    public boolean neighborUpdateReceivingPower(boolean orig, BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify)
    {
        return orig || this.forConnectedDoor(state, world, pos, (neighborPos, neighborState, open) ->
            world.isReceivingRedstonePower(neighborPos) || world.isReceivingRedstonePower(neighborPos.offset(neighborState.get(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN)));
    }

    @Inject(method = "setOpen", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void setOpen(@Nullable Entity entity, World world, BlockState state, BlockPos pos, boolean newOpen, CallbackInfo ci)
    {
        this.forConnectedDoor(state, world, pos, (neighborPos, neighborState, open) ->
            world.setBlockState(neighborPos, neighborState.with(OPEN, newOpen), NOTIFY_LISTENERS));
    }

    @Unique
    private boolean forConnectedDoor(BlockState state, World world, BlockPos pos, ForConnectedDoorFunc func)
    {
        Direction facing = state.get(FACING);
        DoorHinge hinge = state.get(HINGE);
        boolean open = state.get(OPEN);

        BlockPos neighborPos = pos.offset(hinge == DoorHinge.LEFT ? facing.rotateClockwise(Direction.Axis.Y) : facing.rotateCounterclockwise(Direction.Axis.Y));
        BlockState neighborState = world.getBlockState(neighborPos);

        if (neighborState.getBlock() == this && neighborState.get(FACING) == facing && neighborState.get(HINGE) != hinge
            && neighborState.get(HALF) == state.get(HALF) && neighborState.get(OPEN) == open)
        {
            return func.invoke(neighborPos, neighborState, open);
        }

        return false;
    }

    @Unique
    @Environment(EnvType.CLIENT)
    private static void sendUsePacket(World world, Hand hand, BlockHitResult hit, BlockPos pos)
    {
        //noinspection ConstantConditions
        ((ClientPlayerInteractionManagerMixin)MinecraftClient.getInstance().interactionManager).callSendSequencedPacket((ClientWorld)world, i -> new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(hit.getPos(), hit.getSide(), pos, hit.isInsideBlock()), i));
    }

    private interface ForConnectedDoorFunc
    {
        boolean invoke(BlockPos neighborPos, BlockState neighborState, boolean open);
    }
}
