# Drone control hardening — local review

Refs netherg-io/blockfield-releases#4. Baseline: `8c6a00547a6812703ca632915b149e31e185a014`.

This changes the **existing Superb Warfare Fabric runtime**, not a second drone engine. It is a prerequisite slice of the Drone Warfare epic, **not the port of the separate addon**. Keep the epic open and this PR draft until local review.

## Changed runtime

`DroneControlAccess` supplies the same exact-owner/entity binding, linked/active monitor, life/spectator, world and existing per-drone range checks to movement, mouse, fire and interaction packets. Finite mouse input is checked after Float narrowing; block-target coordinates cannot silently saturate to Int limits. The artillery target still comes from the existing client protocol: these numeric checks do not prove a server raycast or LOS. Mounted-vehicle passenger/turret handling is retained; packets no longer update both a mounted vehicle and a remote drone in one mouse operation.

The monitor only activates an eligible main-hand binding. Leaving a missing drone is permitted, link/unlink clears `Using`, and dormant copies do not repeatedly reset another active drone. Client camera access is confined to client-annotated methods called on the client; the old unconditional client-field read in server inventory ticks is removed.

Input cleanup calls the existing `processInput(0)` and `mouseInput(0, 0)` plus clears `fire`, instead of writing unrelated persistent-data keys. A separate Fabric Kotlin entrypoint tracks only loaded drones through entity load/unload callbacks, checks control at the start of world ticks, invalidates the active monitor on disconnect, and drops references on unload/server stop. It does not scan the entire world each tick, load chunks, create a second backend, delete drones or change the existing signal-loss explosion/balance. Tick checks are a fallback, not proof that all same-tick death/damage ordering races are solved.

ScoutDroneEntity and its non-combat overrides, models/resources, class stocks, recipes and registry IDs are not changed.

## Tests actually run

```sh
bash dev/test-drone-control.sh
```

Kotlin 1.9.0 on OpenJDK 21, `-Werror`; the standalone harness targets JVM 17 for compiler compatibility (the production project remains Java 21).

- **198** policy checks: independent-gate matrix, activation/active use, owner and entity mismatch, world/life/range, NaN/infinity, Float overflow and Int target conversion.
- **35** adapter/lifecycle checks execute the actual `DroneControlAccess` and `DroneControlEvents` sources with explicitly isolated Minecraft/Fabric test doubles, including copy-on-read tags, disconnection, unload, duplicate cleanup and stopped-server references.
- Shell syntax and Fabric metadata JSON parse checked.

Fixtures under `dev/drone-control-tests/fixtures` are not production classes or proof of real API/linkage compatibility. The harness does **not** compile MonitorItem or the payload classes against Minecraft. Full Gradle/remap build, dedicated-server class loading, camera rendering, real callbacks and two-client tests have **not** been run in the authoring container. Existing GitHub build CI may provide additional results; do not treat a queued run as passing.

## Required local acceptance

Use the existing project toolchain/cache for the full build; `PORT-BRIEF.md` reserves heavy Gradle work for the coordinator. Review the existing CI result and then run `./gradlew build` on the local coordinator as appropriate. Do not publish a release merely to validate this branch.

1. Start dedicated Fabric 1.21.1 without loading client-only classes on the server. Check both normal and scout drone control.
2. With two clients test foreign/copied monitors, wrong linked IDs, dead/spectator operators, missing drones, another world and each drone's configured control distance.
3. Start movement/mouse/fire, then deselect, switch between two monitors, stop control, die, disconnect, unload a chunk and reconnect. Inspect real synced input, inventory tags, camera type, sound and entity tracking. Test a grounded drone as well as a flying one.
4. Ensure an unused inventory monitor cannot cancel active control. Test off-hand interactions without toggling the main-hand monitor.
5. Verify turret/passenger mouse input, ordinary vehicle driving, artillery marking, drone interaction and the scout's existing prohibition on armed attachments/fire.
6. Check actual range loss, world unload, server stop/restart and gameplay-room teardown. Confirm no retained entity/world references or forced chunks.

## Epic work not implemented by this PR

