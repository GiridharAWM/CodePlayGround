/**
 * CODE VISUALIZATION PLAYGROUND - VISUALIZER ENGINE
 * Drives compilation, AJAX trace capture, step-by-step state visualization,
 * array memory inspection, call stack trees, condition evaluation, and editor line syncing.
 */

// State tracking
let currentEvents = [];
let currentStepIndex = -1;
let fullOutputText = "";
let isAutoPlaying = false;
let autoPlayInterval = null;
let playSpeed = 1000;

// Sample Programs (Requirement 26)
const SAMPLE_PROGRAMS = {
    sample1: `public class Main {
    public static void main(String[] args) {
        int a = 10;
        int b = 20;
        int sum = a + b;
        System.out.println(sum);
    }
}`,
    sample2: `public class Main {
    public static void main(String[] args) {
        int[] numbers = {10, 20, 30, 40, 50};

        for(int i = 0; i < numbers.length; i++) {
            System.out.println(numbers[i]);
        }
    }
}`,
    sample3: `public class Main {
    public static void main(String[] args) {
        int n = 5;

        if(n > 0) {
            System.out.println("Positive");
        } else {
            System.out.println("Negative");
        }
    }
}`,
    sample4: `public class Main {
    static int factorial(int n) {
        if(n <= 1) {
            return 1;
        }

        return n * factorial(n - 1);
    }

    public static void main(String[] args) {
        int result = factorial(5);
        System.out.println(result);
    }
}`,
    sample5: `public class Main {
    public static void main(String[] args) {
        int count = 1;
        int total = 0;

        while (count <= 4) {
            total = total + count;
            System.out.println("Count: " + count + ", Total: " + total);
            count++;
        }
    }
}`,
    sample6: `public class Main {
    static class Account {
        int balance;
        Account(int initial) {
            this.balance = initial;
        }
        void deposit(int amt) {
            balance += amt;
        }
    }

    public static void main(String[] args) {
        Account acc = new Account(100);
        acc.deposit(50);
        System.out.println("Balance: " + acc.balance);
    }
}`,
    sample7: `public class Main {
    public static void main(String[] args) {
        try {
            int a = 10;
            int b = 0;
            int result = a / b;
            System.out.println(result);
        } catch (ArithmeticException e) {
            System.out.println("Caught exception: " + e.getMessage());
        }
    }
}`
};

// DOM Content Loaded Handler
document.addEventListener('DOMContentLoaded', () => {
    setupEditorLineNumbers();
    setupEventListeners();
    checkUrlForProgramLoad();

    // Default code if empty
    const editor = document.getElementById('codeEditor');
    if (editor && !editor.value.trim()) {
        loadSample('sample1');
    }
});

// Setup Editor Line Numbers
function setupEditorLineNumbers() {
    const editor = document.getElementById('codeEditor');
    const gutter = document.getElementById('lineNumbersGutter');
    if (!editor || !gutter) return;

    function updateLines() {
        const lines = editor.value.split('\n').length;
        let html = '';
        for (let i = 1; i <= lines; i++) {
            html += `<div class="line-number" id="gutter-line-${i}">${i}</div>`;
        }
        gutter.innerHTML = html;
    }

    editor.addEventListener('input', updateLines);
    editor.addEventListener('scroll', () => {
        gutter.scrollTop = editor.scrollTop;
    });

    updateLines();
}

// Event Listeners Binding
function setupEventListeners() {
    const runBtn = document.getElementById('runButton');
    const stepBtn = document.getElementById('stepButton');
    const nextBtn = document.getElementById('nextButton');
    const prevBtn = document.getElementById('prevButton');
    const resetBtn = document.getElementById('resetButton');
    const autoBtn = document.getElementById('autoPlayButton');
    const speedSelect = document.getElementById('speedSelect');
    const sampleSelect = document.getElementById('sampleSelector');
    const saveBtn = document.getElementById('saveProgramButton');

    if (runBtn) runBtn.addEventListener('click', handleRunCode);
    if (stepBtn) stepBtn.addEventListener('click', stepForward);
    if (nextBtn) nextBtn.addEventListener('click', stepForward);
    if (prevBtn) prevBtn.addEventListener('click', stepBackward);
    if (resetBtn) resetBtn.addEventListener('click', resetExecution);
    if (autoBtn) autoBtn.addEventListener('click', toggleAutoPlay);
    if (speedSelect) {
        speedSelect.addEventListener('change', (e) => {
            playSpeed = parseInt(e.target.value);
            if (isAutoPlaying) {
                stopAutoPlay();
                startAutoPlay();
            }
        });
    }
    if (sampleSelect) {
        sampleSelect.addEventListener('change', (e) => {
            loadSample(e.target.value);
        });
    }
    if (saveBtn) {
        saveBtn.addEventListener('click', openSaveProgramModal);
    }
}

