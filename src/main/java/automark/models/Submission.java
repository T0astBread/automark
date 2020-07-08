package automark.models;

import java.util.*;

public class Submission {
    private final String slug;
    private final String studentName;
    private final String studentEmail;
    private final String fileURL;
    private final List<Problem> problems;
    private boolean isDisqualified = false;

    public Submission(String slug, String studentName, String studentEmail, String fileURL) {
        this.slug = slug;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.fileURL = fileURL;
        this.problems = new ArrayList<>();
    }

    public String getSlug() {
        return slug;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getFileURL() {
        return fileURL;
    }

    public List<Problem> getProblems() {
        return problems;
    }

    public void addProblem(Problem problem) {
        this.problems.add(problem);
    }

    public boolean isDisqualified() {
        return isDisqualified;
    }

    public void setDisqualified(boolean disqualified) {
        isDisqualified = disqualified;
    }
}
