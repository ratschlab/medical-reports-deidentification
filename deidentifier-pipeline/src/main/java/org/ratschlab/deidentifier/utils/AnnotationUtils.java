package org.ratschlab.deidentifier.utils;

import gate.Annotation;
import gate.AnnotationSet;
import org.apache.commons.lang3.Range;

import java.util.*;
import java.util.stream.Collectors;

public class AnnotationUtils {
    public static Set<Range<Long>> annotationRanges(List<Annotation> as) {
        if(as.isEmpty()) {
            return Collections.emptySet();
        }

        List<Annotation> sorted = as.stream().
            sorted(Comparator.comparingLong(a -> a.getStartNode().getOffset())).
            collect(Collectors.toList());

        Set<Range<Long>> ranges = new HashSet<>();

        Annotation first = sorted.get(0);
        Range<Long> last = Range.between(first.getStartNode().getOffset(), first.getEndNode().getOffset());

        if(as.size() == 1) {
            ranges.add(last);
            return ranges;
        }

        for(Annotation a : sorted.subList(1, sorted.size())) {
            // inclusive ranges
            Range<Long> ar = Range.between(a.getStartNode().getOffset(), a.getEndNode().getOffset());

            if(last.isOverlappedBy(ar) || last.getMaximum() == ar.getMinimum()) {
                // calculate union
                last = Range.between(last.getMinimum(), Math.max(last.getMaximum(), ar.getMaximum()));
            } else {
                ranges.add(last);
                last = ar;
            }
        }

        ranges.add(last);

        return ranges;
    }

    public static Set<Range<Long>> annotationRanges(AnnotationSet as) {
        return annotationRanges(as.stream().collect(Collectors.toList()));
    }

    public static List<String> sortOverlappingAnnotations(AnnotationSet overlapping) {
        List<String> parents = overlapping.stream().
                sorted((a1, a2) -> {
                    // descending order in size (from larger to smaller)
                    int diff = (int) (a2.getEndNode().getOffset() - a2.getStartNode().getOffset()) - (int) (a1.getEndNode().getOffset() - a1.getStartNode().getOffset());
                    if (diff == 0) {
                        return a1.getId() - a2.getId(); // breaking ties with ID (in ascending order)
                    } else {
                        return diff;
                    }
                }).
                map(an -> an.getType()).
                collect(Collectors.toList());

        return parents;
    }

    public static void sortAnnotations(List<Annotation> annotations) {
        // proper sorting
        annotations.sort((a1, a2) -> {
            int result = a1.getStartNode().getOffset().compareTo(a2.getStartNode().getOffset());
            if (result == 0) {
                result = -a1.getEndNode().getOffset().compareTo(a2.getEndNode().getOffset());

                if (result == 0) {
                    result = a1.getId().compareTo(a2.getId());
                }
            }

            return result;
        });
    }

    public static boolean hasOverlappingAnnotations(AnnotationSet as) {
        for(Annotation a1 : as) {
            for(Annotation a2 : as) {
                if(!a1.equals(a2) && a1.overlaps(a2)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void removeEmptyAnnotations(AnnotationSet as) {
        List<Annotation> toRemove = as.stream().filter(a -> a.getEndNode().getOffset() - a.getStartNode().getOffset() == 0).collect(Collectors.toList());
        toRemove.forEach(a -> as.remove(a));
    }

    public static void removeRedundantAnnotations(AnnotationSet as) {
        // remove annotations either contained in another annotation, or arbitrarily one coextensive annotation
        List<Annotation> toRemove = new ArrayList<>();
        for(Annotation a1 : as) {
            for(Annotation a2: as) {
                if(!a1.equals(a2) && (a1.coextensive(a2) && a1.getId() < a2.getId() || a1.withinSpanOf(a2))) {
                    toRemove.add(a1);
                }
            }
        }

        toRemove.forEach(a -> as.remove(a));
    }
}
