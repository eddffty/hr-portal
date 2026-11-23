import {store} from './store.js';
import {esc, button, empty} from './common.js';

const starts = ['08:30 — 09:50', '10:05 — 11:25', '11:40 — 13:00', '13:45 — 15:05', '15:20 — 16:40', '16:55 — 18:15', '18:30 — 19:50', '20:00 — 21:20'];

export function availabilityView() {
    const own = store.profile.role === 'EMPLOYEE', user = store.avUser || store.profile.login,
        rows = store.availability || [];
    return `<section class="panel"><div class="toolbar">${[0, 1].map(p => `<button data-action="av-parity" data-id="${p}" class="${store.avParity === p ? 'active' : ''}">${p ? 'Нечётная' : 'Чётная'} неделя</button>`).join('')}${!own ? `<label>Сотрудник<select id="availability-user">${store.data.employees.map(p => `<option value="${p.login}" ${p.login === user ? 'selected' : ''}>${esc(p.name)}</option>`).join('')}</select></label>` : ''}</div><p>Постоянное расписание повторяется каждую ${store.avParity ? 'нечётную' : 'чётную'} неделю. Пары и личную занятость можно внести вручную.</p>${own ? `<div class="selection-summary"><strong>Выбрано: ${store.avSelected.size}</strong>${button('Добавить занятость', 'av-add', '', true, !store.avSelected.size)}</div>` : ''}<div class="tablewrap"><table class="availability-table"><thead><tr><th>Время</th>${['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'].map(d => '<th>' + d + '</th>').join('')}</tr></thead><tbody>${starts.map((t, r) => `<tr><td>${t}</td>${Array.from({length: 6}, (_, i) => {
        const key = (i + 1) + '-' + r,
            row = rows.find(a => a.parity === store.avParity && a.weekday === i + 1 && a.slot === r);
        return `<td><button class="availability-cell ${row ? 'busy' : ''} ${store.avSelected.has(key) ? 'chosen' : ''}" data-action="${own ? (row ? 'av-edit' : 'av-select') : 'noop'}" data-id="${row ? row.id : key}" title="${row ? esc(row.description) : own ? 'Добавить занятость' : 'Свободно'}">${store.avSelected.has(key) ? '✓' : row ? esc(row.description) : '+'}</button></td>`
    }).join('')}</tr>`).join('')}</tbody></table></div><p>Загрузка с сайта вуза ещё не подключена. Уже назначенные смены автоматически не снимаются.</p></section>`;
}

export function availabilityDialog(id) {
    const row = (store.availability || []).find(r => r.id === Number(id));
    return `<h2>${row ? 'Редактировать' : 'Добавить'} занятость</h2><p>${store.avParity ? 'Нечётная' : 'Чётная'} неделя · ${row ? '1 ячейка' : store.avSelected.size + ' ячеек'}</p><form id="availability-form" data-cells="${row ? row.weekday + '-' + row.slot : [...store.avSelected].join(',')}"><label>Описание<textarea name="description" required maxlength="200" placeholder="Пара, вторая работа…">${row ? esc(row.description) : ''}</textarea></label><div class="toolbar">${button('Отмена', 'close')}${row ? button('Удалить занятость', 'av-delete', row.id) : ''}<button class="primary">Сохранить</button></div></form>`;
}
