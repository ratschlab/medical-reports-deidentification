package org.ratschlab.deidentifier.utils;

import gate.Annotation;
import gate.AnnotationSet;
import org.apache.commons.lang3.Range;

import java.util.*;
import java.util.stream.Collectors;

public class AnnotationUtils {
    public static Set<Range<Long>> annotationRanges(AnnotationSet as) {
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
}
