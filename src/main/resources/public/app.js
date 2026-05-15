const state = {
  channels: [],
  programs: [],
  token: sessionStorage.getItem("adminToken") || "",
  editingChannelId: null,
  editingProgramId: null
};

const $ = (id) => document.getElementById(id);

document.addEventListener("DOMContentLoaded", () => {
  bindEvents();
  syncAdminState();
  loadData();
});

function bindEvents() {
  $("adminOpenButton").addEventListener("click", openAdminPanel);
  $("closeAdminButton").addEventListener("click", closeAdminPanel);
  $("adminOverlay").addEventListener("click", closeAdminPanelOnBackdrop);
  $("refreshButton").addEventListener("click", loadData);
  $("channelFilter").addEventListener("change", loadPrograms);
  $("sortSelect").addEventListener("change", loadPrograms);
  $("loginForm").addEventListener("submit", login);
  $("logoutButton").addEventListener("click", logout);
  $("channelForm").addEventListener("submit", saveChannel);
  $("programForm").addEventListener("submit", saveProgram);
  $("cancelChannelEdit").addEventListener("click", resetChannelForm);
  $("cancelProgramEdit").addEventListener("click", resetProgramForm);
  $("channelRows").addEventListener("click", handleChannelAction);
  $("programRows").addEventListener("click", handleProgramAction);
}

async function api(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };

  if (state.token) {
    headers["X-Admin-Token"] = state.token;
  }

  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  const data = text ? JSON.parse(text) : {};

  if (!response.ok) {
    throw new Error(data.message || "Запит не виконано");
  }
  return data;
}

async function loadData() {
  try {
    state.channels = await api("/api/channels");
    renderChannelControls();
    await loadPrograms();
    renderAdminTables();
    showStatus("Дані оновлено");
  } catch (error) {
    showStatus(error.message, true);
  }
}

async function loadPrograms() {
  try {
    const channelId = $("channelFilter").value;
    const sort = $("sortSelect").value;
    const params = new URLSearchParams({ sort });
    if (channelId) {
      params.set("channelId", channelId);
    }
    state.programs = await api(`/api/programs?${params.toString()}`);
    renderPrograms();
    renderAdminTables();
  } catch (error) {
    showStatus(error.message, true);
  }
}

function renderChannelControls() {
  const selectedFilter = $("channelFilter").value;
  const selectedProgramChannel = $("programChannel").value;
  $("channelFilter").innerHTML = `<option value="">Усі канали</option>${state.channels.map(channelOption).join("")}`;
  $("programChannel").innerHTML = state.channels.map(channelOption).join("");
  $("channelFilter").value = selectedFilter;
  $("programChannel").value = selectedProgramChannel || (state.channels[0]?.id ?? "");
}

function channelOption(channel) {
  return `<option value="${channel.id}">${escapeHtml(channel.name)}</option>`;
}

function renderPrograms() {
  $("programCount").textContent = `${state.programs.length} ${pluralPrograms(state.programs.length)}`;

  if (state.programs.length === 0) {
    $("programList").innerHTML = `<div class="empty-state">Передачі не знайдено</div>`;
    return;
  }

  $("programList").innerHTML = state.programs.map((program) => `
    <article class="program-card">
      <div class="program-time">
        <span>${escapeHtml(program.startTime)} - ${escapeHtml(program.endTime)}</span>
        <small>${formatDate(program.date)}</small>
      </div>
      <div class="program-body">
        <div class="program-title-row">
          <h3>${escapeHtml(program.title)}</h3>
          <span class="pill age-pill">${program.ageRating}+</span>
        </div>
        <div class="meta-row">
          <span class="pill">${escapeHtml(program.channelName)}</span>
          <span class="pill">${escapeHtml(program.genre)}</span>
        </div>
        <p>${escapeHtml(program.description)}</p>
      </div>
    </article>
  `).join("");
}

function renderAdminTables() {
  $("channelRows").innerHTML = state.channels.map((channel) => `
    <tr>
      <td>${escapeHtml(channel.name)}</td>
      <td>${escapeHtml(channel.category)}</td>
      <td>
        <div class="row-actions">
          <button class="edit-button" type="button" data-action="edit-channel" data-id="${channel.id}">Редагувати</button>
          <button class="danger-button" type="button" data-action="delete-channel" data-id="${channel.id}">Видалити</button>
        </div>
      </td>
    </tr>
  `).join("");

  $("programRows").innerHTML = state.programs.map((program) => `
    <tr>
      <td>${escapeHtml(program.date)}<br>${escapeHtml(program.startTime)} - ${escapeHtml(program.endTime)}</td>
      <td>${escapeHtml(program.title)}</td>
      <td>${escapeHtml(program.channelName)}</td>
      <td>
        <div class="row-actions">
          <button class="edit-button" type="button" data-action="edit-program" data-id="${program.id}">Редагувати</button>
          <button class="danger-button" type="button" data-action="delete-program" data-id="${program.id}">Видалити</button>
        </div>
      </td>
    </tr>
  `).join("");
}

