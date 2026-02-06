document.addEventListener("DOMContentLoaded", () => {
    const sendBtn = document.getElementById("sendBtn");
    const userInput = document.getElementById("userInput");
    const chatBody = document.getElementById("chatBody");

    function addMessage(text, sender) {
        const msg = document.createElement("div");
        msg.classList.add("msg", sender);

        const avatar = document.createElement("div");
        avatar.classList.add("avatar");
        avatar.textContent = sender === "user" ? "🧑" : "🤖";

        const bubble = document.createElement("div");
        bubble.classList.add("bubble");
        bubble.textContent = text;

        msg.appendChild(avatar);
        msg.appendChild(bubble);
        chatBody.appendChild(msg);
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    function sendMessage() {
        const text = userInput.value.trim();
        if (!text) return;
        addMessage(text, "user");
        userInput.value = "";

        // 模拟AI回复
        setTimeout(() => {
            addMessage("这是AI的回复：" + text, "ai");
        }, 800);
    }

    sendBtn.addEventListener("click", sendMessage);
    userInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) sendMessage();
    });
});
