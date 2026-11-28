package org.figuramc.figura.utils;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.Badges;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.lua.api.nameplate.EntityNameplateCustomization;
import org.figuramc.figura.permissions.Permissions;

import java.util.UUID;
import java.util.regex.Pattern;

public class NameplateUtils {
    public static Component buildEntityName(Component text, Component name, Avatar avatar, UUID id) {
        int config = Configs.ENTITY_NAMEPLATE.value;
        if (config == 0 || AvatarManager.panic)
            return text;

        EntityNameplateCustomization custom = getCustom(avatar);
        Component replacement = custom != null && custom.getJson() != null ? custom.getJson().copy() : name;
        replacement = TextUtils.replaceInText(replacement, "\\$\\{name\\}", name);

        if (id != null)
            replacement = Badges.appendBadges(replacement, id, config > 1);

        if (text.getString().equals(name.getString()))
            return replacement;

        return TextUtils.replaceInText(text, "\\b" + Pattern.quote(name.getString()) + "\\b", replacement);
    }

    public static EntityNameplateCustomization getCustom(Avatar avatar) {
        if (Configs.ENTITY_NAMEPLATE.value == 0 || AvatarManager.panic || avatar == null || avatar.luaRuntime == null
                || avatar.permissions.get(Permissions.NAMEPLATE_EDIT) != 1)
            return null;
        return avatar.luaRuntime.nameplate.ENTITY;
    }
}
