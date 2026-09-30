package com.atsuishio.superbwarfare.fabric;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Замена net.neoforged.neoforge.items.wrapper.SidedInvWrapper.
 *
 * <p>ponytail: выброшены костыли NeoForge под ванильную печь и варочную стойку (лимит ведра и
 * бутылок в 1 штуку). Оборачиваются только контейнеры самого мода. Если сюда когда-нибудь заведут
 * ванильный WorldlyContainer, вернуть проверки из апстрима.
 */
public class SidedInvWrapper implements IItemHandler {
    protected final WorldlyContainer inv;
    protected final Direction side;

    public SidedInvWrapper(WorldlyContainer inv, Direction side) {
        this.inv = inv;
        this.side = side;
    }

    public static int getSlot(WorldlyContainer inv, int slot, Direction side) {
        if (side == null) return slot;
        int[] slots = inv.getSlotsForFace(side);
        return slot < slots.length ? slots[slot] : -1;
    }

    @Override
    public int getSlots() {
        return side == null ? inv.getContainerSize() : inv.getSlotsForFace(side).length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int i = getSlot(inv, slot, side);
        return i == -1 ? ItemStack.EMPTY : inv.getItem(i);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        int slot1 = getSlot(inv, slot, side);
        if (slot1 == -1) return stack;

        ItemStack stackInSlot = inv.getItem(slot1);

        int m;
        if (!stackInSlot.isEmpty()) {
            if (stackInSlot.getCount()
                    >= Math.min(stackInSlot.getMaxStackSize(), getSlotLimit(slot))) return stack;
            if (!ItemStack.isSameItemSameComponents(stack, stackInSlot)) return stack;
            if (!inv.canPlaceItemThroughFace(slot1, stack, side) || !inv.canPlaceItem(slot1, stack))
                return stack;

            m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - stackInSlot.getCount();

            if (stack.getCount() <= m) {
                if (!simulate) {
                    ItemStack copy = stack.copy();
                    copy.grow(stackInSlot.getCount());
                    setInventorySlotContents(slot1, copy);
                }
                return ItemStack.EMPTY;
            }

            stack = stack.copy();
            if (!simulate) {
                ItemStack copy = stack.split(m);
                copy.grow(stackInSlot.getCount());
                setInventorySlotContents(slot1, copy);
                return stack;
            }
            stack.shrink(m);
            return stack;
        }

        if (!inv.canPlaceItemThroughFace(slot1, stack, side) || !inv.canPlaceItem(slot1, stack))
            return stack;

        m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        if (m < stack.getCount()) {
            stack = stack.copy();
            if (!simulate) {
                setInventorySlotContents(slot1, stack.split(m));
                return stack;
            }
            stack.shrink(m);
            return stack;
        }

        if (!simulate) setInventorySlotContents(slot1, stack);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;

        int slot1 = getSlot(inv, slot, side);
        if (slot1 == -1) return ItemStack.EMPTY;

        ItemStack stackInSlot = inv.getItem(slot1);
        if (stackInSlot.isEmpty()) return ItemStack.EMPTY;
        if (side != null && !inv.canTakeItemThroughFace(slot1, stackInSlot, side))
            return ItemStack.EMPTY;

        if (simulate) {
            if (stackInSlot.getCount() < amount) return stackInSlot.copy();
            ItemStack copy = stackInSlot.copy();
            copy.setCount(amount);
            return copy;
        }

        int m = Math.min(stackInSlot.getCount(), amount);
        ItemStack ret = inv.removeItem(slot1, m);
        inv.setChanged();
        return ret;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        int slot1 = getSlot(inv, slot, side);
        if (slot1 != -1) setInventorySlotContents(slot1, stack);
    }

    private void setInventorySlotContents(int slot, ItemStack stack) {
        // как в NeoForge: за setChanged отвечает обёртка, а не вызывающий код
        inv.setChanged();
        inv.setItem(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inv.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        int slot1 = getSlot(inv, slot, side);
        return slot1 != -1 && inv.canPlaceItem(slot1, stack);
    }
}
