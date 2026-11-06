package org.figuramc.figura.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VanillaCompatBlockParticleTest {

    @Test
    void legacyBlockParticleAddsId() {
        assertEquals(
                "block{block_state:{id:\"minecraft:snow\"}}",
                VanillaCompat.Blocks.legacyBlockParticle("block", "minecraft:snow")
        );
    }

    @Test
    void legacyBlockParticleKeepsProperties() {
        assertEquals(
                "block{block_state:{id:\"minecraft:oak_log\",properties:{\"axis\":\"y\"}}}",
                VanillaCompat.Blocks.legacyBlockParticle("block", "minecraft:oak_log[axis=y]")
        );
    }

    @Test
    void legacyBlockParticleHandlesQuotedProperties() {
        assertEquals(
                "block{block_state:{id:\"minecraft:oak_log\",properties:{\"axis\":\"y\"}}}",
                VanillaCompat.Blocks.legacyBlockParticle("block", "minecraft:oak_log[axis=\"y\"]")
        );
    }

    @Test
    void particleOptionsSpaceIsNormalized() {
        assertEquals(
                "minecraft:flash{color:[1,0,0]}",
                VanillaCompat.Particles.normalizeOptions("minecraft:flash {color:[1,0,0]}")
        );
        assertEquals(
                "minecraft:flash{color:[1,0,0]}",
                VanillaCompat.Particles.normalizeOptions("minecraft:flash   {color:[1,0,0]}")
        );
    }

}
