# Changelog

## 2026-04-21

- Fixed Bukkit startup failure on modern Paper/CraftBukkit where reflection version parsing could throw `NumberFormatException` for non-legacy package tokens.
- Added resilient version resolution with multi-source fallback and unsupported-version diagnostics.
- Added startup safeguards to avoid cascading reflection initialization failures during command restore and reload flows.
