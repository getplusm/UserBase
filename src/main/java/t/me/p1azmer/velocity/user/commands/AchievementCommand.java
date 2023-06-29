package t.me.p1azmer.velocity.user.commands;

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import t.me.p1azmer.velocity.user.BasePlugin;
import t.me.p1azmer.velocity.user.Config;
import t.me.p1azmer.velocity.user.api.achievement.Achievement;
import t.me.p1azmer.velocity.user.model.BasedPlayer;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

public class AchievementCommand implements SimpleCommand {

    private final Dao<BasedPlayer, String> playerDao;
    private final String achievementSelf;
    private final String achievementFormat;
    private final String achievementEmpty;

    public AchievementCommand(Dao<BasedPlayer, String> playerDao) {
        this.playerDao = playerDao;
        this.achievementSelf = BasePlugin.getConfig().MESSAGES.ACHIEVEMENT.MESSAGE;
        this.achievementFormat = BasePlugin.getConfig().MESSAGES.ACHIEVEMENT.FORMAT;
        this.achievementEmpty = BasePlugin.getConfig().MESSAGES.ACHIEVEMENT.EMPTY;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player player) {
            String username = ((Player) source).getUsername();
            BasedPlayer basedPlayer = BasePlugin.fetchInfo(this.playerDao, username);
            if (basedPlayer == null) {
                try {
                    basedPlayer = this.playerDao.createIfNotExists(new BasedPlayer(player));
                } catch (SQLException e) {
                    source.sendMessage(Component.text("Ошибка с базой данных. Перезайдите!", NamedTextColor.RED));
                    throw new RuntimeException(e);
                }
            }
            List<Achievement> achievements = basedPlayer.getAchievements();
            if (achievements.isEmpty()) {
                player.sendMessage(BasePlugin.getSerializer().deserialize(this.achievementEmpty));
                return;
            }
            StringBuilder builder = new StringBuilder();
            int size = achievements.size();
            for (int i = 0; i < size; i++) {
                Achievement achievement = achievements.get(i);
                builder.append(MessageFormat.format(this.achievementFormat, achievement.getDisplayName(), achievement.getDisplayValue(), achievement.getDisplayFrom()));
                if (i < size - 1) {
                    builder.append("\n");
                }
            }
            player.sendMessage(BasePlugin.getSerializer().deserialize(MessageFormat.format(this.achievementSelf, builder.toString())));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
