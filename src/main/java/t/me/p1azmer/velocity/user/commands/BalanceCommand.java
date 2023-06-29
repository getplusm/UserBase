package t.me.p1azmer.velocity.user.commands;

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import t.me.p1azmer.velocity.user.BasePlugin;
import t.me.p1azmer.velocity.user.model.BasedPlayer;

import java.sql.SQLException;
import java.text.MessageFormat;

public class BalanceCommand implements SimpleCommand {
    private final Dao<BasedPlayer, String> playerDao;
    private final String balanceSelf;

    public BalanceCommand(Dao<BasedPlayer, String> playerDao) {
        this.playerDao = playerDao;
        this.balanceSelf = BasePlugin.getConfig().MESSAGES.BALANCE_SELF;
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
                    source.sendMessage(Component.text("&cОшибка с базой данных. Перезайдите!"));
                    throw new RuntimeException(e);
                }
            }
            player.sendMessage(BasePlugin.getSerializer().deserialize(MessageFormat.format(this.balanceSelf, basedPlayer.getBalance())));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

}
