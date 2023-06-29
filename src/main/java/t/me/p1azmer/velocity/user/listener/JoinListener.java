package t.me.p1azmer.velocity.user.listener;

import com.j256.ormlite.dao.Dao;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import t.me.p1azmer.velocity.user.BasePlugin;
import t.me.p1azmer.velocity.user.hook.LuckpermsHook;
import t.me.p1azmer.velocity.user.model.BasedPlayer;
import t.me.p1azmer.velocity.user.model.SQLRuntimeException;

import java.sql.SQLException;
import java.util.UUID;

public class JoinListener {

    private final BasePlugin basePlugin;
    private final Dao<BasedPlayer, String> playerDao;

    public JoinListener(BasePlugin plugin, Dao<BasedPlayer, String> playerDao) {
        this.basePlugin = plugin;
        this.playerDao = playerDao;
    }

    @Subscribe
    public void onPreLoginEvent(PreLoginEvent event) {
        event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
    }


    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        this.basePlugin.initialPlayer(event.getPlayer());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        BasedPlayer basedPlayer = BasePlugin.fetchInfo(this.playerDao, event.getOriginalProfile().getId());

        if (basedPlayer != null && !basedPlayer.getUuid().isEmpty()) {
            event.setGameProfile(event.getOriginalProfile().withId(UUID.fromString(basedPlayer.getUuid())));
            return;
        }
        basedPlayer = BasePlugin.fetchInfo(this.playerDao, event.getUsername());

        if (basedPlayer != null) {
            String currentUuid = basedPlayer.getUuid();

            if (currentUuid.isEmpty()) {
                try {
                    basedPlayer.setUuid(event.getGameProfile().getId().toString()).setGroup(LuckpermsHook.getPrimaryGroup(event.getGameProfile().getId()).getName());
                    this.playerDao.update(basedPlayer);
                } catch (SQLException e) {
                    throw new SQLRuntimeException(e);
                }
            } else {
                event.setGameProfile(event.getOriginalProfile().withId(UUID.fromString(currentUuid)));
            }
        }
    }
}
