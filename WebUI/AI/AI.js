const box = document.getElementById("uploadBox");
const upload = document.getElementById("fileUpload");

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
            alert("解析结果：\n" + result);
        }
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

    // 确保文件字段是从 <input id="fileUpload"> 中追加的多个文件
    formData.delete("fileUpload");
    for (let file of upload.files) formData.append("files", file);

    // 将“是否批改”信息加入到表单中（把 isCorrect 替换为你实际使用的字段名）
    const isCorrect = formData.get("isCorrect"); // <-- 如果字段名不是 isCorrect，请修改这里
    formData.set("proofread", isCorrect === "是" ? "yes" : "no");

    // 发送到服务器并获取字符串结果（假设返回 JSON: { result: "..." }）
    fetch("/api/fileextract/temp", {
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
        // 显示结果：如果页面有 #resultBox，则写入，否则打印到控制台
        const resultBox = document.getElementById("resultBox");
        if (resultBox) {
            resultBox.textContent = result;
        } else {
            console.log("server result:", result);
            alert("服务器返回结果：" + result);
        }
    })
    .catch(err => {
        console.error("上传或解析失败：", err);
        alert("上传失败：" + (err.message || err));
    });
});
// ...existing code...
