const STAGES = [
    {name: "DOWNLOAD", niceName: "Download"},
    {name: "UNZIP", niceName: "Unzip"},
    {name: "EXTRACT", niceName: "Extract"},
    {name: "JPLAG", niceName: "JPlag"},
    {name: "PREPARE_COMPILE", niceName: "Prepare-Compile"},
    {name: "COMPILE", niceName: "Compile"},
    {name: "TEST", niceName: "Test"},
    {name: "SUMMARY", niceName: "Summary"},
]

const PROBLEM_TYPES = [
    "EXCEPTION",
    "NOT_SUBMITTED",
    "INVALID_SUBMISSION_FILE",
    "PLAGIARIZED",
    "COMPILATION_ERROR",
    "TEST_SUITE_FAILURE",
    "TEST_FAILURE",
]

let selectedStage = null
let visibleStage = null
let lastCompletedStage = null

const updateHash = () => {
    const newHash = `#${selectedStage ? selectedStage.name : ""}`
    if (location.hash !== newHash)
        location.hash = newHash
}

const renderStages = () => {
    let done = true
    let _lastStage = null
    const listElement = document.createElement("ul")
    STAGES.forEach(stage => {
        const element = document.createElement("li")
        element.innerHTML = `<a href="#${stage.name}">${stage.niceName}</a>`

        if (done)
            element.classList.add("done")
        if ((selectedStage ? selectedStage.name : lastCompletedStage) === stage.name)
            element.classList.add("selected")

        listElement.appendChild(element)

        element.addEventListener("click", evt => {
            console.log("Clicked", stage)
            selectedStage = stage
            renderStages()
            renderCurrentSubmissions()
            updateHash()
        })

        if (stage.name === lastCompletedStage) {
            done = false
            _lastStage = stage
        }
    })

    const oldListElement = document.querySelector("#stages ul")
    const stagesElement = oldListElement.parentElement
    stagesElement.removeChild(oldListElement)
    stagesElement.appendChild(listElement)

    return _lastStage
}

const renderSubmissions = async (submissions, stage) => {
    if (visibleStage && stage && visibleStage.name === stage.name)
        return

    const submissionsTbody = document.querySelector("#submissions tbody")
    submissionsTbody.innerHTML = ""
    submissions
        .map(renderSubmission)
        .forEach(submissionElem => submissionsTbody.appendChild(submissionElem))
    visibleStage = stage
}

const renderCurrentSubmissions = async () => {
    const submissions = await fetch(`/submissions?stageName=${selectedStage.name}`).then(r => r.json())
    console.log("Rendering for stage", selectedStage ? selectedStage.name : selectedStage, submissions)
    renderSubmissions(submissions, selectedStage)
}

/*<tr>
    <td><input type="checkbox" value="false"></td>
    <td><input type="checkbox" value="false"></td>
    <td>Studen TEST</td>
    <td>test1234@tmail.com</td>
    <td><input type="checkbox" value="false"></td>
    <td><input type="checkbox" value="false"></td>
    <td><input type="checkbox" value="false"></td>
    <td><input type="checkbox" value="false"></td>
    <td><input type="checkbox" value="false"></td>
    <td><input type="checkbox" value="false"></td>
    <td><input type="checkbox" value="false"></td>
</tr>*/
const renderSubmission = submission => {
    const tr = document.createElement("tr")

    const expandTd = document.createElement("td")
    tr.appendChild(expandTd)
    const expandInput = document.createElement("input")
    expandInput.setAttribute("type", "checkbox")
    expandInput.checked = false
    expandTd.appendChild(expandInput)

    const disqualifiedTd = document.createElement("td")
    tr.appendChild(disqualifiedTd)
    const disqualifiedInput = document.createElement("input")
    disqualifiedInput.setAttribute("type", "checkbox")
    disqualifiedInput.checked = submission.isDisqualified
    disqualifiedTd.appendChild(disqualifiedInput)

    const nameTd = document.createElement("td")
    tr.appendChild(nameTd)
    nameTd.innerText = submission.studentName

    const emailTd = document.createElement("td")
    tr.appendChild(emailTd)
    emailTd.innerText = submission.studentEmail

    PROBLEM_TYPES.forEach(problemType => {
        tr.appendChild(renderProblemTd(problemType, submission))
    })

    return tr
}

const renderProblemTd = (problemType, submission) => {
    const isPresent = submission.problems.some(p => p.type === problemType)

    const problemTd = document.createElement("td")
    const problemInput = document.createElement("input")
    problemInput.setAttribute("type", "checkbox")
    problemInput.checked = isPresent
    problemTd.appendChild(problemInput)
    return problemTd
}

(async () => {
    const metadata = await fetch("/latest-metadata").then(r => r.json())
    console.log(metadata)
    lastCompletedStage = metadata.lastStage
    selectedStage = renderStages()
    renderSubmissions(metadata.submissions, selectedStage)
    updateHash()

    window.addEventListener("hashchange", () => {
        console.log("hashchange")
        const stageName = location.hash.substr(1)
        const newSelectedStage = STAGES.find(s => s.name === stageName)
        if (newSelectedStage != null)
            selectedStage = newSelectedStage
        renderStages()
        renderCurrentSubmissions()
    })
})()
