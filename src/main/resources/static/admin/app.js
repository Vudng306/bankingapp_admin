const $ = (s) => document.querySelector(s);
const state = {
  token: localStorage.getItem('adminToken') || '',
  section: localStorage.getItem('adminSection') || 'overview',
  page: 0,
  lastRows: [],
  lastColumns: [],
  securityTimer: null
};

const sections = [
  {id:'overview', title:'Dashboard tổng quan', desc:'Số liệu toàn hệ thống, biểu đồ giao dịch và ma trận tính năng theo DB hiện có.'},
  {id:'users', title:'Người dùng', desc:'Quản lý đăng ký, trạng thái khóa/mở, PIN, số dư tổng theo user.'},
  {id:'accounts', title:'Tài khoản ngân hàng', desc:'Theo dõi số tài khoản, số dư, loại tài khoản và trạng thái.'},
  {id:'transactions', title:'Giao dịch', desc:'Lịch sử chuyển tiền, nạp tiền, tiết kiệm; hỗ trợ lọc và xuất CSV/in PDF.'},
  {id:'savings', title:'Tiết kiệm', desc:'Quản lý sổ tiết kiệm, kỳ hạn, lãi suất, trạng thái tất toán.'},
  {id:'cards', title:'Thẻ ảo', desc:'Khóa/mở thẻ và cập nhật hạn mức giao dịch.'},
  {id:'devices', title:'Thiết bị đăng nhập', desc:'Quản lý thiết bị, push token, sinh trắc học và đăng xuất từ xa.'},
  {id:'notifications', title:'Thông báo', desc:'Xem, tạo và đánh dấu thông báo cho user.'},
  {id:'topups', title:'Nạp điện thoại', desc:'Theo dõi giao dịch nạp tiền điện thoại.'},
  {id:'security', title:'OTP & phiên chuyển tiền', desc:'Kiểm tra OTP đã dùng/hết hạn và transfer session xác nhận giao dịch.'}
];

