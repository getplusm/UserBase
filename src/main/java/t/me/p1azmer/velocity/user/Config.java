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
                "���������� ��������� ��� ����������� ���������",
                "^ �������� ������ ������, $ �������� ����� ������",
                "[A-Za-z0-9_] - ��� ����� �������� A-Z, a-z, 0-9 � _",
                "{3,16} ��������, ��� ���������� ����� ���������� �� 3 �� 16 ��������"
        })
        public String ALLOWED_NICKNAME_REGEX = "^[A-Za-z0-9_]{3,16}$";

        @Create
        public MAIN.COMMAND_PERMISSION_STATE COMMAND_PERMISSION_STATE;

        @Comment({
                "��������� ����",
                " FALSE - ������� ��������� ��� ����",
                " TRUE - ������� �������� ������������, ���� � ���� ��� ����",
                " HAS - ������� ��������, ���� � ������������ ���� �����"
        })
        public static class COMMAND_PERMISSION_STATE {
            @Comment("�����: userbase.commands.userbase")
            public CommandPermissionState ADMIN_COMMAND = CommandPermissionState.HAS;
        }
    }

    @Create
    public MESSAGES MESSAGES;

    public static class MESSAGES {
        public String NICKNAME_INVALID_KICK = "&c��� ��� �������� ����������� �������. �������� ���� ���!";
        public String BALANCE_SELF = "&f��� ������: &2{0}";

        @Create
        public MESSAGES.ACHIEVEMENT ACHIEVEMENT;

        public static class ACHIEVEMENT {

            public String EMPTY = "&c� ��� ��� ���������� ��� ���������� ����������!";
            public String MESSAGE = "&f���� ����������:{NL}{0}";
            @Comment({
                    "{0} - ��������",
                    "{1} - ��������",
                    "{2} - �� ���� ��������"
            })
            public String FORMAT = "&6{0}&8- &e{1}&8. &7��������: &f{2}";
        }
    }

    @Create
    public DATABASE DATABASE;

    @Comment("��������� ����������� � ��")
    public static class DATABASE {

        @Comment({
                "��������� ����",
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

