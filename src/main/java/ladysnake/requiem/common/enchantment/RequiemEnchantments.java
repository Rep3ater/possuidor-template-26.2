package ladysnake.requiem.common.enchantment;

import ladysnake.requiem.Requiem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class RequiemEnchantments {
    public static final ResourceLocation HUMANITY_ID = Requiem.id("humanity");
    public static final ResourceKey<Enchantment> HUMANITY = ResourceKey.create(Registries.ENCHANTMENT, HUMANITY_ID);

    public static void init() {
        // Enchantments are data-driven in 26.2
    }
}
