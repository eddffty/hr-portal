export async function api(path,method='GET',body){
 const headers=body===undefined?{}:{'Content-Type':'application/json'};
 const token=sessionStorage.getItem('hr-session-token');
 if(token)headers.Authorization='Bearer '+token;
 else if(path!=='/login') {const error=new Error('Войдите в систему');error.status=401;throw error;}
 const response=await fetch('/api'+path,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
 if(!response.ok){const error=await response.json().catch(()=>({}));const failure=new Error(error.message||'Не удалось выполнить запрос');failure.status=response.status;throw failure;}
 if(path==='/login'){
  const nextToken=response.headers.get('X-Portal-Token');
  if(!nextToken)throw new Error('Сервер использует старую версию авторизации. Перезапустите приложение.');
  sessionStorage.setItem('hr-session-token',nextToken);
 }
 if(path==='/logout')sessionStorage.removeItem('hr-session-token');
 const text=await response.text();return text?JSON.parse(text):null;
}
