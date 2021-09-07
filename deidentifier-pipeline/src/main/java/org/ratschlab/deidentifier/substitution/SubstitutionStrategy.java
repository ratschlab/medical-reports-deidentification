package org.ratschlab.deidentifier.substitution;

import gate.Document;

public interface SubstitutionStrategy {
    Document substitute(Document doc);
}
