package automark.stages.download;

import automark.io.*;
import automark.models.*;
import org.jsoup.*;
import org.jsoup.nodes.*;

import java.io.*;
import java.util.*;


public class MoodleSession implements AutoCloseable {

    public MoodleSession(String baseUrl) throws MoodleException {
        this.baseUrl = sanitizeBaseUrl(baseUrl);
    }

    public MoodleSession login(
            String username,
            String password
    ) throws MoodleException, IOException {
        final String LOGIN_PAGE = "/login/index.php";
        final String LOGINTOKEN_NAME = "logintoken";

        if (this.loggedIn)
            logout();

        Document formDoc = httpGet(LOGIN_PAGE);

        this.sessionCookies = Jsoup.connect(this.baseUrl + LOGIN_PAGE)
                .method(Connection.Method.POST)
                .followRedirects(true)
                .cookies(this.sessionCookies)
                .data("username", username)
                .data("password", password)
                .data(LOGINTOKEN_NAME, hiddenInputVal(formDoc, LOGINTOKEN_NAME))
                .execute()
                .cookies();

        this.loggedIn = true;
        System.out.println("Logged into Moodle");

        return this;
    }


    public List<Submission> listSubmissions(String assignmentID) throws IOException, MoodleException {
        final String VIEW_PAGE = "/mod/assign/view.php";
        final String CONTEXTID_NAME = "contextid";
        final String ID_NAME = "id";
        final String USERID_NAME = "userid";
        final String SESSKEY_NAME = "sesskey";

        if (!this.loggedIn)
            throw new MoodleException(NOT_LOGGED_IN_EXC);

        final String assignmentSuffix = "?id=" + assignmentID + "&action=grading";
        Document formDoc = httpGet(VIEW_PAGE + assignmentSuffix);

        Connection.Response response = Jsoup.connect(this.baseUrl + VIEW_PAGE)
                .method(Connection.Method.POST)
                .followRedirects(true)
                .cookies(this.sessionCookies)
                .data("perpage", "-1")
                .data("filter", "")
                .data("quickgrading", "1")
                .data("downloadasfolders", "1")
                .data(CONTEXTID_NAME, hiddenInputVal(formDoc, CONTEXTID_NAME))
                .data(ID_NAME, hiddenInputVal(formDoc, ID_NAME))
                .data(USERID_NAME, hiddenInputVal(formDoc, USERID_NAME))
                .data(SESSKEY_NAME, hiddenInputVal(formDoc, SESSKEY_NAME))
                .data("_qf__mod_assign_grading_options_form", "1")
                .data("mform_isexpanded_id_general", "1")
                .data("action", "saveoptions")
                .data("submitbutton", "Sichern+und+Tabelle+aktualisieren")
                .execute();
        mergeCookies(response.cookies());

        Document gradingDoc = httpGet(VIEW_PAGE + assignmentSuffix);
        List<Submission> submissions = new ArrayList<>();
        for (int i = 0; ; i++) {
            Element row = gradingDoc.getElementById("mod_assign_grading_r" + i);

            if (row == null) {
                return submissions;
            }

            Element nameCell = row.select(".cell.c2 a").first();
            if (nameCell == null)
                throw new MoodleException("Submission has no name cell; Index: " + i + "; URL: " + gradingDoc.location());
            String studentName = nameCell.text();

            Element emailCell = row.select(".cell.c3").first();
            if (emailCell == null)
                throw new MoodleException("Submission has no email cell; Index: " + i + "; URL: " + gradingDoc.location());
            String studentEmail = emailCell.text();

            Element fileLink = row.selectFirst(".cell.c8 a[target=_blank]");
            String fileUrl = null;
            if (fileLink != null)
                fileUrl = fileLink.attr("href");

            String slug = studentName
                    .trim()
                    .toLowerCase()
                    .replaceAll("\\s", "_")
                    + "_" + i;

            submissions.add(new Submission(slug, studentName, studentEmail, fileUrl));
        }
    }

    public File downloadSubmission(
            Submission submission,
            File downloadsDir,
            boolean overrideExisting
    ) throws MoodleException, IOException {
        if (!this.loggedIn)
            throw new MoodleException(NOT_LOGGED_IN_EXC);

        if (submission.isDisqualified())
            throw new MoodleException("Tried to download a disqualified submission: \"" + submission.getSlug() + "\"");

        File outputFile = new File(downloadsDir, submission.getSlug() + ".zip");
        if (!overrideExisting && outputFile.exists())
            throw new MoodleException("Submission file already exists");

        Connection.Response response = Jsoup.connect(submission.getFileURL())
                .cookies(this.sessionCookies)
                .ignoreContentType(true)
                .execute();
        mergeCookies(response.cookies());

        try (InputStream bodyStream = response.bodyStream();
             OutputStream outputStream = new FileOutputStream(outputFile)) {
            FileIO.pipe(bodyStream, outputStream);
        }

        return outputFile;
    }


    public void logout() throws IOException, MoodleException {
        if (!this.loggedIn)
            return;

        Document formDoc = httpGet("/");
        Element logoutLink = formDoc.selectFirst("a[data-title=logout,moodle]");

        if (logoutLink == null)
            throw new MoodleException("No logout link", formDoc);

        Jsoup.connect(logoutLink.attr("href"))
                .method(Connection.Method.GET)
                .followRedirects(true)
                .cookies(this.sessionCookies)
                .execute();

        this.loggedIn = false;
        System.out.println("Logged out of Moodle");
    }

    /**
     * Only used from {@link AutoCloseable}. Use logout() instead.
     *
     * @throws IOException
     * @throws MoodleException
     */
    @Deprecated
    @Override
    public void close() throws IOException, MoodleException {
        logout();
    }


    private final String NOT_LOGGED_IN_EXC = "Not logged in";

    private final String baseUrl;
    private Map<String, String> sessionCookies = new HashMap<>();
    private boolean loggedIn = false;


    private Document httpGet(String url) throws IOException {
        Connection.Response response = Jsoup.connect(this.baseUrl + url)
                .cookies(this.sessionCookies)
                .method(Connection.Method.GET)
                .followRedirects(true)
                .execute();

        mergeCookies(response.cookies());

        return response.parse();
    }

    private void mergeCookies(Map<String, String> cookies) {
        cookies.forEach((k, v) -> {
            this.sessionCookies.put(k, v);
        });
    }

    private static String sanitizeBaseUrl(String baseUrl) throws MoodleException {
        if (!baseUrl.matches("^http(s)?://.*"))
            throw new MoodleException("baseUrl must start with http:// or https://");

        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    private static String hiddenInputVal(Document doc, String inputName) throws MoodleException {
        String inputValue = doc.select("input[name=" + inputName + "][type=hidden]").attr("value");
        if (inputValue == null)
            throw new MoodleException("Hidden input " + inputName + " unexpectedly has no value; on page: " + doc.location());
        return inputValue;
    }
}
