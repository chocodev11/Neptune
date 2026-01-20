package dev.lrxh.neptune.game.kit.menu.editor.button;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.configs.impl.MenusLocale;
import dev.lrxh.neptune.configs.impl.MessagesLocale;
import dev.lrxh.neptune.game.kit.Kit;
import dev.lrxh.neptune.game.kit.menu.editor.KitLayoutEditorMenu;
import dev.lrxh.neptune.profile.impl.Profile;
import dev.lrxh.neptune.providers.clickable.Replacement;
import dev.lrxh.neptune.utils.CC;
import dev.lrxh.neptune.utils.ItemBuilder;
import dev.lrxh.neptune.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * Button to reset kit layout to default kit items.
 */
public class KitLayoutResetButton extends Button {
    private final Kit kit;

    public KitLayoutResetButton(int slot, Kit kit) {
        super(slot);
        this.kit = kit;
    }

    @Override
    public void onClick(ClickType type, Player player) {
        Profile profile = API.getProfile(player);
        if (profile == null)
            return;

        // Reset to default kit items
        profile.getGameData().get(kit).setKitLoadout(kit.getItems());
        Profile.save(profile);

        MessagesLocale.KIT_EDITOR_RESET.send(player.getUniqueId(), new Replacement("<kit>", kit.getDisplayName()));

        // Reopen the menu to show default items
        new KitLayoutEditorMenu(kit).open(player);
    }

    @Override
    public ItemStack getItemStack(Player player) {
        Material material = Material.getMaterial(MenusLocale.KIT_EDITOR_LAYOUT_RESET_MATERIAL.getString());
        if (material == null)
            material = Material.BARRIER;

        return new ItemBuilder(material)
                .name(MenusLocale.KIT_EDITOR_LAYOUT_RESET_NAME.getString())
                .lore(MenusLocale.KIT_EDITOR_LAYOUT_RESET_LORE.getStringList())
                .build();
    }
}
