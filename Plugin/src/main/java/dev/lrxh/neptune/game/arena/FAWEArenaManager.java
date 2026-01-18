package dev.lrxh.neptune.game.arena;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import dev.lrxh.neptune.configs.ConfigService;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;

/**
 * Manages arena clipboard operations using FAWE (FastAsyncWorldEdit).
 * Handles copying, pasting, and restoring arena blocks with offset support.
 */
@Getter
public class FAWEArenaManager {
    private static FAWEArenaManager instance;

    // Configuration for arena duplication offset
    private int offsetX = 1000;
    private int offsetY = 0;
    private int offsetZ = 0;
    private int maxSlots = 50;

    // Slot allocation tracking
    private final BitSet usedSlots = new BitSet();

    private FAWEArenaManager() {
    }

    public static FAWEArenaManager get() {
        if (instance == null) {
            instance = new FAWEArenaManager();
        }
        return instance;
    }

    /**
     * Load configuration from arena-settings.yml file.
     */
    public void loadConfig() {
        YamlConfiguration config = ConfigService.get().getArenaSettingsConfig().getConfiguration();

        this.offsetX = config.getInt("duplication.offset-x", 1000);
        this.offsetY = config.getInt("duplication.offset-y", 0);
        this.offsetZ = config.getInt("duplication.offset-z", 0);
        this.maxSlots = config.getInt("duplication.max-slots", 50);
    }

    /**
     * Configure the arena duplication offset and max slots programmatically.
     */
    public void configure(int offsetX, int offsetY, int offsetZ, int maxSlots) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.maxSlots = maxSlots;
    }

    /**
     * Allocates a slot for arena duplication.
     * 
     * @return slot index, or -1 if no slots available
     */
    public synchronized int allocateSlot() {
        int slot = usedSlots.nextClearBit(0);
        if (slot >= maxSlots) {
            return -1;
        }
        usedSlots.set(slot);
        return slot;
    }

    /**
     * Releases a previously allocated slot.
     */
    public synchronized void releaseSlot(int slot) {
        if (slot >= 0 && slot < maxSlots) {
            usedSlots.clear(slot);
        }
    }

    /**
     * Calculate the offset position for a given slot.
     */
    public BlockVector3 getOffsetForSlot(int slot) {
        return BlockVector3.at(
                (long) (slot + 1) * offsetX,
                (long) (slot + 1) * offsetY,
                (long) (slot + 1) * offsetZ);
    }

    /**
     * Copy a region to a clipboard asynchronously.
     */
    public CompletableFuture<Clipboard> copyRegion(Location min, Location max) {
        return CompletableFuture.supplyAsync(() -> {
            World world = BukkitAdapter.adapt(min.getWorld());
            BlockVector3 minVec = BukkitAdapter.asBlockVector(min);
            BlockVector3 maxVec = BukkitAdapter.asBlockVector(max);

            CuboidRegion region = new CuboidRegion(world, minVec, maxVec);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(world)
                    .maxBlocks(-1)
                    .build()) {

                ForwardExtentCopy copy = new ForwardExtentCopy(
                        editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingEntities(false);
                Operations.complete(copy);
            }

            return clipboard;
        });
    }

    /**
     * Paste a clipboard at an offset position asynchronously.
     */
    public CompletableFuture<Void> pasteClipboard(Clipboard clipboard, org.bukkit.World bukkitWorld,
            BlockVector3 offset) {
        return CompletableFuture.runAsync(() -> {
            World world = BukkitAdapter.adapt(bukkitWorld);

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(world)
                    .maxBlocks(-1)
                    .build()) {

                ClipboardHolder holder = new ClipboardHolder(clipboard);
                BlockVector3 origin = clipboard.getOrigin();
                BlockVector3 pasteAt = origin.add(offset);

                Operation operation = holder
                        .createPaste(editSession)
                        .to(pasteAt)
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);
            }
        });
    }

    /**
     * Restore an arena by re-pasting the clipboard at the offset position.
     */
    public CompletableFuture<Void> restoreArena(Clipboard clipboard, org.bukkit.World bukkitWorld,
            BlockVector3 offset) {
        return pasteClipboard(clipboard, bukkitWorld, offset);
    }

    /**
     * Clear blocks in a region (set to air).
     */
    public CompletableFuture<Void> clearRegion(org.bukkit.World bukkitWorld, Location min, Location max) {
        return CompletableFuture.runAsync(() -> {
            World world = BukkitAdapter.adapt(bukkitWorld);
            BlockVector3 minVec = BukkitAdapter.asBlockVector(min);
            BlockVector3 maxVec = BukkitAdapter.asBlockVector(max);

            CuboidRegion region = new CuboidRegion(world, minVec, maxVec);

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(world)
                    .maxBlocks(-1)
                    .build()) {

                editSession.setBlocks((com.sk89q.worldedit.regions.Region) region,
                        BukkitAdapter.adapt(org.bukkit.Material.AIR.createBlockData()));
            }
        });
    }
}
