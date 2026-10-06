'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const controls = {};
const element = () => ({style:{},append(){},addEventListener(){}});
const context = vm.createContext({
 document: {getElementById(id){return controls[id] ||= {...element(),value:id==='timing'?'observed':'all',checked:false};},createElement:element,createTextNode:x=>x,querySelector:()=>null},
 location:{pathname:'/wren'},window:{addEventListener(){}},
 fetch:()=>new Promise(()=>{}),setInterval(){},console
});
vm.runInContext(fs.readFileSync('scripts/click_debug/viewer.js','utf8'), context);
const run = code => vm.runInContext(code, context);
run(`sessionOrder=['old','new'];sessionInfo={old:{build_flavor:'recording'},new:{build_flavor:'standard'}};
 data=[{session:'old',event:'input',source:'physical',method:'mouse',side:'left',action:'press',timestamp_ms:100,elapsed_ns:1000000},
       {session:'new',event:'input',source:'physical',method:'mouse',side:'left',action:'press',timestamp_ms:200,elapsed_ns:2000000}];`);
for (const [choice,expected,timing] of [['older',['old'],'native'],['newest',['new'],'observed'],['all',['old','new'],'observed']]) {
 run(`selected=${JSON.stringify(choice)}`);
 assert.equal(run('JSON.stringify(selectedIds())'),JSON.stringify(expected));
 assert.equal(run('JSON.stringify(filtered().map(r=>r.session))'),JSON.stringify(expected));
 assert.equal(run('defaultTiming()'),timing);
}
run("selected='older'"); assert.equal(run('time(data[0])'),100);
run("selected='newest'"); assert.equal(run('time(data[1])'),2);
run("selected='new';sessionOrder.push('later')"); assert.equal(run('JSON.stringify(selectedIds())'),'["new"]');
run("selected='newest'"); assert.equal(run('JSON.stringify(selectedIds())'),'["later"]');
console.log('Viewer cohort filtering and clock alignment passed.');
