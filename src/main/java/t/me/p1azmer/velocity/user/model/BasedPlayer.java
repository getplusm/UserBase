package t.me.p1azmer.velocity.user.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import t.me.p1azmer.velocity.user.api.achievement.Achievement;
import t.me.p1azmer.velocity.user.api.achievement.AchievementSerialize;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@DatabaseTable(tableName = "UsersBase")
public class BasedPlayer {

    public static final String NICKNAME_FIELD = "NICKNAME";
    public static final String LOWERCASE_NICKNAME_FIELD = "LOWERCASENICKNAME";
    public static final String BALANCE_FIELD = "BALANCE";
    public static final String GROUP_FIELD = "GROUP";
    public static final String UUID_FIELD = "UUID";
    public static final String ACHIEVEMENT_FIELD = "ACHIEVEMENT";

    @DatabaseField(canBeNull = false, columnName = NICKNAME_FIELD)
    private String nickname;

    @DatabaseField(id = true, columnName = LOWERCASE_NICKNAME_FIELD)
    private String lowercaseNickname;

    @DatabaseField(defaultValue = "0", canBeNull = false, columnName = BALANCE_FIELD)
    private int balance = 0;
    @DatabaseField(canBeNull = false, columnName = ACHIEVEMENT_FIELD, persisterClass = AchievementSerialize.class)
    private List<Achievement> achievement;

    @DatabaseField(columnName = UUID_FIELD)
    private String uuid = "";

    @DatabaseField(canBeNull = false, defaultValue = "default", columnName = GROUP_FIELD)
    private String group = "default";

    @Deprecated
    public BasedPlayer(String nickname, String lowercaseNickname, int balance, String group, List<Achievement> achievement, String uuid) {
        this.nickname = nickname;
        this.lowercaseNickname = lowercaseNickname;
        this.uuid = uuid;
        this.balance = balance;
        this.group = group;
        this.achievement = achievement;
    }

    public BasedPlayer(Player player) {
        this(player.getUsername(), player.getUniqueId(), player.getRemoteAddress());
    }

    public BasedPlayer(String nickname, UUID uuid, InetSocketAddress ip) {
        this(nickname, uuid.toString(), ip.getAddress().getHostAddress());
    }

    public BasedPlayer(String nickname, String uuid, String ip) {
        this.nickname = nickname;
        this.lowercaseNickname = nickname.toLowerCase(Locale.ROOT);
        this.uuid = uuid;
        this.balance = 0;
        this.group = "default";
        this.achievement = new ArrayList<>();
    }

    public BasedPlayer() {

    }

    public BasedPlayer setNickname(String nickname) {
        this.nickname = nickname;
        this.lowercaseNickname = nickname.toLowerCase(Locale.ROOT);

        return this;
    }

    public String getNickname() {
        return this.nickname == null ? this.lowercaseNickname : this.nickname;
    }

    public String getLowercaseNickname() {
        return this.lowercaseNickname;
    }

    public BasedPlayer setUuid(String uuid) {
        this.uuid = uuid;

        return this;
    }

    public String getUuid() {
        return this.uuid == null ? "" : this.uuid;
    }

    public UUID getId() {
        return UUID.fromString(this.getUuid());
    }

    public int getBalance() {
        return balance;
    }

    public void addBalance(int value) {
        this.balance += value;
    }

    public void removeBalance(int value) {
        this.balance -= value;
        if (this.balance < 0) {
            this.balance = 0;
        }
    }

    public void setBalance(int value) {
        this.balance = value;
    }

    public String getGroup() {
        return group;
    }

    public BasedPlayer setGroup(String group) {
        this.group = group;
        return this;
    }

    public List<Achievement> getAchievements() {
        return achievement;
    }

    @Nullable
    public Achievement getAchievement(String name) {
        return this.getAchievements().stream().filter(f -> f.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void addAchievement(String name, Object value) {
        Achievement achievement = new Achievement(name, value, "От администрации");
        this.getAchievements().add(achievement);
    }

    public void addAchievement(String name, Object value, String from) {
        Achievement achievement = new Achievement(name, value, (from == null ? "От администрации" : from));
        this.getAchievements().add(achievement);
    }

    public void removeAchievement(String name) {
        Achievement achievement = getAchievement(name);
        if (achievement != null)
            this.getAchievements().remove(achievement);
    }

    public void removeAchievement(Achievement achievement) {
        this.getAchievements().remove(achievement);
    }

    public void setAchievement(List<Achievement> map) {
        this.achievement = map;
    }
}
