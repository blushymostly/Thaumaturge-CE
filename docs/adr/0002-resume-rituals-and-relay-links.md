# Persist ritual progress and vis relay links

New infusion jobs save their input quote and remaining steps. On first tick a legacy Forge job starts again from the intact altar, since the old job format consumed its resources only at completion. Relays save their selected parent and withhold output whenever the path cannot be resolved in loaded chunks.

Ordinary CE26 world generation applies during new chunk generation. Existing chunk terrain and the Labyrinth and Outer Lands stay outside this migration.
