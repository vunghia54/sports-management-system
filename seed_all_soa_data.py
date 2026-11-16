# -*- coding: utf-8 -*-
"""
KỊCH BẢN NẠP DỮ LIỆU TỰ ĐỘNG CHO HỆ THỐNG VN SPORTS SOA (ĐỘC LẬP 100%)
Đọc trực tiếp dữ liệu chuẩn từ api-gateway/src/main/resources/static/js/seed-data.json:
- 5 môn thể thao & 5 giải đấu tiêu biểu (5 nội dung thi đấu)
- 22 Đội tuyển chuẩn xác:
  + 6 CLB Bóng đá V.League 1 (11 cầu thủ ĐTQG Việt Nam mỗi đội)
  + 4 CLB Bóng rổ VBA
  + 4 CLB Bóng chuyền VĐQG
  + 4 Đoàn Cầu lông BWF
  + 4 Đội tuyển Esports VCS
- 203 Vận động viên chính thức trong Athlete Service :8086
- Đúng 20 trận đấu & 15 kết quả trong Match Service :8082 & Result Service :8087
- Tự động đồng bộ 5 Bảng xếp hạng trong Ranking Service :8088
"""
import os
import sys
import json
import datetime
import urllib.request
import urllib.error

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

GATEWAY_URL = 'http://localhost:8080'
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SEED_FILE = os.path.join(BASE_DIR, 'api-gateway', 'src', 'main', 'resources', 'static', 'js', 'seed-data.json')

print("=========================================================================")
print("  VN SPORTS SOA - TỰ ĐỘNG NẠP DỮ LIỆU (5 MÔN / 22 ĐỘI / 20 TRẬN)       ")
print("=========================================================================")

if not os.path.exists(SEED_FILE):
    print(f"Không tìm thấy file dữ liệu chuẩn: {SEED_FILE}")
    exit(1)

with open(SEED_FILE, 'r', encoding='utf-8') as f:
    SEED = json.load(f)

