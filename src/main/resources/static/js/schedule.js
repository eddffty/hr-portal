import {planToolbar} from './planner.js';
import {store} from './store.js';
import {esc, date, time, period, place, button, empty} from './common.js';

export function addDays(value, count) {
    const d = new Date(value + 'T12:00:00');
    d.setDate(d.getDate() + count);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function weekNumber(value) {
    const d = new Date(value + 'T00:00:00Z');
    d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay() || 7));
    return Math.ceil((((d - new Date(Date.UTC(d.getUTCFullYear(), 0, 1))) / 86400000) + 1) / 7);
}

export function weekToolbar() {
    return `<div class="toolbar">${button('‹', 'week', -1)}<strong>${date(store.data.weekStart)} — ${date(addDays(store.data.weekStart, 5))}</strong>${button('›', 'week', 1)}<span class="spacer"></span><span class="muted">${weekNumber(store.data.weekStart) % 2 === 0 ? 'Чётная' : 'Нечётная'} неделя</span></div>`;
}

export function kind(s) {
    if (s.locked) return s.owner === store.profile.login ? 'mine' : 'pending';
    if (!s.owner) return 'free';
    return s.owner === store.profile.login ? 'mine' : 'other';
}

export function matchesFilter(shift) {
    if (store.filter.startsWith('employee:')) return shift.owner === store.filter.slice(9);
    if (store.filter === 'occupied') return Boolean(shift.owner);
    if (store.filter === 'pending') return shift.locked;
    return store.filter === 'all' || kind(shift) === store.filter;
}

export function selectionReason(s) {
    if (s.date < store.data.today) return 'Эта смена уже прошла. Выберите сегодняшнюю или будущую дату.';
    if (s.completed) return 'Это дежурство уже завершено.';
    if (s.locked) return 'По этой смене уже рассматривается заявка.';
    if ((store.availability || []).some(r => r.parity === weekNumber(s.date) % 2 && r.weekday === new Date(s.date + 'T12:00:00').getDay() && r.slot === s.slot)) return 'Вы заняты по личному расписанию.';
    if (s.owner) return s.owner === store.profile.login ? 'Эта смена уже назначена вам.' : 'Смена назначена сотруднику ' + s.ownerName + '.';
    if (store.data.myShifts.some(other => other.date === s.date && other.start < s.end && s.start < other.end)) return 'У вас уже есть дежурство в это время в другом корпусе.';
    return null;
}

export function calendar(propose = false, building = store.building) {
    if (store.planning && building === 'all') return "<h3>Корпус 1</h3>" + calendar(false, 1) + "<h3>Корпус 4</h3>" + calendar(false, 4);
    const shifts = store.data.schedule.filter(s => s.building === building).map(s => Object.hasOwn(store.plan, s.id) ? {
        ...s,
        owner: store.plan[s.id],
        ownerName: store.data.employees.find(p => p.login === store.plan[s.id])?.name || "Свободно"
    } : s);
    if (!shifts.length) return empty('На эту неделю график ещё не подготовлен');
    return `<div class="toolbar">${[1, 4].map(b => `<button data-action="building" data-id="${b}" class="${store.building === b ? 'active' : ''}">Корпус ${b}</button>`).join('')}</div><div class="tablewrap"><table><thead><tr><th>Время</th>${['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб'].map((d, i) => `<th>${d}<small>${date(addDays(store.data.weekStart, i))}</small></th>`).join('')}</tr></thead><tbody>${Array.from({length: 8}, (_, r) => {
        const sample = shifts.find(s => s.slot === r);
        return `<tr><td>${sample ? time(sample.start) + ' — ' + time(sample.end) : ''}</td>${Array.from({length: 6}, (_, d) => {
            const s = shifts.find(s => s.slot === r && s.date === addDays(store.data.weekStart, d));
            if (!s) return '<td>—</td>';
            const k = kind(s), selected = store.selected.has(s.id), hidden = !propose && !matchesFilter(s);
            const reason = selectionReason(s), disabled = propose && Boolean(reason);
            return `<td>${hidden ? '' : `<button class="slot ${selected ? 'selected' : k} ${s.date < store.data.today ? 'past' : ''}" title="${esc(propose && reason ? reason : period(s))}" data-action="${propose ? 'select' : 'slot'}" data-id="${s.id}" ${disabled ? 'disabled' : ''}>${selected ? '✓ Выбрано' : s.locked ? esc(s.ownerName) + ' · На согласовании' : s.completed ? (s.owner ? esc(s.ownerName) + ' ✓' : 'Не назначена') : s.owner ? esc(s.ownerName) : propose && reason ? 'Недоступно' : '+ Выбрать смену'}</button>`}</td>`
        }).join('')}</tr>`
    }).join('')}</tbody></table></div>`;
}

