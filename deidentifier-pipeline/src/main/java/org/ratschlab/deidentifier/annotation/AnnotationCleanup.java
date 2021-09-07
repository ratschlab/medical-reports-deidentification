package org.ratschlab.deidentifier.annotation;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


@CreoleResource(
        name = "Annotation Cleanup",
        comment = "Annotates tokens based on previous metadata")
public class AnnotationCleanup extends AbstractLanguageAnalyser {

    private static final Logger log = Logger.getLogger(AnnotationNormalizer.class);

    /**
     * Annotation set name from which this PR will take its input annotations.
     */
    private String inputASName;

    /**
     * Annotation set name into which this PR will create new annotations.
     */
    private String outputASName;

    private Set<String> annotationTypes;

    public String getInputASName() {
        return inputASName;
    }

    @gate.creole.metadata.Optional
    @RunTime
    @CreoleParameter(comment = "The annotation set used for input annotations")
    public void setInputASName(String inputASName) {
        this.inputASName = inputASName;
    }

    public String getOutputASName() {
        return outputASName;
    }

    @gate.creole.metadata.Optional
    @RunTime
    @CreoleParameter(comment = "The annotation set used for output annotations")
    public void setOutputASName(String outputASName) {
        this.outputASName = outputASName;
    }

    public Set<String> getAnnotationTypes() {
        return annotationTypes;
    }

    @gate.creole.metadata.Optional
    @RunTime
    @CreoleParameter(comment = "The annotation types considered")
    public void setAnnotationTypes(Set<String> annotationTypes) {
        this.annotationTypes = annotationTypes;
    }

    /**
     * Execute this this is some experiment over the current document.
     *
     * @throws ExecutionException if an error occurs during processing.
     */
    public void execute() throws ExecutionException {
        Document doc = getDocument();
        if (doc == null) {
            return;
        }

        AnnotationSet inputAS = doc.getAnnotations(inputASName);
        AnnotationSet outputAS = doc.getAnnotations(outputASName);

        org.ratschlab.deidentifier.annotation.Utils.removeEmptyAnnotations(inputAS);

        // TODO: enforce distinct annotations!

        for(String type : inputAS.getAllTypes().stream().filter(s -> annotationTypes.contains(s)).collect(Collectors.toList())) {
            List<Annotation> annots = inputAS.get(type).inDocumentOrder().
                    stream().filter(a -> !a.getFeatures().isEmpty()).collect(Collectors.toList()); // filtering out annotations coming from original markup but happening to have the same name as a tag

            if(annots.isEmpty()) {
                continue;
            }

            Annotation cur = annots.get(0);
            for(int i = 1; i < annots.size(); i++) {
                Annotation next = annots.get(i);

                Optional<String> typeCur = Optional.ofNullable(cur.getFeatures().get("type")).map(o -> o.toString());
                Optional<String> typeNext = Optional.ofNullable(next.getFeatures().get("type")).map(o -> o.toString());

                boolean typeMissingInOne = (!typeCur.isPresent() ^ !typeNext.isPresent()) || typeNext.equals(Optional.of("other")) || typeCur.equals(Optional.of("other"));

                boolean isSameType = typeCur.equals(typeNext);

                if((typeMissingInOne || isSameType) && cur.coextensive(next)) {
                    FeatureMap mergedFeatures = mergeFeatureMaps(cur.getFeatures(), next.getFeatures());

                    if(mergedFeatures.containsKey("type")) {
                        Set<String> curTypes = new HashSet(Arrays.asList(mergedFeatures.get("type").toString().split(",")));

                        Set<String> newTypes = curTypes;
                        // removing 'other' type if some other type is present, since it doesn't add any additional information
                        if(curTypes.size() > 1) {
                            newTypes = curTypes.stream().filter(s -> !s.equals("other")).collect(Collectors.toSet());
                        }

                        mergedFeatures.put("type", String.join(",", newTypes));
                    }

                    cur = next;
                    cur.setFeatures(mergedFeatures);
                } else if((typeMissingInOne || isSameType)  && cur.withinSpanOf(next)) {
                    cur = next;
                } else if((typeMissingInOne || isSameType)  && next.withinSpanOf(cur)){
                    // don't do anything, skip next
                } else {
                    outputAS.add(cur);
                    cur = next;
                }
            }

            // doesn't make sense to annotate dangling whitespaces, but causes issues further down the line.
            String curContent = gate.Utils.stringFor(doc, cur);
            int i = curContent.length() - 1;
            int trim = 0;
            while(i >= 0 && Character.isWhitespace(curContent.charAt(i))) {
                i--; trim++;
            }

            if(trim > 0) {
                try {
                    outputAS.add(cur.getStartNode().getOffset(), cur.getEndNode().getOffset() - trim, cur.getType(), cur.getFeatures());
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                outputAS.add(cur);
            }
        }
    }

    static FeatureMap mergeFeatureMaps(FeatureMap a, FeatureMap b) {
        FeatureMap ret = Factory.newFeatureMap();

        Set<Object> keys = new HashSet(a.keySet());
        keys.addAll(b.keySet());

        for(Object key : keys) {
            String aVal = a.getOrDefault(key, "").toString();  // TODO warning if not string
            String bVal = b.getOrDefault(key, "").toString();

            if(!aVal.isEmpty() && !bVal.isEmpty() && !aVal.equals(bVal)) {
                ret.put(key, aVal + "," + bVal);
            } else if(aVal.isEmpty()) {
                ret.put(key, bVal);
            } else {
                ret.put(key, aVal);
            }
        }

        return ret;
    }
}
