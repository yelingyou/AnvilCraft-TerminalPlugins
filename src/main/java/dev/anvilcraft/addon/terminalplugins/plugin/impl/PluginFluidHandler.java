package dev.anvilcraft.addon.terminalplugins.plugin.impl;

import dev.anvilcraft.addon.terminalplugins.init.AddonDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

// 把插件物品上的 FLUID_BUFFER 组件包装成一个单罐 IFluidHandler，交给铁砧工艺的 FluidHandlerWrapper 使用。
public class PluginFluidHandler implements IFluidHandler {
    private final ItemStack pluginStack;
    private final int capacity;

    public PluginFluidHandler(ItemStack pluginStack, int capacityMb) {
        this.pluginStack = pluginStack;
        this.capacity = Math.max(1000, capacityMb);
    }

    public FluidStack getFluid() {
        return this.pluginStack.getOrDefault(AddonDataComponents.FLUID_BUFFER, FluidStack.EMPTY);
    }

    private void setFluid(FluidStack fluid) {
        this.pluginStack.set(AddonDataComponents.FLUID_BUFFER, fluid);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return this.getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return this.capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        FluidStack current = this.getFluid();
        if (!current.isEmpty() && !FluidStack.isSameFluidSameComponents(current, resource)) {
            return 0;
        }
        int space = this.capacity - current.getAmount();
        int amount = Math.min(space, resource.getAmount());
        if (amount <= 0) {
            return 0;
        }
        if (action.execute()) {
            if (current.isEmpty()) {
                this.setFluid(resource.copyWithAmount(amount));
            } else {
                this.setFluid(current.copyWithAmount(current.getAmount() + amount));
            }
        }
        return amount;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack current = this.getFluid();
        if (current.isEmpty() || !FluidStack.isSameFluidSameComponents(current, resource)) {
            return FluidStack.EMPTY;
        }
        return this.drain(Math.min(resource.getAmount(), current.getAmount()), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack current = this.getFluid();
        if (current.isEmpty() || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int amount = Math.min(maxDrain, current.getAmount());
        FluidStack drained = current.copyWithAmount(amount);
        if (action.execute()) {
            this.setFluid(amount >= current.getAmount()
                ? FluidStack.EMPTY
                : current.copyWithAmount(current.getAmount() - amount));
        }
        return drained;
    }
}