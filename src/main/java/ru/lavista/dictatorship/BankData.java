package ru.lavista.dictatorship;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent per-world balances for the royal bank. */
public final class BankData extends SavedData {
    private static final String DATA_NAME = "dictatorship_bank";
    private static final Factory<BankData> FACTORY = new Factory<>(BankData::new, BankData::load, DataFixTypes.LEVEL);

    final Map<UUID, Integer> balances = new HashMap<>();

    public static BankData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static BankData load(CompoundTag tag, HolderLookup.Provider registries) {
        BankData data = new BankData();
        ListTag entries = tag.getList("accounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            try {
                UUID player = UUID.fromString(entry.getString("player"));
                int amount = Math.max(0, entry.getInt("amount"));
                if (amount > 0) data.balances.put(player, amount);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed account entries rather than failing world loading.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        balances.forEach((uuid, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("player", uuid.toString());
            entry.putInt("amount", amount);
            entries.add(entry);
        });
        tag.put("accounts", entries);
        return tag;
    }
}
