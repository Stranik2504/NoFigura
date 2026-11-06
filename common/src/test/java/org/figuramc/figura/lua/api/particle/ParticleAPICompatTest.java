package org.figuramc.figura.lua.api.particle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleAPICompatTest {

    @Test
    void blockParticleUsesExactType() {
        assertEquals(
                "block{block_state:{id:\"minecraft:snow\"}}",
                ParticleAPI.convertOldToNewParticleFormat("minecraft:block minecraft:snow")
        );
        assertEquals(
                "block_marker{block_state:{id:\"minecraft:stone\"}}",
                ParticleAPI.convertOldToNewParticleFormat("minecraft:block_marker minecraft:stone")
        );
    }

    @Test
    void unknownTypeIsNotRewritten() {
        assertEquals(
                "minecraft:notblock minecraft:snow",
                ParticleAPI.convertOldToNewParticleFormat("minecraft:notblock minecraft:snow")
        );
    }

    @Test
    void dustAndItemKeepLegacyArguments() {
        assertEquals(
                "dust{color:[1.0,0.5,0.0],scale:2}",
                ParticleAPI.convertOldToNewParticleFormat("minecraft:dust 1 0.5 0 2")
        );
        assertEquals(
                "item{item:{id:minecraft:stone}}",
                ParticleAPI.convertOldToNewParticleFormat("minecraft:item minecraft:stone")
        );
    }
}
