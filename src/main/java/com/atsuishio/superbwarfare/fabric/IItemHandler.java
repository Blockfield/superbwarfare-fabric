package com.atsuishio.superbwarfare.fabric;

import net.minecraft.world.item.ItemStack;

/**
 * Замена net.neoforged.neoforge.items.IItemHandler.
 *
 * <p>На Java по той же причине, что и IEnergyStorage: из Kotlin нужен доступ handler.slots.
 *
 * <p>IItemHandlerModifiable из NeoForge слит сюда: отдельный интерфейс с тремя реализациями и без
 * единой ссылки в моде смысла не имел, а SlotItemHandler иначе пришлось бы кастовать.
 */
public interface IItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    boolean isItemValid(int slot, ItemStack stack);

    default void setStackInSlot(int slot, ItemStack stack) {
        throw new UnsupportedOperationException(getClass().getName() + " is not modifiable");
    }
}
