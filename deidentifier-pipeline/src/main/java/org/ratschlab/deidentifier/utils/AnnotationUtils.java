package org.ratschlab.deidentifier.utils;

import com.google.common.collect.Range;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.annotation.AnnotationSetImpl;

import java.util.*;
import java.util.stream.Collectors;

public class AnnotationUtils {
    public static Set<Range<Long>> annotationRanges(List<Annotation> as) {
        return annotationRanges(as, Collections.EMPTY_SET);
    }

    public static Set<Range<Long>> annotationRanges(List<Annotation> as, Set<Range<Long>> toleranceRanges) {
        if(as.isEmpty()) {
            return Collections.emptySet();
        }

        List<Annotation> sorted = as.stream().
            sorted(Comparator.comparingLong(a -> a.getStartNode().getOffset())).
            collect(Collectors.toList());

        Set<Range<Long>> ranges = new HashSet<>();

        Annotation first = sorted.get(0);
        Range<Long> last = Range.closedOpen(first.getStartNode().getOffset(), first.getEndNode().getOffset());

        if(as.size() == 1) {
            ranges.add(last);
            return ranges;
        }

        for(Annotation a : sorted.subList(1, sorted.size())) {
            // inclusive ranges
            Range<Long> ar = Range.closedOpen(a.getStartNode().getOffset(), a.getEndNode().getOffset());

            if(ar.isConnected(last) || toleranceRanges.contains(Range.closedOpen(last.upperEndpoint(), ar.lowerEndpoint()))) {
                // calculate union
                last = Range.closedOpen(last.lowerEndpoint(), Math.max(last.upperEndpoint(), ar.upperEndpoint()));
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

    public static Set<Range<Long>> annotationRanges(AnnotationSet as, Set<Range<Long>> toleranceRanges) {
        return annotationRanges(as.stream().collect(Collectors.toList()), toleranceRanges);
    }

    protected static boolean compareSets(Set<Range<Long>> setA, Set<Range<Long>> setB, Set<Range<Long>> spaceRanges) {
        for(Range<Long> a : setA) {
            if(!setB.contains(a)) {
                List<Range<Long>> contained = setB.stream().filter(r -> a.isConnected(r)).collect(Collectors.toList());
                contained.addAll(spaceRanges.stream().filter(r -> a.isConnected(r)).collect(Collectors.toList()));

                assert !contained.isEmpty();

                long min = contained.stream().map(x -> x.lowerEndpoint()).min(Comparator.naturalOrder()).orElseThrow(() -> new AssertionError("Should not be empty"));
                long max = contained.stream().map(x -> x.upperEndpoint()).max(Comparator.naturalOrder()).orElseThrow(() -> new AssertionError("Should not be empty"));

                if(!Range.closedOpen(min, max).encloses(a)) {
                    return false;
                }

            }
        }

        return true;
    }

    public static boolean checkAnnotationCoverage(List<Annotation> origAnnotations, List<Annotation> newAnnotations, AnnotationSet markups) {
        Set<Range<Long>> origRange = AnnotationUtils.annotationRanges(origAnnotations);
        Set<Range<Long>> newRange = AnnotationUtils.annotationRanges(newAnnotations);

        Set<Range<Long>> spaceRanges = new HashSet();

        List<Annotation> allLeaves = AnnotationUtils.computeLeaves(markups);
        allLeaves.sort(Comparator.comparingLong(a -> a.getStartNode().getOffset()));

        Document doc = markups.getDocument();
        for(int i = 0; i < allLeaves.size() - 1; i++) {
            Annotation a = allLeaves.get(i);
            Annotation b = allLeaves.get(i+1);

            if(gate.Utils.stringFor(doc, a.getEndNode().getOffset(), b.getStartNode().getOffset()).matches("\\s+")) {
                try {
                    spaceRanges.add(Range.closedOpen(a.getEndNode().getOffset(), b.getStartNode().getOffset()));
                } catch(Exception e) {
                    throw new AssertionError(e.getMessage());
                }
            }
        }

        boolean ret = compareSets(origRange, newRange, spaceRanges);
        ret &=  compareSets(newRange, origRange, spaceRanges);

        assert ret;
        return ret;
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
                if(a1.equals(a2)) {
                    continue;
                }
                if(a1.coextensive(a2)) {
                    if(a1.getId() < a2.getId()) {
                        toRemove.add(a1);
                    }
                } else if(a1.withinSpanOf(a2)) {
                    toRemove.add(a1);
                }
            }
        }

        toRemove.forEach(a -> as.remove(a));
    }


    public static List<Annotation> computeLeaves(AnnotationSet structuredFieldsOrig) {
        AnnotationSet structuredFields = new AnnotationSetImpl(structuredFieldsOrig);

        List<Annotation> structuredFieldList = structuredFields.inDocumentOrder();

        structuredFieldList.sort((a1, a2) -> {
            if(a1.getStartNode().getOffset() == a2.getStartNode().getOffset()) {
                if(a1.getEndNode().getOffset() == a2.getEndNode().getOffset()) {
                    return a1.getId() - a2.getId();
                }
                return (int) (a2.getEndNode().getOffset() - a1.getEndNode().getOffset());
            }
            return (int) (a1.getStartNode().getOffset() - a2.getStartNode().getOffset());

        });

        List<Annotation> removeStructured = new ArrayList<>();
        for(int i = 0; i < structuredFieldList.size() - 1; i++) {
            Annotation a = structuredFieldList.get(i);
            Annotation b = structuredFieldList.get(i+1);
            if(a.getStartNode().equals(b.getStartNode()) && a.getEndNode().equals(b.getEndNode()) && a.getId() < b.getId()) {
                removeStructured.add(a);
            }
        }

        //structuredFields.removeAll(removeStructured);

        return structuredFields.stream().
                filter(an -> gate.Utils.getContainedAnnotations(structuredFields, an).stream().filter(anc -> !removeStructured.contains(anc) && anc != an).count() == 0).
                collect(Collectors.toList());
    }
}