- Separate public upstream fork and Fabric Drone Warfare addon/build/release. Available connector actions did not provide repository/fork creation; no new public repository or upstream resource copy was made. The owner's approval of the public-fork approach remains valid. Existing private repository visibility and licenses are unchanged.
- Source/release and resource-license audit of `SmartStreamLabs/-SBW-Drone-Warfare`, public dependency supply, clean addon build and release provenance.
- Protocol session IDs/generations and sequence rejection. Current wire payloads cannot distinguish delayed input from a previous control generation after reactivation.
- Complete camera/input/audio session teardown, physical binding/unlink/dismantling authorization, same-tick ordering, game-room/context and consent-PvP integration. The four input checks are not a claim that every interaction/damage path is secured.
- Angle/Acro physics, inertia/payload/battery model, EW/fiber/signal rendering, collision zones, advanced sound, chunk-ticket budgets and addon stock integration.

No main-branch change, merge, tag, jar release, modpack pin, deployment or production restart belongs to this PR.

## Follow-up: control-session anti-replay (this stage)

Refs netherg-io/blockfield-releases#4, in-game finding from 2026-09-15: `VehicleMovementMessage`,
`MouseMoveMessage` and `DroneFireMessage` were authorised only by the currently-held monitor/linked
drone (the check this doc describes above), with no session/sequence binding. A packet queued
while one activation was live could still be applied after the player switched drones or
reactivated, because `resolve()`/`canUse()` only look at _current_ state.

This closes that gap **inside superbwarfare-fabric itself**, not by depending on
blockfield-mod's `DroneControlSessions` (that class stays private-repo-only; this mod is the one
going public per the owner's 2026-09-14 decision, so it must not gain a build dependency on the
private mod). The model is a much smaller, purpose-built anti-replay counter:

- `DroneControlSession` (`control/DroneControlPolicy.kt`): a pure, Minecraft-free id + strictly
  increasing sequence, unit-tested standalone (`dev/drone-control-tests/DroneControlSessionChecks.kt`).
- `DroneEntity.SESSION`: synced entity data holding the current session id (`"none"` when
  uncontrolled) — server-authoritative, readable by clients, never writable by them.
- `DroneEntity.beginControlSession()` / `endControlSession()` / `acceptControlSequence(...)`:
  minted once per monitor activation (`MonitorItem.use()`, server side, right after
  `DroneControlAccess.resetInput`), and invalidated by `resetInput` itself — the single choke
  point every existing teardown path (unload, `stopMonitor`, `resetIfUncontrolled`, disconnect)
  already routed through, so no new teardown call sites were needed.
- `DroneControlAccess.acceptsSequence(...)`: the one check the three packet handlers call after
  `resolve()`/ownership already passed. A stale session id, or a duplicate/out-of-order sequence
  within the live session, is dropped with no side effect (the rest of the packet still no-ops).
- Client side: `DroneEntity.nextClientSequence()` is a plain per-entity counter that resets
  whenever the synced session id changes underneath it; the three send sites
  (`ClientEventHandler.handleControlVehicle`, `ClientMouseHandler.handleClientTick`,
  `ClickEventHandler.droneLeftClick`) read the drone's current session id and next sequence when
  they already resolve the drone for other reasons. Plain vehicle control (non-drone) is
  unaffected; the new packet fields default to `"none"`/`0` and are ignored on that path.

### Tests run

Historical results below used Kotlin 2.1.21 and JDK 21. Current build commands:

```sh
bash dev/test-drone-control.sh
```

- Policy checks: 198 passed (unchanged).
- Adapter/lifecycle checks: 44 passed (9 new: fresh session accepts, replay rejected, foreign
  session rejected, next sequence in-session accepted, `resetInput` invalidates the session,
  reactivation mints a new id, the old id is never accepted again even with a fresh sequence,
  a new session restarts its own sequence at 0, `stopMonitor` ends the session too).
- Session checks (new): 11 passed — wrong session id, negative sequence, duplicate/replayed
  sequence (including idempotent double-rejection), stale/other-session sequence, sequence gaps
  are fine, no rewinding after a gap, auto-generated ids are distinct.

Full Gradle build (`./gradlew build`) passed. In-game: normal monitor/drone control end-to-end
(link, activate, move/look/fire, deactivate) verified on the rig with two clients against the
built jar — see the PR body for the evidence log. Reconnect/death/dimension-change and looping
motor sound teardown are the next stage (control-session **teardown gaps**), not this one; this
stage only adds the anti-replay pinning, it does not change when a session ends.

### Still open for this epic

Everything listed under "Epic work not implemented by this PR" above still applies **except**
"Protocol session IDs/generations and sequence rejection", which this stage implements.

## Follow-up: control-session teardown gaps (netherg-io/blockfield-releases#4, stage 2)

