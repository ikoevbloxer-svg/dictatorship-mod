package ru.lavista.dictatorship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Random;

@Mod(DictatorshipMod.MODID)
public final class DictatorshipMod {
    public static final String MODID = "dictatorship";
    private static final int LAW_INTERVAL = 20 * 120;
    private static final Random RANDOM = new Random();
    private static int ticks = 0;
    private static int currentLaw = -1;
    private static boolean palaceBuilt = false;
    private static BlockPos jailCell = BlockPos.ZERO;
    private static BlockPos jailRelease = BlockPos.ZERO;
    private static BlockPos shameSpot = BlockPos.ZERO;
    private static BlockPos hamsterWheel = BlockPos.ZERO;
    private static final Map<UUID, Integer> punishmentModes = new HashMap<>();
    private static final Map<UUID, Horse> patrols = new HashMap<>();
    private static final Map<UUID, Integer> patrolAges = new HashMap<>();
    private static final Map<UUID, Integer> jailTimers = new HashMap<>();
    private static final Map<UUID, Integer> arrestCooldowns = new HashMap<>();
    private static int bankTicks = 0;

    private record Law(String title, String explanation) {}
    private static final List<Law> LAWS = List.of(
        new Law("ВОДА — ВРАГ КОРОНЫ", "Королевские учёные доказали, что вода опасна. Плавание временно запрещено!"),
        new Law("ЗАПРЕТ БЕГА", "Подданные слишком быстро перемещаются. Король требует ходить чинно и степенно."),
        new Law("ЗОЛОТОЙ ДРЕСС-КОД", "Каждый уважающий себя подданный обязан носить золотой шлем."),
        new Law("НОЧНОЙ КОМЕНДАНТСКИЙ ЧАС", "После заката выходить под открытое небо запрещено."),
        new Law("НАЛОГ НА КИРКИ", "Король считает добычу полезных ископаемых привилегией. Всем шахтёрам — усталость!"),
        new Law("ОБЯЗАТЕЛЬНАЯ ЗАРЯДКА", "Каждый подданный обязан подпрыгивать. Королевская медицина сказала!"),
        new Law("ЗАПРЕТ НА ЯБЛОКИ", "Придворный диетолог объявил яблоки угрозой национальной безопасности."),
        new Law("ЛУНА — ГОСУДАРСТВЕННАЯ ТАЙНА", "Смотреть на луну без письменного разрешения запрещено."),
        new Law("НАЛОГ НА СВЕТ", "Факелы принадлежат короне. За яркое освещение полагается слабость."),
        new Law("ЗАПРЕТ НА ПРЫЖКИ", "Король считает прыжки проявлением чрезмерной радости."),
        new Law("ОБЯЗАТЕЛЬНЫЙ ПОКЛОН", "Каждые несколько секунд подданные должны вспоминать о величии трона."),
        new Law("АЛМАЗЫ — СОБСТВЕННОСТЬ КОРОНЫ", "Найденные алмазы объявлены национальным достоянием."),
        new Law("ЗАПРЕТ НА РЫБАЛКУ", "Рыбы пожаловались королю на нарушение личных границ."),
        new Law("ПОДОЗРИТЕЛЬНЫЕ КУРЫ", "Куры признаны шпионами соседнего королевства. Не приближайтесь к ним."),
        new Law("НАЛОГ НА СОН", "Спать можно только после уплаты воображаемого налога."),
        new Law("ЗАПРЕТ НА КРАФТ", "Ремесленники слишком самостоятельны. Король временно не одобряет их деятельность."),
        new Law("ОБЯЗАТЕЛЬНЫЙ КРАСНЫЙ ЦВЕТ", "Красный — цвет короны. Остальные цвета подозрительны."),
        new Law("ЗАПРЕТ НА ДЕРЕВЬЯ", "Лес занимает слишком много королевской земли. Не стойте рядом с деревьями."),
        new Law("НАЛОГ НА ЕДУ", "Каждый приём пищи облагается королевской пошлиной."),
        new Law("ЗАПРЕТ НА КАМЕНЬ", "Камень портит королевский пейзаж. Шахтёры, остановитесь!"),
        new Law("ОБЯЗАТЕЛЬНАЯ УЛЫБКА", "Недовольное лицо считается оскорблением монарха."),
        new Law("ПОРТРЕТ В КАЖДОМ ДОМЕ", "В каждом доме должен быть символ королевской власти."),
        new Law("ЗАПРЕТ НА ЛЕСТНИЦЫ", "Лестницы способствуют социальному расслоению. Пользуйтесь рампами!"),
        new Law("КОРОЛЕВСКИЙ ЧАС ТИШИНЫ", "Прыгать и шуметь запрещено. Король отдыхает."),
        new Law("НАЛОГ НА ЖЕЛЕЗО", "Железо стратегически важно. Всё найденное железо учитывается казной."),
        new Law("ЗАПРЕТ НА ЦВЕТЫ", "Цветы отвлекают подданных от государственных задач."),
        new Law("ОБЯЗАТЕЛЬНЫЙ ПЕРЕХОД ПО МОСТАМ", "Переходить реки вброд — непатриотично."),
        new Law("КОРОЛЬ ВСЕГДА ПРАВ", "Любое действие короля автоматически считается законным."),
        new Law("ДЕНЬ БЕЗ ИНСТРУМЕНТОВ", "Настоящий подданный умеет работать руками. Инструменты под запретом."),
        new Law("ЗАПРЕТ НА СМЕХ", "Придворные сообщили, что народ слишком весёлый. Веселье приостановлено.")
    );

