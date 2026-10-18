package com.desyncguard.client.mixin;

import com.desyncguard.client.DesyncHud;
import com.desyncguard.client.DesyncStats;
import com.desyncguard.client.DesyncTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void desyncguard$checkSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.player == null) return;
        if (mc.screen instanceof CreativeModeInventoryScreen) return;

        int containerId = packet.getContainerId();
        if (containerId < 0 || !DesyncTracker.recentClickIn(containerId)) return;

        AbstractContainerMenu menu = containerId == 0 ? mc.player.inventoryMenu : mc.player.containerMenu;
        if (menu.containerId != containerId) return;
        int slot = packet.getSlot();
        if (slot < 0 || slot >= menu.slots.size()) return;

        if (!ItemStack.matches(menu.getSlot(slot).getItem(), packet.getItem())) {
            int key = DesyncTracker.slotKey(containerId, slot);
            DesyncTracker.flagSlotDesync(key);
            DesyncStats.recordInventoryDesync(key);
            DesyncHud.notifyInventoryDesync();
        }
    }

    @Inject(method = "handleContainerContent", at = @At("HEAD"))
    private void desyncguard$checkContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread() || mc.player == null) return;
        if (mc.screen instanceof CreativeModeInventoryScreen) return;

        int containerId = packet.getContainerId();
        if (!DesyncTracker.recentClickIn(containerId)) return;

        AbstractContainerMenu menu = containerId == 0 ? mc.player.inventoryMenu : mc.player.containerMenu;
        if (menu.containerId != containerId) return;

        List<ItemStack> items = packet.getItems();
        int n = Math.min(items.size(), menu.slots.size());
        boolean any = false;
        for (int i = 0; i < n; i++) {
            if (!ItemStack.matches(menu.getSlot(i).getItem(), items.get(i))) {
                int key = DesyncTracker.slotKey(containerId, i);
                DesyncTracker.flagSlotDesync(key);
                DesyncStats.recordInventoryDesync(key);
                any = true;
            }
        }
        if (any) DesyncHud.notifyInventoryDesync();
    }
}
