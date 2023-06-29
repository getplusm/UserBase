package t.me.p1azmer.velocity.user;

import org.jetbrains.annotations.NotNull;
import t.me.p1azmer.velocity.user.api.perms.CommandPermissionState;
import t.me.p1azmer.velocity.user.configuration.velocity.YamlConfig;
import t.me.p1azmer.velocity.user.dependencies.DatabaseLibrary;

import java.util.Optional;

public class Config extends YamlConfig {
    @Create
    public MAIN MAIN;
    public static class MAIN {
        @Comment({
                "Регулярное выражение для разрешенных никнеймов",
                "^ означает начало строки, $ означает конец строки",
                "[A-Za-z0-9_] - это набор символов A-Z, a-z, 0-9 и _",
                "{3,16} означает, что допустимая длина составляет от 3 до 16 символов"
        })
        public String ALLOWED_NICKNAME_REGEX = "^[A-Za-z0-9_]{3,16}$";

        @Create
        public MAIN.COMMAND_PERMISSION_STATE COMMAND_PERMISSION_STATE;

        @Comment({
                "Доступные типы",
                " FALSE - Команда отключена для всех",
                " TRUE - Команда доступна пользователю, если у него нет прав",
                " HAS - Команда доступна, если у пользователя есть права"
        })
        public static class COMMAND_PERMISSION_STATE {
            @Comment("Право: userbase.commands.userbase")
            public CommandPermissionState ADMIN_COMMAND = CommandPermissionState.HAS;
        }
    }

    @Create
    public MESSAGES MESSAGES;

    public static class MESSAGES {
        public String NICKNAME_INVALID_KICK = "&cВаш ник содержит запрещенные символы. Измените свой ник!";
        public String BALANCE_SELF = "&fВаш баланс: &2{0}";

        @Create
        public MESSAGES.ACHIEVEMENT ACHIEVEMENT;

        public static class ACHIEVEMENT {

            public String EMPTY = "&cУ Вас нет выпаленных или полученных достижений!";
            public String MESSAGE = "&fВаши достижения:{NL}{0}";
            @Comment({
                    "{0} - Название",
                    "{1} - Значение",
                    "{2} - От кого получено"
            })
            public String FORMAT = "&6{0}&8- &e{1}&8. &7Получено: &f{2}";
        }
    }

    @Create
    public DATABASE DATABASE;

    @Comment("Настройка подключения к БД")
    public static class DATABASE {

        @Comment({
                "Доступные типы",
                " H2_LEGACY_V1",
                " H2",
                " POSTGRESQL",
                " SQLITE"
        })
        public DatabaseLibrary STORAGE_TYPE = DatabaseLibrary.MYSQL;

        public String HOSTNAME = "127.0.0.1:3306";
        public String USER = "user";
        public String PASSWORD = "password";
        public String DATABASE = "limboauth";
        public String CONNECTION_PARAMETERS = "?autoReconnect=true&initialTimeout=1&useSSL=false";
    }

    @NotNull
    public static <T extends Enum<T>> Optional<T> getEnum(@NotNull String str, @NotNull Class<T> clazz) {
        try {
            return Optional.of(Enum.valueOf(clazz, str.toUpperCase()));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}

