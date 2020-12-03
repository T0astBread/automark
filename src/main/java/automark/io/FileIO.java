package automark.io;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class FileIO {

    public static void rm(File file) throws IOException {
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            rmDir(file.toPath());
        }
    }

    public static void rmDir(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public static void cpDir(Path source, Path destination) throws IOException {
        List<File> filesInSource = Files.walk(source)
                .map(Path::toFile)
                .filter(file -> file.exists() && file.isFile())
                .collect(Collectors.toList());
        for (File file : filesInSource) {
            Path relPath = source.relativize(file.toPath());
            Path newFilePath = destination.resolve(relPath);
            newFilePath.toFile().getParentFile().mkdirs();
            Files.copy(file.toPath(), newFilePath);
        }
    }

    public static void pipe(InputStream in, OutputStream out) throws IOException {
        int n;
        byte[] buffer = new byte[16384];
        while ((n = in.read(buffer)) > -1) {
            out.write(buffer, 0, n);
        }
    }

    /**
     * @return The platform's file separator but escaped for use in regex replacements
     */
    public static String getEscapedFileSeperator() {
        final boolean isWindows = System.getProperty("os.name").contains("Windows");
        return isWindows ? "\\\\" : File.separator;
    }
}
