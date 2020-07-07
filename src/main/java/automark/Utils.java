package automark;

import automark.errors.*;
import automark.execution.*;
import automark.models.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class Utils {

    public static boolean isSet(int bitField, int bitMask) {
        return (bitField & bitMask) != 0;
    }

    public static void pipe(InputStream in, OutputStream out) throws IOException {
        int n;
        byte[] buffer = new byte[16384];
        while ((n = in.read(buffer)) > -1) {
            out.write(buffer, 0, n);
        }
    }

    public static boolean isEmpty(File folder) {
        String[] contents = folder.list();
        return contents != null && contents.length == 0;
    }

    public static void deleteFolder(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public static void copyFolder(Path source, Path destination) throws IOException {
        copyFolder(source, destination, file -> true);
    }

    public static void copyFolder(Path source, Path destination, Predicate<File> filter) throws IOException {
        List<File> filesInSource = Files.walk(source)
                .map(Path::toFile)
                .filter(file -> file.exists() && file.isFile() && filter.test(file))
                .collect(Collectors.toList());
        for (File file : filesInSource) {
            Path relPath = source.relativize(file.toPath());
            Path newFilePath = destination.resolve(relPath);
            newFilePath.toFile().getParentFile().mkdirs();
            Files.copy(file.toPath(), newFilePath);
        }
    }

    public static File cleanAndMakeStageDir(File dir) throws AutomarkException {
        if(dir.exists()) {
            try {
                Utils.deleteFolder(dir.toPath());
            } catch (IOException e) {
                throw new AutomarkException("Failed to clean up previous run", e);
            }
        }
        dir.mkdirs();
        return dir;
    }

    public static String getWantedPackageForSubmission(Submission submission) {
        return "automark.testbed." + submission.getSlug();
    }
}
