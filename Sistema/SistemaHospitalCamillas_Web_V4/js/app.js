"use strict";

const STORAGE_KEY = "hospital_pro_camillas_web_v4";

const USERS = {
  admin: { password: "1234", name: "Administrador del sistema", role: "Administrador" },
  medico: { password: "1234", name: "Personal médico", role: "Médico" },
  mantenimiento: { password: "1234", name: "Personal de mantenimiento", role: "Mantenimiento" },
  consulta: { password: "1234", name: "Usuario de consulta", role: "Consulta" }
};

const VIEW_META = {
  dashboard: { eyebrow: "PANEL OPERATIVO", title: "Centro de control" },
  map: { eyebrow: "MONITOREO EN TIEMPO REAL", title: "Mapa de camillas" },
  beds: { eyebrow: "GESTIÓN HOSPITALARIA", title: "Estado de camillas" },
  patients: { eyebrow: "REGISTRO CLÍNICO", title: "Pacientes" },
  assignment: { eyebrow: "ATENCIÓN Y DISPONIBILIDAD", title: "Asignar / liberar" },
  status: { eyebrow: "MANTENIMIENTO OPERATIVO", title: "Limpieza y fallas" },
  triage: { eyebrow: "PRIORIDAD DE ATENCIÓN", title: "Triaje" },
  reports: { eyebrow: "INFORMACIÓN DEL SISTEMA", title: "Reportes" },
  settings: { eyebrow: "ADMINISTRACIÓN", title: "Herramientas" }
};

const STATUS = {
  Libre: { color: "#9be8bb", icon: "✓", label: "Disponible" },
  Ocupada: { color: "#ff9aa9", icon: "●", label: "Ocupada" },
  "En limpieza": { color: "#ffe08a", icon: "◌", label: "En limpieza" },
  Falla: { color: "#aeb9ca", icon: "!", label: "Con falla" }
};

const TRIAGE_LEVELS = [
  { value: "I - Azul - Reanimación", color: "#8ec9ff", time: "Inmediato" },
  { value: "II - Rojo - Emergencia", color: "#ff9eaa", time: "Inmediato / 7 min" },
  { value: "III - Naranja - Urgente", color: "#ffc17e", time: "30 minutos" },
  { value: "IV - Verde - Menos urgente", color: "#9be8bb", time: "45 minutos" },
  { value: "V - Negro - No urgente", color: "#b4bfd0", time: "60 minutos" }
];

let session = null;
let currentView = "dashboard";
let viewContext = {};
let state = loadState();

const $ = (selector, parent = document) => parent.querySelector(selector);
const $$ = (selector, parent = document) => [...parent.querySelectorAll(selector)];

function isoMinutesAgo(minutes) {
  return new Date(Date.now() - minutes * 60_000).toISOString();
}

function seedState() {
  return {
    beds: [
      { id: 1, code: "CAMILLA 001", area: "Área de emergencia", status: "Libre", patientId: null, observation: "Lista para asignación", updatedAt: isoMinutesAgo(8) },
      { id: 2, code: "CAMILLA 002", area: "Box 1", status: "Ocupada", patientId: 2, observation: "Monitoreo clínico activo", updatedAt: isoMinutesAgo(21) },
      { id: 3, code: "CAMILLA 003", area: "Pasillo Norte", status: "En limpieza", patientId: null, observation: "Limpieza en proceso", updatedAt: isoMinutesAgo(34) },
      { id: 4, code: "CAMILLA 004", area: "Sala de observación", status: "Falla", patientId: null, observation: "Rueda derecha con desperfecto", updatedAt: isoMinutesAgo(58) },
      { id: 5, code: "CAMILLA 005", area: "Box 2", status: "Libre", patientId: null, observation: "Lista para asignación", updatedAt: isoMinutesAgo(65) },
      { id: 6, code: "CAMILLA 006", area: "Observación 2", status: "Libre", patientId: null, observation: "Lista para asignación", updatedAt: isoMinutesAgo(92) },
      { id: 7, code: "CAMILLA 007", area: "Triaje", status: "Ocupada", patientId: 1, observation: "Paciente en evaluación", updatedAt: isoMinutesAgo(107) },
      { id: 8, code: "CAMILLA 008", area: "Box 3", status: "Libre", patientId: null, observation: "Disponible", updatedAt: isoMinutesAgo(143) }
    ],
    patients: [
      { id: 1, document: "10245", name: "Juan Pérez", age: 45, sex: "Masculino", phone: "70701010", createdAt: isoMinutesAgo(220) },
      { id: 2, document: "20880", name: "Ana López", age: 32, sex: "Femenino", phone: "60702020", createdAt: isoMinutesAgo(190) },
      { id: 3, document: "30991", name: "María Flores", age: 28, sex: "Femenino", phone: "71703030", createdAt: isoMinutesAgo(124) }
    ],
    triages: [
      { id: 1, patientId: 2, level: "II - Rojo - Emergencia", bloodPressure: "135/85", temperature: 38.1, heartRate: 102, notes: "Dolor torácico. Control médico inmediato.", createdAt: isoMinutesAgo(40) },
      { id: 2, patientId: 1, level: "III - Naranja - Urgente", bloodPressure: "128/79", temperature: 37.2, heartRate: 89, notes: "Evaluación en triaje.", createdAt: isoMinutesAgo(88) }
    ],
    activities: [
      { id: 1, action: "Cambio de estado", detail: "CAMILLA 003 pasó a limpieza", icon: "◌", at: isoMinutesAgo(34), user: "mantenimiento" },
      { id: 2, action: "Asignación de camilla", detail: "CAMILLA 002 asignada a Ana López", icon: "⇄", at: isoMinutesAgo(21), user: "medico" },
      { id: 3, action: "Registro de triaje", detail: "Ana López · Nivel II - Rojo", icon: "✚", at: isoMinutesAgo(40), user: "medico" },
      { id: 4, action: "Registro de falla", detail: "CAMILLA 004 · Rueda derecha", icon: "!", at: isoMinutesAgo(58), user: "mantenimiento" }
    ]
  };
}

function loadState() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (saved && Array.isArray(saved.beds) && Array.isArray(saved.patients) && Array.isArray(saved.triages)) {
      return { ...seedState(), ...saved, activities: Array.isArray(saved.activities) ? saved.activities : [] };
    }
  } catch (_) {
    // Se utiliza el contenido inicial cuando el navegador no puede leer los datos locales.
  }
  return seedState();
}

function saveState() {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); } catch (_) { /* Navegador en modo restringido. */ }
  updateAlertCount();
}

