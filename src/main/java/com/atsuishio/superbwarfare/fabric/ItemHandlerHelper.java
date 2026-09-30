package com.atsuishio.superbwarfare.fabric;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Замена net.neoforged.neoforge.items.ItemHandlerHelper: только то, что зовёт мод. */
public class ItemHandlerHelper {
    private ItemHandlerHelper() {}

    public static ItemStack insertItem(IItemHandler dest, ItemStack stack, boolean simulate) {
        if (dest == null || stack.isEmpty()) return stack;

        for (int i = 0; i < dest.getSlots(); i++) {
            stack = dest.insertItem(i, stack, simulate);
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }
        return stack;
    }

    /**
     * Кладёт стек в инвентарь, сначала добивая уже лежащие такие же стеки: так же, как это делает
     * подбор предмета игроком.
     */
    public static ItemStack insertItemStacked(
            IItemHandler inventory, ItemStack stack, boolean simulate) {
        if (inventory == null || stack.isEmpty()) return stack;
        if (!stack.isStackable()) return insertItem(inventory, stack, simulate);

        int size = inventory.getSlots();

        for (int i = 0; i < size; i++) {
            if (ItemStack.isSameItemSameComponents(inventory.getStackInSlot(i), stack)) {
                stack = inventory.insertItem(i, stack, simulate);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }

        for (int i = 0; i < size; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                stack = inventory.insertItem(i, stack, simulate);
                if (stack.isEmpty()) return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    /**
     * Кладёт стек в инвентарь игрока, остаток выбрасывает в мир.
     *
     * <p>ponytail: вместо PlayerMainInvWrapper из NeoForge используется ванильный Inventory.add --
     * это та же логика подбора (хотбар в приоритете, pop-анимация), только без своей обёртки над 36
     * слотами. Остаток летит через Player.drop, а не ItemEntity у ног с задержкой подбора 40 тиков.
     * Если понадобится точное поведение NeoForge, писать сюда обёртку RangedWrapper.
     */
    public static void giveItemToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) return;

        Level level = player.level();
        ItemStack remainder = stack.copy();
        int before = remainder.getCount();

        player.getInventory().add(remainder);

        if (remainder.getCount() != before) {
            level.playSound(
                    null,
                    player.getX(),
                    player.getY() + 0.5,
                    player.getZ(),
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.2F,
                    ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
        }

        if (!remainder.isEmpty() && !level.isClientSide) {
            player.drop(remainder, false);
        }
    }
}
