import {availabilityDialog} from './js/availability.js';
import {candidateDialog} from './js/planner.js';
import {staffDialog} from './js/staff.js';
import {api} from './js/api.js';
import {store} from './js/store.js';
import {render, loginView} from './js/layout.js';
import {toast, dialog, closeDialog, button, period} from './js/common.js';
import {addDays, slotDialog, newShiftDialog, selectionReason} from './js/schedule.js';
import {swapDialog} from './js/shifts.js';

let loadSequence = 0;

async function refresh(force = false) {
    const generation = store.generation, sequence = ++loadSequence;
    const [data, approvals, availability, staff] = await Promise.all([api('/state' + (store.week ? '?week=' + store.week : '')), store.profile?.manager ? api('/manager/requests') : Promise.resolve([]), api('/availability' + (store.profile?.role !== 'EMPLOYEE' ? '?user=' + encodeURIComponent(store.avUser || store.data?.employees?.[0]?.login || 'ivanov') : '')), store.profile?.role !== 'EMPLOYEE' ? api('/staff') : Promise.resolve([])]);
    if (generation !== store.generation || sequence !== loadSequence) return;
    store.data = data;
    store.week = data.weekStart;
    store.approvals = approvals;
    store.availability = availability;
    store.staff = staff;
    if (force || (!document.getElementById('modal').open && !store.selected.size && !store.avSelected.size && !store.planning && !['INPUT', 'SELECT', 'TEXTAREA'].includes(document.activeElement?.tagName))) render();
}

async function mutate(path, method = 'POST', body, message = 'Изменения сохранены') {
    if (store.busy) return;
    store.busy = true;
    document.querySelectorAll('#modal button').forEach(b => b.disabled = true);
    try {
        await api(path, method, body);
        store.selected.clear();
        store.comment = '';
        closeDialog();
        await refresh(true);
        toast(message);
        return true;
    } catch (error) {
        toast(error.message);
        if (error.status === 401) reset(); else {
            await refresh(false).catch(() => {
            });
            document.querySelectorAll('#modal button').forEach(b => b.disabled = false);
        }
    } finally {
        store.busy = false;
    }
}

function reset() {
    store.avSelected.clear();
    store.avUser = null;
    store.availability = [];
    store.staff = [];
    store.planning = false;
    store.plan = {};
    sessionStorage.removeItem('hr-session-token');
    store.generation++;
    loadSequence++;
    store.profile = null;
    store.data = null;
    store.approvals = [];
    store.selected.clear();
    store.comment = '';
    store.week = null;
    store.page = 'schedule';
    store.filter = 'all';
    store.completed = false;
    store.unread = false;
    closeDialog();
    loginView();
}

function findShift(id) {
    return [...store.data.schedule, ...store.data.myShifts].find(s => s.id === id);
}

function confirmProposal(ids) {
    if (!ids.length) return toast('Выберите хотя бы одну свободную смену');
    const unavailable = ids.map(findShift).map(selectionReason).find(Boolean);
    if (unavailable) return toast(unavailable);
    dialog('<h2>Подтвердить заявку на дежурство</h2>' + ids.map(id => '<p>' + period(findShift(id)) + '</p>').join('') + '<p>Заявка будет направлена руководителю.</p><div class="toolbar">' + button('Отмена', 'close') + button('Подтвердить', 'submit-proposal', ids.join(','), true) + '</div>');
}

