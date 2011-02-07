package payback.params;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * This component handles the {@link RuntimeParam} fields of a wrapped {@link #setConfigurable(Object) configurable object}.
 */
public class RuntimeParamsWrapper {

    private Object configurable;

    public void setConfigurable(Object configurable) {
        this.configurable = configurable;
    }

    public RuntimeParamsWrapper(Object parametrable) {
        this.configurable = parametrable;
    }

    public Map<Field, RuntimeParam> getParams() {
        Map<Field, RuntimeParam> result = new HashMap<Field, RuntimeParam>();
        for (Field field : configurable.getClass().getDeclaredFields()) {
            RuntimeParam annotation = field.getAnnotation(RuntimeParam.class);
            if (annotation != null) {
                result.put(field, annotation);
            }
        }
        return result;
    }

    public void setParam(Field field, Object value) throws Exception {
        Class<?> type = field.getType();
        field.setAccessible(true);
        RuntimeParam annotation = field.getAnnotation(RuntimeParam.class);
        Double d = Math.max(annotation.minValue(), (Double) value);
        d = Math.min(annotation.maxValue(), d);

        if (Integer.class.equals(type) || Integer.TYPE.equals(type)) {
            field.set(configurable, d.intValue());
        } else if (Float.class.equals(type) || Float.TYPE.equals(type)) {
            field.set(configurable, d.floatValue());
        } else {
            field.set(configurable, d.doubleValue());
        }
    }
}
