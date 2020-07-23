import {
    Component,
    html,
    hashIsNew,
    hashIsEdit,
    hashIsStart,
    NEW_ASSIGNMENT_TITLE,
    OPEN_ASSIGNMENT_TITLE,
} from "/utils.js"
import Dashboard from "./Dashboard.js"
import ConfigEditor from "./ConfigEditor.js"


export default class App extends Component {
    constructor() {
        super()
        this.state = {
            hasLoaded: false,
            workingDirIsProject: true,
            newScreenIsOpen: hashIsNew(),
            editScreenIsOpen: hashIsEdit(),
            startScreenIsOpen: hashIsStart(),
            lastSelectedStageFromDashboard: null,
            lastWorkingDirFromConfigEditor: null,
        }

        window.addEventListener("hashchange", evt => {
            if (hashIsStart()) {
                this.loadWorkingDir()
            } else if (!this.state.workingDirIsProject && !hashIsNew()) {
                location.hash = "#start"
            }
            this.setState({
                ...this.state,
                newScreenIsOpen: hashIsNew(),
                editScreenIsOpen: hashIsEdit(),
                startScreenIsOpen: hashIsStart(),
            })
        })

        this.loadWorkingDir()
            .then(() => setTimeout(() => this.setState({
                 ...this.state,
                 hasLoaded: true,
             }), 100))
    }

    async loadWorkingDir() {
        const response = await fetch("/working-dir-is-project")
        const text = await response.text()
        console.log("working-dir-is-project:", text)
        if (response.status !== 200) {
            alert(text)
        }
        const workingDirIsProject = text === "true"
        this.setState({
            ...this.state,
            workingDirIsProject,
        })
        location.hash = workingDirIsProject ? "#latest" : "#start"

        console.log(workingDirIsProject, text)
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
            lastWorkingDirFromConfigEditor: text,
        })
        this.loadWorkingDir()
    }

    setConfigEditorOpen(type, isOpen) {
        if (isOpen) {
            location.hash = `${type}`
        } else if (this.state.lastSelectedStageFromDashboard) {
            location.hash = `#${this.state.lastSelectedStageFromDashboard}`
        } else {
            location.hash = "#latest"
        }
    }

    onDashboardStageSelect(newStageName) {
        console.log("selected stage in dashboard", newStageName)
        this.setState({
            ...this.state,
            lastSelectedStageFromDashboard: newStageName,
        })
    }

    onConfigEditorConfirm(newWorkingDir) {
        this.setState({
            ...this.state,
            lastWorkingDirFromConfigEditor: newWorkingDir,
        })
        this.setConfigEditorOpen(null, false)
    }

    render() {
        const {
            workingDirIsProject,
            hasLoaded,
            newScreenIsOpen,
            editScreenIsOpen,
            startScreenIsOpen,
            lastWorkingDirFromConfigEditor,
        } = this.state

        return html`
            <${Dashboard}
                onRequestConfigEdit="${type => this.setConfigEditorOpen(type, true)}"
                onDashboardStageSelect="${newStageName => this.onDashboardStageSelect(newStageName)}"
                defaultWorkingDir="${lastWorkingDirFromConfigEditor}"/>
            <div id="start-screen" class="curtain ${!workingDirIsProject || startScreenIsOpen ? 'up' : ''} ${hasLoaded ? '' : 'no-anim'}">
                <div>
                    <h1>Automark</h1>
                    <p>No assignment is open</p>
                    <button title="${NEW_ASSIGNMENT_TITLE}"
                        onClick="${() => this.setConfigEditorOpen('new', true)}">
                        <span class="symbol">➕️</span> New
                    </button>
                    <button title="${OPEN_ASSIGNMENT_TITLE}"
                        onClick="${() => this.chooseWorkingDir()}">
                        <span class="symbol">📂</span> Open
                    </button>
                </div>
            </div>
            <div class="curtain ${(newScreenIsOpen || editScreenIsOpen) ? 'up' : ''} ${hasLoaded ? '' : 'no-anim'}">
                <${ConfigEditor} type="new"
                    hidden="${!newScreenIsOpen}"
                    onClose="${() => this.setConfigEditorOpen('new', false)}"
                    onConfirm="${this.onConfigEditorConfirm.bind(this)}"/>
                <${ConfigEditor} type="edit"
                    hidden="${!editScreenIsOpen}"
                    onClose="${() => this.setConfigEditorOpen('edit', false)}"
                    onConfirm="${this.onConfigEditorConfirm.bind(this)}"/>
            </div>
        `
    }
}
