# Pinned build inputs

Two build inputs are not on any Maven repository. `build.gradle.kts` (`pinnedJar`) downloads them
from immutable URLs into `.gradle/pinned-libs/` and fails the build if the sha256 differs, so a
clean checkout builds without private repositories, `mavenLocal()` or personal tokens.

| Jar                                                | sha256                                                             | License                 | Source                                                                                                                                                                                                                                                                                                                                                                                                                 |
| -------------------------------------------------- | ------------------------------------------------------------------ | ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `rhino-1.8.1-SNAPSHOT.jar`                         | `4aab6124356e91e16263fa065556444b96bf5d9438e1399914cd971ef8b73616` | MPL-2.0 (Mozilla Rhino) | Upstream [Mercurows/SuperbWarfare](https://github.com/Mercurows/SuperbWarfare) vendors it as `libs/rhino-1.8.1-SNAPSHOT.jar`; the URL is pinned to upstream commit `676af9f44fce5e0c12e5206c663b976898ced1a8` (git blob `38672b9448bd2400beafe02a85b777edad841bce`, same as tag `upstream-0.8.9.1-1.21` here). Shaded by upstream into package `org.mozillaa`; its classes are copied into our mod jar.                |
| `simplebedrockmodel-fabric-2.5.1+mc1.21.1-bf4.jar` | `d32a0232a63bbc73561bd2d6aa18837911bcdeffa166d7bb6c4f524d4f5c80c6` | LGPL-3.0 (Sh1roCu)      | Release asset [`bf4`](https://github.com/Blockfield/simplebedrockmodel-fabric/releases/tag/bf4) of [Blockfield/simplebedrockmodel-fabric](https://github.com/Blockfield/simplebedrockmodel-fabric), a fork of [Sh1roCu/SimpleBedrockModel-Fabric](https://github.com/Sh1roCu/SimpleBedrockModel-Fabric) branch `1.21.1`. The same jar the Blockfield pack ships; compile and dev-runtime dependency only, not bundled. |

Until 2026-09 both jars were committed here. The committed SimpleBedrockModel jar (`-bf3`, sha256
`04d6c7d20e7ed32394c60323528a9323cd91e1bf0011652554720d258fa2a0dd`) had the same class files as
`bf4` and differed only in `simplebedrockmodel.fabric.mixins.json`, where a local patch moved
`common.FabricItemMixin` into the client section; the pack has run the unpatched `bf4` on the
dedicated server since 2.31.7, so the patch was dropped.
