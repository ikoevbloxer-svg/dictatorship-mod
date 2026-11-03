package ru.lavista.dictatorship;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ru.lavista.dictatorship.item.ModItems;
import ru.lavista.dictatorship.item.ModSounds;

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
    private static BlockPos palaceBase = BlockPos.ZERO;
    private static ServerLevel palaceLevel;
    private static BlockPos jailCell = BlockPos.ZERO;
    private static BlockPos jailRelease = BlockPos.ZERO;
    private static BlockPos shameSpot = BlockPos.ZERO;
    private static BlockPos hamsterWheel = BlockPos.ZERO;
    private static final Map<UUID, Integer> punishmentModes = new HashMap<>();
    private static final Map<UUID, Horse> patrols = new HashMap<>();
    private static final Map<UUID, List<Villager>> patrolOfficers = new HashMap<>();
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

    public DictatorshipMod(IEventBus modBus, ModContainer modContainer) {
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        NeoForge.EVENT_BUS.register(this);
    }

    private static boolean isRoyalTerritory(BlockPos pos, net.minecraft.world.level.LevelAccessor level) {
        return palaceLevel != null && level == palaceLevel && !palaceBase.equals(BlockPos.ZERO)
                && Math.abs(pos.getX() - palaceBase.getX()) <= 42
                && Math.abs(pos.getZ() - palaceBase.getZ()) <= 42
                && pos.getY() >= palaceBase.getY() && pos.getY() <= palaceBase.getY() + 20;
    }

    @SubscribeEvent
    public void protectRoyalPigs(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Pig pig)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (isWearingCrown(player)) return;

        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("§c§lКОРОЛЕВСКИЙ УКАЗ: §fсвиньи находятся под защитой короны. Причинение им вреда запрещено!"));
        if (!patrols.containsKey(player.getUUID()) && !jailTimers.containsKey(player.getUUID())
                && arrestCooldowns.getOrDefault(player.getUUID(), 0) <= 0) {
            dispatchPatrol(player);
        }
    }

    @SubscribeEvent
    public void protectRoyalGroundsFromExplosions(ExplosionEvent.Detonate event) {
        if (palaceLevel == null || event.getLevel() != palaceLevel) return;
        event.getAffectedBlocks().removeIf(pos -> isRoyalTerritory(pos, event.getLevel()));
    }

    @SubscribeEvent
    public void onRoyalPropertyPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || isWearingCrown(player) || !isRoyalTerritory(event.getPos(), event.getLevel())) return;
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("§4Строительство на королевской земле запрещено без лицензии Рахита III."));
        if (!patrols.containsKey(player.getUUID()) && !jailTimers.containsKey(player.getUUID())
                && arrestCooldowns.getOrDefault(player.getUUID(), 0) <= 0) dispatchPatrol(player);
    }

    @SubscribeEvent
    public void onRoyalPropertyBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || isWearingCrown(player)) return;
        if (!isRoyalTerritory(event.getPos(), event.getLevel())) return;
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("§4ОСКВЕРНЕНИЕ КОРОЛЕВСКОЙ СОБСТВЕННОСТИ! Офицеры уже в пути."));
        if (!patrols.containsKey(player.getUUID()) && !jailTimers.containsKey(player.getUUID())
                && arrestCooldowns.getOrDefault(player.getUUID(), 0) <= 0) dispatchPatrol(player);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        if (!palaceBuilt) {
            BlockPos spawn = level.getSharedSpawnPos();
            BlockPos kingdomOrigin = spawn.offset(56, 0, 0); // 56 blocks east of world spawn
            BlockPos marker = kingdomOrigin;
            jailCell = kingdomOrigin.offset(19, 1, 0);
            jailRelease = kingdomOrigin.offset(19, 1, 6);
            if (!level.getBlockState(marker).is(Blocks.GOLD_BLOCK)) buildPalace(level, kingdomOrigin);
            palaceBuilt = true;
            player.sendSystemMessage(Component.literal("§6§lКОРОЛЕВСТВО ОСНОВАНО! §r§eДобро пожаловать во владения короны."));
        }
        player.sendSystemMessage(Component.literal("§6Текущий закон: §f" + (currentLaw < 0 ? "Король пока молчит." : LAWS.get(currentLaw).title())));
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cleanupPatrol(player.getUUID());
            patrolAges.remove(player.getUUID());
        }
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Integer cooldown = arrestCooldowns.get(player.getUUID());
            if (cooldown != null && cooldown > 0) arrestCooldowns.put(player.getUUID(), cooldown - 1);
            if (isWearingCrown(player)) {
                if (patrols.containsKey(player.getUUID())) cleanupPatrol(player.getUUID());
                if (currentLaw >= 0) { /* Корона освобождает от всех указов. */ }
                continue;
            }
            if (currentLaw >= 0) applyLaw(player, currentLaw);
            boolean enteredRoyalChambers = isInRoyalChambers(player);
            if (!jailTimers.containsKey(player.getUUID()) && !patrols.containsKey(player.getUUID())
                    && (arrestCooldowns.getOrDefault(player.getUUID(), 0) <= 0)
                    && (enteredRoyalChambers || (currentLaw >= 0 && violatesLaw(player, currentLaw)))) {
                dispatchPatrol(player);
            }
        }
    }

    private static boolean isWearingCrown(ServerPlayer player) {
        return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.ROYAL_CROWN.get());
    }

    private static boolean isInRoyalChambers(ServerPlayer player) {
        if (palaceBase.equals(BlockPos.ZERO) || player.serverLevel() != palaceLevel) return false;
        BlockPos pos = player.blockPosition();
        return Math.abs(pos.getX() - palaceBase.getX()) <= 10
                && Math.abs(pos.getZ() - palaceBase.getZ()) <= 10
                && pos.getY() >= palaceBase.getY() + 1
                && pos.getY() <= palaceBase.getY() + 10;
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
        cleanupPatrol(player.getUUID());
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
        List<Villager> officers = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Villager officer = EntityType.VILLAGER.create(level);
            if (officer != null) {
                officer.moveTo(start.x, start.y, start.z, player.getYRot(), 0);
                officer.setCustomName(Component.literal(i == 0 ? "§cКоролевский офицер" : "§6Королевский конвоир"));
                officer.setCustomNameVisible(true);
                officer.setPersistenceRequired();
                level.addFreshEntity(officer);
                officer.startRiding(horse, true);
                officers.add(officer);
            }
        }
        patrols.put(player.getUUID(), horse);
        patrolOfficers.put(player.getUUID(), officers);
        patrolAges.put(player.getUUID(), 0);
        player.sendSystemMessage(Component.literal("§c§lВНИМАНИЕ! §fЗа вами выехала офицерская карета за нарушение королевского указа!"));
    }

    private static void tickPatrols(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Horse horse = patrols.get(player.getUUID());
            if (horse == null) continue;
            if (isWearingCrown(player)) { cleanupPatrol(player.getUUID()); patrolAges.remove(player.getUUID()); continue; }
            int age = patrolAges.getOrDefault(player.getUUID(), 0) + 1;
            patrolAges.put(player.getUUID(), age);
            if (!horse.isRemoved() && horse.level() == player.level()) {
                horse.getNavigation().moveTo(player, 1.65);
                if (horse.distanceToSqr(player) < 12.0 || age > 160) {
                    cleanupPatrol(player.getUUID());
                    patrolAges.remove(player.getUUID());
                    arrestPlayer(player);
                }
            } else if (age > 160 || horse.level() != player.level()) {
                cleanupPatrol(player.getUUID());
                patrolAges.remove(player.getUUID());
                arrestPlayer(player);
            }
        }
    }

    /** Removes every entity belonging to this player's escort, not only the horse. */
    private static void cleanupPatrol(UUID playerId) {
        List<Villager> officers = patrolOfficers.remove(playerId);
        if (officers != null) {
            for (Villager officer : officers) {
                if (officer != null && !officer.isRemoved()) officer.discard();
            }
        }
        Horse horse = patrols.remove(playerId);
        if (horse != null && !horse.isRemoved()) horse.discard();
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
            if (player != null && isWearingCrown(player)) {
                iterator.remove();
                punishmentModes.remove(player.getUUID());
                cleanupPatrol(player.getUUID());
                BlockPos exit = jailRelease.equals(BlockPos.ZERO) ? player.serverLevel().getSharedSpawnPos().offset(27, 1, 14) : jailRelease;
                player.connection.teleport(exit.getX() + 0.5, exit.getY(), exit.getZ() + 0.5, player.getYRot(), player.getXRot());
                player.sendSystemMessage(Component.literal("§6§lКОРОНА ПРИЗНАЛА ВАС МОНАРХОМ! §fВсе указы и наказания отменены."));
                continue;
            }
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
            Villager thrower = EntityType.VILLAGER.create(level);
            if (thrower == null) continue;
            thrower.moveTo(x, player.getY(), z, player.getYRot(), 0);
            level.addFreshEntity(thrower);
            net.minecraft.world.entity.projectile.ThrownEgg egg = new net.minecraft.world.entity.projectile.ThrownEgg(level, thrower);
            egg.setPos(x, player.getY() + 1.5, z);
            Vec3 direction = player.position().add(0, 0.5, 0).subtract(egg.position()).normalize();
            egg.shoot(direction.x, direction.y, direction.z, 0.85F, 3.0F);
            level.addFreshEntity(egg);
            thrower.discard();
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
        if (isWearingCrown(p)) return;
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
        // origin is the kingdom centre, deliberately placed 56 blocks from world spawn.
        BlockPos base = origin;
        palaceBase = base;
        palaceLevel = level;

        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState polished = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState dark = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        BlockState roof = Blocks.DARK_OAK_PLANKS.defaultBlockState();

        // Royal courtyard: paved central avenue, trimmed lawn and a proper perimeter wall.
        for (int x = -42; x <= 42; x++) for (int z = -42; z <= 42; z++) {
            level.setBlock(base.offset(x, 0, z),
                    (Math.abs(x) <= 2 || Math.abs(z) <= 2) ? polished : Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (int y = 1; y <= 14; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            if (Math.abs(x) == 42 || Math.abs(z) == 42) {
                level.setBlock(base.offset(x, 1, z), stone, 3);
                level.setBlock(base.offset(x, 2, z), Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);
                if ((x % 8 == 0 && Math.abs(z) == 42) || (z % 8 == 0 && Math.abs(x) == 42))
                    level.setBlock(base.offset(x, 3, z), Blocks.LANTERN.defaultBlockState(), 3);
            }
        }
        // Main gate with stone piers and gold caps.
        for (int x : new int[]{-3, 3}) for (int y = 1; y <= 4; y++)
            level.setBlock(base.offset(x, y, -42), y == 4 ? gold : stone, 3);
        level.setBlock(base.offset(0, 1, -42), Blocks.DARK_OAK_FENCE_GATE.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 2, -42), Blocks.DARK_OAK_FENCE_GATE.defaultBlockState(), 3);

        // Palace: broad symmetrical hall, tall corner towers, layered roof and grand staircase.
        int halfW = 13, halfD = 12, wallH = 9;
        for (int x = -halfW; x <= halfW; x++) for (int z = -halfD; z <= halfD; z++) {
            for (int y = 0; y <= 14; y++) {
                boolean edge = Math.abs(x) == halfW || Math.abs(z) == halfD;
                BlockState state = Blocks.AIR.defaultBlockState();
                if (y == 0) state = polished;
                else if (edge && y <= wallH) state = (y == 1 || y == 5 || y == 9) ? stone : Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
                else if (y == wallH + 1 && edge) state = Blocks.STONE_BRICK_SLAB.defaultBlockState();
                level.setBlock(base.offset(x, y, z), state, 3);
            }
        }
        // Grand entrance opening and stairs.
        for (int y = 1; y <= 4; y++) for (int x = -2; x <= 2; x++)
            level.setBlock(base.offset(x, y, -halfD), Blocks.AIR.defaultBlockState(), 3);
        for (int step = 1; step <= 5; step++) for (int x = -4; x <= 4; x++)
            level.setBlock(base.offset(x, 1, -halfD - step), Blocks.STONE_BRICK_STAIRS.defaultBlockState(), 3);
        // Tall windows with stone mullions.
        for (int x : new int[]{-9, -5, 5, 9}) for (int y = 3; y <= 7; y++) {
            level.setBlock(base.offset(x, y, -halfD), Blocks.GLASS_PANE.defaultBlockState(), 3);
            level.setBlock(base.offset(x, y, halfD), Blocks.GLASS_PANE.defaultBlockState(), 3);
        }
        for (int z : new int[]{-7, -3, 3, 7}) for (int y = 3; y <= 7; y++) {
            level.setBlock(base.offset(-halfW, y, z), Blocks.GLASS_PANE.defaultBlockState(), 3);
            level.setBlock(base.offset(halfW, y, z), Blocks.GLASS_PANE.defaultBlockState(), 3);
        }
        // Four corner towers with crenellations.
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            int cx = sx * 10, cz = sz * 9;
            for (int x = cx - 3; x <= cx + 3; x++) for (int z = cz - 3; z <= cz + 3; z++)
                for (int y = 1; y <= 13; y++) {
                    boolean edge = Math.abs(x - cx) == 3 || Math.abs(z - cz) == 3;
                    if (edge) level.setBlock(base.offset(x, y, z), y % 4 == 0 ? dark : stone, 3);
                    else if (y == 1) level.setBlock(base.offset(x, y, z), polished, 3);
                }
            for (int x = cx - 4; x <= cx + 4; x++) for (int z = cz - 4; z <= cz + 4; z++)
                if (Math.abs(x - cx) == 4 || Math.abs(z - cz) == 4)
                    level.setBlock(base.offset(x, 14, z), Blocks.STONE_BRICK_WALL.defaultBlockState(), 3);
            level.setBlock(base.offset(cx, 15, cz), gold, 3);
        }
        // Layered central roof, with a raised ridge and gold finials.
        for (int layer = 0; layer < 4; layer++) {
            int inset = layer * 2;
            int y = 10 + layer;
            for (int x = -halfW + inset; x <= halfW - inset; x++) for (int z = -halfD + inset; z <= halfD - inset; z++)
                if (Math.abs(x) == halfW - inset || Math.abs(z) == halfD - inset)
                    level.setBlock(base.offset(x, y, z), roof, 3);
        }
        for (int x = -7; x <= 7; x++) for (int z = -5; z <= 5; z++)
            level.setBlock(base.offset(x, 13, z), roof, 3);
        level.setBlock(base.offset(0, 14, 0), gold, 3);
        level.setBlock(base.offset(0, 15, 0), Blocks.LIGHTNING_ROD.defaultBlockState(), 3);
        // Throne room details.
        for (int x = -3; x <= 3; x++) for (int z = 5; z <= 8; z++) level.setBlock(base.offset(x, 1, z), gold, 3);
        level.setBlock(base.offset(0, 2, 6), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 3, 6), Blocks.DARK_OAK_STAIRS.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 1, -8), Blocks.LECTERN.defaultBlockState(), 3);
        for (int x : new int[]{-11, 11}) for (int z = -8; z <= 8; z += 4) {
            level.setBlock(base.offset(x, 1, z), Blocks.QUARTZ_PILLAR.defaultBlockState(), 3);
            level.setBlock(base.offset(x, 2, z), gold, 3);
        }

        // Shame platform: framed, roofed pavilion rather than a bare wooden slab.
        BlockPos stage = base.offset(0, 0, -25);
        for (int x = -6; x <= 6; x++) for (int z = -4; z <= 4; z++)
            level.setBlock(stage.offset(x, 0, z), x == -6 || x == 6 || z == -4 || z == 4 ? dark : roof, 3);
        for (int x : new int[]{-5, 5}) for (int z : new int[]{-3, 3})
            for (int y = 1; y <= 5; y++) level.setBlock(stage.offset(x, y, z), stone, 3);
        for (int x = -6; x <= 6; x++) for (int z = -4; z <= 4; z++)
            if (Math.abs(x) == 6 || Math.abs(z) == 4) level.setBlock(stage.offset(x, 6, z), roof, 3);
        level.setBlock(stage.offset(0, 1, 0), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);

        // Jail: fortified stone building with barred windows, cells, roof and watch corner.
        BlockPos jail = base.offset(24, 0, 2);
        jailCell = jail.offset(0, 1, 0);
        jailRelease = jail.offset(0, 1, 8);
        for (int x = -6; x <= 6; x++) for (int z = -8; z <= 8; z++) for (int y = 0; y <= 7; y++) {
            boolean edge = Math.abs(x) == 6 || Math.abs(z) == 8;
            BlockState state = y == 0 ? dark : edge ? stone : Blocks.AIR.defaultBlockState();
            if (y == 7 && edge) state = dark;
            level.setBlock(jail.offset(x, y, z), state, 3);
        }
        for (int x : new int[]{-3, 0, 3}) for (int y = 2; y <= 4; y++)
            level.setBlock(jail.offset(x, y, -8), Blocks.IRON_BARS.defaultBlockState(), 3);
        for (int z : new int[]{-4, 0, 4}) for (int y = 1; y <= 5; y++)
            level.setBlock(jail.offset(0, y, z), Blocks.IRON_BARS.defaultBlockState(), 3);
        level.setBlock(jail.offset(0, 1, -8), Blocks.IRON_DOOR.defaultBlockState(), 3);
        for (int x = -7; x <= 7; x++) for (int z = -9; z <= 9; z++)
            if (Math.abs(x) + Math.abs(z) <= 16) level.setBlock(jail.offset(x, 8, z), dark, 3);
        level.setBlock(jail.offset(0, 9, 0), Blocks.LANTERN.defaultBlockState(), 3);

        // Guard barracks: timber-and-stone lodge with a pitched roof.
        BlockPos barracks = base.offset(-24, 0, 2);
        for (int x = -6; x <= 6; x++) for (int z = -5; z <= 5; z++) for (int y = 0; y <= 6; y++) {
            boolean edge = Math.abs(x) == 6 || Math.abs(z) == 5;
            BlockState state = y == 0 ? polished : edge && y <= 4 ? (y == 1 ? stone : roof) : Blocks.AIR.defaultBlockState();
            level.setBlock(barracks.offset(x, y, z), state, 3);
        }
        for (int x = -5; x <= 5; x++) for (int z = -4; z <= 4; z++)
            if (Math.abs(x) + Math.abs(z) <= 8) level.setBlock(barracks.offset(x, 5, z), roof, 3);
        level.setBlock(barracks.offset(0, 1, -5), Blocks.DARK_OAK_DOOR.defaultBlockState(), 3);
        for (int x : new int[]{-4, 4}) for (int y = 2; y <= 3; y++)
            level.setBlock(barracks.offset(x, y, -5), Blocks.GLASS_PANE.defaultBlockState(), 3);

        // Monument on a stepped plinth, with a recognizable crowned silhouette.
        BlockPos statue = base.offset(0, 0, -36);
        for (int y = 0; y <= 2; y++) for (int x = -3 + y; x <= 3 - y; x++) for (int z = -3 + y; z <= 3 - y; z++)
            level.setBlock(statue.offset(x, y, z), y == 2 ? gold : dark, 3);
        for (int y = 3; y <= 7; y++) level.setBlock(statue.offset(0, y, 0), gold, 3);
        for (int x : new int[]{-1, 1}) level.setBlock(statue.offset(x, 6, 0), gold, 3);
        for (int y = 8; y <= 11; y++) level.setBlock(statue.offset(0, y, 0), gold, 3);
        for (int x = -2; x <= 2; x++) level.setBlock(statue.offset(x, 12, 0), gold, 3);
        for (int x : new int[]{-2, 2}) level.setBlock(statue.offset(x, 13, 0), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        level.setBlock(statue.offset(0, 13, 0), Blocks.GOLD_BLOCK.defaultBlockState(), 3);

        // Formal approach, lamps and garden beds.
        for (int z = -40; z <= -16; z++) for (int x = -2; x <= 2; x++)
            level.setBlock(base.offset(x, 0, z), polished, 3);
        for (int z : new int[]{-38, -28, -18, 12, 24, 36}) for (int x : new int[]{-7, 7}) {
            level.setBlock(base.offset(x, 1, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(base.offset(x, 2, z), Blocks.LANTERN.defaultBlockState(), 3);
        }
        for (int x : new int[]{-32, 32}) for (int z : new int[]{-24, -12, 12, 24}) {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
                level.setBlock(base.offset(x + dx, 0, z + dz), Blocks.MOSS_BLOCK.defaultBlockState(), 3);
            level.setBlock(base.offset(x, 1, z), Blocks.FLOWERING_AZALEA.defaultBlockState(), 3);
        }

        // Bank counter and wheel prop.
        BlockPos bank = base.offset(-24, 0, 14);
        for (int x = -3; x <= 3; x++) level.setBlock(bank.offset(x, 0, 0), polished, 3);
        level.setBlock(bank.offset(0, 1, 0), Blocks.LECTERN.defaultBlockState(), 3);
        BlockPos wheel = base.offset(0, 0, -31);
        for (int x = -2; x <= 2; x++) {
            level.setBlock(wheel.offset(x, 1, 0), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(wheel.offset(x, 2, 0), Blocks.OAK_FENCE.defaultBlockState(), 3);
        }
        level.setBlock(wheel, Blocks.HAY_BLOCK.defaultBlockState(), 3);
        level.setBlock(base, gold, 3); // persistent build marker
    }

}
