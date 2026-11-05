"""Build attributed, country-level public-data snapshots. No event coordinates are published."""
import csv
import hashlib
import io
import json
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen
from zipfile import ZipFile
import xml.etree.ElementTree as ET
import sys
import gzip
import re
from email.utils import parsedate_to_datetime

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'work'
OUT = ROOT / 'backend/src/main/resources/data'
PUBLIC = ROOT / 'frontend/public/data'
URLS = {
    'countries': 'https://api.worldbank.org/v2/country?format=json&per_page=400',
    'history': 'https://ucdp.uu.se/downloads/organizedviolencecy/organizedviolencecy-261-csv.zip',
    'candidate': 'https://ucdp.uu.se/downloads/candidateged/GEDEvent_v26_01_26_09.csv',
    'world': 'https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_admin_0_countries.geojson',
    'landforms': 'https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_geography_regions_polys.geojson',
    'news': 'https://news.un.org/feed/subscribe/en/news/all/rss.xml'
}
INDICATORS = {'spending': 'MS.MIL.XPND.CD', 'personnel': 'MS.MIL.TOTL.P1', 'gdp': 'NY.GDP.MKTP.CD', 'population': 'SP.POP.TOTL'}
PENDING = {}

def fetch(url):
    with urlopen(Request(url, headers={'User-Agent': 'ConflictAtlas-Educational/1.0'}), timeout=60) as response:
        body=response.read(50_000_000)
        return gzip.decompress(body) if body[:2]==b'\x1f\x8b' else body

def dump(path, data):
    PENDING[path] = json.dumps(data, ensure_ascii=False, separators=(',', ':'))

