package automark.gui;

import automark.models.*;
import com.google.gson.*;
import spark.*;

import javax.swing.*;

public class GUI {
    public static void start() {
        final Gson GSON = new Gson();
        final String MIME_JSON = "application/json";


        Spark.initExceptionHandler((e) -> {
            JOptionPane.showMessageDialog(null, e.getMessage(), e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        });

        Spark.staticFiles.location("/automark/gui");

        Spark.get("/subs", (request, response) -> {
            response.type(MIME_JSON);
            return new Submission(
                    "test_student_1",
                    "Test STUDENT",
                    "teststudent@gmail.com",
                    "https://moodle.filefdkslf");
        }, GSON::toJson);
    }
}
