package net.fabricmc.fabric.api.util;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;

public enum TriState {
    FALSE(Boolean.FALSE),
    DEFAULT(null),
    TRUE(Boolean.TRUE);

    private final @Nullable Boolean value;

    TriState(@Nullable Boolean value) {
        this.value = value;
    }

    public static TriState of(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    public static TriState of(@Nullable Boolean bool) {
        return bool == null ? DEFAULT : of(bool.booleanValue());
    }

    public boolean get() {
        return this == TRUE;
    }

    @Nullable
    public Boolean getBoxed() {
        return this.value;
    }

    public boolean orElse(boolean value) {
        return this == DEFAULT ? value : this.get();
    }

    public boolean orElseGet(BooleanSupplier supplier) {
        return this == DEFAULT ? Objects.requireNonNull(supplier, "supplier").getAsBoolean() : this.get();
    }

}
