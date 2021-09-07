package org.ratschlab.deidentifier.workflows;

import gate.Corpus;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.persist.PersistenceException;
import gate.util.GateException;
import org.ratschlab.deidentifier.pipelines.PipelineFactory;
import org.ratschlab.gate.GateTools;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Set;

public class EvaluateCorpus extends DefaultWorkflowConcern {


    private File corpusOutputDirPath;
    private File reportOutputDir;

    public EvaluateCorpus(String key, String response, Set<String> annotationTypes, File corpusOutputDirPath, File reportOutputDir) {
        this.corpusOutputDirPath = corpusOutputDirPath;
        this.key = key;
        this.response = response;
        this.annotationTypes = annotationTypes;
        this.reportOutputDir = reportOutputDir;
    }

    private String key;
    private String response;
    private Set<String> annotationTypes;

    @Override
    public void doneHook() {
        // only trigger evaluation if a marked corpus was provided
        System.out.println("Evaluating...");

        try {
            SerialAnalyserController evalPipeline = PipelineFactory.setupEvaluationPipeline(key, response, annotationTypes, reportOutputDir);

            Corpus corpus = GateTools.openCorpus(corpusOutputDirPath);

            evalPipeline.setCorpus(corpus);
            evalPipeline.execute();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (PersistenceException e) {
            e.printStackTrace();
        } catch (GateException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
}
