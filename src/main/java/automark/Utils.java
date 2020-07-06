package automark;

import automark.errors.*;

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
}
