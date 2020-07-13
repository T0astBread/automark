package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;
import automark.stages.download.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class DownloadStage {
    public static List<Submission> run(File workingDir, Properties config) throws UserFriendlyException {
        List<Submission> submissions;
        String assignmentID = config.getProperty(Config.ASSIGNMENT_ID);

        try (MoodleSession moodleSession = new MoodleSession(config.getProperty(Config.MOODLE_BASE_URL))) {
            File downloadsDir = Metadata.mkStageDir(Stage.DOWNLOAD, workingDir);

            String username = config.getProperty(Config.MOODLE_USERNAME);
            if(username == null) username = UI.prompt("Moodle username: ", false);
            String password = config.getProperty(Config.MOODLE_PASSWORD);
            if(password == null) password = UI.prompt("Moodle password: ", true);
            moodleSession.login(username, password);

            submissions = moodleSession.listSubmissions(assignmentID);

            List<String> teachers = Config.asList(config.getProperty(Config.MOODLE_TEACHERS));
            submissions = filterTeachers(submissions, teachers);

            for (Submission submission : submissions) {
                if (submission.getFileURL() == null) {
                    submission.addProblem(Problem.createNotSubmittedProblem());
                    submission.setDisqualified(true);
                } else {
                    try {
                        File submissionFile = moodleSession.downloadSubmission(submission, downloadsDir, true);
                        System.out.println("Downloaded submission file " + submissionFile.getAbsolutePath());
                    } catch (Exception e) {
                        submission.addProblem(Problem.createException(Stage.DOWNLOAD, e));
                        submission.setDisqualified(true);
                    }
                }
            }
        } catch (MoodleException | IOException e) {
            throw new UserFriendlyException("Failed to download Moodle submissions", e);
        }

        return submissions;
    }

    private static List<Submission> filterTeachers(List<Submission> submissions, List<String> teachers) {
        return submissions.stream()
                .filter(submission -> !teachers.contains(submission.getStudentEmail()))
                .collect(Collectors.toList());
    }
}
