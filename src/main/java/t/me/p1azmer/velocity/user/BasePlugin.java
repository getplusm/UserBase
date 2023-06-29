package t.me.p1azmer.velocity.user;

import com.google.inject.Inject;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.db.DatabaseType;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableInfo;
import com.j256.ormlite.table.TableUtils;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;
import t.me.p1azmer.velocity.user.commands.AchievementCommand;
import t.me.p1azmer.velocity.user.commands.BalanceCommand;
import t.me.p1azmer.velocity.user.commands.admin.AdminCommand;
import t.me.p1azmer.velocity.user.configuration.kyori.Serializer;
import t.me.p1azmer.velocity.user.configuration.kyori.Serializers;
import t.me.p1azmer.velocity.user.dependencies.DatabaseLibrary;
import t.me.p1azmer.velocity.user.hook.LuckpermsHook;
import t.me.p1azmer.velocity.user.listener.JoinListener;
import t.me.p1azmer.velocity.user.model.BasedPlayer;
import t.me.p1azmer.velocity.user.model.SQLRuntimeException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

@com.velocitypowered.api.plugin.Plugin(
        id = "userbase",
        name = "UserBase",
        version = BuildConstants.VERSION,
        authors = {"plazmer"},
        description = "simple plugin for player base"
)
public class BasePlugin {
    @MonotonicNonNull
    private final Logger logger;
    @MonotonicNonNull
    private static Serializer SERIALIZER;

    private Dao<BasedPlayer, String> playerDao;

    private Pattern nicknameValidationPattern;

    private Component nicknameInvalidKick;

    private final ProxyServer server;
    private final File dataDirectoryFile;
    private final File configFile;

    private static final Config config = new Config();

    @Inject
    public BasePlugin(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = logger;

        this.server = server;

        this.dataDirectoryFile = dataDirectory.toFile();
        this.configFile = dataDirectory.resolve("config.yml").toFile();

        this.server.getPluginManager().getPlugin("luckperms").flatMap(PluginContainer::getInstance).orElseThrow(() -> {
            this.logger.error("Luckperms plugin not found. Plugin cannot work without LP");
            return null;
        });
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        System.setProperty("com.j256.simplelogging.level", "ERROR");
        this.reload();
    }

