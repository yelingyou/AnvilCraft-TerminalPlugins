/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 AnvilCraft-TerminalPlugins contributors
 *
 * This file is part of AnvilCraft-TerminalPlugins, an addon for AnvilCraft.
 * Licensed under the GNU Lesser General Public License v3.0 or later.
 * See the LICENSE file in the project root for the full license text.
 */
package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.component.AlchemySettings;
import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginContext;
import dev.anvilcraft.addon.terminalplugins.plugin.PluginKind;
import dev.anvilcraft.addon.terminalplugins.plugin.TerminalPlugin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * 炼金插件：<b>按条件自动使用药水</b>——语义对齐精妙背包的「炼金升级」。
 *
 * <p>每个条目 = 一瓶「过滤药水」+ 一个触发条件（受伤 / 着火 / 水下 / 负效果 ……）+ 阈值。
 * 每隔若干 tick 检查自己（以及配置允许时半径内的附近实体），条件满足就从终端连接的存储中
 * 取出一瓶匹配的药水使用：普通药水直接饮用生效，喷溅 / 滞留药水就地投掷，不祥之瓶照常使用。
 * 用完的空瓶会放回存储。</p>
 */
public class AlchemyPlugin implements TerminalPlugin {
    @Override
    public PluginKind kind() {
        return PluginKind.ALCHEMY;
    }

    @Override
    public int intervalTicks() {
        return 5;
    }

    @Override
    public void onPlayerTick(PluginContext context) {
        ServerPlayer player = context.player();
        if (player == null || !context.storageReachable() || !player.isAlive()) {
            return;
        }
        AlchemySettings settings = context.pluginStack().getOrDefault(
            AddonDataComponents.ALCHEMY_SETTINGS,
            AlchemySettings.DEFAULT
        );
        int interval = Math.max(1, settings.interval());
        if (context.gameTime() % interval != 0) {
            return;
        }
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(player);
        if (settings.nearby() != AlchemySettings.NearbyTarget.NONE) {
            AABB area = player.getBoundingBox().inflate(settings.radius());
            for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area)) {
                if (entity != player && entity.isAlive() && settings.nearby().matches(entity)) {
                    targets.add(entity);
                }
            }
        }
        for (LivingEntity target : targets) {
            if (AlchemyPlugin.applyTo(context, settings, target)) {
                // 与精妙背包一致：一次检查只施加一次
                return;
            }
        }
    }

    /** 对单个目标尝试施加：找到第一个「条件满足且存储里有对应药水」的条目。 */
    private static boolean applyTo(PluginContext context, AlchemySettings settings, LivingEntity target) {
        for (AlchemySettings.AlchemyEntry entry : settings.normalizedEntries()) {
            if (entry.isEmpty() || !entry.trigger().test(target, entry.value())) {
                continue;
            }
            ItemStack potion = context.storage().extractFirst(
                stack -> AlchemyPlugin.matchesFilter(stack, entry.filter(), settings.matchAmplifier()),
                1
            );
            if (potion.isEmpty()) {
                continue;
            }
            AlchemyPlugin.usePotion(context, target, potion);
            return true;
        }
        return false;
    }

    /** 使用一瓶药水：喷溅 / 滞留就地投掷，其余直接生效。 */
    private static void usePotion(PluginContext context, LivingEntity target, ItemStack potion) {
        ServerLevel level = (ServerLevel) target.level();
        if (potion.is(Items.SPLASH_POTION) || potion.is(Items.LINGERING_POTION)) {
            ThrownPotion thrown = new ThrownPotion(
                level,
                target.getX(),
                target.getEyeY() - 0.1,
                target.getZ()
            );
            thrown.setItem(potion.copyWithCount(1));
            level.addFreshEntity(thrown);
            level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.SPLASH_POTION_THROW,
                SoundSource.PLAYERS,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
            );
        } else {
            PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                contents.forEachEffect(effect -> target.addEffect(new MobEffectInstance(effect)));
            } else if (potion.is(Items.OMINOUS_BOTTLE)) {
                potion.finishUsingItem(level, target);
            }
            level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                SoundEvents.GENERIC_DRINK,
                SoundSource.PLAYERS,
                0.5F,
                level.getRandom().nextFloat() * 0.1F + 0.9F
            );
        }
        ItemStack remainder = potion.getCraftingRemainingItem();
        if (!remainder.isEmpty()) {
            context.insertIntoStorage(remainder.copy());
        }
    }

    /** 候选药水是否匹配条目里的过滤药水（效果集合包含关系，可选校验等级）。 */
    private static boolean matchesFilter(ItemStack stack, ItemStack filter, boolean matchAmplifier) {
        if (filter.isEmpty() || stack.isEmpty() || !stack.is(filter.getItem())) {
            return false;
        }
        PotionContents filterContents = filter.get(DataComponents.POTION_CONTENTS);
        PotionContents stackContents = stack.get(DataComponents.POTION_CONTENTS);
        if (filterContents == null || stackContents == null) {
            return ItemStack.isSameItem(stack, filter);
        }
        List<MobEffectInstance> wanted = new ArrayList<>();
        filterContents.forEachEffect(wanted::add);
        if (wanted.isEmpty()) {
            return true;
        }
        List<MobEffectInstance> present = new ArrayList<>();
        stackContents.forEachEffect(present::add);
        for (MobEffectInstance want : wanted) {
            boolean found = false;
            for (MobEffectInstance have : present) {
                if (!have.getEffect().equals(want.getEffect())) {
                    continue;
                }
                if (matchAmplifier && have.getAmplifier() != want.getAmplifier()) {
                    continue;
                }
                found = true;
                break;
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}
