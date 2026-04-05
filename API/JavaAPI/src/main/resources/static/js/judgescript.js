const box = document.getElementById("uploadBox");
const upload = document.getElementById("fileUpload");
const submitBtn = document.querySelector(".submit-btn");
const downloadBtn = document.querySelector(".download-btn");
const reportForm = document.getElementById("reportForm");
const scoreArea = document.querySelector("#reportForm textarea[placeholder*='成绩汇总']");

let lastExtractedTexts = [];
let lastExtractedFileName = [];
let activeSSE = null;

// ===== 文件输入入口：支持点击、选择、拖拽三种方式，最终都走 sendFiles =====

// 点击上传
box.addEventListener("click", () => upload.click());

// 选择上传
upload.addEventListener("change", () => {
    if (upload.files.length > 0) {
        renderSelectedFiles(upload.files);
        sendFiles();
    }
});

// 拖拽上传
box.addEventListener("dragover", e => {
    e.preventDefault();
    box.classList.add("drag");
});

box.addEventListener("dragleave", () => {
    box.classList.remove("drag");
});

box.addEventListener("drop", e => {
    e.preventDefault();
    box.classList.remove("drag");
    upload.files = e.dataTransfer.files;

    if (upload.files.length > 0) {
        renderSelectedFiles(upload.files);
        sendFiles();
    }
});

submitBtn.addEventListener("click", async ev => {
    ev.preventDefault();

    if (!reportForm.reportValidity()) {
        return;
    }

    // 每次新任务都先清空展示区，避免与上一次结果混在一起
    scoreArea.value = "";
    disableBtn();

    const { payload, warns } = buildGeneratePayload();

    if (warns.length > 0) {
        alert("以下文件命名不符合规范，已跳过：" + warns.join("，"));
    }

    if (payload.extractedTexts.length === 0) {
        alert("没有可用于批改的有效文件，请先上传并提取文件。");
        enableBtn();
        return;
    }

    try {
        const json = await requestJson("/api/ai/generate/start", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        });

        const id = json && json.data ? json.data.id : null;
        if (!id) {
            throw new Error("未获取到任务ID");
        }

        openSSE(id);
    } catch (err) {
        console.error(err);
        alert("启动生成任务失败：" + toErrorMessage(err));
        enableBtn();
    }
});

// 下载按钮点击事件
downloadBtn.addEventListener("click", async () => {
    const text = scoreArea.value.trim();
    if (!text) {
        alert("请先执行【一键批改】生成成绩汇总");
        return;
    }

    const timeStamp = Date.now();
    try {
        const response = await fetch(`/api/generate/judgereport/${timeStamp}`, {
            method: "POST",
            headers: {
                "Content-Type": "text/plain;charset=UTF-8"
            },
            body: text
        });

        if (!response.ok) {
            const raw = await response.text();
            let backendMsg = raw;

            try {
                const parsed = raw ? JSON.parse(raw) : {};
                backendMsg = extractErrorMessage(parsed) || backendMsg;
            } catch {
                // keep plain-text backend message
            }

            throw new Error(backendMsg || `下载失败（${response.status}）`);
        }

        const blob = await response.blob();
        const a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = `judgereport_${timeStamp}.xlsx`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(a.href);
    } catch (err) {
        console.error(err);
        alert("下载失败：" + toErrorMessage(err));
    }
});

function renderSelectedFiles(files) {
    let names = "";
    for (const file of files) {
        names += `<span>${file.name};</span><br>`;
    }
    box.innerHTML = names;
}

async function sendFiles() {
    // 重新提取前先清空旧内容，保证 payload 与当前文件选择一致
    lastExtractedFileName = [];
    lastExtractedTexts = [];
    disable2Btn();
    submitBtn.textContent = "正在上传中…";

    const formData = new FormData();
    for (const file of upload.files) {
        formData.append("files", file);
    }

    try {
        const container = await requestJson("/api/fileextract/temp", {
            method: "POST",
            body: formData
        });

        const arr = container ? container.data : null;
        if (!Array.isArray(arr)) {
            throw new Error("返回格式错误");
        }

        const passed = arr.filter(item => item.success && item.content);
        lastExtractedTexts = passed.map(item => item.content);
        lastExtractedFileName = passed.map(item => item.fileName);
        enableBtn();
    } catch (err) {
        console.error(err);
        alert("提取失败：" + toErrorMessage(err));
        disable2Btn();
    }
}