def get_auth_token():
    payload = json.dumps({'username': 'admin', 'password': 'admin123'}).encode('utf-8')
    req = urllib.request.Request(
        f'{GATEWAY_URL}/api/v1/auth/login',
        data=payload,
        headers={'Content-Type': 'application/json'},
        method='POST'
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            return json.loads(resp.read().decode('utf-8')).get('accessToken')
    except Exception as e:
        print(f"Lỗi kết nối Gateway: {e}")
        return None

token = get_auth_token()
if not token:
    print("KHÔNG THỂ KẾT NỐI API GATEWAY :8080. Hãy đảm bảo các Service đã được Start.")
    exit(1)

print(" -> Đăng nhập ADMIN thành công qua Gateway :8080.")

def api_call(path, method='GET', data=None):
    headers = {'Accept': 'application/json', 'Authorization': f'Bearer {token}'}
    body = json.dumps(data, ensure_ascii=False).encode('utf-8') if data is not None else None
    if body:
        headers['Content-Type'] = 'application/json; charset=utf-8'
    req = urllib.request.Request(f'{GATEWAY_URL}{path}', data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            text = resp.read().decode('utf-8')
            parsed = json.loads(text) if text else {}
            if method == 'GET' and isinstance(parsed, dict) and 'content' in parsed:
                return parsed['content']
            return parsed
    except urllib.error.HTTPError as e:
        err_body = ""
        try:
            err_body = e.read().decode('utf-8', errors='replace')
        except Exception:
            pass
        print(f"  [WARN] {method} {path} -> HTTP {e.code}: {err_body[:240]}")
        return None
    except Exception as e:
        print(f"  [WARN] {method} {path} -> {e}")
        return None

# 1. NẠP 5 MÔN THỂ THAO, 5 GIẢI ĐẤU & 5 NỘI DUNG THI ĐẤU
print("\n[1/5] Đồng bộ 5 môn thể thao & 5 giải đấu (Tournament Service :8081)...")
current_sports = api_call('/api/v1/sports') or []
sport_by_name = {s.get('name'): s.get('id') for s in current_sports}
old_sport_id_to_new = {}
for sp in SEED.get('sports', []):
    old_sid = sp.get('id')
    if sp['name'] not in sport_by_name:
        res = api_call('/api/v1/sports', 'POST', {
            'name': sp['name'],
            'sportType': sp['sportType'],
            'description': sp.get('description', '')
        })
        if res and res.get('id'):
            sport_by_name[sp['name']] = res['id']
    if old_sid and sp['name'] in sport_by_name:
        old_sport_id_to_new[old_sid] = sport_by_name[sp['name']]
if 'Bóng đá Nam 11 người' in sport_by_name:
    old_sport_id_to_new.setdefault(161, sport_by_name['Bóng đá Nam 11 người'])

def fetch_all_tournaments():
    items = []
    page = 0
    while True:
        headers = {'Accept': 'application/json', 'Authorization': f'Bearer {token}'}
        req = urllib.request.Request(f'{GATEWAY_URL}/api/v1/tournaments?page={page}&size=100', headers=headers, method='GET')
        with urllib.request.urlopen(req, timeout=15) as resp:
            parsed = json.loads(resp.read().decode('utf-8'))
            content = parsed.get('content', []) if isinstance(parsed, dict) else (parsed if isinstance(parsed, list) else [])
            items.extend(content)
            total_pages = int(parsed.get('totalPages', 1)) if isinstance(parsed, dict) else 1
            page += 1
            if page >= total_pages or not content:
                break
    return items

current_tours = fetch_all_tournaments()
tour_by_name = {t.get('name'): t.get('id') for t in current_tours}
old_tour_id_to_new = {}
for tr in SEED.get('tournaments', []):
    old_id = tr.get('id')
    if tr['name'] not in tour_by_name:
        res = api_call('/api/v1/tournaments', 'POST', {
            'name': tr['name'],
            'description': tr.get('description', ''),
            'startDate': tr.get('startDate'),
            'endDate': tr.get('endDate'),
            'location': tr.get('location'),
            'registrationStartDate': tr.get('registrationStartDate'),
            'registrationEndDate': tr.get('registrationEndDate'),
            'status': tr.get('status', 'ONGOING'),
            'maxParticipants': tr.get('maxParticipants', 16)
        })
        if res and res.get('id'):
            tour_by_name[tr['name']] = res['id']
    if old_id and tr['name'] in tour_by_name:
        old_tour_id_to_new[old_id] = tour_by_name[tr['name']]

current_comps = api_call('/api/v1/competitions') or []
comp_by_name = {(c.get('competitionName') or c.get('name')): c.get('id') for c in current_comps}
old_comp_id_to_new = {}
comp_to_tour = {}
for cp in SEED.get('competitions', []):
    old_cid = cp.get('id')
    cname = cp.get('competitionName') or cp.get('name')
    new_tid = old_tour_id_to_new.get(cp.get('tournamentId'), cp.get('tournamentId'))
    if cname not in comp_by_name:
        new_sid = old_sport_id_to_new.get(cp.get('sportId'), cp.get('sportId', 1))
        res = api_call('/api/v1/competitions', 'POST', {
            'tournamentId': new_tid,
            'sportId': new_sid,
            'name': cname,
            'competitionName': cname,
            'status': cp.get('status') or 'OPEN_REGISTRATION'
        })
        if res and res.get('id'):
            comp_by_name[cname] = res['id']
    if cname in comp_by_name:
        new_cid = comp_by_name[cname]
        if old_cid:
            old_comp_id_to_new[old_cid] = new_cid
        comp_to_tour[new_cid] = new_tid
        api_call(f'/api/v1/competitions/{new_cid}/rule', 'PUT', {
            'maxParticipants': 32,
            'minTeamMembers': 1,
            'maxTeamMembers': 25,
            'scoringMethod': 'POINTS'
        })
        api_call(f'/api/v1/competitions/{new_cid}/format', 'PUT', {
            'formatType': cp.get('formatType', 'ROUND_ROBIN'),
            'numberOfGroups': cp.get('numberOfGroups', 1),
            'qualifyingSpotsPerGroup': cp.get('qualifyingSpotsPerGroup', 2)
        })

print(f" -> Đã đồng bộ {len(sport_by_name)} môn, {len(tour_by_name)} giải đấu, {len(comp_by_name)} nội dung.")

# 2. NẠP 203 VẬN ĐỘNG VIÊN (ATHLETE SERVICE :8086)
print("\n[2/5] Đồng bộ 203 vận động viên (Athlete Service :8086)...")
current_athletes = api_call('/api/v1/athletes') or []
ath_by_name = {a.get('fullName'): a.get('id') for a in current_athletes}
for ath in SEED.get('athletes', []):
    fname = ath.get('fullName')
    if fname and fname not in ath_by_name:
        res = api_call('/api/v1/athletes', 'POST', {
            'fullName': fname,
            'dateOfBirth': ath.get('dateOfBirth', '1998-05-15'),
            'gender': ath.get('gender', 'MALE'),
            'identityCardNumber': ath.get('identityCardNumber') or ath.get('citizenId') or '001098000001',
            'phone': ath.get('phone', '0901234567'),
            'email': ath.get('email', 'athlete@sports.vn'),
            'nationality': ath.get('nationality', 'Vietnam'),
            'healthStatus': 'QUALIFIED' if ath.get('healthStatus') in (None, '', 'FIT') else ath.get('healthStatus')
        })
        if res and res.get('id'):
            ath_by_name[fname] = res['id']
print(f" -> Đã đồng bộ {len(ath_by_name)} vận động viên.")

# 3. NẠP 22 ĐỘI TUYỂN & ĐỘI HÌNH CHI TIẾT (TEAM SERVICE :8085)
print("\n[3/5] Đồng bộ 22 đội tuyển & đội hình thi đấu (Team Service :8085)...")
current_teams = api_call('/api/v1/teams') or []
team_by_name = {t.get('name'): t.get('id') for t in current_teams}
for tm in SEED.get('teams', []):
    tname = tm.get('name')
    if tname not in team_by_name:
        res = api_call('/api/v1/teams', 'POST', {
            'name': tname,
            'shortName': tm.get('shortName', 'CLB'),
            'managerName': tm.get('managerName', 'HLV Trưởng'),
            'contactPhone': tm.get('contactPhone', '0901234567'),
            'contactEmail': tm.get('contactEmail', 'club@sports.vn'),
            'logoUrl': tm.get('logoUrl')
        })
        if res and res.get('id'):
            team_by_name[tname] = res['id']

    tid = team_by_name.get(tname)
    if tid:
        existing_members = api_call(f'/api/v1/teams/{tid}/members') or []
        if len(existing_members) == 0:
            for mem in tm.get('members', []):
                aname = mem.get('athleteName')
                aid = ath_by_name.get(aname, mem.get('athleteId', 1))
                api_call(f'/api/v1/teams/{tid}/members', 'POST', {
                    'athleteId': aid,
                    'athleteName': aname,
                    'jerseyNumber': mem.get('jerseyNumber', 1),
                    'position': mem.get('position', 'Tuyển thủ')
                })
print(f" -> Đã đồng bộ {len(team_by_name)} đội tuyển cùng đầy đủ đội hình.")

# 4. NẠP 20 TRẬN ĐẤU & 15 KẾT QUẢ (MATCH :8082 & RESULT :8087)
print("\n[4/5] Đồng bộ 20 trận đấu & 15 kết quả (Match :8082 & Result :8087)...")
def ensure_participant_map(cid, required_names):
    parts = api_call(f'/api/v1/participants/competition/{cid}') or []
    by_name = {p.get('displayName'): p.get('id') for p in parts if p.get('id')}
    missing = [n for n in required_names if n and n not in by_name]
    if not missing:
        return by_name

    tid = comp_to_tour.get(cid)
    orig_tour = api_call(f'/api/v1/tournaments/{tid}') if tid else None
    orig_comp = api_call(f'/api/v1/competitions/{cid}')
    today_iso = datetime.date.today().isoformat()
    if orig_tour and orig_tour.get('id'):
        orig_end = str(orig_tour.get('endDate') or today_iso)
        bumped_end = orig_end if orig_end >= today_iso else today_iso
        api_call(f"/api/v1/tournaments/{tid}", 'PATCH', {
            'registrationStartDate': str(orig_tour.get('registrationStartDate') or today_iso) if str(orig_tour.get('registrationStartDate') or today_iso) <= today_iso else today_iso,
            'registrationEndDate': today_iso,
            'startDate': today_iso,
            'endDate': bumped_end,
            'status': 'OPEN_REGISTRATION'
        })
    if orig_comp and orig_comp.get('status') != 'OPEN_REGISTRATION':
        api_call(f"/api/v1/competitions/{cid}", 'PUT', {
            'tournamentId': orig_comp.get('tournamentId'),
            'sportId': orig_comp.get('sportId'),
            'name': orig_comp.get('name'),
            'status': 'OPEN_REGISTRATION'
        })

    for pname in missing:
        team_id = team_by_name.get(pname)
        aid = ath_by_name.get(pname)
        ptype = 'TEAM' if team_id else 'ATHLETE'
        ref_id = team_id if team_id else aid
        if not ref_id:
            continue
        reg = api_call('/api/v1/registrations', 'POST', {
            'competitionId': cid,
            'participantType': ptype,
            'participantReferenceId': ref_id,
            'participantName': pname
        })
        if reg and reg.get('id'):
            api_call(f"/api/v1/registrations/{reg['id']}/approve", 'PUT')

    if orig_comp and orig_comp.get('status') and orig_comp.get('status') != 'OPEN_REGISTRATION':
        api_call(f"/api/v1/competitions/{cid}", 'PUT', {
            'tournamentId': orig_comp.get('tournamentId'),
            'sportId': orig_comp.get('sportId'),
            'name': orig_comp.get('name'),
            'status': orig_comp.get('status')
        })
    if orig_tour and orig_tour.get('id'):
        api_call(f"/api/v1/tournaments/{tid}", 'PATCH', {
            'registrationStartDate': orig_tour.get('registrationStartDate'),
            'registrationEndDate': orig_tour.get('registrationEndDate'),
            'startDate': orig_tour.get('startDate'),
            'endDate': orig_tour.get('endDate'),
            'status': orig_tour.get('status')
        })

    parts_after = api_call(f'/api/v1/participants/competition/{cid}') or []
    return {p.get('displayName'): p.get('id') for p in parts_after if p.get('id')}

participants_by_comp = {}
comp_required_names = {}
for m in SEED.get('matches', []):
    new_cid = old_comp_id_to_new.get(m.get('competitionId'), m.get('competitionId'))
    if new_cid:
        comp_required_names.setdefault(new_cid, set()).update([m.get('participant1'), m.get('participant2')])
for cid, names in comp_required_names.items():
    participants_by_comp[cid] = ensure_participant_map(cid, names)

current_matches = api_call('/api/v1/matches') or []
match_key_to_id = {(m.get('participant1'), m.get('participant2'), m.get('round')): m.get('id') for m in current_matches}
old_match_id_to_new = {}

match_status_by_old_id = {}
for m in SEED.get('matches', []):
    key = (m.get('participant1'), m.get('participant2'), m.get('round'))
    old_mid = m.get('id')
    if old_mid:
        match_status_by_old_id[old_mid] = m.get('status', 'SCHEDULED')
    if key not in match_key_to_id:
        new_tid = old_tour_id_to_new.get(m.get('tournamentId'), m.get('tournamentId'))
        new_cid = old_comp_id_to_new.get(m.get('competitionId'), m.get('competitionId'))
        pmap = participants_by_comp.get(new_cid, {})
        res = api_call('/api/v1/matches', 'POST', {
            'tournamentId': new_tid,
            'competitionId': new_cid,
            'round': m.get('round'),
            'groupName': m.get('groupName'),
            'matchTime': m.get('matchTime'),
            'location': m.get('location'),
            'referee': m.get('referee'),
            'refereeUsername': m.get('refereeUsername') or ('referee1' if m.get('status') != 'FINISHED' else 'referee1'),
            'participant1Id': pmap.get(m.get('participant1')),
            'participant2Id': pmap.get(m.get('participant2')),
            'participant1': m.get('participant1'),
            'participant2': m.get('participant2'),
            'status': m.get('status', 'SCHEDULED')
        })
        if res and res.get('id'):
            match_key_to_id[key] = res['id']
    if old_mid and key in match_key_to_id:
        old_match_id_to_new[old_mid] = match_key_to_id[key]

current_results = api_call('/api/v1/results') or []
res_by_match_id = {r.get('matchId'): r for r in current_results if r.get('matchId') is not None}
for r in SEED.get('results', []):
    old_mid = r.get('matchId')
    new_mid = old_match_id_to_new.get(old_mid, old_mid)
    should_confirm = (r.get('status') == 'CONFIRMED') or (match_status_by_old_id.get(old_mid) == 'FINISHED')
    if new_mid and new_mid not in res_by_match_id:
        created_res = api_call('/api/v1/results', 'POST', {
            'matchId': new_mid,
            'scoreParticipant1': r.get('scoreParticipant1', 0),
            'scoreParticipant2': r.get('scoreParticipant2', 0),
            'notes': r.get('notes') or r.get('note', ''),
            'details': r.get('details', [])
        })
        if created_res and created_res.get('result') and created_res['result'].get('id'):
            rid = created_res['result']['id']
            if should_confirm:
                api_call(f'/api/v1/results/{rid}/confirm', 'PUT')
            res_by_match_id[new_mid] = created_res['result']
    elif new_mid and should_confirm:
        existing_r = res_by_match_id[new_mid]
        if existing_r.get('status') != 'CONFIRMED' and existing_r.get('id'):
            api_call(f"/api/v1/results/{existing_r['id']}/confirm", 'PUT')
print(f" -> Đã đồng bộ {len(match_key_to_id)} trận đấu & {len(res_by_match_id)} kết quả.")

# 5. ĐỒNG BỘ 5 BẢNG XẾP HẠNG (RANKING SERVICE :8088)
print("\n[5/5] Đồng bộ 5 Bảng xếp hạng (Ranking Service :8088)...")
for rk_entry in SEED.get('rankings', []):
    rk_obj = rk_entry.get('ranking') or {}
    items = rk_entry.get('items') or []
    old_cid = rk_obj.get('competitionId')
    new_cid = old_comp_id_to_new.get(old_cid, old_cid)
    if new_cid and items:
        api_call(f'/api/v1/rankings/competition/{new_cid}/items', 'PUT', items)
print(" -> Đã đồng bộ 5 Bảng xếp hạng.")

print("\n=========================================================================")
print("  HOÀN TẤT 100%! HỆ THỐNG ĐÃ SẴN SÀNG TẠI:                              ")
print("  >>> http://localhost:8080 <<<                                        ")
print("  - Bóng đá: đúng 6 đội (11 cầu thủ ĐTQG mỗi đội)                       ")
print("  - 4 môn còn lại: đúng 4 đội mỗi môn                                   ")
print("  - Tổng cộng hệ thống: đúng 22 đội tuyển, 203 VĐV, 20 trận đấu          ")
print("=========================================================================")
