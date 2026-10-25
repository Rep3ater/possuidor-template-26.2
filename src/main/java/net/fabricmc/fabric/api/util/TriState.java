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

    public net.minecraft.util.TriState toVanilla() {
        return switch (this) {
            case TRUE -> net.minecraft.util.TriState.TRUE;
            case FALSE -> net.minecraft.util.TriState.FALSE;
            case DEFAULT -> net.minecraft.util.TriState.DEFAULT;
        };
    }

    public static TriState fromVanilla(net.minecraft.util.TriState vanilla) {
        return switch (Objects.requireNonNull(vanilla, "vanilla")) {
            case TRUE -> TRUE;
            case FALSE -> FALSE;
            case DEFAULT -> DEFAULT;
        };
    }
}
