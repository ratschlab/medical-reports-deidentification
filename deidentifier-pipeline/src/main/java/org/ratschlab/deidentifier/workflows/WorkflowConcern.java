package org.ratschlab.deidentifier.workflows;

import gate.Document;

// TODO rename?
public interface WorkflowConcern {
    Document preProcessDoc(Document doc);
    Document postProcessDoc(Document doc);
    Document postProcessMergedDoc(Document doc);
    void doneHook();
}
