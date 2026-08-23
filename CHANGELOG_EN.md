# Changelog

## 1.1.0

### Modular Refactor

- Split the project into separate `common` and Forge modules.
- Separated the API, configuration, inventory services, edit services, compatibility layer, platform events, and Mixins.
- Unified placement, replacement, stacking, quick-transfer, and dropped-item handling rules.
- Separated container automation from inventory interaction logic to reduce cross-module coupling.

### Edit Mode

- Added the `/petiteinventory edit` edit-mode toggle.
- Added eight-direction item resizing from edges and corners; the red border appears only over a resizable region.
- Added Ctrl multi-selection, drag selection, and Shift range selection.
- Added a preset color palette opened with right-click, including batch color editing.
- Holding right-click for 0.5 seconds enters batch footprint mode and allows an area to be assigned to multiple items.
- Added a Petite layout toggle at the top of every container Screen.
- Added `/petiteinventory blacklist` and `/petiteinventory whitelist` to switch the default Screen-layout behavior.

### Inventory and Interaction

- Added mouse-wheel and `R`-key rotation for non-square item instances.
- Item replacement now supports both larger-to-smaller and smaller-to-larger exchanges.
- Right-click placement now validates the complete footprint before vanilla click handling, preventing out-of-bounds, blocked, or space-inadequate placement.
- Shift quick transfer searches the hotbar first and the main inventory second, while checking the item's complete footprint.
- Kept the hotbar separate from the Petite main-inventory layout; multi-slot items cannot span the two groups.
- Fixed grid-width calculation for storages with an even number of columns and corrected occupancy checks during item merging.
- Fixed duplicate results when quick-moving outputs from merchants, anvils, smithing tables, and similar result slots.
- Improved whole-item hover, highlight, and footprint rendering to avoid rendering multi-slot items repeatedly.
- Added footprint-aware insertion for hoppers and other automation paths.

### Mod Compatibility

- Improved Sophisticated Core and Sophisticated Backpacks support, including actual footprint rendering, occupancy validation, and footprint-aware quick transfer.
- Separated Sophisticated storage slots from the player's main inventory and hotbar.
- Added size and background-color matching for TACZ `modern_kinetic_gun` items through their `GunId` NBT.
- Retained the KubeJS event bridge.

### Configuration and Distribution

- Added `border_items.json`, `border_colors.json`, `screen_layouts.json`, and `screen_layout_mode.json`.
- Item background slots are cropped dynamically from the current inventory texture for resource-pack compatibility.
- Removed the fixed `area.png` asset.
- Removed the old `/size` and color-editing commands; use edit mode and configuration files instead.
- Changed the project license to GNU AGPL v3.0.
