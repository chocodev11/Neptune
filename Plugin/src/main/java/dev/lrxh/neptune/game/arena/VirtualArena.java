package dev.lrxh.neptune.game.arena;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import dev.lrxh.api.arena.IArena;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;

/**
 * VirtualArena is now an alias for DuplicatedArena.
 * This class exists for backward compatibility - new code should use
 * DuplicatedArena directly.
 * 
 * @deprecated Use {@link DuplicatedArena} instead
 */
@Deprecated
public class VirtualArena extends DuplicatedArena {

    public VirtualArena(String name, String displayName,
            Location redSpawn, Location blueSpawn,
            Location min, Location max,
            double buildLimit, boolean enabled,
            List<Material> whitelistedBlocks, int deathY,
            IArena owner, Clipboard clipboard,
            int slotIndex, BlockVector3 offset) {
        super(name, displayName, redSpawn, blueSpawn, min, max,
                buildLimit, enabled, whitelistedBlocks, deathY,
                owner, clipboard, slotIndex, offset);
    }
}
