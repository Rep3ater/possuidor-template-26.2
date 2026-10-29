package ladysnake.requiem.common.advancement.criterion;

import com.mojang.serialization.Codec;
import ladysnake.requiem.api.v1.remnant.RemnantType;
import ladysnake.requiem.common.RequiemRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public abstract class RemnantTypePredicate {
    public static final RemnantTypePredicate ANY = new RemnantTypePredicate() {
        @Override
        public boolean matches(RemnantType type) {
            return true;
        }
    };

    public static final Codec<RemnantTypePredicate> CODEC = Codec.STRING
        .xmap(
            id -> {
                ResourceLocation identifier = ResourceLocation.tryParse(id);
                return identifier != null ? new Single(RequiemRegistries.REMNANT_STATES.get(identifier)) : ANY;
            },
            pred -> pred instanceof Single s ? Optional.ofNullable(RequiemRegistries.REMNANT_STATES.getKey(s.type)).map(ResourceLocation::toString).orElse("") : ""
        );

    public abstract boolean matches(RemnantType type);

    public static class Single extends RemnantTypePredicate {
        private final RemnantType type;

        public Single(RemnantType type) {
            this.type = type;
        }

        @Override
        public boolean matches(RemnantType type) {
            return this.type == type;
        }
    }
}
