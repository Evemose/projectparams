package org.projectparams.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Priority(1)
public @interface DefaultValue {
    String value() default "superSecretDefaultValuePlaceholder";
}
