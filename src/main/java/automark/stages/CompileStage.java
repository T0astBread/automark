package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class CompileStage {

    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        File prepareCompileDir = new File(Metadata.getDataDir(workingDir), "prepare_compile");
        File compileDir = Metadata.mkStageDir(Stage.COMPILE, workingDir);

        try {
            FileIO.cpDir(prepareCompileDir.toPath(), compileDir.toPath());
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to copy prepared submissions to compilation working dir", e);
        }

        List<File> testFiles = SubmissionUtils.getTestFiles(workingDir, config);
        Set<String> testFileNames = testFiles.stream()
                .map(File::getName)
                .filter(fileName -> fileName.endsWith(".java"))
                .collect(Collectors.toSet());

        for (Submission submission : submissions) {
            if (submission.isDisqualified())
                continue;

            File submissionInCompile = new File(compileDir, submission.getSlug());

            List<File> sourcesWithTests;
            try {
                sourcesWithTests = Files.walk(submissionInCompile.toPath())
                        .map(Path::toFile)
                        .filter(file -> file.isFile() && file.getName().endsWith(".java"))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                submission.addProblem(Problem.createException(Stage.COMPILE,
                        new UnexpectedException("Failed to search for sources in compile dir for " + submission.getSlug(), e)));
                submission.setDisqualified(true);
                continue;
            }

            List<File> sourcesOnly = sourcesWithTests.stream()
                    .filter(file -> !testFileNames.contains(file.getName()))
                    .collect(Collectors.toList());
            try {

                List<Diagnostic<? extends JavaFileObject>> diagnostics = compile(sourcesOnly);
                if (diagnostics != null) {
                    submission.addProblem(createCompilationErrorProblem(diagnostics));
                    submission.setDisqualified(true);
                    continue;
                }

                List<Diagnostic<? extends JavaFileObject>> diagnosticsWithTests = compile(sourcesWithTests);
                if (diagnosticsWithTests != null) {
                    System.out.println();
                    System.out.println("Compilation error(s) in submission " + submission.getSlug() + " when compiled with test suites:");
                    System.out.println();
                    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticsWithTests) {
                        String fileName = diagnostic.getSource().toUri().toString();
                        String errorMessage = diagnostic.getMessage(null) + ", line (" + diagnostic.getLineNumber() + ")";
                        System.out.println(fileName);
                        System.out.println(errorMessage);
                    }
                }
            } catch (IOException e) {
                submission.addProblem(Problem.createException(Stage.COMPILE, e));
                submission.setDisqualified(true);
            }
        }

        return submissions;
    }

    private static List<Diagnostic<? extends JavaFileObject>> compile(List<File> sourceFiles) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();

        boolean success;
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnosticCollector, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnit =
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles);
            JavaCompiler.CompilationTask compilationTask = compiler.getTask(
                    null,
                    fileManager, diagnosticCollector,
                    null, null,
                    compilationUnit);
            success = compilationTask.call();
        }

        if (!success) {
            return diagnosticCollector.getDiagnostics();
        }

        return null;
    }

    private static Problem createCompilationErrorProblem(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        String summary = diagnostics.stream()
                .filter(diagnostic -> diagnostic.getKind() != Diagnostic.Kind.NOTE)
                .map(diagnostic -> diagnostic.getMessage(null)
                        + " in " + new File(diagnostic.getSource().getName()).getName()
                        + " at " + diagnostic.getLineNumber()
                        + ":" + diagnostic.getColumnNumber())
                .collect(Collectors.joining("\n"));
        return new Problem(Stage.COMPILE, Problem.Type.COMPILATION_ERROR, summary);
    }
}
