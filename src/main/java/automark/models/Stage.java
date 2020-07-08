package automark.models;

import java.io.*;
import java.util.*;
import java.util.function.*;

public enum Stage {
    DOWNLOAD, UNZIP, EXTRACT, JPLAG, PREPARE_COMPILE, COMPILE, TEST, SUMMARY;
}
