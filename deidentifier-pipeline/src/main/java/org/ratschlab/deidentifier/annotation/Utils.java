package org.ratschlab.deidentifier.annotation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import gate.*;
import gate.util.InvalidOffsetException;
import gate.util.OffsetComparator;
import org.apache.commons.lang3.tuple.Pair;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysGeneral;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysName;
import org.ratschlab.deidentifier.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utils functions is in JAPE rules
 */
public class Utils {
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
                    features.put("format", "ll");
                    features.put(FeatureKeysName.LASTNAME, gate.Utils.stringFor(doc, ans.get(i)));

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

    private static final String FORMAT_SEP = "_";
    private static final String FORMAT_COMPONENT_SEP = "-";
    private static String adjustFormatForCase(String f, Annotation a, Document doc) {
        if(f.contains(FORMAT_COMPONENT_SEP)) {
            // don't adjust case for formats consisting of multiple parts
            return f;
        }

        String text = gate.Utils.stringFor(doc, a);

        if(text.toUpperCase().equals(text)) {
            return f.toUpperCase();
        }
        else if(text.toLowerCase().equals(text)) {
            return f.toLowerCase();
        }

        return f;
    }

    private static String determineNameFormatFromBindings(Map<String,AnnotationSet> bindings, Document doc) {
        List<String> formats = bindings.keySet().stream().filter(s -> s.startsWith("format")).
            map(s -> Pair.of(s, bindings.get(s).iterator().next())).
            sorted(Comparator.comparing(Pair::getValue)). // order by annotation
            map(p -> adjustFormatForCase(p.getKey(), p.getValue(), doc)).
            collect(Collectors.toList());

        if(formats.isEmpty()) {
            return "";
        }

        return formats.stream().map(s -> s.split(FORMAT_SEP)).filter(a -> a.length > 1).map(a -> a[1].replaceAll(FORMAT_COMPONENT_SEP, "").
            replaceAll("DASH", "-").
            replaceAll("SPACE", " ").
            replaceAll("SLASH", "/")).collect(Collectors.joining(" "));
    }

    public static void addNameAnnotation(String rule, String type, String format, String nameBinding, Map<String,AnnotationSet> bindings, Document doc, AnnotationSet outputAs) {
        if(!bindings.containsKey(nameBinding)) {
            return;
        }

        FeatureMap feat = Factory.newFeatureMap();

        feat.put(FeatureKeysGeneral.RULE, rule);
        feat.put(FeatureKeysGeneral.TYPE, type);

        if(format.isEmpty()) {
            format = determineNameFormatFromBindings(bindings, doc);
        }
        feat.put(FeatureKeysName.NAME_FORMAT, format);

        if(format.isEmpty()) {
            System.out.println("WARNING: didn't find format " + rule + " " + gate.Utils.stringFor(doc, bindings.get(nameBinding).iterator().next()));
        }

        Map<String, String> fieldMappings = ImmutableMap.of("firstname", FeatureKeysName.FIRSTNAME,
            "lastname", FeatureKeysName.LASTNAME,
            "signature", FeatureKeysName.NAME_SIGNATURE,
            "salutation", FeatureKeysName.NAME_SALUTATION
        );

        for(Map.Entry<String, String> e: fieldMappings.entrySet()) {
            Stream<AnnotationSet> annotationsWithTag = bindings.keySet().stream().filter(s -> s.startsWith(e.getKey())).map(s -> bindings.get(s));

            Stream<String> annotatedTexts = annotationsWithTag.map(as -> as.stream().sorted().map(a -> gate.Utils.stringFor(doc, a)).collect(Collectors.joining(" ")));

            String annotatedText = annotatedTexts.collect(Collectors.joining(" ")).
                replace(" .", "."); // remove preceding "."

            if(!annotatedText.isEmpty()) {
                feat.put(e.getValue(), annotatedText);
            }
        }

        if(format.equals("S") && !feat.containsKey(FeatureKeysName.NAME_SIGNATURE)) {
            feat.put(FeatureKeysName.NAME_SIGNATURE, gate.Utils.stringFor(doc, bindings.get(nameBinding).iterator().next()));
        }

        outputAs.add(bindings.get(nameBinding).firstNode(), bindings.get(nameBinding).lastNode(), "Name", feat);
    }

    public static void addDateAnnotation(String rule, String type, Map<String,AnnotationSet> bindings, Document doc, AnnotationSet outputAs) {
        if(!bindings.containsKey("date1")) {
            return;
        }

        List<FeatureMap> featuresLst =  ImmutableList.of(
                processDateFeatures(rule, type, doc, "1", bindings),
                processDateFeatures(rule, type, doc, "2", bindings));

        // fill in year, month if available in other date
        for(int i = 0; i < 2; i++) {
            for(String k : ImmutableList.of("month", "year")) {
                int otherIndex = 1 - i;
                if(!featuresLst.get(i).containsKey(k) && featuresLst.get(otherIndex).containsKey(k)) {
                    featuresLst.get(i).put(k, featuresLst.get(otherIndex).get(k));
                }
            }
        }

        outputAs.add(bindings.get("date1").firstNode(), bindings.get("date1").lastNode(), "Date", featuresLst.get(0));

        if(bindings.containsKey("date2")) {
            outputAs.add(bindings.get("date2").firstNode(), bindings.get("date2").lastNode(), "Date", featuresLst.get(1));
        }
    }

    private static Set<String> DATE_COMPONENTS_SEPARATORS = ImmutableSet.of("-", " ", ".", "/", ",");