const listConfigs = {
  users: {
    endpoint:'/admin-api/users',
    filters:`<input name="keyword" placeholder="Tìm tên, email, SĐT..."><select name="status"><option value="">Mọi trạng thái</option><option>ACTIVE</option><option>LOCKED</option></select>`,
    columns:[
      c('id','ID'), c('fullName','Họ tên'), c('email','Email'), c('phone','SĐT'), c('status','Trạng thái', badge), c('hasPin','PIN', yesNo), c('accountCount','Số TK'), c('totalBalance','Tổng số dư', money), c('createdAt','Ngày tạo', dateTime)
    ],
    actions:(r)=>[
      btn('Chi tiết',()=>detailUser(r.id)),
      btn(r.status==='ACTIVE'?'Khóa':'Mở',()=>patch(`/admin-api/users/${r.id}/status`,{status:r.status==='ACTIVE'?'LOCKED':'ACTIVE'}), r.status==='ACTIVE'?'danger':'ok')
    ]
  },
  accounts: {
    endpoint:'/admin-api/accounts',
    filters:`<input name="keyword" placeholder="Tìm STK, chủ TK, email..."><select name="status"><option value="">Mọi trạng thái</option><option>ACTIVE</option><option>LOCKED</option></select>`,
    columns:[
      c('id','ID'), c('accountNumber','Số tài khoản', mono), c('user.fullName','Chủ TK'), c('user.email','Email'), c('balance','Số dư', money), c('currency','Tiền tệ'), c('accountType','Loại'), c('status','Trạng thái', badge), c('createdAt','Ngày tạo', dateTime)
    ],
    actions:(r)=>[
      btn(r.status==='ACTIVE'?'Khóa':'Mở',()=>patch(`/admin-api/accounts/${r.id}/status`,{status:r.status==='ACTIVE'?'LOCKED':'ACTIVE'}), r.status==='ACTIVE'?'danger':'ok'),
      btn('Sửa số dư',()=>editBalance(r))
    ]
  },
  transactions: {
    endpoint:'/admin-api/transactions',
    filters:`<input name="keyword" placeholder="Mã GD, mô tả, STK, chủ TK..."><select name="type"><option value="">Mọi loại</option><option>INTERNAL</option><option>INTERBANK</option><option>TOPUP</option><option>SAVINGS_DEPOSIT</option><option>SAVINGS_WITHDRAW</option></select><select name="status"><option value="">Mọi trạng thái</option><option>PENDING</option><option>SUCCESS</option><option>FAILED</option></select><input type="date" name="fromDate"><input type="date" name="toDate">`,
    columns:[
      c('id','ID'), c('referenceCode','Mã tham chiếu', mono), c('type','Loại'), c('status','Trạng thái', badge), c('amount','Số tiền', money), c('fee','Phí', money), c('fromAccount.accountNumber','Từ TK', mono), c('toAccount.accountNumber','Đến TK', mono), c('toExternalAccount','TK ngoài', mono), c('toBankCode','Bank'), c('description','Nội dung'), c('createdAt','Ngày tạo', dateTime)
    ],
    actions:(r)=>[
      btn('Xem',()=>showModal(r)),
      btn('Trạng thái',()=>editTxStatus(r))
    ]
  },
  savings: {
    endpoint:'/admin-api/savings',
    filters:`<input name="keyword" placeholder="Tên, email, STK nguồn..."><select name="status"><option value="">Mọi trạng thái</option><option>ACTIVE</option><option>MATURED</option><option>WITHDRAWN</option></select>`,
    columns:[c('id','ID'), c('user.fullName','User'), c('sourceAccount.accountNumber','TK nguồn', mono), c('principal','Gốc', money), c('interestRate','Lãi %'), c('termMonths','Kỳ hạn'), c('accruedInterest','Lãi tạm tính', money), c('status','Trạng thái', badge), c('maturityDate','Đáo hạn')],
    actions:(r)=>[btn('Trạng thái',()=>editSavingsStatus(r))]
  },
  cards: {
    endpoint:'/admin-api/cards',
    filters:`<input name="keyword" placeholder="Số thẻ, chủ thẻ, STK..."><select name="status"><option value="">Mọi trạng thái</option><option>ACTIVE</option><option>LOCKED</option></select>`,
    columns:[c('id','ID'), c('cardNumber','Số thẻ', mono), c('cardholderName','Chủ thẻ'), c('account.accountNumber','STK', mono), c('expiryDate','Hết hạn'), c('dailyLimit','Hạn mức/ngày', money), c('status','Trạng thái', badge), c('createdAt','Ngày tạo', dateTime)],
    actions:(r)=>[
      btn(r.status==='ACTIVE'?'Khóa':'Mở',()=>patch(`/admin-api/cards/${r.id}`,{status:r.status==='ACTIVE'?'LOCKED':'ACTIVE'}), r.status==='ACTIVE'?'danger':'ok'),
      btn('Hạn mức',()=>editCardLimit(r))
    ]
  },
  devices: {
    endpoint:'/admin-api/devices',
    filters:`<input name="keyword" placeholder="Tên máy, deviceId, user..."><select name="active"><option value="">Mọi thiết bị</option><option value="true">Đang active</option><option value="false">Đã đăng xuất</option></select>`,
    columns:[c('id','ID'), c('user.fullName','User'), c('deviceName','Tên máy'), c('deviceId','Device ID', mono), c('hasPushToken','Push token', yesNo), c('biometricEnabled','Sinh trắc', yesNo), c('active','Active', badgeBool), c('lastLoginAt','Lần cuối', dateTime)],
    actions:(r)=>[
      btn(r.active?'Đăng xuất từ xa':'Kích hoạt',()=>patch(`/admin-api/devices/${r.id}`,{active:!r.active}), r.active?'danger':'ok'),
      btn(r.biometricEnabled?'Tắt bio':'Bật bio',()=>patch(`/admin-api/devices/${r.id}`,{biometricEnabled:!r.biometricEnabled}))
    ]
  },
  notifications: {
    endpoint:'/admin-api/notifications',
    before: notificationForm,
    filters:`<input name="keyword" placeholder="Tiêu đề, nội dung, user..."><select name="type"><option value="">Mọi loại</option><option>TRANSACTION</option><option>BALANCE</option><option>SYSTEM</option></select><select name="read"><option value="">Tất cả</option><option value="false">Chưa đọc</option><option value="true">Đã đọc</option></select>`,
    columns:[c('id','ID'), c('user.fullName','User'), c('title','Tiêu đề'), c('content','Nội dung'), c('type','Loại'), c('read','Đã đọc', yesNo), c('createdAt','Ngày tạo', dateTime)],
    actions:(r)=>[btn(r.read?'Đánh dấu chưa đọc':'Đánh dấu đã đọc',()=>patch(`/admin-api/notifications/${r.id}/read`,{read:!r.read}))]
  },
  topups: {
    endpoint:'/admin-api/topups',
    filters:`<input name="keyword" placeholder="Nhà mạng, SĐT, mã GD, user...">`,
    columns:[c('id','ID'), c('carrier','Nhà mạng'), c('phoneNumber','SĐT', mono), c('faceValue','Mệnh giá', money), c('transaction.referenceCode','Mã GD', mono), c('transaction.status','Trạng thái', badge), c('transaction.fromAccount.user.fullName','User'), c('createdAt','Ngày tạo', dateTime)],
    actions:(r)=>[btn('Xem GD',()=>showModal(r.transaction))]
  }
};

