package payback.params;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks fields which are configurable, numeric parameters, whose value may be modified at runtime.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RuntimeParam {

    double value();

    double minValue() default Double.MIN_VALUE;

    double maxValue() default Double.MAX_VALUE;
    
}
