import {availabilityView} from './availability.js';
import {staffView} from './staff.js';
import {store} from './store.js';
import {esc, role, button, empty} from './common.js';
import {scheduleView} from './schedule.js';
import {shiftsView} from './shifts.js';
import {exchangeView, requestsView} from './exchange.js';
import {timeView} from './time.js';
import {profileView} from './profile.js';

const navAssets = {
    schedule: "8d525.svg",
    availability: "b0133.svg",
    shifts: "08611.svg",
    propose: "c2c39.svg",
    exchange: "b31c1.svg",
    requests: "b9e0b.svg",
    notifications: "c2a4f.svg",
    staff: "d81d6.svg",
    time: "4f1bb.svg"
};
const pages = {
    availability: ["▦", "Моя занятость", "Личное расписание чётной и нечётной недели"],
    staff: ["♙", "Сотрудники", "Сотрудники и их занятость"],
    schedule: ['▦', 'Расписание', 'Единый график дежурств сотрудников'],
    shifts: ['☑', 'Мои смены', 'Ваши назначения из общего расписания'],
    propose: ['▣', 'Предложить дежурство', 'Выберите свободные смены и подтвердите заявку'],
    exchange: ['⇄', 'Биржа смен', 'Найдите замену или примите дежурство коллеги'],
    requests: ['▤', 'Запросы', 'Обмены и согласование изменений'],
    notifications: ['♧', 'Уведомления', 'Персональные события и решения'],
    time: ['◷', 'Учёт времени', 'Плановое и фактическое время дежурств'],
    profile: ['◉', 'Мой профиль', 'Ваш вклад в работу команды']
};

function notificationView() {
    const items = store.data.notifications.filter(n => !store.unread || !n.read);
    return `<div class="toolbar"><button data-action="unread" data-id="false" class="${!store.unread ? 'active' : ''}">Все</button><button data-action="unread" data-id="true" class="${store.unread ? 'active' : ''}">Непрочитанные</button>${button('✓ Отметить прочитанными', 'read')}</div><section class="panel">${items.length ? items.map(n => `<div class="row"><span>${n.read ? '✓' : '●'}</span><div class="info"><strong>${esc(n.text)}</strong><span class="muted">${new Date(n.createdAt).toLocaleString('ru-RU')}</span></div></div>`).join('') : empty('Уведомлений нет')}</section>`;
}

export function render() {
    if (!store.profile || !store.data) return;
    document.body.classList.toggle('sidebar-collapsed',store.collapsed);
    document.body.classList.toggle('planning-mode', store.planning);
    const p = store.profile, employee = p.role === 'EMPLOYEE',
        menu = employee ? ['schedule', 'availability', 'shifts', 'propose', 'exchange', 'requests', 'notifications', 'time'] : ['schedule', 'availability', 'requests', 'staff', 'time', 'notifications'];
    const [icon, baseTitle, description] = pages[store.page];
    const title = store.page === 'availability' && !employee ? 'Занятость сотрудников' : baseTitle;
    let content;
    switch (store.page) {
        case 'availability':
            content = availabilityView();
            break;
        case 'staff':
            content = staffView();
            break;
        case 'schedule':
            content = scheduleView();
            break;
        case 'propose':
            content = scheduleView(true);
            break;
        case 'shifts':
            content = shiftsView();
            break;
        case 'exchange':
            content = exchangeView();
            break;
        case 'requests':
            content = requestsView();
            break;
        case 'notifications':
            content = notificationView();
            break;
        case 'time':
            content = timeView();
            break;
        case 'profile':
            content = profileView();
            break;
    }
    document.getElementById('app').innerHTML = `<aside><div class="brand">HR<span style="color:#7b9bb4">.</span><small>ПОРТАЛ</small></div><nav>${menu.map(key => `<button data-action="page" data-id="${key}" class="${store.page === key ? 'active' : ''}" title="${key === 'availability' && !employee ? 'Занятость сотрудников' : pages[key][1]}">${navAssets[key] ? `<img class="nav-icon" src="/assets/figma/${navAssets[key]}" alt="">` : pages[key][0]} <span class="label">${key === 'requests' && p.manager ? 'Согласование' : key === 'availability' && !employee ? 'Занятость сотрудников' : pages[key][1]}</span></button>`).join('')}</nav></aside><main><header><span class="header-path">Личный кабинет / ${title}</span><div class="header-actions"><button class="profile-link header-profile" data-action="page" data-id="profile" title="Открыть мой профиль" aria-label="Открыть профиль: ${esc(p.name)}"><span class="profile-avatar" aria-hidden="true">${esc(p.initials)}</span><span class="profile-caption"><strong>${esc(p.name)}</strong><span>Мой профиль</span></span><span class="profile-chevron" aria-hidden="true">›</span></button>${button("Выйти", "logout")}</div></header><div class="heading"><div><h1>${p.manager && store.page === 'requests' ? 'Согласование заявок' : title}</h1><p>${description}</p></div><div class="toolbar">${button('↻ Обновить', 'refresh')}<span class="badge">● Демо</span></div></div><div id="content">${content}</div><p class="sync-note">Общие данные обновляются автоматически. Изменения сохраняются в базе данных.</p></main>`;
}

export function loginView(error = '') {
    document.getElementById('app').innerHTML = `<div class="login"><form id="login-form"><div class="muted">HR / ПОРТАЛ СОТРУДНИКА</div><h1>Вход в систему</h1><p>Ваше расписание. Всё под рукой.</p><label>Логин<input name="login" required maxlength="100" autocomplete="username" placeholder="ivanov"></label><label>Пароль<input name="password" required maxlength="100" type="password" autocomplete="current-password" placeholder="Введите пароль"></label>${error ? '<p role="alert">' + esc(error) + '</p>' : ''}<button class="primary">Войти →</button><small>Доступ только по учётной записи сотрудника. Потеряли пароль? Обратитесь к ответственному.</small></form></div>`;
}
