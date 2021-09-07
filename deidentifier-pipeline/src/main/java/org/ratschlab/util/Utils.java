package org.ratschlab.util;

import java.util.Optional;

public class Utils {
    public static Optional<Integer> maybeParseInt(String str) {
        try {
            return Optional.of(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<Double> maybeParseDouble(String str) {
        try {
            return Optional.of(Double.parseDouble(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
