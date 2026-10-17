package com.pasillos.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Clic derecho para encender o apagar. Encendida y en la mano, la niebla de la dimension retrocede. */
public class FlashlightItem extends Item {

    public FlashlightItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isOn(ItemStack stack) {
        return stack.getItem() instanceof FlashlightItem && stack.hasTag() && stack.getTag().getBoolean("on");
    }

    /** True si el jugador lleva una linterna encendida en cualquiera de las dos manos. */
    public static boolean holdingOn(Player player) {
        return player != null && (isOn(player.getMainHandItem()) || isOn(player.getOffhandItem()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean on = !isOn(stack);
        stack.getOrCreateTag().putBoolean("on", on);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK,
                SoundSource.PLAYERS, 0.6F, on ? 1.5F : 1.0F);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
