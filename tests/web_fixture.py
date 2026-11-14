#!/usr/bin/env python3
"""Loopback-only UI preview with synthetic data. No TV/router/light connections."""
from http.server import ThreadingHTTPServer,BaseHTTPRequestHandler
from pathlib import Path
import json,time,argparse,copy,uuid
parser=argparse.ArgumentParser();parser.add_argument('--port',type=int,default=9040);args=parser.parse_args()
page=Path(__file__).resolve().parents[1]/'app/assets/settings/index.html'
state={'version':'0.13.1','device':{'manufacturer':'Philips','model':'TEST','displayName':'Philips Test TV','productModel':'55TEST/12'},'display':{'camera':True,'clock':True,'date':True,'homeTitle':'Zuhause'},'cameras':[{'id':'fixture-1','name':'Eingang','available':True,'host':'Lokale Quelle','status':'ready'},{'id':'fixture-2','name':'Garten','available':False,'status':'missing'}],'defaultCameraId':'fixture-1'}
saver={'style':'clock','date':True,'dim':True}
hue={'configured':True,'connected':True,'host':'10.0.0.2','roomId':'1','roomName':'TV-Zimmer','rooms':[{'id':'1','name':'TV-Zimmer'},{'id':'2','name':'Küche'}]}
state['interface']={'language':'de','textFont':'sans','clockFont':'sans'}
state['appearance']={'theme':'nordlicht','layout':'focus','order':['ambilight','cameras','hue','tv','inputs','system','ard','zdf','app3','app4']}
state['integrations']={'hue':True,'unifi':True}
inventory=[{'id':'ard','name':'ARD Mediathek','available':True},{'id':'zdf','name':'ZDF Mediathek','available':True},{'id':'pkg:tv.test.netflix','name':'Netflix','available':True},{'id':'pkg:tv.test.youtube','name':'YouTube','available':True},{'id':'pkg:tv.test.music','name':'Radio','available':True}]
def app_view(value):
 ids=value.get('apps',['ard','zdf']);enabled=value.get('enabled',False);return {'enabled':enabled,'apps':ids,'inventory':copy.deepcopy(inventory),'limit':4,'ard':enabled and 'ard' in ids,'zdf':enabled and 'zdf' in ids,'ardAvailable':True,'zdfAvailable':True}
state['appShortcuts']=app_view({'enabled':False,'apps':['ard','zdf']})
state['remote']={'red':'cameras','green':'ambilight','yellow':'hue','blue':'inputs'}
def device_info():return {'productModel':state['device'].get('productModel',''),'productModelSource':'stored' if state['device'].get('productModel') else 'unavailable','manufacturer':'Philips','platformModel':'TEST','androidVersion':'8.0.0','apiLevel':26,'securityPatch':'2019-01-01','hardware':'Test hardware','abis':['armeabi-v7a'],'appVersion':'0.13.1','appVersionCode':29,'width':1920,'height':1080,'densityDpi':320,'uptimeMs':7260000,'storageFreeBytes':3000000000,'storageTotalBytes':8000000000,'checkedAt':int(time.time()*1000)}
automation={'enabled':False,'ready':True,'lastAttemptAt':0,'lastResult':'none'}
custom_profiles=[]
profile_type='standard'
selected_profile=''
def profile_config():
 return {**{k:copy.deepcopy(state[k]) for k in ['appearance','integrations','interface','remote']},'type':profile_type,'display':{k:state['display'][k] for k in ['camera','clock','date']},'appShortcuts':{k:copy.deepcopy(state['appShortcuts'][k]) for k in ['enabled','apps']}}
def builtin_profile(media=False):
 config=profile_config();config['type']='media' if media else 'standard';config['display']={'camera':not media,'clock':True,'date':True};config['appearance']={'theme':'nordlicht','layout':'focus','order':['ambilight','ard','zdf','app3','app4','cameras','hue','tv','inputs','system'] if media else ['ambilight','cameras','hue','tv','inputs','system','ard','zdf','app3','app4']};config['integrations']={'hue':not media,'unifi':not media};config['appShortcuts']={'enabled':media,'apps':list(state['appShortcuts']['apps'])};config['interface']={'language':'de','textFont':'sans','clockFont':'sans'};config['remote']={'red':'cameras','green':'ambilight','yellow':'hue','blue':'inputs'};return config