    private static String separatorAfterAnnot(Document doc, Annotation a) {
        try {
            DocumentContent content = doc.getContent();

            long start = a.getEndNode().getOffset();
            long pos = start;

            StringBuffer sep = new StringBuffer("");
            boolean hasWhiteSpace = false;
            boolean notAllWhiteSpace = false;

            String prev = "";
            while(pos < content.size()-1L && DATE_COMPONENTS_SEPARATORS.contains(content.getContent(pos, pos+1L).toString())) {
                String c = content.getContent(pos, pos+1L).toString();

                if(c.equals(" ")) {
                    hasWhiteSpace = true;

                    if(prev.equals(".") || prev.equals(",")) {
                        sep.append(c);
                    }
                }

                if(!c.equals(" ")) {
                    notAllWhiteSpace = true;

                    if(!c.equals(prev)) {
                        sep.append(c);
                    }
                }

                prev = c;
                pos++;
            }

            if(hasWhiteSpace && !notAllWhiteSpace) {
                return " ";
            }

            return sep.toString();

        } catch(InvalidOffsetException e) {}

        return "";
    }

    private static String removeLeadingZeroFromDateComponent(String annotStr) {
        return org.ratschlab.util.Utils.maybeParseInt(annotStr).map(i -> i.toString()).orElse(annotStr);
    }

    private static Optional<Annotation> getAnnotationWithPrefix(String prefix, Map<String,AnnotationSet> bindings) {
        Optional<String> existingKey = bindings.keySet().stream().filter(s -> s.startsWith(prefix)).findFirst();
        return existingKey.map(k -> bindings.get(k).iterator().next());
    }

    private static FeatureMap processDateFeatures(String rule, String type, Document doc, String datePostfix, Map<String,AnnotationSet> bindings) {
        FeatureMap feat = Factory.newFeatureMap();

        feat.put("rule", rule);
        feat.put("type", type);

        List<Pair<String, Annotation>> dateComponents = new ArrayList<>();

        // extract day
        Optional<Annotation> dayAnnotation = getAnnotationWithPrefix("day" + datePostfix, bindings);
        dayAnnotation.ifPresent(day -> {
            String dayStr = gate.Utils.stringFor(doc, day);
            String dayFormat = "dd" + separatorAfterAnnot(doc, day);

            dateComponents.add(Pair.of(dayFormat, day));
            feat.put("day", removeLeadingZeroFromDateComponent(dayStr));
        });

        DateUtils dateUtils = new DateUtils();
        DateUtils dateUtilsEng = new DateUtils(Locale.ENGLISH);

        // extract month
        Optional<Annotation> monthAnnotation = getAnnotationWithPrefix("month" + datePostfix, bindings);
        monthAnnotation.ifPresent(month -> {
            String monthStr = gate.Utils.stringFor(doc, month);
            String monthFormat = dateUtils.determineMonthFormat(monthStr).
                orElse(dateUtilsEng.determineMonthFormat(monthStr).orElse(new SimpleDateFormat("MM"))).
                toPattern() + separatorAfterAnnot(doc, month);

            dateComponents.add(Pair.of(monthFormat, month));
            feat.put("month", removeLeadingZeroFromDateComponent(monthStr));
        });

        // extract year
        Optional<Annotation> yearAnnotation = getAnnotationWithPrefix("year" + datePostfix, bindings);
        yearAnnotation.ifPresent(year -> {
            String yearStr = gate.Utils.stringFor(doc, year);
            String yearFormat = dateUtils.determineYearFormat(yearStr).map(df -> df.toPattern()).orElse("yyyy") +
                    separatorAfterAnnot(doc, year).replace(".", ""); // year not followed by dot

            String yearStrFixed = yearStr;
            if(yearStr.length() == 2) {
                // 03 --> 2003
                yearStrFixed = org.ratschlab.util.Utils.maybeParseInt(yearStr).map(x -> (x < 50 ? 2000 + x : 1900 + x)).
                    map(Object::toString).orElse(yearStr);
            }

            dateComponents.add(Pair.of(yearFormat, year));
            feat.put("year", yearStrFixed);
        });

        String dateFormat = dateComponents.stream().sorted(Map.Entry.comparingByValue()).
               map(p -> p.getKey()).collect(Collectors.joining()).
                // remove dangling date component separators, except the dot
                replaceAll(String.format("[%s]$", DATE_COMPONENTS_SEPARATORS.stream().filter(s -> !s.equals(".")).collect(Collectors.joining())), "").
                replaceAll(",?\\s*$", "");

        feat.put("format", dateFormat);

        return feat;
    }

    public static boolean possibleSlashDate(Map<String,AnnotationSet> bindings, Document doc) {
        if(!bindings.containsKey("month1") || !bindings.containsKey("year1") ) {
            return false;
        }

        String month = gate.Utils.stringFor(doc, bindings.get("month1").iterator().next());
        String year = gate.Utils.stringFor(doc, bindings.get("year1").iterator().next());

        int monthNum = org.ratschlab.util.Utils.maybeParseInt(month).orElse(0);
        int yearNum = org.ratschlab.util.Utils.maybeParseInt(year).orElse(0);

        if(monthNum > yearNum || (year.length() > 0 && year.charAt(0) == '0') || (month.length() > 0 && month.charAt(0) == '0')) {
            return true; // not a scale
        }

        if(month.length() == 1) {
            return false; // single digit month, probably part of scale.
        }

        return true;
    }
}
