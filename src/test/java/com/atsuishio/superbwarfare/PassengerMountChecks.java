package com.atsuishio.superbwarfare;

import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleMounts;

/**
 * Contract checks for {@link VehicleMounts}: intact chains resolve to their root, half-torn-down
 * chains resolve to null instead of throwing. The naive walk next to each case documents the
 * pre-fix behavior: vanilla {@code getRootVehicle} has no null stop and NPEs on the same torn input
 * that crashed the renderer after the passengers bursts.
 */
public final class PassengerMountChecks {
    private PassengerMountChecks() {}

    /** Mount stub modelling {@code isPassenger}/{@code getVehicle} of one entity. */
    static final class Stub {
        final boolean riding;
        final Stub vehicle;

        Stub(boolean riding, Stub vehicle) {
            this.riding = riding;
            this.vehicle = vehicle;
        }
    }

    static final VehicleMounts.Link<Stub> LINK =
            new VehicleMounts.Link<>() {
                @Override
                public boolean riding(Stub mount) {
                    return mount.riding;
                }

                @Override
                public Stub vehicleOrNull(Stub mount) {
                    return mount.vehicle;
                }
            };

    /** The pre-fix walk (plain vanilla {@code getRootVehicle} shape): no null stop. */
    static Stub naiveRoot(Stub mount) {
        Stub current = mount;
        while (current.riding) current = current.vehicle;
        return current;
    }

    public static void main(String[] args) {
        int count = 0;

        var parked = new Stub(false, null);
        check(VehicleMounts.rootOrNull(parked, LINK) == parked, "unmounted mount is its own root");
        check(naiveRoot(parked) == parked, "naive walk agrees on intact chains");
        count += 2;

        var carrier = new Stub(false, null);
        var gunner = new Stub(true, carrier);
        var driver = new Stub(true, gunner);
        check(VehicleMounts.rootOrNull(driver, LINK) == carrier, "chain resolves to its root");
        check(naiveRoot(driver) == carrier, "naive walk agrees on intact chains");
        count += 2;

        // Kill while mounted / dropped SetPassengers: flag outlives the link.
        var torn = new Stub(true, null);
        try {
            naiveRoot(torn);
            check(false, "naive walk must throw on a torn link");
        } catch (NullPointerException expected) {
            count++;
        }
        check(VehicleMounts.rootOrNull(torn, LINK) == null, "torn link resolves to null");
        count++;

        // Torn link deeper in the chain resolves to null as well.
        var deep = new Stub(true, new Stub(true, null));
        check(VehicleMounts.rootOrNull(deep, LINK) == null, "deep torn link resolves to null");
        count++;

        check(VehicleMounts.rootOrNull(null, LINK) == null, "null mount resolves to null");
        count++;

        check(VehicleMounts.rootVehicleOrNull(null) == null, "null entity resolves to null");
        count++;

        check(count == 9, "passenger mount checks must not be empty");
        System.out.println("PassengerMountChecks OK: " + count + " checks");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
