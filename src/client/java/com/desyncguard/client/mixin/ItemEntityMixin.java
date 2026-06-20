package com.desyncguard.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Unique
    private int desyncguard$ticksColliding = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void desyncguard$onTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity)(Object)this;

        if (!self.level().isClientSide) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (player.getBoundingBox().intersects(self.getBoundingBox())) {
            desyncguard$ticksColliding++;

            if (desyncguard$ticksColliding > 20) {
                self.setDeltaMovement(0, 0, 0);
            }
        } else {
            desyncguard$ticksColliding = 0;
        }
    }
}
