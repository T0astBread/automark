import { h, Component, render } from "/preact-10.4.6.min.js"
import htm from "/htm-3.0.4.min.js"

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


const stageFromName = stageName => STAGES.find(s => s.name === stageName)

const stageFromHash = () => stageFromName(location.hash.substr(1))

const getLastCompletedIndex = lastCompletedStage => {
    for (let i = 0; i < STAGES.length; i++) {
        const stage = STAGES[i]
        if (stage === lastCompletedStage)
            return i
    }
    return -1
}


// Initialize htm with Preact
const html = htm.bind(h)

class App extends Component {
    constructor() {
        super()
        this.state = {
            selectedStage: stageFromHash(),
            runWebSocket: null,
            submissionsData: {},
            terminalText: [
                "Automark v0.0.0",
                "",
                "Output will appear here",
                "",
                "",
            ]
        }

        this.loadData()

        window.addEventListener("hashchange", () => {
            const selectedStage = stageFromHash()
            if (selectedStage != null) {
                this.setState({
                    ...this.state,
                    selectedStage,
                })
            }
        })
    }

    async loadData() {
        const data = await fetch("/data").then(r => r.json())
        console.log(data)

        this.setState({
            ...this.state,
            submissionsData: data,
        })
    }

    render() {
        const {
            runWebSocket,
            terminalText,
            selectedStage,
            submissionsData,
        } = this.state
        const isRunning = runWebSocket != null

        const selectedSubmissions = selectedStage == null ? null : submissionsData[selectedStage.name]

        // see #1
        let lastStageWasCompleted = false

        return html`
            <div id="toolbar">
                <button><span class="symbol">📂</span> Open</button>
                <button><span class="symbol">▶</span>️ Run</button>
                <button id="rollback"><span class="symbol">⬅️</span>️ Rollback</button>
                <div class="spacer"></div>
                <h1>Automark</h1>
            </div>
            <div id="columns-container">
                <div id="stages">
                    <ul>
                        ${STAGES.map(stage => {
                            const data = submissionsData[stage.name]
                            const isCompleted = data != null
                            // here is #1
                            const isWorking = isRunning && !isCompleted && lastStageWasCompleted
                            const isSelected = selectedStage === stage

                            lastStageWasCompleted = isCompleted

                            return html`
                                <li class="${
                                    isCompleted ? "done" : ""} ${
                                    isWorking ? "working" : ""} ${
                                    isSelected ? "selected" : ""}">
                                    <a href="#${stage.name}">${stage.niceName}</a>
                                </li>
                            `
                        })}
                    </ul>
                </div>
                <div id="submissions">
                    <table class="${selectedSubmissions == null ? "hidden" : ""}">
                        <thead>
                        <tr>
                            <th rowspan="2"></th>
                            <th rowspan="2">🗑️</th>
                            <th rowspan="2">Name</th>
                            <th rowspan="2">Email</th>
                            <th colspan="7">Problems</th>
                        </tr>
                        <tr>
                            <th>E</th>
                            <th>I</th>
                            <th>N</th>
                            <th>P</th>
                            <th>C</th>
                            <th>TS</th>
                            <th>T</th>
                        </tr>
                        </thead>
                        <tbody>${
                            selectedSubmissions == null ? null : selectedSubmissions.map(submission => html`
                                <tr>
                                    <td><input type="checkbox" checked="${false}"/></td>
                                    <td><input type="checkbox" checked="${submission.isDisqualified}"/></td>
                                    <td>${submission.studentName}</td>
                                    <td>${submission.studentEmail}</td>
                                    ${PROBLEM_TYPES.map(problemType => (
                                        html`<td><input type="checkbox" checked="${submission.problems.some(p => p.type === problemType)}"/></td>`
                                    ))}
                                </tr>
                            `)
                        }</tbody>
                    </table>
                    <div class="submissions-error ${selectedSubmissions != null ? "hidden" : ""}" id="submissions-error-stage-not-completed">
                        <div class="symbol">⚠️</div>
                        <div>Selected stage has not been completed yet</div>
                    </div>
                </div>
                <pre id="terminal"><code>${terminalText.map(line => `${line}\n`)}</code></pre>
            </div>
        `
    }
}

render(html`<${App}/>`, document.body)
