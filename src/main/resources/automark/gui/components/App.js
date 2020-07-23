import { Component, html } from "/utils.js"
import Dashboard from "./Dashboard.js"
import ConfigEditor from "./ConfigEditor.js"


const hashIsCreationWizard = () => {
    const hashStr = location.hash.substr(1)
    return hashStr === "new"
}

export default class App extends Component {
    constructor() {
        super()
        this.state = {
            hasLoaded: false,
            workingDirIsProject: true,
            creationWizardIsOpen: hashIsCreationWizard(),
            lastSelectedStageFromDashboard: null,
        }

        window.addEventListener("hashchange", evt => {
        const _hashIsCreationWizard = hashIsCreationWizard()
            if (!_hashIsCreationWizard && !this.state.workingDirIsProject) {
                location.hash = "#new"
            } else {
                this.setState({
                    ...this.state,
                    creationWizardIsOpen: _hashIsCreationWizard,
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

    setCreationWizardOpen(creationWizardIsOpen) {
        if (creationWizardIsOpen) {
            location.hash = "#new"
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

    render() {
        const {
            hasLoaded,
            workingDirIsOpen,
            creationWizardIsOpen,
        } = this.state

        return html`
            <${Dashboard}
                onRequestNewProject="${() => this.setCreationWizardOpen(true)}"
                onDashboardStageSelect=${newStageName => this.onDashboardStageSelect(newStageName)}/>
            <div class="curtain ${(workingDirIsOpen || creationWizardIsOpen) ? 'up' : ''} ${hasLoaded ? '' : 'no-anim'}">
                <${ConfigEditor} hidden="${!creationWizardIsOpen}"
                    onClose="${() => this.setCreationWizardOpen(false)}"/>
            </div>
        `
    }
}