function esc(value) {
  return String(value ?? "").replace(/[&<>'"]/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[char]));
}

function dateText(date) {
  if (!date) return "-";
  return new Intl.DateTimeFormat("es-BO", { dateStyle: "short", timeStyle: "short" }).format(new Date(date));
}

function relativeTime(date) {
  const diff = Math.max(0, Date.now() - new Date(date).getTime());
  const mins = Math.round(diff / 60_000);
  if (mins < 1) return "Ahora";
  if (mins < 60) return `Hace ${mins} min`;
  const hours = Math.round(mins / 60);
  if (hours < 24) return `Hace ${hours} h`;
  return `Hace ${Math.round(hours / 24)} d`;
}

function nextId(list) {
  return list.reduce((max, item) => Math.max(max, Number(item.id) || 0), 0) + 1;
}

function getBed(id) { return state.beds.find(bed => Number(bed.id) === Number(id)); }
function getPatient(id) { return state.patients.find(patient => Number(patient.id) === Number(id)); }
function patientName(id) { return getPatient(id)?.name || "Sin paciente asignado"; }
function statusMeta(status) { return STATUS[status] || STATUS.Falla; }
function statusColor(status) { return statusMeta(status).color; }
function countBeds(status) { return state.beds.filter(bed => bed.status === status).length; }
function isAdmin() { return session?.role === "Administrador"; }

function can(permission) {
  if (!session) return false;
  if (session.role === "Administrador") return true;
  const permissions = {
    patients: session.role === "Médico",
    assign: session.role === "Médico",
    status: session.role === "Mantenimiento",
    triage: session.role === "Médico",
    reports: session.role === "Médico",
    settings: false,
    manageBeds: false
  };
  return Boolean(permissions[permission]);
}

function addActivity(action, detail, icon = "•") {
  state.activities.unshift({
    id: nextId(state.activities),
    action,
    detail,
    icon,
    at: new Date().toISOString(),
    user: session?.username || "sistema"
  });
  state.activities = state.activities.slice(0, 60);
  saveState();
}

function activeAlerts() {
  const alerts = [];
  state.beds.filter(bed => bed.status === "Falla").forEach(bed => alerts.push({ type: "danger", icon: "!", title: `${bed.code} requiere mantenimiento`, detail: bed.observation || "Se registró una falla." }));
  state.beds.filter(bed => bed.status === "En limpieza").forEach(bed => alerts.push({ type: "warn", icon: "◌", title: `${bed.code} está en limpieza`, detail: bed.area }));
  if (countBeds("Libre") === 0) alerts.push({ type: "danger", icon: "●", title: "No hay camillas libres", detail: "Revisa altas, limpieza y disponibilidad." });
  return alerts;
}

function showToast(title, message, type = "success") {
  const icons = { success: "✓", warning: "!", error: "×" };
  const toast = document.createElement("article");
  toast.className = `toast ${type === "success" ? "" : type}`;
  toast.innerHTML = `<span class="toast-icon">${icons[type] || "✓"}</span><div><b>${esc(title)}</b><small>${esc(message)}</small></div>`;
  $("#toastContainer").append(toast);
  setTimeout(() => toast.remove(), 3900);
}

function updateAlertCount() {
  const count = activeAlerts().length;
  const element = $("#notificationCount");
  if (element) {
    element.textContent = count;
    element.style.display = count ? "grid" : "none";
  }
}

function updateSessionUI() {
  if (!session) return;
  $("#sideUserName").textContent = session.name;
  $("#sideUserRole").textContent = session.role;
  $("#userAvatar").textContent = session.name.split(" ").slice(0, 2).map(part => part[0]).join("").toUpperCase();
  $$("[data-permission]").forEach(button => { button.hidden = !can(button.dataset.permission); });
  updateAlertCount();
}

function login(username, password) {
  const profile = USERS[String(username || "").trim().toLowerCase()];
  if (!profile || profile.password !== password) {
    $("#loginMessage").textContent = "Usuario o contraseña incorrectos.";
    return;
  }
  session = { username: String(username).trim().toLowerCase(), ...profile };
  $("#loginMessage").textContent = "";
  $("#loginScreen").classList.add("is-hidden");
  $("#appShell").classList.remove("is-hidden");
  updateSessionUI();
  navigate("dashboard");
  showToast("Sesión iniciada", `Bienvenido, ${session.name}.`);
}

function logout() {
  session = null;
  $("#appShell").classList.add("is-hidden");
  $("#loginScreen").classList.remove("is-hidden");
  $("#loginPassword").value = "1234";
  $("#loginUser").focus();
}

function navigate(view, context = {}) {
  const nav = $(`[data-view="${view}"]`);
  if (nav?.dataset.permission && !can(nav.dataset.permission)) {
    showToast("Acceso restringido", "Tu rol no tiene permiso para este módulo.", "warning");
    return;
  }
  currentView = view;
  viewContext = { ...context };
  $$(".nav-item[data-view]").forEach(item => item.classList.toggle("active", item.dataset.view === view));
  const meta = VIEW_META[view] || VIEW_META.dashboard;
  $("#pageEyebrow").textContent = meta.eyebrow;
  $("#pageTitle").textContent = meta.title;
  $("#sidebar").classList.remove("open");
  renderCurrentView();
  $("#viewRoot").focus({ preventScroll: true });
}

function renderCurrentView() {
  const renderers = { dashboard: renderDashboard, map: renderMap, beds: renderBeds, patients: renderPatients, assignment: renderAssignment, status: renderStatus, triage: renderTriage, reports: renderReports, settings: renderSettings };
  (renderers[currentView] || renderDashboard)();
}

function viewHeader(eyebrow, title, detail, actions = "") {
  return `<div class="view-header"><div><p class="eyebrow">${esc(eyebrow)}</p><h2>${esc(title)}</h2><p>${esc(detail)}</p></div><div class="header-actions">${actions}</div></div>`;
}

function metricCard({ label, value, icon, color, go, status }) {
  return `<button class="stat-card" type="button" style="--accent:${color}" data-go="${go}" ${status ? `data-status="${esc(status)}"` : ""}><span class="stat-icon">${icon}</span><span class="stat-value">${value}</span><span class="stat-name">${esc(label)}</span><span class="stat-link">Ver detalle →</span></button>`;
}

function quickAction(label, detail, icon, action, allowed = true) {
  return `<button class="quick-action" type="button" data-action="${action}" ${allowed ? "" : "disabled title=\"No tienes permiso para esta acción\""}><span class="qa-icon">${icon}</span><b>${esc(label)}</b><small>${esc(detail)}</small></button>`;
}

function statusPill(status) {
  return `<span class="status-pill" style="--status:${statusColor(status)}">${statusMeta(status).icon} ${esc(status)}</span>`;
}

function renderDashboard() {
  const root = $("#viewRoot");
  const total = Math.max(state.beds.length, 1);
  const occupied = countBeds("Ocupada");
  const occupation = Math.round((occupied / total) * 100);
  const actions = [
    quickAction("Mapa completo", "Estado visual por camilla", "▦", "open-map"),
    quickAction("Camillas libres", "Encuentra disponibilidad", "✓", "open-free-beds"),
    quickAction("Asignar paciente", "Vincula una camilla", "⇄", "open-assignment", can("assign")),
    quickAction("Nuevo paciente", "Registra datos clínicos", "♙", "new-patient", can("patients")),
    quickAction("Limpieza / falla", "Actualiza disponibilidad", "⚙", "open-status", can("status")),
    quickAction("Registrar triaje", "Prioriza una atención", "✚", "open-triage", can("triage"))
  ].join("");

  root.innerHTML = `
    ${viewHeader("RESUMEN OPERATIVO", "Todo bajo control", "Consulta la disponibilidad y entra directamente a la acción que necesitas.", `<button class="btn btn-secondary" type="button" data-action="open-alerts">♧ Ver alertas</button><button class="btn btn-primary" type="button" data-action="quick-actions">⚡ Acciones rápidas</button>`)}
    <section class="stat-grid">
      ${metricCard({ label: "Camillas libres", value: countBeds("Libre"), icon: "✓", color: "#35cb91", go: "beds", status: "Libre" })}
      ${metricCard({ label: "Camillas ocupadas", value: countBeds("Ocupada"), icon: "●", color: "#fb7185", go: "map", status: "Ocupada" })}
      ${metricCard({ label: "En limpieza", value: countBeds("En limpieza"), icon: "◌", color: "#f7c948", go: "map", status: "En limpieza" })}
      ${metricCard({ label: "Con falla", value: countBeds("Falla"), icon: "!", color: "#aeb9ca", go: "map", status: "Falla" })}
      ${metricCard({ label: "Pacientes registrados", value: state.patients.length, icon: "♙", color: "#4f8cff", go: can("patients") ? "patients" : "map" })}
      ${metricCard({ label: "Triajes registrados", value: state.triages.length, icon: "✚", color: "#fb923c", go: can("triage") ? "triage" : "map" })}
    </section>
    <section class="dashboard-grid">
      <div class="dashboard-main">
        <article class="panel-card"><div class="panel-heading"><div><h3>Capacidad de atención</h3><p>Ocupación en tiempo real del área de camillas.</p></div><span class="badge ${occupation >= 80 ? "danger" : "good"}">${occupation >= 80 ? "Atención requerida" : "Operación estable"}</span></div>
          <div class="capacity-row"><div><p class="capacity-total">CAMILLAS OCUPADAS</p><div class="capacity-number">${occupied}<span> / ${state.beds.length}</span></div><div class="progress-track"><div class="progress-value" style="width:${occupation}%"></div></div><p class="form-hint">${occupation}% de ocupación actual.</p></div>
          <div class="capacity-breakdown"><div class="mini-count" style="--status:#35cb91"><b>${countBeds("Libre")}</b><span>Disponibles</span></div><div class="mini-count" style="--status:#fb7185"><b>${occupied}</b><span>Ocupadas</span></div><div class="mini-count" style="--status:#f7c948"><b>${countBeds("En limpieza")}</b><span>Limpieza</span></div><div class="mini-count" style="--status:#b9c4d7"><b>${countBeds("Falla")}</b><span>Con falla</span></div></div></div>
        </article>
        <article class="panel-card"><div class="panel-heading"><div><h3>Accesos rápidos</h3><p>Los botones se adaptan a tu rol de usuario.</p></div></div><div class="quick-action-grid">${actions}</div></article>
        <article class="panel-card"><div class="panel-heading"><div><h3>Actividad reciente</h3><p>Últimas acciones guardadas en el sistema.</p></div><button class="btn btn-ghost" type="button" data-go="reports">Ver reportes</button></div>${activityList(state.activities.slice(0, 6))}</article>
      </div>
      <aside class="panel-card"><div class="panel-heading"><div><h3>Mapa rápido de camillas</h3><p>Haz clic en una tarjeta para abrir su detalle.</p></div><button class="btn btn-ghost" type="button" data-go="map">Abrir mapa</button></div><div class="bed-mini-grid">${state.beds.slice().sort((a, b) => a.code.localeCompare(b.code)).slice(0, 8).map(miniBedCard).join("")}</div></aside>
    </section>`;
  bindCommonActions(root);
  root.querySelectorAll(".mini-bed").forEach(button => button.addEventListener("click", () => navigate("map", { selectedBedId: Number(button.dataset.id) })));
}

function activityList(items) {
  if (!items.length) return `<div class="empty-state"><span>◌</span><b>Aún no hay actividad</b><p>Las acciones que realices aparecerán aquí.</p></div>`;
  return `<div class="activity-list">${items.map(item => `<article class="activity-row"><span class="activity-icon">${esc(item.icon || "•")}</span><div><b>${esc(item.action)}</b><small>${esc(item.detail)}</small></div><time title="${esc(dateText(item.at))}">${esc(relativeTime(item.at))}</time></article>`).join("")}</div>`;
}

function miniBedCard(bed) {
  const meta = statusMeta(bed.status);
  return `<button class="mini-bed" type="button" style="--status:${meta.color}" data-id="${bed.id}"><b>${esc(bed.code)}</b><span>${esc(bed.status)}</span><small>${esc(bed.patientId ? patientName(bed.patientId) : bed.area)}</small></button>`;
}

function filterBeds(query = "", status = "Todos") {
  const normalized = String(query).trim().toLowerCase();
  return state.beds.filter(bed => {
    const matchesText = !normalized || `${bed.code} ${bed.area} ${patientName(bed.patientId)}`.toLowerCase().includes(normalized);
    return matchesText && (status === "Todos" || bed.status === status);
  }).sort((a, b) => a.code.localeCompare(b.code));
}

function statusOptions(selected = "Todos", includeAll = true) {
  const values = includeAll ? ["Todos", ...Object.keys(STATUS)] : Object.keys(STATUS);
  return values.map(value => `<option value="${esc(value)}" ${value === selected ? "selected" : ""}>${esc(value)}</option>`).join("");
}

function bedCard(bed, selectedId) {
  const meta = statusMeta(bed.status);
  return `<button class="bed-map-card ${Number(selectedId) === bed.id ? "selected" : ""}" type="button" style="--status:${meta.color}" data-select-bed="${bed.id}"><span class="bed-status">${meta.icon} ${esc(bed.status)}</span><b>${esc(bed.code)}</b><span class="bed-area">${esc(bed.area)}</span><span class="bed-patient">${esc(bed.patientId ? patientName(bed.patientId) : "Sin paciente asignado")}</span></button>`;
}

function detailPanel(bed) {
  if (!bed) return `<aside class="panel-card detail-card"><div class="panel-heading"><div><h3>Camilla seleccionada</h3><p>Selecciona una tarjeta del mapa.</p></div></div><div class="detail-empty"><div><span>▦</span>El detalle, paciente y acciones disponibles aparecerán aquí.</div></div></aside>`;
  const patient = getPatient(bed.patientId);
  const meta = statusMeta(bed.status);
  let action = `<button class="btn btn-secondary btn-full" type="button" data-bed-action="details" data-id="${bed.id}">Ver detalle completo</button>`;
  if (bed.status === "Libre") action = `<button class="btn btn-primary btn-full" type="button" data-bed-action="assign" data-id="${bed.id}" ${can("assign") ? "" : "disabled"}>⇄ Asignar paciente</button>`;
  if (bed.status === "Ocupada") action = `<button class="btn btn-danger btn-full" type="button" data-bed-action="release" data-id="${bed.id}" ${can("assign") ? "" : "disabled"}>↩ Liberar camilla</button>`;
  if (["En limpieza", "Falla"].includes(bed.status)) action = `<button class="btn btn-primary btn-full" type="button" data-bed-action="status" data-id="${bed.id}" ${can("status") ? "" : "disabled"}>⚙ Gestionar estado</button>`;
  return `<aside class="panel-card detail-card"><div class="panel-heading"><div><h3>Camilla seleccionada</h3><span class="detail-status" style="--status:${meta.color}">${meta.icon} ${esc(bed.status)}</span></div><button class="icon-btn" type="button" data-bed-action="details" data-id="${bed.id}" title="Ver detalle">i</button></div><div class="info-list"><div><small>CÓDIGO</small><b>${esc(bed.code)}</b></div><div><small>ÁREA</small><b>${esc(bed.area)}</b></div><div><small>PACIENTE</small><b>${esc(patient?.name || "Sin paciente asignado")}</b></div><div><small>OBSERVACIÓN</small><b>${esc(bed.observation || "Sin observación")}</b></div><div><small>ÚLTIMA ACTUALIZACIÓN</small><b>${esc(dateText(bed.updatedAt))}</b></div></div>${action}<button class="btn btn-ghost btn-full" type="button" data-go="beds" data-status="${esc(bed.status)}">Abrir en tabla</button></aside>`;
}

function renderMap() {
  const root = $("#viewRoot");
  const query = viewContext.query || "";
  const status = viewContext.status || "Todos";
  const visible = filterBeds(query, status);
  let selected = getBed(viewContext.selectedBedId);
  if (!selected && visible.length) { selected = visible[0]; viewContext.selectedBedId = selected.id; }
  root.innerHTML = `
    ${viewHeader("MONITOREO EN TIEMPO REAL", "Mapa interactivo de camillas", "Selecciona una camilla para ver sus detalles y ejecutar la acción disponible.", `<button class="btn btn-secondary" type="button" data-go="beds">▤ Vista de tabla</button>${can("manageBeds") ? `<button class="btn btn-primary" type="button" data-action="new-bed">＋ Nueva camilla</button>` : ""}`)}
    <div class="filter-bar"><input id="mapSearch" class="filter-input" value="${esc(query)}" placeholder="Buscar por código, área o paciente"><select id="mapStatus" class="select-input">${statusOptions(status)}</select><button id="resetMap" class="btn btn-ghost" type="button">Restablecer</button><span class="filter-summary">${visible.length} de ${state.beds.length} camillas mostradas</span></div>
    <div class="legend"><span><i style="--legend:#9be8bb"></i>Libre</span><span><i style="--legend:#ff9aa9"></i>Ocupada</span><span><i style="--legend:#ffe08a"></i>En limpieza</span><span><i style="--legend:#aeb9ca"></i>Falla</span></div>
    <section class="map-layout"><div class="panel-card">${visible.length ? `<div class="bed-grid">${visible.map(bed => bedCard(bed, selected?.id)).join("")}</div>` : emptyState("No se encontraron camillas", "Prueba con otro estado o con una búsqueda diferente.")}</div>${detailPanel(selected)}</section>`;
  bindCommonActions(root);
  root.querySelectorAll("[data-select-bed]").forEach(button => button.addEventListener("click", () => { viewContext.selectedBedId = Number(button.dataset.selectBed); renderMap(); }));
  root.querySelectorAll("[data-bed-action]").forEach(button => button.addEventListener("click", () => handleBedAction(button.dataset.bedAction, Number(button.dataset.id))));
  bindMapFilters();
}

function bindMapFilters() {
  const input = $("#mapSearch");
  const status = $("#mapStatus");
  input.addEventListener("input", event => rerenderWithFocus("mapSearch", event.target.selectionStart, () => { viewContext.query = event.target.value; renderMap(); }));
  status.addEventListener("change", event => { viewContext.status = event.target.value; renderMap(); });
  $("#resetMap").addEventListener("click", () => { viewContext = {}; renderMap(); });
}

function rerenderWithFocus(inputId, caret, render) {
  render();
  const nextInput = $(`#${inputId}`);
  if (nextInput) {
    nextInput.focus();
    const position = Math.min(caret ?? nextInput.value.length, nextInput.value.length);
    nextInput.setSelectionRange(position, position);
  }
}

function renderBeds() {
  const root = $("#viewRoot");
  const query = viewContext.query || "";
  const status = viewContext.status || "Todos";
  const beds = filterBeds(query, status);
  root.innerHTML = `
    ${viewHeader("GESTIÓN HOSPITALARIA", "Estado de camillas", "Busca, filtra y administra el inventario de camillas.", `<button class="btn btn-secondary" type="button" data-go="map">▦ Mapa visual</button>${can("manageBeds") ? `<button class="btn btn-primary" type="button" data-action="new-bed">＋ Agregar camilla</button>` : ""}`)}
    <div class="filter-bar"><input id="bedsSearch" class="filter-input" value="${esc(query)}" placeholder="Buscar código, área o paciente"><select id="bedsStatus" class="select-input">${statusOptions(status)}</select><button id="resetBeds" class="btn btn-ghost" type="button">Limpiar filtros</button><span class="filter-summary">${beds.length} resultado(s)</span></div>
    ${beds.length ? bedTable(beds) : emptyState("Sin resultados", "No hay camillas que coincidan con tu filtro.")}`;
  bindCommonActions(root);
  root.querySelectorAll("[data-bed-action]").forEach(button => button.addEventListener("click", () => handleBedAction(button.dataset.bedAction, Number(button.dataset.id))));
  $("#bedsSearch").addEventListener("input", event => rerenderWithFocus("bedsSearch", event.target.selectionStart, () => { viewContext.query = event.target.value; renderBeds(); }));
  $("#bedsStatus").addEventListener("change", event => { viewContext.status = event.target.value; renderBeds(); });
  $("#resetBeds").addEventListener("click", () => { viewContext = {}; renderBeds(); });
}

function bedTable(beds) {
  return `<div class="table-wrap"><table class="data-table"><thead><tr><th>Camilla</th><th>Área</th><th>Estado</th><th>Paciente</th><th>Actualizado</th><th>Acciones</th></tr></thead><tbody>${beds.map(bed => `<tr><td><span class="cell-title">${esc(bed.code)}</span><span class="cell-subtitle">${esc(bed.observation || "Sin observación")}</span></td><td>${esc(bed.area)}</td><td>${statusPill(bed.status)}</td><td>${esc(bed.patientId ? patientName(bed.patientId) : "-")}</td><td>${esc(relativeTime(bed.updatedAt))}</td><td><div class="row-actions"><button class="row-btn" type="button" data-bed-action="details" data-id="${bed.id}" title="Ver detalle">i</button><button class="row-btn" type="button" data-bed-action="map" data-id="${bed.id}" title="Abrir mapa">▦</button>${isAdmin() ? `<button class="row-btn" type="button" data-bed-action="edit" data-id="${bed.id}" title="Editar">✎</button>` : ""}${isAdmin() && bed.status === "Libre" ? `<button class="row-btn" type="button" data-bed-action="delete" data-id="${bed.id}" title="Eliminar">×</button>` : ""}</div></td></tr>`).join("")}</tbody></table></div>`;
}

function renderPatients() {
  const root = $("#viewRoot");
  const query = viewContext.query || "";
  const normalized = query.toLowerCase();
  const patients = state.patients.filter(patient => !normalized || `${patient.name} ${patient.document} ${patient.phone}`.toLowerCase().includes(normalized)).sort((a, b) => a.name.localeCompare(b.name));
  root.innerHTML = `
    ${viewHeader("REGISTRO CLÍNICO", "Pacientes", "Gestiona los datos necesarios para asignar camillas y registrar triaje.", `<button class="btn btn-primary" type="button" data-action="new-patient">＋ Nuevo paciente</button>`)}
    <div class="filter-bar"><input id="patientSearch" class="filter-input" value="${esc(query)}" placeholder="Buscar por CI, nombre o teléfono"><span class="filter-summary">${patients.length} paciente(s) mostrados</span></div>
    ${patients.length ? patientTable(patients) : emptyState("No hay pacientes", "Registra un paciente para comenzar.", "＋ Nuevo paciente", "new-patient")}`;
  bindCommonActions(root);
  root.querySelectorAll("[data-patient-action]").forEach(button => button.addEventListener("click", () => handlePatientAction(button.dataset.patientAction, Number(button.dataset.id))));
  $("#patientSearch").addEventListener("input", event => rerenderWithFocus("patientSearch", event.target.selectionStart, () => { viewContext.query = event.target.value; renderPatients(); }));
}

function patientTable(patients) {
  return `<div class="table-wrap"><table class="data-table"><thead><tr><th>Paciente</th><th>CI / Documento</th><th>Edad</th><th>Sexo</th><th>Teléfono</th><th>Camilla</th><th>Acciones</th></tr></thead><tbody>${patients.map(patient => {
    const assigned = state.beds.find(bed => bed.patientId === patient.id && bed.status === "Ocupada");
    return `<tr><td><span class="cell-title">${esc(patient.name)}</span><span class="cell-subtitle">Registrado ${esc(relativeTime(patient.createdAt))}</span></td><td>${esc(patient.document)}</td><td>${esc(patient.age)}</td><td>${esc(patient.sex)}</td><td>${esc(patient.phone || "-")}</td><td>${esc(assigned?.code || "-")}</td><td><div class="row-actions"><button class="row-btn" type="button" data-patient-action="details" data-id="${patient.id}" title="Ver detalle">i</button><button class="row-btn" type="button" data-patient-action="edit" data-id="${patient.id}" title="Editar">✎</button>${isAdmin() ? `<button class="row-btn" type="button" data-patient-action="delete" data-id="${patient.id}" title="Eliminar">×</button>` : ""}</div></td></tr>`;
  }).join("")}</tbody></table></div>`;
}

function renderAssignment() {
  const root = $("#viewRoot");
  const freeBeds = state.beds.filter(bed => bed.status === "Libre").sort((a, b) => a.code.localeCompare(b.code));
  const availablePatients = state.patients.filter(patient => !state.beds.some(bed => bed.patientId === patient.id && bed.status === "Ocupada")).sort((a, b) => a.name.localeCompare(b.name));
  const occupied = state.beds.filter(bed => bed.status === "Ocupada").sort((a, b) => a.code.localeCompare(b.code));
  root.innerHTML = `
    ${viewHeader("ATENCIÓN Y DISPONIBILIDAD", "Asignar o liberar camilla", "Asigna una camilla libre a un paciente o libera una asignación finalizada.", `<button class="btn btn-secondary" type="button" data-go="map">▦ Ver mapa</button><button class="btn btn-primary" type="button" data-action="new-patient">＋ Nuevo paciente</button>`)}
    <section class="split-layout"><article class="panel-card"><div class="panel-heading"><div><h3>Nueva asignación</h3><p>Solo aparecen pacientes sin camilla y camillas disponibles.</p></div><span class="badge good">${freeBeds.length} libres</span></div>
      ${availablePatients.length && freeBeds.length ? `<form id="assignmentForm" class="form-grid"><div class="form-field full"><label for="assignPatient">Paciente</label><select id="assignPatient" class="select-input">${availablePatients.map(patient => `<option value="${patient.id}">${esc(patient.name)} · CI ${esc(patient.document)}</option>`).join("")}</select></div><div class="form-field full"><label for="assignBed">Camilla libre</label><select id="assignBed" class="select-input">${freeBeds.map(bed => `<option value="${bed.id}">${esc(bed.code)} · ${esc(bed.area)}</option>`).join("")}</select></div><div class="form-field full"><button class="btn btn-primary btn-full" type="submit">⇄ Confirmar asignación</button></div></form>` : emptyState("No se puede realizar una asignación", availablePatients.length ? "No hay camillas libres por el momento." : "No hay pacientes disponibles para asignar.", "＋ Registrar paciente", "new-patient")}</article>
      <article class="panel-card"><div class="panel-heading"><div><h3>Camillas ocupadas</h3><p>Selecciona liberar cuando el paciente deje la camilla.</p></div><span class="badge ${occupied.length ? "warn" : "good"}">${occupied.length} ocupada(s)</span></div>${occupied.length ? `<div class="table-wrap"><table class="data-table"><thead><tr><th>Camilla</th><th>Paciente</th><th>Área</th><th>Acción</th></tr></thead><tbody>${occupied.map(bed => `<tr><td><span class="cell-title">${esc(bed.code)}</span></td><td>${esc(patientName(bed.patientId))}</td><td>${esc(bed.area)}</td><td><button class="btn btn-danger" type="button" data-bed-action="release" data-id="${bed.id}">Liberar</button></td></tr>`).join("")}</tbody></table></div>` : emptyState("Sin camillas ocupadas", "Actualmente no hay asignaciones activas.")}</article></section>`;
  bindCommonActions(root);
  $("#assignmentForm")?.addEventListener("submit", event => {
    event.preventDefault();
    const patientId = Number($("#assignPatient").value);
    const bedId = Number($("#assignBed").value);
    requestAssignment(patientId, bedId);
  });
  root.querySelectorAll("[data-bed-action]").forEach(button => button.addEventListener("click", () => handleBedAction(button.dataset.bedAction, Number(button.dataset.id))));
}

function renderStatus() {
  const root = $("#viewRoot");
  let selected = getBed(viewContext.selectedBedId) || state.beds.slice().sort((a, b) => a.code.localeCompare(b.code))[0];
  if (selected) viewContext.selectedBedId = selected.id;
  const newStatus = viewContext.newStatus || (selected?.status === "Ocupada" ? "Libre" : selected?.status || "Libre");
  root.innerHTML = `
    ${viewHeader("MANTENIMIENTO OPERATIVO", "Limpieza, falla y disponibilidad", "Actualiza el estado de una camilla y registra una observación para el equipo.", `<button class="btn btn-secondary" type="button" data-go="map">▦ Abrir mapa</button>`)}
    <section class="split-layout"><article class="panel-card"><div class="panel-heading"><div><h3>Actualizar camilla</h3><p>Una camilla ocupada se libera al cambiar su estado.</p></div></div>
      ${selected ? `<form id="statusForm" class="form-grid"><div class="form-field full"><label for="statusBed">Camilla</label><select id="statusBed" class="select-input">${state.beds.slice().sort((a,b) => a.code.localeCompare(b.code)).map(bed => `<option value="${bed.id}" ${bed.id === selected.id ? "selected" : ""}>${esc(bed.code)} · ${esc(bed.area)} (${esc(bed.status)})</option>`).join("")}</select></div><div class="form-field full"><label>Estado actual</label><div class="detail-status" style="--status:${statusColor(selected.status)}">${statusMeta(selected.status).icon} ${esc(selected.status)} · ${esc(selected.area)}</div></div><div class="form-field full"><label for="newStatus">Nuevo estado</label><select id="newStatus" class="select-input"><option value="Libre" ${newStatus === "Libre" ? "selected" : ""}>Libre</option><option value="En limpieza" ${newStatus === "En limpieza" ? "selected" : ""}>En limpieza</option><option value="Falla" ${newStatus === "Falla" ? "selected" : ""}>Falla</option></select></div><div class="form-field full"><label for="statusObservation">Observación</label><textarea id="statusObservation" class="textarea-input" placeholder="Describe la limpieza, falla o disponibilidad...">${esc(selected.observation || "")}</textarea><p class="form-hint">El estado Falla requiere una observación.</p></div><div class="inline-actions full"><button class="btn btn-ghost" type="button" data-status-shortcut="En limpieza">◌ Marcar limpieza</button><button class="btn btn-ghost" type="button" data-status-shortcut="Falla">! Registrar falla</button><button class="btn btn-ghost" type="button" data-status-shortcut="Libre">✓ Marcar libre</button></div><div class="form-field full"><button class="btn btn-primary btn-full" type="submit">Guardar cambio de estado</button></div></form>` : emptyState("No hay camillas", "Agrega una camilla para gestionar su estado.")}</article>
      <article class="panel-card"><div class="panel-heading"><div><h3>Resumen de camillas</h3><p>Selecciona una fila para cargarla en el formulario.</p></div></div>${bedStatusRows()}</article></section>`;
  bindCommonActions(root);
  $("#statusBed")?.addEventListener("change", event => { viewContext.selectedBedId = Number(event.target.value); viewContext.newStatus = ""; renderStatus(); });
  root.querySelectorAll("[data-status-shortcut]").forEach(button => button.addEventListener("click", () => {
    $("#newStatus").value = button.dataset.statusShortcut;
    if (button.dataset.statusShortcut === "Falla") $("#statusObservation").focus();
  }));
  $("#statusForm")?.addEventListener("submit", event => {
    event.preventDefault();
    requestStatusChange(selected.id, $("#newStatus").value, $("#statusObservation").value.trim());
  });
  root.querySelectorAll("[data-select-status-bed]").forEach(button => button.addEventListener("click", () => { viewContext.selectedBedId = Number(button.dataset.selectStatusBed); renderStatus(); }));
}

function bedStatusRows() {
  return `<div class="table-wrap"><table class="data-table"><thead><tr><th>Camilla</th><th>Estado</th><th>Observación</th><th></th></tr></thead><tbody>${state.beds.slice().sort((a,b) => a.code.localeCompare(b.code)).map(bed => `<tr><td><span class="cell-title">${esc(bed.code)}</span><span class="cell-subtitle">${esc(bed.area)}</span></td><td>${statusPill(bed.status)}</td><td>${esc(bed.observation || "-")}</td><td><button class="row-btn" type="button" data-select-status-bed="${bed.id}">Seleccionar</button></td></tr>`).join("")}</tbody></table></div>`;
}

function triageColor(level) { return TRIAGE_LEVELS.find(item => item.value === level)?.color || "#b4bfd0"; }
function triageTime(level) { return TRIAGE_LEVELS.find(item => item.value === level)?.time || "-"; }

function renderTriage() {
  const root = $("#viewRoot");
  const triages = state.triages.slice().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  root.innerHTML = `
    ${viewHeader("PRIORIDAD DE ATENCIÓN", "Registro de triaje", "Registra signos vitales y prioridad de atención de cada paciente.", `<button class="btn btn-secondary" type="button" data-go="patients">♙ Ver pacientes</button>`)}
    <section class="split-layout"><article class="panel-card"><div class="panel-heading"><div><h3>Nuevo registro</h3><p>Completa la información antes de guardar el triaje.</p></div></div>${state.patients.length ? `<form id="triageForm" class="form-grid"><div class="form-field full"><label for="triagePatient">Paciente</label><select id="triagePatient" class="select-input">${state.patients.slice().sort((a,b) => a.name.localeCompare(b.name)).map(patient => `<option value="${patient.id}">${esc(patient.name)} · CI ${esc(patient.document)}</option>`).join("")}</select></div><div class="form-field"><label for="triageBp">Presión arterial</label><input id="triageBp" class="text-input" placeholder="Ej. 120/80" required></div><div class="form-field"><label for="triageTemp">Temperatura °C</label><input id="triageTemp" class="text-input" type="number" min="30" max="45" step="0.1" value="36.5" required></div><div class="form-field"><label for="triageHeart">Frecuencia cardíaca</label><input id="triageHeart" class="text-input" type="number" min="20" max="250" value="80" required></div><div class="form-field"><label for="triageLevel">Nivel de prioridad</label><select id="triageLevel" class="select-input">${TRIAGE_LEVELS.map(level => `<option value="${esc(level.value)}">${esc(level.value)} · ${esc(level.time)}</option>`).join("")}</select></div><div class="form-field full"><label for="triageNotes">Notas</label><textarea id="triageNotes" class="textarea-input" placeholder="Observaciones de la atención..."></textarea></div><div class="form-field full"><button class="btn btn-primary btn-full" type="submit">✚ Guardar triaje</button></div></form>` : emptyState("Primero registra un paciente", "Necesitas al menos un paciente para registrar triaje.", "＋ Nuevo paciente", "new-patient")}</article>
      <article class="panel-card"><div class="panel-heading"><div><h3>Historial reciente</h3><p>${triages.length} registro(s) guardado(s).</p></div></div>${triages.length ? triageTable(triages) : emptyState("Sin triajes registrados", "Los registros aparecerán aquí.")}</article></section>`;
  bindCommonActions(root);
  $("#triageForm")?.addEventListener("submit", event => {
    event.preventDefault();
    const patientId = Number($("#triagePatient").value);
    const bloodPressure = $("#triageBp").value.trim();
    if (!bloodPressure) { showToast("Falta información", "Ingresa la presión arterial.", "warning"); return; }
    state.triages.push({ id: nextId(state.triages), patientId, level: $("#triageLevel").value, bloodPressure, temperature: Number($("#triageTemp").value), heartRate: Number($("#triageHeart").value), notes: $("#triageNotes").value.trim(), createdAt: new Date().toISOString() });
    addActivity("Registro de triaje", `${patientName(patientId)} · ${$("#triageLevel").value}`, "✚");
    showToast("Triaje guardado", "El registro se añadió correctamente.");
    renderTriage();
  });
  root.querySelectorAll("[data-triage-action]").forEach(button => button.addEventListener("click", () => handleTriageAction(button.dataset.triageAction, Number(button.dataset.id))));
}

function triageTable(triages) {
  return `<div class="table-wrap"><table class="data-table"><thead><tr><th>Fecha</th><th>Paciente</th><th>Nivel</th><th>Signos</th><th>Acciones</th></tr></thead><tbody>${triages.map(triage => `<tr><td>${esc(dateText(triage.createdAt))}</td><td><span class="cell-title">${esc(patientName(triage.patientId))}</span><span class="cell-subtitle">${esc(triage.notes || "Sin notas")}</span></td><td><span class="status-pill" style="--status:${triageColor(triage.level)}">${esc(triage.level.split(" - ").slice(0, 2).join(" - "))}</span><span class="cell-subtitle">${esc(triageTime(triage.level))}</span></td><td>PA ${esc(triage.bloodPressure)}<span class="cell-subtitle">T ${esc(triage.temperature)}° · FC ${esc(triage.heartRate)}</span></td><td><div class="row-actions"><button class="row-btn" type="button" data-triage-action="details" data-id="${triage.id}">i</button>${isAdmin() ? `<button class="row-btn" type="button" data-triage-action="delete" data-id="${triage.id}">×</button>` : ""}</div></td></tr>`).join("")}</tbody></table></div>`;
}

function renderReports() {
  const root = $("#viewRoot");
  const currentAlerts = activeAlerts();
  root.innerHTML = `
    ${viewHeader("INFORMACIÓN DEL SISTEMA", "Reportes operativos", "Exporta la información del sistema y revisa la actividad registrada.", `<button class="btn btn-secondary" type="button" data-export="beds">⇩ Camillas CSV</button><button class="btn btn-primary" type="button" data-export="patients">⇩ Pacientes CSV</button>`)}
    <section class="report-grid"><article class="report-kpi"><b>${state.beds.length}</b><span>Camillas registradas</span></article><article class="report-kpi"><b>${state.patients.length}</b><span>Pacientes registrados</span></article><article class="report-kpi"><b>${state.triages.length}</b><span>Triajes guardados</span></article></section>
    <section class="split-layout"><article class="panel-card"><div class="panel-heading"><div><h3>Estado de camillas</h3><p>Resumen por disponibilidad.</p></div><button class="btn btn-ghost" type="button" data-export="beds">Exportar CSV</button></div>${bedTable(state.beds.slice().sort((a,b) => a.code.localeCompare(b.code)))}</article><article class="panel-card"><div class="panel-heading"><div><h3>Alertas activas</h3><p>Situaciones que requieren seguimiento.</p></div><span class="badge ${currentAlerts.some(alert => alert.type === "danger") ? "danger" : "good"}">${currentAlerts.length} alerta(s)</span></div>${alertsList(currentAlerts)}</article></section>
    <section class="panel-card" style="margin-top:14px"><div class="panel-heading"><div><h3>Auditoría de acciones</h3><p>Historial de cambios y registros del sistema.</p></div><button class="btn btn-ghost" type="button" data-export="activities">Exportar CSV</button></div>${activityList(state.activities)}</section>`;
  bindCommonActions(root);
  root.querySelectorAll("[data-export]").forEach(button => button.addEventListener("click", () => exportCsv(button.dataset.export)));
  root.querySelectorAll("[data-bed-action]").forEach(button => button.addEventListener("click", () => handleBedAction(button.dataset.bedAction, Number(button.dataset.id))));
}

function alertsList(alerts) {
  if (!alerts.length) return `<div class="empty-state"><span>✓</span><b>Sin alertas críticas</b><p>Las camillas se encuentran en una condición operativa estable.</p></div>`;
  return `<div class="activity-list">${alerts.map(alert => `<article class="activity-row"><span class="activity-icon">${esc(alert.icon)}</span><div><b>${esc(alert.title)}</b><small>${esc(alert.detail)}</small></div><span class="badge ${alert.type === "danger" ? "danger" : "warn"}">${alert.type === "danger" ? "Revisar" : "En proceso"}</span></article>`).join("")}</div>`;
}

function renderSettings() {
  const root = $("#viewRoot");
  root.innerHTML = `
    ${viewHeader("ADMINISTRACIÓN", "Herramientas del sistema", "Respalda, restaura o reinicia los datos usados en esta demostración.")}
    <section class="tool-grid"><article class="tool-card"><span class="tool-icon">⇩</span><h3>Crear respaldo</h3><p>Descarga una copia de todos los datos en formato JSON.</p><button id="backupButton" class="btn btn-primary" type="button">Descargar respaldo</button></article><article class="tool-card"><span class="tool-icon">⇧</span><h3>Restaurar respaldo</h3><p>Importa un archivo JSON creado desde esta misma página.</p><input id="restoreFile" type="file" accept="application/json" hidden><button id="restoreButton" class="btn btn-secondary" type="button">Seleccionar archivo</button></article><article class="tool-card"><span class="tool-icon">↻</span><h3>Restablecer demostración</h3><p>Vuelve a cargar los datos iniciales de camillas, pacientes y triaje.</p><button id="resetDataButton" class="btn btn-danger" type="button">Restablecer datos</button></article><article class="tool-card"><span class="tool-icon">i</span><h3>Acerca de la versión web</h3><p>Aplicación HTML, CSS y JavaScript sin instalar una base de datos.</p><button id="aboutButton" class="btn btn-ghost" type="button">Ver información</button></article></section>`;
  $("#backupButton").addEventListener("click", exportBackup);
  $("#restoreButton").addEventListener("click", () => $("#restoreFile").click());
  $("#restoreFile").addEventListener("change", importBackup);
  $("#resetDataButton").addEventListener("click", () => openConfirmModal("Restablecer datos", "Se eliminarán los cambios locales de esta demostración y se volverán a cargar los datos iniciales.", "Restablecer", true, () => {
    state = seedState();
    addActivity("Datos restablecidos", "Se recuperó la información inicial de demostración", "↻");
    showToast("Datos restablecidos", "La demostración volvió a su estado inicial.");
    navigate("dashboard");
  }));
  $("#aboutButton").addEventListener("click", () => openModal("Acerca del proyecto", `<p class="modal-note">Sistema Hospitalario PRO V4 es una versión web del sistema de gestión de camillas. Está desarrollada únicamente con <b>HTML, CSS y JavaScript</b>, por eso puedes abrirla directamente en Visual Studio Code o en tu navegador.</p><div class="info-list"><div><small>MÓDULOS</small><b>Camillas, pacientes, asignación, limpieza/falla, triaje, reportes y herramientas.</b></div><div><small>ALMACENAMIENTO</small><b>Los cambios se guardan localmente en este navegador.</b></div><div><small>USUARIO RECOMENDADO</small><b>admin / 1234</b></div></div>`));
}

function emptyState(title, detail, buttonText = "", action = "") {
  return `<div class="empty-state"><div><span>◌</span><b>${esc(title)}</b><p>${esc(detail)}</p>${buttonText ? `<button class="btn btn-primary" type="button" data-action="${esc(action)}">${esc(buttonText)}</button>` : ""}</div></div>`;
}

function bindCommonActions(root) {
  root.querySelectorAll("[data-go]").forEach(button => button.addEventListener("click", () => navigate(button.dataset.go, { status: button.dataset.status || "Todos", selectedBedId: button.dataset.id ? Number(button.dataset.id) : undefined })));
  root.querySelectorAll("[data-action]").forEach(button => button.addEventListener("click", () => handleAction(button.dataset.action)));
}

function handleAction(action) {
  const actions = {
    "open-map": () => navigate("map"),
    "open-free-beds": () => navigate("beds", { status: "Libre" }),
    "open-assignment": () => navigate("assignment"),
    "new-patient": () => openPatientModal(),
    "open-status": () => navigate("status"),
    "open-triage": () => navigate("triage"),
    "new-bed": () => openBedModal(),
    "open-alerts": () => openAlertsModal(),
    "quick-actions": () => openQuickActions()
  };
  actions[action]?.();
}

function handleBedAction(action, id) {
  const bed = getBed(id);
  if (!bed) return;
  if (action === "details") openBedDetails(bed);
  if (action === "map") navigate("map", { selectedBedId: id });
  if (action === "edit" && isAdmin()) openBedModal(bed);
  if (action === "delete" && isAdmin()) requestBedDelete(bed);
  if (action === "assign" && can("assign")) openAssignModal(bed);
  if (action === "release" && can("assign")) requestRelease(bed);
  if (action === "status" && can("status")) navigate("status", { selectedBedId: id });
}

function handlePatientAction(action, id) {
  const patient = getPatient(id);
  if (!patient) return;
  if (action === "details") openPatientDetails(patient);
  if (action === "edit") openPatientModal(patient);
  if (action === "delete" && isAdmin()) requestPatientDelete(patient);
}

function handleTriageAction(action, id) {
  const triage = state.triages.find(item => item.id === id);
  if (!triage) return;
  if (action === "details") openTriageDetails(triage);
  if (action === "delete" && isAdmin()) openConfirmModal("Eliminar triaje", "Este registro será eliminado de forma local.", "Eliminar", true, () => {
    state.triages = state.triages.filter(item => item.id !== id);
    addActivity("Triaje eliminado", `${patientName(triage.patientId)} · ${triage.level}`, "×");
    showToast("Triaje eliminado", "El registro fue eliminado.");
    renderTriage();
  });
}

function openModal(title, body, eyebrow = "HOSPITAL PRO V4") {
  $("#modalEyebrow").textContent = eyebrow;
  $("#modalTitle").textContent = title;
  $("#modalBody").innerHTML = body;
  const modal = $("#appModal");
  if (!modal.open) modal.showModal();
}

function closeModal() {
  const modal = $("#appModal");
  if (modal.open) modal.close();
}

function openConfirmModal(title, message, confirmLabel, danger, onConfirm) {
  openModal(title, `<div class="confirmation-icon">!</div><p class="modal-note">${esc(message)}</p><div class="form-actions"><button class="btn btn-ghost" type="button" data-close-modal>Cancelar</button><button id="confirmModalButton" class="btn ${danger ? "btn-danger" : "btn-primary"}" type="button">${esc(confirmLabel)}</button></div>`, "CONFIRMACIÓN REQUERIDA");
  $("#confirmModalButton").addEventListener("click", () => { closeModal(); onConfirm(); });
  $("[data-close-modal]", $("#modalBody"))?.addEventListener("click", closeModal);
}

function openBedDetails(bed) {
  const patient = getPatient(bed.patientId);
  openModal(`Detalle de ${bed.code}`, `<div class="info-list"><div><small>ÁREA</small><b>${esc(bed.area)}</b></div><div><small>ESTADO</small><span class="detail-status" style="--status:${statusColor(bed.status)}">${statusMeta(bed.status).icon} ${esc(bed.status)}</span></div><div><small>PACIENTE ASIGNADO</small><b>${esc(patient?.name || "Sin paciente asignado")}</b></div><div><small>OBSERVACIÓN</small><b>${esc(bed.observation || "Sin observación")}</b></div><div><small>ACTUALIZADO</small><b>${esc(dateText(bed.updatedAt))}</b></div></div><div class="form-actions"><button class="btn btn-ghost" type="button" data-close-modal>Cerrar</button><button id="detailMapButton" class="btn btn-primary" type="button">Abrir en mapa</button></div>`);
  $("#detailMapButton").addEventListener("click", () => { closeModal(); navigate("map", { selectedBedId: bed.id }); });
  $("[data-close-modal]", $("#modalBody"))?.addEventListener("click", closeModal);
}

function openBedModal(existing = null) {
  if (!isAdmin()) { showToast("Acceso restringido", "Solo el administrador puede modificar camillas.", "warning"); return; }
  openModal(existing ? "Editar camilla" : "Nueva camilla", `<form id="bedForm" class="form-grid"><div class="form-field"><label for="bedCode">Código de camilla</label><input id="bedCode" class="text-input" value="${esc(existing?.code || "")}" placeholder="Ej. CAMILLA 009" required></div><div class="form-field"><label for="bedArea">Área / ubicación</label><input id="bedArea" class="text-input" value="${esc(existing?.area || "")}" placeholder="Ej. Box 4" required></div><div class="form-field full"><label for="bedObservation">Observación inicial</label><textarea id="bedObservation" class="textarea-input" placeholder="Ej. Lista para asignación">${esc(existing?.observation || "Lista para asignación")}</textarea></div><div class="form-actions full"><button class="btn btn-ghost" type="button" data-close-modal>Cancelar</button><button class="btn btn-primary" type="submit">${existing ? "Guardar cambios" : "Crear camilla"}</button></div></form>`);
  $("#bedForm").addEventListener("submit", event => {
    event.preventDefault();
    const code = $("#bedCode").value.trim().toUpperCase();
    const area = $("#bedArea").value.trim();
    const observation = $("#bedObservation").value.trim() || "Lista para asignación";
    if (state.beds.some(bed => bed.id !== existing?.id && bed.code.toLowerCase() === code.toLowerCase())) { showToast("Código duplicado", "Ya existe una camilla con ese código.", "warning"); return; }
    if (existing) {
      existing.code = code; existing.area = area; existing.observation = observation; existing.updatedAt = new Date().toISOString();
      addActivity("Camilla editada", `${code} · ${area}`, "✎");
      showToast("Camilla actualizada", "Los datos fueron guardados.");
    } else {
      state.beds.push({ id: nextId(state.beds), code, area, status: "Libre", patientId: null, observation, updatedAt: new Date().toISOString() });
      addActivity("Camilla registrada", `${code} · ${area}`, "＋");
      showToast("Camilla creada", "La nueva camilla está disponible.");
    }
    closeModal();
    renderCurrentView();
  });
  $("[data-close-modal]", $("#modalBody"))?.addEventListener("click", closeModal);
}

function requestBedDelete(bed) {
  if (bed.status !== "Libre") { showToast("No se puede eliminar", "Solo se puede eliminar una camilla que esté Libre.", "warning"); return; }
  openConfirmModal("Eliminar camilla", `¿Deseas eliminar ${bed.code}? Esta acción solo afecta los datos locales.`, "Eliminar camilla", true, () => {
    state.beds = state.beds.filter(item => item.id !== bed.id);
    addActivity("Camilla eliminada", bed.code, "×");
    showToast("Camilla eliminada", `${bed.code} fue eliminada.`);
    navigate("beds");
  });
}

function openPatientModal(existing = null) {
  if (!can("patients")) { showToast("Acceso restringido", "Tu rol no puede administrar pacientes.", "warning"); return; }
  openModal(existing ? "Editar paciente" : "Nuevo paciente", `<form id="patientForm" class="form-grid"><div class="form-field"><label for="patientDocument">CI / Documento</label><input id="patientDocument" class="text-input" value="${esc(existing?.document || "")}" required></div><div class="form-field"><label for="patientName">Nombre completo</label><input id="patientName" class="text-input" value="${esc(existing?.name || "")}" required></div><div class="form-field"><label for="patientAge">Edad</label><input id="patientAge" class="text-input" type="number" min="0" max="120" value="${esc(existing?.age ?? "")}" required></div><div class="form-field"><label for="patientSex">Sexo</label><select id="patientSex" class="select-input"><option ${existing?.sex === "Masculino" ? "selected" : ""}>Masculino</option><option ${existing?.sex === "Femenino" ? "selected" : ""}>Femenino</option><option ${existing?.sex === "Otro" ? "selected" : ""}>Otro</option></select></div><div class="form-field full"><label for="patientPhone">Teléfono</label><input id="patientPhone" class="text-input" value="${esc(existing?.phone || "")}" placeholder="Ej. 70701010"></div><div class="form-actions full"><button class="btn btn-ghost" type="button" data-close-modal>Cancelar</button><button class="btn btn-primary" type="submit">${existing ? "Guardar cambios" : "Registrar paciente"}</button></div></form>`);
  $("#patientForm").addEventListener("submit", event => {
    event.preventDefault();
    const documentId = $("#patientDocument").value.trim();
    const name = $("#patientName").value.trim();
    if (state.patients.some(patient => patient.id !== existing?.id && patient.document.toLowerCase() === documentId.toLowerCase())) { showToast("Documento duplicado", "Ya existe un paciente con ese CI o documento.", "warning"); return; }
    if (existing) {
      Object.assign(existing, { document: documentId, name, age: Number($("#patientAge").value), sex: $("#patientSex").value, phone: $("#patientPhone").value.trim() });
      addActivity("Paciente editado", `${name} · CI ${documentId}`, "✎");
      showToast("Paciente actualizado", "Los datos fueron guardados.");
    } else {
      state.patients.push({ id: nextId(state.patients), document: documentId, name, age: Number($("#patientAge").value), sex: $("#patientSex").value, phone: $("#patientPhone").value.trim(), createdAt: new Date().toISOString() });
      addActivity("Paciente registrado", `${name} · CI ${documentId}`, "＋");
      showToast("Paciente registrado", "El paciente ya puede ser asignado.");
    }
    closeModal();
    renderCurrentView();
  });
  $("[data-close-modal]", $("#modalBody"))?.addEventListener("click", closeModal);
}

function openPatientDetails(patient) {
  const bed = state.beds.find(item => item.patientId === patient.id && item.status === "Ocupada");
  const triages = state.triages.filter(item => item.patientId === patient.id).length;
  openModal(`Paciente: ${patient.name}`, `<div class="info-list"><div><small>CI / DOCUMENTO</small><b>${esc(patient.document)}</b></div><div><small>EDAD Y SEXO</small><b>${esc(patient.age)} años · ${esc(patient.sex)}</b></div><div><small>TELÉFONO</small><b>${esc(patient.phone || "-")}</b></div><div><small>CAMILLA ACTUAL</small><b>${esc(bed ? `${bed.code} · ${bed.area}` : "Sin camilla asignada")}</b></div><div><small>TRIAJES REGISTRADOS</small><b>${triages}</b></div></div><div class="form-actions"><button class="btn btn-ghost" type="button" data-close-modal>Cerrar</button>${can("assign") && !bed ? `<button id="patientAssignButton" class="btn btn-primary" type="button">Asignar camilla</button>` : ""}</div>`);
  $("#patientAssignButton")?.addEventListener("click", () => { closeModal(); openAssignModal(null, patient.id); });
  $("[data-close-modal]", $("#modalBody"))?.addEventListener("click", closeModal);
}

function requestPatientDelete(patient) {
  const hasBed = state.beds.some(bed => bed.patientId === patient.id);
  const hasTriage = state.triages.some(triage => triage.patientId === patient.id);
  if (hasBed || hasTriage) { showToast("No se puede eliminar", "El paciente tiene una camilla asignada o historial de triaje.", "warning"); return; }
  openConfirmModal("Eliminar paciente", `¿Deseas eliminar a ${patient.name}?`, "Eliminar paciente", true, () => {
    state.patients = state.patients.filter(item => item.id !== patient.id);
    addActivity("Paciente eliminado", patient.name, "×");
    showToast("Paciente eliminado", "El registro fue eliminado.");
    renderPatients();
  });
}

function openAssignModal(selectedBed = null, selectedPatientId = null) {
  if (!can("assign")) { showToast("Acceso restringido", "Tu rol no puede asignar camillas.", "warning"); return; }
  const beds = state.beds.filter(bed => bed.status === "Libre");
  const patients = state.patients.filter(patient => !state.beds.some(bed => bed.patientId === patient.id && bed.status === "Ocupada"));
  if (!beds.length || !patients.length) { showToast("Asignación no disponible", !beds.length ? "No hay camillas libres." : "No hay pacientes disponibles.", "warning"); return; }
  openModal("Asignar camilla", `<p class="modal-note">Selecciona el paciente y la camilla que deseas asignar.</p><form id="assignModalForm" class="form-grid"><div class="form-field full"><label for="modalAssignPatient">Paciente</label><select id="modalAssignPatient" class="select-input">${patients.map(patient => `<option value="${patient.id}" ${patient.id === selectedPatientId ? "selected" : ""}>${esc(patient.name)} · CI ${esc(patient.document)}</option>`).join("")}</select></div><div class="form-field full"><label for="modalAssignBed">Camilla libre</label><select id="modalAssignBed" class="select-input">${beds.map(bed => `<option value="${bed.id}" ${bed.id === selectedBed?.id ? "selected" : ""}>${esc(bed.code)} · ${esc(bed.area)}</option>`).join("")}</select></div><div class="form-actions full"><button class="btn btn-ghost" type="button" data-close-modal>Cancelar</button><button class="btn btn-primary" type="submit">Confirmar asignación</button></div></form>`);
  $("#assignModalForm").addEventListener("submit", event => { event.preventDefault(); closeModal(); requestAssignment(Number($("#modalAssignPatient").value), Number($("#modalAssignBed").value)); });
  $("[data-close-modal]", $("#modalBody"))?.addEventListener("click", closeModal);
}

function requestAssignment(patientId, bedId) {
  const patient = getPatient(patientId);
  const bed = getBed(bedId);
  if (!patient || !bed || bed.status !== "Libre") { showToast("Asignación inválida", "La camilla ya no está disponible o el paciente no existe.", "warning"); renderCurrentView(); return; }
  if (state.beds.some(item => item.patientId === patient.id && item.status === "Ocupada")) { showToast("Paciente con camilla", "El paciente ya tiene una camilla asignada.", "warning"); return; }
  openConfirmModal("Confirmar asignación", `¿Asignar ${bed.code} a ${patient.name}?`, "Asignar camilla", false, () => {
    bed.status = "Ocupada"; bed.patientId = patient.id; bed.observation = "Asignada a paciente"; bed.updatedAt = new Date().toISOString();
    addActivity("Asignación de camilla", `${bed.code} asignada a ${patient.name}`, "⇄");
    showToast("Asignación correcta", `${bed.code} fue asignada a ${patient.name}.`);
    renderCurrentView();
  });
}

function requestRelease(bed) {
  if (bed.status !== "Ocupada") { showToast("Acción no disponible", "Esta camilla no está ocupada.", "warning"); return; }
  const patient = getPatient(bed.patientId);
  openConfirmModal("Liberar camilla", `¿Liberar ${bed.code}${patient ? ` asignada a ${patient.name}` : ""}?`, "Liberar camilla", true, () => {
    bed.status = "Libre"; bed.patientId = null; bed.observation = "Liberada y lista para asignación"; bed.updatedAt = new Date().toISOString();
    addActivity("Liberación de camilla", `${bed.code}${patient ? ` · ${patient.name}` : ""}`, "↩");
    showToast("Camilla liberada", `${bed.code} vuelve a estar disponible.`);
    renderCurrentView();
  });
}

function requestStatusChange(bedId, newStatus, observation) {
  const bed = getBed(bedId);
  if (!bed) return;
  if (newStatus === "Falla" && !observation) { showToast("Falta observación", "Describe la falla de la camilla.", "warning"); return; }
  const apply = () => {
    const previous = bed.status;
    bed.status = newStatus; bed.patientId = null; bed.observation = observation || (newStatus === "Libre" ? "Disponible para asignación" : "Limpieza en proceso"); bed.updatedAt = new Date().toISOString();
    addActivity("Cambio de estado", `${bed.code}: ${previous} → ${newStatus}`, newStatus === "Falla" ? "!" : "◌");
    showToast("Estado actualizado", `${bed.code} ahora está ${newStatus}.`);
    viewContext = { selectedBedId: bed.id };
    renderStatus();
  };
  if (bed.status === "Ocupada") openConfirmModal("Liberar paciente", `${bed.code} está ocupada. Este cambio liberará la camilla del paciente asignado.`, "Cambiar estado", true, apply);
  else apply();
}

function openTriageDetails(triage) {
  openModal("Detalle de triaje", `<div class="info-list"><div><small>PACIENTE</small><b>${esc(patientName(triage.patientId))}</b></div><div><small>NIVEL</small><span class="detail-status" style="--status:${triageColor(triage.level)}">${esc(triage.level)} · ${esc(triageTime(triage.level))}</span></div><div><small>SIGNOS VITALES</small><b>PA ${esc(triage.bloodPressure)} · T ${esc(triage.temperature)}°C · FC ${esc(triage.heartRate)}</b></div><div><small>NOTAS</small><b>${esc(triage.notes || "Sin notas")}</b></div><div><small>FECHA</small><b>${esc(dateText(triage.createdAt))}</b></div></div><div class="form-actions"><button class="btn btn-ghost" type="button" data-close-modal>Cerrar</button></div>`);
  $("[data-close-modal]", $("#modalBody"))?.addEventListener("click", closeModal);
}

function openQuickActions() {
  openModal("Acciones rápidas", `<p class="modal-note">Elige el módulo al que deseas ingresar. Las acciones se muestran según los permisos de tu usuario.</p><div class="quick-action-grid">${quickAction("Mapa completo", "Ver todas las camillas", "▦", "open-map")}${quickAction("Camillas libres", "Disponibilidad actual", "✓", "open-free-beds")}${quickAction("Asignar paciente", "Nueva asignación", "⇄", "open-assignment", can("assign"))}${quickAction("Nuevo paciente", "Registrar información", "♙", "new-patient", can("patients"))}${quickAction("Limpieza / falla", "Cambiar estado", "⚙", "open-status", can("status"))}${quickAction("Registrar triaje", "Priorizar atención", "✚", "open-triage", can("triage"))}</div>`);
  $("#modalBody").querySelectorAll("[data-action]").forEach(button => button.addEventListener("click", () => {
    const action = button.dataset.action;
    closeModal();
    handleAction(action);
  }));
}

function openAlertsModal() {
  const alerts = activeAlerts();
  openModal("Alertas del sistema", `<p class="modal-note">Estas son las situaciones que necesitan una revisión operativa.</p>${alertsList(alerts)}<div class="form-actions"><button class="btn btn-ghost" type="button" data-close-modal>Cerrar</button><button id="alertsMapButton" class="btn btn-primary" type="button">Abrir mapa</button></div>`, "MONITOREO OPERATIVO");
  $("#alertsMapButton").addEventListener("click", () => { closeModal(); navigate("map"); });
  $("[data-close-modal]", $("#modalBody"))?.addEventListener("click", closeModal);
}

function exportCsv(type) {
  const definitions = {
    beds: { name: "reporte_camillas.csv", columns: ["Código", "Área", "Estado", "Paciente", "Observación", "Actualizado"], rows: state.beds.map(bed => [bed.code, bed.area, bed.status, bed.patientId ? patientName(bed.patientId) : "", bed.observation, dateText(bed.updatedAt)]) },
    patients: { name: "reporte_pacientes.csv", columns: ["CI", "Nombre", "Edad", "Sexo", "Teléfono", "Registrado"], rows: state.patients.map(patient => [patient.document, patient.name, patient.age, patient.sex, patient.phone, dateText(patient.createdAt)]) },
    activities: { name: "reporte_auditoria.csv", columns: ["Fecha", "Usuario", "Acción", "Detalle"], rows: state.activities.map(item => [dateText(item.at), item.user, item.action, item.detail]) }
  };
  const report = definitions[type];
  if (!report) return;
  const csv = [report.columns, ...report.rows].map(row => row.map(cell => `"${String(cell ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
  downloadFile(report.name, `\uFEFF${csv}`, "text/csv;charset=utf-8");
  addActivity("Reporte exportado", report.name, "⇩");
  showToast("Reporte descargado", "El archivo CSV se generó correctamente.");
}

function exportBackup() {
  downloadFile("respaldo_hospital_pro_v4.json", JSON.stringify(state, null, 2), "application/json");
  addActivity("Respaldo creado", "Archivo JSON descargado", "⇩");
  showToast("Respaldo descargado", "Guárdalo en una carpeta segura.");
}

function downloadFile(name, content, type) {
  const url = URL.createObjectURL(new Blob([content], { type }));
  const link = document.createElement("a");
  link.href = url; link.download = name; document.body.append(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 500);
}

function importBackup(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    try {
      const imported = JSON.parse(reader.result);
      if (!Array.isArray(imported.beds) || !Array.isArray(imported.patients) || !Array.isArray(imported.triages)) throw new Error("Formato no válido");
      openConfirmModal("Restaurar respaldo", "Los datos locales actuales se reemplazarán por el contenido del archivo seleccionado.", "Restaurar", true, () => {
        state = { ...seedState(), ...imported, activities: Array.isArray(imported.activities) ? imported.activities : [] };
        addActivity("Respaldo restaurado", file.name, "⇧");
        showToast("Respaldo restaurado", "Los datos fueron cargados correctamente.");
        navigate("dashboard");
      });
    } catch (_) { showToast("Archivo no válido", "Selecciona un respaldo JSON creado desde esta aplicación.", "error"); }
  };
  reader.readAsText(file);
  event.target.value = "";
}

function initEvents() {
  $("#loginForm").addEventListener("submit", event => { event.preventDefault(); login($("#loginUser").value, $("#loginPassword").value); });
  $$("[data-demo]").forEach(button => button.addEventListener("click", () => { const user = button.dataset.demo; $("#loginUser").value = user; $("#loginPassword").value = "1234"; login(user, "1234"); }));
  $$(".nav-item[data-view]").forEach(button => button.addEventListener("click", () => navigate(button.dataset.view)));
  $("#logoutButton").addEventListener("click", () => openConfirmModal("Cerrar sesión", "¿Deseas salir de la sesión actual?", "Cerrar sesión", false, logout));
  $("#quickActionsButton").addEventListener("click", openQuickActions);
  $("#notificationButton").addEventListener("click", openAlertsModal);
  $("#openMobileNav").addEventListener("click", () => $("#sidebar").classList.add("open"));
  $("#closeMobileNav").addEventListener("click", () => $("#sidebar").classList.remove("open"));
  $("#appModal").addEventListener("click", event => { if (event.target === $("#appModal")) closeModal(); });
  $("[data-close-modal]", $("#appModal")).addEventListener("click", closeModal);
  $("#globalSearch").addEventListener("keydown", event => {
    if (event.key === "Enter") {
      event.preventDefault();
      navigate("map", { query: event.target.value.trim() });
      event.target.value = "";
    }
  });
  document.addEventListener("keydown", event => {
    if (event.key === "/" && session && !["INPUT", "TEXTAREA", "SELECT"].includes(document.activeElement.tagName)) { event.preventDefault(); $("#globalSearch").focus(); }
    if (event.key === "Escape") $("#sidebar").classList.remove("open");
  });
}

initEvents();
