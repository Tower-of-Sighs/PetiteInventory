# Architecture

The repository is split into a Java 8 `common` library and loader/version
targets under `targets/`. The default ownership rule is deliberately broad:
code stays in `common` unless it directly calls Minecraft, Forge, a third-party
mod API, or a version-specific runtime contract.

## Common

`common` owns the inventory domain and all reusable behavior:

- `core` contains value objects and footprint math.
- `inventory` contains `Area`, `BorderTheme`, edit-mode state, admission result,
  the generic `ContainerGrid`, and generic stacking algorithms.
- `service` contains admission policy, runtime subscriptions, item-footprint
  parsing, and rotation rules.
- `spi` contains the ports for stacks, inventory slots, grid slots, platform
  providers, and other target boundaries.
- `event` contains the synchronous loader-neutral event bus and shared events.
- `api` contains stable public value types.

Common code must not import Minecraft, Forge, Mixin, networking, rendering,
configuration backends, or optional integrations.

## Targets

Targets contain only adapters and behavior proven to depend on a target API.
For Forge 1.20.1 this means:

- `platform.spi` adapts `ItemStack`, player inventories, menu slots, and grid
  slots to the common ports.
- `platform.inventory` adapts Forge menus, hoppers, registries, configuration,
  and Sophisticated Core. Its `ContainerGrid` and `ContainerStackingService`
  are thin adapters over common algorithms.
- `platform.mixin` only translates game callbacks into `InventoryEvents` or
  delegates to target services; it does not own inventory policy.
- `platform`, `bootstrap`, `client`, `config`, and `compat` contain lifecycle,
  UI, persistence, and optional third-party integration code respectively.

There is intentionally no target `com.sighs.petiteinventory.inventory` package.
When a future target needs a different implementation, it should implement a
common SPI in its own target package rather than moving reusable logic out of
`common`.

## Event flow

Mixins and loader callbacks publish `InventoryEvents` to the internal bus.
`PlatformServices` discovers the target provider with `ServiceLoader`; the
provider installs one `InventoryRuntime` and target-only subscribers. Shared
admission, close-defense, grid, stacking, and footprint logic is therefore
reused by every target while only API translation remains version-specific.
