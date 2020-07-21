<link rel="stylesheet" href="manual-style.css">

# Automark

Automark is a batch processing program to automatically download,
test and grade Java programming assignments and homework.

To build or run Automark, a Java 11 JDK (or later) is required.
OpenJDK is recommended. A JRE is not sufficient, since it does not
include development tools needed for compiling and testing
submissions.

<section>

## Operation

Automark implements a so-called "pipeline": a program divided into
several sequential steps - "stages" - that are dependent on the
result of the respective previous stage.

These are the stages of Automark in order of execution:

| Stage | Purpose |
|-|-|
| __DOWNLOAD__ | Downloads submissions with metadata from [Moodle](https://en.wikipedia.org/wiki/Moodle) |
| __UNZIP__ | Expects the submission to be a single ZIP file and unpacks it |
| __EXTRACT__ | Filters submitted files to only keep certain specified files |
| __JPLAG__ | Runs submission files through [JPlag](https://jplag.ipd.kit.edu/) |
| __PREPARE_COMPILE__ | Prepares source files for namespaced compilation |
| __COMPILE__ | Compiles source files __in the same classpath as Automark is running in__ |
| __TEST__ | Tests submissions against a set of specified test suites __in the same classpath as Automark is running in__ |
| __SUMMARY__ | Generates a report containing test failures and other problems for each submission |

Stages are run without manual intervention unless required.
If a submission fails to fulfill the requirements of a stage, it will
be tagged with a [problem](#problems) and possibly excluded. If a
stage completes, Automark will automatically continue with the next
stage, unless a problem has been detected in at least one submission
during the stage. See [Problems after a Stage](#problems-after-a-stage).

Stages are only marked successful if the whole stage completes for
every submission without unrecoverable errors. In case of an
unrecoverable error, automark will exit but can simply retry the
failed stage from the result of the previous stage.

Successful stages can be rolled back. See [rollback](#rollback).

The JPLAG stage has some special behavior. See [JPlag](#jplag).

For security considerations see [Security](#security).

</section>

<section>

## Subcommands

<div class="subcommand">

### `run`

Starts execution from the last completed stage.

This is also what's run when no subcommand is specified.

#### CLI
```
automark [run]
```

#### GUI

Click the "Run" button in the toolbar.

</div>

<div class="subcommand">

### `status`

Displays details about the current state of submissions.

#### CLI
```
automark status
```

#### GUI

The whole GUI acts as a "status dashboard". You can overview
submissions in the middle, stages on the left and terminal output on
the right. You can click on stages on the left to view the status
after that specific stage.

</div>

<div class="subcommand">

### `rollback`

Deletes the results of every stage after __and including__ a specified
target stage and marks these stages as not completed.

rollback does not revert global state like the [JPlag repository](#jplag) or
sent emails.

#### CLI
```
automark rollback <stage>
```

`<stage>` is the name of the target stage (f.ex. `COMPILE`).

#### GUI

Select the target stage on the left and click the "Rollback" button
in the toolbar.

</div>


<div class="subcommand">

### `mark-resolved`

Marks one or more problems in one or more submissions as resolved
without actually changing anything about the submission.

Works after every stage but only affects the results of the most
recently completed stage so if you roll back, this will be reverted.

#### CLI
```
automark mark-resolved <slug> [--problem <ident>] [--requalify]
```

`<slug>` is the slug of the submission to operate on or `_`
(underscore) to operate on all submissions.

`--problem <ident>` must be present if you want to
mark one or more problems as resolved. `<ident>` is either the name
of the problem (f.ex. `EXCEPTION` to match `EXCEPTION` problems) or
the numerical position of a single problem in the output of
`automark status`. If a problem name is specified, all matching
problems are marked as resolved.

`--requalify` must be present if you want to re-include a submission
that has been excluded from further processing due to a critical
problem.

mark-resolved without either `--problem` or `--requalify` is a no-op.

#### GUI

mark-resolved is available in the GUI but only if you have selected
the most recently completed stage on the left.

Clicking on checked checkboxes in the problem rainbow marks all
problems of that type for the submission as resolved.

Clicking on the "X" button of a problem in the expanded view of a
submission marks that specific problem as resolved.

Clicking on a checked checkbox in the "trash can" column re-includes
a submission that has been excluded from further problem due to a
critical problem.

</div>


<div class="subcommand">

### `mark-plagiarized`

Tags one or more submissions with the PLAGIARIZED problem.

Works after every stage but only affects the results of the most
recently completed stage so if you roll back, this will be reverted.

Can be reverted using [mark-resolved](#mark-resolved).

#### CLI
```
automark mark-plagiarized <slugs>
```

`<slugs>` is one or more space-seperated submission slugs to mark as
plagiarized. Note that you might have to quote (`"`) this parameter
if you specify more than one slug.

#### GUI

mark-plagiarized is available in the GUI but only if you have
selected the most recently completed stage on the left.

To mark a submission as plagiarized, simply click the checkbox
corresponding to the PLAGIARIZED problem in the problem rainbow.
(This is the checkbox in the yellow "P" column).

Reverting mark-plagiarized in the GUI is as simple as clicking the
same checkbox again.

</div>

<div class="subcommand">

### `gui`

Starts the GUI.

#### CLI
```
automark gui
```

#### GUI

Not possible

</div>

<div class="subcommand">

### `manual`

Shows this manual in the browser.

#### CLI
```
automark <manual|--help|-h>
```

#### GUI

Click the "Help" link in the toolbar.

</div>

</section>

<section>

## Problems

A problem denotes a submission's inability to fully pass a stage.

Problems generally don't mean a submission is invalid or Automark is
unable to process the submission. If a submission is actually
invalid to the point where it can't be processed further, it will be
marked as "disqualified" (i.e. excluded from further processing).

### Problems after a Stage

In case Automark reports that some submissions have new problems
after a stage, you have the following choices:

* __Do nothing:__ The problems might be actual mistakes in the
  submissions that should be marked as such.
* __rollback:__ Use [rollback](#rollback) to revert the results of
  the stage. You can then edit the results of the previous stage to
  correct the problems.
* __mark-resolved:__ Manually correct the problems and use
  [mark-resolved](#mark-resolved).

</section>

<section>

## JPlag

### The JPlag repository

After submissions are run through JPlag, they are copied to a special
folder called the JPlag repository. The JPlag repository is a
historic collections of all submissions for any given assignment and
is used to detect plagiarism from previous years' submissions.

The JPlag repo location is specified using the `jplagRepository` key in
the configuration file. An absolute path should be used.

Each assignment has its own sub-folder in the repo named after what
was specified as the assignment name using the `assignmentName` key.

To remove a submission from the JPlag repo, simply delete the
corresponding folder in `<repo_location>/<assignmentName>`. Rolling
the JPLAG stage back does not remove a submission from the JPlag
repo. Submissions that are already in the repo will not be copied
to the repo again and __will override submissions local to the
current execution,__ so __if you roll back the JPLAG stage, make sure to
delete all submissions of the current year from the JPlag repo!__

### Detecting plagiarism

After the JPLAG stage, submissions aren't automatically marked as
plagiarized, as this would be too error prone. Instead, you will
decide what to mark as plagiarized using the generated JPlag report
and [mark-plagiarized](#mark-plagiarized).

Execution always stops after the JPLAG stage in oder to allow you to
review the JPlag report and mark plagiarized submissions.

</section>

<section>

## Security

Submissions are compiled and tested in the same process and classpath
as the running program. It is __strongly__ advised to limit access to
resources such as the network and filesystems using isolation
technologies such as virtual machines.

Alternatively, you can also review every submission before any code
is compiled or run. This is always possible since execution always
stops after the JPLAG stage.

</section>
