import { Component, html, createRef } from "/utils.js"

const JPLAG_LANGUAGES = [
    "java19",
    "java17",
    "java15",
    "java15dm",
    "java12",
    "java11",
]

const ASSIGNMENT_ID_REGEX = "\\d+"
const SOURCE_FILES_REGEX = "(\\w+\\.java(,\\s*\\w+\\.java)*)?"


export default class ConfigEditor extends Component {
    constructor() {
        super()
        this.state = {
            assignmentName: null,
            assignmentID: null,
            jplagLanguage: "java19",
            jplagRepository: null,
            sourceFiles: "",
            downloadStage: "moodle",
            moodleBaseURL: null,
            moodleTeachers: "",
            moodleUsernameEnabled: true,
            moodleUsername: null,
            moodlePasswordEnabled: false,
            moodlePassword: null,
            path: null,
            formIsValid: false,
        }
//        this.state = {
//            assignmentName: "Debug Assignment",
//            assignmentID: "1234",
//            jplagLanguage: "java19",
//            jplagRepository: "/path/to/jplag-repo",
//            sourceFiles: "MyFile1.java, File2.java",
//            downloadStage: "moodle",
//            moodleBaseURL: "https://www.school.com/mymoodle",
//            moodleTeachers: "teacher1@school.com, other.teacher@school.com",
//            moodleUsernameEnabled: true,
//            moodleUsername: "moodleuser123",
//            moodlePasswordEnabled: false,
//            moodlePassword: null,
//            path: "/path/to/DebugAssignment",
//            formIsValid: false,
//        }
        this.formRef = createRef()
    }

    componentDidUpdate() {
        // Validate in case new fields have been rendered
        const formIsValid = this.formIsValid()
        if (formIsValid !== this.state.formIsValid) {
            super.setState({
                ...this.state,
                formIsValid,
            })
        }
    }

    async onPathSelectButtonClick(fieldName) {
        const response = await (await fetch("/dir-choice")).text()
        if (response !== "false") {
            this.setState({
                ...this.state,
                [fieldName]: response,
            })
        }
    }

    onInputChange(evt) {
        const { name, value } = evt.currentTarget
        console.log(name, value)
        this.setState({
            ...this.state,
            [name]: value,
        })
    }

    onJPlagLanguageSelect(evt) {
        const jplagLanguage = evt.currentTarget.value
        console.log("jplagLanguage", jplagLanguage)
        this.setState({
            ...this.state,
            jplagLanguage,
        })
    }

    onDownloadStageRadioChange(evt) {
        if (evt.currentTarget.checked) {
            const downloadStage = evt.currentTarget.value
            this.setState({
                ...this.state,
                downloadStage,
                moodleUsername: downloadStage === "bypass" ? null : this.state.moodleUsername,
                moodlePassword: downloadStage === "bypass" ? null : this.state.moodlePassword,
            })
        }
    }

    onFieldToggleChange(evt) {
        const { name, checked } = evt.currentTarget
        const fieldName = name.substr(0, name.length - "Enabled".length)
        this.setState({
            ...this.state,
            [name]: checked,
            [fieldName]: checked ? this.state[fieldName] : null,
        })
    }

    setState(state) {
        super.setState({
            ...state,
            formIsValid: this.formIsValid(),
        })
    }

    formIsValid() {
        const formElem = this.formRef.current
        return formElem != null && formElem.checkValidity()
    }

    async onConfirmClick(evt, onConfirm) {
        console.log("confirm")
        evt.preventDefault()

        const response = await fetch("/new", {
            method: "POST",
            body: JSON.stringify(this.state),
        })
        const text = await response.text()
        console.log("POST /new", text)
        if (response.status !== 200) {
            alert(text)
        } else if (text.startsWith("true ")) {
            onConfirm(text.substr("true ".length))
        }
    }

