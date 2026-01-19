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
    private int offsetX = 250;
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

    // Single-threaded executor to ensure all FAWE operations happen sequentially
    private final java.util.concurrent.ExecutorService faweExecutor = java.util.concurrent.Executors
            .newSingleThreadExecutor();

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
                    .fastMode(true) // Enable FastMode for copy
                    .maxBlocks(-1)
                    .build()) {

                ForwardExtentCopy copy = new ForwardExtentCopy(
                        editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingEntities(true);
                Operations.complete(copy);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return clipboard;
        }, faweExecutor);
    }

    /**
     * Paste a clipboard at an offset position asynchronously.
     */
    public CompletableFuture<Void> pasteClipboard(Clipboard clipboard, org.bukkit.World bukkitWorld,
            BlockVector3 offset) {

        // 1. Pre-load chunks on the main thread
        BlockVector3 origin = clipboard.getOrigin();
        BlockVector3 pasteAt = origin.add(offset);

        int minX = pasteAt.getBlockX() >> 4;
        int minZ = pasteAt.getBlockZ() >> 4;
        int maxX = (pasteAt.getBlockX() + clipboard.getRegion().getWidth()) >> 4;
        int maxZ = (pasteAt.getBlockZ() + clipboard.getRegion().getLength()) >> 4;

        CompletableFuture<Void> chunkLoadFuture = CompletableFuture.runAsync(() -> {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    bukkitWorld.getChunkAtAsync(x, z).join();
                }
            }
        }, org.bukkit.Bukkit.getScheduler().getMainThreadExecutor(dev.lrxh.neptune.Neptune.get()));

        // 2. Run paste after chunks are ready, strictly sequentially using faweExecutor
        return chunkLoadFuture.thenRunAsync(() -> {
            World world = BukkitAdapter.adapt(bukkitWorld);

            try (EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(world)
                    .fastMode(true) // Enable FastMode
                    .checkMemory(false) // Disable memory checks for speed
                    .allowedRegionsEverywhere()
                    .limitUnlimited()
                    .maxBlocks(-1)
                    .build()) {

                try (ClipboardHolder holder = new ClipboardHolder(clipboard)) {
                    Operation operation = holder
                            .createPaste(editSession)
                            .to(pasteAt)
                            .ignoreAirBlocks(true) // Optimization
                            .build();

                    Operations.complete(operation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, faweExecutor);
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
                    .fastMode(true)
                    .maxBlocks(-1)
                    .build()) {

                editSession.setBlocks((com.sk89q.worldedit.regions.Region) region,
                        BukkitAdapter.adapt(org.bukkit.Material.AIR.createBlockData()));
            }
        }, faweExecutor);
    }

    /**
     * Save a clipboard to a schematic file on disk.
     */
    public CompletableFuture<Void> saveSchematic(Clipboard clipboard, String arenaName) {
        return CompletableFuture.runAsync(() -> {
            java.io.File file = new java.io.File(dev.lrxh.neptune.Neptune.get().getDataFolder(),
                    "schematics/" + arenaName + ".schem");
            file.getParentFile().mkdirs();

            try (com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter writer = com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat.SPONGE_SCHEMATIC
                    .getWriter(new java.io.FileOutputStream(file))) {
                writer.write(clipboard);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }, faweExecutor);
    }

    /**
     * Load a schematic file from disk asynchronously.
     */
    public CompletableFuture<Clipboard> loadSchematic(String arenaName) {
        return CompletableFuture.supplyAsync(() -> {
            java.io.File file = new java.io.File(dev.lrxh.neptune.Neptune.get().getDataFolder(),
                    "schematics/" + arenaName + ".schem");
            if (!file.exists())
                return null;

            try (com.sk89q.worldedit.extent.clipboard.io.ClipboardReader reader = com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat.SPONGE_SCHEMATIC
                    .getReader(new java.io.FileInputStream(file))) {
                return reader.read();
            } catch (java.io.IOException e) {
                e.printStackTrace();
                return null;
            }
        }, faweExecutor);
    }

    /**
     * Shutdown the FAWE executor service.
     * Should be called on plugin disable to prevent thread leaks.
     */
    public void shutdown() {
        faweExecutor.shutdown();
        try {
            if (!faweExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                faweExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            faweExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
