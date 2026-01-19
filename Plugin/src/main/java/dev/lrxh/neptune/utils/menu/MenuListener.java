package dev.lrxh.neptune.utils.menu;

import dev.lrxh.neptune.API;
import dev.lrxh.neptune.game.kit.menu.editor.KitLayoutEditorMenu;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onButtonPress(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Menu menu = MenuService.get().get(player);
        if (menu == null)
            return;

        // Handle KitLayoutEditorMenu specially
        if (menu instanceof KitLayoutEditorMenu) {
            handleKitLayoutEditorClick(event, player, menu);
            return;
        }

        // Standard menu handling
        if (event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getOpenInventory().getTopInventory())) {
            Button button = menu.getButton(event.getSlot());

            if (button == null || !button.isMoveAble()) {
                event.setCancelled(true);
            }

            if (button != null) {
                if (API.getProfile(player).getSettingData().isMenuSound()) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                }

                button.onClick(event.getClick(), player);
                if (menu.isUpdateOnClick()) {
                    menu.update(player);
                }
            }
        } else {
            event.setCancelled(true);
        }
    }

    /**
     * Handle click events for KitLayoutEditorMenu.
     * - Slots 0-40: Editable (allow item movement within this area only)
     * - Slots 41-53: Control buttons and glass panes
     * - Block any interaction with player inventory
     * - Block dropping items
     */
    private void handleKitLayoutEditorClick(InventoryClickEvent event, Player player, Menu menu) {
        boolean isTopInventory = event.getClickedInventory() != null
                && event.getClickedInventory().equals(player.getOpenInventory().getTopInventory());

        // Block clicking in player's own inventory
        if (!isTopInventory) {
            event.setCancelled(true);
            return;
        }

        // Block DROP actions
        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getSlot();

        // Editable area (0-40): Allow item movement
        if (KitLayoutEditorMenu.isEditableSlot(slot)) {
            // Check if trying to shift-click (which would move to player inventory)
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
            }
            // Allow normal click/drag within editable area
            return;
        }

        // Non-editable area (41-53): Handle as buttons
        event.setCancelled(true);
        Button button = menu.getButton(slot);
        if (button != null) {
            if (API.getProfile(player).getSettingData().isMenuSound()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            button.onClick(event.getClick(), player);
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;
        MenuService.get().remove(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MenuService.get().remove(event.getPlayer());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        Menu menu = MenuService.get().get(player);
        if (menu == null)
            return;

        // For KitLayoutEditorMenu, allow dragging only within editable slots (0-40)
        if (menu instanceof KitLayoutEditorMenu) {
            for (int slot : event.getRawSlots()) {
                // Raw slots >= 54 are in the player's inventory (bottom)
                // We only allow dragging within the editable area (slots 0-40)
                if (slot >= 54 || !KitLayoutEditorMenu.isEditableSlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            // Allow the drag within editable area
            return;
        }

        // For all other menus, cancel drag events
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Menu menu = MenuService.get().get(player);

        // Block dropping items while in KitLayoutEditorMenu
        if (menu instanceof KitLayoutEditorMenu) {
            event.setCancelled(true);
        }
    }
}
