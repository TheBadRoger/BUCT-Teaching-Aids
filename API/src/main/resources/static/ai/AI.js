console.log("AI.js loaded (version: 20251128-1)");
const box = document.getElementById("uploadBox");
const upload = document.getElementById("fileUpload");

// 存放文件解析后的文本（会随表单一起提交）
let extractedText = "";

// 点击上传
box.addEventListener("click", () => upload.click());

// 选择上传
upload.addEventListener("change", () => {
    if (upload.files.length > 0) {
        let names = "";
        for (let file of upload.files) names += `<span>${file.name}</span><br>`;
        box.innerHTML = names;
        // 文件选择后立即发送到服务器
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

    let names = "";
    for (let file of upload.files) names += `<span>${file.name}</span><br>`;
    box.innerHTML = names;
    
    // 拖拽上传后立即发送到服务器
    sendFiles();
});

// ...existing code...
// 定义文件发送函数
function sendFiles() {
    if (upload.files.length === 0) {
        alert("请先选择文件");
        return;
    }
    
    var formData = new FormData();
    for (let file of upload.files) {
        formData.append("files", file);
    }

    console.log("正在上传文件到服务器...");
    
    fetch("http://127.0.0.1:8081/api/fileextract/temp", {
        method: "POST",
        body: formData
    })
    .then(async res => {
        if (!res.ok) {
            const text = await res.text();
            throw new Error(text || `HTTP ${res.status}`);
        }
        return res.json();
    })
    .then(data => {
        const result = (typeof data === "object" && data !== null) ? (data.result ?? JSON.stringify(data)) : String(data);
        const resultBox = document.getElementById("resultBox");
        if (resultBox) {
            resultBox.textContent = result;
        } else {
            console.log("server result:", result);
            // 不再弹窗提示上传成功或解析结果，避免打断用户操作
        }
        // 保存解析文本至全局变量，并同步到隐藏字段，以便后续“一键生成”提交时一并发送
        extractedText = result;
        const hidden = document.getElementById("extractedText");
        if (hidden) hidden.value = result;
    })
    .catch(err => {
        console.error("上传或解析失败：", err);
        alert("上传失败：" + (err.message || err));
    });
}

// 定义自动提交函数
function autoSubmitForm() {
    var rpform = document.getElementById("reportForm");
    // 检查是否填写了必填项
    var selects = rpform.querySelectorAll("select[required]");
    var textareas = rpform.querySelectorAll("textarea[required]");
    
    let allFilled = true;
    for (let select of selects) {
        if (!select.value) {
            allFilled = false;
            break;
        }
    }
    for (let textarea of textareas) {
        if (!textarea.value) {
            allFilled = false;
            break;
        }
    }
    
    // 如果必填项都填了，自动提交
    if (allFilled) {
        rpform.dispatchEvent(new Event("submit"));
    }
}

// 提交（合并并替换原有重复的 submit 处理器）
var rpform = document.getElementById("reportForm");
rpform.addEventListener("submit", e => {
    e.preventDefault();
    var formData = new FormData(rpform);

    // 文件已在上传时转换为文本并通过 extractedText 发送，这里不再附加文件

    // 将“是否批改”信息加入到表单中（把 isCorrect 替换为你实际使用的字段名）
    const isCorrect = formData.get("isCorrect"); // <-- 如果字段名不是 isCorrect，请修改这里
    formData.set("proofread", isCorrect === "是" ? "yes" : "no");

    // 把文件解析得到的文本一并加入提交参数（必填或非必填都可）
    formData.set("extractedText", extractedText || "");

    // 发送到服务器启动生成任务并订阅 SSE
    const startUrl = "http://127.0.0.1:8081/api/ai/generate/start";
    console.log("提交到生成启动接口：", startUrl);
    // 打印表单字段摘要（避免在控制台泄露过多文本）
    try {
        const entries = [];
        for (let pair of formData.entries()) {
            // 只显示字段名与前 100 字符的值
            const key = pair[0];
            let val = pair[1];
            if (typeof val === 'string') val = val.slice(0, 100);
            else val = Object.prototype.toString.call(val);
            entries.push(key + "=" + val);
        }
        console.log("FormData 摘要:", entries.join(", "));
    } catch (e) {
        console.log("无法读取 FormData 摘要", e);
    }

    // POST 启动任务，返回的 JSON 应包含 { id: "..." }
    fetch(startUrl, {
        method: "POST",
        body: formData
    })
    .then(async res => {
        if (!res.ok) {
            const text = await res.text();
            throw new Error(text || `HTTP ${res.status}`);
        }
        return res.json();
    })
    .then(data => {
        const id = (data && data.id) ? data.id : null;
        if (!id) throw new Error("无法获取生成任务 id");

        const resultBox = document.getElementById("resultBox");
        if (resultBox) {
            resultBox.textContent = "正在生成（实时流）...\n";
        }

        // 使用 EventSource 订阅后端转发的 SSE
        const esUrl = `http://127.0.0.1:8081/api/ai/generate/stream/${id}`;
        console.log("建立 SSE 连接：", esUrl);
        const es = new EventSource(esUrl);

        // 打字机缓冲与状态
        let typeBuffer = "";
        let isTyping = false;
        let doneReceived = false; // 是否收到后端的 done 事件
        const typingSpeed = 20; // 毫秒/字符，可根据需要调整

        function startTyping() {
            if (isTyping) return;
            isTyping = true;
            (function tick() {
                if (!resultBox) { isTyping = false; return; }
                if (typeBuffer.length === 0) {
                    isTyping = false;
                    // 如果已经收到 done 信号且缓冲已空，则显示完成标记并关闭连接
                    if (doneReceived) {
                        try {
                            resultBox.textContent += "\n【生成完成】";
                        } catch (e) { /* ignore */ }
                        try { es.close(); } catch (e) { /* ignore */ }
                    }
                    return;
                }
                const ch = typeBuffer.charAt(0);
                resultBox.textContent += ch;
                typeBuffer = typeBuffer.slice(1);
                setTimeout(tick, typingSpeed);
            })();
        }

        es.onmessage = function(evt) {
            if (!resultBox) return;
            const raw = String(evt.data || "");

            // 尝试解析 JSON 并提取所有 text 字段（支持嵌套），其他字段忽略
            let appended = "";
            try {
                const parsed = JSON.parse(raw);
                function findText(node) {
                    let out = "";
                    if (node == null) return "";
                    if (typeof node === 'string') return node;
                    if (Array.isArray(node)) {
                        for (const item of node) out += findText(item);
                        return out;
                    }
                    if (typeof node === 'object') {
                        for (const k in node) {
                            if (!Object.prototype.hasOwnProperty.call(node, k)) continue;
                            if (k === 'text' && typeof node[k] === 'string') {
                                out += node[k];
                            } else {
                                out += findText(node[k]);
                            }
                        }
                        return out;
                    }
                    return "";
                }
                appended = findText(parsed);
            } catch (e) {
                // 如果不是合法 JSON，尝试用正则抽取 text 字段（容错 polyMas 非标准分段）
                try {
                    const re = /"text"\s*:\s*"([\\\s\S]*?)"/g;
                    let m;
                    while ((m = re.exec(raw)) !== null) {
                        try { appended += JSON.parse('"' + m[1].replace(/"/g, '\\"') + '"'); } catch (e2) { appended += m[1]; }
                    }
                } catch (e2) {
                    appended = "";
                }
            }

            if (appended) {
                // 将要显示的文本放入缓冲区，由打字机逐字符输出
                typeBuffer += appended;
                startTyping();
            }
        };

        es.addEventListener('message', function(evt) {
            // 有些浏览器触发 onmessage 即此处也会被调用，保持兼容
        });

        es.addEventListener('done', function(evt) {
            // 标记已完成，实际显示在缓冲清空后由打字机逻辑追加，避免与中间数据混入
            doneReceived = true;
        });

        es.addEventListener('error', function(evt) {
            console.error('SSE error', evt);
            if (resultBox) resultBox.textContent += "\n[连接或生成出错]";
            es.close();
        });

    })
    .catch(err => {
        console.error("启动生成任务失败：", err);
        alert("生成失败：" + (err.message || err));
    });
});
// ...existing code...
