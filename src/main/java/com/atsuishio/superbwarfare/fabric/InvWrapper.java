package com.atsuishio.superbwarfare.fabric;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Замена net.neoforged.neoforge.items.wrapper.InvWrapper: Container -> IItemHandler. */
public class InvWrapper implements IItemHandler {
    private final Container inv;

    public InvWrapper(Container inv) {
        this.inv = inv;
    }

    public Container getInv() {
        return inv;
    }

    @Override
    public int getSlots() {
        return inv.getContainerSize();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv.getItem(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack stackInSlot = inv.getItem(slot);

        int m;
        if (!stackInSlot.isEmpty()) {
            if (stackInSlot.getCount()
                    >= Math.min(stackInSlot.getMaxStackSize(), getSlotLimit(slot))) return stack;
            if (!ItemStack.isSameItemSameComponents(stack, stackInSlot)) return stack;
            if (!inv.canPlaceItem(slot, stack)) return stack;

            m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot)) - stackInSlot.getCount();

            if (stack.getCount() <= m) {
                if (!simulate) {
                    ItemStack copy = stack.copy();
                    copy.grow(stackInSlot.getCount());
                    inv.setItem(slot, copy);
                    inv.setChanged();
                }
                return ItemStack.EMPTY;
            }

            // копия, чтобы не трогать переданный стек
            stack = stack.copy();
            if (!simulate) {
                ItemStack copy = stack.split(m);
                copy.grow(stackInSlot.getCount());
                inv.setItem(slot, copy);
                inv.setChanged();
                return stack;
            }
            stack.shrink(m);
            return stack;
        }

        if (!inv.canPlaceItem(slot, stack)) return stack;

        m = Math.min(stack.getMaxStackSize(), getSlotLimit(slot));
        if (m < stack.getCount()) {
            stack = stack.copy();
            if (!simulate) {
                inv.setItem(slot, stack.split(m));
                inv.setChanged();
                return stack;
            }
            stack.shrink(m);
            return stack;
        }

        if (!simulate) {
            inv.setItem(slot, stack);
            inv.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount == 0) return ItemStack.EMPTY;

        ItemStack stackInSlot = inv.getItem(slot);
        if (stackInSlot.isEmpty()) return ItemStack.EMPTY;

        if (simulate) {
            if (stackInSlot.getCount() < amount) return stackInSlot.copy();
            ItemStack copy = stackInSlot.copy();
            copy.setCount(amount);
            return copy;
        }

        int m = Math.min(stackInSlot.getCount(), amount);
        ItemStack removed = inv.removeItem(slot, m);
        inv.setChanged();
        return removed;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        inv.setItem(slot, stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inv.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inv.canPlaceItem(slot, stack);
    }
}