export function scheduleView(propose = false) {
    const employee = store.profile.role === 'EMPLOYEE';
    const scoped = store.data.schedule.filter(s => store.building === 'all' || s.building === store.building);
    const mine = scoped.filter(s => s.owner === store.profile.login && !s.completed).length;
    const assigned = scoped.filter(s => s.owner).length;
    const free = scoped.filter(s => !s.owner && !s.locked && !s.completed).length;
    const filters = employee ? [['all', 'Все смены'], ['mine', 'Мои смены'], ['other', 'Коллеги'], ['free', 'Свободные']] : [['all', 'Все смены'], ['occupied', 'Назначенные'], ['free', 'Свободные'], ['pending', 'На согласовании']];
    const selectedUser = store.filter.startsWith('employee:') ? store.filter.slice(9) : '';
    const employeeSelect = !employee ? '<label class="employee-filter">Сотрудник<select id="schedule-employee"><option value="">Все сотрудники</option>' + store.data.employees.map(p => '<option value="' + p.login + '" ' + (selectedUser === p.login ? 'selected' : '') + '>' + esc(p.name) + '</option>').join('') + '</select></label>' : '';
    const selectedName = store.data.employees.find(p => p.login === selectedUser)?.name;
    const stats = employee ? '<div class="stat">Мои смены на неделе<strong>' + mine + '</strong></div><div class="stat">Плановая выработка<strong>' + Math.floor(mine * 80 / 60) + ' ч ' + mine * 80 % 60 + ' мин</strong></div>' : '<div class="stat">Назначено смен<strong>' + assigned + '</strong></div><div class="stat">Свободно для заявки<strong>' + free + '</strong></div>';
    if (employee && !propose) return '<section class="panel schedule-employee"><div class="schedule-layout"><div class="schedule-filters"><div class="stat">Мои смены на этой неделе<strong>' + mine + '</strong></div>' + filters.map(([f, t]) => '<button data-action="filter" data-id="' + f + '" class="' + (store.filter === f ? 'active' : '') + '">' + t + '</button>').join('') + '<p class="muted">Тёмные — мои смены<br>Зелёные — свободные</p></div><div class="schedule-main">' + weekToolbar() + calendar(false) + '</div></div></section>';
    return (!propose ? '<div class="stats">' + stats + '<div class="stat">' + (store.building === 'all' ? 'Оба корпуса' : 'Корпус ' + store.building) + ' · всего смен<strong>' + scoped.length + '</strong></div></div>' : '') + '<section class="panel">' + weekToolbar() + (store.planning ? planToolbar() : '') + (propose ? '<div class="selection-summary"><div><strong>Выбрано смен: ' + store.selected.size + '</strong><p>Нажмите на зелёную свободную смену, затем подтвердите выбор.</p></div>' + button('Подтвердить выбор' + (store.selected.size ? ' · ' + store.selected.size : ''), 'confirm-proposal', '', true, !store.selected.size) + '</div><div class="slot-legend"><span><i class="legend-dot free"></i>Свободно — можно выбрать</span><span><i class="legend-dot selected"></i>Выбрано</span><span><i class="legend-dot occupied"></i>Занято или недоступно</span></div>' : '') + (!propose ? '<div class="toolbar">' + filters.map(([f, t]) => '<button data-action="filter" data-id="' + f + '" class="' + (store.filter === f ? 'active' : '') + '">' + t + '</button>').join('') + employeeSelect + (!employee && !store.planning ? button('Составить расписание', 'plan-start', '', true) : '') + (store.profile.role === 'SCHEDULER' ? button('Добавить смену', 'new-shift') + button('Опубликовать расписание', 'publish-schedule', '', true) : '') + '</div><p class="schedule-context">Общее расписание · Корпус ' + store.building + ' · ' + (selectedName ? esc(selectedName) : store.filter === 'all' ? 'Все сотрудники' : 'Выбран фильтр: ' + esc(filters.find(([f]) => f === store.filter)?.[1] || store.filter)) + '</p>' + (employee ? '<p class="schedule-legend"><span class="legend-dot mine"></span>Тёмным выделены ваши смены. Светлым — смены коллег.</p>' : '<p class="schedule-legend">Показаны назначения сотрудников из общего графика.</p>') : '') + calendar(propose) + (propose ? '<label>Комментарий<textarea id="proposal-comment" maxlength="2000" placeholder="Комментарий для руководителя">' + esc(store.comment) + '</textarea></label>' : '') + '</section>';
}

export function slotDialog(s) {
    if (store.profile.role === 'SCHEDULER') return `<h2>Назначение дежурства</h2><p>${period(s)}</p><p>${place(s)}</p><form id="assignment-form" data-id="${s.id}"><label>Сотрудник<select name="owner"><option value="">Свободная смена</option>${store.data.employees.map(p => `<option value="${p.login}" ${s.owner === p.login ? 'selected' : ''}>${esc(p.name)}</option>`).join('')}</select></label><div class="toolbar">${button('Отмена', 'close')}<button class="primary">Сохранить назначение</button></div></form>`;
    const reason = selectionReason(s), canTake = store.profile.role === 'EMPLOYEE' && !reason;
    return '<h2>' + (canTake ? 'Подтвердить выбор смены' : 'Карточка смены') + '</h2><p>' + period(s) + '</p><p>' + place(s) + '</p><p>' + esc(s.ownerName) + '</p>' + (store.profile.role === 'EMPLOYEE' ? (reason ? '<div class="availability-note">' + esc(reason) + '</div>' : '<p>После подтверждения заявка отправится руководителю на согласование.</p>') : '') + '<div class="toolbar">' + button('Закрыть', 'close') + (canTake ? button('Отправить заявку', 'submit-proposal', s.id, true) : '') + '</div>';

}


export function newShiftDialog() {
    return '<h2>Новое дежурство</h2><form id="new-shift-form"><label>Дата<input type="date" name="date" required min="' + store.data.today + '" value="' + store.data.today + '"></label><label>Время<select name="slot">' + ['08:30 — 09:50', '10:05 — 11:25', '11:40 — 13:00', '13:45 — 15:05', '15:20 — 16:40', '16:55 — 18:15', '18:30 — 19:50', '20:00 — 21:20'].map((t, i) => '<option value="' + i + '">' + t + '</option>').join('') + '</select></label><label>Корпус<select name="building"><option>1</option><option>4</option></select></label><label>Аудитория<input name="room" required maxlength="40" value="1108"></label><label>Сотрудник<select name="owner"><option value="">Свободная смена</option>' + store.data.employees.map(p => '<option value="' + p.login + '">' + esc(p.name) + '</option>').join('') + '</select></label><div class="toolbar">' + button('Отмена', 'close') + '<button class="primary">Создать смену</button></div></form>'
}
