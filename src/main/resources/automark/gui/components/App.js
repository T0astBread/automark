import { Component, html } from "/utils.js"
import Dashboard from "./Dashboard.js"


export default class App extends Component {
    constructor() {
        super()
        this.state = {
//            workingDir: null,
//            creationWizardIsOpen: false,
            a: true
        }
    }

    render() {
        return html`
            <button onClick="${() => this.setState({a: true})}">Down</button>
            <${Dashboard} />
            <div class="curtain ${this.state.a ? 'up' : ''}">
                <button onClick="${() => this.setState({a: false})}">Up</button>
            </div>
        `
    }
}
