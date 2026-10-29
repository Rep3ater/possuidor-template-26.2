package ladysnake.requiem.common.item;

import ladysnake.requiem.common.locki.LockiSpiritSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** One visual age of Locki's living apple. Ownership and source-tree metadata live on the stack. */
public final class LockiAppleItem extends Item {
    private final int rotStage;

    public LockiAppleItem(Properties properties, int rotStage) {
        super(properties);
        this.rotStage = Math.max(0, Math.min(4, rotStage));
    }

    public int getRotStage() {
        return rotStage;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        // Preserve vanilla apple consumption/animation/inventory handling, then resolve the
        // living Locki soul server-side. This is intentionally called before super: vanilla
        // may shrink the stack to empty, but the owner UUID must still be readable.
        if (!level.isClientSide()) {
            LockiSpiritSystem.onAppleEaten(stack, user);
        }
        return super.finishUsingItem(stack, level, user);
    }
}
