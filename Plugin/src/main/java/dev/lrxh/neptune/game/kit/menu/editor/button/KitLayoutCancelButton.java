package dev.lrxh.neptune.game.kit.menu.editor.button;

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
        player.closeInventory();
    }

    @Override
    public ItemStack getItemStack(Player player) {
        return new ItemBuilder(Material.RED_WOOL)
                .name("&c&lCANCEL")
                .lore("&7Click to cancel without saving")
                .build();
    }
}
