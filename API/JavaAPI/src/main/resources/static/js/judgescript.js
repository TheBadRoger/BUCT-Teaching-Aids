const box = document.getElementById("uploadBox");
const upload = document.getElementById("fileUpload");
const submitBtn = document.querySelector('.submit-btn');
const downloadBtn = document.querySelector('.download-btn');
let lastExtractedTexts = [];   // 每一项就是单个文件的纯文本
let lastExtractedFileName = [];

// 点击上传
box.addEventListener("click", () => upload.click());

// 选择上传
upload.addEventListener("change", () => {
    if (upload.files.length > 0) {
        let names = "";
        for (let file of upload.files) names += `<span>${file.name};</span><br>`;
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
    for (let file of upload.files) names += `<span>${file.name};</span><br>`;
    box.innerHTML = names;

    // 拖拽上传后立即发送到服务器
    sendFiles();
});

/*****************************************************************
 *  0. 全局变量：保存最近一次提取到的多文件内容
 *****************************************************************/

/*****************************************************************
 *  1. 文件上传 / 提取（沿用你原来代码，只把结果存到全局）
 *****************************************************************/
function sendFiles() {
    lastExtractedFileName = [];
    lastExtractedTexts = [];
    disable2Btn();
    submitBtn.textContent = "正在上传中…";
    const formData = new FormData();
    for (const file of upload.files) formData.append("files", file);

    fetch("/api/fileextract/temp", {
        method: "POST",
        body: formData
    })
        .then(r => r.ok ? r.json() : r.text().then(Promise.reject))
        .then(container => {
            // 1. 取数组
            const arr = container.data;   // ← 关键
            if (!Array.isArray(arr)) throw "返回格式错误";

            // 2. 只保留成功且非空的内容（也可按需保留失败提示）
            lastExtractedTexts = arr
                .filter(f => f.success && f.content)
                .map(f => f.content);

            lastExtractedFileName = arr
                .filter(f => f.success && f.content)
                .map(f => f.fileName);

            //alert("已提取 " + lastExtractedTexts.length + " 个文件");
            enableBtn();
        })
        .catch(err => {
            console.error(err);
            alert("提取失败：" + err);
        });
}

/*****************************************************************
 *  2. 点击【一键生成】按钮：先提取（若还没提取过）再调 /generate/start
 *****************************************************************/
document.querySelector(".submit-btn").addEventListener("click", ev => {
    ev.preventDefault();

    const form = document.getElementById("reportForm");
    if (!form.reportValidity()) {   // 手动触发校验，不通过就 return
        return;
    }

    document.querySelector("#reportForm textarea[placeholder*='成绩汇总']").value = "";
    disableBtn();

    const fd = new FormData(form);
    const params = new URLSearchParams();
    fd.forEach((v, k) => params.append(k, v));

    // 把多文件内容追加为 extractedTexts 数组
    let warns = [];
    for (let i = 0; i < lastExtractedTexts.length; i++) {
        if (lastExtractedFileName[i].split('_').length !== 5) {
            warns.push(lastExtractedFileName[i]);
        } else {
            params.append("extractedTexts", lastExtractedTexts[i]);
            params.append("fileNames", lastExtractedFileName[i]);
        }
    }

    params.append("counts", lastExtractedFileName.length.toString());

    if (warns.length > 0) {
        alert("以下文件命名不符合规范，已跳过：" + warns.join('，'));
    }

    // POST /generate/start 拿到任务 id
    fetch("/api/ai/generate/start", {
        method: "POST",
        body: params
    })
        .then(r => r.ok ? r.json() : r.text().then(Promise.reject))
        .then(json => {
            const {id} = json;
            openSSE(id);          // 建立 SSE 接收流
        })
        .catch(err => {
            console.error(err);
            alert("启动生成任务失败：" + err);
            enableBtn();

        });
});

/*****************************************************************
 *  3. SSE 接收流，并把 AI 返回写到「学生成绩」文本框
 *****************************************************************/
function openSSE(id) {
    const evt = new EventSource(`/api/ai/generate/stream/${id}`);
    const scoreArea = document.querySelector("#reportForm textarea[placeholder*='成绩汇总']");

    /* 工具：追加文本并自动滚动 */
    const append = txt => {
        scoreArea.value += txt;
        scoreArea.scrollTop = scoreArea.scrollHeight;
    };

    evt.addEventListener('done', e => {
        evt.close();
        enableBtn();
    });
    evt.addEventListener('error', e => {
        evt.close();
        enableBtn();
    });

    evt.addEventListener("fileStart", e => {
        const {index, total} = JSON.parse(e.data).data;
        append(`========== 第 ${index + 1}/${total} 个文件 ==========\n`);
    });
    evt.addEventListener("message", e => append(JSON.parse(e.data).data));
    evt.addEventListener("done", e => {
        append("\n🎉 全部批改完成！");

        evt.close();
    });
    evt.addEventListener("error", e => {
        append("\n❌ 服务器异常：" + (JSON.parse(e.data).data || ""));
        evt.close();
    });
}

function disable2Btn() {
    submitBtn.disabled = true;
    submitBtn.textContent = '📄 请先选择文件';
    downloadBtn.disabled = true;
}

function disableBtn() {
    submitBtn.disabled = true;
    submitBtn.textContent = '📄 批改中…';
    downloadBtn.disabled = true;
}

function enableBtn() {
    submitBtn.disabled = false;
    submitBtn.textContent = '📄 一键批改';
    downloadBtn.disabled = false;
}

// 下载按钮点击事件
downloadBtn.addEventListener("click", () => {
    // 下载按钮点击事件（保持不变）
    downloadBtn.addEventListener("click", () => {
        const scoreArea = document.querySelector("#reportForm textarea[placeholder*='成绩汇总']");
        const text = scoreArea.value.trim();

        if (!text) {
            alert('请先执行【一键批改】生成成绩汇总');
            return;
        }

        const timeStamp = Date.now();
        window.location.href =
            `/api/generate/judgereport/${timeStamp}?text=${encodeURIComponent(text)}`;
    });
});