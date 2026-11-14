package com.leclowndu93150.thaumaturge.client.screen.research.detail.bookmark;

import com.leclowndu93150.thaumaturge.client.screen.research.detail.Rect;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record BookmarkSlot(BookmarkKind kind, @Nullable ResourceLocation recipeId, Rect hoverArea) {}
