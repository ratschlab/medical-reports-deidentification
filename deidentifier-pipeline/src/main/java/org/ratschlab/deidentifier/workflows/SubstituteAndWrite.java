package org.ratschlab.deidentifier.workflows;

import gate.Document;
import org.ratschlab.deidentifier.substitution.SubstitutionStrategy;

import java.util.ArrayList;
import java.util.List;

public class SubstituteAndWrite extends DefaultWorkflowConcern {
    private SubstitutionStrategy strat;
    private List<WorkflowConcern> writers;

    public SubstituteAndWrite(SubstitutionStrategy strat, WorkflowConcern writer) {
        this.strat = strat;
        this.writers = new ArrayList<>();
        writers.add(writer);
    }

    public SubstituteAndWrite(SubstitutionStrategy strat, List<WorkflowConcern> writers) {
        this.strat = strat;
        this.writers = writers;
    }

    @Override
    public Document postProcessDoc(Document doc) {
        Document substDoc = strat.substitute(doc);

        for(WorkflowConcern writer : this.writers) {
            writer.postProcessDoc(substDoc);
        }

        return doc; //returning original document on purpose!
    }

    @Override
    public void doneHook() {
        for(WorkflowConcern writer : this.writers) {
            writer.doneHook();
        }
    }
}
