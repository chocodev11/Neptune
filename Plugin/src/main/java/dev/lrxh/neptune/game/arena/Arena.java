package dev.lrxh.neptune.game.arena;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import dev.lrxh.api.arena.IArena;
import dev.lrxh.neptune.game.kit.KitService;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class Arena implements IArena {
    private String name;
    private String displayName;
    private Location redSpawn;
    private Location blueSpawn;
    private boolean enabled;
    private int deathY;
    private Location min;
    private Location max;
    private double buildLimit;
    private List<Material> whitelistedBlocks;
    private Clipboard clipboard;
    private Arena owner;
    private boolean doneLoading;

    public Arena(String name, String displayName, Location redSpawn, Location blueSpawn, boolean enabled, int deathY) {
        this.name = name;
        this.displayName = displayName;
        this.redSpawn = redSpawn;
        this.blueSpawn = blueSpawn;
        this.enabled = enabled;
        this.deathY = deathY;

        this.buildLimit = 0;
        this.whitelistedBlocks = new ArrayList<>();
        this.doneLoading = true;
    }

    public Arena(String name, String displayName, Location redSpawn, Location blueSpawn,
            Location min, Location max, double buildLimit, boolean enabled,
            List<Material> whitelistedBlocks, int deathY) {

        this(name, displayName, redSpawn, blueSpawn, enabled, deathY);
        this.min = min;
        this.max = max;
        this.buildLimit = buildLimit;
        this.whitelistedBlocks = (whitelistedBlocks != null ? whitelistedBlocks : new ArrayList<>());

        if (min != null && max != null) {
            this.doneLoading = false;
            FAWEArenaManager.get().copyRegion(min, max).thenAccept(clip -> {
                // Save to disk immediately and free RAM
                FAWEArenaManager.get().saveSchematic(clip, this.name);
                this.clipboard = null;
                this.doneLoading = true;
            });
        }
    }

    public Arena(String name, String displayName, Location redSpawn, Location blueSpawn,
            Location min, Location max, double buildLimit, boolean enabled,
            List<Material> whitelistedBlocks, int deathY, Clipboard clipboard, Arena owner) {

        this(name, displayName, redSpawn, blueSpawn, min, max, buildLimit, enabled, whitelistedBlocks, deathY);
        // If provided a clipboard (e.g. from conversion), save it and clear it.
        if (clipboard != null) {
            FAWEArenaManager.get().saveSchematic(clipboard, name);
            this.clipboard = null;
            this.doneLoading = true;
        } else {
            this.clipboard = null;
            this.owner = owner;
            this.doneLoading = true;
        }
    }

    public Arena(String name) {
        this(name, name, null, null, false, -68321);
        this.min = null;
        this.max = null;
        this.buildLimit = 68321;
        this.whitelistedBlocks = new ArrayList<>();
    }

    @Override
    public boolean isSetup() {
        return !(redSpawn == null || blueSpawn == null || min == null || max == null);
    }

    @Override
    public void remove() {
        // Empty for base Arena
    }

    /**
     * Create a duplicate of this arena at an offset position using FAWE.
     */
    public synchronized CompletableFuture<DuplicatedArena> createDuplicate() {
        CompletableFuture<DuplicatedArena> future = new CompletableFuture<>();
        UUID uuid = UUID.randomUUID();

        // Allocate a slot for this duplicate
        int slotIndex = FAWEArenaManager.get().allocateSlot();
        if (slotIndex == -1) {
            future.completeExceptionally(new RuntimeException("No arena slots available"));
            return future;
        }

        // Calculate offset for this slot
        BlockVector3 offset = FAWEArenaManager.get().getOffsetForSlot(slotIndex);

        // 1. Load schematic from disk (Async)
        FAWEArenaManager.get().loadSchematic(this.name).thenCompose(loadedClipboard -> {
            if (loadedClipboard == null) {
                throw new RuntimeException("Schematic not found for arena: " + this.name);
            }

            // 2. Paste the loaded clipboard
            return FAWEArenaManager.get().pasteClipboard(loadedClipboard, this.min.getWorld(), offset)
                    .thenApply(v -> loadedClipboard); // Pass clipboard down the chain if needed, or just proceed
        }).thenAccept(ignored -> {
            try {
                // Calculate new locations with offset applied
                Location newMin = this.min.clone().add(offset.x(), offset.y(), offset.z());
                Location newMax = this.max.clone().add(offset.x(), offset.y(), offset.z());
                Location newRedSpawn = this.redSpawn.clone().add(offset.x(), offset.y(), offset.z());
                Location newBlueSpawn = this.blueSpawn.clone().add(offset.x(), offset.y(), offset.z());

                String dupName = this.name + "_" + uuid;

                DuplicatedArena duplicate = new DuplicatedArena(
                        dupName,
                        this.displayName,
                        newRedSpawn,
                        newBlueSpawn,
                        newMin,
                        newMax,
                        this.buildLimit,
                        this.enabled,
                        new ArrayList<>(this.whitelistedBlocks),
                        this.deathY,
                        this,
                        null, // Duplicates don't need to hold the clipboard either
                        slotIndex,
                        offset);

                future.complete(duplicate);
            } catch (Exception ex) {
                FAWEArenaManager.get().releaseSlot(slotIndex);
                future.completeExceptionally(ex);
            }
        }).exceptionally(ex -> {
            FAWEArenaManager.get().releaseSlot(slotIndex);
            future.completeExceptionally(ex);
            return null;
        });

        return future;
    }

    public List<String> getWhitelistedBlocksAsString() {
        List<String> result = new ArrayList<>();
        for (Material mat : whitelistedBlocks) {
            result.add(mat.name());
        }
        return result;
    }

    public void restore() {
        if (clipboard != null && min != null) {
            FAWEArenaManager.get().restoreArena(clipboard, min.getWorld(), BlockVector3.ZERO);
        }
    }

    public void setMin(Location min) {
        this.min = min;
        if (min != null && max != null) {
            this.doneLoading = false;
            FAWEArenaManager.get().copyRegion(min, max).thenAccept(clip -> {
                this.clipboard = clip;
                this.doneLoading = true;
            });
        }
    }

    public void setMax(Location max) {
        this.max = max;
        if (min != null && max != null) {
            this.doneLoading = false;
            FAWEArenaManager.get().copyRegion(min, max).thenAccept(clip -> {
                this.clipboard = clip;
                this.doneLoading = true;
            });
        }
    }

    public void setRedSpawn(Location redSpawn) {
        this.redSpawn = redSpawn;
        if (buildLimit == 68321) {
            this.buildLimit = redSpawn.getBlockY() + 5;
        }
    }

    public void setBlueSpawn(Location blueSpawn) {
        this.blueSpawn = blueSpawn;
        if (buildLimit == 68321) {
            this.buildLimit = blueSpawn.getBlockY() + 5;
        }
    }

    public void delete(boolean save) {
        KitService.get().removeArenasFromKits(this);
        ArenaService.get().arenas.remove(this);

        if (save)
            ArenaService.get().save();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Arena arena) {
            return arena.getName().equals(name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
