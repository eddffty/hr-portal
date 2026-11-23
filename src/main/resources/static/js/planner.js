import {store} from './store.js';
import {esc, period, button} from './common.js';

export function planToolbar() {
    return `<div class="selection-summary"><div><strong>Составление расписания · Два корпуса</strong><p>Выбирайте смены и сотрудников. Изменения увидят все после сохранения.</p></div>${button('Отменить', 'plan-cancel')}${button('Сохранить расписание · ' + Object.keys(store.plan).length, 'plan-save', '', true, !Object.keys(store.plan).length)}</div>`;
}

export function candidateDialog(shift, candidates) {
    return `<h2>Назначить сотрудника</h2><p>${period(shift)} · Корпус ${shift.building}</p><form id="plan-form" data-id="${shift.id}"><div class="candidate-list"><label><input type="radio" name="owner" value="" ${!shift.owner ? 'checked' : ''}> Оставить свободной</label>${candidates.map(c => `<label class="${c.requested ? 'candidate-requested' : ''} ${!c.available ? 'muted' : ''}"><input type="radio" name="owner" value="${c.login}" ${c.available ? '' : 'disabled'} ${shift.owner === c.login ? 'checked' : ''}><span><strong>${esc(c.name)}</strong>${c.requested ? '<small>Подал заявку на эту смену</small>' : ''}${!c.available ? '<small>' + esc(c.reason) + '</small>' : ''}</span></label>`).join('')}</div><div class="toolbar">${button('Отмена', 'close')}<button class="primary">Выбрать сотрудника</button></div></form>`;
}
