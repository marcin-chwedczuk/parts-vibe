package app.partsvibe.shared.events.handling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(HandlesEvents.class)
public @interface HandlesEvent {
    String name();

    int version() default 1;
}
