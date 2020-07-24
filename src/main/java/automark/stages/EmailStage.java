package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;
import org.simplejavamail.api.email.*;
import org.simplejavamail.api.mailer.*;
import org.simplejavamail.api.mailer.config.*;
import org.simplejavamail.email.*;
import org.simplejavamail.mailer.*;

import java.io.*;
import java.util.*;

public class EmailStage {
    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions, boolean enableInsecureDebugMechanisms) throws UserFriendlyException {

        if (!"true".equalsIgnoreCase(config.getProperty(Config.EMAIL_STAGE_ENABLED))) {
            System.out.println(Config.EMAIL_STAGE_ENABLED + " not set to \"true\" - not executing EMAIL stage");
            return submissions;
        }

        // Set up SMTP parameters
        final String smtpHost = config.getProperty(Config.SMTP_HOST);
        if (smtpHost == null)
            throw new UserFriendlyException(Config.SMTP_HOST + " config property must be set for EMAIL stage");

        final String _smtpPort = config.getProperty(Config.SMTP_PORT);
        final int smtpPort = _smtpPort == null ? 465 : Config.asInt(_smtpPort);

        final String _smtpProtocol = config.getProperty(Config.SMTP_PROTOCOL);
        final TransportStrategy transportStrategy = _smtpProtocol == null ? TransportStrategy.SMTPS : TransportStrategy.valueOf(_smtpProtocol);

        String smtpUsername = config.getProperty(Config.SMTP_USERNAME);
        if (smtpUsername == null)
            smtpUsername = UI.prompt("SMTP username: ", false);

        String smtpPassword = config.getProperty(Config.SMTP_PASSWORD);
        if (smtpPassword == null)
            smtpPassword = UI.prompt("SMTP password: ", true);

        final String smtpFromAddress = config.getProperty(Config.SMTP_FROM_ADDRESS);
        if (smtpFromAddress == null)
            throw new UserFriendlyException(Config.SMTP_FROM_ADDRESS + " config property must be set for EMAIL stage");

        final String smtpFromName = config.getProperty(Config.SMTP_FROM_NAME);
        if (smtpFromName == null)
            throw new UserFriendlyException(Config.SMTP_FROM_NAME + " config property must be set for EMAIL stage");

        Mailer mailer = MailerBuilder
                .withSMTPServer(smtpHost, smtpPort, smtpUsername, smtpPassword)
                .withTransportStrategy(transportStrategy)
                .verifyingServerIdentity(!enableInsecureDebugMechanisms)
                .buildMailer();

        File summaryDir = new File(Metadata.getDataDir(workingDir), "summary");
        for (Submission submission : submissions) {
            File submissionSummaryFile = new File(summaryDir, submission.getSlug() + ".txt");
            try (BufferedReader reader = new BufferedReader(new FileReader(submissionSummaryFile))) {
                String subject = reader.readLine().substring("Subject: ".length());
                reader.readLine();

                StringBuilder body = new StringBuilder();
                while (true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    body.append(line).append("\n");
                }

                Email email = EmailBuilder.startingBlank()
                        .from(smtpFromName, smtpFromAddress)
                        .to(submission.getStudentName(), submission.getStudentEmail())
                        .withSubject(subject)
                        .withPlainText(body.toString())
                        .buildEmail();

                System.out.println("Sending mail to " + submission.getStudentEmail() + "...");
                mailer.sendMail(email);
            } catch (Exception e) {
                submission.setDisqualified(true);
                submission.addProblem(Problem.createException(Stage.EMAIL, e));
            }
        }

        return submissions;
    }
}
