package automark.gui;

import automark.*;
import automark.io.*;
import automark.models.*;
import automark.subcommands.*;
import com.google.gson.*;
import com.google.gson.reflect.*;
import spark.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.lang.reflect.*;
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
            JFrame frame = new JFrame();
            frame.setTitle("Ignore me");
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            JOptionPane.showMessageDialog(frame, e.getMessage(), e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
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


        Spark.get("/working-dir-is-project", (request, response) -> {
            return new File(workingDir.get(), "config.properties").exists();
        });


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

            JFrame frame = new JFrame();
            frame.setTitle("Ignore me");
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                workingDir.set(fileChooser.getSelectedFile());
                webSocketHandler.setWorkingDir(workingDir.get());
            }
            frame.setVisible(false);
            return workingDir.get().getName();
        });


        Spark.post("/mark-resolved", (request, response) -> {
            String submissionSlug = request.queryParams("submissionSlug");
            String problemIdentifier = request.queryParams("problemIdentifier");
            boolean requalify = "true".equals(request.queryParams("requalify"));
            try {
                MarkResolved.run(workingDir.get(), submissionSlug, problemIdentifier, requalify);
            } catch (UserFriendlyException e) {
                response.status(500);
                e.printStackTrace();
                return e.getMessage();
            }
            return "ok";
        });


        Spark.post("/mark-plagiarized", (request, response) -> {
            String submissionSlug = request.queryParams("submissionSlug");
            try {
                MarkPlagiarized.run(workingDir.get(), List.of(submissionSlug));
            } catch (UserFriendlyException e) {
                response.status(500);
                e.printStackTrace();
                return e.getMessage();
            }
            return "ok";
        });


        Spark.get("/config", (request, response) -> {
            response.type(MIME_JSON);
            try {
                Properties config = Config.loadConfig(workingDir.get());

                Map<String, Object> responseBody = new HashMap<>();
                config.forEach((key, value) -> {
                    responseBody.put(key.toString(), value.toString());
                });

                responseBody.put("path", workingDir.get().getAbsolutePath());
                responseBody.put("moodleUsernameEnabled", responseBody.containsKey(Config.MOODLE_USERNAME));
                responseBody.put("moodlePasswordEnabled", responseBody.containsKey(Config.MOODLE_PASSWORD));

                return responseBody;
            } catch (UserFriendlyException e) {
                response.status(500);
                e.printStackTrace();
                return e.getMessage();
            }
        }, GSON::toJson);


        Spark.post("/config", (request, response) -> {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> bodyProps = GSON.fromJson(request.body(), type);

            bodyProps.remove("moodleUsernameEnabled");
            bodyProps.remove("moodlePasswordEnabled");
            bodyProps.remove("smtpUsernameEnabled");
            bodyProps.remove("smtpPasswordEnabled");
            bodyProps.remove("formIsValid");

            Set<String> nullKeys = new HashSet<>();
            bodyProps.forEach((key, value) -> {
                if (value == null)
                    nullKeys.add(key);
            });
            nullKeys.forEach(bodyProps::remove);

            String path = bodyProps.remove("path");

            File assignmentDir = new File(path);
            if (!assignmentDir.exists()) {
                int r = JOptionPane.showConfirmDialog(null,
                        "Assignment directory does not exist. Create it now?",
                        "Automark",
                        JOptionPane.OK_CANCEL_OPTION);
                if (r == JOptionPane.OK_OPTION) {
                    assignmentDir.mkdirs();
                } else {
                    return "Cancelled by user";
                }
            }

            Properties config = new Properties();
            config.putAll(bodyProps);
            File configFile = Config.getConfigFile(assignmentDir);
            try (FileWriter writer = new FileWriter(configFile)) {
                config.store(writer, "Generated by Automark GUI");
            } catch (IOException e) {
                response.status(500);
                return e.getMessage();
            }

            workingDir.set(assignmentDir);
            webSocketHandler.setWorkingDir(workingDir.get());

            return "true " + assignmentDir.getName();
        });


        Spark.get("/dir-choice", (request, response) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(workingDir.get().getParentFile());
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

            JFrame frame = new JFrame();
            frame.setTitle("Ignore me");
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                frame.setVisible(false);
                return fileChooser.getSelectedFile().getAbsolutePath();
            }
            frame.setVisible(false);

            return "false";
        });


        String url = "http://localhost:4567/auth?secret=" + initiationSecret;
        if(commandLineArgs.openBrowser) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to open " + url + " in browser");
            }
        } else {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    System.exit(-999);
                }
                System.out.println();
                System.out.println("Open this URL in your browser: " + url);
                System.out.println();
            }).start();
        }
    }

    public static void startManual(CommandLineArgs commandLineArgs) {
        Spark.initExceptionHandler((e) -> {
            JFrame frame = new JFrame();
            frame.setTitle("Ignore me");
            frame.setAlwaysOnTop(true);
            frame.setVisible(true);
            JOptionPane.showMessageDialog(frame, e.getMessage(), e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            e.printStackTrace();
        });

        Spark.staticFiles.location("/automark/gui");

        Spark.init();

        if(commandLineArgs.openBrowser) {
            String url = "http://localhost:4567/manual.html";
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
