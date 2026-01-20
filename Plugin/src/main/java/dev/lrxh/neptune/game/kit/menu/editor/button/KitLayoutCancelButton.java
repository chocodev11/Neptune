package dev.lrxh.neptune.game.kit.menu.editor.button;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.configs.impl.MenusLocale;
import dev.lrxh.neptune.profile.data.ProfileState;
import dev.lrxh.neptune.profile.impl.Profile;
import dev.lrxh.neptune.utils.CC;
import dev.lrxh.neptune.utils.ItemBuilder;
import dev.lrxh.neptune.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * Button to cancel kit editing without saving.
 */
public class KitLayoutCancelButton extends Button {

    public KitLayoutCancelButton(int slot) {
        super(slot);
    }

    @Override
    public void onClick(ClickType type, Player player) {
        Profile profile = API.getProfile(player);
        if (profile != null) {
            // Reset kit editor state
            profile.setState(ProfileState.IN_LOBBY);
            profile.getGameData().setKitEditor(null);
        }
        player.closeInventory();
    }

    @Override
    public ItemStack getItemStack(Player player) {
        Material material = Material.getMaterial(MenusLocale.KIT_EDITOR_LAYOUT_CANCEL_MATERIAL.getString());
        if (material == null)
            material = Material.RED_WOOL;

        return new ItemBuilder(material)
                .name(MenusLocale.KIT_EDITOR_LAYOUT_CANCEL_NAME.getString())
                .lore(MenusLocale.KIT_EDITOR_LAYOUT_CANCEL_LORE.getStringList())
                .build();
    }
}
