export const esc=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export const date=value=>new Date(value+'T12:00:00').toLocaleDateString('ru-RU',{day:'2-digit',month:'long'});
export const time=value=>String(value).slice(0,5);
export const period=s=>`${date(s.date)} · ${time(s.start)} — ${time(s.end)}`;
export const place=s=>`Корпус ${s.building} · Аудитория ${esc(s.room)}`;
export const status={WAITING_COLLEAGUE:'Ожидает ответа коллеги',WAITING_MANAGER:'Ожидает руководителя',APPROVED:'Одобрено',REJECTED_COLLEAGUE:'Отклонено коллегой',REJECTED_MANAGER:'Отклонено руководителем',CANCELLED:'Отменено',OPEN:'Опубликовано',RESERVED:'Есть отклик · На согласовании'};
export const type={TRANSFER:'Передача смены',SWAP:'Обмен сменами',ASSIGNMENT:'Свободное дежурство'};
export const button=(text,action,id='',primary=false,disabled=false)=>`<button data-action="${action}" data-id="${esc(id)}" class="${primary?'primary':''}" ${disabled?'disabled':''}>${text}</button>`;
export const empty=text=>`<div class="empty">${text||'Пока ничего нет'}</div>`;
export function toast(text){const el=document.getElementById('toast');el.textContent=text;el.style.display='block';clearTimeout(toast.timer);toast.timer=setTimeout(()=>el.style.display='none',4000);}
export function dialog(content){const modal=document.getElementById('modal');if(modal.open)modal.close();modal.innerHTML=content;modal.showModal();}
export function closeDialog(){document.getElementById('modal').close();}
export const role=p=>p.role==='MANAGER'?'Руководитель':p.role==='SCHEDULER'?'Ответственный за расписание':'Сотрудник';
