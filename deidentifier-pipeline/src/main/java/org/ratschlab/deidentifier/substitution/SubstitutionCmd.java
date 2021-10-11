package org.ratschlab.deidentifier.substitution;

import com.google.common.collect.ImmutableList;
import gate.*;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.ratschlab.DeidCmd;
import org.ratschlab.deidentifier.AnnotationCmd;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysDate;
import org.ratschlab.deidentifier.pipelines.PipelineFactory;
import org.ratschlab.deidentifier.sources.KisimFormat;
import org.ratschlab.deidentifier.sources.KisimSource;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.deidentifier.workflows.*;
import org.ratschlab.gate.GateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(description = "Apply Substitution to Annotated Corpus", name = "substitute")
public class SubstitutionCmd extends DeidCmd {
    private static final Logger log = LoggerFactory.getLogger(SubstitutionCmd.class);

    @CommandLine.Parameters(index = "0", description = "Corpus Dir")
    private File annotatedCorpusDir;

    @CommandLine.Option(names = {"-o"}, description = "Output Dir")
    private File outputDir = null;

    @CommandLine.Option(names = {"--db-config"}, description = "Path to DB config file")
    private File databaseConfigPath = null;

    @CommandLine.Option(names = {"--doc-type-filter"}, description = "Path to file type list to consider")
    private File docTypeFilterPath = null;

    @CommandLine.Option(names = {"--annotation-name"}, description = "Annotation set name containing annotations to deidentify")
    private String finalAnnotationName = AnnotationCmd.PHI_ANNOTATION_NAME;

    @CommandLine.Option(names = {"--rnd-seed"}, description = "Random seed used to pick random values for date shifts (keep the value protected!)")
    private int rngSeed = -1;

    @CommandLine.Option(names = {"--method"}, description = "Substitution Methods: ${COMPLETION-CANDIDATES}", required = true)
    private SubstitutionMethods substMethod = null;

    @CommandLine.Option(names = {"-t"}, description = "Number of parallel pipelines (default: 1)")
    private int threads = 1;

    @CommandLine.Option(names = {"--min-days-shift"}, description = "Minimum number of days to shift")
    private Integer minDaysShift = null;

    @CommandLine.Option(names = {"--max-days-shift"}, description = "Maximum number of days to shift")
    private Integer maxDaysShift = null;

    @CommandLine.Option(names = "--keep-patient-ids", description = "don't substitute patient IDs")
    boolean keepPatientIds = false;

    @CommandLine.Option(names = "--keep-case-ids", description = "don't substitute case IDs")
    boolean keepCaseIds = false;

    @CommandLine.Option(names = "--keep-dates", description = "don't substitute dates (Scrubber substitution method only)")
    boolean keepDates = false;

    @CommandLine.Option(names = "--no-substitute-whole-address", description = "don't substitute whole address, substitute parts annotated within addresses independently")
    boolean substituteWholeAddress = true;

    @CommandLine.Option(names = {"--fields-blacklist"}, description = "Path to files giving field blacklist")
    private File fieldsBlacklistPath = null;

    @CommandLine.Option(names = {"--context-window-size"}, description = "Context window size for replacement tags (default 0). Number of original tokens to add in replacement tags (in case annotations get split up)")
    private int contextWindowForReplacementTags = 0;


    public enum SubstitutionMethods {
        DateShift, ReplacementTags, Scrubber, Identity
    }

    // TODO: add strats option

    public static void main(String[] args) {
        org.ratschlab.util.Utils.tieSystemOutAndErrToLog();
        System.exit(CommandLine.call(new SubstitutionCmd(), args));
    }

    public static Set<String> loadDocTypeSet(File path) throws IOException {
        if(path == null) {
            return Collections.emptySet();
        }
        return Files.readAllLines(path.toPath()).stream().map(s -> s.split(",")[0]).collect(Collectors.toSet());
    }

