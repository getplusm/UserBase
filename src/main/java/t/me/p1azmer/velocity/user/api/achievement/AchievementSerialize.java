package t.me.p1azmer.velocity.user.api.achievement;

import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.SerializableType;

import java.lang.reflect.Field;

public class AchievementSerialize extends SerializableType {

    private static final AchievementSerialize singleton = new AchievementSerialize();

    public AchievementSerialize() {
        super(SqlType.SERIALIZABLE, new Class[]{Achievement.class});
    }

    public static AchievementSerialize getSingleton() {
        return singleton;
    }

    @Override
    public boolean isValidForField(Field field) {
        return Achievement.class.isAssignableFrom(field.getType());
    }
}
