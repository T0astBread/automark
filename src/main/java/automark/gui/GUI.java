package automark.gui;

import automark.*;
import automark.io.*;
import automark.models.*;
import automark.subcommands.*;
import com.google.gson.*;
import spark.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.atomic.*;

public class GUI {
    public static void start(CommandLineArgs commandLineArgs) {
        final Gson GSON = new Gson();
        final String MIME_JSON = "application/json";

        final File workingDir = commandLineArgs.workingDir;

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

            Metadata.MetadataLoadingResult result = Metadata.loadLatestMetadata(workingDir);
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

            List<Submission> submissions = Metadata.loadSubmissions(Metadata.getMetadataFile(workingDir, stage));
            return submissions;
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
                Rollback.run(workingDir, targetStage);
            } catch (UserFriendlyException e) {
                e.printStackTrace();
                response.status(500);
                return e.getMessage();
            }

            return "";
        }, GSON::toJson);


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
