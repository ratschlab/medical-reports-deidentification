package org.ratschlab.deidentifier.utils.paths;

import gate.Annotation;
import gate.AnnotationSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.ratschlab.deidentifier.utils.AnnotationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class PathConstraint {
    public PathConstraint(String annotationPatternName, List<String> expectedParents) {
        this.annotationName = annotationPatternName;
        this.expectedParents = expectedParents;
    }

    private final String annotationName;
    private final List<String> expectedParents;

    public String getAnnotationName() {
        return annotationName;
    }

    public List<String> getExpectedParents() {
        return expectedParents;
    }

    public static boolean checkConstraints(Annotation a, PathConstraint constraint, AnnotationSet annots) {
        // TODO: should be a regex!
        if(!a.getType().equals(constraint.annotationName)) {
            return false;
        }

        // check parent constraints
        return parentConstraintsValid(annots, a, constraint.getExpectedParents(), Collections.emptySet());
    }

    public static boolean parentConstraintsValid(AnnotationSet inputAS, Annotation a, List<String> expectedParents, Set<String> blacklistedParents) {
        AnnotationSet overlapping = gate.Utils.getOverlappingAnnotations(inputAS, a);

        List<String> completePath = AnnotationUtils.sortOverlappingAnnotations(overlapping);
        if(completePath.size() > 0) {
            completePath.remove(0); // remove root
        }

        List<String> parents = completePath;

        Set<String> parentSet = new HashSet(parents);
        boolean noBlacklistedParent = !blacklistedParents.stream().anyMatch(s -> parentSet.contains(s));

        if(!noBlacklistedParent) {
            return false;
        }

        int expected_ind = 0;

        if(!expectedParents.isEmpty()) {
            boolean acceptingAny = false;

            if(expectedParents.get(expected_ind).isEmpty()) {
                acceptingAny = true;
                expected_ind++;
            }

            for(String p : parents) {

                if(expected_ind >= expectedParents.size()) {
                    return acceptingAny;  // "consumed" all constraints, if accepting any it is ok if still some children left
                }

                String curParent = expectedParents.get(expected_ind);


                if (p.matches(curParent)) {
                    expected_ind++;
                    acceptingAny = false;

                    if (expected_ind < expectedParents.size() && expectedParents.get(expected_ind).isEmpty()) {
                        acceptingAny = true;
                        expected_ind++;
                    }
                } else if (!acceptingAny) {
                    return false; // violated constraint
                }
            }
        }

        return expected_ind >= expectedParents.size() || (expected_ind == expectedParents.size() - 1 && expectedParents.get(expected_ind).isEmpty()); // check if all constraints consumed
    }

    public static PathConstraint constructPath(String originalPath) {
        List<String> completePath = Arrays.stream(originalPath.split(("/"))).map(s -> s.trim()).
                collect(Collectors.toList());

        boolean singleField = false;
        // add empty root for single fields
        if(completePath.size() == 1) {
            completePath.add(0, ""); //prefix
            singleField = true;
        } else if(completePath.get(0).isEmpty()) {
            // otherwise removing empty ""
            completePath.remove(0);
        }

        String originalFieldNamePattern = completePath.get(completePath.size() - 1);

        if(singleField) {
            completePath.add(""); // postfix single fields
        }

        return new PathConstraint(originalFieldNamePattern, completePath);
    }


    public static List<PathConstraint> loadFieldBlacklistPath(File path) throws IOException {
        CSVParser records = CSVParser.parse(path, Charset.defaultCharset(), CSVFormat.newFormat(';').withCommentMarker('#'));

        List<PathConstraint> ret = new ArrayList<>();

        for(CSVRecord r : records) {
            if (r.size() < 1) {
                continue;
            }

            ret.add(PathConstraint.constructPath(r.get(0)));
        }

        return ret;
    }
}
