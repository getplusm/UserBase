package t.me.p1azmer.velocity.user.api.achievement;

import java.io.Serializable;
import java.util.Locale;

public class Achievement implements Serializable {

    private final String name;
    private Object value;
    private String from;

    public Achievement(String name, Object value, String from) {
        this.name = name;
        this.value = value;
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    public String getDisplayFrom() {
        return this.getFrom().replace("_", " ");
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return getName().replace("_", " ");
    }

    public Object getValue() {
        return value;
    }

    public Object getDisplayValue() {
        Object value = getValue();

        if (value instanceof Boolean) {
            boolean result = (Boolean) value;
            return result ? "да" : "нет";
        } else if (value instanceof String) {
            boolean result = Boolean.parseBoolean(((String) value).toLowerCase(Locale.ROOT));
            return result ? "да" : "нет";
        }

        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
