// SPDX-License-Identifier: MIT

package ir.kebritam.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Csv {

    String columnName() default "";

    int columnIndex() default Integer.MAX_VALUE;
}
