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
    {name: "EXCEPTION", shortName: "E"},
    {name: "NOT_SUBMITTED", shortName: "N"},
    {name: "INVALID_SUBMISSION_FILE", shortName: "I"},
    {name: "PLAGIARIZED", shortName: "P"},
    {name: "COMPILATION_ERROR", shortName: "C"},
    {name: "TEST_SUITE_FAILURE", shortName: "TS"},
    {name: "TEST_FAILURE", shortName: "T"},
].map(p => ({...p,
    bgStyleClass: `bg-${p.name.toLowerCase().replace(/_/g, "-")}`
}))


const stageFromName = stageName => STAGES.find(s => s.name === stageName)

const stageFromHash = () => stageFromName(location.hash.substr(1))


// Initialize htm with Preact
const html = htm.bind(h)

class App extends Component {
    constructor() {
        super()
        this.state = {
            workingDir: "",
            selectedStage: stageFromHash(),
            runWebSocket: null,
            submissionsData: {},
            expandedSubmissions: [],
        }
        this.terminalRef = createRef()

        this.loadData()
            .then(() => {
                if(this.state.selectedStage == null)
                    this.selectLastCompletedStage()
                this.loadWorkingDir()
            })

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

    async loadWorkingDir() {
        const response = await fetch("/working-dir")
        const text = await response.text()
        console.log(text)
        if (response.status !== 200) {
            alert(text)
        }
        this.setState({
            ...this.state,
            workingDir: text,
        })
    }

    async chooseWorkingDir() {
        const response = await fetch("/working-dir", {
            method: "POST"
        })
        const text = await response.text()
        console.log(text)
        if (response.status !== 200) {
            alert(text)
        }
        this.setState({
            ...this.state,
            workingDir: text,
        })
        await this.loadData(true)
    }

    async loadData(jumpToLastSuccessful) {
        const submissionsData = await fetch("/data").then(r => r.json())
        console.log(submissionsData)

        this.setState({
            ...this.state,
            submissionsData,
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
            for (let i = 0; i < childElementCount - 2000; i++) {
                terminalText.children[i].remove()
            }
            const lineSpan = document.createElement("span")
            lineSpan.innerText = text
            terminalText.appendChild(lineSpan)
            terminal.scrollTop = terminal.scrollHeight
        }
    }

    async markResolved(submission, problemIdentifier, requalify) {
        console.log("markResolved", submission, problemIdentifier, requalify)

        let queryParams = `submissionSlug=${submission.slug}`
        if (problemIdentifier != null)
            queryParams += `&problemIdentifier=${problemIdentifier}`
        if (requalify)
            queryParams += "&requalify=true"
        const response = await fetch(`/mark-resolved?${queryParams}`, {
            method: "POST"
        })
        const text = await response.text()
        console.log(text)
        if (response.status !== 200) {
            alert(text)
        }
        this.loadData()
    }

    render() {
        const {
            workingDir,
            runWebSocket,
            selectedStage,
            submissionsData,
            expandedSubmissions,
        } = this.state
        const isRunning = runWebSocket != null

        const selectedSubmissions = selectedStage == null ? null : submissionsData[selectedStage.name]
        const nothingCompleted = Object.keys(submissionsData).length === 0

        // see #1
        let lastStageWasCompleted = false

        return html`
            <title>${workingDir} - Automark</title>
            <div id="toolbar">
                <button onClick="${() => this.chooseWorkingDir()}" disabled="${isRunning}">
                    <span class="symbol">📂</span> Open
                </button>
                <span>${workingDir}</span>
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
                        <tr>${
                            PROBLEM_TYPES.map(problemType => html`
                                <th title="${problemType.name}" class="${problemType.bgStyleClass}">
                                    ${problemType.shortName}
                                </th>`)
                        }</tr>
                        </thead>
                        <tbody>${
                            selectedSubmissions == null ? null : selectedSubmissions.map(submission => {
                                const isExpanded = expandedSubmissions.includes(submission.slug)
                                const toggleExpanded = () => this.setState({
                                    ...this.state,
                                    expandedSubmissions: isExpanded
                                        ? expandedSubmissions.filter(s => submission.slug !== s)
                                        : [...expandedSubmissions, submission.slug]
                                })
                                const hasProblems = submission.problems.length > 0

                                return html`
                                    <tr class="submission-row">
                                        <td><input type="checkbox"
                                            class="${hasProblems ? '' : 'hidden'}"
                                            checked="${isExpanded}"
                                            onClick="${toggleExpanded}"
                                        /></td>
                                        <td><input type="checkbox"
                                            checked="${submission.isDisqualified}"
                                            disabled="${!submission.isDisqualified}"
                                            title="${submission.isDisqualified
                                                ? 'Re-include disqualified submission ' + submission.slug
                                                : submission.slug + ' is not disqualified from further processing'}"
                                            onClick="${() => this.markResolved(submission, null, true)}"
                                        /></td>
                                        <td>${submission.studentName}</td>
                                        <td>${submission.studentEmail}</td>
                                        ${PROBLEM_TYPES.map(problemType => {
                                            const problems = submission.problems.filter(p => p.type === problemType.name)
                                            const isPresent = problems.length > 0
                                            const title = isPresent
                                                ? `Mark ${problems.length} ${problemType.name} problem${problems.length > 1 ? 's' : ''} in ${submission.slug} as resolved`
                                                : `No ${problemType.name} problems in ${submission.slug}`
                                            return html`<td class="${problemType.bgStyleClass}">
                                                <input type="checkbox"
                                                    checked="${isPresent}"
                                                    disabled="${!isPresent}"
                                                    title="${title}"
                                                    style="${isPresent ? 'cursor:pointer' : ''}"
                                                    onClick="${() => this.markResolved(submission, problemType.name, false)}"
                                                />
                                            </td>`
                                        })}
                                    </tr>
                                    ${!(hasProblems && isExpanded) ? '' : submission.problems.map(problem => {
                                        const problemType = PROBLEM_TYPES.find(p => p.name === problem.type)

                                        return html`<tr class="problem-row">
                                            <td colspan="2"></td>
                                            <td colspan="9">
                                                <h4 class="${problemType.bgStyleClass}">${problem.type}</h4>
                                                <pre>${problem.summary}</pre>
                                            </td>
                                        </tr>`
                                    })}
                                `
                            })
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
