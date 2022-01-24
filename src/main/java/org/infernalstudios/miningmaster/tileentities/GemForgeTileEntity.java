/*
 * Copyright 2021 Infernal Studios
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.infernalstudios.miningmaster.tileentities;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.items.ItemStackHandler;
import org.infernalstudios.miningmaster.MiningMaster;
import org.infernalstudios.miningmaster.containers.GemForgeContainer;
import org.infernalstudios.miningmaster.init.MMItems;
import org.infernalstudios.miningmaster.init.MMTileEntityTypes;

import javax.annotation.Nullable;

public class GemForgeTileEntity extends LockableTileEntity implements ISidedInventory, ITickableTileEntity, IRecipeHolder, IRecipeHelperPopulator {

    @Nullable
    protected ITextComponent customName;
    private final ItemStackHandler inventory = new ItemStackHandler(9){
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markDirty();
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipes = new Object2IntOpenHashMap<>();

    private static final int[] SLOTS_UP = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] SLOTS_DOWN = new int[]{9};
    private static final int[] SLOTS_HORIZONTAL = new int[]{9};

    public GemForgeTileEntity() {
        super(MMTileEntityTypes.GEM_FORGE_TILE_ENTITY.get());
    }

    public void tick() {

    }

    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.pos, 3, this.getUpdateTag());
    }

    public CompoundNBT getUpdateTag() {
        return this.write(new CompoundNBT());
    }

    public void setCustomName(ITextComponent name) {
        this.customName = name;
    }

    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        inventory.deserializeNBT(nbt.getCompound("inv"));
        CompoundNBT compoundnbt = nbt.getCompound("RecipesUsed");

        for(String s : compoundnbt.keySet()) {
            this.recipes.put(new ResourceLocation(s), compoundnbt.getInt(s));
        }

    }

    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        compound.put("inv", inventory.serializeNBT());
        CompoundNBT compoundnbt = new CompoundNBT();
        this.recipes.forEach((recipeId, craftedAmount) -> {
            compoundnbt.putInt(recipeId.toString(), craftedAmount);
        });
        compound.put("RecipesUsed", compoundnbt);
        return compound;
    }

    @Nullable
    public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
        return new GemForgeContainer(id, inv, this.inventory);
    }

    @Override
    protected Container createMenu(int id, PlayerInventory inv) {
        return new GemForgeContainer(id, inv, this.inventory);
    }

    public ITextComponent getDisplayName() {
        return this.customName != null ? this.customName : new TranslationTextComponent(MiningMaster.MOD_ID + ':' + "container.gem_forge");
    }

    @Override
    protected ITextComponent getDefaultName() {
        return null;
    }

    // TWEAK THIS
    protected boolean canSmelt(@Nullable IRecipe<?> recipeIn) {
        if (!this.inventory.getStackInSlot(0).isEmpty() && recipeIn != null) {
            ItemStack itemstack = ((IRecipe<ISidedInventory>) recipeIn).getCraftingResult(this);
            if (itemstack.isEmpty()) {
                return false;
            } else {
                ItemStack itemstack1 = this.inventory.getStackInSlot(2);
                if (itemstack1.isEmpty()) {
                    return true;
                } else if (!itemstack1.isItemEqual(itemstack)) {
                    return false;
                } else if (itemstack1.getCount() + itemstack.getCount() <= this.getInventoryStackLimit() && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()) { // Forge fix: make furnace respect stack sizes in furnace recipes
                    return true;
                } else {
                    return itemstack1.getCount() + itemstack.getCount() <= itemstack.getMaxStackSize(); // Forge fix: make furnace respect stack sizes in furnace recipes
                }
            }
        } else {
            return false;
        }
    }

    // TWEAK THIS
    private void smelt(@Nullable IRecipe<?> recipe) {
        if (recipe != null && this.canSmelt(recipe)) {
            ItemStack itemstack = this.inventory.getStackInSlot(0);
            ItemStack itemstack1 = ((IRecipe<ISidedInventory>) recipe).getCraftingResult(this);
            ItemStack itemstack2 = this.inventory.getStackInSlot(2);
            if (itemstack2.isEmpty()) {
                this.inventory.setStackInSlot(2, itemstack1.copy());
            } else if (itemstack2.getItem() == itemstack1.getItem()) {
                itemstack2.grow(itemstack1.getCount());
            }

            if (!this.world.isRemote) {
                this.setRecipeUsed(recipe);
            }

            itemstack.shrink(1);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_DOWN;
        } else {
            return side == Direction.UP ? SLOTS_UP : SLOTS_HORIZONTAL;
        }
    }

    public int getInventoryStackLimit() {
        return 1;
    }

    public boolean canInsertItem(int index, ItemStack itemStackIn, @Nullable Direction direction) {
        return this.isItemValidForSlot(index, itemStackIn);
    }

    // TWEAK THIS
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        return stack.getItem() != MMItems.FIRE_RUBY.get();
    }

    public int getSizeInventory() {
        return this.inventory.getSlots();
    }

    public boolean isEmpty() {
        for(int i = 0; i < 9; i++) {
            if (!this.inventory.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    // Returns the stack in the given slot.
    public ItemStack getStackInSlot(int index) {
        return this.inventory.getStackInSlot(index);
    }

    // Removes up to a specified number of items from an inventory slot and returns them in a new stack.
    public ItemStack decrStackSize(int index, int count) {
        return !this.inventory.getStackInSlot(index).isEmpty() && count > 0 ? this.inventory.getStackInSlot(index).split(count) : ItemStack.EMPTY;
    }

    // Removes a stack from the given slot and returns it.
    public ItemStack removeStackFromSlot(int index) {
        ItemStack itemStack = this.inventory.getStackInSlot(index);
        this.inventory.setStackInSlot(index, ItemStack.EMPTY);
        return itemStack;
    }

    // Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.inventory.setStackInSlot(index, stack);
        if (stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }
        this.markDirty();
    }

    public boolean isUsableByPlayer(PlayerEntity player) {
        if (this.world.getTileEntity(this.pos) != this) {
            System.out.println(false);
            return false;
        } else {
            System.out.println(true);
            return player.getDistanceSq((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.5D, (double)this.pos.getZ() + 0.5D) <= 64.0D;
        }
    }

    // TWEAK THIS
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    public void clear() {
        for(int i = 0; i < 9; i++) {
            this.inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public void setRecipeUsed(@Nullable IRecipe<?> recipe) {
        if (recipe != null) {
            ResourceLocation resourcelocation = recipe.getId();
            this.recipes.addTo(resourcelocation, 1);
        }
    }

    @Nullable
    public IRecipe<?> getRecipeUsed() {
        return null;
    }

    public void fillStackedContents(RecipeItemHelper helper) {
        for(int i = 0; i < 9; i++) {
            helper.accountStack(this.inventory.getStackInSlot(i));
        }
    }

    net.minecraftforge.common.util.LazyOptional<? extends net.minecraftforge.items.IItemHandler>[] handlers =
            net.minecraftforge.items.wrapper.SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
        if (!this.removed && facing != null && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == Direction.UP)
                return handlers[0].cast();
            else if (facing == Direction.DOWN)
                return handlers[1].cast();
            else
                return handlers[2].cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        for (int x = 0; x < handlers.length; x++)
            handlers[x].invalidate();
    }
}