# Superb Warfare — Fabric 1.21.1 port

Unofficial port of [Superb Warfare](https://github.com/Mercurows/SuperbWarfare) (NeoForge 1.21.1,
by Atsuishio, Roki27, Light_Quanta and contributors) to **Fabric 1.21.1**, maintained by
[Blockfield](https://github.com/Blockfield) for the Blockfield builds.
Not affiliated with or endorsed by the upstream authors.

Upstream READMEs: [中文](./README-zh.md) | [English](./README-en.md) (they describe the original
Forge/NeoForge releases, not this port).

## Base

- Upstream snapshot: tag `upstream-0.8.9.1-1.21` (Superb Warfare 0.8.9.1 for 1.21.1).
- Port branch: `main`. Porting notes: [PORT-BRIEF.md](./PORT-BRIEF.md).
- Minecraft 1.21.1, Fabric Loader 0.19.3, Fabric API 0.116.15+1.21.1, Fabric Language Kotlin 1.13.7.

Required mods (see `depends` in `fabric.mod.json`): Fabric API, Fabric Language Kotlin,
Forge Config API Port, Accessories, GeckoLib, Porting Lib 3.1.0-beta.90 (core, entity, items,
level_events, client_events, transfer) and SimpleBedrockModel-Fabric
(see [libs/README.md](./libs/README.md)). Cloth Config and JEI are optional.

## Build

Requires native JDK 21, Python 3.12+, Node.js 22 and Just 1.57.0. No private repositories or tokens are needed;
the two non-Maven inputs are downloaded from pinned URLs and checked by sha256 (see `libs/README.md`).

```sh
git clone https://github.com/Blockfield/superbwarfare-fabric.git
cd superbwarfare-fabric
just setup
just check
just build
```

The mod jar is written to `build/libs/superbwarfare-<version>-mc1.21.1.jar`. Release jars are
attached to [GitHub Releases](https://github.com/Blockfield/superbwarfare-fabric/releases) and
named after the release tag.

## Releases

After committing to `main`, run `scripts/bump-fork.sh superbwarfare` from
[blockfield-client](https://github.com/Blockfield/blockfield-client). It creates a
`bfN` tag, waits for the build and pins the released JAR. Passing an existing `bfN`
as the second argument only updates the pin. Update the server pin as well and
use a coordinated server/client release. `just format` applies source formatting.

## License

Code is licensed under the **GNU LGPL-3.0-only** ([COPYING.LESSER](./COPYING.LESSER), which
supplements the GPL-3.0 in [COPYING](./COPYING)), as in the upstream repository. These files are
kept unchanged from upstream.

The upstream README states that models, textures and other art assets are _all rights reserved_
by the Superb Warfare team; they are included here exactly as published in the upstream public
repository and remain the property of their authors.

Port changes are © Blockfield and contributors, under the same license.