async function login(event) {
  event.preventDefault();
  try {
    const data = await api("/api/login", {
      method: "POST",
      body: JSON.stringify({
        username: $("username").value,
        password: $("password").value
      })
    });
    state.token = data.token;
    sessionStorage.setItem("adminToken", state.token);
    $("loginForm").reset();
    syncAdminState();
    showStatus("Вхід виконано");
  } catch (error) {
    showStatus(error.message, true);
  }
}

function logout() {
  state.token = "";
  sessionStorage.removeItem("adminToken");
  resetChannelForm();
  resetProgramForm();
  syncAdminState();
  showStatus("Вихід виконано");
}

function syncAdminState() {
  $("loginForm").classList.toggle("hidden", Boolean(state.token));
  $("adminContent").classList.toggle("hidden", !state.token);
  $("logoutButton").classList.toggle("hidden", !state.token);
  $("adminTitle").textContent = state.token ? "Керування даними" : "Вхід адміністратора";
}

function openAdminPanel() {
  $("adminOverlay").classList.remove("hidden");
  if (state.token) {
    $("channelName").focus();
  } else {
    $("username").focus();
  }
}

function closeAdminPanel() {
  $("adminOverlay").classList.add("hidden");
}

function closeAdminPanelOnBackdrop(event) {
  if (event.target === $("adminOverlay")) {
    closeAdminPanel();
  }
}

async function saveChannel(event) {
  event.preventDefault();
  const payload = {
    name: $("channelName").value,
    category: $("channelCategory").value
  };
  const path = state.editingChannelId ? `/api/channels/${state.editingChannelId}` : "/api/channels";
  const method = state.editingChannelId ? "PUT" : "POST";

  try {
    await api(path, { method, body: JSON.stringify(payload) });
    resetChannelForm();
    await loadData();
    showStatus("Канал збережено");
  } catch (error) {
    showStatus(error.message, true);
  }
}

async function saveProgram(event) {
  event.preventDefault();
  const payload = {
    channelId: Number($("programChannel").value),
    title: $("programTitle").value,
    description: $("programDescription").value,
    date: $("programDate").value,
    startTime: $("programStart").value,
    endTime: $("programEnd").value,
    genre: $("programGenre").value,
    ageRating: Number($("programAge").value)
  };
  const path = state.editingProgramId ? `/api/programs/${state.editingProgramId}` : "/api/programs";
  const method = state.editingProgramId ? "PUT" : "POST";

  try {
    await api(path, { method, body: JSON.stringify(payload) });
    resetProgramForm();
    await loadData();
    showStatus("Передачу збережено");
  } catch (error) {
    showStatus(error.message, true);
  }
}

async function handleChannelAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) {
    return;
  }

  const id = Number(button.dataset.id);
  const channel = state.channels.find((item) => item.id === id);

  if (button.dataset.action === "edit-channel" && channel) {
    state.editingChannelId = id;
    $("channelName").value = channel.name;
    $("channelCategory").value = channel.category;
    $("saveChannelButton").textContent = "Оновити канал";
    return;
  }

  if (button.dataset.action === "delete-channel" && confirm("Видалити канал разом із його передачами?")) {
    try {
      await api(`/api/channels/${id}`, { method: "DELETE" });
      await loadData();
      showStatus("Канал видалено");
    } catch (error) {
      showStatus(error.message, true);
    }
  }
}

async function handleProgramAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) {
    return;
  }

  const id = Number(button.dataset.id);
  const program = state.programs.find((item) => item.id === id);

  if (button.dataset.action === "edit-program" && program) {
    state.editingProgramId = id;
    $("programChannel").value = program.channelId;
    $("programTitle").value = program.title;
    $("programDescription").value = program.description;
    $("programDate").value = program.date;
    $("programStart").value = program.startTime;
    $("programEnd").value = program.endTime;
    $("programGenre").value = program.genre;
    $("programAge").value = program.ageRating;
    $("saveProgramButton").textContent = "Оновити передачу";
    return;
  }

  if (button.dataset.action === "delete-program" && confirm("Видалити передачу?")) {
    try {
      await api(`/api/programs/${id}`, { method: "DELETE" });
      await loadData();
      showStatus("Передачу видалено");
    } catch (error) {
      showStatus(error.message, true);
    }
  }
}

function resetChannelForm() {
  state.editingChannelId = null;
  $("channelForm").reset();
  $("saveChannelButton").textContent = "Зберегти канал";
}

function resetProgramForm() {
  state.editingProgramId = null;
  $("programForm").reset();
  $("programChannel").value = state.channels[0]?.id ?? "";
  $("programAge").value = 0;
  $("saveProgramButton").textContent = "Зберегти передачу";
}

function showStatus(message, isError = false) {
  $("statusLine").textContent = message;
  $("statusLine").classList.toggle("error", isError);
}

function pluralPrograms(count) {
  if (count === 1) {
    return "передача";
  }
  if (count > 1 && count < 5) {
    return "передачі";
  }
  return "передач";
}

function formatDate(value) {
  const date = new Date(`${value}T00:00:00`);
  return new Intl.DateTimeFormat("uk-UA", {
    day: "2-digit",
    month: "long",
    year: "numeric"
  }).format(date);
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
