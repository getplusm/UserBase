package t.me.p1azmer.velocity.user.dependencies;

import com.j256.ormlite.jdbc.JdbcSingleConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;

public enum DatabaseLibrary {
  H2_LEGACY_V1(
      BaseLibrary.H2_V1,
          (classLoader, dir, jdbc, user, password) -> fromDriver(classLoader.loadClass("org.h2.Driver"), jdbc, null, null, false),
          (dir, hostname, database) -> "jdbc:h2:" + dir + "/userbase"
  ),
  H2(
      BaseLibrary.H2_V2,
          (classLoader, dir, jdbc, user, password) -> {
            Connection modernConnection = fromDriver(classLoader.loadClass("org.h2.Driver"), jdbc, null, null, true);

            Path legacyDatabase = dir.resolve("userbase.mv.db");
            if (Files.exists(legacyDatabase)) {
              Path dumpFile = dir.resolve("userbase.dump.sql");
              try (Connection legacyConnection = H2_LEGACY_V1.connect(dir, null, null, user, password)) {
                try (Statement migrateStatement = legacyConnection.createStatement()) {
                  migrateStatement.execute("SCRIPT TO '" + dumpFile + "'");
                }
              }

              try (Statement migrateStatement = modernConnection.createStatement()) {
                migrateStatement.execute("RUNSCRIPT FROM '" + dumpFile + "'");
              }

              Files.delete(dumpFile);
              Files.move(legacyDatabase, dir.resolve("userbase-backup.mv.db"));
            }

            return modernConnection;
          },
          (dir, hostname, database) -> "jdbc:h2:" + dir + "/userbase"
  ),
  MYSQL(
      BaseLibrary.MYSQL,
          (classLoader, dir, jdbc, user, password)
              -> fromDriver(classLoader.loadClass("com.mysql.cj.jdbc.NonRegisteringDriver"), jdbc, user, password, true),
          (dir, hostname, database) ->
              "jdbc:mysql://" + hostname + "/" + database
  ),
  POSTGRESQL(
      BaseLibrary.POSTGRESQL,
          (classLoader, dir, jdbc, user, password) -> fromDriver(classLoader.loadClass("org.postgresql.Driver"), jdbc, user, password, true),
          (dir, hostname, database) -> "jdbc:postgresql://" + hostname + "/" + database
  ),
  SQLITE(
      BaseLibrary.SQLITE,
          (classLoader, dir, jdbc, user, password) -> fromDriver(classLoader.loadClass("org.sqlite.JDBC"), jdbc, user, password, true),
          (dir, hostname, database) -> "jdbc:sqlite:" + dir + "/userbase.db"
  );

  private final BaseLibrary baseLibrary;
  private final DatabaseConnector connector;
  private final DatabaseStringGetter stringGetter;

  DatabaseLibrary(BaseLibrary baseLibrary, DatabaseConnector connector, DatabaseStringGetter stringGetter) {
    this.baseLibrary = baseLibrary;
    this.connector = connector;
    this.stringGetter = stringGetter;
  }

  public Connection connect(ClassLoader classLoader, Path dir, String hostname, String database, String user, String password)
      throws ReflectiveOperationException, SQLException, IOException {
    return this.connect(classLoader, dir, this.stringGetter.getJdbcString(dir, hostname, database), user, password);
  }

  public Connection connect(Path dir, String hostname, String database, String user, String password)
      throws ReflectiveOperationException, SQLException, IOException {
    return this.connect(dir, this.stringGetter.getJdbcString(dir, hostname, database), user, password);
  }

  public Connection connect(ClassLoader classLoader, Path dir, String jdbc, String user, String password)
      throws ReflectiveOperationException, SQLException, IOException {
    return this.connector.connect(classLoader, dir, jdbc, user, password);
  }

  public Connection connect(Path dir, String jdbc, String user, String password) throws IOException, ReflectiveOperationException, SQLException {
    return this.connector.connect(new IsolatedClassLoader(new URL[]{this.baseLibrary.getClassLoaderURL()}), dir, jdbc, user, password);
  }

  public ConnectionSource connectToORM(Path dir, String hostname, String database, String user, String password)
      throws ReflectiveOperationException, IOException, SQLException, URISyntaxException {
    String jdbc = this.stringGetter.getJdbcString(dir, hostname, database);
    URL baseLibraryURL = this.baseLibrary.getClassLoaderURL();
    ClassLoader currentClassLoader = DatabaseLibrary.class.getClassLoader();
    Method addPath = currentClassLoader.getClass().getDeclaredMethod("addPath", Path.class);
    addPath.setAccessible(true);
    addPath.invoke(currentClassLoader, Path.of(baseLibraryURL.toURI()));

    return new JdbcSingleConnectionSource(jdbc, this.connect(currentClassLoader, dir, jdbc, user, password));
  }

  private static Connection fromDriver(Class<?> connectionClass, String jdbc, String user, String password, boolean register)
      throws ReflectiveOperationException, SQLException {
    Constructor<?> legacyConstructor = connectionClass.getConstructor();

    Properties info = new Properties();
    if (user != null) {
      info.put("user", user);
    }

    if (password != null) {
      info.put("password", password);
    }

    Object driver = legacyConstructor.newInstance();

    DriverManager.deregisterDriver((Driver) driver);
    if (register) {
      DriverManager.registerDriver((Driver) driver);
    }

    Method connect = connectionClass.getDeclaredMethod("connect", String.class, Properties.class);
    connect.setAccessible(true);
    return (Connection) connect.invoke(driver, jdbc, info);
  }

  public interface DatabaseConnector {
    Connection connect(ClassLoader classLoader, Path dir, String jdbc, String user, String password)
        throws ReflectiveOperationException, SQLException, IOException;
  }

  public interface DatabaseStringGetter {
    String getJdbcString(Path dir, String hostname, String database);
  }
}