def comparable(config):
 config=copy.deepcopy(config);slots=['ard','zdf','app3','app4'];count=len(config['appShortcuts']['apps']) if config['appShortcuts']['enabled'] else 0;config['appearance']['order']=[i for i in config['appearance']['order'] if i not in slots or slots.index(i)<count];return config
def profile_view():
 current=profile_config();profiles=[{'id':'standard','name':'Standard','builtin':True,'config':builtin_profile()},{'id':'media','name':'Mediathek','builtin':True,'config':builtin_profile(True)}]+copy.deepcopy(custom_profiles)
 active=next((p['id'] for p in profiles if comparable(p['config'])==comparable(current)),'');selected=next((p for p in profiles if p['id']==selected_profile),None)
 if selected:active=selected_profile if comparable(selected['config'])==comparable(current) else ''
 selected_id=selected['id'] if selected else active or profile_type
 return {'current':current,'profiles':profiles,'activeId':active,'selectedId':selected_id,'modified':selected_id!=active,'limit':8}
class Handler(BaseHTTPRequestHandler):
 def log_message(self,*args):pass
 def send(self,data,status=200):
  body=data if isinstance(data,bytes) else json.dumps(data).encode();self.send_response(status);self.send_header('Content-Type','text/html; charset=utf-8' if isinstance(data,bytes) else 'application/json');self.send_header('Cache-Control','no-store');self.end_headers();self.wfile.write(body)
 def do_GET(self):
  if self.path=='/':return self.send(page.read_text().replace('__ATPD_UI_CONFIG__',json.dumps(state['interface'],ensure_ascii=False,separators=(',',':'))).replace('__ATPD_UI_EN__',(page.parents[1]/'i18n/en.json').read_text()).encode())
  if self.path=='/api/state':return self.send(state)
  if self.path=='/api/profiles':return self.send(profile_view())
  if self.path=='/api/app-shortcuts':return self.send(state['appShortcuts'])
  if self.path.startswith('/api/hue/') and not state['integrations']['hue']:return self.send({'message':'Hue ist auf diesem TV deaktiviert.'},409)
  if self.path.startswith('/api/unifi/') and not state['integrations']['unifi']:return self.send({'message':'UniFi ist auf diesem TV deaktiviert.'},409)
  if self.path=='/api/screensaver/state':return self.send(saver)
  if self.path=='/api/device/state':return self.send(device_info())
  if self.path=='/api/device/diagnostic':return self.send({'format':'tv-dashboard-diagnostics-v1','device':{'manufacturer':'Philips','platformModel':'TEST','appVersion':'0.13.1'},'network':{'transport':'WLAN','internet':'validated'}})
  if self.path=='/api/unifi/state':return self.send({'configured':True,'host':'10.0.0.1','siteId':'fixture-site','ready':True,'checkedAt':int(time.time()*1000),'sites':[{'id':'fixture-site','name':'Zuhause'}],'devices':[{'name':'Wohnzimmer AP','model':'U7 Test','state':'ONLINE'},{'name':'Gateway','model':'Test Gateway','state':'ONLINE'},{'name':'Switch','model':'Test Switch','state':'OFFLINE'}]})
  if self.path=='/api/hue/automation':return self.send(automation)
  if self.path=='/api/hue/state':return self.send(hue)
  if self.path=='/api/inputs/state':return self.send({'inputs':[{'name':'Apple TV','port':'HDMI 4','status':'Verbunden'},{'name':'HDMI 1','port':'HDMI 1','status':'Standby'}]})
  if self.path=='/api/network/state':return self.send({'source':'android','connected':True,'transport':'LAN','internet':'validated','address':'10.0.0.20','ssid':'Zuhause','checkedAt':int(time.time()*1000)})
  self.send({'message':'Not found'},404)
 def do_POST(self):
  global profile_type,selected_profile
  data=json.loads(self.rfile.read(int(self.headers.get('Content-Length','0'))) or b'{}')
  if self.path.startswith('/api/profiles/'):
   try:
    action=self.path.split('/')[-1];entry=next((p for p in profile_view()['profiles'] if p['id']==data.get('id')),None)
    if action=='apply':
     if not entry:raise ValueError()
     config=copy.deepcopy(entry['config']);profile_type=config.pop('type');selected_profile=entry['id'];state['display'].update(config.pop('display'));state['appShortcuts']=app_view(config.pop('appShortcuts'));state.update(config)
    elif action in ('save','rename'):
     name=data['name'].strip()
     if not name or len(name)>40 or any(ord(c)<32 for c in name) or name.casefold() in ('standard','mediathek') or any(p['name'].casefold()==name.casefold() and p['id']!=data.get('id') for p in custom_profiles):raise ValueError()
     if data.get('id'):
      if not entry or entry['builtin']:raise ValueError()
      item=next(p for p in custom_profiles if p['id']==data['id']);item['name']=name
      if action=='save':item['config']={**profile_config(),'type':data.get('type',profile_type)}
     else:
      if action!='save' or len(custom_profiles)>=8:raise ValueError()
      custom_profiles.append({'id':'custom-'+str(uuid.uuid4()),'name':name,'builtin':False,'config':{**profile_config(),'type':data.get('type',profile_type)}})
    elif action=='delete':
     if not entry or entry['builtin']:raise ValueError()
     custom_profiles.remove(next(p for p in custom_profiles if p['id']==data['id']))
    else:raise ValueError()
    return self.send(profile_view())
   except (ValueError,KeyError):return self.send({'message':'Bitte Profilname und Auswahl prüfen.'},400)
  if self.path=='/api/app-shortcuts':
   if set(data)!={'enabled','apps'} or type(data['enabled']) is not bool or not isinstance(data['apps'],list) or len(data['apps'])>4 or any(not isinstance(i,str) for i in data['apps']) or len(set(data['apps']))!=len(data['apps']) or any(i not in [a['id'] for a in inventory] for i in data['apps']):return self.send({'message':'Invalid app selection'},400)
   state['appShortcuts']=app_view(data);return self.send(state['appShortcuts'])
  if self.path=='/api/integrations':
   if set(data)!={'hue','unifi'} or any(type(v) is not bool for v in data.values()):return self.send({'message':'Invalid integration settings'},400)
   state['integrations']=data;return self.send(data)
  if self.path=='/api/display':
   state['display']={key:data[key] for key in ['camera','clock','date','homeTitle']};state['device']['displayName']=data.get('deviceName') or 'Philips Test TV'
  elif self.path=='/api/interface':state['interface']=data;return self.send(data)
  elif self.path=='/api/appearance':state['appearance']=data;return self.send(data)
  elif self.path=='/api/remote/configure':state['remote']=data;return self.send(data)
  elif self.path=='/api/device/model':state['device']['productModel']=data['model'];return self.send(device_info())
  elif self.path=='/api/screensaver/configure':saver.update(data);return self.send(saver)
  elif self.path=='/api/hue/automation':automation['enabled']=data['enabled'];return self.send(automation)
  elif self.path=='/api/hue/room':hue.update(roomId=data['id'],roomName=next(r['name'] for r in hue['rooms'] if r['id']==data['id']));return self.send(hue)
  elif self.path=='/api/hue/configure':hue.update(host=data['host']);return self.send(hue)
  elif self.path=='/api/hue/pair':return self.send(hue)
  elif self.path=='/api/camera/delete':state['cameras']=[c for c in state['cameras'] if c['id']!=data['id']]
  elif self.path=='/api/camera':
   old=next((c for c in state['cameras'] if c['id']==data['id']),None)
   if old:old['name']=data['name']
   else:state['cameras'].append({'id':'fixture-new','name':data['name'],'available':bool(data['url']),'status':'missing'})
   if data.get('setDefault'):state['defaultCameraId']=data['id'] or 'fixture-new'
  self.send({'ok':True})
print(f'Synthetic preview: http://127.0.0.1:{args.port}',flush=True)
ThreadingHTTPServer(('127.0.0.1',args.port),Handler).serve_forever()
