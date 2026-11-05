## Folia / Paper

This fork runs on **Folia** and on **Paper 1.20+** (Spigot is no longer supported).

- All tasks use the Paper schedulers (global / entity / async) via `scheduler/Scheduler.java`.
- Displays (tablist, bossbar, scoreboard) and ambient particles are per-player entity tasks.
- Teleports use `teleportAsync`, console commands and world rules run on the global region.
- NoteBlockAPI join music is disabled on Folia (the plugin is not Folia-compatible).
