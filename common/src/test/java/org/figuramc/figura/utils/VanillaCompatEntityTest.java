package org.figuramc.figura.utils;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class VanillaCompatEntityTest {

    @Test
    void blockAttachedDefaultsAreAdded() {
        CompoundTag tag = new CompoundTag();
        VanillaCompat.Entities.addBlockAttachedDefaults(tag);
        assertNotNull(tag.get("block_pos"));
        assertNotNull(tag.get("facing"));
    }
}