function c(key,label,fmt){return {key,label,fmt};}
function btn(text,onClick,cls='secondary'){return {text,onClick,cls};}
function get(obj,path){return path.split('.').reduce((o,k)=>o==null?null:o[k],obj);}
function money(v){if(v==null||v==='')return '';return Number(v).toLocaleString('vi-VN')+' ₫';}
function dateTime(v){if(!v)return '';return String(v).replace('T',' ').slice(0,19);}
function mono(v){return v==null?'':`<span class="mono">${escapeHtml(v)}</span>`;}
function badge(v){return v==null?'':`<span class="badge ${escapeHtml(v)}">${escapeHtml(v)}</span>`;}
function badgeBool(v){return `<span class="badge ${v}">${v?'ACTIVE':'OFF'}</span>`;}
function yesNo(v){return v?'<span class="badge true">Có</span>':'<span class="badge false">Không</span>';}
function escapeHtml(v){return String(v??'').replace(/[&<>'"]/g,m=>({ '&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;' }[m]));}

async function api(path, opts={}){
  const headers = {'Content-Type':'application/json'};
  if(state.token) headers['X-Admin-Token'] = state.token;
  const res = await fetch(path,{...opts,headers:{...headers,...(opts.headers||{})}});
  const json = await res.json().catch(()=>({success:false,message:'Response không phải JSON'}));
  if(!res.ok || json.success===false){throw new Error(json.message || `HTTP ${res.status}`);}
  return json.data;
}

async function patch(path, body){
  await api(path,{method:'PATCH',body:JSON.stringify(body)});
  toast('Cập nhật thành công');
  loadSection();
}

function toast(msg){const el=$('#toast'); el.textContent=msg; el.classList.remove('hidden'); setTimeout(()=>el.classList.add('hidden'),2600);}
function showModal(obj){$('#modalBody').textContent=JSON.stringify(obj,null,2); $('#modal').classList.remove('hidden');}
function closeModal(){$('#modal').classList.add('hidden');}
window.closeModal=closeModal;

function init(){
  $('#loginForm').addEventListener('submit', login);
  $('#logoutBtn').addEventListener('click', logout);
  $('#refreshBtn').addEventListener('click', ()=>loadSection());
  $('#exportBtn').addEventListener('click', exportCsv);
  $('#printBtn').addEventListener('click', ()=>window.print());
  renderNav();
  if(state.token) showApp(); else showLogin();
}

async function login(e){
  e.preventDefault();
  $('#loginError').textContent='';
  try{
    const data=await api('/admin-api/login',{method:'POST',body:JSON.stringify({username:$('#username').value,password:$('#password').value})});
    state.token=data.token; localStorage.setItem('adminToken',data.token); showApp();
  }catch(err){$('#loginError').textContent=err.message;}
}
async function logout(){try{await api('/admin-api/logout',{method:'POST'});}catch(e){} localStorage.removeItem('adminToken'); state.token=''; showLogin();}
function showLogin(){$('#loginView').classList.remove('hidden'); $('#appView').classList.add('hidden');}
function showApp(){$('#loginView').classList.add('hidden'); $('#appView').classList.remove('hidden'); loadSection();}

function renderNav(){
  $('#nav').innerHTML=sections.map(s=>`<button data-id="${s.id}">${s.title}</button>`).join('');
  $('#nav').addEventListener('click',e=>{const b=e.target.closest('button'); if(!b)return; state.section=b.dataset.id; state.page=0; localStorage.setItem('adminSection',state.section); loadSection();});
}

async function loadSection(){
  sections.forEach(s=>{const b=document.querySelector(`#nav button[data-id="${s.id}"]`); if(b)b.classList.toggle('active',s.id===state.section);});
  const meta=sections.find(s=>s.id===state.section)||sections[0]; $('#pageTitle').textContent=meta.title; $('#pageDesc').textContent=meta.desc;
  try{
    if(state.section==='overview') return renderOverview();
    if(state.section==='security') return renderSecurity();
    return renderList(state.section);
  }catch(err){$('#content').innerHTML=`<div class="card error">${escapeHtml(err.message)}</div>`;}
}

async function renderOverview(){
  const d=await api('/admin-api/summary');
  state.lastRows=d.recentTransactions||[]; state.lastColumns=listConfigs.transactions.columns;
  const kpis=[
    ['Người dùng',d.totalUsers,`${d.activeUsers} active · ${d.lockedUsers} khóa`],
    ['Tổng số dư',money(d.totalBalance),'Trên toàn bộ tài khoản'],
    ['Giao dịch',d.totalTransactions,`${d.successfulTransactions} success · ${d.pendingTransactions} pending · ${d.failedTransactions} failed`],
    ['Tiền GD thành công',money(d.successfulAmount),'Tổng amount SUCCESS'],
    ['Tiết kiệm',d.totalSavings,`${d.activeSavings} sổ đang active`],
    ['Thẻ ảo',d.totalCards,`${d.activeCards} thẻ active`],
    ['Thiết bị',d.totalDevices,`${d.activeDevices} thiết bị active`],
    ['Thông báo chưa đọc',d.unreadNotifications,`${d.phoneTopups} lượt nạp điện thoại`]
  ];
  $('#content').innerHTML=`
    <div class="grid kpi-grid">${kpis.map(k=>`<div class="card kpi"><span>${k[0]}</span><strong>${k[1]}</strong><small>${k[2]}</small></div>`).join('')}</div>
    <div class="card table-card" style="margin-top:16px"><h3>Giao dịch gần đây</h3>${table(d.recentTransactions||[],listConfigs.transactions.columns,()=>[])}</div>
  `;
}

function bars(rows){
  if(!rows.length)return '<p class="muted">Chưa có dữ liệu giao dịch.</p>';
  const max=Math.max(...rows.map(r=>Number(r.successAmount||0)),1);
  return `<div class="bars">${rows.map(r=>`<div class="bar"><div title="${money(r.successAmount)}" style="height:${Math.max(6,Number(r.successAmount||0)/max*160)}px"></div><span>${escapeHtml(r.label)}</span></div>`).join('')}</div>`;
}
function featureHtml(f){return `<div class="feature"><div><b>${escapeHtml(f.name)}</b><br><small>${escapeHtml(f.storage)}</small></div>${f.supportedByDb?'<span class="badge true">Có DB</span>':'<span class="badge false">Thiếu DB</span>'}</div>`;}

async function renderList(id){
  const cfg=listConfigs[id]; state.lastRows=[]; state.lastColumns=cfg.columns;
  $('#content').innerHTML=`${cfg.before?cfg.before():''}<form id="filterForm" class="toolbar card">${cfg.filters||''}<button type="submit">Lọc</button></form><div id="tableArea" class="card table-card"><p class="muted">Đang tải...</p></div>`;
  $('#filterForm').addEventListener('submit',e=>{e.preventDefault(); state.page=0; loadTable(cfg);});
  loadTable(cfg);
}

async function loadTable(cfg){
  const form=$('#filterForm');
  const params=new URLSearchParams({page:String(state.page),size:'20'});
  if(form){new FormData(form).forEach((v,k)=>{if(String(v).trim())params.set(k,v);});}
  const data=await api(`${cfg.endpoint}?${params}`);
  state.lastRows=data.content||[];
  $('#tableArea').innerHTML=`${table(data.content||[],cfg.columns,cfg.actions||(()=>[]))}${pager(data)}`;
}

function table(rows,columns,actions){
  if(!rows.length)return '<p class="muted">Không có dữ liệu.</p>';
  return `<div class="table-wrap"><table><thead><tr>${columns.map(col=>`<th>${col.label}</th>`).join('')}<th>Thao tác</th></tr></thead><tbody>${rows.map(r=>`<tr>${columns.map(col=>`<td>${cell(r,col)}</td>`).join('')}<td><div class="row-actions">${actions(r).map(a=>`<button class="${a.cls}" data-action="${actionStore(a.onClick)}">${a.text}</button>`).join('')}</div></td></tr>`).join('')}</tbody></table></div>`;
}
const actionFns=[];
function actionStore(fn){actionFns.push(fn); return actionFns.length-1;}
document.addEventListener('click',e=>{const b=e.target.closest('[data-action]'); if(!b)return; const fn=actionFns[Number(b.dataset.action)]; if(fn)fn();});
function cell(row,col){const v=get(row,col.key); return col.fmt?col.fmt(v,row):escapeHtml(v??'');}
function pager(data){return `<div class="row-actions" style="padding:14px"><button class="secondary" ${data.page<=0?'disabled':''} onclick="state.page--;loadSection()">Trang trước</button><span class="muted" style="align-self:center">Trang ${data.page+1}/${Math.max(data.totalPages,1)} · ${data.totalElements} dòng</span><button class="secondary" ${!data.hasNext?'disabled':''} onclick="state.page++;loadSection()">Trang sau</button></div>`;}

async function renderSecurity(){
  $('#content').innerHTML=`
    <div class="grid two-col">
      <div class="card table-card"><h3>OTP</h3><form id="otpFilter" class="toolbar"><select name="purpose"><option value="">Mọi purpose</option><option>REGISTER</option><option>LOGIN</option><option>TRANSFER</option><option>RESET_PASSWORD</option></select><select name="used"><option value="">Tất cả</option><option value="false">Chưa dùng</option><option value="true">Đã dùng</option></select><button>Lọc</button></form><div id="otpTable"></div></div>
      <div class="card table-card"><h3>Transfer sessions</h3><form id="sessionFilter" class="toolbar"><select name="used"><option value="">Tất cả</option><option value="false">Chưa dùng</option><option value="true">Đã dùng</option></select><button>Lọc</button></form><div id="sessionTable"></div></div>
    </div>`;
  $('#otpFilter').addEventListener('submit',e=>{e.preventDefault(); loadSecurityTable('otp');});
  $('#sessionFilter').addEventListener('submit',e=>{e.preventDefault(); loadSecurityTable('session');});
  loadSecurityTable('otp'); loadSecurityTable('session');
}
async function loadSecurityTable(type){
  const isOtp=type==='otp'; const form=$(isOtp?'#otpFilter':'#sessionFilter'); const params=new URLSearchParams({page:'0',size:'20'});
  new FormData(form).forEach((v,k)=>{if(String(v).trim())params.set(k,v);});
  const data=await api(`${isOtp?'/admin-api/otps':'/admin-api/transfer-sessions'}?${params}`);
  const cols=isOtp?[c('id','ID'),c('user.fullName','User'),c('code','OTP',mono),c('purpose','Purpose'),c('channel','Kênh'),c('used','Đã dùng',yesNo),c('expiresAt','Hết hạn',dateTime),c('createdAt','Tạo lúc',dateTime)]:[c('id','ID'),c('user.fullName','User'),c('transferType','Loại'),c('fromAccountId','From ID'),c('toAccountNumber','To STK',mono),c('amount','Số tiền',money),c('used','Đã dùng',yesNo),c('expiresAt','Hết hạn',dateTime)];
  $(isOtp?'#otpTable':'#sessionTable').innerHTML=table(data.content||[],cols,(r)=>[btn('Xem',()=>showModal(r))]);
  state.lastRows=data.content||[]; state.lastColumns=cols;
}

function notificationForm(){
  return `<form id="createNotifForm" class="card form-grid" onsubmit="createNotification(event)">
    <input name="userId" placeholder="User ID" required>
    <input name="title" placeholder="Tiêu đề" required>
    <select name="type"><option>SYSTEM</option><option>TRANSACTION</option><option>BALANCE</option></select>
    <button type="submit">Tạo thông báo</button>
    <textarea name="content" class="wide" placeholder="Nội dung thông báo" style="grid-column:1/-1"></textarea>
  </form>`;
}
async function createNotification(e){
  e.preventDefault(); const body=Object.fromEntries(new FormData(e.target).entries());
  await api('/admin-api/notifications',{method:'POST',body:JSON.stringify(body)}); e.target.reset(); toast('Đã tạo thông báo'); loadSection();
}
window.createNotification=createNotification;

async function detailUser(id){const data=await api(`/admin-api/users/${id}`); showModal(data);}
function editBalance(r){const v=prompt('Nhập số dư mới',r.balance); if(v!=null)patch(`/admin-api/accounts/${r.id}/balance`,{balance:v});}
function editTxStatus(r){const v=prompt('Trạng thái mới: PENDING, SUCCESS, FAILED',r.status); if(v)patch(`/admin-api/transactions/${r.id}/status`,{status:v.trim().toUpperCase()});}
function editSavingsStatus(r){const v=prompt('Trạng thái mới: ACTIVE, MATURED, WITHDRAWN',r.status); if(v)patch(`/admin-api/savings/${r.id}/status`,{status:v.trim().toUpperCase()});}
function editCardLimit(r){const v=prompt('Nhập hạn mức/ngày. Để trống = bỏ hạn mức',r.dailyLimit??''); if(v!==null)patch(`/admin-api/cards/${r.id}`,{dailyLimit:v.trim()===''?null:v});}

function exportCsv(){
  if(!state.lastRows.length){toast('Không có dữ liệu để xuất');return;}
  const headers=state.lastColumns.map(c=>c.label);
  const rows=state.lastRows.map(r=>state.lastColumns.map(c=>csvVal(get(r,c.key))));
  const csv=[headers.map(csvVal).join(','),...rows.map(r=>r.join(','))].join('\n');
  const blob=new Blob(['\ufeff'+csv],{type:'text/csv;charset=utf-8'});
  const url=URL.createObjectURL(blob); const a=document.createElement('a');
  a.href=url; a.download=`bank-admin-${state.section}-${new Date().toISOString().slice(0,10)}.csv`; a.click(); URL.revokeObjectURL(url);
}
function csvVal(v){if(v&&typeof v==='object')v=JSON.stringify(v); return `"${String(v??'').replace(/"/g,'""')}"`;}
window.state=state; window.loadSection=loadSection;
init();
