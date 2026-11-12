import {api} from './js/api.js';
import {store} from './js/store.js';
import {render,loginView} from './js/layout.js';
import {toast,dialog,closeDialog,button,period} from './js/common.js';
import {addDays,slotDialog,newShiftDialog,selectionReason} from './js/schedule.js';
import {swapDialog} from './js/shifts.js';
let loadSequence=0;
async function refresh(force=false){
 const generation=store.generation,sequence=++loadSequence;
 const [data,approvals]=await Promise.all([api('/state'+(store.week?'?week='+store.week:'')),store.profile?.manager?api('/manager/requests'):Promise.resolve([])]);
 if(generation!==store.generation||sequence!==loadSequence)return;
 store.data=data;store.week=data.weekStart;store.approvals=approvals;
 if(force||(!document.getElementById('modal').open&&!store.selected.size&&!['INPUT','SELECT','TEXTAREA'].includes(document.activeElement?.tagName)))render();
}
async function mutate(path,method='POST',body,message='Изменения сохранены'){
 if(store.busy)return;store.busy=true;document.querySelectorAll('#modal button').forEach(b=>b.disabled=true);
 try{await api(path,method,body);store.selected.clear();store.comment='';closeDialog();await refresh(true);toast(message);}
 catch(error){toast(error.message);if(error.status===401)reset();else {await refresh(false).catch(()=>{});document.querySelectorAll('#modal button').forEach(b=>b.disabled=false);}}
 finally{store.busy=false;}
}
function reset(){sessionStorage.removeItem('hr-session-token');store.generation++;loadSequence++;store.profile=null;store.data=null;store.approvals=[];store.selected.clear();store.comment='';store.week=null;store.page='schedule';store.filter='all';store.completed=false;store.unread=false;closeDialog();loginView();}
function findShift(id){return [...store.data.schedule,...store.data.myShifts].find(s=>s.id===id);}
function confirmProposal(ids){if(!ids.length)return toast('Выберите хотя бы одну свободную смену');const unavailable=ids.map(findShift).map(selectionReason).find(Boolean);if(unavailable)return toast(unavailable);dialog('<h2>Подтвердить заявку на дежурство</h2>'+ids.map(id=>'<p>'+period(findShift(id))+'</p>').join('')+'<p>Заявка будет направлена руководителю.</p><div class="toolbar">'+button('Отмена','close')+button('Подтвердить','submit-proposal',ids.join(','),true)+'</div>');}
document.addEventListener('change',event=>{if(event.target.id==='schedule-employee'){store.filter=event.target.value?'employee:'+event.target.value:'all';render();}});
document.addEventListener('input',event=>{if(event.target.id==='proposal-comment')store.comment=event.target.value;});
document.addEventListener('click',async event=>{
 const target=event.target.closest('button[data-action]');if(!target)return;event.preventDefault();if(store.busy)return;
 const action=target.dataset.action,id=target.dataset.id,num=Number(id);
 try{
 switch(action){
 case 'page':store.filter='all';store.page=id;store.selected.clear();store.comment='';render();await refresh(true);break;
 case 'refresh':await refresh(true);toast('Данные обновлены');break;
 case 'logout':await api('/logout','POST');reset();break;
 case 'week':store.week=addDays(store.week,num*7);store.selected.clear();await refresh(true);break;
 case 'building':store.building=num;store.selected.clear();render();break;
 case 'filter':store.filter=id;render();break;
 case 'completed':store.completed=id==='true';render();break;
 case 'unread':store.unread=id==='true';render();break;
 case 'new-shift':dialog(newShiftDialog());break;
 case 'slot':dialog(slotDialog(findShift(num)));break;
 case 'select':if(selectionReason(findShift(num))){toast(selectionReason(findShift(num)));break;}store.selected.has(num)?store.selected.delete(num):store.selected.add(num);render();break;
 case 'take-free':confirmProposal([num]);break;
 case 'confirm-proposal':confirmProposal([...store.selected]);break;
 case 'submit-proposal':await mutate('/proposals','POST',{shiftIds:id.split(',').map(Number),comment:store.comment},'Заявка отправлена руководителю');break;
 case 'close':closeDialog();break;
 case 'publish':await mutate('/offers/'+num);break;
 case 'withdraw':await mutate('/offers/'+num,'DELETE');break;
 case 'decline':await mutate('/exchange/'+num+'/decline','POST',undefined,'Предложение скрыто только для вас');break;
 case 'confirm-apply':{const offer=store.data.offers.find(o=>o.id===num);dialog('<h2>Подтвердить отклик</h2><p>'+period(offer.shift)+'</p><p>Предложение будет зарезервировано до решения руководителя. До одобрения владелец смены не меняется.</p><div class="toolbar">'+button('Отмена','close')+button('Отправить отклик','apply',num,true)+'</div>');break;}
 case 'apply':await mutate('/exchange/'+num+'/apply','POST',undefined,'Отклик отправлен руководителю');break;
 case 'swap':dialog(swapDialog(findShift(num)));break;
 case 'respond-yes':case 'respond-no':await mutate('/requests/'+num+'/decision','POST',{accept:action==='respond-yes'});break;
 case 'cancel-request':await mutate('/requests/'+num+'/cancel');break;
 case 'approve':case 'reject':await mutate('/manager/requests/'+num,'POST',{accept:action==='approve'});break;
 case 'read':await mutate('/notifications/read');break;
 case 'start-time':await mutate('/time/start/'+num);break;
 case 'stop-time':await mutate('/time/stop/'+num);break;
 case 'publish-schedule':await mutate('/schedule/publish','POST',undefined,'Сотрудники получили уведомление об обновлении расписания');break;
 }
 }catch(error){toast(error.message);if(error.status===401)reset();}
});
document.addEventListener('submit',async event=>{
 event.preventDefault();if(store.busy)return;const form=event.target,data=new FormData(form);
 if(form.id==='login-form'){
   store.busy=true;form.querySelector('button').disabled=true;
   try{const generation=++store.generation;const profile=await api('/login','POST',Object.fromEntries(data));const rating=await api('/rating');if(generation!==store.generation)return;store.profile=profile;store.rating=rating;store.page=profile.manager?'requests':'schedule';await refresh(true);}
   catch(error){reset();loginView(error.message);}finally{store.busy=false;}
 }else if(form.id==='swap-form')await mutate('/swaps','POST',{ownShiftId:Number(form.dataset.id),targetShiftId:Number(data.get('target')),comment:data.get('comment')},'Предложение отправлено коллеге');
 else if(form.id==='new-shift-form')await mutate('/schedule/shifts','POST',{date:data.get('date'),slot:Number(data.get('slot')),building:Number(data.get('building')),room:data.get('room'),owner:data.get('owner')||null});
 else if(form.id==='assignment-form')await mutate('/schedule/shifts/'+form.dataset.id,'PUT',{owner:data.get('owner')||null});
});
async function boot(){try{[store.profile,store.rating]=await Promise.all([api('/profile'),api('/rating')]);store.page=store.profile.manager?'requests':'schedule';await refresh(true);}catch{reset();}}
setInterval(()=>{if(store.profile&&!store.busy&&!document.hidden)refresh().catch(error=>{if(error.status===401)reset();});},4000);
window.addEventListener('focus',()=>{if(store.profile&&!store.busy)refresh().catch(()=>{});});
boot();
