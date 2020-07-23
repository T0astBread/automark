import { h, Component, render, createRef } from "/libs/preact-10.4.6.min.js"
import htm from "/libs/htm-3.0.4.min.js"

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


const getHash = () => location.hash.substr(1)

const hashIsNew = () => getHash() === "new"
const hashIsEdit = () => getHash() === "edit"


// Initialize htm with Preact
const html = htm.bind(h)

export {
    Component,
    createRef,
    render,
    html,
    STAGES,
    STAGES_REVERSE,
    PROBLEM_TYPES,
    stageFromName,
    stageFromHash,
    getHash,
    hashIsNew,
    hashIsEdit,
}
