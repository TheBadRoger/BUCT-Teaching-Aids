const box = document.getElementById("uploadBox");
const upload = document.getElementById("fileUpload");
const submitBtn = document.querySelector('.submit-btn');

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

/*****************************************************************
 *  0. 全局变量：保存最近一次提取到的多文件内容
 *****************************************************************/
let lastExtractedTexts = [];   // 每一项就是单个文件的纯文本
let lastExtractedFileName = [];
/*****************************************************************
 *  1. 文件上传 / 提取（沿用你原来代码，只把结果存到全局）
 *****************************************************************/
function sendFiles() {
  if (upload.files.length === 0) {
    alert("请先选择文件");
    return;
  }
  const formData = new FormData();
  for (const file of upload.files) formData.append("files", file);

  fetch("http://10.15.2.38:4444/api/fileextract/temp", {
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

    lastExtractedFileName=arr
        .filter(f => f.success && f.content)
        .map(f => f.fileName);
    
    console.log("已提取", lastExtractedTexts.length, "个文件");
  })
  .catch(err => {
    console.error(err);
    alert("提取失败：" + err);
  });
}

/* 选择/拖拽后自动提取 */
upload.addEventListener("change", () => sendFiles());
document.getElementById("uploadBox")
        .addEventListener("drop", () => sendFiles());

/*****************************************************************
 *  2. 点击【一键生成】按钮：先提取（若还没提取过）再调 /generate/start
 *****************************************************************/
document.querySelector(".submit-btn").addEventListener("click", ev => {
  ev.preventDefault();          // 阻止表单的默认提交
  document.querySelector("#reportForm textarea[placeholder*='成绩']").value="";
  disableBtn();
  // 如果还没提取过，先补提取
  if (lastExtractedTexts.length === 0) {
    sendFiles();
    if (lastExtractedTexts.length === 0) return; // 提取失败就不继续
  }

  // 收集表单其他字段
  const form = document.getElementById("reportForm");
  const fd   = new FormData(form);
  const params = new URLSearchParams();
  fd.forEach((v, k) => params.append(k, v));

  // 把多文件内容追加为 extractedTexts 数组
  lastExtractedTexts.forEach(t => params.append("extractedTexts", t));
  lastExtractedFileName.forEach(t => params.append("fileNames", t));

  // POST /generate/start 拿到任务 id
  fetch("http://10.15.2.38:4444/api/ai/generate/start", {
    method: "POST",
    body: params
  })
    .then(r => r.ok ? r.json() : r.text().then(Promise.reject))
    .then(json => {
      const { id } = json;
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
  const evt = new EventSource(`http://10.15.2.38:4444/api/ai/generate/stream/${id}`);
  const scoreArea = document.querySelector("#reportForm textarea[placeholder*='成绩']");

  /* 工具：追加文本并自动滚动 */
  const append = txt => {
    scoreArea.value += txt;
    scoreArea.scrollTop = scoreArea.scrollHeight;
  };

  evt.addEventListener('done',  e => { evt.close(); enableBtn(); });
  evt.addEventListener('error', e => { evt.close(); enableBtn(); });

  evt.addEventListener("fileStart", e => {
    const { index, total } = JSON.parse(e.data);
    append(`\n========== 第 ${index + 1}/${total} 个文件 ==========\n`);
  });
  evt.addEventListener("message", e => append(e.data));
  evt.addEventListener("fileEnd", e => {
    const { index, total } = JSON.parse(e.data);
    append(`\n---------- 第 ${index + 1} 个文件结束 ----------\n`);
  });
  evt.addEventListener("done", e => {
    append("\n🎉 全部批改完成！");
    
    evt.close();
  });
  evt.addEventListener("error", e => {
    append("\n❌ 服务器异常：" + (e.data || ""));
    evt.close();
  });
}

function disableBtn() {
  submitBtn.disabled = true;
  submitBtn.textContent = '📄 批改中…';
}

function enableBtn() {
  submitBtn.disabled = false;
  submitBtn.textContent = '📄 一键批改';
}