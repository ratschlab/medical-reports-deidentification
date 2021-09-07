package org.ratschlab.deidentifier.utils;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Node;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TokenWindowsExtractor {
    public static final String DEFAULT_TOKEN = "Token";

    private List<Annotation> tokenList;
    private Map<Annotation, Integer> tokenLookup;

    public TokenWindowsExtractor(AnnotationSet as) {
        this.tokenList = new ArrayList<>(as.get(DEFAULT_TOKEN).inDocumentOrder());
        this.tokenLookup = IntStream.range(0, tokenList.size()).boxed().
                collect(Collectors.toMap(i -> tokenList.get(i), i -> i));

        this.tokenLookup.size();
    }

    public Pair<Node, Node> getAnnotationWindow(Annotation token, int window) {
        Integer idx = tokenLookup.get(token);

        if(idx != null) {
            Annotation start = tokenList.get(Math.max(idx - window, 0));
            Annotation end = tokenList.get(Math.min(idx + window, tokenList.size() - 1));

            return Pair.of(start.getStartNode(), end.getEndNode());
        } else {
           throw new IllegalArgumentException(String.format("Invalid token %s %s", token.getType(), token.toString()));
        }
    }

    // Called from JAPE
    public boolean annotationInWindowPresent(Annotation token, int window, AnnotationSet as, String type) {
        Pair<Node, Node> nodes = getAnnotationWindow(token, window);
        AnnotationSet covering = as.get(nodes.getLeft().getOffset(), nodes.getRight().getOffset());

        return !covering.get(type).isEmpty();
    }
}
