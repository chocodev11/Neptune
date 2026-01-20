package dev.lrxh.neptune.game.kit.menu.editor.button;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.configs.impl.MenusLocale;
import dev.lrxh.neptune.configs.impl.MessagesLocale;
import dev.lrxh.neptune.game.kit.Kit;
import dev.lrxh.neptune.game.kit.menu.editor.KitLayoutEditorMenu;
import dev.lrxh.neptune.profile.data.ProfileState;
import dev.lrxh.neptune.profile.impl.Profile;
import dev.lrxh.neptune.providers.clickable.Replacement;
import dev.lrxh.neptune.utils.ItemBuilder;
import dev.lrxh.neptune.utils.menu.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Button to save the kit layout from the editor GUI.
 */
public class KitLayoutSaveButton extends Button {
    private final Kit kit;
    private final KitLayoutEditorMenu menu;

    public KitLayoutSaveButton(int slot, Kit kit, KitLayoutEditorMenu menu) {
        super(slot);
        this.kit = kit;
        this.menu = menu;
    }

    @Override
    public void onClick(ClickType type, Player player) {
        Profile profile = API.getProfile(player);
        if (profile == null)
            return;

        Inventory inv = player.getOpenInventory().getTopInventory();

        // Extract items from the editable area (slots 0-40)
        // 0-35 = inventory, 36-39 = armor, 40 = offhand
        List<ItemStack> loadout = new ArrayList<>();
        for (int i = 0; i <= KitLayoutEditorMenu.EDITABLE_END; i++) {
            ItemStack item = inv.getItem(i);
            loadout.add(item != null ? item.clone() : null);
        }

        // Save the layout
        profile.getGameData().get(kit).setKitLoadout(loadout);
        Profile.save(profile);

        // Reset kit editor state
        profile.setState(ProfileState.IN_LOBBY);
        profile.getGameData().setKitEditor(null);

        player.closeInventory();
        MessagesLocale.KIT_EDITOR_STOP.send(player.getUniqueId(), new Replacement("<kit>", kit.getDisplayName()));
    }

    @Override
    public ItemStack getItemStack(Player player) {
        Material material = Material.getMaterial(MenusLocale.KIT_EDITOR_LAYOUT_SAVE_MATERIAL.getString());
        if (material == null)
            material = Material.LIME_WOOL;

        return new ItemBuilder(material)
                .name(MenusLocale.KIT_EDITOR_LAYOUT_SAVE_NAME.getString())
                .lore(MenusLocale.KIT_EDITOR_LAYOUT_SAVE_LORE.getStringList())
                .build();
    }
}