def build():
    WORK.mkdir(exist_ok=True)
    fetched = datetime.now(timezone.utc).isoformat()
    raw = {}
    for key, url in URLS.items():
        print('Reading', key, flush=True)
        cache = WORK / f'source-{key}-{hashlib.sha256(url.encode()).hexdigest()[:12]}.bin'
        raw[key] = cache.read_bytes() if '--reuse-downloads' in sys.argv and cache.exists() and key!='news' else fetch(url)
        cache.write_bytes(raw[key])
    countries = {}
    for c in json.loads(raw['countries'].decode('utf-8-sig'))[1]:
        if c['region']['id'] == 'NA':
            continue
        countries[c['id']] = {'id': c['id'], 'name': c['name'], 'region': c['region']['value'], 'latitude': float(c['latitude'] or 0), 'longitude': float(c['longitude'] or 0), 'historical': False}
    aliases = {
        'United States of America':'USA','Russia (Soviet Union)':'RUS','Serbia (Yugoslavia)':'SRB',
        'DR Congo (Zaire)':'COD','Congo':'COG','Ivory Coast':'CIV','Turkey':'TUR','Egypt':'EGY',
        'Iran':'IRN','Syria':'SYR','Laos':'LAO','North Korea':'PRK','South Korea':'KOR',
        'Venezuela':'VEN','Yemen (North Yemen)':'YEM','Vietnam (North Vietnam)':'VNM',
        'Cambodia (Kampuchea)':'KHM','Myanmar (Burma)':'MMR','Madagascar (Malagasy)':'MDG',
        'Zimbabwe (Rhodesia)':'ZWE','Kingdom of eSwatini (Swaziland)':'SWZ','Samoa (Western Samoa)':'WSM',
        'Tanzania':'TZA','Gambia':'GMB','Kyrgyzstan':'KGZ','Slovakia':'SVK','Bahamas':'BHS',
        'Bosnia-Herzegovina':'BIH','Brunei':'BRN','Cape Verde':'CPV','East Timor':'TLS',
        'Federated States of Micronesia':'FSM','Kosovo':'XKX','Antigua & Barbuda':'ATG',
        'Saint Kitts and Nevis':'KNA','Saint Lucia':'LCA','Saint Vincent and the Grenadines':'VCT',
        'Czech Republic':'CZE','Czechoslovakia':'XCS','German Democratic Republic':'XDD','Somalia':'SOM','Nauru':'NRU',
        'Yemen (South Yemen)':'XSY','Taiwan':'TWN','Vatican City State':'VAT'
    }
    for iso, name, historical in [('XCS','Czechoslovakia',True),('XDD','German Democratic Republic',True),('XSY','South Yemen',True),('XNY','North Yemen',True),('SUN','Soviet Union',True),('YUG','Yugoslavia / predecessor series',True),('TWN','Taiwan',False),('VAT','Vatican City',False),('NRU','Nauru',False)]:
        countries[iso] = {'id':iso,'name':name,'region':'Historical entities' if historical else 'Other economies','latitude':0,'longitude':0,'historical':historical}
    names = {c['name'].casefold(): c['id'] for c in countries.values()}
    def identify(name, year):
        if name == 'Russia (Soviet Union)' and year < 1992: return 'SUN'
        if name == 'Serbia (Yugoslavia)' and year < 2006: return 'YUG'
        if name == 'Yemen (North Yemen)' and year < 1990: return 'XNY'
        iso = aliases.get(name) or names.get(name.casefold())
        if not iso: raise ValueError('Unmapped source country: '+name)
        return iso

    world = json.loads(raw['world'])
    features = []
    for f in world['features']:
        p = f['properties']; iso = p['WB_A3']
        if iso == '-99': iso = p['ISO_A3_EH']
        if iso not in countries:
            iso = p['ADM0_A3']
        if iso in countries:
            countries[iso]['longitude'] = p['LABEL_X']; countries[iso]['latitude'] = p['LABEL_Y']
        features.append({'type':'Feature','id':iso,'properties':{'id':iso,'name':p['NAME_EN'] or p['ADMIN'],'region':p['CONTINENT']},'geometry':f['geometry']})
    dump(PUBLIC/'world.geojson',{'type':'FeatureCollection','features':features})
    landforms = json.loads(raw['landforms'])
    physical = [{'type':'Feature','properties':{'name':f['properties'].get('NAME'),'type':f['properties'].get('FEATURECLA')},'geometry':f['geometry']} for f in landforms['features'] if f['properties'].get('FEATURECLA') in ['Range/mtn','Plateau','Desert','Basin','Plain','Tundra']]
    if len(physical)<10: raise ValueError('Unexpected Natural Earth landform schema.')
    dump(PUBLIC/'landforms.geojson',{'type':'FeatureCollection','features':physical})

    history = []
    with ZipFile(io.BytesIO(raw['history'])) as archive:
        filename = next(n for n in archive.namelist() if n.endswith('.csv'))
        reader = csv.DictReader(io.TextIOWrapper(archive.open(filename),encoding='utf-8-sig'))
        for r in reader:
            year = int(r['year']); iso = identify(r['country'],year)
            history.append({'country':iso,'sourceName':r['country'],'year':year,'best':int(r['cumulative_total_deaths_in_orgvio_best']), 'low':int(r['cumulative_total_deaths_in_orgvio_low']), 'high':int(r['cumulative_total_deaths_in_orgvio_high']), 'stateBased':int(r['sb_total_deaths_best']), 'nonState':int(r['ns_total_deaths_best']), 'oneSided':int(r['os_total_deaths_best'])})
    candidates = defaultdict(lambda:{'events':0,'best':0,'low':0,'high':0,'stateBased':0,'nonState':0,'oneSided':0})
    seen = set()
    for r in csv.DictReader(io.StringIO(raw['candidate'].decode('utf-8-sig'))):
        if r['id'] in seen: raise ValueError('Duplicate candidate event '+r['id'])
        seen.add(r['id']); month = r['date_start'][:7]; iso = identify(r['country'], int(r['year']))
        a = candidates[(iso,month)]; a['events'] += 1
        for k in ['best','low','high']: a[k] += int(r[k])
        a[{'1':'stateBased','2':'nonState','3':'oneSided'}[r['type_of_violence']]] += int(r['best'])
    candidate_rows = [{'country':iso,'month':month,**data} for (iso,month),data in sorted(candidates.items())]
    wb = {}; metric_receipts = []
    for key, indicator in INDICATORS.items():
        print('Reading indicator', key, flush=True)
        url = f'https://api.worldbank.org/v2/country/all/indicator/{indicator}?format=json&mrnev=1&per_page=1000'
        metric_raw = fetch(url)
        payload = json.loads(metric_raw.decode('utf-8-sig'))
        metric_receipts.append({'id':key,'url':url,'sha256':hashlib.sha256(metric_raw).hexdigest()})
        for r in payload[1] or []:
            if r['countryiso3code'] in countries and r['value'] is not None:
                wb.setdefault(r['countryiso3code'],{})[key] = {'value':r['value'],'year':int(r['date']),'indicator':indicator}
    dump(OUT/'countries.json',sorted(countries.values(),key=lambda c:c['name']))
    dump(OUT/'history.json',history)
    dump(OUT/'candidate.json',candidate_rows)
    dump(OUT/'metrics.json',{'fetchedAt':fetched,'countries':wb})
    root = ET.fromstring(raw['news'])
    topic=re.compile(r'\b(?:wars?|gaza|sudan|ukraine|lebanon|syria|militar\w*|peace\w*|humanitarian|ceasefire|displac\w*|conflict\w*|attack\w*|violence|aid|rafah|myanmar|armed|drone\w*|flee)\b',re.I)
    news = [{'title':item.findtext('title'),'url':item.findtext('link'),'publishedAt':parsedate_to_datetime(item.findtext('pubDate').replace(' CET',' +0100').replace(' CEST',' +0200')).isoformat(),'source':'UN News'} for item in root.findall('./channel/item') if topic.search(item.findtext('title') or '')][:15]
    dump(OUT/'news.json',{'fetchedAt':fetched,'items':news})
    issues = [{'country':r['country'],'period':str(r.get('year',r.get('month'))),'best':r['best'],'low':r['low'],'high':r['high'],'type':'source_interval_inconsistent'} for r in history+candidate_rows if not 0<=r['low']<=r['best']<=r['high']]
    manifest = {'fetchedAt':fetched,'historyVersion':'26.1','historyStart':1989,'historyEnd':2025,'candidateVersion':'26.01.26.09','candidateStart':'2026-01-01','candidateEnd':'2026-09-30','historyRows':len(history),'candidateEvents':len(seen),'countryCount':len(countries),'qualityIssues':issues,'sources':[{ 'id':key,'url':url,'sha256':hashlib.sha256(raw[key]).hexdigest()} for key,url in URLS.items()]+metric_receipts}
    dump(OUT/'manifest.json',manifest)
    # Complete all downloads/parsing before replacing any published output. Publish the manifest last.
    for path,payload in PENDING.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary=path.with_suffix(path.suffix+'.tmp');temporary.write_text(payload,encoding='utf-8');temporary.replace(path)
    print('Snapshot ready:',len(history),'country-years;',len(seen),'candidate events;',len(wb),'resource profiles')

if __name__ == '__main__': build()
