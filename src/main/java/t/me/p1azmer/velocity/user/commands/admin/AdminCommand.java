package t.me.p1azmer.velocity.user.commands.admin;

import com.google.common.collect.ImmutableList;
import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import t.me.p1azmer.velocity.user.BasePlugin;
import t.me.p1azmer.velocity.user.api.perms.CommandPermissionState;
import t.me.p1azmer.velocity.user.hook.LuckpermsHook;
import t.me.p1azmer.velocity.user.model.BasedPlayer;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdminCommand implements SimpleCommand {

    private static final Component AVAILABLE_SUBCOMMANDS_MESSAGE = Component.text("��������� ����������:", NamedTextColor.WHITE);
    private static final Component NO_AVAILABLE_SUBCOMMANDS_MESSAGE = Component.text("��������, �� ��� ��� ��� ���������.", NamedTextColor.WHITE);

    private final BasePlugin plugin;
    private final Dao<BasedPlayer, String> playerDao;

    public AdminCommand(BasePlugin plugin, Dao<BasedPlayer, String> playerDao) {
        this.plugin = plugin;
        this.playerDao = playerDao;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            return Arrays.stream(Subcommand.values())
                    .filter(command -> command.hasPermission(source))
                    .map(Subcommand::getCommand)
                    .collect(Collectors.toList());
        } else if (args.length == 1) {
            String argument = args[0];
            return Arrays.stream(Subcommand.values())
                    .filter(command -> command.hasPermission(source))
                    .map(Subcommand::getCommand)
                    .filter(str -> str.regionMatches(true, 0, argument, 0, argument.length()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            return Stream.of(this.playerDao.iterator()).map(f -> {
                try {
                    return f.current().getNickname();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set_group")) {
            return LuckpermsHook.getPlugin().getGroupManager().getLoadedGroups().stream().filter(Objects::nonNull).map(Group::getName).collect(Collectors.toList());
        } else {
            return ImmutableList.of();
        }
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        int argsAmount = args.length;
        if (argsAmount > 0) {
            try {
                Subcommand subcommand = Subcommand.valueOf(args[0].toUpperCase(Locale.ROOT));
                if (!subcommand.hasPermission(source)) {
                    this.showHelp(source);
                    return;
                }

                subcommand.executor.execute(this, source, args);
            } catch (IllegalArgumentException e) {
                this.showHelp(source);
            }
        } else {
            this.showHelp(source);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return BasePlugin.getConfig().MAIN.COMMAND_PERMISSION_STATE.ADMIN_COMMAND.hasPermission(invocation.source(), "userbase.commands.userbase");
    }

    private void showHelp(CommandSource source) {

        List<Subcommand> availableSubcommands = Arrays.stream(Subcommand.values())
                .filter(command -> command.hasPermission(source)).toList();

        if (availableSubcommands.size() > 0) {
            source.sendMessage(AVAILABLE_SUBCOMMANDS_MESSAGE);
            availableSubcommands.forEach(command -> source.sendMessage(command.getMessageLine()));
        } else {
            source.sendMessage(NO_AVAILABLE_SUBCOMMANDS_MESSAGE);
        }
    }


    //TODO: Rework all message with Config.Messages
    private enum Subcommand {
        RELOAD("������������� ������", CommandPermissionState.HAS,
                (parent, source, args) -> {
                    parent.plugin.reload();
                    source.sendMessage(Component.text("������ ������� ������������", NamedTextColor.GREEN));
                }
        ),
        REMOVE_BALANCE("������� ������ � ������. [���] [���-��]", CommandPermissionState.HAS,
                (AdminCommand parent, CommandSource source, String[] args) -> {
                    if (args.length < 3) {
                        source.sendMessage(Component.text("������� ��� ������ � ���-�� ������!", NamedTextColor.RED));
                        return;
                    }
                    String userName = args[1];
                    int value = Integer.parseInt(args[2]);
                    if (value <= 0) {
                        source.sendMessage(Component.text("�� ������� �������� ���-��!", NamedTextColor.RED));
                        return;
                    }
                    BasedPlayer basedPlayer = BasePlugin.fetchInfo(parent.playerDao, userName);
                    if (basedPlayer == null) {
                        source.sendMessage(Component.text("����� � ����� '" + userName + "' �� ������!"));
                        return;
                    }
                    try {
                        boolean incorrect = false;
                        if (basedPlayer.getBalance() < value) {
                            source.sendMessage(Component.text("������ ������ ������ ��� ��������� �����. ������ ���������� �� 0", NamedTextColor.WHITE));
                            incorrect = true;
                        }
                        basedPlayer.removeBalance(value);
                        parent.playerDao.update(basedPlayer);
                        if (!incorrect)
                            source.sendMessage(Component.text("������ ������ �������� �� " + value + " �����. ��������� ������: " + basedPlayer.getBalance(), NamedTextColor.WHITE));
                    } catch (SQLException e) {
                        source.sendMessage(Component.text("������ � ����������� ������ ������. �������� � �������", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                }),
        ADD_BALANCE("�������� ������ ������. [���] [���-��]", CommandPermissionState.HAS,
                (AdminCommand parent, CommandSource source, String[] args) -> {
                    if (args.length < 3) {
                        source.sendMessage(Component.text("������� ��� ������ � ���-�� ������!", NamedTextColor.RED));
                        return;
                    }
                    String userName = args[1];
                    int value = Integer.parseInt(args[2]);
                    if (value <= 0) {
                        source.sendMessage(Component.text("�� ������� �������� ���-��!", NamedTextColor.RED));
                        return;
                    }
                    BasedPlayer basedPlayer = BasePlugin.fetchInfo(parent.playerDao, userName);
                    if (basedPlayer == null) {
                        source.sendMessage(Component.text("����� � ����� '" + userName + "' �� ������!"));
                        return;
                    }
                    try {
                        basedPlayer.addBalance(value);
                        parent.playerDao.update(basedPlayer);
                        source.sendMessage(Component.text("������ ������ �������� �� " + value + " �����. ��������� ������: " + basedPlayer.getBalance(), NamedTextColor.WHITE));
                    } catch (SQLException e) {
                        source.sendMessage(Component.text("������ � ����������� ������ ������. �������� � �������", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                }),
        SET_BALANCE("���������� ������ ������. [���] [���-��]", CommandPermissionState.HAS,
                (AdminCommand parent, CommandSource source, String[] args) -> {
                    if (args.length < 3) {
                        source.sendMessage(Component.text("������� ��� ������ � ���-�� ������!", NamedTextColor.RED));
                        return;
                    }
                    String userName = args[1];
                    int value = Integer.parseInt(args[2]);
                    if (value <= -1) {
                        source.sendMessage(Component.text("�� ������� �������� ���-��!", NamedTextColor.RED));
                        return;
                    }
                    BasedPlayer basedPlayer = BasePlugin.fetchInfo(parent.playerDao, userName);
                    if (basedPlayer == null) {
                        source.sendMessage(Component.text("����� � ����� '" + userName + "' �� ������!"));
                        return;
                    }
                    try {
                        basedPlayer.setBalance(value);
                        parent.playerDao.update(basedPlayer);
                        source.sendMessage(Component.text("������ ������ ���������� �� " + value + " �����", NamedTextColor.WHITE));
                    } catch (SQLException e) {
                        source.sendMessage(Component.text("������ � ����������� ������ ������. �������� � �������", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                }),
        SET_GROUP("���������� ������ ������. [���] [������]", CommandPermissionState.HAS,
                (AdminCommand parent, CommandSource source, String[] args) -> {
                    if (args.length < 3) {
                        source.sendMessage(Component.text("������� ��� ������ � �������� ������!", NamedTextColor.RED));
                        return;
                    }
                    String userName = args[1];
                    String value = args[2];
                    Group group = LuckpermsHook.getGroup(value);
                    if (group == null) {
                        source.sendMessage(Component.text("��������� ���� ������ �� ������� � LP", NamedTextColor.RED));
                        return;
                    }
                    BasedPlayer basedPlayer = BasePlugin.fetchInfo(parent.playerDao, userName);
                    if (basedPlayer == null) {
                        source.sendMessage(Component.text("����� � ����� '" + userName + "' �� ������!"));
                        return;
                    }
                    User user = LuckpermsHook.getUser(basedPlayer.getId());
                    if (user == null) {
                        source.sendMessage(Component.text("����� � ����� '" + userName + "' �� ������ � ���� LP!", NamedTextColor.RED));
                        return;
                    }
                    InheritanceNode node = InheritanceNode.builder(group).build();
                    Set<String> groups = user.getNodes().stream()
                            .filter(NodeType.INHERITANCE::matches)
                            .map(NodeType.INHERITANCE::cast)
                            .map(InheritanceNode::getGroupName)
                            .collect(Collectors.toSet());
                    if (groups.contains(group.getName())) {
                        source.sendMessage(Component.text("����� � ����� '" + userName + "' ��� ����� ����� ���� ������!", NamedTextColor.RED));
                        return;
                    }
                    try {
                        DataMutateResult result = user.data().add(node);
                        if (result.wasSuccessful()) {
                            basedPlayer.setGroup(value);
                            parent.playerDao.update(basedPlayer);
                            LuckpermsHook.getPlugin().getUserManager().saveUser(user);
                            source.sendMessage(Component.text("������ ������ ��������� �� " + value, NamedTextColor.WHITE));
                        } else {
                            source.sendMessage(Component.text("������ � ������������� ������ ������. ������: ", NamedTextColor.RED));
                        }
                    } catch (SQLException e) {
                        source.sendMessage(Component.text("������ � ����������� ������ ������. �������� � �������", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                }),
        ADD_ACHIEVEMENT("�������� ���������� ������. [���] [�������� ����������] [��������] *[������ �������]", CommandPermissionState.HAS,
                (AdminCommand parent, CommandSource source, String[] args) -> {
                    if (args.length < 4) {
                        source.sendMessage(Component.text("������� ��� ������, �������� ����������, �������� �, ���� ������, �����������, ������ �� ��� �������", NamedTextColor.RED));
                        return;
                    }
                    String userName = args[1];
                    String achievement = args[2];
                    Object value = args[3];
                    String from = args.length >= 5 ? args[4] : null;

                    BasedPlayer basedPlayer = BasePlugin.fetchInfo(parent.playerDao, userName);
                    if (basedPlayer == null) {
                        source.sendMessage(Component.text("����� � ����� '" + userName + "' �� ������!"));
                        return;
                    }
                    try {
                        basedPlayer.addAchievement(achievement, value, from);
                        parent.playerDao.update(basedPlayer);
                        source.sendMessage(Component.text("������ '" + userName + "' ��������� ����������: " + achievement + ". ��������: " + value + "." + (from != null ? " ��������: " + from : ""), NamedTextColor.WHITE));
                    } catch (SQLException e) {
                        source.sendMessage(Component.text("������ � ����������� ������ ������. �������� � �������", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                }),
        REMOVE_ACHIEVEMENT("������ ���������� ������. [���] [�������� ����������]", CommandPermissionState.HAS,
                (AdminCommand parent, CommandSource source, String[] args) -> {
                    if (args.length < 3) {
                        source.sendMessage(Component.text("������� ��� ������ � �������� ����������!", NamedTextColor.RED));
                        return;
                    }
                    String userName = args[1];
                    String achievement = args[2];

                    BasedPlayer basedPlayer = BasePlugin.fetchInfo(parent.playerDao, userName);
                    if (basedPlayer == null) {
                        source.sendMessage(Component.text("����� � ����� '" + userName + "' �� ������!"));
                        return;
                    }

                    if (basedPlayer.getAchievement(achievement) == null) {
                        source.sendMessage(Component.text("��������� ���� ���������� �� ������� � ������������!", NamedTextColor.RED));
                        return;
                    }

                    try {
                        basedPlayer.removeAchievement(achievement);
                        parent.playerDao.update(basedPlayer);
                        source.sendMessage(Component.text("���������� '" + achievement + "' ������� ������� � ������!", NamedTextColor.WHITE));
                    } catch (SQLException e) {
                        source.sendMessage(Component.text("������ � ����������� ������ ������. �������� � �������", NamedTextColor.RED));
                        throw new RuntimeException(e);
                    }
                }),
        ;

        private final String command;
        private final String description;
        private final CommandPermissionState permissionState;
        private final SubcommandExecutor executor;

        Subcommand(String description, CommandPermissionState permissionState, SubcommandExecutor executor) {
            this.permissionState = permissionState;
            this.command = this.name().toLowerCase(Locale.ROOT);
            this.description = description;
            this.executor = executor;
        }

        public boolean hasPermission(CommandSource source) {
            return this.permissionState.hasPermission(source, "userbase.command.admin." + this.command);
        }

        public Component getMessageLine() {
            return Component.textOfChildren(
                    Component.text("  /userbase " + this.command, NamedTextColor.GREEN),
                    Component.text(" - ", NamedTextColor.DARK_GRAY),
                    Component.text(this.description, NamedTextColor.YELLOW)
            );
        }

        public String getCommand() {
            return this.command;
        }
    }

    private interface SubcommandExecutor {
        void execute(AdminCommand parent, CommandSource source, String[] args);
    }
}
