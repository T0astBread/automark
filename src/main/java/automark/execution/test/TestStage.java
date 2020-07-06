package automark.execution.test;

import automark.*;
import automark.config.*;
import automark.errors.*;
import automark.execution.*;
import automark.execution.compile.*;
import automark.models.*;
import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.*;
import org.junit.platform.engine.support.descriptor.*;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class TestStage implements Stage {
    @Override
    public String getName() {
        return "TEST";
    }

    @Override
    public List<Submission> run(Config config, List<Submission> submissions) throws AutomarkException {
        File compileDir = new File(config.getWorkingDir(), CompileStage.NAME.toLowerCase());
        if (!(compileDir.exists() && compileDir.isDirectory()))
            throw new AutomarkException("Folder from compile stage doesn't exist - maybe try re-running the compile stage? (i.e. rollback and re-run)");

        String[] testFiles = config.getList(ConfigConstants.TEST_FILES);
        int threadCount = 0;

        for (Submission submission : submissions) {
            if(submission.isDisqualified())
                continue;

            File submissionDir = new File(compileDir, submission.getSlug());
            if(!(submissionDir.exists() && submissionDir.isDirectory())) {
                submission.addProblem(Problem.createException(getName(), new AutomarkException("Submission folder not found")));
            }


            // Load classes
            URLClassLoader classLoader;
            try {
                classLoader = new URLClassLoader(new URL[]{ submissionDir.toURI().toURL() });
            } catch (Throwable e) {
                submission.addProblem(Problem.createException(getName(), new AutomarkException("Error while running tests", e)));
                submission.setDisqualified(true);
                continue;
            }
            for (String testFileName : testFiles) {
                String testClassName = testFileName.substring(0, testFileName.length() - ".java".length());

                Class testClass = null;
                String testClassCanonical = Utils.getWantedPackageForSubmission(submission) + "." + testClassName;
                try {
                    testClass = classLoader.loadClass(testClassCanonical);
                    UI.get().println("Loaded class: " + testClass.getCanonicalName());
                } catch (Throwable e) {
                    submission.addProblem(Problem.createTestSuiteFail(getName(), testClassName));
                    continue;
                }


                Class finalTestClass = testClass;
                int threadNum = threadCount++;
                UI.get().println("Thread " + threadNum + " is starting");
                Thread testThread = new Thread(() -> {
                    UI.get().println("Testing submission " + submission.getSlug());
                    runTests(finalTestClass, submission);
                    UI.get().println("Thread " + threadNum + " is done");
                });
                testThread.setPriority(Thread.MIN_PRIORITY);
                testThread.start();
                try {
                    testThread.join(5000);
                } catch (InterruptedException e) {
                    UI.get().println("Main thread interrupted");
                }
                if(testThread.isAlive()) {
                    testThread.stop();
                    UI.get().println("Warning: Forcefully killed test thread for " + submission.getSlug());
                    submission.addProblem(Problem.createTestSuiteFail(getName(), testClassName));
                }
            }
        }

        return submissions;
    }

    private void runTests(Class testClass, Submission submission) {
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        LauncherDiscoveryRequest launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(testClass))
                .build();
        Launcher launcher = LauncherFactory.create();
//        TestPlan testPlan = launcher.discover(launcherDiscoveryRequest);
        launcher.registerTestExecutionListeners(listener);
//        PrintStream sout = System.out;
//        System.setOut(new PrintStream(new OutputStream() {
//            public void write(int b) {
//                //DO NOTHING
//            }
//        }));
        launcher.execute(launcherDiscoveryRequest);
//        System.setOut(sout);

        TestExecutionSummary summary = listener.getSummary();
        for (TestExecutionSummary.Failure failure : summary.getFailures()) {
//            failure.getException().printStackTrace();
            Optional<TestSource> _testSource = failure.getTestIdentifier().getSource();
            if(_testSource.isPresent()) {
                TestSource testSource = _testSource.get();
                if(testSource instanceof MethodSource) {
                    MethodSource methodSource = (MethodSource) testSource;
                    Problem problem = Problem.createTestFail(getName(), testClass.getSimpleName(), methodSource.getMethodName());
                    System.out.println("FAIL: " + problem.summary);
                    submission.addProblem(problem);
                }
            }
        }
        System.out.print("Tests (run/failed/skipped): ");
        System.out.println(summary.getTestsSucceededCount() + "/" + summary.getTestsFailedCount() + "/" + summary.getTestsSkippedCount());
    }
}
