package dev.lrxh.neptune.game.kit.menu.editor.button;

import dev.lrxh.neptune.utils.menu.Button;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A movable button that represents a kit item in the layout editor.
 * Items can be dragged around within the editable area.
 */
@Getter
public class KitLayoutItemButton extends Button {
    private final ItemStack item;

    public KitLayoutItemButton(int slot, ItemStack item) {
        super(slot, true); // moveAble = true
        this.item = item;
    }

    @Override
    public ItemStack getItemStack(Player player) {
        return item;
    }
}