    @Override
    public Integer call() {
        super.call();

        if(outputDir != null && outputDir.exists()) {
            try {
                Files.delete(outputDir.toPath());
            } catch(DirectoryNotEmptyException e) {}
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Gate.init();

            SerialAnalyserController myController = PipelineFactory.NoOpController();

            List<WorkflowConcern> concerns = new ArrayList<>();

            Function<Document, DeidentificationSubstituter> substFactory = getSubstFactory();

            List<PathConstraint> fieldBlacklist = this.fieldsBlacklistPath != null ? PathConstraint.loadFieldBlacklistPath(this.fieldsBlacklistPath) : Collections.emptyList();

            List<WorkflowConcern> sinks = new ArrayList<>();

            Function<Document, String> kisim2Json = d -> new KisimFormat().documentToJson(d);
            if(outputDir != null) {
                sinks.add(new WriteDocsToFiles(outputDir, kisim2Json, d -> String.format("%s.json", d.getName().replaceAll("\\|", "_"))));
            }

            if(databaseConfigPath != null) {
                sinks.add(new WriteDocsToDatabase(new KisimSource(databaseConfigPath), kisim2Json));
            }

            if(outputDir == null && databaseConfigPath == null) {
                throw new IllegalArgumentException("-o and --db-config cannot be both undefined!");
            }

            concerns.add(new SubstituteAndWrite(new DeidentificationSubstitution(finalAnnotationName, substFactory, this.substituteWholeAddress, fieldBlacklist, this.contextWindowForReplacementTags),
                    sinks));

            Set<String> docIds = loadDocTypeSet(docTypeFilterPath);

            Stream<Optional<Document>> docs = GateTools.readDocsInCorpus(annotatedCorpusDir).
                    filter(d -> docIds.isEmpty() || d.map(x -> docIds.contains(x.getName().split("-")[0])).orElse(false));

            PipelineWorkflow<Optional<Document>> workflow = new PipelineWorkflow<>(
                    docs,
                    id -> id,
                    myController,
                    threads,
                    concerns);

            workflow.run();
        } catch (GateException| MalformedURLException e) {
            e.printStackTrace();
            return 1;
        } catch(IOException e) {
            e.printStackTrace();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    private static final List<SimpleDateFormat> dateFormats = ImmutableList.of(new SimpleDateFormat("dd.M.yy"),
            new SimpleDateFormat("d.M.yy"), new SimpleDateFormat("d.M.yyyy"), new SimpleDateFormat("d.MM.yyyy"),
            new SimpleDateFormat("dd.MM.yy"), new SimpleDateFormat("dd.MM.yyyy"),
            new SimpleDateFormat("MM/yy"), new SimpleDateFormat("MM/yyyy"), new SimpleDateFormat("M/yyyy"),
            new SimpleDateFormat("MM.yyyy"), new SimpleDateFormat("dd.MM."),
            new SimpleDateFormat("d.MM."), new SimpleDateFormat("dd.MM"), new SimpleDateFormat("d.MM"),
            new SimpleDateFormat("yyyy"), new SimpleDateFormat("M yyyy"));
    public static void attemptExtractDate(Annotation an, Document doc) {
        FeatureMap feat = an.getFeatures();
        if(feat.containsKey(FeatureKeysDate.DATE_FORMAT)) {
            return;
        }

        String origStr = gate.Utils.stringFor(doc, an);

        String bestFormat = "";
        Date bestDate = null;
        for(SimpleDateFormat fmt : dateFormats) {
            try {
                String format = fmt.toLocalizedPattern();

                // attempt to parse longest
                if(format.length() > bestFormat.length()) {
                    Date date = fmt.parse(origStr);

                    if(fmt.format(date).equals(origStr)) {
                        bestDate = date;
                        bestFormat = format;
                    }
                }

            } catch(ParseException ex) {}
        }

        if(bestDate == null) {
            log.warn(String.format("Was not able to process date: %s", origStr));
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(bestDate);

        feat.put(FeatureKeysDate.DATE_FORMAT, bestFormat);

        if(bestFormat.contains("yy")) {
            feat.put(FeatureKeysDate.YEAR_FORMAT, String.valueOf(cal.get(Calendar.YEAR)));
        }

        feat.put(FeatureKeysDate.MONTH_FORMAT, String.valueOf(cal.get(Calendar.MONTH) + 1));
        feat.put(FeatureKeysDate.DAY_FORMAT, String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
    }

    private Function<Document, DeidentificationSubstituter> getSubstFactory() {
        switch (this.substMethod) {
            case DateShift: {
                if (minDaysShift == null || maxDaysShift == null || rngSeed == -1) {
                    System.err.println("Require all of --min-days-shift, --max-days-shift and --rnd-seed to be set for DateShift");
                    System.exit(1);
                }

                if(keepDates) {
                    System.err.println("--keep-dates is not supported by DateShift substitution!");
                    System.exit(1);
                }

                return this.dateShiftSubstFactory();
            }
            case ReplacementTags: {
                if (keepPatientIds || keepCaseIds || keepDates) {
                    System.err.println("--keep-patient-ids, --keep-case-ids and --keep-dates are not supported by ReplacementTags substitution!");
                    System.exit(1);
                }

                return this.replacementTagsSubstFactory();
            }
            case Scrubber: {
                return d -> new ScrubberSubstitution(keepPatientIds, keepCaseIds, keepDates);
            }
            case Identity: {
                return d -> new IdentitySubstitution();
            }
            default: {
                System.err.println("Requiring a substitution method to be set");
                System.exit(1);
                return null;
            }
        }
    }

    private Function<Document, DeidentificationSubstituter> dateShiftSubstFactory() {
        return d -> {
            AnnotationSet dates = d.getAnnotations(finalAnnotationName).get("Date");
            dates.forEach(an -> attemptExtractDate(an, d));

            int docSeed = d.getName().hashCode() + this.rngSeed;

            // take the largest year as default year
            int defaultYear = dates.stream().
                    map(an -> org.ratschlab.util.Utils.maybeParseInt(an.getFeatures().getOrDefault(FeatureKeysDate.YEAR_FORMAT,"").toString())).
                    filter(o -> o.isPresent()).map(x -> x.get()).max((a,b) -> a - b).orElse(0);

            DateShiftSubstitution subst = new DateShiftSubstitution(docSeed, defaultYear, this.minDaysShift, this.maxDaysShift);
            subst.setKeepCaseIDs(this.keepCaseIds);
            subst.setKeepPatientIDs(this.keepPatientIds);
            return subst;
        };
    }

    private Function<Document, DeidentificationSubstituter> replacementTagsSubstFactory() {
        return d -> {
            AnnotationSet dates = d.getAnnotations(finalAnnotationName).get("Date");
            dates.forEach(an -> attemptExtractDate(an, d));

            return new ReplacementTagsSubstitution();
        };
    }
}
