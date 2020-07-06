package automark;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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

    public static void deleteFolder(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
