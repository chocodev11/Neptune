package dev.lrxh.neptune.game.arena;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import dev.lrxh.api.arena.IArena;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

/**
 * Represents a duplicated arena in the real world at an offset position.
 * Replaces VirtualArena that used BlockChanger virtual worlds.
 */
@Getter
public class DuplicatedArena implements IArena {
    private final String name;
    private final String displayName;
    private final boolean enabled;
    private final int deathY;
    private final double buildLimit;
    private final List<Material> whitelistedBlocks;
    private final IArena owner;
    private final Clipboard clipboard;
    private final int slotIndex;
    private final BlockVector3 offset;

    @Setter
    private Location redSpawn;
    @Setter
    private Location blueSpawn;
    @Setter
    private Location min;
    @Setter
    private Location max;

    public DuplicatedArena(String name, String displayName,
            Location redSpawn, Location blueSpawn,
            Location min, Location max,
            double buildLimit, boolean enabled,
            List<Material> whitelistedBlocks, int deathY,
            IArena owner, Clipboard clipboard,
            int slotIndex, BlockVector3 offset) {
        this.name = name;
        this.displayName = displayName;
        this.redSpawn = redSpawn;
        this.blueSpawn = blueSpawn;
        this.min = min;
        this.max = max;
        this.buildLimit = buildLimit;
        this.enabled = enabled;
        this.whitelistedBlocks = whitelistedBlocks;
        this.deathY = deathY;
        this.owner = owner;
        this.clipboard = clipboard;
        this.slotIndex = slotIndex;
        this.offset = offset;
    }

    @Override
    public boolean isSetup() {
        return redSpawn != null && blueSpawn != null && min != null && max != null && clipboard != null;
    }

    @Override
    public void remove() {
        FAWEArenaManager.get().releaseSlot(slotIndex);
    }

    @Override
    public void restore() {
        if (clipboard != null && min != null && min.getWorld() != null) {
            FAWEArenaManager.get().restoreArena(clipboard, min.getWorld(), offset);
        }
    }

    @Override
    public void delete(boolean save) {
        remove();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DuplicatedArena that = (DuplicatedArena) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
