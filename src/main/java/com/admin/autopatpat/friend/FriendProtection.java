package com.admin.autopatpat.friend;

import com.admin.autopatpat.AutoPatPatClient;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public final class FriendProtection {
    private FriendProtection() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && !player.isSpectator() && entity instanceof PlayerEntity target
                    && AutoPatPatClient.getConfigManager().get().isProtectedFriend(target.getGameProfile().name())) {
                player.sendMessage(Text.translatable("message.autopatpat.friend_protected",
                        target.getGameProfile().name()), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
