import {
    Component,
    html,
    hashIsNew,
    hashIsEdit,
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
            lastSelectedStageFromDashboard: null,
            lastWorkingDirFromConfigEditor: null,
        }

        window.addEventListener("hashchange", evt => {
            if (!this.state.workingDirIsProject && !hashIsNew()) {
                location.hash = "#start"
            } else {
                this.setState({
                    ...this.state,
                    newScreenIsOpen: hashIsNew(),
                    editScreenIsOpen: hashIsEdit(),
                })
            }
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
        if (!workingDirIsProject) {
            this.setState({
                ...this.state,
                workingDirIsProject: false,
            })
            location.hash = "#new"
        }
        console.log(workingDirIsProject, text)
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
            hasLoaded,
            workingDirIsOpen,
            newScreenIsOpen,
            editScreenIsOpen,
            lastWorkingDirFromConfigEditor,
        } = this.state

        return html`
            <${Dashboard}
                onRequestConfigEdit="${type => this.setConfigEditorOpen(type, true)}"
                onDashboardStageSelect="${newStageName => this.onDashboardStageSelect(newStageName)}"
                defaultWorkingDir="${lastWorkingDirFromConfigEditor}"/>
            <div class="curtain ${(workingDirIsOpen || newScreenIsOpen || editScreenIsOpen) ? 'up' : ''} ${hasLoaded ? '' : 'no-anim'}">
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
