package necek.development.autodrip.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MainClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if(hand != Hand.MAIN_HAND) return ActionResult.PASS;
            MinecraftClient mc = MinecraftClient.getInstance();
            BlockPos blockPos = hitResult.getBlockPos();
            BlockState blockState = world.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if(block instanceof TrapdoorBlock && player.getMainHandStack().isOf(Items.POINTED_DRIPSTONE)) {
                if(blockState.get(TrapdoorBlock.OPEN)) return ActionResult.PASS;
                BlockPos down = blockPos.down();
                if(world.getBlockState(down).isAir()) {
                    place(mc, down);
                }
            }
            return ActionResult.PASS;
        });
    }

    private void place(MinecraftClient mc, BlockPos pos) {
        Vec3d hitPos = Vec3d.ofCenter(pos);
        BlockPos neighbour;
        Direction side = getPlaceSide(mc, pos);

        if (side == null) {
            side = Direction.UP;
            neighbour = pos;
        } else {
            neighbour = pos.offset(side);
            hitPos = hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        }

        BlockHitResult blockHitResult = new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);
        interact(mc, blockHitResult, Hand.MAIN_HAND, true);
    }

    private boolean canPlaceBlock(MinecraftClient mc, BlockPos blockPos, boolean checkEntities) {
        if (blockPos == null) return false;
        if (!World.isValid(blockPos)) return false;
        return !checkEntities || mc.world.canPlace(Blocks.POINTED_DRIPSTONE.getDefaultState(), blockPos, ShapeContext.absent());
    }

    private void interact(MinecraftClient mc, BlockHitResult blockHitResult, Hand hand, boolean swing) {
        boolean wasSneaking = mc.player.isSneaking();
        mc.player.setSneaking(false);

        ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);

        if (result.isAccepted()) {
            if (swing) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }

        mc.player.setSneaking(wasSneaking);
    }

    private Direction getPlaceSide(MinecraftClient mc, BlockPos blockPos) {
        Vec3d lookVec = blockPos.toCenterPos().subtract(mc.player.getEyePos());
        double bestRelevancy = -Double.MAX_VALUE;
        Direction bestSide = null;

        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            BlockState state = mc.world.getBlockState(neighbor);

            if (state.isAir() || isClickable(state.getBlock())) continue;
            if (!state.getFluidState().isEmpty()) continue;

            double relevancy = side.getAxis().choose(lookVec.getX(), lookVec.getY(), lookVec.getZ()) * side.getDirection().offset();
            if (relevancy > bestRelevancy) {
                bestRelevancy = relevancy;
                bestSide = side;
            }
        }

        return bestSide;
    }

    private boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock
                || block instanceof ButtonBlock
                || block instanceof BlockWithEntity
                || block instanceof DoorBlock
                || block instanceof NoteBlock
                || block instanceof TrapdoorBlock
                || block instanceof FenceGateBlock;
    }
}