// Load Sample Program into Editor
function loadSample(sampleKey) {
    const editor = document.getElementById('codeEditor');
    if (editor && SAMPLE_PROGRAMS[sampleKey]) {
        editor.value = SAMPLE_PROGRAMS[sampleKey];
        setupEditorLineNumbers();
        resetExecution();
        const sampleSelect = document.getElementById('sampleSelector');
        if (sampleSelect) sampleSelect.value = sampleKey;
    }
}

// Run Code via AJAX (Requirement 4 & 22)
async function handleRunCode() {
    const editor = document.getElementById('codeEditor');
    const runBtn = document.getElementById('runButton');
    const outputPanel = document.getElementById('outputPanel');
    const code = editor.value.trim();

    if (!code) {
        showToast('Please enter Java source code to run.', 'error');
        return;
    }

    // Set UI to loading state
    runBtn.disabled = true;
    runBtn.innerHTML = `<span>⏳ Compiling &amp; Running...</span>`;
    outputPanel.innerHTML = `<span style="color: var(--text-muted);">&gt; Compiling program on server...</span>`;
    stopAutoPlay();

    try {
        const response = await fetch('run-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: new URLSearchParams({ code: code })
        });

        const data = await response.json();

        if (data.compilationError) {
            // Compilation error received
            outputPanel.innerHTML = `<span style="color: var(--accent-danger); font-weight: 700;">COMPILATION ERROR</span>\n------------------------------------------------\n${escapeHtml(data.compilationError)}\n------------------------------------------------`;
            currentEvents = [];
            currentStepIndex = -1;
            updateStepDisplay();
            showToast('Compilation failed! Check output terminal.', 'error');
            return;
        }

        if (!data.success) {
            outputPanel.innerHTML = `<span style="color: var(--accent-danger); font-weight: 700;">ERROR</span>\n------------------------------------------------\n${escapeHtml(data.error || 'Execution failed.')}\n------------------------------------------------`;
            currentEvents = [];
            currentStepIndex = -1;
            updateStepDisplay();
            showToast(data.error || 'Execution error.', 'error');
            return;
        }

        // Execution Succeeded
        fullOutputText = data.output || '';
        currentEvents = data.events || [];
        
        let termText = '';
        if (data.output) {
            termText += `<span style="color: var(--accent-cyan); font-weight: 700;">OUTPUT</span>\n------------------------------------------------\n${escapeHtml(data.output)}\n------------------------------------------------`;
        }
        if (data.runtimeError) {
            termText += `\n<span style="color: var(--accent-warning); font-weight: 700;">RUNTIME WARNING / ERROR</span>\n${escapeHtml(data.runtimeError)}`;
        }
        if (!termText) {
            termText = `<span style="color: var(--accent-success);">&gt; Program executed successfully with no console output.</span>`;
        }
        outputPanel.innerHTML = termText;

        if (currentEvents.length > 0) {
            currentStepIndex = 0;
            renderCurrentStep();
            showToast(`Execution trace loaded: ${currentEvents.length} events recorded.`, 'success');
        } else {
            currentStepIndex = -1;
            updateStepDisplay();
            showToast('Program executed, but no trace events captured.', 'info');
        }

    } catch (err) {
        console.error(err);
        outputPanel.innerHTML = `<span style="color: var(--accent-danger);">&gt; Network or Server Error: ${escapeHtml(err.message)}</span>`;
        showToast('Failed to connect to execution server.', 'error');
    } finally {
        runBtn.disabled = false;
        runBtn.innerHTML = `<span>▶ RUN</span>`;
    }
}

// Stepping Controls
function stepForward() {
    if (!currentEvents || currentEvents.length === 0) {
        showToast('Please click RUN first to generate execution trace.', 'info');
        return;
    }
    if (currentStepIndex < currentEvents.length - 1) {
        currentStepIndex++;
        renderCurrentStep();
    } else {
        showToast('Reached the end of execution trace.', 'info');
        stopAutoPlay();
    }
}

function stepBackward() {
    if (!currentEvents || currentEvents.length === 0) return;
    if (currentStepIndex > 0) {
        currentStepIndex--;
        renderCurrentStep();
    } else {
        showToast('Already at the first step.', 'info');
    }
}

