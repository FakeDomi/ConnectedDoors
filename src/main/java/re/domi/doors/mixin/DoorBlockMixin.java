package re.domi.doors.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        if (ConnectedDoors.modPresent)
        {
            this.updateConnectedDoor(state, world, pos, (neighborPos, neighborState, open) ->
                world.setBlockState(neighborPos, neighborState.with(OPEN, !open), Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD));
        }
        else
        {
            this.updateConnectedDoor(state, world, pos, (neighborPos, neighborState, open) ->
                {
                    world.setBlockState(neighborPos, neighborState.with(OPEN, !open), Block.NOTIFY_LISTENERS | Block.REDRAW_ON_MAIN_THREAD);
                    sendUsePacket(hand, hit, neighborPos);
                });
        }
    }

    @Inject(method = "neighborUpdate", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci, boolean receivingPower)
    {
        if (!world.isClient())
        {
            this.updateConnectedDoor(state, world, pos, (neighborPos, neighborState, open) ->
                world.setBlockState(neighborPos, neighborState.with(OPEN, receivingPower), Block.NOTIFY_LISTENERS));
        }
    }

    @Unique
    private void updateConnectedDoor(BlockState state, World world, BlockPos pos, UpdateDoorCallback callback)
    {
        Direction facing = state.get(FACING);
        DoorHinge hinge = state.get(HINGE);
        boolean open = state.get(OPEN); // this value hasn't been updated yet

        BlockPos neighborPos = pos.offset(hinge == DoorHinge.LEFT ? facing.rotateClockwise(Direction.Axis.Y) : facing.rotateCounterclockwise(Direction.Axis.Y));
        BlockState neighborState = world.getBlockState(neighborPos);

        if (neighborState.getBlock() == this && neighborState.get(FACING) == facing && neighborState.get(HINGE) != hinge
            && neighborState.get(HALF) == state.get(HALF) && neighborState.get(OPEN) == open)
        {
            callback.run(neighborPos, neighborState, open);
        }
    }

    @Unique
    @Environment(EnvType.CLIENT)
    private static void sendUsePacket(Hand hand, BlockHitResult hit, BlockPos pos)
    {
        //noinspection ConstantConditions
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(hit.getPos(), hit.getSide(), pos, hit.isInsideBlock())));
    }

    private interface UpdateDoorCallback
    {
        void run(BlockPos neighborPos, BlockState neighborState, boolean open);
    }
}
