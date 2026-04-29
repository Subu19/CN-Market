package net.craftnepal.market.world;

import net.craftnepal.market.Market;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages WorldEdit schematic pasting for market pathways.
 *
 * KEY DESIGN: ChunkLoadEvent only ENQUEUES chunks. A repeating task drains the queue ONE chunk per
 * tick. This means WorldEdit never pastes inside a ChunkLoadEvent callback, so it cannot trigger
 * new ChunkLoadEvents that re-enter the listener.
 */
public class SchematicManager {

    private static SchematicManager instance;
    private Object clipboard;
    private boolean worldEditPresent = false;
    private final NamespacedKey pastedKey;

    /** Chunks waiting to have the schematic pasted. */
    private final Queue<PendingChunk> pasteQueue = new ArrayDeque<>();

    /** Chunk keys currently in the queue (avoid duplicate enqueues). */
    private final Set<Long> queued = new HashSet<>();

    /** Whether we are currently executing a paste (blocks new queue drains). */
    private boolean isPasting = false;

    /** The repeating drain task — started on first enqueue, cancelled when queue empties. */
    private BukkitTask drainTask = null;

    private SchematicManager() {
        this.pastedKey = new NamespacedKey(Market.getPlugin(), "pathway_pasted");
    }

    public static SchematicManager getInstance() {
        if (instance == null)
            instance = new SchematicManager();
        return instance;
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    public void init() {
        worldEditPresent = Bukkit.getPluginManager().getPlugin("WorldEdit") != null;
        if (!worldEditPresent)
            return;

        String schematicPath =
                Market.getMainConfig().getString("market-world.pathway-schematic", "");
        if (schematicPath == null || schematicPath.isBlank())
            return;

        loadSchematic(schematicPath);
    }

    public void loadSchematic(String relativePath) {
        if (!worldEditPresent)
            return;
        try {
            File file = new File(Market.getPlugin().getDataFolder(), relativePath);
            if (!file.exists()) {
                Market.getPlugin().getLogger()
                        .warning("[Market] Schematic not found: " + file.getAbsolutePath());
                return;
            }
            com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat format =
                    com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats.findByFile(file);
            if (format == null) {
                Market.getPlugin().getLogger()
                        .warning("[Market] Unknown schematic format: " + file.getName());
                return;
            }
            try (com.sk89q.worldedit.extent.clipboard.io.ClipboardReader reader =
                    format.getReader(new java.io.FileInputStream(file))) {
                clipboard = reader.read();
            }
            Market.getPlugin().getLogger()
                    .info("[Market] Pathway schematic loaded: " + file.getName());
        } catch (Exception ex) {
            Market.getPlugin().getLogger().log(Level.SEVERE, "[Market] Error loading schematic",
                    ex);
        }
    }

    public boolean isAvailable() {
        return worldEditPresent && clipboard != null;
    }

    // ── Public API called from ChunkLoadEvent ─────────────────────────────────

    /**
     * Enqueue a chunk for pathway pasting. NEVER pastes immediately. Safe to call directly from
     * ChunkLoadEvent — no WorldEdit work happens here.
     */
    public void enqueueChunk(World world, Chunk chunk, int plotSize, int pathwayWidth) {
        if (!isAvailable())
            return;

        // PDC check — already pasted in a previous session
        if (chunk.getPersistentDataContainer().has(pastedKey, PersistentDataType.BYTE))
            return;

        long key = chunkKey(chunk.getX(), chunk.getZ());
        if (!queued.add(key))
            return; // already in queue

        pasteQueue.add(new PendingChunk(world, chunk, plotSize, pathwayWidth, key));
        Market.getPlugin().getLogger().info("[Market] Queued chunk " + chunk.getX() + ","
                + chunk.getZ() + " (queue size: " + pasteQueue.size() + ")");

        startDrainTaskIfNeeded();
    }

    // ── Drain task ────────────────────────────────────────────────────────────

    private void startDrainTaskIfNeeded() {
        if (drainTask != null && !drainTask.isCancelled())
            return;

        // Process 3 chunks every tick to make it much faster while staying safe
        drainTask =
                Bukkit.getScheduler().runTaskTimer(Market.getPlugin(), this::drainOne, 1L, 1L);
    }

    private void drainOne() {
        if (isPasting)
            return;
        
        // Process up to 3 chunks per tick for speed
        for (int i = 0; i < 3; i++) {
            if (pasteQueue.isEmpty()) {
                if (i == 0) { // Only cancel if we didn't do any work this tick
                    drainTask.cancel();
                    drainTask = null;
                }
                break;
            }

            PendingChunk pending = pasteQueue.poll();
            queued.remove(pending.key);

            Chunk chunk = pending.chunk;

            // Chunk may have unloaded while it was queued
            if (!chunk.isLoaded()) {
                continue;
            }

            // PDC double-check
            if (chunk.getPersistentDataContainer().has(pastedKey, PersistentDataType.BYTE))
                continue;

            // Mark BEFORE paste
            chunk.getPersistentDataContainer().set(pastedKey, PersistentDataType.BYTE, (byte) 1);

            isPasting = true;
            try {
                doPaste(pending);
            } catch (Exception ex) {
                Market.getPlugin().getLogger().log(Level.WARNING,
                        "[Market] Paste failed for chunk " + chunk.getX() + "," + chunk.getZ(), ex);
            } finally {
                isPasting = false;
            }
        }
    }

    // ── Tile math + paste execution ────────────────────────────────────────────

    private void doPaste(PendingChunk pending) {
        int totalSize = pending.plotSize + pending.pathwayWidth;
        int halfPath = pending.pathwayWidth / 2;

        int minWorldX = pending.chunk.getX() << 4;
        int minWorldZ = pending.chunk.getZ() << 4;
        int maxWorldX = minWorldX + 15;
        int maxWorldZ = minWorldZ + 15;

        int nXStart = (int) Math.floor((double) (minWorldX + halfPath) / totalSize);
        int nZStart = (int) Math.floor((double) (minWorldZ + halfPath) / totalSize);
        int nXEnd = (int) Math.floor((double) (maxWorldX + halfPath) / totalSize);
        int nZEnd = (int) Math.floor((double) (maxWorldZ + halfPath) / totalSize);

        boolean pastedAny = false;
        for (int nx = nXStart; nx <= nXEnd; nx++) {
            int tileX = nx * totalSize - halfPath;
            if (tileX < minWorldX || tileX > maxWorldX)
                continue;

            for (int nz = nZStart; nz <= nZEnd; nz++) {
                int tileZ = nz * totalSize - halfPath;
                if (tileZ < minWorldZ || tileZ > maxWorldZ)
                    continue;

                int y = Market.getMainConfig().getInt("market-world.pathway-y-level", 65);
                pasteSchematic(pending.world, tileX, y, tileZ);
                pastedAny = true;
                Market.getPlugin().getLogger().info("[Market] Pasted tile at " + tileX + "," + tileZ
                        + " for chunk " + pending.chunk.getX() + "," + pending.chunk.getZ());
            }
        }

        if (!pastedAny) {
            Market.getPlugin().getLogger().info("[Market] No pathway tiles in chunk "
                    + pending.chunk.getX() + "," + pending.chunk.getZ());
        }
    }

    private void pasteSchematic(World world, int x, int y, int z) {
        try {
            com.sk89q.worldedit.world.World weWorld =
                    com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world);
            com.sk89q.worldedit.extent.clipboard.Clipboard cb =
                    (com.sk89q.worldedit.extent.clipboard.Clipboard) clipboard;

            try (com.sk89q.worldedit.EditSession editSession =
                    com.sk89q.worldedit.WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
                
                editSession.setFastMode(true);

                com.sk89q.worldedit.function.operation.Operation op =
                        new com.sk89q.worldedit.session.ClipboardHolder(cb).createPaste(editSession)
                                .to(com.sk89q.worldedit.math.BlockVector3.at(x, y, z))
                                .ignoreAirBlocks(true).build();

                com.sk89q.worldedit.function.operation.Operations.complete(op);
                editSession.flushSession();
            }
        } catch (Exception ex) {
            Market.getPlugin().getLogger()
                    .warning("[Market] Failed to paste at " + x + "," + z + ": " + ex.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static class PendingChunk {
        final World world;
        final Chunk chunk;
        final int plotSize;
        final int pathwayWidth;
        final long key;

        PendingChunk(World world, Chunk chunk, int plotSize, int pathwayWidth, long key) {
            this.world = world;
            this.chunk = chunk;
            this.plotSize = plotSize;
            this.pathwayWidth = pathwayWidth;
            this.key = key;
        }
    }
}
