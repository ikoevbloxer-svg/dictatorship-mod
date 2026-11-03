package ru.lavista.dictatorship.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.lavista.dictatorship.DictatorshipMod;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DictatorshipMod.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> WHIP_CRACK = SOUNDS.register("whip_crack",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DictatorshipMod.MODID, "whip_crack")));
    private ModSounds() {}
}