document.addEventListener('change', async event => {
    if (event.target.id === 'availability-user') {
        store.avUser = event.target.value;
        await refresh(true);
        return;
    }
    if (event.target.id === 'schedule-employee') {
        store.filter = event.target.value ? 'employee:' + event.target.value : 'all';
        render();
    }
});
document.addEventListener('input', event => {
    if (event.target.id === 'proposal-comment') store.comment = event.target.value;
});
document.addEventListener('click', async event => {
    const target = event.target.closest('button[data-action]');
    if (!target) return;
    event.preventDefault();
    if (store.busy) return;
    const action = target.dataset.action, id = target.dataset.id, num = Number(id);
    try {
        switch (action) {
            case 'sidebar-toggle':
                store.collapsed = !store.collapsed;
                render();
                break;
            case 'noop':
                break;
            case 'av-parity':
                store.avParity = num;
                store.avSelected.clear();
                render();
                break;
            case 'av-select':
                store.avSelected.has(id) ? store.avSelected.delete(id) : store.avSelected.add(id);
                render();
                break;
            case 'av-add':
                dialog(availabilityDialog());
                break;
            case 'av-edit':
                dialog(availabilityDialog(num));
                break;
            case 'av-delete':
                await mutate('/availability/' + num, 'DELETE');
                break;
            case 'staff-new':
                dialog(staffDialog());
                break;
            case 'staff-edit':
                dialog(staffDialog(id));
                break;
            case 'staff-availability':
                store.avUser = id;
                store.page = 'availability';
                await refresh(true);
                break;
            case 'plan-start':
                store.planning = true;
                store.building = 'all';
                store.filter = 'all';
                store.plan = {};
                render();
                break;
            case 'plan-cancel':
                store.planning = false;
                store.building = 1;
                store.plan = {};
                render();
                break;
            case 'plan-save':
                if (await mutate('/planner/save', 'POST', {
                    changes: Object.entries(store.plan).map(([id, owner]) => ({
                        shiftId: Number(id),
                        owner
                    }))
                })) {
                    store.planning = false;
                    store.building = 1;
                    store.plan = {};
                    render();
                }
                break;
            case 'request-type':
                store.requestType = id;
                render();
                break;
            case 'request-status':
                store.requestStatus = id;
                render();
                break;
            case 'page':
                if (id !== 'schedule') {
                    store.planning = false;
                    store.plan = {};
                    if (store.building === 'all') store.building = 1;
                }
                store.filter = 'all';
                store.page = id;
                store.selected.clear();
                store.avSelected.clear();
                store.comment = '';
                render();
                await refresh(true);
                window.scrollTo(0, 0);
                break;
            case 'refresh':
                await refresh(true);
                toast('Данные обновлены');
                break;
            case 'logout':
                await api('/logout', 'POST');
                reset();
                break;
            case 'week':
                store.week = addDays(store.week, num * 7);
                store.selected.clear();
                await refresh(true);
                break;
            case 'building':
                store.building = num;
                store.selected.clear();
                render();
                break;
            case 'filter':
                store.filter = id;
                render();
                break;
            case 'completed':
                store.completed = id === 'true';
                render();
                break;
            case 'unread':
                store.unread = id === 'true';
                render();
                break;
            case 'new-shift':
                dialog(newShiftDialog());
                break;
            case 'slot':
                if (store.planning) {
                    dialog(candidateDialog(findShift(num), await api('/planner/candidates/' + num)));
                    break;
                }
                dialog(slotDialog(findShift(num)));
                break;
            case 'select':
                if (selectionReason(findShift(num))) {
                    toast(selectionReason(findShift(num)));
                    break;
                }
                store.selected.has(num) ? store.selected.delete(num) : store.selected.add(num);
                render();
                break;
            case 'take-free':
                confirmProposal([num]);
                break;
            case 'confirm-proposal':
                confirmProposal([...store.selected]);
                break;
            case 'submit-proposal':
                await mutate('/proposals', 'POST', {
                    shiftIds: id.split(',').map(Number),
                    comment: store.comment
                }, 'Заявка отправлена руководителю');
                break;
            case 'close':
                closeDialog();
                break;
            case 'publish':
                await mutate('/offers/' + num);
                break;
            case 'withdraw':
                await mutate('/offers/' + num, 'DELETE');
                break;
            case 'decline':
                await mutate('/exchange/' + num + '/decline', 'POST', undefined, 'Предложение скрыто только для вас');
                break;
            case 'confirm-apply': {
                const offer = store.data.offers.find(o => o.id === num);
                dialog('<h2>Подтвердить отклик</h2><p>' + period(offer.shift) + '</p><p>Предложение будет зарезервировано до решения руководителя. До одобрения владелец смены не меняется.</p><div class="toolbar">' + button('Отмена', 'close') + button('Отправить отклик', 'apply', num, true) + '</div>');
                break;
            }
            case 'apply':
                await mutate('/exchange/' + num + '/apply', 'POST', undefined, 'Отклик отправлен руководителю');
                break;
            case 'swap':
                dialog(swapDialog(findShift(num)));
                break;
            case 'respond-yes':
            case 'respond-no':
                await mutate('/requests/' + num + '/decision', 'POST', {accept: action === 'respond-yes'});
                break;
            case 'cancel-request':
                await mutate('/requests/' + num + '/cancel');
                break;
            case 'approve':
            case 'reject':
                if (action === 'reject') {
                    dialog('<h2>Отклонить заявку</h2><form id="rejection-form" data-id="' + num + '"><label>Причина отказа<textarea name="reason" required maxlength="500"></textarea></label><button class="primary">Отклонить заявку</button></form>');
                    break;
                }
                await mutate('/manager/requests/' + num, 'POST', {accept: action === 'approve'});
                break;
            case 'read':
                await mutate('/notifications/read');
                break;
            case 'start-time':
                await mutate('/time/start/' + num);
                break;
            case 'stop-time':
                await mutate('/time/stop/' + num);
                break;
            case 'publish-schedule':
                await mutate('/schedule/publish', 'POST', undefined, 'Сотрудники получили уведомление об обновлении расписания');
                break;
        }
    } catch (error) {
        toast(error.message);
        if (error.status === 401) reset();
    }
});
document.addEventListener('submit', async event => {
    event.preventDefault();
    if (store.busy) return;
    const form = event.target, data = new FormData(form);
    if (form.id === 'login-form') {
        store.busy = true;
        form.querySelector('button').disabled = true;
        try {
            const generation = ++store.generation;
            const profile = await api('/login', 'POST', Object.fromEntries(data));
            const rating = await api('/rating');
            if (generation !== store.generation) return;
            store.profile = profile;
            store.rating = rating;
            store.page = profile.manager ? 'requests' : 'schedule';
            await refresh(true);
        } catch (error) {
            reset();
            loginView(error.message);
        } finally {
            store.busy = false;
        }
    } else if (form.id === 'availability-form') {
        await mutate('/availability', 'PUT', {
            parity: store.avParity,
            cells: form.dataset.cells.split(','),
            description: data.get('description')
        });
        store.avSelected.clear();
        render();
    } else if (form.id === 'plan-form') {
        if (!data.has('owner')) return toast('Выберите доступного сотрудника');
        store.plan[form.dataset.id] = data.get('owner') || null;
        closeDialog();
        render();
    } else if (form.id === 'staff-form') await mutate('/staff', form.dataset.new === 'true' ? 'POST' : 'PUT', {
        ...Object.fromEntries(data),
        active: data.has('active')
    });
    else if (form.id === 'rejection-form') await mutate('/manager/requests/' + form.dataset.id, 'POST', {
        accept: false,
        reason: data.get('reason')
    });
    else if (form.id === 'swap-form') await mutate('/swaps', 'POST', {
        ownShiftId: Number(form.dataset.id),
        targetShiftId: Number(data.get('target')),
        comment: data.get('comment')
    }, 'Предложение отправлено коллеге');
    else if (form.id === 'new-shift-form') await mutate('/schedule/shifts', 'POST', {
        date: data.get('date'),
        slot: Number(data.get('slot')),
        building: Number(data.get('building')),
        room: data.get('room'),
        owner: data.get('owner') || null
    });
    else if (form.id === 'assignment-form') await mutate('/schedule/shifts/' + form.dataset.id, 'PUT', {owner: data.get('owner') || null});
});

async function boot() {
    try {
        [store.profile, store.rating] = await Promise.all([api('/profile'), api('/rating')]);
        store.page = store.profile.manager ? 'requests' : 'schedule';
        await refresh(true);
    } catch {
        reset();
    }
}

setInterval(() => {
    if (store.profile && !store.busy && !document.hidden) refresh().catch(error => {
        if (error.status === 401) reset();
    });
}, 4000);
window.addEventListener('focus', () => {
    if (store.profile && !store.busy) refresh().catch(() => {
    });
});
boot();
