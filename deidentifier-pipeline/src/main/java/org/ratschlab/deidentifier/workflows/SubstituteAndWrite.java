package org.ratschlab.deidentifier.workflows;

import gate.Document;
import org.ratschlab.deidentifier.substitution.SubstitutionStrategy;

public class SubstituteAndWrite extends DefaultWorkflowConcern {
    private SubstitutionStrategy strat;
    private WorkflowConcern writer;

    public SubstituteAndWrite(SubstitutionStrategy strat, WorkflowConcern writer) {
        this.strat = strat;
        this.writer = writer;
    }

    @Override
    public Document postProcessDoc(Document doc) {
        Document substDoc = strat.substitute(doc);
        writer.postProcessDoc(substDoc);
        return doc; //returning original document on purpose!
    }

    @Override
    public Document postProcessMergedDoc(Document doc) {
       return writer.postProcessMergedDoc(doc);
    }

    @Override
    public void doneHook() {
        writer.doneHook();
    }
}
