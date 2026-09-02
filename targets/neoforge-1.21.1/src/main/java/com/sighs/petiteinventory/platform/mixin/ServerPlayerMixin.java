package com.sighs.petiteinventory.platform.mixin;

import com.sighs.petiteinventory.platform.IAbstractContainerMenu;
import com.sighs.petiteinventory.platform.IAbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "initMenu", at = @At("HEAD"))
    private void qqq(AbstractContainerMenu menu, CallbackInfo ci) {
        ((IAbstractContainerMenu) menu).setPlayer((ServerPlayer) (Object) this);
    }
}
