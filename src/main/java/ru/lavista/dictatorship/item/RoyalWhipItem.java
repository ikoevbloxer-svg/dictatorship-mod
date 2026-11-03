package ru.lavista.dictatorship.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import ru.lavista.dictatorship.DictatorshipMod;

public class RoyalWhipItem extends Item {
    public RoyalWhipItem(Properties properties) {
        super(properties);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() != this || !(event.getTarget() instanceof LivingEntity target)) return;
        event.setCanceled(true);
        if (player.level().isClientSide()) return;
        target.hurt(player.damageSources().playerAttack(player), 4.0F);
        player.level().playSound(null, target.blockPosition(), ModSounds.WHIP_CRACK.get(),
                SoundSource.PLAYERS, 1.35F, 0.92F + player.getRandom().nextFloat() * 0.16F);
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }
}
