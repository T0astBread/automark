package automark.stages;

import automark.*;
import automark.io.*;
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

public class TestStage {
    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        File compileDir = new File(Metadata.getDataDir(workingDir), "compile");
        List<File> testFiles = Metadata.getTestFiles(workingDir);


        int threadCount = 0;

        for (Submission submission : submissions) {
            if(submission.isDisqualified())
                continue;

            File submissionDir = new File(compileDir, submission.getSlug());

            // Load classes
            URLClassLoader classLoader;
            try {
                classLoader = new URLClassLoader(new URL[]{ submissionDir.toURI().toURL() });
            } catch (Throwable e) {
                submission.addProblem(Problem.createException(Stage.TEST, new UnexpectedException("Error while running tests", e)));
                submission.setDisqualified(true);
                continue;
            }
            for (File testFile : testFiles) {
                String testFileName = testFile.getName();
                String testClassName = testFileName.substring(0, testFileName.length() - ".java".length());

                Class testClass = null;
                String testClassCanonical = Metadata.getPackageNameForSubmission(submission) + "." + testClassName;
                try {
                    testClass = classLoader.loadClass(testClassCanonical);
                    System.out.println("Loaded class: " + testClass.getCanonicalName());
                } catch (Throwable e) {
                    submission.addProblem(createTestSuiteFailProblem(testClassName));
                    continue;
                }


                Class finalTestClass = testClass;
                int threadNum = threadCount++;
                System.out.println("Thread " + threadNum + " is starting");
                Thread testThread = new Thread(() -> {
                    System.out.println("Testing submission " + submission.getSlug());
                    runTests(finalTestClass, submission);
                    System.out.println("Thread " + threadNum + " is done");
                });
                testThread.setPriority(Thread.MIN_PRIORITY);
                testThread.start();
                try {
                    testThread.join(5000);
                } catch (InterruptedException e) {
                    System.out.println("Main thread interrupted");
                }
                if(testThread.isAlive()) {
                    testThread.stop();
                    System.out.println("Warning: Forcefully killed test thread for " + submission.getSlug());
                    submission.addProblem(createTestSuiteFailProblem(testClassName));
                }
            }
        }

        return submissions;
    }

    private static void runTests(Class testClass, Submission submission) {
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
                    Problem problem = createTestFailProblem(testClass.getSimpleName(), methodSource.getMethodName());
                    System.out.println("FAIL: " + problem.summary);
                    submission.addProblem(problem);
                }
            }
        }
        System.out.print("Tests (run/failed/skipped): ");
        System.out.println(summary.getTestsSucceededCount() + "/" + summary.getTestsFailedCount() + "/" + summary.getTestsSkippedCount());
    }
    
    private static Problem createTestSuiteFailProblem(String testSuiteName) {
        return new Problem(Stage.TEST, Problem.Type.TEST_SUITE_FAILURE, testSuiteName);
    }

    private static Problem createTestFailProblem(String testSuiteName, String testName) {
        return new Problem(Stage.TEST, Problem.Type.TEST_FAILURE, testSuiteName + "::" + testName);
    }
}
