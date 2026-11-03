package ru.lavista.dictatorship.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.lavista.dictatorship.DictatorshipMod;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DictatorshipMod.MODID);
    public static final DeferredHolder<Item, Item> ROYAL_WHIP = ITEMS.register("royal_whip",
            () -> new RoyalWhipItem(new Item.Properties().stacksTo(1).durability(256)));
    public static final DeferredHolder<Item, Item> ROYAL_CROWN = ITEMS.register("royal_crown",
            () -> new Item(new Item.Properties().stacksTo(1).equippable(EquipmentSlot.HEAD)));
    private ModItems() {}
}
