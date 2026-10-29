/*
 * Requiem
 * Copyright (C) 2017-2023 Ladysnake
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses>.
 */
package ladysnake.requiem.common.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

public class RemnantTradeOffer extends MerchantOffer {
    private final MerchantOffer vanillaOffer, demonOffer;
    private final boolean exorcism;
    private boolean tempDisabled;
    boolean demonCustomer;

    public static RemnantTradeOffer fromNbt(HolderLookup.Provider registries, CompoundTag compound) {
        MerchantOffer vanillaOffer = MerchantOffer.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), (compound.contains("vanilla_offer", net.minecraft.nbt.Tag.TAG_COMPOUND) ? compound.getCompound("vanilla_offer") : new CompoundTag())).getOrThrow();
        MerchantOffer demonOffer = MerchantOffer.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), (compound.contains("demon_offer", net.minecraft.nbt.Tag.TAG_COMPOUND) ? compound.getCompound("demon_offer") : new CompoundTag())).getOrThrow();
        boolean exorcism = compound.getBoolean("exorcism");
        RemnantTradeOffer offer = new RemnantTradeOffer(vanillaOffer, demonOffer, exorcism);
        if (compound.getBoolean("demon_customer")) offer.demonCustomer = true;
        if (compound.getBoolean("temp_disabled")) offer.tempDisabled = true;
        return offer;
    }

    public RemnantTradeOffer(MerchantOffer vanillaOffer, MerchantOffer demonOffer, boolean exorcism) {
        super(vanillaOffer.getItemCostA(), vanillaOffer.getItemCostB(), vanillaOffer.getResult(), vanillaOffer.getUses(), vanillaOffer.getMaxUses(), vanillaOffer.getXp(), vanillaOffer.getPriceMultiplier(), vanillaOffer.getDemand());
        this.vanillaOffer = vanillaOffer;
        this.demonOffer = demonOffer;
        this.exorcism = exorcism;
    }

    private MerchantOffer getDelegate() {
        return this.demonCustomer ? this.demonOffer : this.vanillaOffer;
    }

    public boolean isExorcism() {
        return this.demonCustomer && this.exorcism;
    }

    @Override
    public ItemCost getItemCostA() {
        return getDelegate().getItemCostA();
    }

    @Override
    public Optional<ItemCost> getItemCostB() {
        return getDelegate().getItemCostB();
    }

    @Override
    public ItemStack getBaseCostA() {
        return getDelegate().getBaseCostA();
    }

    @Override
    public ItemStack getCostA() {
        return getDelegate().getCostA();
    }

    @Override
    public ItemStack getCostB() {
        return getDelegate().getCostB();
    }

    @Override
    public ItemStack getResult() {
        return getDelegate().getResult();
    }

    @Override
    public ItemStack assemble() {
        return getDelegate().assemble();
    }

    @Override
    public void updateDemand() {
        getDelegate().updateDemand();
    }

    @Override
    public int getUses() {
        return getDelegate().getUses();
    }

    @Override
    public void resetUses() {
        getDelegate().resetUses();
    }

    @Override
    public int getMaxUses() {
        return getDelegate().getMaxUses();
    }

    @Override
    public void increaseUses() {
        getDelegate().increaseUses();
    }

    @Override
    public int getDemand() {
        return getDelegate().getDemand();
    }

    @Override
    public void addToSpecialPriceDiff(int diff) {
        getDelegate().addToSpecialPriceDiff(diff);
    }

    @Override
    public void resetSpecialPriceDiff() {
        getDelegate().resetSpecialPriceDiff();
    }

    @Override
    public int getSpecialPriceDiff() {
        return getDelegate().getSpecialPriceDiff();
    }

    @Override
    public void setSpecialPriceDiff(int diff) {
        getDelegate().setSpecialPriceDiff(diff);
    }

    @Override
    public float getPriceMultiplier() {
        return getDelegate().getPriceMultiplier();
    }

    @Override
    public int getXp() {
        return getDelegate().getXp();
    }

    @Override
    public boolean isOutOfStock() {
        return tempDisabled || getDelegate().isOutOfStock();
    }

    @Override
    public void setToOutOfStock() {
        getDelegate().setToOutOfStock();
    }

    @Override
    public boolean needsRestock() {
        return getDelegate().needsRestock();
    }

    @Override
    public boolean shouldRewardExp() {
        return getDelegate().shouldRewardExp();
    }

    @Override
    public boolean satisfiedBy(ItemStack first, ItemStack second) {
        return getDelegate().satisfiedBy(first, second);
    }

    @Override
    public boolean take(ItemStack firstBuyStack, ItemStack secondBuyStack) {
        return getDelegate().take(firstBuyStack, secondBuyStack);
    }

    @Override
    public MerchantOffer copy() {
        RemnantTradeOffer copy = new RemnantTradeOffer(this.vanillaOffer.copy(), this.demonOffer.copy(), this.exorcism);
        copy.demonCustomer = this.demonCustomer;
        copy.tempDisabled = this.tempDisabled;
        return copy;
    }

    public void setRemnant(boolean demon) {
        demonCustomer = demon;
    }

    public void setTempDisabled(boolean tempDisabled) {
        this.tempDisabled = tempDisabled;
    }
}
