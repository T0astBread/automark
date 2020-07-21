import { Component, html } from "/utils.js"
import Dashboard from "./Dashboard.js"


export default class App extends Component {
    constructor() {
        super()
        this.state = {
            workingDir: null,
            creationWizardIsOpen: false,
        }
    }

    render() {
        return html`
            <${Dashboard} />
        `
    }
}
