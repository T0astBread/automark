package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;
import automark.stages.test.*;
import org.junit.platform.engine.*;
import org.junit.platform.engine.discovery.*;
import org.junit.platform.engine.support.descriptor.*;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.*;

public class TestStage {

    private static final String METHOD_SIGNATURE_LINE_REGEX = "^\\s*(?:(?:public|protected|private)\\s+)?void\\s+(\\w+)\\([\\w\\s,]*\\).*$";

    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        // Read test suite data
        File compileDir = new File(Metadata.getDataDir(workingDir), "compile");
        List<File> testFiles = SubmissionUtils.getTestFiles(workingDir, config);
        List<File> testSuiteFiles = testFiles.stream()
                .filter(testFile -> testFile.getName().endsWith(".java"))
                .collect(Collectors.toList());
        List<TestSuite> testSuites = readTestSuites(testSuiteFiles);

        // Print overview of parsed tests
        System.out.println();
        System.out.println("Recognized tests:");
        for (TestSuite suite : testSuites) {
            System.out.println(suite.getName());
            for (TestMethod method : suite.getMethods()) {
                Summaries.printIndentation(System.out, 1);
                System.out.println(method.getMethodName());

                if (method.getDescription() != null) {
                    Summaries.printIndentation(System.out, 2);
                    System.out.println(method.getDescription());
                }
            }
        }
        System.out.println();


        int threadCount = 0;

        // Iterate over submissions
        for (Submission submission : submissions) {
            if (submission.isDisqualified())
                continue;

            File submissionDir = new File(compileDir, submission.getSlug());

            // Create a new ClassLoader
            URLClassLoader classLoader;
            try {
                classLoader = new URLClassLoader(new URL[]{submissionDir.toURI().toURL()});
            } catch (Throwable e) {
                submission.addProblem(Problem.createException(Stage.TEST, new UnexpectedException("Error while running tests", e)));
                submission.setDisqualified(true);
                continue;
            }

            // Go over test suites
            for (TestSuite testSuite : testSuites) {
                // Load test suite class
                Class testClass = null;
                String testClassCanonical = SubmissionUtils.getPackageNameForSubmission(submission) + "." + testSuite.getName();
                try {
                    testClass = classLoader.loadClass(testClassCanonical);
                    System.out.println("Loaded class: " + testClass.getCanonicalName());
                } catch (Throwable e) {
                    e.printStackTrace(System.out);
                    boolean failedToCompile = submission
                            .getProblems()
                            .stream()
                            .anyMatch(problem -> problem.type == Problem.Type.TEST_SUITE_FAILURE
                                    && problem.stage == Stage.COMPILE
                                    && problem.summary.startsWith(testSuite.getName()));
                    if (!failedToCompile) {
                        submission.addProblem(Problem.createTestSuiteFailure(
                                Stage.TEST,
                                testSuite.getName(),
                                "Class not loaded: " + e.getClass().getSimpleName()
                        ));
                    }
                    continue;
                }


                // Run & await tests in a new thread
                Class finalTestClass = testClass;
                int threadNum = threadCount++;
                System.out.println("Thread " + threadNum + " is starting");
                final PrintStream sout = System.out;
                Thread testThread = new Thread(() -> {
                    System.out.println("Testing submission " + submission.getSlug());
                    runTests(testSuite, finalTestClass, submission);
                    System.out.println("Thread " + threadNum + " is done");
                });
                testThread.setPriority(Thread.MIN_PRIORITY);
                testThread.start();
                try {
                    testThread.join(5000);
                } catch (InterruptedException e) {
                    System.out.println("Main thread interrupted");
                }
                System.setOut(sout);
                if (testThread.isAlive()) {
                    testThread.stop();
                    System.out.println("Warning: Forcefully killed test thread for " + submission.getSlug());
                    submission.addProblem(Problem.createTestSuiteFailure(
                            Stage.TEST,
                            testSuite.getName(),
                            "Timeout (some tests might not have been run)"
                    ));
                }
            }
        }

        return submissions;
    }

    private static List<TestSuite> readTestSuites(List<File> testFiles) {
        List<TestSuite> testSuites = new ArrayList<>(testFiles.size());

        for (File testFile : testFiles) {
            List<TestMethod> methods = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(testFile))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    if (line.trim().equals("@Test")) {
                        line = reader.readLine();
                        String description = null;
                        if (!line.matches(METHOD_SIGNATURE_LINE_REGEX)) {
                            description = line
                                    .trim()
                                    .substring(2)
                                    .trim();
                            line = reader.readLine();
                        }
                        String methodName = line
                                .replaceAll(METHOD_SIGNATURE_LINE_REGEX, "$1");
                        methods.add(new TestMethod(methodName, description));
                    }
                }
                testSuites.add(new TestSuite(methods, testFile));
            } catch (IOException e) {
                e.printStackTrace(System.out);
                System.out.println("Unable to parse test file: " + testFile.getAbsolutePath());
            }
        }

        return testSuites;
    }

    private static void runTests(TestSuite testSuiteData, Class testClass, Submission submission) {
        // Set up JUnit launcher
        LauncherDiscoveryRequest launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(testClass))
                .build();
        Launcher launcher = LauncherFactory.create();

        // Set up execution listener that captures successful tests
        Set<String> succeededTests = new HashSet<>();
        launcher.registerTestExecutionListeners(new TestExecutionListener() {
            @Override
            public void executionSkipped(TestIdentifier testIdentifier, String reason) {
                System.out.println("Skipped: " + testIdentifier.getDisplayName());
            }

            @Override
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                System.out.println("Finished: " + testIdentifier.getDisplayName() + ", " + testExecutionResult.getStatus());

                if (testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                    TestSource source = testIdentifier.getSource().orElse(null);
                    if (source instanceof MethodSource) {
                        succeededTests.add(((MethodSource) source).getMethodName());
                    }
                }
            }
        });

        // Actually execute tests
        launcher.execute(launcherDiscoveryRequest);

        // Generate problems in submission for failed tests
        testSuiteData.getMethods().stream()
                .filter(testMethod -> !succeededTests.contains(testMethod.getMethodName()))
                .forEach(failedTestMethod -> submission.addProblem(createTestFailProblem(testSuiteData.getName(), failedTestMethod)));
    }

    private static Problem createTestFailProblem(String testSuiteName, TestMethod method) {
        StringBuilder summary = new StringBuilder()
                .append(testSuiteName)
                .append("::")
                .append(method.getMethodName());

        if (method.getDescription() != null) {
            summary.append("\n  ")
                    .append(method.getDescription());
        }

        return new Problem(Stage.TEST, Problem.Type.TEST_FAILURE, summary.toString());
    }
}
