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

const INITIAL_STATE = {
	assignmentName: null,
	jplagLanguage: "java19",
	jplagRepository: null,
	sourceFiles: "",
	testsDir: null,
	downloadStage: "moodle",
	moodleAssignmentID: null,
	moodleBaseURL: null,
	moodleTeachers: "",
	moodleUsernameEnabled: true,
	moodleUsername: null,
	moodlePasswordEnabled: false,
	moodlePassword: null,
	emailStageEnabled: "true",
	smtpHost: null,
	smtpPort: "465",
	smtpUsernameEnabled: true,
	smtpUsername: null,
	smtpPasswordEnabled: true,
	smtpPassword: null,
	smtpProtocol: "SMTPS",
	smtpFromName: null,
	smtpFromAddress: null,
	path: null,
	formIsValid: false,
}


export default class ConfigEditor extends Component {
    constructor(props) {
        super(props)
        this.state = INITIAL_STATE
//        this.state = {
//            assignmentName: "Debug Assignment",
//            jplagLanguage: "java19",
//            jplagRepository: "/path/to/jplag-repo",
//            sourceFiles: "MyFile1.java, File2.java",
//            downloadStage: "moodle",
//            moodleAssignmentID: "1234",
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

        if (props.type === "edit")
            this.loadConfig()
    }

