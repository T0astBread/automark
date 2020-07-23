import { Component, html } from "/utils.js"


export default class ConfigEditor extends Component {
    constructor() {
        super()
        this.state = {
            downloadStage: "moodle",
            moodleUsernameEnabled: true,
            moodlePasswordEnabled: false,
        }
    }

    onDownloadStageRadioChange(evt) {
        if (evt.currentTarget.checked) {
            this.setState({
                ...this.state,
                downloadStage: evt.currentTarget.value,
            })
        }
    }

    render({ hidden }) {
        const {
            downloadStage,
            moodleUsernameEnabled,
            moodlePasswordEnabled,
        } = this.state

        return html`
            <div class="${hidden ? 'hidden' : ''} screen" id="assignment-editor-screen">
                <h2>New Assignment</h2>
                <form>
                    <label for="assignmentNameInput">Assignment name </label>
                    <input name="assignmentName" id="assignmentNameInput"/>
                    <label for="assignmentIDInput">Assignment ID </label>
                    <input name="assignmentID" id="assignmentIDInput"/>
                    <label for="jplagLanguageInput">JPlag language </label>
                    <select name="jplagLanguage" id="jplagLanguageInput">
                        <option>java19</option>
                        <option>java17</option>
                        <option>java15</option>
                        <option>java15dm</option>
                        <option>java12</option>
                        <option>java11</option>
                        <option>python3</option>
                        <option>c/c++</option>
                        <option>c#-1.2</option>
                        <option>char</option>
                        <option>text</option>
                        <option>scheme</option>
                    </select>
                    <label for="jplagRepositoryInput" class="full">JPlag repository path</label>
                    <input name="jplagRepository" id="jplagRepositoryInput"/>
                    <label for="sourceFilesInput" class="full">Source file names</label>
                    <input name="sourceFiles" id="sourceFilesInput"/>
                    <label id="download-stage-label" class="more-distance-top">Download stage to use</label>
                    <div class="radio-option">
                        <input type="radio"
                            name="downloadStage"
                            value="moodle"
                            id="download-stage-scraper"
                            checked="${downloadStage === 'moodle'}"
                            onChange="${evt => this.onDownloadStageRadioChange(evt)}"/>
                        <label for="download-stage-scraper">
                            <strong>MoodleScraperStage (recommended)</strong>
                            <div class="details">
                                Automatically scrapes submission and student data (like email addresses) from Moodle
                            </div>
                        </label>
                    </div>
                    <div class="radio-option">
                        <input type="radio"
                            name="downloadStage"
                            value="bypass"
                            id="download-stage-bypass"
                            checked="${downloadStage === 'bypass'}"
                            onChange="${evt => this.onDownloadStageRadioChange(evt)}"/>
                        <label for="download-stage-bypass">
                            <strong>BypassDownloadStage</strong>
                            <div class="details">
                                <small>Use this if MoodleScraperStage is broken.</small>
                                <br/>
                                Bypasses scraping Moodle and instead relies on manual data entry
                            </div>
                        </label>
                    </div>

                    ${downloadStage !== "moodle" ? '' : html`
                        <label for="moodleBaseURLInput" class="wide more-distance-top">Moodle base URL</label>
                        <input name="moodleBaseURL" id="moodleBaseURLInput" class="more-distance-top"/>
                        <label for="moodleTeachersInput" class="full">Email addresses of Moodle teachers</label>
                        <input name="moodleTeachers" id="moodleTeachersInput"/>

                        <div class="check-option">
                            <input type="checkbox"
                                name="moodleUsernameEnabled"
                                id="moodleUsernameEnabledInput"
                                checked="${moodleUsernameEnabled}"
                                onChange="${evt => this.setState({...this.state, moodleUsernameEnabled: evt.currentTarget.checked})}"/>
                            <label for="moodleUsernameEnabledInput" class="wide">Save Moodle username in config</label>
                        </div>
                        <label for="moodleUsernameInput" class="less-distance">Moodle username</label>
                        <input
                            name="moodleUsername"
                            id="moodleUsernameInput"
                            class="less-distance"
                            disabled="${!moodleUsernameEnabled}"/>

                        <div class="check-option">
                            <input type="checkbox"
                                name="moodlePasswordEnabled"
                                id="moodlePasswordEnabledInput"
                                checked="${moodlePasswordEnabled}"
                                onChange="${evt => this.setState({...this.state, moodlePasswordEnabled: evt.currentTarget.checked})}"/>
                            <label for="moodlePasswordEnabledInput" class="wide">Save Moodle password in config</label>
                        </div>
                        <label for="moodlePasswordInput" class="less-distance">Moodle password</label>
                        <input
                            type="password"
                            name="moodlePassword"
                            id="moodlePasswordInput"
                            class="less-distance"
                            disabled="${!moodlePasswordEnabled}"/>
                    `}
                </form>
            </div>
        `
    }
}
