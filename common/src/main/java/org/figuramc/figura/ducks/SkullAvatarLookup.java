package org.figuramc.figura.ducks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
* Resolves which Avatar belongs to a skull that is being rendered.
* Avatars used to be stored on shared objects (the SkullBlockRenderer instance and the cached RenderType of a skin),
* which made the result depend on whichever skull was extracted last (wrong or flickering heads).
* Here the avatar is tied to the thing that is actually being drawn: the block render state or the item stack.
*/
public final class SkullAvatarLookup {
    private static final Map<Object, Optional<Avatar>> BLOCK_STATES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Avatar NONE = null;

    private static Avatar pendingSpecial;
    private static boolean hasPendingSpecial;

    public static void setPendingSpecial(@Nullable Avatar avatar) {
        pendingSpecial = avatar;
        hasPendingSpecial = true;
    }

    public static void clearPendingSpecial() {
        pendingSpecial = null;
        hasPendingSpecial = false;
    }

    private SkullAvatarLookup() {}

    public static void putBlock(Object blockRenderState, @Nullable Avatar avatar) {
        BLOCK_STATES.put(blockRenderState, Optional.ofNullable(avatar));
    }

    @Nullable
    public static Avatar fromItem(@Nullable ItemStack stack) {
        if (stack == null)
            return NONE;

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        return fromProfile(stack.get(DataComponents.PROFILE));
    }

    @Nullable
    public static Avatar fromProfile(@Nullable ResolvableProfile profile) {
        if (profile == null)
            return NONE;

        UUID id = profile.partialProfile().id();
        if (id != null && !Util.NIL_UUID.equals(id)) {
            Avatar avatar = AvatarManager.getAvatarForPlayer(id);
            if (avatar != null)
                return avatar;
        }

        String name = profile.partialProfile().name();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (name == null || name.isEmpty() || connection == null)
            return NONE;

        PlayerInfo info = connection.getPlayerInfo(name);
        return info == null ? NONE : AvatarManager.getAvatarForPlayer(info.getProfile().id());
    }

    /**
     * @param block    render state of a placed skull, or null
     * @param item     item stack of a held / worn / inventory skull, or null
      * @param fallback avatar found through the RenderType association
      */
    @Nullable
    public static Avatar resolve(@Nullable Object block, @Nullable ItemStack item, @Nullable Avatar fallback) {
        if (hasPendingSpecial) {
            Avatar special = pendingSpecial;
            clearPendingSpecial();
            return special;
        }

        if (block != null) {
            Optional<Avatar> tagged = BLOCK_STATES.get(block);
            return tagged != null ? tagged.orElse(null) : fallback;
        }

        if (item != null && item.get(DataComponents.PROFILE) != null)
            return fromItem(item);

        return fallback;
    }
}