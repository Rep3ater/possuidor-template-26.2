package ladysnake.requiem.common.block;

import com.google.common.base.Suppliers;
import ladysnake.requiem.Requiem;
import ladysnake.requiem.api.v1.block.ObeliskRune;
import ladysnake.requiem.api.v1.block.VagrantTargetableBlock;
import ladysnake.requiem.common.block.obelisk.InertRunestoneBlock;
import ladysnake.requiem.common.block.obelisk.ReclamationRunestoneBlock;
import ladysnake.requiem.common.block.obelisk.RiftRunestoneBlock;
import ladysnake.requiem.common.block.obelisk.RunestoneBlock;
import ladysnake.requiem.common.entity.effect.RequiemStatusEffects;
import ladysnake.requiem.common.item.RequiemItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class RequiemBlocks {
    private static final Map<Block, BlockRegistration> allBlocks = new LinkedHashMap<>();

    public static final Block TACHYLITE = make(Block::new, "tachylite/tachylite", CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN)
        .destroyTime(25.0f)
        .explosionResistance(600.0f)
    );
    public static final Block CHISELED_TACHYLITE = makeVariant(TACHYLITE, "tachylite/chiseled");
    public static final RotatedPillarBlock CHISELED_TACHYLITE_PILLAR = makePillar(CHISELED_TACHYLITE);
    public static final SlabBlock CHISELED_TACHYLITE_SLAB = makeSlab(CHISELED_TACHYLITE);
    public static final StairBlock CHISELED_TACHYLITE_STAIRS = makeStairs(CHISELED_TACHYLITE);
    public static final SlabBlock TACHYLITE_SLAB = makeSlab(TACHYLITE);
    public static final StairBlock TACHYLITE_STAIRS = makeStairs(TACHYLITE);
    public static final Block SCRAPED_TACHYLITE = makeVariant(TACHYLITE, "tachylite/scraped");
    public static final InertRunestoneBlock TACHYLITE_RUNESTONE = make(InertRunestoneBlock::new, "tachylite/runestone", CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.ofFullCopy(TACHYLITE));
    public static final DropExperienceBlock DERELICT_TACHYLITE = make(p -> new DropExperienceBlock(UniformInt.of(7, 14), p), "tachylite/derelict", CreativeModeTabs.NATURAL_BLOCKS, BlockBehaviour.Properties.ofFullCopy(TACHYLITE));
    public static final RunestoneBlock RUNIC_TACHYLITE_ATTRITION = makeRunic("attrition", 3);
    public static final RunestoneBlock RUNIC_TACHYLITE_EMANCIPATION = makeRunic("emancipation", 1);
    public static final RunestoneBlock RUNIC_TACHYLITE_PENANCE = makeRunic("penance", 3);
    public static final ReclamationRunestoneBlock RUNIC_TACHYLITE_RECLAMATION = make(p -> new ReclamationRunestoneBlock(p, () -> RequiemStatusEffects.RECLAMATION, 1), "tachylite/runic/reclamation", CreativeModeTabs.FUNCTIONAL_BLOCKS, BlockBehaviour.Properties.ofFullCopy(TACHYLITE_RUNESTONE));
    public static final RiftRunestoneBlock RIFT_RUNE = make(RiftRunestoneBlock::new, "tachylite/runic/rift", CreativeModeTabs.FUNCTIONAL_BLOCKS, BlockBehaviour.Properties.ofFullCopy(TACHYLITE_RUNESTONE));
    // The Suffering Head has a custom placed model and supplies its own BlockItem from RequiemItems.
    public static final Block SUFFERING_HEAD = makeWithoutItem(
        Block::new,
        "suffering_head",
        BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE).noOcclusion()
    );
    public static final Block CONSTRUCT_HEART = make(
        ladysnake.requiem.common.construct.block.ConstructHeartBlock::new,
        "construct_heart",
        CreativeModeTabs.FUNCTIONAL_BLOCKS,
        BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).lightLevel(state -> 12).noOcclusion()
    );
    public static final Block SMALL_CONSTRUCT_HEART = make(
        ladysnake.requiem.common.construct.block.SmallConstructHeartBlock::new,
        "small_construct_heart",
        CreativeModeTabs.FUNCTIONAL_BLOCKS,
        BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).lightLevel(state -> 12).noOcclusion()
    );
    public static final Block KEY_TRIGGER_R = make(
        ladysnake.requiem.common.construct.block.KeyTriggerBlock::newR,
        "key_trigger_r",
        CreativeModeTabs.REDSTONE_BLOCKS,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()
    );
    public static final Block KEY_TRIGGER_V = make(
        ladysnake.requiem.common.construct.block.KeyTriggerBlock::newV,
        "key_trigger_v",
        CreativeModeTabs.REDSTONE_BLOCKS,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()
    );
    public static final Block CLONING_MACHINE = make(
        Block::new,
        "cloning_machine",
        CreativeModeTabs.FUNCTIONAL_BLOCKS,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).lightLevel(state -> 7).noOcclusion()
    );

    private static Block makeVariant(Block base, String id) {
        return make(Block::new, id, CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.ofFullCopy(base));
    }

    private static RotatedPillarBlock makePillar(Block base) {
        return make(RotatedPillarBlock::new, allBlocks.get(base).name() + "_pillar", CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.ofFullCopy(base));
    }

    private static StairBlock makeStairs(Block base) {
        return make(p -> new StairBlock(base.defaultBlockState(), p), allBlocks.get(base).name() + "_stairs", CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.ofFullCopy(base));
    }

    private static SlabBlock makeSlab(Block base) {
        return make(SlabBlock::new, allBlocks.get(base).name() + "_slab", CreativeModeTabs.BUILDING_BLOCKS, BlockBehaviour.Properties.ofFullCopy(base));
    }

    private static RunestoneBlock makeRunic(String effectName, int maxLevel) {
        return make(p -> new RunestoneBlock(
            p,
            Suppliers.memoize(() -> BuiltInRegistries.MOB_EFFECT.get(Requiem.id(effectName)).orElseThrow()),
            maxLevel
        ), "tachylite/runic/" + effectName, CreativeModeTabs.FUNCTIONAL_BLOCKS, BlockBehaviour.Properties.ofFullCopy(TACHYLITE));
    }

    private static <B extends Block> B makeWithoutItem(Function<BlockBehaviour.Properties, B> factory, String name, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Requiem.id(name));
        properties.setId(key);
        B ret = factory.apply(properties);
        allBlocks.put(ret, new BlockRegistration(name, null));
        return ret;
    }

    private static <B extends Block> B make(Function<BlockBehaviour.Properties, B> factory, String name, ResourceKey<CreativeModeTab> itemGroup, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Requiem.id(name));
        properties.setId(key);
        B ret = factory.apply(properties);
        allBlocks.put(ret, new BlockRegistration(name, new BlockRegistration.BlockItemRegistration(itemGroup, BlockItem::new)));
        return ret;
    }

    public static void registerBlocks(RegisterEvent.RegisterHelper<Block> helper) {
        allBlocks.forEach((block, reg) -> {
            helper.register(Requiem.id(reg.name()), block);
            if (block instanceof ObeliskRune rune) {
                ObeliskRune.LOOKUP.registerForBlocks((world, pos, state, blockEntity, context) -> rune, block);
            }
        });
    }

    public static void registerBlockItems(RegisterEvent.RegisterHelper<Item> helper) {
        allBlocks.forEach((block, reg) -> {
            BlockRegistration.BlockItemRegistration bir = reg.blockItemRegistration();
            if (bir != null) {
                ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Requiem.id(reg.name()));
                Item.Properties itemProps = new Item.Properties().setId(itemKey);
                BlockItem item = bir.blockItemFactory().apply(block, itemProps);
                item.registerBlocks(Item.BY_BLOCK, item);
                helper.register(itemKey, item);
                RequiemItems.addToGroup(bir.group(), item);
            }
        });
    }

    public static void postRegister() {
        InertRunestoneBlock.registerCallbacks();
        VagrantTargetableBlock.LOOKUP.registerForBlocks((world, pos, state, blockEntity, context) -> state.getValue(InertRunestoneBlock.ACTIVATED) ? RIFT_RUNE : null, RIFT_RUNE);
    }

    public static void init() {
        // Registered via RegisterEvent
    }

    public static Stream<Map.Entry<Block, Identifier>> streamRunestones() {
        return allBlocks.entrySet().stream().filter(e -> e.getKey() instanceof InertRunestoneBlock).map(e -> Map.entry(e.getKey(), Requiem.id(e.getValue().name())));
    }
}
