# Local dependency jars

The build reads the bridged mods (and their test/runtime dependencies) from
this folder via the `flatDir` repository in `build.gradle`. The jars themselves
are **not** committed. Drop these files in here before building:

| File | Where to get it |
|------|-----------------|
| `armored-elytra-1.14.1.jar` | Build from source: https://github.com/DorkixAzIgazi/armored-elytra (`./gradlew build`) |
| `elytratrims-4.9.0.jar` | https://modrinth.com/mod/elytra-trims (Fabric 26.2 build, renamed to `elytratrims-4.9.0.jar`; build/test fixture, runtime integration is optional) |
| `enderitemod-1.9.0.jar` | https://modrinth.com/mod/enderite-mod (Fabric 26.2 build, renamed to `enderitemod-1.9.0.jar`) |
| `fabric-language-kotlin-1.13.13.jar` | https://modrinth.com/mod/fabric-language-kotlin (test/runtime dependency of Elytra Trims, not a hard Elytra Compat dependency) |
| `architectury-21.0.4.jar` | https://modrinth.com/mod/architectury-api (Fabric) |
| `cloth-config-26.2.155.jar` | https://modrinth.com/mod/cloth-config (Fabric) |

The exact filenames matter - `build.gradle` references them by
`name:version` (e.g. `":enderitemod:1.9.0"`).
