import {
    Component,
    createRef,
    html,
    STAGES,
    STAGES_REVERSE,
    PROBLEM_TYPES,
    stageFromHash,
 } from "/utils.js"


export default class DashboardScreen extends Component {
    constructor({ onDashboardStageSelect, defaultWorkingDir }) {
        super()
        this.state = {
            workingDir: "",
            defaultWorkingDir,
            selectedStage: stageFromHash(),
            runWebSocket: null,
            submissionsData: {},
            expandedSubmissions: [],
        }
        this.terminalRef = createRef()

        this.loadData()
            .then(() => {
                if(this.state.selectedStage == null && location.hash !== "#new")
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
                if (onDashboardStageSelect)
                    onDashboardStageSelect(selectedStage.name)
            } else if (location.hash === "#latest") {
                this.selectLastCompletedStage()
            }
        })
    }

    componentDidMount() {
        this.appendTerminalText("Output will appear here\n\n")
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

    async markPlagiarized(submission) {
        console.log("markPlagiarized", submission)

        const response = await fetch(`/mark-plagiarized?submissionSlug=${submission.slug}`, {
            method: "POST"
        })
        const text = await response.text()
        console.log(text)
        if (response.status !== 200) {
            alert(text)
        }
        this.loadData()
    }

    render({ onRequestNewProject, defaultWorkingDir }) {
        const {
            workingDir,
            runWebSocket,
            selectedStage,
            submissionsData,
            expandedSubmissions,
        } = this.state
        if (defaultWorkingDir !== this.state.defaultWorkingDir) {
            this.setState({
                ...this.state,
                defaultWorkingDir,
            })
            this.loadWorkingDir()
                .then(() => this.loadData())
        }

        const isRunning = runWebSocket != null

        const selectedSubmissions = selectedStage == null ? null : submissionsData[selectedStage.name]
        const nothingCompleted = Object.keys(submissionsData).length === 0
        const lastCompletedIsSelected = this.getLastCompletedStage() === selectedStage

        // see #1
        let lastStageWasCompleted = false

        return html`
            <title>${workingDir} - Automark</title>
            <div id="toolbar">
                <button onClick="${onRequestNewProject}" disabled="${isRunning}">
                    <span class="symbol">📝</span> New
                </button>
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
                <a href="manual.html">Help</a>
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
                                            disabled="${!(lastCompletedIsSelected && submission.isDisqualified)}"
                                            title="${submission.isDisqualified
                                                ? lastCompletedIsSelected
                                                    ? 'Re-include disqualified submission ' + submission.slug
                                                    : submission.slug + ' is disqualified from further processing'
                                                : submission.slug + ' is not disqualified from further processing'}"
                                            onClick="${() => this.markResolved(submission, null, true)}"
                                        /></td>
                                        <td>${submission.studentName}</td>
                                        <td>${submission.studentEmail}</td>
                                        ${PROBLEM_TYPES.map(problemType => {
                                            const problems = submission.problems.filter(p => p.type === problemType.name)
                                            const isPresent = problems.length > 0
                                            const problemIsPlag = problemType.name === "PLAGIARIZED"

                                            let title
                                            if (lastCompletedIsSelected) {
                                                if (isPresent) {
                                                    title = `Mark ${problems.length} ${problemType.name} problem${problems.length > 1 ? 's' : ''} in ${submission.slug} as resolved`
                                                } else if (problemIsPlag) {
                                                    title = `Mark ${submission.slug} as plagiarized`
                                                } else {
                                                    title = `No ${problemType.name} problems in ${submission.slug}`
                                                }
                                            } else {
                                                title = `${problems.length > 0 ? problems.length : 'No'} ${problemType.name} problem${problems.length !== 1 ? 's' : ''} in ${submission.slug}`
                                            }

                                            const onClick = () => {
                                                if (lastCompletedIsSelected) {
                                                    if (problemIsPlag && !isPresent)
                                                        this.markPlagiarized(submission)
                                                    else
                                                        this.markResolved(submission, problemType.name, false)
                                                }
                                            }

                                            return html`<td class="${problemType.bgStyleClass}">
                                                <input type="checkbox"
                                                    checked="${isPresent}"
                                                    disabled="${!lastCompletedIsSelected || (!problemIsPlag && !isPresent)}"
                                                    title="${title}"
                                                    style="${isPresent && lastCompletedIsSelected ? 'cursor:pointer' : ''}"
                                                    onClick="${onClick}"
                                                />
                                            </td>`
                                        })}
                                    </tr>
                                    ${!(hasProblems && isExpanded) ? '' : submission.problems.map((problem, i) => {
                                        const problemType = PROBLEM_TYPES.find(p => p.name === problem.type)

                                        return html`<tr class="problem-row">
                                            <td colspan="2"></td>
                                            <td colspan="9">
                                                <h4 class="${problemType.bgStyleClass}">
                                                    ${problem.type}
                                                    <button class="resolve-button ${lastCompletedIsSelected ? '' : 'hidden'}"
                                                        onClick="${() => this.markResolved(submission, i+1, false)}"
                                                        >⨯</button>
                                                </h4>
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
