package com.sighs.petiteinventory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sighs.petiteinventory.loader.EntryCache;
import com.sighs.petiteinventory.loader.EntryLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.nio.file.Files;  // ← 新增
import java.nio.file.Path;   // ← 新增
import java.nio.file.Paths;  // ← 新增

public class SizeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("petitesize")
                        .requires(src -> src.hasPermission(2))
                        /* ========== set ========== */
                        .then(
                                Commands.literal("set")
                                        .then(
                                                Commands.argument("width", IntegerArgumentType.integer(1, 9))
                                                        .then(
                                                                Commands.argument("height", IntegerArgumentType.integer(1, 9))
                                                                        .executes(ctx -> {
                                                                            // 手持物品模式: /petitesize set 2 2
                                                                            ServerPlayer player = ctx.getSource().getPlayer();
                                                                            if (player == null) {
                                                                                ctx.getSource().sendFailure(
                                                                                        Component.literal("❌ 必须是玩家才能使用手持物品模式！"));
                                                                                return 0;
                                                                            }
                                                                            ItemStack held = player.getMainHandItem();
                                                                            if (held.isEmpty()) {
                                                                                ctx.getSource().sendFailure(
                                                                                        Component.literal("❌ 你必须手持一个物品！"));
                                                                                return 0;
                                                                            }

                                                                            String itemId = ForgeRegistries.ITEMS
                                                                                    .getKey(held.getItem()).toString();
                                                                            int width = IntegerArgumentType.getInteger(ctx, "width");
                                                                            int height = IntegerArgumentType.getInteger(ctx, "height");
                                                                            String size = width + "*" + height;

                                                                            // 生成NBT精确键（如果适用）
                                                                            String configKey = itemId;
                                                                            if (itemId.equals("tacz:modern_kinetic_gun") && held.hasTag()) {
                                                                                String gunId = held.getTag().getString("GunId");
                                                                                if (gunId != null && !gunId.isEmpty()) {
                                                                                    configKey = itemId + "{GunId:\"" + gunId + "\"}";
                                                                                }
                                                                            }

                                                                            // 使用新的缓存方法（即时生效+持久化）
                                                                            EntryCache.setSizeByCommand(configKey, size);

                                                                            String finalConfigKey = configKey;
                                                                            ctx.getSource().sendSuccess(() ->
                                                                                            Component.literal("✅ 已设置 ")
                                                                                                    .append(Component.literal(finalConfigKey)
                                                                                                            .withStyle(ChatFormatting.YELLOW))
                                                                                                    .append(Component.literal(" 的尺寸为 "))
                                                                                                    .append(Component.literal(size)
                                                                                                            .withStyle(ChatFormatting.AQUA))
                                                                                                    .append(Component.literal(" （已保存）")),
                                                                                    true);
                                                                            return 1;
                                                                        })
                                                                        .then(
                                                                                Commands.argument("item", StringArgumentType.string())
                                                                                        .suggests((ctx, builder) -> {
                                                                                            ForgeRegistries.ITEMS.getKeys()
                                                                                                    .forEach(key -> builder.suggest(key.toString()));
                                                                                            return builder.buildFuture();
                                                                                        })
                                                                                        .executes(ctx -> {
                                                                                            // 指定物品ID模式: /petitesize set 2 2 minecraft:bed
                                                                                            String itemId = StringArgumentType.getString(ctx, "item");
                                                                                            int width = IntegerArgumentType.getInteger(ctx, "width");
                                                                                            int height = IntegerArgumentType.getInteger(ctx, "height");
                                                                                            String size = width + "*" + height;

                                                                                            EntryCache.setSizeByCommand(itemId, size);

                                                                                            ctx.getSource().sendSuccess(() ->
                                                                                                            Component.literal("✅ 已设置 ")
                                                                                                                    .append(Component.literal(itemId)
                                                                                                                            .withStyle(ChatFormatting.YELLOW))
                                                                                                                    .append(Component.literal(" 的尺寸为 "))
                                                                                                                    .append(Component.literal(size)
                                                                                                                            .withStyle(ChatFormatting.AQUA))
                                                                                                                    .append(Component.literal(" （已保存）")),
                                                                                                    true);
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                        )
                        )
                        /* ========== get ========== */
                        .then(
                                Commands.literal("get")
                                        .executes(ctx -> {
                                            // 手持物品模式
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("❌ 必须是玩家才能使用手持物品模式！"));
                                                return 0;
                                            }
                                            ItemStack held = player.getMainHandItem();
                                            if (held.isEmpty()) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("❌ 你必须手持一个物品！"));
                                                return 0;
                                            }

                                            String itemId = ForgeRegistries.ITEMS
                                                    .getKey(held.getItem()).toString();

                                            // 使用支持NBT的匹配方法
                                            String size = EntryCache.matchItem(itemId, held);
                                            if (size == null) {
                                                ctx.getSource().sendSuccess(() ->
                                                                Component.literal("📋 物品 ")
                                                                        .append(Component.literal(itemId)
                                                                                .withStyle(ChatFormatting.YELLOW))
                                                                        .append(Component.literal(" 未设置自定义尺寸，使用默认 1×1")),
                                                        false);
                                            } else {
                                                ctx.getSource().sendSuccess(() ->
                                                                Component.literal("📋 物品 ")
                                                                        .append(Component.literal(itemId)
                                                                                .withStyle(ChatFormatting.YELLOW))
                                                                        .append(Component.literal(" 的尺寸是: "))
                                                                        .append(Component.literal(size)
                                                                                .withStyle(ChatFormatting.AQUA)),
                                                        false);
                                            }
                                            return 1;
                                        })
                                        .then(
                                                Commands.argument("item", StringArgumentType.string())
                                                        .suggests((ctx, builder) -> {
                                                            EntryCache.UnitMapCache.keySet().stream()
                                                                    .sorted()
                                                                    .forEach(builder::suggest);
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ctx -> {
                                                            // 指定物品ID模式
                                                            String itemId = StringArgumentType.getString(ctx, "item");
                                                            String size = EntryCache.matchItem(itemId);
                                                            if (size == null) {
                                                                ctx.getSource().sendSuccess(() ->
                                                                                Component.literal("📋 物品 ")
                                                                                        .append(Component.literal(itemId)
                                                                                                .withStyle(ChatFormatting.YELLOW))
                                                                                        .append(Component.literal(" 未设置自定义尺寸，使用默认 1×1")),
                                                                        false);
                                                            } else {
                                                                ctx.getSource().sendSuccess(() ->
                                                                                Component.literal("📋 物品 ")
                                                                                        .append(Component.literal(itemId)
                                                                                                .withStyle(ChatFormatting.YELLOW))
                                                                                        .append(Component.literal(" 的尺寸是: "))
                                                                                        .append(Component.literal(size)
                                                                                                .withStyle(ChatFormatting.AQUA)),
                                                                        false);
                                                            }
                                                            return 1;
                                                        })
                                        )
                        )
                        /* ========== list ========== */
                        .then(
                                Commands.literal("list")
                                        .executes(ctx -> {
                                            if (EntryCache.UnitMapCache.isEmpty() && EntryCache.NBTMapCache.isEmpty()) {
                                                ctx.getSource().sendSuccess(() ->
                                                                Component.literal("📋 当前没有自定义尺寸配置")
                                                                        .withStyle(ChatFormatting.GRAY),
                                                        false);
                                                return 1;
                                            }

                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("=== 自定义尺寸配置 ===")
                                                                    .withStyle(ChatFormatting.GOLD),
                                                    false);

                                            // 显示NBT精确匹配（优先级最高）
                                            if (!EntryCache.NBTMapCache.isEmpty()) {
                                                ctx.getSource().sendSuccess(() ->
                                                                Component.literal("🔧 NBT精确匹配:")
                                                                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                                                        false);
                                                EntryCache.NBTMapCache.forEach((item, size) -> {
                                                    Component line = Component.literal("  " + item + " → ")
                                                            .withStyle(ChatFormatting.YELLOW)
                                                            .append(Component.literal(size)
                                                                    .withStyle(ChatFormatting.AQUA));
                                                    ctx.getSource().sendSuccess(() -> line, false);
                                                });
                                            }

                                            // 按尺寸分组显示普通物品
                                            Map<String, List<String>> sizeGroups = new HashMap<>();
                                            EntryCache.UnitMapCache.forEach((item, size) -> {
                                                sizeGroups.computeIfAbsent(size, k -> new ArrayList<>()).add(item);
                                            });

                                            if (!sizeGroups.isEmpty()) {
                                                ctx.getSource().sendSuccess(() ->
                                                                Component.literal("📦 普通物品:")
                                                                        .withStyle(ChatFormatting.GREEN),
                                                        false);
                                                sizeGroups.forEach((size, items) -> {
                                                    Component line = Component.literal("  📏 " + size + ": ")
                                                            .withStyle(ChatFormatting.AQUA)
                                                            .append(Component.literal(String.join(", ", items))
                                                                    .withStyle(ChatFormatting.YELLOW));
                                                    ctx.getSource().sendSuccess(() -> line, false);
                                                });
                                            }

                                            return 1;
                                        })
                        )