Root cause for the dimension-change gap: `DroneEntity.getController()` (and seven duplicate
call sites doing the same lookup inline) resolved the controller via
`EntityFindUtil.findPlayer(this.level(), CONTROLLER)` -- a **level-scoped** lookup. A `Player`
entity is only tracked by the `Level` it currently occupies, so the instant an operator changed
dimension away from their drone, this returned `null`. That silently broke the single teardown
choke point: `DroneControlAccess.resetIfUncontrolled` reads `val player = drone.getController()`
and only calls `stopMonitor` (camera reset + `Using=false`) `if (player != null)` -- with the old
lookup, a dimension-changed operator looked exactly like "no controller at all", so `resetInput`
ran but `stopMonitor` never did, leaving the operator's monitor stuck on `Using=true` and no
`ResetCameraTypeMessage` ever sent.

Fix: `EntityFindUtil.findPlayerAnywhere` (`tools/EntityFindUtil.kt`) looks the player up via
`ServerLevel.getServer().playerList` -- authoritative across every dimension, like vanilla's own
`/tp`-across-dimensions support -- instead of the current level's entity tracker.
`DroneEntity.getController()` and every inline duplicate of the same lookup now route through it
(one shared fix instead of patching each call site). `baseTick()`'s distance-based signal-loss
explosion now also checks `controller.level() === this.level()` first: positions from two
different dimensions are not a meaningful distance, so a cross-dimension controller no longer
risks a bogus explosion -- dimension-mismatch teardown is left entirely to the existing
`resetIfUncontrolled`/`canUse` `sameWorld` check on the next world tick, which ends the session
cleanly (camera reset, `Using=false`) without a blast. The existing same-world out-of-range
signal-loss explosion is unchanged.

Chunk unload and reconnect were re-verified against the _existing_ (unmodified this stage) code
and found already correct: `DroneControlEvents.ENTITY_UNLOAD` already calls
`getController()?.let { stopMonitor(it, entity) }` + `resetInput(entity)` and drops the entity
from the per-world `loaded` set; `ServerPlayConnectionEvents.DISCONNECT` already ends the session
and clears `Using` for every drone the disconnecting player owns. Both routes reuse the same
`resetInput`/`stopMonitor` choke point as the dimension-change fix, so no second call site was
added for them.

### Tests run

Historical results below used Kotlin 2.1.21 and JDK 21. Current build commands:

```sh
just check
just build
```

- Policy checks: 198 passed (unchanged).
- Adapter/lifecycle checks: 46 passed (44 from stage 1 + 2 new: dimension-change teardown clears
  input and stops the monitor when `getController()` must search another `ServerLevel`; fixtures
  gained a server-wide `TestServer.players` registry mirroring `MinecraftServer.getPlayerList()`,
  replacing the old level-scoped `Level.players` map that could not reproduce this bug).
- Sanity check: temporarily forcing the fixture `getController()` back to `null` made the new
  dimension-change assertions fail to compile clean (`-Werror` flagged the `resetIfUncontrolled`
  guards as "condition is always false"), confirming the new test actually exercises the fix.
- Full Gradle build: passed.
- In-game (rig, server + two clients, this stage's jar on all three): dimension-change teardown,
  chunk-unload teardown, and disconnect/reconnect-with-one-drone all verified -- see the PR body
  and `/var/tmp/bf-jobs/impl-4-s2/evidence/` for logs, RCON transcripts and screenshots.
- NOT done in-game: replaying a stale/pre-disconnect control packet against a live server socket
  (no packet-injection tool available via the rig's bot interface). The anti-replay logic itself
  is unit-tested (stage 1, unchanged, still green) but this specific live-replay scenario needs
  either a custom test client or owner-provided tooling.
- NOT verified: the looping motor sound (`VehicleSoundInstance.EngineSound`) teardown on every
  exit path. Code reading shows it already stops generically via `mobileVehicle.isRemoved()` (drone
  destroyed/discarded) or `!engineRunning()` (`abs(power) <= 0.05`, reached once `resetInput` zeroes
  the throttle and drone deceleration brings power down) -- the same two conditions cover manual
  stop, death, disconnect, dimension change and chunk unload without any dedicated per-path
  handling, so no code change looked warranted. This is reasoning from source, not an audible or
  instrumented check (this is a headless rig; no audio device, and no debug hook was added/removed
  for it this stage) -- needs owner confirmation with real audio output.
