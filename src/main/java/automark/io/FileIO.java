package automark.io;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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

    public static void pipe(InputStream in, OutputStream out) throws IOException {
        int n;
        byte[] buffer = new byte[16384];
        while ((n = in.read(buffer)) > -1) {
            out.write(buffer, 0, n);
        }
    }
}
