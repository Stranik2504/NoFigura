package org.figuramc.figura.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaCompatTextureTest {

    @Test
    void legacyModelTexturePaths() {
        assertEquals("textures/entity/beacon/beacon_beam.png", VanillaCompat.Textures.modelTexturePath("beacon_beam"));
        assertEquals("textures/entity/rabbit/rabbit_white.png", VanillaCompat.Textures.legacyResourcePath("textures/entity/rabbit/white.png"));
        assertEquals("textures/block/oak_hanging_sign.png", VanillaCompat.Textures.legacyResourcePath("textures/entity/signs/hanging/oak.png"));
        assertEquals("textures/entity/equipment/humanoid/leather.png", VanillaCompat.Textures.legacyResourcePath("textures/models/armor/leather_layer_1.png"));
    }

}
