export function initAiChat() {
    const modal = document.getElementById("aiChatModal");
    const openBtn = document.getElementById("aiChatBtn");
    const closeBtn = document.getElementById("closeAiChat");
    const sendBtn = document.getElementById("sendChatBtn");
    const input = document.getElementById("chatInput");
    const messages = document.getElementById("chatMessages");

    if (!openBtn || !modal) {
        console.warn("AI Chat elements not found");
        return;
    }

    modal.addEventListener("click", (e) => {
        if (e.target === modal) {
            modal.style.display = "none";
        }
    });


    openBtn.addEventListener("click", () => {
        modal.style.display = "flex";
        input?.focus();
    });

    closeBtn?.addEventListener("click", () => {
        modal.style.display = "none";
    });

    sendBtn?.addEventListener("click", sendMessage);
    input?.addEventListener("keydown", (e) => {
        if (e.key === "Enter") sendMessage();
    });

    async function sendMessage() {
        const text = input.value.trim();
        if (!text) return;

        appendMessage("user", text);
        input.value = "";

        appendMessage("bot", "Thinking…");

        try {
            const res = await fetch("/api/ai/chat", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ message: text })
            });

            const data = await res.json();
            removeLastBotMessage();
            appendMessage("bot", data.reply);
        } catch {
            removeLastBotMessage();
            appendMessage("bot", "Something went wrong.");
        }
    }

    function appendMessage(type, text) {
        const div = document.createElement("div");
        div.className = `chat-message ${type}`;
        div.textContent = text;
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
    }

    function removeLastBotMessage() {
        const bots = messages.querySelectorAll(".chat-message.bot");
        if (bots.length) bots[bots.length - 1].remove();
    }
}