    componentDidUpdate(prevProps) {
        // Is this an "edit" type ConfigEditor that has just been un-hidden
        if (this.props.type === "edit" && prevProps.hidden && !this.props.hidden) {
            this.loadConfig()
        } else {
            // Validate in case new fields have been rendered
            const formIsValid = this.formIsValid()
            if (formIsValid !== this.state.formIsValid) {
                super.setState({
                    ...this.state,
                    formIsValid,
                })
            }
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

    async loadConfig() {
        const response = await fetch("/config")
        if (response.status === 200) {
            const body = await response.json()
            console.log("GET /config", body)
            this.setState({
                ...INITIAL_STATE,
                ...body,
                emailStageEnabled: (`${body.emailStageEnabled}`).toLowerCase() === "true",
            })
        }
    }

    async onConfirmClick(evt, onConfirm) {
        console.log("confirm")
        evt.preventDefault()

        const response = await fetch("/config", {
            method: "POST",
            body: JSON.stringify(this.state),
        })
        const text = await response.text()
        console.log("POST /config", text)
        if (response.status !== 200) {
            alert(text)
        } else if (text.startsWith("true ")) {
            onConfirm(text.substr("true ".length))
        }
    }

    render({ type, hidden, onClose, onConfirm }) {
        const {
            assignmentName,
            jplagLanguage,
            jplagRepository,
            sourceFiles,
            testsDir,
            downloadStage,
            moodleAssignmentID,
            moodleBaseURL,
            moodleTeachers,
            moodleUsernameEnabled,
            moodleUsername,
            moodlePasswordEnabled,
            moodlePassword,
            emailStageEnabled,
            smtpHost,
            smtpPort,
            smtpUsernameEnabled,
            smtpUsername,
            smtpPasswordEnabled,
            smtpPassword,
            smtpProtocol,
            smtpFromName,
            smtpFromAddress,
            path,
            formIsValid,
        } = this.state

        return html`
            <div class="${hidden ? 'hidden' : ''} screen assignment-editor-screen" id="${type}_assignment-editor-screen">
                <h2>${type === "edit" ? "Edit" : "New"} Assignment</h2>
                <form ref="${this.formRef}">
                    <div class="form-body">
                        <label for="${type}_pathInput" class="full">Assignment directory</label>
                        <input name="path"
                            id="${type}_pathInput"
                            class="has-choose-button"
                            defaultValue="${path}"
                            onInput="${this.onInputChange.bind(this)}"
                            minlength="1"
                            required
                            disabled="${type === 'edit'}"/>
                        <button type="button"
                            class="choose"
                            disabled="${type === 'edit'}"
                            onClick="${() => this.onPathSelectButtonClick('path')}">
                            Choose...
                        </button>
                        <label for="${type}_assignmentNameInput">Assignment name</label>
                        <input name="assignmentName"
                            id="${type}_assignmentNameInput"
                            defaultValue="${assignmentName}"
                            onInput="${this.onInputChange.bind(this)}"
                            minlength="1"
                            required/>
                        <label for="${type}_jplagLanguageInput" class="more-distance-top">JPlag language</label>
                        <select name="jplagLanguage"
                            id="${type}_jplagLanguageInput"
                            class="more-distance-top"
                            onChange="${this.onJPlagLanguageSelect.bind(this)}">
                            ${JPLAG_LANGUAGES.map(lang => html`
                                <option value="${lang}" selected="${jplagLanguage === lang}">${lang}</option>
                            `)}
                        </select>
                        <label for="${type}_jplagRepositoryInput" class="full">JPlag repository path</label>
                        <input name="jplagRepository"
                            id="${type}_jplagRepositoryInput"
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
                        <label for="${type}_sourceFilesInput" class="full more-distance-top">Source files (comma-seperated Java file names)</label>
                        <input name="sourceFiles"
                            id="${type}_sourceFilesInput"
                            defaultValue="${sourceFiles}"
                            onInput="${this.onInputChange.bind(this)}"
                            pattern="${SOURCE_FILES_REGEX}"/>
                        <label for="${type}_testsDirInput" class="full">Tests directory</label>
                        <input name="testsDir"
                            id="${type}_testsDirInput"
                            class="has-choose-button"
                            defaultValue="${testsDir}"
                            onInput="${this.onInputChange.bind(this)}"/>
                        <button type="button"
                            class="choose"
                            onClick="${() => this.onPathSelectButtonClick('testsDir')}">
                            Choose...
                        </button>
                        <label id="${type}_download-stage-label" class="more-distance-top">Download stage to use</label>
                        <div class="radio-option">
                            <input type="radio"
                                name="downloadStage"
                                value="moodle"
                                id="${type}_download-stage-scraper"
                                checked="${downloadStage === 'moodle'}"
                                onChange="${evt => this.onDownloadStageRadioChange(evt)}"/>
                            <label for="${type}_download-stage-scraper">
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
                                id="${type}_download-stage-bypass"
                                checked="${downloadStage === 'bypass'}"
                                onChange="${evt => this.onDownloadStageRadioChange(evt)}"/>
                            <label for="${type}_download-stage-bypass">
                                <strong>BypassDownloadStage</strong>
                                <div class="details">
                                    <small>Use this if MoodleScraperStage is broken.</small>
                                    <br/>
                                    Bypasses scraping Moodle and instead relies on manual data entry
                                </div>
                            </label>
                        </div>

                        ${downloadStage !== "moodle" ? '' : html`
							<label for="${type}_moodleAssignmentIDInput" class="more-distance-top">Moodle assignment ID (numeric)</label>
							<input name="moodleAssignmentID"
								id="${type}_moodleAssignmentIDInput"
								class="more-distance-top"
								defaultValue="${moodleAssignmentID}"
								onInput="${this.onInputChange.bind(this)}"
								pattern="${ASSIGNMENT_ID_REGEX}"
								inputmode="number"
								required/>
                            <label for="${type}_moodleBaseURLInput" class="wide">Moodle base URL</label>
                            <input type="url"
                                name="moodleBaseURL"
                                id="${type}_moodleBaseURLInput"
                                defaultValue="${moodleBaseURL}"
                                onInput="${this.onInputChange.bind(this)}"
                                required/>
                            <label for="${type}_moodleTeachersInput" class="full">Email addresses of Moodle teachers (comma-seperated)</label>
                            <small class="full">No mail will be sent to these addresses.</small>
                            <input name="moodleTeachers"
                                id="${type}_moodleTeachersInput"
                                type="email"
                                multiple
                                onInput="${this.onInputChange.bind(this)}"
                                defaultValue="${moodleTeachers}"/>

                            <div class="check-option more-distance-top">
                                <input type="checkbox"
                                    name="moodleUsernameEnabled"
                                    id="${type}_moodleUsernameEnabledInput"
                                    checked="${moodleUsernameEnabled}"
                                    onChange="${this.onFieldToggleChange.bind(this)}"/>
                                <label for="${type}_moodleUsernameEnabledInput" class="wide">Save Moodle username in config</label>
                            </div>
                            <label for="${type}_moodleUsernameInput" class="less-distance">Moodle username</label>
                            <input name="moodleUsername"
                                id="${type}_moodleUsernameInput"
                                class="less-distance"
                                disabled="${!moodleUsernameEnabled}"
                                defaultValue="${moodleUsername}"
                                onInput="${this.onInputChange.bind(this)}"
                                required="${moodleUsernameEnabled}"/>

                            <div class="check-option">
                                <input type="checkbox"
                                    name="moodlePasswordEnabled"
                                    id="${type}_moodlePasswordEnabledInput"
                                    checked="${moodlePasswordEnabled}"
                                    onChange="${this.onFieldToggleChange.bind(this)}"/>
                                <label for="${type}_moodlePasswordEnabledInput" class="wide">Save Moodle password in config</label>
                            </div>
                            <label for="${type}_moodlePasswordInput" class="less-distance">Moodle password</label>
                            <input type="password"
                                name="moodlePassword"
                                id="${type}_moodlePasswordInput"
                                class="less-distance"
                                disabled="${!moodlePasswordEnabled}"
                                defaultValue="${moodlePassword}"
                                onInput="${this.onInputChange.bind(this)}"
                                required="${moodlePasswordEnabled}"/>
                        `}

                        <div class="check-option more-distance-top">
                            <input type="checkbox"
                                name="emailStageEnabled"
                                id="${type}_emailStageEnabledInput"
                                checked="${emailStageEnabled}"
                                onChange="${this.onFieldToggleChange.bind(this)}"/>
                            <label for="${type}_emailStageEnabledInput" class="wide">Enable EMAIL stage</label>
                        </div>
                        <label for="${type}_smtpHostInput" class="">SMTP host</label>
                        <input name="smtpHost"
                            id="${type}_smtpHostInput"
                            defaultValue="${smtpHost}"
                            onInput="${this.onInputChange.bind(this)}"
                            disabled="${!emailStageEnabled}"
                            required="${emailStageEnabled}"/>
                        <label for="${type}_smtpPortInput">SMTP port</label>
                        <input type="number"
                            name="smtpPort"
                            id="${type}_smtpPortInput"
                            defaultValue="${smtpPort}"
                            onInput="${this.onInputChange.bind(this)}"
                            min="1"
                            max="65535"
                            disabled="${!emailStageEnabled}"
                            required="${emailStageEnabled}"/>
                        <label for="${type}_smtpProtocolInput">SMTP protocol</label>
                        <select name="smtpProtocol"
                            id="${type}_smtpProtocolInput"
                            onChange="${this.onInputChange.bind(this)}"
                            disabled="${!emailStageEnabled}">
                            ${["SMTP", "SMTPS", "SMTP_TLS"].map(p => html`
                                <option value="${p}" selected="${smtpProtocol === p}">${p}</option>
                            `)}
                        </select>

                        <div class="check-option more-distance-top">
                            <input type="checkbox"
                                name="smtpUsernameEnabled"
                                id="${type}_smtpUsernameEnabledInput"
                                checked="${smtpUsernameEnabled}"
                                onChange="${this.onFieldToggleChange.bind(this)}"
                                disabled="${!emailStageEnabled}"/>
                            <label for="${type}_smtpUsernameEnabledInput" class="wide">Save SMTP username in config</label>
                        </div>
                        <label for="${type}_smtpUsernameInput" class="less-distance">SMTP username</label>
                        <input name="smtpUsername"
                            id="${type}_smtpUsernameInput"
                            class="less-distance"
                            defaultValue="${smtpUsername}"
                            onInput="${this.onInputChange.bind(this)}"
                            disabled="${!emailStageEnabled || !smtpUsernameEnabled}"
                            required="${emailStageEnabled && smtpUsernameEnabled}"/>
                        <div class="check-option">
                            <input type="checkbox"
                                name="smtpPasswordEnabled"
                                id="${type}_smtpPasswordEnabledInput"
                                checked="${smtpPasswordEnabled}"
                                onChange="${this.onFieldToggleChange.bind(this)}"
                                disabled="${!emailStageEnabled}"/>
                            <label for="${type}_smtpPasswordEnabledInput" class="wide">Save SMTP password in config</label>
                        </div>
                        <label for="${type}_smtpPasswordInput" class="less-distance">SMTP password</label>
                        <input type="password"
                            name="smtpPassword"
                            id="${type}_smtpPasswordInput"
                            class="less-distance"
                            defaultValue="${smtpPassword}"
                            onInput="${this.onInputChange.bind(this)}"
                            disabled="${!emailStageEnabled || !smtpPasswordEnabled}"
                            required="${emailStageEnabled && smtpPasswordEnabled}"/>

                        <label for="${type}_smtpFromNameInput" class="more-distance-top">Email "From" Name</label>
                        <input name="smtpFromName"
                            id="${type}_smtpFromNameInput"
                            class="more-distance-top"
                            defaultValue="${smtpFromName}"
                            onInput="${this.onInputChange.bind(this)}"
                            disabled="${!emailStageEnabled}"
                            required="${emailStageEnabled}"/>
                        <label for="${type}_smtpFromAddressInput">Email "From" Address</label>
                        <input type="email"
                            name="smtpFromAddress"
                            id="${type}_smtpFromAddressInput"
                            defaultValue="${smtpFromAddress}"
                            onInput="${this.onInputChange.bind(this)}"
                            disabled="${!emailStageEnabled}"
                            required="${emailStageEnabled}"/>
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
