# Persist ritual progress and vis relay links

New infusion jobs save their input quote and remaining steps. On first tick a legacy Forge job starts again from the intact altar, since the old job format consumed its resources only at completion. Relays save their selected parent and withhold output whenever the path cannot be resolved in loaded chunks.

Node capture charges the wand when the ritual starts. The node saves the expected states of the remaining shell blocks and the timing for their removal. After a reload, the ritual resumes with loaded chunks and skips any block whose state has changed. The node withholds vis until it turns into the jar, so the saved result keeps the captured stock.

Ordinary CE26 world generation applies during new chunk generation. Existing chunk terrain and the Labyrinth and Outer Lands stay outside this migration.
