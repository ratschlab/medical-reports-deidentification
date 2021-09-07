package org.ratschlab.deidentifier.substitution;

import gate.Document;
import org.ratschlab.deidentifier.workflows.DefaultWorkflowConcern;

public class SubstitutionConcern extends DefaultWorkflowConcern {
    private SubstitutionStrategy subst;

    public SubstitutionConcern(SubstitutionStrategy subst) {
        this.subst = subst;
    }

    @Override
    public Document postProcessDoc(Document doc) {
        return subst.substitute(doc);
    }
}
