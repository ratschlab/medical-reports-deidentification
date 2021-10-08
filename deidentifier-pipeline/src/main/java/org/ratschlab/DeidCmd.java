package org.ratschlab;

import java.util.concurrent.Callable;

public class DeidCmd implements Callable<Integer> {
    @Override
    public Integer call() {
        org.ratschlab.util.Utils.tieSystemOutAndErrToLog();
        return null;
    }
}
