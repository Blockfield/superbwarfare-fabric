package com.atsuishio.superbwarfare.entity.vehicle.utils;

import net.minecraft.world.entity.Entity;

import org.jetbrains.annotations.Nullable;

/**
 * Null-safe mount-chain reads. Vanilla {@code getRootVehicle} walks {@code isPassenger} and {@code
 * getVehicle} assuming they stay consistent, but a passenger flag can outlive the native link (a
 * dropped {@code SetPassengers} packet, kill while mounted) and the walk then calls a method on a
 * null link. Resolve once with an explicit null stop instead.
 */
public final class VehicleMounts {
    private VehicleMounts() {}

    /** One mount link of the walked chain; generic so checks run without entities. */
    public interface Link<T> {
        boolean riding(T mount);

        @Nullable
        T vehicleOrNull(T mount);
    }

    /**
     * Walks the mount chain to its root. Returns null on a half-torn-down link instead of throwing,
     * so renderers fall back to the vanilla path.
     */
    public static <T> T rootOrNull(T mount, Link<T> link) {
        T current = mount;
        while (current != null && link.riding(current)) {
            T vehicle = link.vehicleOrNull(current);
            if (vehicle == null) return null;
            current = vehicle;
        }
        return current;
    }

    /** The root vehicle of {@code entity}, or null when the chain is torn down. */
    public static @Nullable Entity rootVehicleOrNull(@Nullable Entity entity) {
        if (entity == null) return null;
        return rootOrNull(
                entity,
                new Link<>() {
                    @Override
                    public boolean riding(Entity mount) {
                        return mount.isPassenger();
                    }

                    @Override
                    public @Nullable Entity vehicleOrNull(Entity mount) {
                        return mount.getVehicle();
                    }
                });
    }
}
