const API_BASE = "http://localhost:8080";

// Element References
const fileInput = document.getElementById("fileInput");
const fileLabel = document.getElementById("fileLabel");
const fileNameDisplay = document.getElementById("file-name-display");
const expirySelect = document.getElementById("expirySelect");
const passwordToggle = document.getElementById("passwordToggle");
const passwordInput = document.getElementById("passwordInput");
const uploadBtn = document.getElementById("uploadBtn");
const errorMsg = document.getElementById("errorMsg");
const closeBtn = document.getElementById("closeBtn");

const uploadSection = document.getElementById("upload-section");
const loadingSection = document.getElementById("loading-section");
const resultSection = document.getElementById("result-section");

const qrContainer = document.getElementById("qrcode");
const expiryLabel = document.getElementById("expiry-label");
const linkInput = document.getElementById("linkInput");
const copyBtn = document.getElementById("copyBtn");
const resetBtn = document.getElementById("resetBtn");

// Close Panel
closeBtn.addEventListener("click", () => window.close());

// File Selection (bug fix: stop bubbling to prevent double-open)
fileInput.addEventListener("click", (e) => e.stopPropagation())

fileInput.addEventListener("change", () => {
    if (fileInput.files.length > 0) {
        const file = fileInput.files[0];

        if (file.size > 50 * 1024 * 1024) {
            showError("File too large. Max 50MB allowed.");
            fileInput.value = "";
            return;
        }

        fileNameDisplay.textContent = file.name;
        fileLabel.classList.add("has-file");
        uploadBtn.disabled = false;
        uploadBtn.textContent = "Generate QR Code";
        hideError();
    }
});

// Password Toggle
passwordToggle.addEventListener("change", () => {
    passwordInput.classList.toggle("hidden", !passwordToggle.checked);
    if (!passwordToggle.checked) passwordInput.value = "";
});

// Upload
uploadBtn.addEventListener("click", async () => {
    const file = fileInput.files[0];
    if (!file) return;

    const password = passwordToggle.checked ? passwordInput.value.trim() : "";
    const expiryHours = parseInt(expirySelect.value);

    if (passwordToggle.checked && !password) {
        showError("Please enter a password or uncheck the option.");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);
    formData.append("expiryHours", expiryHours);
    if (password) formData.append("password", password);

    showSection("loading");

    try {
        const response = await fetch(`${API_BASE}/api/upload`, {
            method: "POST",
            body: formData
        });

        const data = await response.json();

        if (!response.ok) {
            showSection("upload");
            showError(data.error || "Upload failed. Please try again.");
            return;
        }

        showSection("result");
        renderQR(data.downloadUrl);
        linkInput.value = data.downloadUrl;

        const expiresAt = new Date(data.expiresAt);
        expiryLabel.textContent = `Expires: ${expiresAt.toLocaleString()}`;

    } catch (err) {
        showSection("upload");
        showError("Could not reach server. Please check Internet connection or try again later.");
        console.error(err);
    }
});

// QR Code
function renderQR(url) {
    qrContainer.innerHTML = "";
    new QRCode(qrContainer, {
        text: url,
        width: 200,
        height: 200,
        colorDark: "#000000",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.M
    });
}

// Copy Link
copyBtn.addEventListener("click", () => {
    navigator.clipboard.writeText(linkInput.value).then(() => {
        copyBtn.textContent = "✅ Copied!";
        setTimeout(() => copyBtn.textContent = "Copy Link", 2000);
    });
});

// Reset
resetBtn.addEventListener("click", () => {
    fileInput.value = "";
    fileNameDisplay.textContent = "Click to select a file";
    fileLabel.classList.remove("has-file");
    passwordToggle.checked = false;
    passwordInput.value = "";
    passwordInput.classList.add("hidden");
    uploadBtn.disabled = true;
    uploadBtn.textContent = "Select a file first";
    qrContainer.innerHTML = "";
    hideError();
    showSection("upload");
});

// Helpers
function showSection(name) {
    uploadSection.classList.add("hidden");
    loadingSection.classList.add("hidden");
    resultSection.classList.add("hidden");
    if (name === "upload") uploadSection.classList.remove("hidden");
    if (name === "loading") loadingSection.classList.remove("hidden");
    if (name === "result") resultSection.classList.remove("hidden");
}

function showError(msg) {
    errorMsg.textContent = msg;
    errorMsg.classList.remove("hidden");
}

function hideError() {
    errorMsg.classList.add("hidden");
}
