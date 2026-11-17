# Restore node biomes from the world generator

CE26 restores matching biome cells around a removed or reclassified node by sampling the current world generator. Forge uses the same rule for loaded chunks. This keeps restoration consistent with CE26 and avoids storing ownership and original biome columns in saved worlds.

A removed node can restore cells that another node also affects. A change in an unloaded chunk can persist after the node is gone. The generator determines what returns, including in columns that previously contained a custom biome edit.
