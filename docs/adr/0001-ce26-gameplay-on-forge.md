# Preserve the Forge registry IDs while porting CE26 gameplay

The Forge instance already has worlds containing the existing block and item IDs. Replace block implementations under those IDs and use Forge 1.20.1 block-entity NBT for persistent state. Existing placements start with empty storage or zero charge. Use loaded world state to resolve network connections so an unloaded chunk cannot supply vis or keep an invalid connection alive.

The workbench uses the CE26 payment order for each aspect: wand, relayed vis, crystal. Chunk aura covers the scalar cost. Existing Forge NBT and recipe fields retain their meaning, so this changes payment selection without a world or datapack migration.
