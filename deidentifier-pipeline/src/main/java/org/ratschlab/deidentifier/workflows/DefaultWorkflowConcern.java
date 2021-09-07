package org.ratschlab.deidentifier.workflows;

import gate.Document;

public class DefaultWorkflowConcern implements WorkflowConcern {
    @Override
    public Document preProcessDoc(Document doc) {
        return doc;
    }

    @Override
    public Document postProcessDoc(Document doc) {
        return doc;
    }

    @Override
    public Document postProcessMergedDoc(Document doc) {
        return doc;
    }

    @Override
    public void doneHook() {

    }
}