/* ========== reset ========== */
                        .then(
                                Commands.literal("reset")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("❌ 必须是玩家才能使用手持物品模式！"));
                                                return 0;
                                            }
                                            ItemStack held = player.getMainHandItem();
                                            if (held.isEmpty()) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("❌ 你必须手持一个物品！"));
                                                return 0;
                                            }

                                            String itemId = ForgeRegistries.ITEMS
                                                    .getKey(held.getItem()).toString();

                                            // 生成NBT精确键（如果适用）
                                            String configKey = itemId;
                                            if (itemId.equals("tacz:modern_kinetic_gun") && held.hasTag()) {
                                                String gunId = held.getTag().getString("GunId");
                                                if (gunId != null && !gunId.isEmpty()) {
                                                    configKey = itemId + "{GunId:\"" + gunId + "\"}";
                                                }
                                            }

                                            // 从缓存里删掉这一条
                                            EntryCache.UnitMapCache.remove(configKey);
                                            EntryCache.NBTMapCache.remove(configKey);

                                            // 立即保存到文件
                                            EntryCache.saveConfig();

                                            String finalConfigKey = configKey;
                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("✅ 已重置 ")
                                                                    .append(Component.literal(finalConfigKey)
                                                                            .withStyle(ChatFormatting.YELLOW))
                                                                    .append(Component.literal(" 的尺寸配置")),
                                                    true);
                                            return 1;
                                        })
                        )
                        /* ========== reload ========== */
                        .then(
                                Commands.literal("reload")
                                        .executes(ctx -> {
                                            EntryCache.loadAllRule();
                                            ctx.getSource().sendSuccess(() ->
                                                            Component.literal("🔄 已重新加载配置")
                                                                    .withStyle(ChatFormatting.GREEN),
                                                    true);
                                            return 1;
                                        })
                        )
        );
    }
}