function resetExecution() {
    stopAutoPlay();
    if (currentEvents.length > 0) {
        currentStepIndex = 0;
        renderCurrentStep();
    } else {
        currentStepIndex = -1;
        updateStepDisplay();
        clearVisualizationBoard();
    }
}

function toggleAutoPlay() {
    if (!currentEvents || currentEvents.length === 0) {
        showToast('Please click RUN first to generate trace.', 'info');
        return;
    }
    if (isAutoPlaying) {
        stopAutoPlay();
    } else {
        startAutoPlay();
    }
}

function startAutoPlay() {
    isAutoPlaying = true;
    const autoBtn = document.getElementById('autoPlayButton');
    if (autoBtn) autoBtn.innerHTML = `<span>⏸ PAUSE</span>`;
    
    autoPlayInterval = setInterval(() => {
        if (currentStepIndex < currentEvents.length - 1) {
            currentStepIndex++;
            renderCurrentStep();
        } else {
            stopAutoPlay();
        }
    }, playSpeed);
}

function stopAutoPlay() {
    isAutoPlaying = false;
    if (autoPlayInterval) {
        clearInterval(autoPlayInterval);
        autoPlayInterval = null;
    }
    const autoBtn = document.getElementById('autoPlayButton');
    if (autoBtn) autoBtn.innerHTML = `<span>▶ AUTO</span>`;
}

// Render Current Step (State Reconstitution)
function renderCurrentStep() {
    if (currentStepIndex < 0 || currentStepIndex >= currentEvents.length) return;

    const currentEvent = currentEvents[currentStepIndex];
    updateStepDisplay();
    highlightSourceLine(currentEvent.line);
    renderChronicle(currentEvent);

    // Reconstruct state up to current step
    const state = reconstructStateUpTo(currentStepIndex);

    renderVariablesPanel(state.variables, currentEvent);
    renderArraysPanel(state.arrays, currentEvent);
    renderCallStackPanel(state.callStack, currentEvent);
    renderConditionPanel(currentEvent);
    renderLoopPanel(currentEvent);
    renderTerminalOutputUpTo(state.outputLines);
}

// Reconstruct execution state up to given step index
function reconstructStateUpTo(stepIndex) {
    const variables = {};
    const arrays = {};
    const callStack = [{ name: 'main()', depth: 0 }];
    const outputLines = [];

    for (let i = 0; i <= stepIndex; i++) {
        const ev = currentEvents[i];

        if (ev.type === 'VARIABLE' || ev.type === 'VARIABLE_CHANGE') {
            variables[ev.name] = {
                name: ev.name,
                value: ev.value,
                prevValue: ev.prevValue,
                isRecent: (i === stepIndex)
            };
        } else if (ev.type === 'ARRAY_CREATE' || ev.type === 'ARRAY_CHANGE') {
            try {
                // Parse array string representation [10, 20, 30]
                const clean = ev.value.replace(/\[|\]/g, '').trim();
                const elements = clean ? clean.split(',').map(s => s.trim()) : [];
                arrays[ev.name] = {
                    name: ev.name,
                    elements: elements,
                    accessedIndex: (ev.index !== undefined ? ev.index : null)
                };
            } catch (e) {}
        } else if (ev.type === 'ARRAY_ACCESS') {
            if (arrays[ev.name]) {
                arrays[ev.name].accessedIndex = ev.index;
            }
        } else if (ev.type === 'METHOD_ENTRY') {
            callStack.push({
                name: `${ev.name}(${ev.value || ''})`,
                depth: ev.callDepth || callStack.length
            });
        } else if (ev.type === 'METHOD_EXIT') {
            if (callStack.length > 1) {
                callStack.pop();
            }
        } else if (ev.type === 'OUTPUT') {
            outputLines.push(ev.value);
        }
    }

    return { variables, arrays, callStack, outputLines };
}

// UI Updating Functions
function updateStepDisplay() {
    const tracker = document.getElementById('stepTracker');
    if (tracker) {
        if (currentEvents.length === 0) {
            tracker.textContent = 'Step: 0 / 0';
        } else {
            tracker.textContent = `Current Step: ${currentStepIndex + 1} / ${currentEvents.length}`;
        }
    }
}

