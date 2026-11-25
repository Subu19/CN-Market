package net.craftnepal.market.world;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;

/**
 * Every WorldEdit call lives here. The JVM loads WorldEdit classes when it verifies a class that
 * uses them, so this class must only be touched once WorldEdit is known to be installed;
 * {@link SchematicManager} checks that first. Without WorldEdit the plugin still loads.
 */
final class WorldEditPaster {

    private WorldEditPaster() {}

    /** Reads a schematic; null if the format is unknown. */
    static Object load(File file) throws Exception {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) return null;
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        }
    }

    /** Pastes at x,y,z, never touching the spawn column within {@code spawnRadius} of 0,0. */
    static void paste(Object clipboard, World world, int x, int y, int z, int spawnRadius) throws Exception {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {
            editSession.setFastMode(true);
            CuboidRegion spawn = new CuboidRegion(weWorld,
                    BlockVector3.at(-spawnRadius, -100, -spawnRadius), BlockVector3.at(spawnRadius, 400, spawnRadius));
            editSession.setMask(Masks.negate(new RegionMask(spawn)));
            Operations.complete(new ClipboardHolder((Clipboard) clipboard).createPaste(editSession)
                    .to(BlockVector3.at(x, y, z)).ignoreAirBlocks(true).build());
            editSession.flushSession();
        }
    }
}
