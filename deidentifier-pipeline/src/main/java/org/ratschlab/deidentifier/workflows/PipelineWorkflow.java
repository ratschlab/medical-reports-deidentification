package org.ratschlab.deidentifier.workflows;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.*;
import akka.stream.javadsl.*;
import gate.Document;
import gate.Factory;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.ratschlab.gate.GateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PipelineWorkflow<I> {
    private static final Logger log = LoggerFactory.getLogger(PipelineWorkflow.class);

    public PipelineWorkflow(Stream<I> docSource, Function<I, Optional<Document>> docConversion,
                            List<Function<Document, Document>> preprocessing,
                            SerialAnalyserController gatePipelineController, int nrParallelGatePipelines,
                            List<Function<Document, Document>> postprocessing,
                            List<Function<Document, Document>> postprocessingMergedDocs,
                            List<Runnable> doneHooks) {
        this.docSource = docSource;
        this.docConversion = docConversion;
        this.preprocessing = preprocessing;
        this.gatePipelineController = gatePipelineController;
        this.nrParallelGatePipelines = nrParallelGatePipelines;
        this.postprocessing = postprocessing;
        this.postprocessingMergedDocs = postprocessingMergedDocs;
        this.doneHooks = doneHooks;
    }

    public PipelineWorkflow(Stream<I> docSource, Function<I, Optional<Document>> docConversion,
                            SerialAnalyserController gatePipelineController, int nrParallelGatePipelines,
                            List<WorkflowConcern> workflowConcerns) {
        this.docSource = docSource;
        this.docConversion = docConversion;
        this.preprocessing = new ArrayList<>();
        this.gatePipelineController = gatePipelineController;
        this.nrParallelGatePipelines = nrParallelGatePipelines;
        this.postprocessing = new ArrayList<>();
        this.postprocessingMergedDocs = new ArrayList<>();
        this.doneHooks = new ArrayList<>();

        workflowConcerns.forEach(c -> addConcern(c));
    }


    private Stream<I> docSource;

    private Function<I, Optional<Document>> docConversion;

    private List<Function<Document, Document>> preprocessing;

    private SerialAnalyserController gatePipelineController;

    private int nrParallelGatePipelines;

    private List<Function<Document, Document>> postprocessing;

    private List<Function<Document, Document>> postprocessingMergedDocs;

    private List<Runnable> doneHooks;


    public void run(){
        final ActorSystem system = ActorSystem.create("Gate-Streams-Pipeline");
        final Materializer materializer = ActorMaterializer.create(system);

        Source<Document, NotUsed> workflow = buildWorkflow();

        final AtomicInteger docCnt = new AtomicInteger(0);
        final CompletionStage<Done> completion = workflow.runForeach(doc -> {

            int cnt = docCnt.addAndGet(1);

            if (cnt % 10 == 0) {
                double megaBytes = java.lang.Math.pow(1024, 2);
                Runtime r = Runtime.getRuntime();
                String usage = String.format("Memory usage: %dMB/%dMB", (int) ((r.maxMemory() - r.freeMemory())/megaBytes), (int) (r.maxMemory()/megaBytes));
                log.info(String.format("Processed %d Documents. %s", cnt, usage));
            }

            Factory.deleteResource(doc);
        }, materializer);


        log.info(String.format("Executing using %d pipeline runners", nrParallelGatePipelines));

        completion.exceptionally(e -> {
            log.error("Unhandled exception in stream", e);
            system.terminate();
            return Done.done();
        });

        completion.thenRun(() -> {
            log.info("Processed total" + docCnt.get() + " Documents");

            doneHooks.forEach(r -> {
                    r.run();
            });

            log.info("terminating");
            // TODO delete worker dirs!
            system.terminate();
        });
    }


    private Source<Document, NotUsed> buildWorkflow() {

        final Source<I, NotUsed> fileSource = Source.fromIterator(() -> docSource.iterator());

        Flow<I, Document, NotUsed> parallelGateProcessing = Flow.fromGraph(GraphDSL.create(builder -> {
            final UniformFanOutShape<I, I> balancer = builder.add(Balance.create(nrParallelGatePipelines));
            final UniformFanInShape<Document, Document> merge = builder.add(Merge.create(nrParallelGatePipelines));

            IntStream.range(0, nrParallelGatePipelines).forEach(i -> {
                try {
                    SerialAnalyserController localController = nrParallelGatePipelines > 1 ? (SerialAnalyserController) Factory.duplicate(gatePipelineController) : gatePipelineController;

                    Flow<I, Optional<Document>, NotUsed> preprocessDocsOpt =  Flow.fromFunction(input -> docConversion.apply(input));

                    Flow<I, Document, NotUsed> preprocessDocs = preprocessDocsOpt.
                            filter(od -> od.isPresent()).
                            map(od -> {
                                Document d = od.get();
                                d.getFeatures().put(WORKFLOW_INDEX, i);
                                return d;
                            });

                    for (Function<Document, Document> f : preprocessing) {
                        preprocessDocs = preprocessDocs.map(d -> f.apply(d));
                    }

                    // doing the heavy lifting
                    preprocessDocs = preprocessDocs.async().map(doc -> GateTools.processDoc(doc, localController)).async();

                    for (Function<Document, Document> f : postprocessing) {
                        preprocessDocs = preprocessDocs.map(d -> f.apply(d));
                    }

                    GraphDSL.Builder.ForwardOps processedDocs = builder.from(balancer.out(i)).via(builder.add(preprocessDocs.async()));

                    processedDocs.toInlet(merge.in(i));
                } catch (ResourceInstantiationException e) {
                    e.printStackTrace();
                } catch (GateException e) {
                    e.printStackTrace();
                } catch(RuntimeException e) {
                    e.printStackTrace();
                }
            });

            return FlowShape.of(balancer.in(), merge.out());

        }));

        Source<Document, NotUsed> processedDocs = fileSource.async().via(parallelGateProcessing).
                async().buffer(1000, OverflowStrategy.backpressure()); // don't overload postprocessing thread

        for (Function<Document, Document> f : postprocessingMergedDocs) {
            processedDocs = processedDocs.map(d -> f.apply(d));
        }

        return processedDocs;
    }

    public void addConcern(WorkflowConcern concern) {
        this.preprocessing.add(doc -> concern.preProcessDoc(doc));
        this.postprocessing.add(doc -> concern.postProcessDoc(doc));
        this.postprocessingMergedDocs.add(doc -> concern.postProcessMergedDoc(doc));
        this.doneHooks.add(() -> concern.doneHook());
    }

    public static final String WORKFLOW_INDEX = "WORKFLOW_INDEX";
}
