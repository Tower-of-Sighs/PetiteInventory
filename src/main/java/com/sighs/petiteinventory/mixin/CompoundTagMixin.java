package com.sighs.petiteinventory.mixin;

import com.sighs.petiteinventory.utils.ItemUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = CompoundTag.class)
public class CompoundTagMixin {
    @ModifyArg(method = "equals", at = @At(value = "INVOKE", target = "Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z"), index = 0)
    private Object remove0(Object a) {
        Map<String, Tag> newTags = new HashMap<>((Map<String, Tag>) a);
        newTags.remove(ItemUtils.ItemRotateHelper.TAG);
        return newTags;
    }

    @ModifyArg(method = "equals", at = @At(value = "INVOKE", target = "Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z"), index = 1)
    private Object remove1(Object a) {
        Map<String, Tag> newTags = new HashMap<>((Map<String, Tag>) a);
        newTags.remove(ItemUtils.ItemRotateHelper.TAG);
        return newTags;
    }
}
