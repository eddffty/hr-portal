// Optional UI regression checks. The application itself only needs Java.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../src/main/resources/static/js');
const source = ['store.js', 'common.js', 'schedule.js'].map(file =>
    fs.readFileSync(path.join(root, file), 'utf8')
        .replace(/^import .*;\s*$/gm, '')
        .replace(/\bexport /g, '')).join('\n');
const context = vm.createContext({});
vm.runInContext(source, context);
vm.runInContext(`
    store.profile={login:'glukhov',role:'EMPLOYEE'};
    store.data={today:'2026-11-11',weekStart:'2026-11-09',myShifts:[],employees:[],schedule:[]};
    const shift={id:1,date:'2026-11-12',start:'08:30',end:'09:50',slot:0,building:1,room:'1108',owner:null,ownerName:'Свободно',locked:false,completed:false};
`, context);
assert.equal(vm.runInContext('selectionReason(shift)', context), null);
assert.match(vm.runInContext('slotDialog(shift)', context), /data-action="submit-proposal"/);
const past = vm.runInContext("slotDialog({...shift,date:'2026-11-10',completed:true})", context);
assert.match(past, /уже прошла/);
assert.doesNotMatch(past, /Подтвердить выбор смены|data-action="submit-proposal"/);
assert.match(vm.runInContext("selectionReason({...shift,owner:'glukhov'})", context), /уже назначена вам/);
assert.match(vm.runInContext('selectionReason({...shift,locked:true})', context), /рассматривается заявка/);
vm.runInContext("store.data.myShifts=[{...shift,id:2,owner:'glukhov',building:4}];", context);
assert.match(vm.runInContext('selectionReason(shift)', context), /другом корпусе/);
vm.runInContext('store.data.myShifts=[];store.data.schedule=[shift];', context);
assert.match(vm.runInContext('scheduleView(true)', context), /selection-summary/);
assert.match(vm.runInContext('calendar(true)', context), /\+ Выбрать смену/);
console.log('Schedule UI checks passed: future/past dates, confirmation, ownership, reservation, time conflict and selection summary.');
