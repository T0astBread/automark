package automark.gui;

import automark.*;
import automark.io.*;
import automark.models.*;
import automark.subcommands.*;
import com.google.gson.*;
import spark.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.*;

public class GUI {
    public static void start(CommandLineArgs commandLineArgs) {
        final Gson GSON = new Gson();
        final String MIME_JSON = "application/json";

        AtomicReference<File> workingDir = new AtomicReference<>(commandLineArgs.workingDir);

        final String initiationSecret = generateSecret();
        if(commandLineArgs.enableInsecureDebugMechanisms)
            System.out.println(initiationSecret);
        AtomicBoolean hasBeenAuthenticated = new AtomicBoolean(false);
        final String permanentSecret = generateSecret();

        Spark.initExceptionHandler((e) -> {
            JOptionPane.showMessageDialog(null, e.getMessage(), e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        });

        Spark.staticFiles.location("/automark/gui");

        RunWebSocketHandler webSocketHandler = new RunWebSocketHandler(workingDir.get(), permanentSecret, commandLineArgs);
        Spark.webSocket("/", webSocketHandler);

        if (!commandLineArgs.enableInsecureDebugMechanisms) {
            // Doesn't apply to static files
            Spark.before(((request, response) -> {
                if(!"/auth".equals(request.pathInfo())) {
                    String authCookie = request.cookie("auth");
                    if (!hasBeenAuthenticated.get() || !permanentSecret.equals(authCookie)) {
                        Spark.halt(401, "Not allowed");
                    }
                }
            }));
        }

        Spark.get("/auth", ((request, response) -> {
            String providedSecret = request.queryParams("secret");
            if(hasBeenAuthenticated.get() || !initiationSecret.equals(providedSecret)) {
                response.status(401);
                return "Not allowed";
            }
            hasBeenAuthenticated.set(true);
            response.cookie("auth", permanentSecret);
            response.redirect("/");
            return "Redirected to /";
        }));


        Spark.get("/latest-metadata", (request, response) -> {
            response.type(MIME_JSON);

            Metadata.MetadataLoadingResult result = Metadata.loadLatestMetadata(workingDir.get());
            return result;
        }, GSON::toJson);


        Spark.get("/submissions", (request, response) -> {
            response.type(MIME_JSON);

            String stageName = request.queryParams("stageName");
            Stage stage;
            try {
                stage = Stage.valueOf(stageName);
            } catch (IllegalArgumentException e) {
                response.status(400);
                return "Unknown stage";
            }

            List<Submission> submissions = Metadata.loadSubmissions(Metadata.getMetadataFile(workingDir.get(), stage));
            return submissions;
        }, GSON::toJson);


        Spark.get("/data", (request, response) -> {
            response.type(MIME_JSON);

            Map<String, List<Submission>> submissions = new HashMap<>();
            for (Stage stage : Stage.values()) {
                File submissionFile = Metadata.getMetadataFile(workingDir.get(), stage);
                if (submissionFile.exists()) {
                    submissions.put(stage.name(), Metadata.loadSubmissions(submissionFile));
                } else {
                    break;
                }
            }
            return submissions;

//            List<Submission> submissions = Metadata.loadSubmissions(Metadata.getMetadataFile(workingDir, stage));
//            return submissions;
        }, GSON::toJson);


        Spark.post("/rollback", (request, response) -> {
            String stageName = request.queryParams("targetStageName");
            Stage targetStage;
            try {
                targetStage = Stage.valueOf(stageName);
            } catch (IllegalArgumentException e) {
                response.status(400);
                return "Unknown stage";
            }

            try {
                Rollback.run(workingDir.get(), targetStage);
            } catch (UserFriendlyException e) {
                e.printStackTrace();
                response.status(500);
                return e.getMessage();
            }

            return "";
        }, GSON::toJson);


        Spark.get("/working-dir", (request, response) -> workingDir.get().getName());


        Spark.post("/working-dir", (request, response) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(workingDir.get());
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "Directories";
                }
            });

            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                workingDir.set(fileChooser.getSelectedFile());
                webSocketHandler.setWorkingDir(workingDir.get());
            }
            return workingDir.get().getName();
        });


        if(commandLineArgs.openBrowser) {
            String url = "http://localhost:4567/auth?secret=" + initiationSecret;
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to open " + url + " in browser");
            }
        }
    }

    private static String generateSecret() {
        return Integer.toString((int) (Math.random() * Integer.MAX_VALUE));
    }
}