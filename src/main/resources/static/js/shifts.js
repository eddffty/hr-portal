import {store} from './store.js';
import {esc, period, place, button, empty} from './common.js';
import {weekToolbar, addDays} from './schedule.js';

export function shiftsView() {
    const items = store.data.myShifts.filter(s => store.completed ? s.completed : !s.completed && s.date >= store.data.weekStart && s.date < addDays(store.data.weekStart, 7));
    return `<div class="toolbar"><button data-action="completed" data-id="false" class="${!store.completed ? 'active' : ''}">Предстоящие смены</button><button data-action="completed" data-id="true" class="${store.completed ? 'active' : ''}">Завершённые смены</button></div><section class="panel">${!store.completed ? weekToolbar() : ''}${items.length ? items.map(s => `<div class="row"><div class="info"><strong>${period(s)}</strong><span class="muted">${place(s)}</span></div>${s.completed ? '<span class="badge">Завершена</span>' : s.locked ? '<span class="badge">На согласовании</span>' : s.offerId ? '<span class="badge">На бирже</span>' : button('Опубликовать на бирже', 'publish', s.id) + button('Предложить обмен', 'swap', s.id)}</div>`).join('') : empty('В выбранном периоде смен нет')}</section>`;
}

export function swapDialog(own) {
    const candidates = store.data.schedule.filter(s => s.owner && s.owner !== store.profile.login && !s.completed && !s.locked && !s.offerId && s.date >= store.data.today);
    return `<h2>Адресный обмен сменами</h2><p>Ваша смена: ${period(own)}</p><p>Выберите смену коллеги на текущей неделе. Сначала согласие даст коллега, затем — руководитель.</p>${candidates.length ? `<form id="swap-form" data-id="${own.id}"><label>Смена коллеги<select name="target">${candidates.map(s => `<option value="${s.id}">${esc(s.ownerName)} · ${period(s)} · Корпус ${s.building}</option>`).join('')}</select></label><label>Комментарий<textarea name="comment" maxlength="2000"></textarea></label><div class="toolbar">${button('Отмена', 'close')}<button class="primary">Отправить предложение</button></div></form>` : empty('Нет доступных смен для обмена на этой неделе') + button('Закрыть', 'close')}`;
}