    public DictatorshipMod() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (!palaceBuilt) {
            BlockPos spawn = level.getSharedSpawnPos();
            BlockPos marker = spawn.offset(8, 0, 8);
            jailCell = spawn.offset(27, 1, 8);
            jailRelease = spawn.offset(27, 1, 14);
            if (!level.getBlockState(marker).is(Blocks.GOLD_BLOCK)) buildPalace(level, spawn);
            palaceBuilt = true;
            player.sendSystemMessage(Component.literal("§6§lКОРОЛЕВСТВО ОСНОВАНО! §r§eДобро пожаловать во владения короны."));
        }
        player.sendSystemMessage(Component.literal("§6Текущий закон: §f" + (currentLaw < 0 ? "Король пока молчит." : LAWS.get(currentLaw).title())));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ticks++;
        if (ticks >= LAW_INTERVAL) {
            ticks = 0;
            int next;
            do { next = RANDOM.nextInt(LAWS.size()); } while (LAWS.size() > 1 && next == currentLaw);
            currentLaw = next;
            Law law = LAWS.get(currentLaw);
            server.getPlayerList().broadcastSystemMessage(Component.literal("§4§l[КОРОЛЕВСКИЙ УКАЗ] §e" + law.title() + "§r\n§f" + law.explanation()), false);
            server.getPlayerList().broadcastSystemMessage(Component.literal("§7Следующий указ будет через 2 минуты."), false);
        }
        tickPatrols(server);
        tickJail(server);
        tickBank(server);
        if (currentLaw < 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Integer cooldown = arrestCooldowns.get(player.getUUID());
            if (cooldown != null && cooldown > 0) arrestCooldowns.put(player.getUUID(), cooldown - 1);
            applyLaw(player, currentLaw);
            if (!jailTimers.containsKey(player.getUUID()) && !patrols.containsKey(player.getUUID())
                    && (arrestCooldowns.getOrDefault(player.getUUID(), 0) <= 0) && violatesLaw(player, currentLaw)) {
                dispatchPatrol(player);
            }
        }
    }

    private static boolean violatesLaw(ServerPlayer p, int law) {
        return switch (law) {
            case 0 -> p.isInWater();
            case 1 -> p.isSprinting();
            case 2 -> !p.getInventory().armor.get(3).is(Items.GOLDEN_HELMET);
            case 3, 7 -> p.level().isNight() && p.level().canSeeSky(p.blockPosition());
            case 8 -> p.getMainHandItem().is(Items.TORCH);
            case 9 -> p.getDeltaMovement().y > 0.15;
            case 12 -> p.getMainHandItem().is(Items.FISHING_ROD);
            case 15 -> p.getMainHandItem().is(Items.CRAFTING_TABLE);
            case 16 -> !p.getInventory().contains(new ItemStack(Items.RED_DYE));
            case 22 -> p.level().getBlockState(p.blockPosition()).is(Blocks.LADDER);
            case 25 -> p.getMainHandItem().is(Items.POPPY);
            case 26 -> p.isInWater();
            case 28 -> p.getMainHandItem().getItem() instanceof net.minecraft.world.item.AxeItem
                    || p.getMainHandItem().getItem() instanceof net.minecraft.world.item.PickaxeItem;
            default -> false;
        };
    }

    private static void dispatchPatrol(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 away = player.position().subtract(level.getSharedSpawnPos().getCenter());
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        Vec3 start = player.position().add(away.normalize().scale(12));
        Horse horse = EntityType.HORSE.create(level);
        if (horse == null) return;
        horse.moveTo(start.x, start.y, start.z, player.getYRot(), 0);
        horse.setCustomName(Component.literal("§cОфицерская карета"));
        horse.setCustomNameVisible(true);
        horse.setPersistenceRequired();
        level.addFreshEntity(horse);
        for (int i = 0; i < 2; i++) {
            Villager officer = EntityType.VILLAGER.create(level);
            if (officer != null) {
                officer.moveTo(start.x, start.y, start.z, player.getYRot(), 0);
                officer.setCustomName(Component.literal(i == 0 ? "§cКоролевский офицер" : "§6Королевский конвоир"));
                officer.setCustomNameVisible(true);
                officer.setPersistenceRequired();
                level.addFreshEntity(officer);
                officer.startRiding(horse, true);
            }
        }
        patrols.put(player.getUUID(), horse);
        patrolAges.put(player.getUUID(), 0);
        player.sendSystemMessage(Component.literal("§c§lВНИМАНИЕ! §fЗа вами выехала офицерская карета за нарушение королевского указа!"));
    }

    private static void tickPatrols(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Horse horse = patrols.get(player.getUUID());
            if (horse == null) continue;
            int age = patrolAges.getOrDefault(player.getUUID(), 0) + 1;
            patrolAges.put(player.getUUID(), age);
            if (!horse.isRemoved() && horse.level() == player.level()) {
                horse.getNavigation().moveTo(player, 1.65);
                if (horse.distanceToSqr(player) < 12.0 || age > 160) {
                    horse.discard();
                    arrestPlayer(player);
                    patrols.remove(player.getUUID());
                    patrolAges.remove(player.getUUID());
                }
            } else if (age > 160) {
                patrols.remove(player.getUUID());
                patrolAges.remove(player.getUUID());
                arrestPlayer(player);
            }
        }
    }

    private static void arrestPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos cell = jailCell.equals(BlockPos.ZERO) ? level.getSharedSpawnPos().offset(27, 1, 8) : jailCell;
        int punishment = RANDOM.nextInt(4);
        if (punishment == 3) {
            player.sendSystemMessage(Component.literal("§4§lКОРОЛЕВСКИЙ ПРИГОВОР: КАЗНЬ! §fВаше дело закрыто решением монарха."));
            player.serverLevel().getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("§4Королевский суд вынес смертный приговор игроку §c" + player.getName().getString() + "§4."), false);
            arrestCooldowns.put(player.getUUID(), 20 * 60);
            player.kill();
            return;
        }
        int seconds;
        BlockPos destination;
        if (punishment == 1) {
            seconds = 30;
            destination = shameSpot.equals(BlockPos.ZERO) ? cell.offset(0, 0, -20) : shameSpot;
            punishmentModes.put(player.getUUID(), 1);
            player.sendSystemMessage(Component.literal("§6§lСЦЕНА ПОЗОРА! §f30 секунд публичного унижения и яиц от подданных."));
        } else if (punishment == 2) {
            seconds = 45;
            destination = hamsterWheel.equals(BlockPos.ZERO) ? cell.offset(0, 0, -27) : hamsterWheel;
            punishmentModes.put(player.getUUID(), 2);
            player.sendSystemMessage(Component.literal("§e§lКОЛЕСО ХОМЯКА! §f45 секунд королевской принудительной зарядки."));
        } else {
            seconds = 30 + RANDOM.nextInt(171);
            destination = cell;
            punishmentModes.put(player.getUUID(), 0);
            player.sendSystemMessage(Component.literal("§4§lАРЕСТОВАНО! §fКоролевский суд назначил вам " + seconds + " секунд заключения."));
        }
        player.connection.teleport(destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, player.getYRot(), player.getXRot());
        jailTimers.put(player.getUUID(), seconds * 20);
        arrestCooldowns.put(player.getUUID(), seconds * 20 + 200);
    }

    private static void tickJail(MinecraftServer server) {
        var iterator = jailTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            int left = entry.getValue() - 1;
            if (player != null) {
                int mode = punishmentModes.getOrDefault(player.getUUID(), 0);
                BlockPos target;
                if (mode == 1) {
                    target = shameSpot.equals(BlockPos.ZERO) ? player.serverLevel().getSharedSpawnPos().offset(8, 1, -19) : shameSpot;
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, true, false));
                    if (left % 100 == 0) throwEggsAt(player);
                } else if (mode == 2) {
                    target = hamsterWheel.equals(BlockPos.ZERO) ? player.serverLevel().getSharedSpawnPos().offset(8, 1, -26) : hamsterWheel;
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 2, true, false));
                    player.setDeltaMovement(0.22, player.getDeltaMovement().y, 0.0);
                    player.hurtMarked = true;
                } else {
                    target = jailCell.equals(BlockPos.ZERO) ? player.serverLevel().getSharedSpawnPos().offset(27, 1, 8) : jailCell;
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 8, true, false));
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 25, 128, true, false));
                }
                if (player.distanceToSqr(Vec3.atCenterOf(target)) > 16) {
                    player.connection.teleport(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
                }
                if (left <= 0) {
                    BlockPos exit = jailRelease.equals(BlockPos.ZERO) ? target.offset(0, 0, 6) : jailRelease;
                    player.connection.teleport(exit.getX() + 0.5, exit.getY(), exit.getZ() + 0.5, player.getYRot(), player.getXRot());
                    player.sendSystemMessage(Component.literal("§aКоролевское наказание окончено. Вы свободны."));
                    punishmentModes.remove(player.getUUID());
                }
            }
            if (left <= 0) iterator.remove(); else entry.setValue(left);
        }
    }

    private static void throwEggsAt(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 3; i++) {
            double x = player.getX() + (RANDOM.nextDouble() - 0.5) * 8;
            double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * 8;
            ItemEntity egg = new ItemEntity(level, x, player.getY() + 2.0, z, new ItemStack(Items.EGG));
            Vec3 direction = player.position().add(0, 0.5, 0).subtract(egg.position()).normalize();
            egg.setDeltaMovement(direction.scale(0.75).add(0, 0.15, 0));
            level.addFreshEntity(egg);
        }
    }

    private static void tickBank(MinecraftServer server) {
        bankTicks++;
        if (bankTicks < 20 * 60 * 5) return;
        bankTicks = 0;
        if (RANDOM.nextInt(100) < 30) {
            BankData data = BankData.get(server.overworld());
            data.balances.clear();
            data.setDirty();
            server.getPlayerList().broadcastSystemMessage(Component.literal("§4§lБАНК ЛОПНУЛ! §fКороль направил все вклады на изготовление новой короны. Вкладчики остались ни с чем."), false);
        } else {
            BankData data = BankData.get(server.overworld());
            data.balances.replaceAll((uuid, amount) -> amount + Math.max(1, amount / 4));
            data.setDirty();
            server.getPlayerList().broadcastSystemMessage(Component.literal("§a§lБАНК ВЫПЛАТИЛ ПРИБЫЛЬ! §fВсе вклады выросли на 25%. Используйте /bank withdraw."), false);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bank")
            .then(Commands.literal("balance").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                int balance = BankData.get(player.serverLevel().getServer().overworld()).balances.getOrDefault(player.getUUID(), 0);
                player.sendSystemMessage(Component.literal("§6Королевский банк: §e" + balance + " алмазов на счету."));
                return 1;
            }))
            .then(Commands.literal("invest").then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                if (player.getInventory().countItem(Items.DIAMOND) < amount) {
                    player.sendSystemMessage(Component.literal("§cНедостаточно алмазов для вклада."));
                    return 0;
                }
                removeDiamonds(player, amount);
                BankData data = BankData.get(player.serverLevel().getServer().overworld());
                data.balances.merge(player.getUUID(), amount, Integer::sum);
                data.setDirty();
                player.sendSystemMessage(Component.literal("§aБанк принял вклад: §e" + amount + " алмазов§a. Риск банкротства — 30% на каждом банковском цикле."));
                return 1;
            })))
            .then(Commands.literal("withdraw").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                BankData data = BankData.get(player.serverLevel().getServer().overworld());
                int amount = data.balances.getOrDefault(player.getUUID(), 0);
                if (amount <= 0) {
                    player.sendSystemMessage(Component.literal("§cНа вашем счету пусто."));
                    return 0;
                }
                int payout = Math.min(64, amount);
                if (!player.getInventory().add(new ItemStack(Items.DIAMOND, payout))) {
                    player.sendSystemMessage(Component.literal("§cОсвободите место в инвентаре."));
                    return 0;
                }
                if (amount == payout) data.balances.remove(player.getUUID());
                else data.balances.put(player.getUUID(), amount - payout);
                data.setDirty();
                player.sendSystemMessage(Component.literal("§aБанк выдал §e" + payout + " алмазов§a."));
                return 1;
            })));
    }

    private static void removeDiamonds(ServerPlayer player, int amount) {
        int left = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && left > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.DIAMOND)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
    }

    private static void applyLaw(ServerPlayer p, int law) {
        switch (law) {
            case 0 -> { if (p.isInWater()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, false)); }
            case 1 -> { if (p.isSprinting()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, true, false)); }
            case 2 -> { if (!p.getInventory().armor.get(3).is(net.minecraft.world.item.Items.GOLDEN_HELMET)) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false)); }
            case 3 -> { if (p.level().isNight() && p.level().canSeeSky(p.blockPosition())) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, true, false)); }
            case 4 -> p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false));
            case 5 -> p.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, true, false));
            case 6 -> { if (p.getMainHandItem().is(net.minecraft.world.item.Items.APPLE)) p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, true, false)); }
            case 7 -> { if (p.level().isNight() && p.level().canSeeSky(p.blockPosition())) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, true, false)); }
            case 8 -> { if (p.getMainHandItem().is(net.minecraft.world.item.Items.TORCH)) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false)); }
            case 9 -> { if (p.getDeltaMovement().y > 0.15) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, true, false)); }
            case 10 -> { if (p.isSprinting()) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, false)); }
            case 11 -> { if (p.getInventory().contains(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND))) p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false)); }
            case 12 -> { if (p.getMainHandItem().is(net.minecraft.world.item.Items.FISHING_ROD)) p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false)); }
            case 13 -> { if (p.distanceToSqr(p.blockPosition().getX(), p.blockPosition().getY(), p.blockPosition().getZ()) < 1) { /* decree is mostly theatrical */ } }
            case 14 -> { if (p.isSleeping()) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false)); }
            case 15 -> { if (p.getMainHandItem().is(net.minecraft.world.item.Items.CRAFTING_TABLE)) p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false)); }
            case 16 -> { if (!p.getInventory().contains(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE))) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false)); }
            case 17 -> { if (p.level().getBlockState(p.blockPosition().below()).is(Blocks.OAK_LOG) || p.level().getBlockState(p.blockPosition().below()).is(Blocks.BIRCH_LOG)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false)); }
            case 18 -> { if (p.getFoodData().getFoodLevel() > 16) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, false)); }
            case 19 -> { if (p.getMainHandItem().is(Blocks.STONE.asItem())) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false)); }
            case 20 -> { if (p.getHealth() < p.getMaxHealth()) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, false)); }
            case 21 -> { if (!p.getInventory().contains(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAINTING))) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, true, false)); }
            case 22 -> { if (p.level().getBlockState(p.blockPosition()).is(Blocks.LADDER)) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, false)); }
            case 23 -> { if (p.isSprinting()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, true, false)); }
            case 24 -> p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, true, false));
            case 25 -> { if (p.getMainHandItem().is(net.minecraft.world.item.Items.POPPY)) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false)); }
            case 26 -> { if (p.isInWater()) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, true, false)); }
            case 27 -> p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, true, false));
            case 28 -> { if (p.getMainHandItem().getItem() instanceof net.minecraft.world.item.AxeItem || p.getMainHandItem().getItem() instanceof net.minecraft.world.item.PickaxeItem) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, false)); }
            case 29 -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, true, false));
            default -> { }
        }
    }

    private static void buildPalace(ServerLevel level, BlockPos origin) {
        BlockPos base = origin.offset(8, 0, 8);
        jailCell = base.offset(19, 1, 0);
        jailRelease = base.offset(19, 1, 6);
        shameSpot = base.offset(0, 1, -19);
        hamsterWheel = base.offset(0, 1, -26);
        int w = 25, d = 25, h = 11;
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        // Clear and build a compact, walk-in royal hall with corner towers.
        for (int x = -w/2; x <= w/2; x++) for (int z = -d/2; z <= d/2; z++) {
            for (int y = 0; y <= h; y++) {
                boolean edge = Math.abs(x) == w/2 || Math.abs(z) == d/2;
                BlockState state = Blocks.AIR.defaultBlockState();
                if (y == 0) state = floor;
                else if (edge && y < h) state = (y % 4 == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : stone);
                else if (y == h && edge) state = Blocks.STONE_BRICK_SLAB.defaultBlockState();
                if (edge && y >= 2 && y <= 4 && x == 0 && z == -d/2) state = Blocks.AIR.defaultBlockState();
                level.setBlock(base.offset(x, y, z), state, 3);
            }
        }
        // Windows, throne, dais, and banners.
        for (int x : new int[]{-8, -4, 4, 8}) for (int y = 4; y <= 6; y++) {
            level.setBlock(base.offset(x, y, -d/2), Blocks.GLASS_PANE.defaultBlockState(), 3);
            level.setBlock(base.offset(x, y, d/2), Blocks.GLASS_PANE.defaultBlockState(), 3);
        }
        for (int x = -2; x <= 2; x++) for (int z = 5; z <= 8; z++)
            level.setBlock(base.offset(x, 1, z), gold, 3);
        level.setBlock(base.offset(0, 2, 6), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 3, 6), Blocks.DARK_OAK_STAIRS.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 1, -9), Blocks.LECTERN.defaultBlockState(), 3);
        // Shame stage in front of the palace.
        BlockPos stage = base.offset(0, 0, -19);
        for (int x = -4; x <= 4; x++) for (int z = -2; z <= 2; z++)
            level.setBlock(stage.offset(x, 0, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
        for (int x : new int[]{-4, 4}) for (int z : new int[]{-2, 2})
            level.setBlock(stage.offset(x, 1, z), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
        // Small jail wing to the east.
        BlockPos jail = base.offset(19, 0, 0);
        for (int x = -3; x <= 3; x++) for (int z = -4; z <= 4; z++) for (int y = 0; y <= 4; y++) {
            boolean edge = Math.abs(x) == 3 || Math.abs(z) == 4;
            BlockState s = y == 0 ? Blocks.STONE_BRICKS.defaultBlockState() :
                edge ? Blocks.IRON_BARS.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (y == 4 && edge) s = Blocks.STONE_BRICKS.defaultBlockState();
            level.setBlock(jail.offset(x, y, z), s, 3);
        }
        level.setBlock(jail.offset(0, 1, 0), Blocks.CHAIN.defaultBlockState(), 3);
        level.setBlock(jail.offset(0, 1, -4), Blocks.IRON_DOOR.defaultBlockState(), 3);
        // Guard barracks to the west.
        BlockPos barracks = base.offset(-19, 0, 0);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = 0; y <= 4; y++) {
            boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
            level.setBlock(barracks.offset(x, y, z), y == 0 ? floor : edge ? stone : Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(barracks.offset(0, 1, -3), Blocks.OAK_DOOR.defaultBlockState(), 3);
        level.setBlock(base, Blocks.GOLD_BLOCK.defaultBlockState(), 3); // persistent build marker
        // Visible bank counter and wheel prop.
        BlockPos bank = base.offset(-19, 0, 10);
        level.setBlock(bank, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        level.setBlock(bank.above(), Blocks.LECTERN.defaultBlockState(), 3);
        BlockPos wheel = base.offset(0, 0, -26);
        for (int x = -2; x <= 2; x++) {
            level.setBlock(wheel.offset(x, 1, 0), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(wheel.offset(x, 2, 0), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
        level.setBlock(wheel.offset(0, 0, 0), Blocks.HAY_BLOCK.defaultBlockState(), 3);
    }
}
