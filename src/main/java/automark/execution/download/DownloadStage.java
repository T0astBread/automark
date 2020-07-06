package automark.execution.download;

import automark.*;
import automark.config.*;
import automark.errors.*;
import automark.execution.*;
import automark.models.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class DownloadStage implements Stage {
    @Override
    public String getName() {
        return "DOWNLOAD";
    }

    @Override
    public List<Submission> run(Config config, List<Submission> submissions) throws AutomarkException {
        String assignmentID = config.get(ConfigConstants.ASSIGNMENT_ID);

        try (MoodleSession moodleSession = new MoodleSession(config.get(ConfigConstants.MOODLE_BASE_URL))) {

            File downloadsDir = new File(config.getWorkingDir(), getName().toLowerCase());
            if(downloadsDir.exists()) {
                try {
                    Utils.deleteFolder(downloadsDir.toPath());
                } catch (IOException e) {
                    throw new AutomarkException("Failed to clear leftover bits from previous run", e);
                }
            }
            downloadsDir.mkdirs();

            moodleSession.login(
                    config.get(ConfigConstants.MOODLE_USERNAME),
                    config.get(ConfigConstants.MOODLE_PASSWORD));

            submissions = moodleSession.listSubmissions(assignmentID);
            String[] teachers = config.getList(ConfigConstants.MOODLE_TEACHERS);
            submissions = filterTeachers(submissions, teachers);

            for (Submission submission : submissions) {
                if(submission.getFileURL() == null) {
                    submission.addProblem(Problem.createNotSubmitted(getName()));
                    submission.setDisqualified(true);
                } else {
                    try {
                        File submissionFile = moodleSession.downloadSubmission(submission, downloadsDir, true);
                        System.out.println("Downloaded submission file " + submissionFile.getAbsolutePath());
                    } catch (Exception e) {
                        submission.addProblem(Problem.createException(getName(), e));
                        submission.setDisqualified(true);
                    }
                }
            }
        } catch (MoodleException | IOException e) {
            throw new AutomarkException("Failed to download Moodle submissions", e);
        }

        return submissions;
    }

    private List<Submission> filterTeachers(List<Submission> submissions, String[] teachers) {
        List<String> teachers_ = List.of(teachers);
        return submissions.stream()
                .filter(submission -> !teachers_.contains(submission.getStudentEmail()))
                .collect(Collectors.toList());
    }
}
