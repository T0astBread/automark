package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;
import net.lingala.zip4j.*;
import net.lingala.zip4j.exception.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class BypassDownloadStage {
    public static List<Submission> run(File workingDir, Properties config) throws UserFriendlyException {
        String zipFileName = findSubmissionZipName(workingDir);
        File zipFile = new File(workingDir, zipFileName);

        File unzipDir1 = new File(workingDir, "unzip1");
        if (unzipDir1.exists()) {
            try {
                FileIO.rm(unzipDir1);
            } catch (IOException e) {
                throw new UserFriendlyException("Failed to clear intermediate dir " + unzipDir1.getPath(), e);
            }
        }
        unzipDir1.mkdirs();

        ZipFile zip = new ZipFile(zipFile);
        try {
            zip.extractAll(unzipDir1.getAbsolutePath());
        } catch (ZipException e) {
            throw new UserFriendlyException("Failed to extract submission ZIP", e);
        }

        Map<String, String> emails;
        try {
            emails = readEmailsCSV(workingDir);
        } catch (IOException e) {
            throw new UserFriendlyException("Attempted but failed to read emails.csv", e);
        }

        List<Submission> submissions = new ArrayList<>();
        File downloadDir = new File(Metadata.getDataDir(workingDir), "download");
        if (downloadDir.exists()) {
            try {
                FileIO.rm(downloadDir);
            } catch (IOException e) {
                throw new UserFriendlyException("Failed to clear download dir", e);
            }
        }
        downloadDir.mkdirs();

        String[] submissionDirs = unzipDir1.list();
        if (submissionDirs == null)
            throw new UserFriendlyException("Failed to list extracted submissions");
        for (String submissionDirName : submissionDirs) {
            String[] parts = submissionDirName.split("_");

            String name = parts[0];
            String slug = SubmissionUtils.getSlugFromName(name);
            String email = emails.get(name);

            Submission submission = new Submission(slug, name, email, null);
            submissions.add(submission);

            File submissionDir = new File(unzipDir1, submissionDirName);
            File[] submissionsDirContents = submissionDir.listFiles();
            if (submissionsDirContents == null)
                throw new UserFriendlyException("Failed to list files in submission dir " + submissionDir.getPath());
            File submissionZipInUnzip1 = null;
            for (File submissionFile : submissionsDirContents) {
                if (submissionFile.getName().endsWith(".zip")) {
                    submissionZipInUnzip1 = submissionFile;
                    break;
                }
            }

            if (submissionZipInUnzip1 == null) {
                submission.addProblem(Problem.createInvalidSubmissionFile(Stage.DOWNLOAD, "Unable to find .zip file"));
                submission.setDisqualified(true);
            } else {
                File submissionZipInDownload = new File(downloadDir, slug + ".zip");
                try {
                    Files.move(submissionZipInUnzip1.toPath(), submissionZipInDownload.toPath());
                } catch (IOException e) {
                    throw new UserFriendlyException("Failed to move " + submissionZipInUnzip1.getPath() + " to " + submissionZipInDownload, e);
                }
            }
        }

        try {
            FileIO.rmDir(unzipDir1.toPath());
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to remove unzip1 dir", e);
        }

        Set<String> submitted = submissions.stream()
                .map(Submission::getStudentEmail)
                .collect(Collectors.toSet());

        int submissionIdx = submissions.size();
        List<Map.Entry<String, String>> nonSubmittedEntries = emails
                .entrySet()
                .stream()
                .filter(entry -> !submitted.contains(entry.getValue()))
                .collect(Collectors.toList());
        for (Map.Entry<String, String> nonSubmittedEntry : nonSubmittedEntries) {
            String name = nonSubmittedEntry.getKey();
            String email = nonSubmittedEntry.getValue();
            Submission submission = new Submission(
                    name.toLowerCase().replaceAll("\\s", "_") + "_" + submissionIdx++,
                    name,
                    email,
                    null
            );
            submission.addProblem(Problem.createNotSubmittedProblem());
            submission.setDisqualified(true);
            submissions.add(submission);
        }

        return submissions;
    }

    private static String findSubmissionZipName(File workingDir) throws UserFriendlyException {
        String[] wdContents = workingDir.list();
        if (wdContents == null)
            throw new UserFriendlyException("Unable to find submission ZIP in workingDir");

        for (String fileName : wdContents) {
            if (fileName.endsWith(".zip"))
                return fileName;
        }

        throw new UserFriendlyException("Unable to find submission ZIP in workingDir");
    }

    private static Map<String, String> readEmailsCSV(File workingDir) throws IOException {
        HashMap<String, String> emailsMap = new HashMap<>();

        File emailsCSV = new File(workingDir, "emails.csv");
        if (!emailsCSV.exists())
            return emailsMap;

        try (BufferedReader reader = new BufferedReader(new FileReader(emailsCSV))) {
            for (int lineNum = 1; ; lineNum++) {
                String line = reader.readLine();
                if (line == null) return emailsMap;

                String[] fields = line.split(";");
                if (fields.length < 2) {
                    System.out.println("Invalid line in emails.csv: " + lineNum + ": Not in format name;email");
                    continue;
                }
                emailsMap.put(fields[0], fields[1]);
            }
        }
    }
}