    render({ hidden, onClose, onConfirm }) {
        const {
            assignmentName,
            assignmentID,
            jplagLanguage,
            jplagRepository,
            sourceFiles,
            downloadStage,
            moodleBaseURL,
            moodleTeachers,
            moodleUsernameEnabled,
            moodleUsername,
            moodlePasswordEnabled,
            moodlePassword,
            path,
            formIsValid,
        } = this.state

        return html`
            <div class="${hidden ? 'hidden' : ''} screen assignment-editor-screen" id="assignment-editor-screen">
                <h2>New Assignment</h2>
                <form ref="${this.formRef}">
                    <div class="form-body">
                        <label for="pathInput" class="full">Assignment directory</label>
                        <input name="path"
                            id="pathInput"
                            class="has-choose-button"
                            defaultValue="${path}"
                            onInput="${this.onInputChange.bind(this)}"
                            minlength="1"
                            required/>
                        <button type="button"
                            class="choose"
                            onClick="${() => this.onPathSelectButtonClick('path')}">
                            Choose...
                        </button>
                        <label for="assignmentNameInput">Assignment name</label>
                        <input name="assignmentName"
                            id="assignmentNameInput"
                            defaultValue="${assignmentName}"
                            onInput="${this.onInputChange.bind(this)}"
                            minlength="1"
                            required/>
                        <label for="assignmentIDInput">Assignment ID (numeric)</label>
                        <input name="assignmentID"
                            id="assignmentIDInput"
                            defaultValue="${assignmentID}"
                            onInput="${this.onInputChange.bind(this)}"
                            pattern="${ASSIGNMENT_ID_REGEX}"
                            inputmode="number"
                            required/>
                        <label for="jplagLanguageInput" class="more-distance-top">JPlag language</label>
                        <select name="jplagLanguage"
                            id="jplagLanguageInput"
                            class="more-distance-top"
                            onChange="${this.onJPlagLanguageSelect.bind(this)}">
                            ${JPLAG_LANGUAGES.map(lang => html`
                                <option value="${lang}" selected="${jplagLanguage === lang}">${lang}</option>
                            `)}
                        </select>
                        <label for="jplagRepositoryInput" class="full">JPlag repository path</label>
                        <input name="jplagRepository"
                            id="jplagRepositoryInput"
                            class="has-choose-button"
                            defaultValue="${jplagRepository}"
                            onInput="${this.onInputChange.bind(this)}"
                            minlength="1"
                            required/>
                        <button type="button"
                            class="choose"
                            onClick="${() => this.onPathSelectButtonClick('jplagRepository')}">
                            Choose...
                        </button>
                        <label for="sourceFilesInput" class="full">Source file names (comma-seperated Java files)</label>
                        <input name="sourceFiles"
                            id="sourceFilesInput"
                            defaultValue="${sourceFiles}"
                            onInput="${this.onInputChange.bind(this)}"
                            pattern="${SOURCE_FILES_REGEX}"/>
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
                            <input type="url"
                                name="moodleBaseURL"
                                id="moodleBaseURLInput"
                                class="more-distance-top"
                                defaultValue="${moodleBaseURL}"
                                onInput="${this.onInputChange.bind(this)}"
                                required/>
                            <label for="moodleTeachersInput" class="full">Email addresses of Moodle teachers (comma-seperated)</label>
                            <small class="full">No mail will be sent to these addresses.</small>
                            <input name="moodleTeachers"
                                id="moodleTeachersInput"
                                type="email"
                                multiple
                                defaultValue="${moodleTeachers}"/>

                            <div class="check-option more-distance-top">
                                <input type="checkbox"
                                    name="moodleUsernameEnabled"
                                    id="moodleUsernameEnabledInput"
                                    checked="${moodleUsernameEnabled}"
                                    onChange="${this.onFieldToggleChange.bind(this)}"/>
                                <label for="moodleUsernameEnabledInput" class="wide">Save Moodle username in config</label>
                            </div>
                            <label for="moodleUsernameInput" class="less-distance">Moodle username</label>
                            <input name="moodleUsername"
                                id="moodleUsernameInput"
                                class="less-distance"
                                disabled="${!moodleUsernameEnabled}"
                                defaultValue="${moodleUsername}"
                                onInput="${this.onInputChange.bind(this)}"
                                required="${moodleUsernameEnabled}"/>

                            <div class="check-option">
                                <input type="checkbox"
                                    name="moodlePasswordEnabled"
                                    id="moodlePasswordEnabledInput"
                                    checked="${moodlePasswordEnabled}"
                                    onChange="${this.onFieldToggleChange.bind(this)}"/>
                                <label for="moodlePasswordEnabledInput" class="wide">Save Moodle password in config</label>
                            </div>
                            <label for="moodlePasswordInput" class="less-distance">Moodle password</label>
                            <input type="password"
                                name="moodlePassword"
                                id="moodlePasswordInput"
                                class="less-distance"
                                disabled="${!moodlePasswordEnabled}"
                                defaultValue="${moodlePassword}"
                                onInput="${this.onInputChange.bind(this)}"
                                required="${moodlePasswordEnabled}"/>
                        `}
                    </div>

                    <div class="spacer"></div>

                    <div class="button-row">
                        <button type="button"
                            onClick="${onClose}">
                            Cancel
                        </button>
                        <button type="submit"
                            onClick="${evt => this.onConfirmClick(evt, onConfirm)}"
                            disabled="${!formIsValid}">
                            Confirm
                        </button>
                    </div>
                </form>
            </div>
        `
    }
}
