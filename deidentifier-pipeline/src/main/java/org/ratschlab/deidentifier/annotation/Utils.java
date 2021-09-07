package org.ratschlab.deidentifier.annotation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gate.*;
import gate.util.Files;
import gate.util.InvalidOffsetException;
import gate.util.OffsetComparator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    public static List<String> sortAnnotations(AnnotationSet overlapping) {
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

    public static File createFileFromUrlOrPath(String urlOrPath) {
        String url = urlOrPath;
        if(!url.startsWith("file")) {
            url = "file:///" + urlOrPath;
        }

        try {
            return Files.fileFromURL(new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return new File(urlOrPath); //fallback
    }


    public List<Annotation> tokensInWindow(AnnotationSet as, Annotation an, int windowSize) {
        List<Annotation> tokenList = as.get("Token").inDocumentOrder();

        int idx = tokenList.indexOf(an);

        if(idx > 0) {
            List<Annotation> leftWindow = tokenList.subList(Math.max(idx - windowSize, 0), idx);
            List<Annotation> rightWindow = tokenList.subList(Math.min(idx + 1, tokenList.size()), Math.min(idx + windowSize, tokenList.size()));

            leftWindow.addAll(rightWindow);
            return leftWindow;
        }

        return Collections.emptyList();
    }

    public static void removeEmptyAnnotations(AnnotationSet as) {
        List<Annotation> toRemove = as.stream().filter(a -> a.getEndNode().getOffset() - a.getStartNode().getOffset() == 0).collect(Collectors.toList());
        toRemove.forEach(a -> as.remove(a));
    }

    public static void addAnnotationSequence(AnnotationSet as, String annotType, String rule, AnnotationSet outputAS) {
        for(Annotation an : as) {
            gate.FeatureMap features = Factory.newFeatureMap();
            features.put("rule", rule);

            outputAS.add(an.getStartNode(), an.getEndNode(), annotType, features);
        }
    }

    public static void annotateIfBeginningOfSentence(AnnotationSet inputAS, Annotation an, String annotType, String rule, AnnotationSet outputAS) {
        AnnotationSet overlappingSentences = inputAS.getCovering("Sentence", an.getStartNode().getOffset(), an.getEndNode().getOffset());

        for(Annotation sent : overlappingSentences) {
            if(sent.getStartNode().getOffset() == an.getStartNode().getOffset()) {
                FeatureMap fm = Factory.newFeatureMap();
                fm.put("rule", rule);
                outputAS.add(an.getStartNode(), an.getEndNode(), annotType, fm);
                break;
            }
        }
    }

    private static boolean isWithinGeneralOrMedical(AnnotationSet inputAS, Annotation an) {
        return gate.Utils.getOverlappingAnnotations(inputAS, an).
                get("Lookup").stream().
                anyMatch(a -> a.getFeatures().getOrDefault("majorType", "").toString().matches("general|medical"));
    }

    public static void addDoctorEnumeration(AnnotationSet inputAS, AnnotationSet outputAS, Map<String,AnnotationSet> bindings, String rule, boolean enforceNotGeneralOrMedical, Document doc) {
        List<Annotation> ans = new ArrayList<>();

        if(bindings.containsKey("remaining_toks")) {
            ans = org.apache.commons.compress.utils.Lists.newArrayList(bindings.get("remaining_toks").iterator());
        }

        ans.add(0, bindings.get("first_tok").iterator().next());

        Collections.sort(ans, new OffsetComparator());

        if(enforceNotGeneralOrMedical) {
            while(!ans.isEmpty()) {
                if(isWithinGeneralOrMedical(inputAS, ans.get(0))) {
                    ans.remove(0);
                } else {
                    break;
                }
            }
            while(!ans.isEmpty()) {
                if(isWithinGeneralOrMedical(inputAS, ans.get(ans.size() - 1))) {
                    ans.remove(ans.size() - 1);
                } else {
                    break;
                }
            }
        }

        if(ans.size() < 1) {
            return;
        }

        long lastStartOffset = -1;
        for(int i = 0; i < ans.size(); i++) {
            Annotation ann = ans.get(i);
            if(i + 1 == ans.size() || ans.get(i).getEndNode().getOffset() != ans.get(i + 1).getStartNode().getOffset()) {
                if(lastStartOffset < 0) {
                    lastStartOffset = ans.get(i).getStartNode().getOffset();
                }

                try {
                    gate.FeatureMap features = Factory.newFeatureMap();
                    features.put("type", "medical staff");
                    features.put("rule", rule);
                    features.put("format", "l");
                    features.put(FeatureKeys.LASTNAME, gate.Utils.stringFor(doc, ans.get(i)));

                    outputAS.add(lastStartOffset, ans.get(i).getEndNode().getOffset(), "Name", features);
                    lastStartOffset = -1;
                } catch (InvalidOffsetException e) {
                    e.printStackTrace();
                }
            } else if(lastStartOffset < 0) {
                lastStartOffset = ans.get(i).getStartNode().getOffset();
            }
        }
    }

    public static void addNameAnnotation(String rule, String type, String format, Map<String,AnnotationSet> bindings, Document doc, AnnotationSet outputAs) {
        addNameAnnotation(rule, type, format, "name", bindings, doc, outputAs);
    }

    public static void addNameAnnotation(String rule, String type, String format, String nameBinding, Map<String,AnnotationSet> bindings, Document doc, AnnotationSet outputAs) {
        if(!bindings.containsKey(nameBinding)) {
            return;
        }

        FeatureMap feat = Factory.newFeatureMap();

        feat.put("rule", rule);
        feat.put("type", type);
        feat.put("format", format);

        Map<String, String> fieldMappings = ImmutableMap.of("firstname", FeatureKeys.FIRSTNAME,
                "lastname", FeatureKeys.LASTNAME,
                "signature", FeatureKeys.NAME_SIGNATURE,
                "format", FeatureKeys.NAME_FORMAT,
                "rule", FeatureKeys.RULE);

        for(Map.Entry<String, String> e: fieldMappings.entrySet()) {
            Optional<Annotation> anOpt = bindings.keySet().stream().filter(s -> s.startsWith(e.getKey())).sorted().map(s -> bindings.get(s).iterator().next()).findFirst();

            anOpt.ifPresent(an -> {
                String text = gate.Utils.stringFor(doc, an);
                feat.put(e.getValue(), text);
            });
        }

        outputAs.add(bindings.get(nameBinding).firstNode(), bindings.get(nameBinding).lastNode(), "Name", feat);
    }

    public static void addDateAnnotation(String rule, String type, String dateFormat, String dateFormat2, Map<String,AnnotationSet> bindings, Document doc, AnnotationSet outputAs) {
        if(!bindings.containsKey("date")) {
            return;
        }

        List<FeatureMap> featuresLst =  ImmutableList.of(
                processFeatures(rule, type, doc, ImmutableList.of("year", "month", "day"), bindings),
                processFeatures(rule, type, doc, ImmutableList.of("year2", "month2", "day2"), bindings));

        List<String> formats = new ArrayList(ImmutableList.of(dateFormat, dateFormat2));

        // fill in year, month if available in other date
        for(int i = 0; i < 2; i++) {
            for(String k : ImmutableList.of("month", "year")) {
                int otherIndex = 1 - i;
                if(!featuresLst.get(i).containsKey(k) && featuresLst.get(otherIndex).containsKey(k)) {
                    featuresLst.get(i).put(k, featuresLst.get(otherIndex).get(k));

                    if(formats.get(i).isEmpty() && k.equals("month")) {
                        // month is missing, so is year, hence format dd.
                        formats.set(i, "dd.");
                    }
                    if(formats.get(i).isEmpty() && k.equals("year")) {
                        // month not missing, but year, hence format dd.MM.
                        formats.set(i, "dd.MM.");
                    }
                }
            }
        }

        for(int i = 0; i < 2; i++) {
            String fmt = formats.get(i);
            if(!fmt.isEmpty()) {
                featuresLst.get(i).put("format", fmt);
            }
        }

        outputAs.add(bindings.get("date").firstNode(), bindings.get("date").lastNode(), "Date", featuresLst.get(0));

        if(bindings.containsKey("date2")) {
            outputAs.add(bindings.get("date2").firstNode(), bindings.get("date2").lastNode(), "Date", featuresLst.get(1));
        }
    }

    private static FeatureMap processFeatures(String rule, String type, Document doc, List<String> possibleBindingKeys, Map<String,AnnotationSet> bindings) {
        FeatureMap feat = Factory.newFeatureMap();

        feat.put("rule", rule);
        feat.put("type", type);

        possibleBindingKeys.forEach(k -> {
            if(bindings.containsKey(k)) {
                String annotStr = gate.Utils.stringFor(doc, bindings.get(k).iterator().next());

                String featureName = k;
                if(k.endsWith("2")) {
                    featureName = featureName.substring(0, k.length() - 1);
                }

                if(!featureName.equals("year")) {
                    // e.g month: 01 -> 1
                    annotStr = org.ratschlab.util.Utils.maybeParseInt(annotStr).map(i -> i.toString()).orElse(annotStr);
                } else {
                    if(annotStr.length() == 2) {
                        // 03 --> 2003
                        // TODO: 30 is very arbitrary, expose as config
                        annotStr = org.ratschlab.util.Utils.maybeParseInt(annotStr).map(x -> (x < 30 ? 2000 + x : 1900 + x)).
                                map(Object::toString).orElse(annotStr);
                    }
                }

                feat.put(featureName, annotStr);
            }
        });

        return feat;
    }
}
