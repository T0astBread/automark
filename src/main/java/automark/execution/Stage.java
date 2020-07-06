package automark.execution;

import automark.config.*;
import automark.errors.*;
import automark.models.*;

import java.util.*;

public interface Stage {
    String getName();
    List<Submission> run(Config config, List<Submission> submissions) throws AutomarkException;
}