    public void reload() {
        config.reload(this.configFile, "userBase");

        ComponentSerializer<Component, Component, String> serializer = Serializers.LEGACY_AMPERSAND.getSerializer();
        if (serializer == null) {
            this.logger.warn("The specified serializer could not be founded, using default. (LEGACY_AMPERSAND)");
        } else {
            setSerializer(new Serializer(serializer));
        }

        ConnectionSource connectionSource;
        try {
            connectionSource = config.DATABASE.STORAGE_TYPE.connectToORM(
                    this.dataDirectoryFile.toPath().toAbsolutePath(),
                    config.DATABASE.HOSTNAME,
                    config.DATABASE.DATABASE + config.DATABASE.CONNECTION_PARAMETERS,
                    config.DATABASE.USER,
                    config.DATABASE.PASSWORD
            );
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        this.nicknameValidationPattern = Pattern.compile(config.MAIN.ALLOWED_NICKNAME_REGEX);
        this.nicknameInvalidKick = getSerializer().deserialize(config.MESSAGES.NICKNAME_INVALID_KICK);

        try {
            TableUtils.createTableIfNotExists(connectionSource, BasedPlayer.class);
            this.playerDao = DaoManager.createDao(connectionSource, BasedPlayer.class);
            this.migrateDb(this.playerDao, config.DATABASE.DATABASE, config.DATABASE.STORAGE_TYPE);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

        EventManager eventManager = this.server.getEventManager();
        eventManager.unregisterListeners(this);
        eventManager.register(this, new JoinListener(this, this.playerDao));

        CommandManager manager = this.server.getCommandManager();
        manager.register("balance", new BalanceCommand(this.playerDao), "bal", "бал", "баланс");
        manager.register("achievement", new AchievementCommand(this.playerDao), "ach", "достижения", "достяги", "ачивки");
        manager.register("userbase", new AdminCommand(this, this.playerDao), "ub");
    }

    public void migrateDb(Dao<?, ?> dao, String database, DatabaseLibrary library) {
        TableInfo<?, ?> tableInfo = dao.getTableInfo();

        Set<FieldType> tables = new HashSet<>();
        Collections.addAll(tables, tableInfo.getFieldTypes());


        String findSql;
        String tableName = tableInfo.getTableName();
        switch (library) {
            case SQLITE -> {
                findSql = "SELECT name FROM PRAGMA_TABLE_INFO('" + tableName + "')";
            }
            case H2 -> {
                findSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + "';";
            }
            case POSTGRESQL -> {
                findSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_CATALOG = '" + database + "' AND TABLE_NAME = '" + tableName + "';";
            }
            case MYSQL -> {
                findSql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + database + "' AND TABLE_NAME = '" + tableName + "';";
            }
            default -> {
                this.logger.error("WRONG DATABASE TYPE.");
                this.server.shutdown();
                return;
            }
        }

        try (GenericRawResults<String[]> queryResult = dao.queryRaw(findSql)) {
            queryResult.forEach(result -> tables.removeIf(table -> table.getColumnName().equalsIgnoreCase(result[0])));

            tables.forEach(table -> {
                try {
                    StringBuilder builder = new StringBuilder("ALTER TABLE ");
                    if (library == DatabaseLibrary.POSTGRESQL) {
                        builder.append('"');
                    }
                    builder.append(tableName);
                    if (library == DatabaseLibrary.POSTGRESQL) {
                        builder.append('"');
                    }
                    builder.append(" ADD ");
                    String columnDefinition = table.getColumnDefinition();
                    DatabaseType databaseType = dao.getConnectionSource().getDatabaseType();
                    if (columnDefinition == null) {
                        List<String> dummy = List.of();
                        databaseType.appendColumnArg(table.getTableName(), builder, table, dummy, dummy, dummy, dummy);
                    } else {
                        databaseType.appendEscapedEntityName(builder, table.getColumnName());
                        builder.append(" ").append(columnDefinition).append(" ");
                    }

                    dao.executeRawNoArgs(builder.toString());
                } catch (SQLException e) {
                    throw new SQLRuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new SQLRuntimeException(e);
        }
    }

    public void initialPlayer(Player player) {
        String nickname = player.getUsername();
        if (!this.nicknameValidationPattern.matcher(nickname).matches()) {
            player.disconnect(this.nicknameInvalidKick);
            return;
        }

        BasedPlayer basedPlayer = fetchInfo(this.playerDao, nickname);

        boolean onlineMode = player.isOnlineMode();

        if (onlineMode) {
            if (basedPlayer == null) {
                basedPlayer = fetchInfo(this.playerDao, player.getUniqueId());


                if (basedPlayer == null) {
                    basedPlayer = new BasedPlayer(player).setUuid(player.getUniqueId().toString()).setGroup(LuckpermsHook.getPrimaryGroup(player.getUniqueId()).getName());

                    try {
                        this.playerDao.create(basedPlayer);
                    } catch (SQLException e) {
                        throw new SQLRuntimeException(e);
                    }
                }
            }
            try {
                updatePlayerData(player);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void updatePlayerData(Player player) throws SQLException {
        String lowercaseNickname = player.getUsername().toLowerCase(Locale.ROOT);
        UpdateBuilder<BasedPlayer, String> updateBuilder = this.playerDao.updateBuilder();
//        updateBuilder.where().eq(BasedPlayer.LOWERCASE_NICKNAME_FIELD, lowercaseNickname);
//        updateBuilder.updateColumnValue(BasedPlayer.LOGIN_IP_FIELD, player.getRemoteAddress().getAddress().getHostAddress());
//        updateBuilder.updateColumnValue(BasedPlayer.LOGIN_DATE_FIELD, System.currentTimeMillis());
//        updateBuilder.update();
    }

    public static BasedPlayer fetchInfo(Dao<BasedPlayer, String> playerDao, UUID uuid) {
        try {
            List<BasedPlayer> playerList = playerDao.queryForEq(BasedPlayer.UUID_FIELD, uuid.toString());
            return (playerList != null ? playerList.size() : 0) == 0 ? null : playerList.get(0);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public static BasedPlayer fetchInfo(Dao<BasedPlayer, String> playerDao, String nickname) {
        try {
            List<BasedPlayer> playerList = playerDao.queryForEq(BasedPlayer.LOWERCASE_NICKNAME_FIELD, nickname.toLowerCase(Locale.ROOT));
            return (playerList != null ? playerList.size() : 0) == 0 ? null : playerList.get(0);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public Dao<BasedPlayer, String> getPlayerDao() {
        return playerDao;
    }

    private static void setSerializer(Serializer serializer) {
        SERIALIZER = serializer;
    }

    public static Serializer getSerializer() {
        return SERIALIZER;
    }

    public static Config getConfig() {
        return config;
    }
}
