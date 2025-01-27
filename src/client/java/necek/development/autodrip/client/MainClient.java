package necek.development.autodrip.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class MainClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand == Hand.MAIN_HAND) {
                BlockState placedBlock = world.getBlockState(hitResult.getBlockPos());

                if (placedBlock.getBlock() instanceof PointedDripstoneBlock) {
                    BlockPos upPos = hitResult.getBlockPos().up();
                    BlockState upState = world.getBlockState(upPos);

                    if (upState.getBlock() instanceof TrapdoorBlock) {
                        BlockHitResult trapdoorHit = new BlockHitResult(
                                new Vec3d(upPos.getX() + 0.5, upPos.getY() + 0.5, upPos.getZ() + 0.5),
                                Direction.UP,
                                upPos,
                                false
                        );

                        MinecraftClient.getInstance().execute(() -> {
                            ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
                            if (clientPlayer != null) {
                                MinecraftClient.getInstance().interactionManager.interactBlock(
                                        clientPlayer,
                                        Hand.MAIN_HAND,
                                        trapdoorHit
                                );
                            }
                        });

                        MinecraftClient.getInstance().execute(() -> {

                                ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
                                if (clientPlayer != null) {
                                    MinecraftClient.getInstance().interactionManager.interactBlock(
                                            clientPlayer,
                                            Hand.MAIN_HAND,
                                            trapdoorHit
                                    );
                                }
                        });
                    }
                }
            }
            return ActionResult.PASS;
        });
    }
}