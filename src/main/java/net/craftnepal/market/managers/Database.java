package net.craftnepal.market.managers;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The SQLite connection and the one thread allowed to use it. Every statement runs on that thread,
 * in submission order, so a write queued before a read is always visible to it.
 */
public final class Database {

    @FunctionalInterface
    public interface Sql<T> {
        T run(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlUpdate {
        void run(Connection connection) throws SQLException;
    }

    private final Plugin plugin;
    private final Logger log;
    private final ExecutorService executor;
    private Connection connection; // touched only on the executor thread

    public Database(Plugin plugin, File file) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Market-Database");
            t.setDaemon(true);
            return t;
        });
        await("open database", c -> {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found", e);
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            return null;
        });
    }

    /** Runs on the database thread and blocks until done. Startup and shutdown only. */
    public <T> T await(String operation, Sql<T> sql) {
        try {
            return executor.submit(() -> sql.run(connection)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(operation + " interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(operation + " failed", e.getCause());
        }
    }

    /** Queues a write. Failures are logged with the operation name; the caller is not told. */
    public void write(String operation, SqlUpdate sql) {
        if (executor.isShutdown()) {
            log.severe("Database closed, dropped write: " + operation);
            return;
        }
        executor.execute(() -> {
            try {
                sql.run(connection);
            } catch (SQLException | RuntimeException e) {
                log.log(Level.SEVERE, "Database write failed: " + operation, e);
            }
        });
    }

    /** Queues a read and hands the result to {@code onMainThread} on the server thread. */
    public <T> void read(String operation, Sql<T> sql, Consumer<T> onMainThread) {
        if (executor.isShutdown()) return;
        executor.execute(() -> {
            T result;
            try {
                result = sql.run(connection);
            } catch (SQLException | RuntimeException e) {
                log.log(Level.SEVERE, "Database read failed: " + operation, e);
                return;
            }
            if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> onMainThread.accept(result));
        });
    }

    /** Finishes every queued statement, then closes the connection. */
    public void close() {
        executor.execute(() -> {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                log.log(Level.WARNING, "Could not close database", e);
            }
        });
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.severe("Database writes still pending after 30s; some changes may be lost.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
