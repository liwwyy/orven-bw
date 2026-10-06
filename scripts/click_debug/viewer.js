'use strict';
const $ = id => document.getElementById(id);
const sampleName = decodeURIComponent(location.pathname.replace(/^\/|\/$/g, ''));
if (sampleName) {
 document.title = `${sampleName} · Click debug`;
 const heading = document.querySelector('h1');
 if (heading) heading.textContent = `${sampleName} · Click debug`;
}
const colors = {mouse:'#7dd3fc',keyboard_binding:'#38bdf8',cps_boost:'#c4a5ff',spam_click:'#fbad73',mouse_hold_click:'#89e3ac',button_hold:'#f0a8d9',unattributed:'#aab8cc'};
const names = {mouse:'Physical mouse',keyboard_binding:'Physical keyboard',cps_boost:'Physical CPS boost',spam_click:'Spam keybind',mouse_hold_click:'Mouse hold click',button_hold:'Button hold',unattributed:'Unattributed binding'};
let data=[], selected=sampleName?'all':'newest', view=null, bounds=[0,1], plots=new Map(), busy=false, follow=true;
let enabled=new Set(Object.keys(names)), sessionInfo={}, sessionOrder=[], timingChosen=false;
for(const [method,name] of Object.entries(names)){
 const label=document.createElement('label'), input=document.createElement('input'); input.type='checkbox';input.checked=true;
 label.style.color=colors[method];label.append(input,document.createTextNode(name));$('methods').append(label);
 input.onchange=()=>{input.checked?enabled.add(method):enabled.delete(method);render();};
}
function click(row){return ['press','queue','invoke','send'].includes(row.action);}
function nativeMode(){return $('timing').value==='native';}
function plannedMode(){return $('timing').value==='planned';}
function selectedIds(){return selected==='all'?sessionOrder:selected==='newest'?sessionOrder.slice(-1):selected==='older'?sessionOrder.slice(0,-1):[selected];}
function multipleSessions(){return selected==='all'||selected==='older';}
function defaultTiming(){const ids=selectedIds();return ids.length&&ids.every(id=>sessionInfo[id]?.build_flavor==='recording')?'native':'observed';}
function time(row){
 if(plannedMode())return multipleSessions()?row._intended_epoch_ms:row._intended_elapsed_ms;
 if(nativeMode()&&Number.isFinite(row._native_elapsed_ms))return multipleSessions()?row._native_epoch_ms:row._native_elapsed_ms;
 return !multipleSessions()&&Number.isFinite(row.elapsed_ns)?row.elapsed_ns/1e6:row.timestamp_ms;
}
function stageMatches(r){
 const stage=$('timing').value;
 if(stage==='action'||stage==='packet')return r.event===stage;
 if(r.event!=='input')return false;
 if(stage==='native')return r.source==='physical'&&r.method==='mouse'&&['press','release'].includes(r.action);
 if(stage==='planned')return r.source==='artificial'&&r.action==='queue'&&Number.isFinite(r._intended_elapsed_ms);
 return !(r.source==='physical'&&r.action==='queue');
}
function filtered(){return data.filter(r=>selectedIds().includes(r.session)&&enabled.has(r.method)&&stageMatches(r)&&($('side').value==='all'||($('side').value==='both'?['left','right'].includes(r.side):r.side===$('side').value))&&($('releases').checked||click(r))).sort((a,b)=>time(a)-time(b));}
function intervals(rows){
 const previous=new Map(),result=[];
 for(const r of rows.filter(click)){
  const native=nativeMode(), key=r.session+':'+r.side+(native?':'+r._native_segment:''),t=time(r),last=previous.get(key);
  if(last)result.push({x:t,y:native||plannedMode()?t-time(last):Number.isFinite(r.elapsed_ns)&&Number.isFinite(last.elapsed_ns)?(r.elapsed_ns-last.elapsed_ns)/1e6:t-time(last),row:r});
  previous.set(key,r);
 }
 return result;
}
function canvas(id,ymax,ylabel){
 const c=$(id),d=window.devicePixelRatio||1,w=c.clientWidth,h=c.clientHeight;c.width=w*d;c.height=h*d;const ctx=c.getContext('2d');ctx.scale(d,d);
 const box={left:58,right:w-18,top:14,bottom:h-34}, span=Math.max(1,view[1]-view[0]);
 const x=t=>box.left+(t-view[0])/span*(box.right-box.left),y=v=>box.bottom-v/Math.max(1,ymax)*(box.bottom-box.top);
 ctx.font='11px system-ui';ctx.strokeStyle='#31425b';ctx.fillStyle='#9cacc3';
 for(let i=0;i<=4;i++){const yy=y(ymax*i/4);ctx.beginPath();ctx.moveTo(box.left,yy);ctx.lineTo(box.right,yy);ctx.stroke();ctx.fillText((ymax*i/4).toFixed(ymax<10?1:0),4,yy+4);}
 ctx.fillText(ylabel,4,11);
 for(let i=0;i<=4;i++){const t=view[0]+span*i/4;ctx.fillText(((t-bounds[0])/1000).toFixed(2)+' s',x(t)-16,h-10);}
 plots.set(id,{points:[],box,w,h,x,y});return {ctx,x,y,box};
}
function drawPoint(id,p,color,hollow=false){const plot=plots.get(id),ctx=$(id).getContext('2d');const x=plot.x(p.x),y=plot.y(p.y);ctx.strokeStyle=ctx.fillStyle=color;ctx.beginPath();ctx.arc(x,y,3,0,Math.PI*2);hollow?ctx.stroke():ctx.fill();plot.points.push({...p,px:x,py:y});}
function render(){
 const all=filtered();
 const fallbacks=all.filter(r=>r._timing_fallback).length;
 $('timing-note').textContent=plannedMode()?'Model deadlines only; these are not hardware events, queued clicks, packets, or registered hits.':nativeMode()?'Native mouse-event differences; only physical mouse input is plotted. '+(fallbacks?fallbacks+' records use observation-time fallback.':'Clock origins are anchored per session; resets start new interval segments.'):$('timing').value==='action'?'Vanilla doAttack/doUse invocation times for both origins. Unattributed actions have no matched binding queue.':$('timing').value==='packet'?'Client-side submission times for outgoing interaction packets, linked to an active vanilla action. These are not server receipt times.':'Input observation and binding queue times; physical queue duplicates are suppressed because physical presses are already shown.';
 bounds=all.length?[time(all[0]),time(all[all.length-1])+1]:[0,1];if(!view)view=[...bounds];
 if(view[1]<=view[0])view[1]=view[0]+1;
 const shown=all.filter(r=>time(r)>=view[0]&&time(r)<=view[1]),clicks=shown.filter(click),ints=intervals(all).filter(p=>p.x>=view[0]&&p.x<=view[1]&&p.y>=0);
 $('count').textContent=clicks.length;$('physical').textContent=clicks.filter(r=>r.source==='physical').length;$('artificial').textContent=clicks.filter(r=>r.source==='artificial').length;
 const sorted=ints.map(p=>p.y).sort((a,b)=>a-b),mean=sorted.reduce((a,b)=>a+b,0)/sorted.length;
 const median=sorted.length?(sorted[Math.floor((sorted.length-1)/2)]+sorted[Math.floor(sorted.length/2)])/2:null;
 $('median').textContent=median===null?'—':median.toFixed(1)+' ms';$('cv').textContent=mean>0?(Math.sqrt(sorted.reduce((a,b)=>a+(b-mean)**2,0)/sorted.length)/mean).toFixed(3):'—';
 const methods=Object.keys(names).filter(m=>enabled.has(m));canvas('timeline',Math.max(1,methods.length),'Source');
 const plot=plots.get('timeline'),ctx=$('timeline').getContext('2d');ctx.font='11px system-ui';
 methods.forEach((m,i)=>{ctx.fillStyle=colors[m];ctx.fillText(names[m],60,plot.y(i+.5)-8);});
 for(const r of shown)drawPoint('timeline',{x:time(r),y:methods.indexOf(r.method)+.5,row:r},colors[r.method]||'#ddd',!click(r));
 const samples=[],past=all.filter(click);let begin=0,finish=0,counts={left:0,right:0,physical:0,artificial:0};
 const add=(r,v)=>{if(r.side in counts)counts[r.side]+=v;if(r.source in counts)counts[r.source]+=v;};
 const step=Math.max(50,(view[1]-view[0])/500);
 for(let t=view[0];t<=view[1]+step/2;t+=step){while(finish<past.length&&time(past[finish])<=t)add(past[finish++],1);while(begin<finish&&time(past[begin])<=t-1000)add(past[begin++],-1);samples.push({t,...counts});}
 const max=Math.max(5,...samples.map(s=>Math.max(s.left,s.right,s.physical,s.artificial)));const graph=canvas('cps',max,'CPS');
 for(const [key,color] of Object.entries({left:'#93c5fd',right:'#fca5a5',physical:'#7dd3fc',artificial:'#c4a5ff'})){graph.ctx.strokeStyle=color;graph.ctx.beginPath();samples.forEach((s,i)=>i?graph.ctx.lineTo(graph.x(s.t),graph.y(s[key])):graph.ctx.moveTo(graph.x(s.t),graph.y(s[key])));graph.ctx.stroke();graph.ctx.fillStyle=color;graph.ctx.fillText(key,65+Object.keys({left:0,right:0,physical:0,artificial:0}).indexOf(key)*100,14);}
 canvas('intervals',Math.min(2000,ints.reduce((max,p)=>Math.max(max,p.y),100)),'ms');for(const p of ints)drawPoint('intervals',{...p,y:Math.min(p.y,2000)},colors[p.row.method]||'#ddd');
 const bins=new Array(101).fill(0);for(const p of ints)bins[Math.min(100,Math.floor(p.y/10))]++;
 const histogram=canvas('histogram',Math.max(1,...bins),'Count'),hc=histogram.ctx,w=histogram.box.right-histogram.box.left;
 hc.clearRect(55,histogram.box.bottom+3,w+20,29);hc.fillStyle='#c4a5ff';bins.forEach((n,i)=>hc.fillRect(histogram.box.left+i*w/101,histogram.y(n),Math.max(1,w/101-1),histogram.box.bottom-histogram.y(n)));hc.fillStyle='#9cacc3';for(let i=0;i<=5;i++)hc.fillText(i*200+(i===5?'+ ms':' ms'),histogram.box.left+w*i/5-12,$('histogram').clientHeight-10);
 $('start').value=Math.max(0,Math.min(1000,(view[0]-bounds[0])/(bounds[1]-bounds[0])*1000));$('end').value=Math.max(0,Math.min(1000,(view[1]-bounds[0])/(bounds[1]-bounds[0])*1000));
}
async function refresh(){
 if(busy)return;busy=true;
 try{const result=await(await fetch(`${location.pathname.replace(/\/$/, '')}/api/events`)).json();data=result.events;sessionInfo=result.sessions||{};
 const sessions=result.session_order||[...new Set(data.map(r=>r.session))].sort((a,b)=>(sessionInfo[a]?.timestamp_ms||0)-(sessionInfo[b]?.timestamp_ms||0));sessionOrder=sessions;const session=$('session');session.replaceChildren();
 const option=(value,label)=>{const o=document.createElement('option');o.value=value;o.textContent=label;session.append(o);};option('all','All sessions');option('newest','Newest session');option('older','Older sessions');
 for(const id of sessions){const info=sessionInfo[id]||{};const first=data.find(r=>r.session===id);option(id,new Date(info.timestamp_ms??first?.timestamp_ms??0).toLocaleString()+' · '+(info.mod_version||'unknown version')+' · '+(info.click_count??0)+' clicks · '+id.slice(0,8));}
 if(!['all','newest','older'].includes(selected)&&!sessions.includes(selected)){selected='newest';view=null;}session.value=selected;
 if(!timingChosen)$('timing').value=defaultTiming();
 $('status').textContent=result.path+' · '+result.retained+' retained / '+result.seen+' event records · '+result.malformed+' malformed lines skipped';
 $('notice').textContent=result.error||(!data.length?'No clicks yet. Enable Debug mode in ClickAssist → Advanced and start clicking.':result.seen>result.retained?'Only the most recent records are shown; use --max-events to retain more.':'');
 if($('live').checked&&follow)view=null;
 render();
 }catch(error){$('notice').textContent='Could not refresh: '+error.message;}finally{busy=false;}
}
$('session').onchange=()=>{selected=$('session').value;if(!timingChosen)$('timing').value=defaultTiming();view=null;render();};$('timing').onchange=()=>{timingChosen=true;view=null;render();};$('side').onchange=$('releases').onchange=()=>{view=null;render();};$('reset').onclick=()=>{follow=true;view=null;render();};
for(const id of ['start','end'])$(id).oninput=()=>{follow=false;const lo=Math.min(+$('start').value,+$('end').value-1),hi=Math.max(+$('end').value,lo+1);view=[bounds[0]+(bounds[1]-bounds[0])*lo/1000,bounds[0]+(bounds[1]-bounds[0])*hi/1000];render();};
$('download').onclick=()=>{const blob=new Blob([JSON.stringify(filtered().filter(r=>time(r)>=view[0]&&time(r)<=view[1]),null,2)],{type:'application/json'});const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='orven-bw-filtered-clicks.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);};
for(const id of ['timeline','cps','intervals']){
 const c=$(id);let drag=null;
 c.addEventListener('wheel',event=>{event.preventDefault();follow=false;const p=plots.get(id),ratio=Math.clamp?Math.clamp((event.offsetX-p.box.left)/(p.box.right-p.box.left),0,1):Math.max(0,Math.min(1,(event.offsetX-p.box.left)/(p.box.right-p.box.left)));const focus=view[0]+ratio*(view[1]-view[0]),span=Math.max(20,(view[1]-view[0])*Math.exp(event.deltaY*.001));view=[focus-ratio*span,focus+(1-ratio)*span];render();},{passive:false});
 c.onpointerdown=e=>{follow=false;drag={x:e.clientX,view:[...view]};c.setPointerCapture(e.pointerId);};c.onpointerup=()=>{drag=null;};c.onpointercancel=()=>{drag=null;};
 c.onpointermove=e=>{const p=plots.get(id);if(drag){const shift=(e.clientX-drag.x)/(p.box.right-p.box.left)*(drag.view[1]-drag.view[0]);view=drag.view.map(t=>t-shift);render();return;}const point=p.points.reduce((best,point)=>Math.hypot(point.px-e.offsetX,point.py-e.offsetY)<(best?Math.hypot(best.px-e.offsetX,best.py-e.offsetY):12)?point:best,null);const tip=$('tooltip');if(!point){tip.style.display='none';return;}const r=point.row;tip.textContent='Observed '+new Date(r.timestamp_ms).toISOString()+'\n'+r.source+' · '+r.method+' · '+r.side+' · '+r.action+(nativeMode()?(r._timing_fallback?' · observation fallback':' · native mouse timing'):(plannedMode()?' · intended deadline':' · observation timing'))+(r.profile?' · '+r.profile:'')+(Number.isFinite(r._dispatch_lateness_ms)?'\nDispatch lateness '+r._dispatch_lateness_ms.toFixed(3)+' ms':'')+'\nSequence '+r.sequence+' · '+(id==='intervals'?point.y.toFixed(3)+' ms interval':'relative time '+time(r).toFixed(3)+' ms');tip.textContent+=(r.origin_id?'\nOrigin '+r.origin_id:'')+(r.action_id?' · action '+r.action_id:'')+(r.packet_kind?' · '+r.packet_kind:'');tip.style.display='block';tip.style.left=Math.min(e.clientX+12,window.innerWidth-350)+'px';tip.style.top=Math.max(0,e.clientY-90)+'px';};c.onpointerleave=()=>{$('tooltip').style.display='none';};
}
window.addEventListener('resize',render);refresh();setInterval(()=>{if($('live').checked)refresh();},1000);
