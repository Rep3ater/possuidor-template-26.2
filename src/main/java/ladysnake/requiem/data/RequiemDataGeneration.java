package ladysnake.requiem.data;

import ladysnake.requiem.Requiem;
import ladysnake.requiem.common.entity.RequiemEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class RequiemDataGeneration {
    public static final TagKey<EntityType<?>> parmenidesProjections = TagKey.create(
        net.minecraft.core.registries.Registries.ENTITY_TYPE,
        Requiem.id("parmenides_projections")
    );

    private RequiemDataGeneration() { }

    public static void gatherServerData(GatherDataEvent event) {
        if (!event.includeServer()) return;
        DataGenerator generator = event.getGenerator();
        event.addProvider(new EntityTypeTagsProvider(generator.getPackOutput(), event.getLookupProvider(), Requiem.MOD_ID, event.getExistingFileHelper()) {
            @Override
            protected void addTags(HolderLookup.Provider provider) {
                tag(parmenidesProjections).add(RequiemEntities.PARMENIDES_PROJECTION);
            }
        });
    }
}

