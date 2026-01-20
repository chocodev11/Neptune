package dev.lrxh.neptune.game.kit.menu.editor;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.configs.impl.MenusLocale;
import dev.lrxh.neptune.game.kit.Kit;
import dev.lrxh.neptune.game.kit.menu.editor.button.KitLayoutCancelButton;
import dev.lrxh.neptune.game.kit.menu.editor.button.KitLayoutItemButton;
import dev.lrxh.neptune.game.kit.menu.editor.button.KitLayoutResetButton;
import dev.lrxh.neptune.game.kit.menu.editor.button.KitLayoutSaveButton;
import dev.lrxh.neptune.profile.data.KitData;
import dev.lrxh.neptune.profile.impl.Profile;
import dev.lrxh.neptune.utils.menu.Button;
import dev.lrxh.neptune.utils.menu.Filter;
import dev.lrxh.neptune.utils.menu.Menu;
import dev.lrxh.neptune.utils.menu.impl.DisplayButton;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI-based kit layout editor.
 * 
 * Layout (54 slots / 6 rows):
 * - Slots 0-35: Kit items (main inventory - 36 slots)
 * - Slots 36-39: Armor slots (boots, leggings, chestplate, helmet)
 * - Slot 40: Offhand item
 * - Slots 41-50: Black glass (decorative border)
 * - Configurable slots for save, reset, cancel buttons
 * 
 * Editable area: Slots 0-40 (items can be dragged within this area)
 */
@Getter
public class KitLayoutEditorMenu extends Menu {
    private final Kit kit;

    // Editable area bounds (0-40 inclusive)
    public static final int EDITABLE_START = 0;
    public static final int EDITABLE_END = 40;

    public KitLayoutEditorMenu(Kit kit) {
        super(MenusLocale.KIT_EDITOR_LAYOUT_TITLE.getString().replace("<kit>", kit.getDisplayName()), 54, Filter.NONE);
        this.kit = kit;
    }

    @Override
    public List<Button> getButtons(Player player) {
        List<Button> buttons = new ArrayList<>();

        Profile profile = API.getProfile(player);
        if (profile == null)
            return buttons;

        KitData kitData = profile.getGameData().get(kit);

        // Get the current layout (custom or default)
        List<ItemStack> currentLayout;
        if (kitData != null && kitData.getKitLoadout() != null && !kitData.getKitLoadout().isEmpty()) {
            currentLayout = kitData.getKitLoadout();
        } else {
            currentLayout = kit.getItems();
        }

        // Add kit items to slots 0-40 (main inventory + armor + offhand)
        // Layout: 0-35 = inventory, 36-39 = armor, 40 = offhand
        for (int i = 0; i <= EDITABLE_END; i++) {
            ItemStack item = null;
            if (currentLayout != null && i < currentLayout.size()) {
                item = currentLayout.get(i);
            }

            if (item != null && item.getType() != Material.AIR) {
                buttons.add(new KitLayoutItemButton(i, item.clone()));
            }
            // Empty slots remain empty (no button) to allow item placement
        }

        // Get button slots from config
        int saveSlot = MenusLocale.KIT_EDITOR_LAYOUT_SAVE_SLOT.getInt();
        int resetSlot = MenusLocale.KIT_EDITOR_LAYOUT_RESET_SLOT.getInt();
        int cancelSlot = MenusLocale.KIT_EDITOR_LAYOUT_CANCEL_SLOT.getInt();

        // Slots 41-50: Black stained glass pane (decorative border)
        // Skip slots used by buttons
        for (int i = 41; i <= 53; i++) {
            if (i != saveSlot && i != resetSlot && i != cancelSlot) {
                buttons.add(new DisplayButton(i, Material.BLACK_STAINED_GLASS_PANE, " "));
            }
        }

        // Add configurable buttons
        buttons.add(new KitLayoutSaveButton(saveSlot, kit, this));
        buttons.add(new KitLayoutResetButton(resetSlot, kit));
        buttons.add(new KitLayoutCancelButton(cancelSlot));

        return buttons;
    }

    /**
     * Check if a slot is in the editable area (0-40).
     */
    public static boolean isEditableSlot(int slot) {
        return slot >= EDITABLE_START && slot <= EDITABLE_END;
    }
}