function highlightSourceLine(lineNum) {
    // Reset all gutter highlights
    document.querySelectorAll('.line-number').forEach(el => el.classList.remove('active-line'));
    if (lineNum && lineNum > 0) {
        const lineEl = document.getElementById(`gutter-line-${lineNum}`);
        if (lineEl) {
            lineEl.classList.add('active-line');
            lineEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }
}

function renderChronicle(ev) {
    const chronicleContainer = document.getElementById('chronicleContainer');
    if (!chronicleContainer) return;

    let icon = '⚡';
    if (ev.type === 'PROGRAM_START') icon = '🚀';
    if (ev.type === 'PROGRAM_END') icon = '🏁';
    if (ev.type === 'VARIABLE' || ev.type === 'VARIABLE_CHANGE') icon = '📦';
    if (ev.type === 'ARRAY_CREATE' || ev.type === 'ARRAY_ACCESS') icon = '📊';
    if (ev.type === 'CONDITION') icon = '🔀';
    if (ev.type === 'LOOP_ITERATION') icon = '🔁';
    if (ev.type === 'METHOD_ENTRY' || ev.type === 'METHOD_EXIT') icon = '📞';
    if (ev.type === 'EXCEPTION') icon = '⚠️';

    chronicleContainer.innerHTML = `
        <div class="chronicle-card">
            <span class="chronicle-step">Step ${ev.step}</span>
            <span style="font-size: 1.1rem;">${icon}</span>
            <span class="chronicle-text">${escapeHtml(ev.description || 'Executing instruction')}</span>
        </div>
    `;
}

function renderVariablesPanel(variables, currentEvent) {
    const varContainer = document.getElementById('variablesContainer');
    if (!varContainer) return;

    const varKeys = Object.keys(variables);
    if (varKeys.length === 0) {
        varContainer.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">No active local variables.</span>`;
        return;
    }

    let rows = '';
    varKeys.forEach(k => {
        const v = variables[k];
        const isCurrent = (currentEvent.name === k);
        const highlightClass = isCurrent ? 'var-highlight' : '';
        rows += `
            <tr class="${highlightClass}">
                <td class="var-name">${escapeHtml(v.name)}</td>
                <td class="var-arrow">→</td>
                <td class="var-val">${escapeHtml(v.value)}</td>
            </tr>
        `;
    });

    varContainer.innerHTML = `
        <table class="var-table">
            <thead>
                <tr><th>Variable</th><th></th><th>Value</th></tr>
            </thead>
            <tbody>${rows}</tbody>
        </table>
    `;
}

function renderArraysPanel(arrays, currentEvent) {
    const container = document.getElementById('arraysContainer');
    if (!container) return;

    const arrKeys = Object.keys(arrays);
    if (arrKeys.length === 0) {
        container.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">No active arrays in scope.</span>`;
        return;
    }

    let html = '';
    arrKeys.forEach(k => {
        const arr = arrays[k];
        let indexCells = '';
        let valueBoxes = '';

        arr.elements.forEach((elem, idx) => {
            const isAccessed = (currentEvent.name === k && currentEvent.index === idx);
            const accessClass = isAccessed ? 'accessed' : '';
            indexCells += `<div class="array-index-cell">${idx}</div>`;
            valueBoxes += `<div class="array-box ${accessClass}">${escapeHtml(elem)}</div>`;
        });

        html += `
            <div style="margin-bottom: 0.75rem;">
                <div style="font-size: 0.8rem; font-weight: 600; color: #93c5fd; margin-bottom: 0.25rem;">
                    Array: ${escapeHtml(arr.name)} (length: ${arr.elements.length})
                </div>
                <div class="array-container">
                    <div class="array-indices">${indexCells}</div>
                    <div class="array-boxes">${valueBoxes}</div>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

function renderCallStackPanel(callStack, currentEvent) {
    const container = document.getElementById('callStackContainer');
    if (!container) return;

    let framesHtml = '';
    callStack.forEach((frame, idx) => {
        const isActive = (idx === callStack.length - 1);
        const activeClass = isActive ? 'active' : '';
        framesHtml += `
            <div class="stack-frame ${activeClass}">
                <span>${'— '.repeat(idx)} ${escapeHtml(frame.name)}</span>
                <span class="badge ${isActive ? 'badge-info' : 'badge-secondary'}">Frame ${idx}</span>
            </div>
        `;
    });

    container.innerHTML = `<div class="stack-frames">${framesHtml}</div>`;
}

function renderConditionPanel(currentEvent) {
    const container = document.getElementById('conditionContainer');
    if (!container) return;

    if (currentEvent.type === 'CONDITION') {
        const isTrue = (currentEvent.value === 'TRUE');
        const tagClass = isTrue ? 'condition-tag-true' : 'condition-tag-false';
        container.innerHTML = `
            <div class="condition-view">
                <span class="condition-expr">Condition: <strong>${escapeHtml(currentEvent.name)}</strong></span>
                <span class="${tagClass}">Result: ${escapeHtml(currentEvent.value)}</span>
            </div>
        `;
    } else {
        container.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">No active condition evaluated at this step.</span>`;
    }
}

function renderLoopPanel(currentEvent) {
    const container = document.getElementById('loopContainer');
    if (!container) return;

    if (currentEvent.type === 'LOOP_ITERATION') {
        container.innerHTML = `
            <div style="display: flex; align-items: center; justify-content: space-between; background: var(--bg-input); padding: 0.5rem 0.85rem; border-radius: var(--radius-sm); font-family: var(--font-mono); font-size: 0.85rem;">
                <span style="color: var(--accent-cyan); font-weight: 600;">Loop (${escapeHtml(currentEvent.name)})</span>
                <span style="color: #a7f3d0;">${escapeHtml(currentEvent.value || 'Active')}</span>
            </div>
        `;
    } else {
        container.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">No active loop iteration at this step.</span>`;
    }
}

function renderTerminalOutputUpTo(outputLines) {
    const terminalBody = document.getElementById('outputPanel');
    if (!terminalBody) return;

    let out = `<span style="color: var(--accent-cyan); font-weight: 700;">TERMINAL OUTPUT</span>\n------------------------------------------------\n`;
    if (fullOutputText) {
        out += `${escapeHtml(fullOutputText)}\n`;
    } else if (outputLines && outputLines.length > 0) {
        outputLines.forEach(l => {
            out += `${escapeHtml(l)}\n`;
        });
    } else {
        out += `> Console output buffer empty.\n`;
    }
    out += `------------------------------------------------`;
    terminalBody.innerHTML = out;
}

function clearVisualizationBoard() {
    const chronicle = document.getElementById('chronicleContainer');
    const vars = document.getElementById('variablesContainer');
    const arrs = document.getElementById('arraysContainer');
    const stack = document.getElementById('callStackContainer');
    const cond = document.getElementById('conditionContainer');
    const loop = document.getElementById('loopContainer');

    if (chronicle) chronicle.innerHTML = `<div class="chronicle-card"><span class="chronicle-step">Ready</span><span class="chronicle-text">Click RUN to compile and visualize.</span></div>`;
    if (vars) vars.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>`;
    if (arrs) arrs.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>`;
    if (stack) stack.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>`;
    if (cond) cond.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>`;
    if (loop) loop.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">Waiting for execution...</span>`;
    
    document.querySelectorAll('.line-number').forEach(el => el.classList.remove('active-line'));
}

// Save Program AJAX (Requirement 15)
function openSaveProgramModal() {
    const modal = document.getElementById('saveModal');
    if (modal) modal.style.display = 'flex';
}

function closeSaveProgramModal() {
    const modal = document.getElementById('saveModal');
    if (modal) modal.style.display = 'none';
}

async function submitSaveProgram() {
    const nameInput = document.getElementById('saveProgramName');
    const editor = document.getElementById('codeEditor');
    const progName = nameInput ? nameInput.value.trim() : 'My Java Program';
    const code = editor ? editor.value : '';

    if (!code.trim()) {
        showToast('Cannot save empty program.', 'error');
        return;
    }

    try {
        const resp = await fetch('save-program', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ program_name: progName, code: code })
        });
        const res = await resp.json();
        if (res.success) {
            showToast('Program saved successfully!', 'success');
            closeSaveProgramModal();
        } else {
            showToast(res.error || 'Failed to save program.', 'error');
        }
    } catch (e) {
        showToast('Network error while saving program.', 'error');
    }
}

// Load Program from URL Query param (e.g. ?loadId=1)
async function checkUrlForProgramLoad() {
    const urlParams = new URLSearchParams(window.location.search);
    const loadId = urlParams.get('loadId');
    if (!loadId) return;

    try {
        const resp = await fetch(`load-program?id=${encodeURIComponent(loadId)}`);
        const res = await resp.json();
        if (res.success && res.code) {
            const editor = document.getElementById('codeEditor');
            if (editor) {
                editor.value = res.code;
                setupEditorLineNumbers();
                showToast(`Loaded "${res.name}" successfully!`, 'success');
            }
        } else {
            showToast(res.error || 'Failed to load requested program.', 'error');
        }
    } catch (e) {
        console.error(e);
    }
}

// Utility: HTML Escaping
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