function buildGeneratePayload() {
    // 仅将命名符合规范的文件放入批改任务，其余文件记录为 warns 给用户提示
    const payload = {
        extractedTexts: [],
        fileNames: [],
        counts: 0
    };

    const warns = [];
    for (let i = 0; i < lastExtractedTexts.length; i++) {
        const fileName = lastExtractedFileName[i];
        if (!isValidFileName(fileName)) {
            warns.push(fileName);
            continue;
        }

        payload.extractedTexts.push(lastExtractedTexts[i]);
        payload.fileNames.push(fileName);
    }

    payload.counts = payload.fileNames.length;
    return { payload, warns };
}

function isValidFileName(fileName) {
    return typeof fileName === "string" && fileName.split("_").length === 5;
}

async function requestJson(url, options) {
    const response = await fetch(url, options);
    const raw = await response.text();

    // 统一处理「JSON / 纯文本」两种返回，避免调用侧分散写解析逻辑
    let parsed;
    try {
        parsed = raw ? JSON.parse(raw) : {};
    } catch {
        parsed = raw;
    }

    if (!response.ok) {
        throw new Error(extractErrorMessage(parsed) || `请求失败（${response.status}）`);
    }

    if (typeof parsed === "string") {
        throw new Error(parsed || "返回格式错误");
    }

    return parsed;
}

function extractErrorMessage(payload) {
    if (!payload) {
        return "";
    }
    if (typeof payload === "string") {
        return payload;
    }
    if (typeof payload.msg === "string" && payload.msg) {
        return payload.msg;
    }
    if (typeof payload.message === "string" && payload.message) {
        return payload.message;
    }
    return "";
}

function toErrorMessage(err) {
    if (!err) {
        return "未知错误";
    }
    if (typeof err === "string") {
        return err;
    }
    if (err.message) {
        return err.message;
    }
    return String(err);
}

function openSSE(id) {
    // 若有历史流先关闭，保证同一时刻只保留一个活跃连接
    if (activeSSE) {
        activeSSE.close();
    }

    const evt = new EventSource(`/api/ai/generate/stream/${id}`);
    activeSSE = evt;

    const append = text => {
        scoreArea.value += text;
        scoreArea.scrollTop = scoreArea.scrollHeight;
    };

    const closeStream = () => {
        if (activeSSE) {
            activeSSE.close();
            activeSSE = null;
        }
        enableBtn();
    };

    // fileStart/message/done/error 四类事件都走统一追加与收尾逻辑
    evt.addEventListener("fileStart", e => {
        const data = parseSSEData(e);
        if (data && typeof data.index === "number" && typeof data.total === "number") {
            append(`========== 第 ${data.index + 1}/${data.total} 个文件 ==========\n`);
        }
    });

    evt.addEventListener("message", e => {
        const data = parseSSEData(e);
        append(typeof data === "string" ? data : JSON.stringify(data));
    });

    evt.addEventListener("done", () => {
        append("\n🎉 全部批改完成！");
        closeStream();
    });

    evt.addEventListener("error", e => {
        const data = parseSSEData(e);
        const msg = typeof data === "string" && data ? data : "连接中断，请稍后重试";
        append("\n❌ 服务器异常：" + msg);
        closeStream();
    });
}

function parseSSEData(event) {
    if (!event || !event.data) {
        return null;
    }

    // SSE 端可能回传 JSON 字符串，也可能是纯文本，这里做兼容解析
    try {
        const parsed = JSON.parse(event.data);
        if (parsed && Object.prototype.hasOwnProperty.call(parsed, "data")) {
            return parsed.data;
        }
        return parsed;
    } catch {
        return event.data;
    }
}

function disable2Btn() {
    submitBtn.disabled = true;
    submitBtn.textContent = "📄 请先选择文件";
    downloadBtn.disabled = true;
}

function disableBtn() {
    submitBtn.disabled = true;
    submitBtn.textContent = "📄 批改中…";
    downloadBtn.disabled = true;
}

function enableBtn() {
    submitBtn.disabled = false;
    submitBtn.textContent = "📄 一键批改";
    downloadBtn.disabled = false;
}

disable2Btn();
