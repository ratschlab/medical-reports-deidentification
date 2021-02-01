package org.ratschlab.deidentifier.annotation.features;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

abstract public class FeatureKeys {
    public static <F extends FeatureKeys> Set<String> getFieldNames(Class<F> cls) {
        return Arrays.stream(cls.getFields()).filter(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()))
            .map(f -> {
                    try {
                        return f.get(null).toString();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException(String.format("Could not determine value of field %s", f.getName()), e);
                    }
                }
            ).collect(Collectors.toSet());

    }
}
