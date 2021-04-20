package org.ratschlab.deidentifier.annotation;

import com.fasterxml.jackson.core.JsonParser;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysName;
import org.ratschlab.deidentifier.utils.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


@CreoleResource(
        name = "Annotation Cleanup",
        comment = "Annotates tokens based on previous metadata")
public class AnnotationCleanup extends AbstractLanguageAnalyser {

    private static final Logger log = LoggerFactory.getLogger(AnnotationNormalizer.class);

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

        AnnotationUtils.removeEmptyAnnotations(inputAS);

        trimDanglingWhitespace(inputAS, doc);

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

            outputAS.add(cur);
        }

        inputAS.get("Name").stream().forEach(a -> AnnotationCleanup.cleanupNameAnnotationFeature(a.getFeatures()));
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

    protected static void cleanupNameAnnotationFeature(FeatureMap fm) {
        String nameFormat = fm.getOrDefault(FeatureKeysName.NAME_FORMAT, "").toString();

        // fix case
        String fixed = fixCase(nameFormat, "ff", FeatureKeysName.FIRSTNAME, fm);
        fixed = fixCase(fixed, "ll", FeatureKeysName.LASTNAME, fm);

        String separator = ",";

        List<String> parts = Arrays.stream(fixed.split(separator)).
            map(String::trim).collect(Collectors.toList());

        // remove duplicates and format definitions which are already contained in others (e.g "ff ll, ll" -> "ff ll")
        Set<String> partsNonRedundant = new HashSet();
        for(String part : parts) {
            if(parts.stream().noneMatch(p -> !p.equals(part) && p.contains(part))) {
                partsNonRedundant.add(part);
            }
        }

        String newNameFormat = partsNonRedundant.stream().collect(Collectors.joining(separator));
        if (!newNameFormat.equals(nameFormat)) {
            fm.put(FeatureKeysName.NAME_FORMAT, newNameFormat);
        }
    }

    private static String fixCase(String format, String formatAbbrev, String featureKey, FeatureMap fm) {
        String val = fm.getOrDefault(featureKey, "").toString();

        if (!val.isEmpty() && val.equals(val.toUpperCase())) {
            return format.replaceAll(formatAbbrev, formatAbbrev.toUpperCase());
        }

        return format.replaceAll(formatAbbrev.toUpperCase(), formatAbbrev);
    }

    private static void trimDanglingWhitespace(AnnotationSet as, Document doc) {
        List<Annotation> toRemove = new ArrayList<>();

        String annotTmp = "annotDanglingTmp";
        AnnotationSet tmp = doc.getAnnotations(annotTmp);

        for(Annotation cur : as) {
            // doesn't make sense to annotate dangling whitespaces, but causes issues further down the line.
            String curContent = gate.Utils.stringFor(doc, cur);
            int i = curContent.length() - 1;
            int trim = 0;
            int oldTrim = -1;

            while (i >= 0 && trim != oldTrim) {
                oldTrim = trim;

                if (Character.isWhitespace(curContent.charAt(i))) {
                    i--;
                    trim++;
                }
                if (curContent.substring(0, i + 1).endsWith("\\r") || curContent.substring(0, i + 1).endsWith("\\n")) {
                    i -= 2;
                    trim += 2;
                }
            }

            if (trim > 0) {
                try {
                    tmp.add(cur.getStartNode().getOffset(), cur.getEndNode().getOffset() - trim, cur.getType(), cur.getFeatures());
                    toRemove.add(cur);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        toRemove.forEach(a -> as.remove(a));
        tmp.forEach(a -> as.add(a.getStartNode(), a.getEndNode(), a.getType(), a.getFeatures()));
        doc.removeAnnotationSet(annotTmp);
    }
}
