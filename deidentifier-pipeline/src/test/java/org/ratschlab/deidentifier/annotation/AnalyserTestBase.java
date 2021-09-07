package org.ratschlab.deidentifier.annotation;

import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import org.ratschlab.deidentifier.pipelines.PipelineUtils;

public class AnalyserTestBase {
    public void initialize() {
        try {
            Gate.init();
        } catch (GateException e) {
            e.printStackTrace();
        }
        PipelineUtils.registerDeidComponents();
    }

    public static Document createDummyDoc() {
        try {
            return Factory.newDocument("hello_world_very_long_text");
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }

        return null;
    }
}
