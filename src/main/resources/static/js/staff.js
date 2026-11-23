import {store} from './store.js';
import {esc, button, empty} from './common.js';

export function staffView() {
    return `<section class="panel"><div class="toolbar"><h2>Сотрудники</h2><span class="spacer"></span>${button('Добавить сотрудника', 'staff-new', '', true)}</div>${(store.staff || []).map(({
                                                                                                                                                                                                   profile: p,
                                                                                                                                                                                                   active
                                                                                                                                                                                               }) => `<div class="row"><div class="info"><strong>${esc(p.name)}</strong><span class="muted">${esc(p.position)} · ${esc(p.email)}</span><p>Логин: ${esc(p.login)} · ${active ? 'Активен' : 'Отключён'}</p></div>${button('Занятость', 'staff-availability', p.login)}${button('Редактировать', 'staff-edit', p.login)}</div>`).join('') || empty()}</section>`;
}

export function staffDialog(login) {
    const item = (store.staff || []).find(s => s.profile.login === login), p = item?.profile || {};
    return `<h2>${item ? 'Редактировать' : 'Новый'} сотрудник</h2><form id="staff-form" data-new="${!item}">${[['login', 'Логин'], ['name', 'Имя и фамилия'], ['position', 'Должность'], ['department', 'Подразделение'], ['email', 'Рабочая почта']].map(([key, label]) => `<label>${label}<input name="${key}" required maxlength="100" value="${esc(p[key] || '')}" ${key === 'login' && item ? 'readonly' : ''}></label>`).join('')}<label>${item ? 'Новый пароль (пусто — оставить прежний)' : 'Пароль'}<input type="password" name="password" autocomplete="new-password" minlength="8" maxlength="100" ${item ? '' : 'required'}></label><label class="inline-check"><input type="checkbox" name="active" ${!item || item.active ? 'checked' : ''}> Учётная запись активна</label><p>Отключение сохраняет историю смен. Логин после создания не меняется.</p><div class="toolbar">${button('Отмена', 'close')}<button class="primary">Сохранить</button></div></form>`;
}
