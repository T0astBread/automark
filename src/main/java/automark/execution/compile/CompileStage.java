package automark.execution.compile;

import automark.*;
import automark.config.*;
import automark.errors.*;
import automark.execution.*;
import automark.execution.extract.*;
import automark.models.*;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CompileStage implements Stage {
    @Override
    public String getName() {
        return "COMPILE";
    }

    @Override
    public List<Submission> run(Config config, List<Submission> submissions) throws AutomarkException {
        // Set up folders
        File compileDir = Utils.cleanAndMakeStageDir(new File(config.getWorkingDir(), getName().toLowerCase()));
        File extractDir = new File(config.getWorkingDir(), ExtractStage.NAME.toLowerCase());
        if (!(extractDir.exists() && extractDir.isDirectory()))
            throw new AutomarkException("Folder from extract stage doesn't exist - maybe try re-running the " + ExtractStage.NAME + " stage? (i.e. rollback and re-run)");

        try {
            Utils.copyFolder(extractDir.toPath(), compileDir.toPath());
        } catch (IOException e) {
            throw new AutomarkException("Failed to copy sources for compilation", e);
        }


        // Get config properties
        String[] sourceFiles = config.getList(ConfigConstants.SOURCE_FILES);
        config.trySaveBack(ConfigConstants.SOURCE_FILES, String.join(" ", sourceFiles));

        String[] testFiles = config.getList(ConfigConstants.TEST_FILES);
        config.trySaveBack(ConfigConstants.TEST_FILES, String.join(" ", testFiles));

        List<String> allSourceFiles = new ArrayList<>();
        allSourceFiles.addAll(Arrays.asList(sourceFiles));
        allSourceFiles.addAll(Arrays.asList(testFiles));


        // Patch packages & compile
        for (Submission submission : submissions) {
            if(submission.isDisqualified())
                continue;

            File submissionFolder = new File(compileDir, submission.getSlug());
            if(!submissionFolder.exists()) {
                submission.addProblem(Problem.createException(getName(), new AutomarkException("Submission folder not found in " + getName() + " stage")));
                submission.setDisqualified(true);
                continue;
            }

            // Copy test files
            File testFilesDir = new File(config.getWorkingDir(), "testFiles");
            for (String testFileName : testFiles) {
                File testFile = new File(testFilesDir, testFileName);
                if(!testFile.exists())
                    throw new AutomarkException("Test file " + testFileName + " doesn't exist (" + testFile.getAbsolutePath() + ")");

                File destFile = new File(submissionFolder, testFileName);
                try {
                    Files.copy(testFile.toPath(), destFile.toPath());
                } catch (IOException e) {
                    submission.addProblem(Problem.createException(getName(),
                            new AutomarkException("Unable to copy test file " + testFileName, e)));
                }
            }

            try {
                String wantedPackage = "automark.testbed." + submission.getSlug();
                List<File> sourcesForCompilation = patchPackage(submissionFolder, allSourceFiles, wantedPackage);
                UI.get().println("Patched package header of " + submission.getSlug());

                List<Diagnostic<? extends JavaFileObject>> diagnostics = compile(sourcesForCompilation);
                if(diagnostics != null) {
                    Problem compileProblem = Problem.createCompilationError(getName(), diagnostics);
                    submission.addProblem(compileProblem);
                }
            } catch (IOException e) {
                submission.addProblem(Problem.createException(getName(), e));
                submission.setDisqualified(true);
            }
        }

        return submissions;
    }

    private List<File> patchPackage(File submissionFolder, List<String> sourceFiles, String wantedPackage) throws IOException {
        String packagePath = wantedPackage.replaceAll("\\.", File.separator) + File.separator;
        List<File> sourceFilesForCompilation = new ArrayList<>();

        for (String sourceFileName : sourceFiles) {
            File sourceFile = new File(submissionFolder, sourceFileName);
            File destFile = new File(submissionFolder, packagePath + sourceFile.getName());

            if(sourceFile.exists()) {
                destFile.getParentFile().mkdirs();

                try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
                     FileWriter writer = new FileWriter(destFile)) {
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) break;

                        line = line.replaceFirst("^package [^;]+;", "package " + wantedPackage + ";");
                        writer.write(line);
                        writer.write("\n");
                    }
                }
                sourceFile.delete();
                sourceFilesForCompilation.add(destFile);
            }
        }
        return sourceFilesForCompilation;
    }

    public static List<Diagnostic<? extends JavaFileObject>> compile(List<File> sourceFiles) throws IOException {
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
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
                if (diagnostic.getSource() != null) {
                    String fileName = diagnostic.getSource().toUri().toString();
                    String errorMessage = diagnostic.getMessage(null) + ", line (" + diagnostic.getLineNumber() + ")";
                    System.err.println(fileName);
                    System.err.println(errorMessage);
                }
            }
            return diagnosticCollector.getDiagnostics();
        }

        return null;
    }
}
