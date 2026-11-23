import {store} from './store.js';
import {esc, period, place, button, empty, status, type} from './common.js';

export function offerRow(o, invitation = false) {
    return `<div class="row"><div class="info"><strong>${period(o.shift)}</strong><span class="muted">${esc(o.ownerName)} · ${place(o.shift)}</span>${!o.available && !o.own ? '<p>У вас уже есть дежурство в это время</p>' : ''}</div>${o.own ? (o.status === 'OPEN' ? button('Убрать с биржи', 'withdraw', o.id) : '<span class="badge">' + status[o.status] + '</span>') : button(invitation ? 'Принять предложение' : 'Откликнуться', 'confirm-apply', o.id, true, !o.available) + button('Отклонить', 'decline', o.id)}</div>`;
}

export function exchangeView() {
    const own = store.data.offers.filter(o => o.own), others = store.data.offers.filter(o => !o.own);
    return `<section class="panel"><h2>Мои предложения</h2>${own.length ? own.map(o => offerRow(o)).join('') : empty('Опубликуйте смену из раздела «Мои смены»')}</section><section class="panel"><h2>Предложения коллег</h2><p>Отклик резервирует предложение до решения руководителя. Отказ скрывает его только для вас.</p>${others.length ? others.map(o => offerRow(o)).join('') : empty('Доступных предложений нет')}</section>`;
}

export function requestRow(r, manager = false) {
    return `<div class="row"><div class="info"><strong>№${r.id} · ${type[r.type]} · ${esc(r.initiatorName)}</strong><span class="muted">${status[r.status]}${r.recipient ? ' · ' + esc(r.recipientName) : ''}</span>${r.shifts.map(s => `<p>${period(s)} · ${place(s)}</p>`).join('')}${r.comment ? '<p>' + esc(r.comment) + '</p>' : ''}</div>${manager && r.status === 'WAITING_MANAGER' ? button('Одобрить', 'approve', r.id, true) + button('Отклонить', 'reject', r.id) : r.canRespond ? button('Принять', 'respond-yes', r.id, true) + button('Отклонить', 'respond-no', r.id) : r.canCancel ? button('Отменить заявку', 'cancel-request', r.id) : '<span class="badge">' + status[r.status] + '</span>'}</div>`;
}

export function requestsView() {
    if (store.profile.role === 'MANAGER') return managerRequestsView();
    const incoming = store.data.offers.filter(o => !o.own);
    return `${store.profile.role === 'EMPLOYEE' ? `<section class="panel"><h2>Предложения коллег</h2>${incoming.length ? incoming.map(o => offerRow(o, true)).join('') : empty('Новых предложений нет')}</section>` : ''}<section class="panel"><h2>Адресные обмены и мои заявки</h2>${store.data.requests.length ? store.data.requests.map(r => requestRow(r)).join('') : empty('Заявок пока нет')}</section>`;
}

function managerRequestsView() {
    const pending = store.requestStatus === 'pending';
    const rows = store.approvals.filter(r => (store.requestType === 'ASSIGNMENT' ? r.type === 'ASSIGNMENT' : r.type !== 'ASSIGNMENT') && (!pending || r.status === 'WAITING_MANAGER')).sort((a, b) => a.shifts[0].date.localeCompare(b.shifts[0].date) || a.shifts[0].start.localeCompare(b.shifts[0].start));
    return '<section class="panel"><div class="toolbar">' + button('Заявки на дежурство', 'request-type', 'ASSIGNMENT', store.requestType === 'ASSIGNMENT') + button('Заявки на обмен', 'request-type', 'TRANSFER', store.requestType !== 'ASSIGNMENT') + button(pending ? 'Показать историю' : 'Только ожидающие', 'request-status', pending ? 'all' : 'pending') + '</div>' + (rows.length ? rows.map(r => {
        const deadline = new Date(r.shifts[0].date + 'T' + r.shifts[0].start),
            hours = (deadline - Date.now()) / 3600000;
        return (r.status === 'WAITING_MANAGER' && hours >= 0 && hours <= 24 ? '<span class="badge urgent">Срочно · смена в ближайшие 24 часа</span>' : '') + requestRow(r, true)
    }).join('') : empty('Заявок нет')) + '</section>';
}
