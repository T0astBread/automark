import { h, Component, render, createRef } from "/preact-10.4.6.min.js"
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

const STAGES_REVERSE = [...STAGES].reverse()

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


// Initialize htm with Preact
const html = htm.bind(h)

class App extends Component {
    constructor() {
        super()
        this.state = {
            selectedStage: stageFromHash(),
            runWebSocket: null,
            submissionsData: {},
        }
        this.terminalRef = createRef()

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

    async loadData(jumpToLastSuccessful) {
        const data = await fetch("/data").then(r => r.json())
        console.log(data)

        this.setState({
            ...this.state,
            submissionsData: data,
        })
    }

    selectLastCompletedStage() {
        const lastCompletedStage = this.getLastCompletedStage()
        if (lastCompletedStage != null)
            location.hash = `#${lastCompletedStage.name}`
    }

    getLastCompletedStage() {
        const lastCompletedStage = this.state.submissionsData == null
            ? null
            : STAGES_REVERSE.find(stage => this.state.submissionsData[stage.name] != null)

        return lastCompletedStage
    }

    async rollback() {
        const { selectedStage } = this.state

        if (selectedStage != null) {
            console.log("rollback", selectedStage)
            const response = await fetch(`/rollback?targetStageName=${selectedStage.name}`, {
                method: "POST"
            })
            const text = await response.text()
            console.log(text)
            if (response.status !== 200) {
                alert(text)
            }
            await this.loadData()
            this.selectLastCompletedStage()
        }
    }

    async startRun() {
        const runWebSocket = new WebSocket(`ws://${location.host}/run`)

        runWebSocket.addEventListener("message", async ({ data }) => {
            switch (data[0]) {
                case "l":
                    this.appendTerminalText(data.substr(1))
                    break
                case "s":
                    await this.loadData()
                    this.selectLastCompletedStage()
                    break
            }
        })

        runWebSocket.addEventListener("close", async evt => {
            this.setState({
                ...this.state,
                runWebSocket: null,
            })
            await this.loadData()
            this.selectLastCompletedStage()
        })

        runWebSocket.addEventListener("open", evt => {
            runWebSocket.send(document.cookie.replace("auth=", ""))  // FIXME: This WILL break if cookies ever change
        })

        this.setState({
            ...this.state,
            runWebSocket,
        })
    }

    appendTerminalText(text) {
        const terminal = this.terminalRef.current
        if (terminal != null) {
            const terminalText = terminal.children[0]
            const childElementCount = terminalText.childElementCount
            for (let i = 0; i < childElementCount - 200; i++) {
                terminalText.children[i].remove()
            }
            const lineSpan = document.createElement("span")
            lineSpan.innerText = text
            terminalText.appendChild(lineSpan)
            terminal.scrollTop = terminal.scrollHeight
        }
    }

    render() {
        const {
            runWebSocket,
            selectedStage,
            submissionsData,
        } = this.state
        const isRunning = runWebSocket != null

        const selectedSubmissions = selectedStage == null ? null : submissionsData[selectedStage.name]
        const nothingCompleted = Object.keys(submissionsData).length === 0

        // see #1
        let lastStageWasCompleted = false

        return html`
            <div id="toolbar">
                <button disabled="${isRunning}">
                    <span class="symbol">📂</span> Open
                </button>
                <button onClick="${() => this.startRun()}" disabled="${isRunning}">
                    <span class="symbol">▶</span>️ Run
                </button>
                <button onClick="${() => this.rollback()}" disabled="${isRunning || nothingCompleted || selectedSubmissions == null}">
                    <span class="symbol">⬅️</span>️ Rollback
                </button>
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
                    <div class="submissions-error ${nothingCompleted || selectedSubmissions != null ? "hidden" : ""}">
                        <div class="symbol">⚠️</div>
                        <div>Selected stage has not been completed yet</div>
                    </div>
                    <div class="submissions-error ${nothingCompleted ? "" : "hidden"}">
                        <div class="symbol">💡️</div>
                        <div>Nothing to show. Run a stage first!</div>
                    </div>
                </div>
                <pre id="terminal" ref="${this.terminalRef}"><code></code></pre>
            </div>
        `
    }
}

render(html`<${App}/>`, document.body)
