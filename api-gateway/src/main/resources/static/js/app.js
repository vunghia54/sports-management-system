/* ==========================================================================
   VN SPORTS SOA — FRONTEND APPLICATION ENGINE
   Role-Based Access Control: Admin, Organizer, Referee, Team Manager, Viewer, Guest
   ========================================================================== */

const PAGE_PERMISSIONS = {
  'public-home': null,
  'auth-page': null,
  'dashboard': 'reports.read_own_tournaments',
  'tournaments': 'tournaments.create',
  'tournament-detail': 'tournaments.read_public',
  'teams': 'teams.read',
  'athletes': 'athletes.manage_own',
  'schedule': 'matches.read_public',
  'match-detail': 'matches.read_public',
  'results': 'results.submit_assigned',
  'rankings': 'rankings.read_public',
  'reports': 'reports.read_own_tournaments',
  'users': 'users.manage',
  'settings': null
};

const ICONS = {
  home: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1V9.5z"/></svg>',
  lock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>',
  dashboard: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>',
  trophy: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/></svg>',
  bracket: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M3 6h5v4H3zM3 14h5v4H3zM16 10h5v4h-5z"/><path d="M8 8h4v8H8M12 12h4"/></svg>',
  shield: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
  users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
  calendar: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>',
  clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
  checkSquare: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>',
  chart: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>',
  fileText: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>',
  userCheck: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><polyline points="16 11 18 13 22 9"/></svg>',
  settings: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>'
};

const MENU_DEFINITIONS = [
  {
    groupTitle: 'Cổng thông tin',
    items: [
      { id: 'public-home', title: 'Trang chủ', iconSvg: ICONS.home, permission: null },
      { id: 'auth-page', title: 'Tài khoản', iconSvg: ICONS.lock, permission: null }
    ]
  },
  {
    groupTitle: 'Điều hành Giải đấu',
    items: [
      { id: 'dashboard', title: 'Tổng quan', iconSvg: ICONS.dashboard, permission: 'reports.read_own_tournaments' },
      { id: 'tournaments', title: 'Giải đấu', iconSvg: ICONS.trophy, badgeId: 'badgeTournaments', permission: 'tournaments.create' },
      { id: 'tournament-detail', title: 'Nhánh đấu', iconSvg: ICONS.bracket, permission: 'tournaments.read_public' }
    ]
  },
  {
    groupTitle: 'Lực lượng Tham dự',
    items: [
      { id: 'teams', title: 'Đội tuyển', iconSvg: ICONS.shield, badgeId: 'badgeTeams', permission: 'teams.read' },
      { id: 'athletes', title: 'Vận động viên', iconSvg: ICONS.users, badgeId: 'badgeAthletes', permission: 'athletes.manage_own' }
    ]
  },
  {
    groupTitle: 'Thi đấu & Kết quả',
    items: [
      { id: 'schedule', title: 'Lịch thi đấu', iconSvg: ICONS.calendar, badgeId: 'badgeMatches', permission: 'matches.read_public' },
      { id: 'match-detail', title: 'Chi tiết trận', iconSvg: ICONS.clock, permission: 'matches.read_public' },
      { id: 'results', title: 'Kết quả', iconSvg: ICONS.checkSquare, permission: 'results.submit_assigned' },
      { id: 'rankings', title: 'Bảng xếp hạng', iconSvg: ICONS.chart, permission: 'rankings.read_public' }
    ]
  },
  {
    groupTitle: 'Báo cáo & Hệ thống',
    items: [
      { id: 'reports', title: 'Báo cáo', iconSvg: ICONS.fileText, permission: 'reports.read_own_tournaments' },
      { id: 'users', title: 'Người dùng', iconSvg: ICONS.userCheck, permission: 'users.manage' },
      { id: 'settings', title: 'Cài đặt', iconSvg: ICONS.settings, permission: null }
    ]
  }
];

const PAGE_TITLES = {
  'public-home': 'Trang chủ',
  'auth-page': 'Đăng nhập & Đăng ký',
  'dashboard': 'Tổng quan Điều hành',
  'tournaments': 'Quản lý Giải đấu',
  'tournament-detail': 'Chi tiết Giải đấu & Nhánh đấu',
  'teams': 'Quản lý Đội tuyển',
  'athletes': 'Quản lý Vận động viên',
  'schedule': 'Lịch thi đấu',
  'match-detail': 'Chi tiết Trận đấu',
  'results': 'Quản lý Kết quả',
  'rankings': 'Bảng xếp hạng',
  'reports': 'Thống kê & Báo cáo',
  'users': 'Quản lý Người dùng',
  'settings': 'Cài đặt Hệ thống'
};

function createPagedSlice(defaultSize = 10, filters = {}) {
  return {
    items: [],
    page: 0,
    size: defaultSize,
    totalElements: 0,
    totalPages: 0,
    loading: false,
    error: null,
    loaded: false,
    filters
  };
}

function getSafeStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('vn_sports_user') || 'null');
  } catch {
    return null;
  }
}

const AppState = {
  currentPage: 'public-home',
  themeMode: localStorage.getItem('vn_sports_theme') || 'light',
  reducedMotion: localStorage.getItem('vn_sports_reduced_motion') === 'true',
  demoMode: false,
  demoAccounts: [],
  token: localStorage.getItem('vn_sports_jwt') || '',
  user: getSafeStoredUser(),
  permissions: ['tournaments.read_public', 'matches.read_public', 'rankings.read_public'],
  sessionNotice: '',
  handlingSessionExpiry: false,

  selectedTournamentId: null,
  selectedCompetitionId: null,
  selectedMatchId: null,
  tournamentDetailTab: 'bracket',
  homeQuickCompetitionId: null,
  dashboardChartCompetitionId: null,
  reportChartCompetitionId: null,
  selectedSportId: 'ALL',

  loading: {
    global: false,
    tournaments: false,
    sports: false,
    competitions: false,
    teams: false,
    athletes: false,
    matches: false,
    registrations: false,
    results: false,
    rankings: false,
    rankingItems: false,
    notifications: false,
    users: false,
    gatewayRoutes: false
  },
  errors: {},
  stale: {},
  stats: {
    tournaments: null,
    teams: null,
    athletes: null,
    matches: null,
    registrations: null,
    results: null
  },
  detail: {
    tournament: {
      id: null,
      _seq: 0,
      selectedCompetitionId: 'ALL',
      loading: false,
      error: null,
      resultsError: null,
      resultsStale: false,
      item: null,
      competitions: [],
      matches: [],
      results: []
    },
    match: {
      id: null,
      _seq: 0,
      loading: false,
      error: null,
      resultError: null,
      resultStale: false,
      item: null,
      result: null
    }
  },
  caches: {
    resultsByMatchId: {}
  },
  modalSeq: {
    resultForm: 0
  },
  exportingSystemReport: false,
  data: {
    tournaments: [],
    sports: [],
    competitions: [],
    teams: [],
    athletes: [],
    matches: [],
    registrations: [],
    results: [],
    rankings: [],
    referees: [],
    participantsByComp: {},
    notifications: [],
    users: [],
    gatewayRoutes: null
  },
  paged: {
    tournaments: createPagedSlice(10, { keyword: '', status: '' }),
    teams: createPagedSlice(9, { keyword: '' }),
    athletes: createPagedSlice(10, { keyword: '', gender: '', healthStatus: '' }),
    matches: createPagedSlice(10, { tournamentId: '', status: '', keyword: '', date: '' }),
    registrations: createPagedSlice(5, { tournamentId: '', competitionId: '', status: '', keyword: '' }),
    results: createPagedSlice(10, { status: '', winner: '' }),
    rankings: createPagedSlice(5, { keyword: '' }),
    rankingItems: createPagedSlice(10, { competitionId: '', keyword: '' }),
    users: createPagedSlice(10, { keyword: '', role: '' }),
    notifications: createPagedSlice(8, { read: '', keyword: '' })
  },
  charts: {},
  modalPrevFocusEl: null,
  globalSearchSeq: 0,
  globalSearchResults: null,
  globalSearchActiveIdx: -1
};

const RESOURCE_ERROR_LABELS = {
  tournaments: 'Dịch vụ Giải đấu',
  sports: 'Danh mục Môn thể thao',
  competitions: 'Nội dung Thi đấu',
  teams: 'Dịch vụ Đội tuyển',
  athletes: 'Dịch vụ Vận động viên',
  matches: 'Dịch vụ Lịch thi đấu',
  registrations: 'Dịch vụ Đăng ký',
  results: 'Dịch vụ Kết quả',
  rankings: 'Dịch vụ Bảng xếp hạng',
  rankingItems: 'Thứ hạng Đội / VĐV',
  notifications: 'Dịch vụ Thông báo',
  users: 'Dịch vụ Tài khoản',
  gatewayRoutes: 'Trạng thái Gateway/Eureka'
};

/* ---------- PERMISSION & FORMAT HELPERS ---------- */
function hasPermission(permission) {
  if (!permission) return true;
  if (!AppState.permissions || !Array.isArray(AppState.permissions)) return false;
  return AppState.permissions.includes(permission);
}

function getRoleDisplayName(role) {
  if (!role) return 'Khách vãng lai';
  const map = {
    ROLE_ADMIN: 'Quản trị viên',
    ROLE_ORGANIZER: 'Ban tổ chức',
    ROLE_REFEREE: 'Trọng tài',
    ROLE_TEAM_MANAGER: 'Lãnh đội / HLV',
    ROLE_VIEWER: 'Khán giả',
    ROLE_USER: 'Khán giả',
    GUEST: 'Khách vãng lai'
  };
  return map[role] || role;
}

function getPermissionDisplayName(perm) {
  const map = {
    'users.read': 'Xem tài khoản',
    'users.manage': 'Quản lý tài khoản',
    'roles.manage': 'Phân quyền vai trò',
    'tournaments.read_public': 'Xem giải đấu',
    'tournaments.create': 'Tạo giải đấu',
    'tournaments.manage': 'Quản lý & sửa giải đấu',
    'teams.read': 'Xem đội tuyển',
    'teams.manage_own': 'Quản lý đội tuyển',
    'athletes.manage_own': 'Quản lý vận động viên',
    'registrations.manage_own': 'Đăng ký tham gia giải',
    'registrations.review': 'Duyệt hồ sơ tham gia',
    'matches.read_public': 'Xem lịch thi đấu',
    'matches.schedule': 'Xếp & sửa lịch thi đấu',
    'results.submit_assigned': 'Nhập kết quả trận đấu',
    'results.approve': 'Xác nhận kết quả chính thức',
    'rankings.read_public': 'Xem bảng xếp hạng',
    'reports.read_own_tournaments': 'Xem tổng quan & báo cáo',
    'reports.read_all': 'Xuất báo cáo toàn hệ thống',
    'services.monitor': 'Thông tin kỹ thuật SOA'
  };
  return map[perm] || perm;
}

function formatSportType(sportType) {
  const map = {
    TEAM: 'Đồng đội',
    INDIVIDUAL: 'Cá nhân',
    DUAL: 'Đối kháng / Đôi'
  };
  return map[String(sportType || '').toUpperCase()] || sportType || 'Đồng đội';
}

function formatGender(gender) {
  const map = {
    MALE: 'Nam',
    FEMALE: 'Nữ',
    OTHER: 'Khác',
    NAM: 'Nam',
    'NỮ': 'Nữ'
  };
  return map[String(gender || '').toUpperCase()] || gender || 'Chưa rõ';
}

function formatEventType(eventType) {
  const map = {
    EVENT: 'Sự kiện hệ thống',
    MATCH_FINISHED: 'Trận đấu kết thúc',
    MATCH_CREATED: 'Lịch thi đấu mới',
    MATCH_SCHEDULED: 'Xếp lịch thi đấu',
    SCORE_UPDATED: 'Cập nhật tỷ số',
    REGISTRATION_APPROVED: 'Duyệt đăng ký',
    REGISTRATION_CREATED: 'Đăng ký mới',
    REGISTRATION_REJECTED: 'Từ chối đăng ký',
    TOURNAMENT_CREATED: 'Tạo giải đấu mới',
    RANKING_UPDATED: 'Cập nhật BXH'
  };
  return map[String(eventType || '').toUpperCase()] || eventType || 'Thông báo';
}

function formatTeamCrestInitials(name, shortName) {
  if (shortName && String(shortName).trim().length > 0 && String(shortName).trim().length <= 6) {
    return String(shortName).trim().toUpperCase();
  }
  const clean = String(shortName || name || 'Đ')
    .replace(/^(CLB|Câu lạc bộ|Đội tuyển|Đoàn)\s+/i, '')
    .trim();
  const words = clean.split(/\s+/).filter(Boolean);
  if (words.length === 0) return 'Đ';
  if (words.length === 1) return words[0].substring(0, 3).toUpperCase();
  return words.slice(0, 3).map(w => w[0]).join('').toUpperCase();
}

function statusBadgeHtml(status) {
  const s = String(status || '').toUpperCase();
  const successMap = {
    ONGOING: 'Đang diễn ra',
    FINISHED: 'Đã kết thúc',
    APPROVED: 'Đã duyệt',
    FIT: 'Đủ thể lực',
    QUALIFIED: 'Đủ điều kiện',
    CONFIRMED: 'Đã xác nhận',
    UP: 'Đang hoạt động',
    COMPLETED: 'Đã hoàn thành',
    ACTIVE: 'Hoạt động'
  };
  if (successMap[s]) return `<span class="badge badge-success">${successMap[s]}</span>`;

  const warningMap = {
    UPCOMING: 'Sắp khởi tranh',
    SCHEDULED: 'Chờ thi đấu',
    PENDING: 'Chờ duyệt',
    PENDING_CONFIRMATION: 'Chờ xác nhận',
    INJURED: 'Chấn thương',
    OPEN_REGISTRATION: 'Mở đăng ký',
    PAUSED: 'Tạm dừng',
    DRAFT: 'Bản nháp'
  };
  if (warningMap[s]) return `<span class="badge badge-warning">${warningMap[s]}</span>`;

  const dangerMap = {
    CANCELLED: 'Đã hủy',
    REJECTED: 'Từ chối',
    WITHDRAWN: 'Đã rút lui',
    SUSPENDED: 'Treo giò',
    DOWN: 'Mất kết nối',
    INACTIVE: 'Đã khóa',
    CLOSED: 'Đã đóng'
  };
  if (dangerMap[s]) return `<span class="badge badge-error">${dangerMap[s]}</span>`;
  return `<span class="badge badge-neutral">${escapeHtml(s || 'Chưa rõ')}</span>`;
}

function syncStatusBadgeHtml(result) {
  if (!result) return '';
  const status = String(result.status || '').toUpperCase();
  if (status !== 'CONFIRMED') return '';
  const sync = String(result.syncStatus || 'SYNCED').toUpperCase();
  if (sync === 'SYNCED') {
    return '<span class="badge badge-success">Đã đồng bộ Trận & BXH</span>';
  }
  if (sync === 'PENDING') {
    return '<span class="badge badge-warning">Đang chờ đồng bộ liên dịch vụ</span>';
  }
  if (sync === 'FAILED') {
    const suffix = result.syncPermanentFailure ? ' (Lỗi quyền/cố định)' : ` (Thử lại lần ${result.syncAttempts || 0})`;
    return `<span class="badge badge-error" title="${escapeHtml(result.syncError || '')}">Đồng bộ lỗi${escapeHtml(suffix)}</span>`;
  }
  return `<span class="badge badge-neutral">${escapeHtml(sync)}</span>`;
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function getTodayLocalIsoDate() {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/* ---------- AUTH & SESSION MANAGEMENT (NO DEMO PASSWORDS, NO SILENT ELEVATION) ---------- */
function setGuestState() {
  AppState.token = '';
  AppState.user = null;
  AppState.permissions = [
    'tournaments.read_public',
    'matches.read_public',
    'rankings.read_public'
  ];
  AppState.data.users = [];
  AppState.data.athletes = [];
  AppState.data.teams = [];
  AppState.data.referees = [];
  AppState.data.gatewayRoutes = null;
  localStorage.removeItem('vn_sports_jwt');
  localStorage.removeItem('vn_sports_user');
}

function isJwtExpired(token) {
  if (!token) return true;
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return true;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(c =>
      '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
    ).join(''));
    const payload = JSON.parse(jsonPayload);
    if (!payload.exp) return false;
    return payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

function handleSessionExpired(customMessage) {
  const hadAuth = Boolean(AppState.token || localStorage.getItem('vn_sports_jwt'));
  if (AppState.handlingSessionExpiry) return;
  AppState.handlingSessionExpiry = true;
  try {
    setGuestState();
    if (!document.getElementById('appModalBackdrop')?.hidden) {
      closeModal();
    }
    updateHeaderUserUI();
    renderSidebar();
    if (hadAuth || customMessage) {
      const msg = customMessage || 'Phiên đăng nhập đã hết hạn hoặc không còn hợp lệ. Vui lòng đăng nhập lại.';
      AppState.sessionNotice = msg;
      showToast(msg, 'warning');
      navigateTo('auth-page');
    }
  } finally {
    AppState.handlingSessionExpiry = false;
  }
}

async function initAuthState() {
  const token = localStorage.getItem('vn_sports_jwt');
  if (!token) {
    setGuestState();
    return;
  }
  if (isJwtExpired(token)) {
    handleSessionExpired('Phiên đăng nhập trước đó đã hết hạn. Vui lòng đăng nhập lại.');
    return;
  }
  AppState.token = token;
  try {
    const res = await apiCall('/api/v1/auth/me');
    if (res && res.data && res.data.username) {
      AppState.user = {
        id: res.data.id,
        username: res.data.username,
        fullName: res.data.fullName,
        email: res.data.email,
        role: res.data.role
      };
      AppState.permissions = Array.isArray(res.data.permissions) ? res.data.permissions : [];
      localStorage.setItem('vn_sports_user', JSON.stringify(AppState.user));
    } else {
      handleSessionExpired('Không thể xác thực thông tin phiên làm việc. Vui lòng đăng nhập lại.');
    }
  } catch (err) {
    if (err.status === 401 || err.status === 403) {
      if (AppState.currentPage !== 'auth-page') {
        handleSessionExpired('Phiên đăng nhập đã hết hạn hoặc không còn hợp lệ. Vui lòng đăng nhập lại.');
      }
    } else {
      setGuestState();
      AppState.errors.users = err.message;
    }
  }
}

async function apiCall(path, options = {}) {
  const isLoginRequest = path.includes('/api/v1/auth/login') || path.includes('/api/v1/auth/register');
  if (AppState.token && !isLoginRequest && isJwtExpired(AppState.token)) {
    handleSessionExpired('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    const expErr = new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    expErr.status = 401;
    throw expErr;
  }

  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  const hadToken = Boolean(AppState.token);
  if (hadToken) {
    headers['Authorization'] = `Bearer ${AppState.token}`;
  }

  let resp;
  try {
    resp = await fetch(path, Object.assign({}, options, { headers }));
  } catch {
    const netErr = new Error('Không thể kết nối tới máy chủ API Gateway. Vui lòng kiểm tra kết nối dịch vụ.');
    netErr.status = 0;
    throw netErr;
  }

  let body = null;
  const text = await resp.text();
  if (text) {
    try { body = JSON.parse(text); } catch { body = text; }
  }

  if (!resp.ok) {
    if (resp.status === 401 && !isLoginRequest) {
      const msg = hadToken
        ? 'Phiên đăng nhập đã hết hạn hoặc bị từ chối. Vui lòng đăng nhập lại.'
        : ((body && (body.message || body.error)) || 'Vui lòng đăng nhập để thực hiện thao tác này.');
      handleSessionExpired(msg);
      const authErr = new Error(msg);
      authErr.status = 401;
      throw authErr;
    }
    const rawMsg = (body && (body.message || body.error)) || `Lỗi hệ thống (HTTP ${resp.status})`;
    const err = new Error(rawMsg);
    err.status = resp.status;
    err.fieldErrors = (body && (body.errors || body.fieldErrors)) || null;
    err.conflicts = (body && Array.isArray(body.conflicts)) ? body.conflicts : null;
    err.body = body;
    throw err;
  }

  return { data: body, headers: resp.headers, status: resp.status };
}

/* ---------- ACCESSIBLE MODAL, FOCUS TRAP & TOASTS ---------- */
function showToast(message, type = 'success') {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  const titleText = type === 'error' ? 'Lỗi' : type === 'warning' ? 'Lưu ý' : 'Thành công';
  el.innerHTML = `<div><strong>${titleText}:</strong> ${escapeHtml(message)}</div>`;
  container.appendChild(el);
  if (window.gsap && !isReducedMotion()) {
    gsap.fromTo(el, { y: 16, autoAlpha: 0 }, { y: 0, autoAlpha: 1, duration: 0.2, ease: 'power2.out' });
  }
  setTimeout(() => el.remove(), 4200);
}

function getFocusableElements(container) {
  if (!container) return [];
  return Array.from(container.querySelectorAll(
    'a[href], button:not(:disabled), input:not(:disabled):not([type="hidden"]), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])'
  )).filter(el => el.offsetParent !== null || el === document.activeElement);
}

function openModal(title, htmlContent) {
  const backdrop = document.getElementById('appModalBackdrop');
  const dialog = backdrop?.querySelector('.modal-dialog');
  if (!backdrop || !dialog) return;

  if (backdrop.hidden) {
    AppState.modalPrevFocusEl = document.activeElement;
  }
  document.getElementById('modalTitle').textContent = title;
  document.getElementById('modalBodyContent').innerHTML = htmlContent;
  backdrop.hidden = false;
  document.body.style.overflow = 'hidden';

  if (window.gsap && !isReducedMotion()) {
    gsap.fromTo(dialog, { scale: 0.97, autoAlpha: 0 }, { scale: 1, autoAlpha: 1, duration: 0.18, ease: 'power2.out' });
  }

  requestAnimationFrame(() => {
    const focusables = getFocusableElements(dialog);
    const firstInput = focusables.find(el => el.id !== 'modalCloseBtn') || focusables[0] || dialog;
    firstInput.focus();
  });
}

function closeModal() {
  const backdrop = document.getElementById('appModalBackdrop');
  if (!backdrop || backdrop.hidden) return;
  backdrop.hidden = true;
  document.body.style.overflow = '';
  document.getElementById('modalBodyContent').innerHTML = '';

  const prev = AppState.modalPrevFocusEl;
  AppState.modalPrevFocusEl = null;
  if (prev && typeof prev.focus === 'function' && document.contains(prev)) {
    prev.focus();
  }
}

function openConfirmDeleteModal({ entityLabel, itemName, warningNote, onConfirm }) {
  openModal(`Xác nhận xóa ${entityLabel}`, `
    <form id="confirmDeleteForm" onsubmit="event.preventDefault()">
      <div class="form-error-banner" hidden></div>
      <p style="font-size:0.94rem;margin-bottom:12px;">
        Bạn có chắc chắn muốn xóa ${escapeHtml(entityLabel)} <strong>"${escapeHtml(itemName)}"</strong> không?
      </p>
      <p style="font-size:0.84rem;color:var(--text-secondary);background:var(--bg-surface-secondary);padding:10px 12px;border-radius:var(--radius-sm);margin-bottom:18px;">
        ${escapeHtml(warningNote || 'Hành động này không thể hoàn tác. Nếu đối tượng đang có dữ liệu liên kết, hệ thống sẽ từ chối xóa để bảo vệ toàn vẹn dữ liệu.')}
      </p>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy bỏ</button>
        <button type="submit" id="confirmDeleteBtn" class="btn btn-danger">Xác nhận xóa</button>
      </div>
    </form>
  `);

  const form = document.getElementById('confirmDeleteForm');
  form?.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    clearFormErrors(form);
    const unlock = lockFormSubmit(form, 'Đang xóa...');
    try {
      await onConfirm();
      closeModal();
    } catch (err) {
      showFormError(form, err);
    } finally {
      unlock();
    }
  });
}

/* ---------- FORM VALIDATION, LOCKING & INLINE ERRORS ---------- */
function lockFormSubmit(form, loadingText = 'Đang lưu...') {
  if (!form) return () => {};
  const btn = form.querySelector('button[type="submit"]');
  if (!btn) return () => {};
  const originalText = btn.textContent;
  btn.disabled = true;
  btn.textContent = loadingText;
  return () => {
    if (document.contains(btn)) {
      btn.disabled = false;
      btn.textContent = originalText;
    }
  };
}

function clearFormErrors(form) {
  if (!form) return;
  const banner = form.querySelector('.form-error-banner');
  if (banner) {
    banner.hidden = true;
    banner.textContent = '';
  }
  form.querySelectorAll('.input-error').forEach(el => el.classList.remove('input-error'));
  form.querySelectorAll('.field-error-msg').forEach(el => { el.textContent = ''; });
}

function setFieldError(form, fieldName, message) {
  if (!form) return false;
  const field = form.querySelector(`[data-field="${fieldName}"]`);
  if (!field) return false;
  field.classList.add('input-error');
  const group = field.closest('.form-group');
  if (group) {
    let msgEl = group.querySelector('.field-error-msg');
    if (!msgEl) {
      msgEl = document.createElement('div');
      msgEl.className = 'field-error-msg';
      group.appendChild(msgEl);
    }
    msgEl.textContent = message;
  }
  return true;
}

function showFormError(form, err) {
  const message = (err && err.message) || String(err || 'Thao tác thất bại');
  if (form) {
    let mappedAny = false;
    if (err && err.fieldErrors && typeof err.fieldErrors === 'object') {
      Object.entries(err.fieldErrors).forEach(([k, v]) => {
        if (setFieldError(form, k, String(v))) mappedAny = true;
      });
    }
    const conflictBox = form.querySelector('#conflictCheckResultBox');
    if (conflictBox && err && Array.isArray(err.conflicts) && err.conflicts.length > 0) {
      conflictBox.innerHTML = `
        <div style="padding:10px;border-radius:8px;background:var(--color-error-soft);color:var(--color-error);">
          <strong>Máy chủ từ chối lưu do ${err.conflicts.length} xung đột lịch:</strong><br/>
          ${err.conflicts.map(c => '• ' + escapeHtml(c.message || '')).join('<br/>')}
        </div>
      `;
    }
    const banner = form.querySelector('.form-error-banner');
    if (banner) {
      if (err && Array.isArray(err.conflicts) && err.conflicts.length > 0) {
        banner.innerHTML = `<strong>Xung đột lịch thi đấu (HTTP 409):</strong><br/>${err.conflicts.map(c => '• ' + escapeHtml(c.message || '')).join('<br/>')}`;
      } else {
        banner.textContent = mappedAny && message === 'Validation failed'
          ? 'Vui lòng kiểm tra lại các trường thông tin được đánh dấu bên dưới.'
          : message;
      }
      banner.hidden = false;
    }
  }
  showToast(message, 'error');
}

/* ---------- THEME & ACCESSIBILITY CONTROLS ---------- */
function applyTheme(mode) {
  AppState.themeMode = mode;
  localStorage.setItem('vn_sports_theme', mode);
  let resolved = mode;
  if (mode === 'system') {
    resolved = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  document.documentElement.setAttribute('data-theme', resolved);
  document.documentElement.setAttribute('data-theme-mode', mode);

  document.querySelectorAll('.theme-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.themeChoice === mode);
  });

  if (AppState.currentPage === 'dashboard' || AppState.currentPage === 'reports') {
    setTimeout(() => renderCurrentPage(false), 50);
  }
}

function isReducedMotion() {
  return AppState.reducedMotion || window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function toggleReducedMotion() {
  AppState.reducedMotion = !AppState.reducedMotion;
  localStorage.setItem('vn_sports_reduced_motion', String(AppState.reducedMotion));
  renderCurrentPage(false);
}

/* ---------- SERVER-SIDE DEMO CONFIG LOADER (DISABLED BY DEFAULT) ---------- */
async function loadServerDemoConfig() {
  try {
    const res = await fetch('/api/v1/demo/config', { cache: 'no-store' });
    if (!res.ok) {
      AppState.demoMode = false;
      AppState.demoAccounts = [];
      return;
    }
    const data = await res.json();
    if (data && data.enabled === true && Array.isArray(data.accounts) && data.accounts.length > 0) {
      AppState.demoMode = true;
      AppState.demoAccounts = data.accounts;
    } else {
      AppState.demoMode = false;
      AppState.demoAccounts = [];
    }
  } catch {
    AppState.demoMode = false;
    AppState.demoAccounts = [];
  }
}

/* ---------- DATA LOADING & RESILIENCE ---------- */
function unwrapList(data) {
  if (Array.isArray(data)) return data;
  if (data && Array.isArray(data.content)) return data.content;
  return [];
}

function hasCachedSliceData(value) {
  if (Array.isArray(value)) return value.length > 0;
  return Boolean(value && typeof value === 'object' && Object.keys(value).length > 0);
}

async function safeFetchSlice(key, url, transform = unwrapList) {
  AppState.loading[key] = true;
  try {
    const res = await apiCall(url);
    AppState.errors[key] = null;
    AppState.stale[key] = false;
    AppState.data[key] = transform(res.data);
    return { ok: true, data: AppState.data[key] };
  } catch (err) {
    AppState.errors[key] = err.message || `Không thể tải ${RESOURCE_ERROR_LABELS[key] || key}`;
    AppState.stale[key] = hasCachedSliceData(AppState.data[key]);
    return { ok: false, error: err };
  } finally {
    AppState.loading[key] = false;
  }
}

async function safeFetchStats() {
  const endpoints = [
    ['tournaments', '/api/v1/tournaments/statistics'],
    ['teams', '/api/v1/teams/statistics'],
    ['athletes', '/api/v1/athletes/statistics'],
    ['matches', '/api/v1/matches/statistics'],
    ['registrations', '/api/v1/registrations/statistics'],
    ['results', '/api/v1/results/statistics']
  ];
  await Promise.all(endpoints.map(async ([k, url]) => {
    try {
      const res = await apiCall(url);
      if (res && res.data && typeof res.data === 'object') {
        AppState.stats[k] = res.data;
      }
    } catch {
      // Keep prior stats slice if one service is temporarily down
    }
  }));
}

async function loadCoreStats() {
  const tasks = [safeFetchStats()];
  if (hasPermission('services.monitor')) {
    tasks.push(safeFetchSlice('gatewayRoutes', '/api/v1/gateway/routes', (d) => d || null));
  } else {
    AppState.data.gatewayRoutes = null;
    AppState.errors.gatewayRoutes = null;
    AppState.stale.gatewayRoutes = false;
  }
  await Promise.all(tasks);
  updateSidebarBadges();
  updateEurekaFooter();
}

function syncSelectedCompetitionFromList() {
  if (AppState.data.competitions.length > 0) {
    if (!AppState.data.competitions.some(c => Number(c.id) === Number(AppState.selectedCompetitionId))) {
      const withItems = AppState.data.rankings.find(rk => Array.isArray(rk.items) && rk.items.length > 0);
      AppState.selectedCompetitionId = withItems?.ranking?.competitionId || AppState.data.competitions[0].id;
    }
    if (!AppState.data.competitions.some(c => Number(c.id) === Number(AppState.homeQuickCompetitionId))) {
      const withItems = AppState.data.rankings.find(rk => Array.isArray(rk.items) && rk.items.length > 0);
      AppState.homeQuickCompetitionId = withItems?.ranking?.competitionId || AppState.data.competitions[0].id;
    }
    if (!AppState.data.competitions.some(c => Number(c.id) === Number(AppState.dashboardChartCompetitionId))) {
      const withItems = AppState.data.rankings.find(rk => Array.isArray(rk.items) && rk.items.length > 0);
      AppState.dashboardChartCompetitionId = withItems?.ranking?.competitionId || AppState.data.competitions[0].id;
    }
    if (!AppState.data.competitions.some(c => Number(c.id) === Number(AppState.reportChartCompetitionId))) {
      const withItems = AppState.data.rankings.find(rk => Array.isArray(rk.items) && rk.items.length > 0);
      AppState.reportChartCompetitionId = withItems?.ranking?.competitionId || AppState.data.competitions[0].id;
    }
  } else {
    AppState.selectedCompetitionId = null;
    AppState.homeQuickCompetitionId = null;
    AppState.dashboardChartCompetitionId = null;
    AppState.reportChartCompetitionId = null;
  }
}

async function fetchResultsByMatchIds(matchIds) {
  const uniqueIds = Array.from(new Set(
    (Array.isArray(matchIds) ? matchIds : [])
      .map(id => Number(id))
      .filter(id => Number.isFinite(id) && id > 0)
  ));
  if (uniqueIds.length === 0) {
    return { ok: true, results: [] };
  }
  const CHUNK_SIZE = 80;
  const chunks = [];
  for (let i = 0; i < uniqueIds.length; i += CHUNK_SIZE) {
    chunks.push(uniqueIds.slice(i, i + CHUNK_SIZE));
  }
  try {
    const responses = await Promise.all(
      chunks.map(chunk => apiCall(`/api/v1/results/by-matches?matchIds=${chunk.join(',')}`))
    );
    const merged = [];
    responses.forEach(r => {
      if (Array.isArray(r?.data)) {
        r.data.forEach(item => {
          if (item && item.matchId != null) {
            AppState.caches.resultsByMatchId[String(item.matchId)] = item;
            merged.push(item);
          }
        });
      }
    });
    return { ok: true, results: merged };
  } catch (err) {
    return { ok: false, error: err, results: [] };
  }
}

async function loadTournamentDetail(tournamentId) {
  const detail = AppState.detail.tournament;
  const reqSeq = ++detail._seq;
  const preliminaryTid = Number(tournamentId || AppState.selectedTournamentId || AppState.data.tournaments[0]?.id || 0);
  const wasSameTournament = Boolean(preliminaryTid && Number(detail.id) === preliminaryTid);
  if (!wasSameTournament) {
    detail.item = null;
    detail.competitions = [];
    detail.matches = [];
    detail.results = [];
    detail.resultsError = null;
    detail.resultsStale = false;
    detail.selectedCompetitionId = 'ALL';
    AppState.paged.registrations.items = [];
    AppState.paged.registrations.totalElements = 0;
    AppState.paged.registrations.totalPages = 0;
  }
  detail.loading = true;
  detail.error = null;
  if (AppState.currentPage === 'tournament-detail') renderCurrentPage(false);

  try {
    if (AppState.data.tournaments.length === 0) {
      await safeFetchSlice('tournaments', '/api/v1/tournaments?page=0&size=50&sortBy=id&direction=asc');
      if (detail._seq !== reqSeq) return;
    }
    let tid = Number(tournamentId || AppState.selectedTournamentId || AppState.data.tournaments[0]?.id || 0);
    if (!tid) {
      detail.id = null;
      detail.item = null;
      detail.competitions = [];
      detail.matches = [];
      detail.results = [];
      detail.resultsError = null;
      detail.resultsStale = false;
      AppState.paged.registrations.items = [];
      AppState.paged.registrations.totalElements = 0;
      AppState.paged.registrations.totalPages = 0;
      return;
    }
    const stillSameTournament = Number(detail.id) === tid;
    AppState.selectedTournamentId = tid;
    detail.id = tid;

    const [tourRes, compsRes, matchesRes] = await Promise.all([
      apiCall(`/api/v1/tournaments/${tid}`),
      safeFetchSlice('competitions', `/api/v1/competitions?tournamentId=${tid}`),
      safeFetchSlice('matches', `/api/v1/matches/tournament/${tid}`)
    ]);
    if (detail._seq !== reqSeq) return;

    detail.item = tourRes.data;
    detail.competitions = compsRes.ok ? compsRes.data : [];
    detail.matches = matchesRes.ok ? matchesRes.data : [];

    // Default to first competition when tournament has competitions so brackets never mix across competitions
    if (detail.competitions.length > 0) {
      const hasSelected = detail.competitions.some(c => String(c.id) === String(detail.selectedCompetitionId));
      if (!hasSelected && detail.selectedCompetitionId !== 'ALL') {
        detail.selectedCompetitionId = String(detail.competitions[0].id);
      } else if (!stillSameTournament) {
        detail.selectedCompetitionId = String(detail.competitions[0].id);
      }
    } else {
      detail.selectedCompetitionId = 'ALL';
    }

    const matchIds = detail.matches.map(m => m.id).filter(Boolean);
    const resultsRes = await fetchResultsByMatchIds(matchIds);
    if (detail._seq !== reqSeq) return;

    if (resultsRes.ok) {
      const allowedMatchSet = new Set(matchIds.map(Number));
      detail.results = resultsRes.results.filter(r => allowedMatchSet.has(Number(r.matchId)));
      detail.resultsError = null;
      detail.resultsStale = false;
      AppState.errors.results = null;
      AppState.stale.results = false;
    } else {
      detail.resultsError = resultsRes.error?.message || RESOURCE_ERROR_LABELS.results;
      AppState.errors.results = detail.resultsError;
      if (stillSameTournament && detail.results.length > 0) {
        detail.resultsStale = true;
        AppState.stale.results = true;
      } else {
        detail.results = [];
        detail.resultsStale = false;
        AppState.stale.results = false;
      }
    }

    AppState.paged.registrations.filters.tournamentId = String(tid);
    AppState.paged.registrations.page = 0;
    await fetchPagedResource('registrations', updateTournamentRegistrationsRegion);
    if (detail._seq !== reqSeq) return;
  } catch (err) {
    if (detail._seq !== reqSeq) return;
    detail.error = err.message || 'Không thể tải thông tin chi tiết giải đấu';
    if (!detail.item || Number(detail.item.id) !== Number(AppState.selectedTournamentId)) {
      detail.item = null;
    }
  } finally {
    if (detail._seq === reqSeq) {
      detail.loading = false;
      if (AppState.currentPage === 'tournament-detail') renderCurrentPage(false);
    }
  }
}

async function loadMatchDetail(matchId) {
  const detail = AppState.detail.match;
  const reqSeq = ++detail._seq;
  let mid = Number(matchId || AppState.selectedMatchId || 0);
  const isSameMatchImmediate = Boolean(mid && Number(detail.id) === mid);

  if (!isSameMatchImmediate) {
    detail.item = null;
    detail.result = null;
    detail.resultError = null;
    detail.resultStale = false;
  }
  detail.loading = true;
  detail.error = null;
  if (AppState.currentPage === 'match-detail') renderCurrentPage(false);

  try {
    if (!mid) {
      const listRes = await apiCall('/api/v1/matches/paged?page=0&size=1&sortBy=id&sortDir=asc').catch(() => null);
      if (detail._seq !== reqSeq) return;
      const firstMatch = unwrapList(listRes?.data)[0];
      if (firstMatch?.id) mid = Number(firstMatch.id);
    }
    if (!mid) {
      detail.id = null;
      detail.item = null;
      detail.result = null;
      detail.resultError = null;
      detail.resultStale = false;
      return;
    }
    const isSameMatch = Number(detail.id) === mid;
    AppState.selectedMatchId = mid;
    detail.id = mid;

    const matchRes = await apiCall(`/api/v1/matches/${mid}`);
    if (detail._seq !== reqSeq) return;
    detail.item = matchRes.data;
    AppState.errors.matches = null;

    try {
      const resResp = await apiCall(`/api/v1/results/match/${mid}`);
      if (detail._seq !== reqSeq) return;
      const fetched = resResp.data?.result || resResp.data || null;
      if (fetched && Number(fetched.matchId) === mid) {
        detail.result = fetched;
        AppState.caches.resultsByMatchId[String(mid)] = fetched;
      } else {
        detail.result = null;
      }
      detail.resultError = null;
      detail.resultStale = false;
      AppState.errors.results = null;
      AppState.stale.results = false;
    } catch (rErr) {
      if (detail._seq !== reqSeq) return;
      if (rErr.status === 404) {
        delete AppState.caches.resultsByMatchId[String(mid)];
        detail.result = null;
        detail.resultError = null;
        detail.resultStale = false;
        AppState.errors.results = null;
        AppState.stale.results = false;
      } else {
        const cachedSameMatch = AppState.caches.resultsByMatchId[String(mid)] || null;
        const canKeepStale = cachedSameMatch && Number(cachedSameMatch.matchId) === mid;
        detail.result = canKeepStale ? cachedSameMatch : null;
        detail.resultStale = Boolean(canKeepStale);
        detail.resultError = rErr.message || RESOURCE_ERROR_LABELS.results;
        AppState.errors.results = detail.resultError;
        AppState.stale.results = detail.resultStale;
      }
    }
  } catch (err) {
    if (detail._seq !== reqSeq) return;
    detail.error = err.message || 'Không thể tải thông tin chi tiết trận đấu';
    AppState.errors.matches = detail.error;
  } finally {
    if (detail._seq === reqSeq) {
      detail.loading = false;
      if (AppState.currentPage === 'match-detail') renderCurrentPage(false);
    }
  }
}

async function loadPageData(pageId) {
  AppState.loading.global = true;
  try {
    if (pageId === 'public-home') {
      await Promise.all([
        loadCoreStats(),
        safeFetchSlice('tournaments', '/api/v1/tournaments?page=0&size=8&sortBy=id&direction=asc'),
        safeFetchSlice('sports', '/api/v1/sports'),
        safeFetchSlice('competitions', '/api/v1/competitions'),
        safeFetchSlice('matches', '/api/v1/matches/paged?page=0&size=8&sortBy=id&sortDir=desc'),
        safeFetchSlice('results', '/api/v1/results/paged?page=0&size=20&sortBy=id&sortDir=desc'),
        safeFetchSlice('rankings', '/api/v1/rankings/details', (d) => Array.isArray(d) ? d : [])
      ]);
      syncSelectedCompetitionFromList();
    } else if (pageId === 'dashboard') {
      const todayIso = getTodayLocalIsoDate();
      const tasks = [
        loadCoreStats(),
        safeFetchSlice('matches', `/api/v1/matches/paged?date=${todayIso}&page=0&size=10&sortBy=matchTime&sortDir=asc`),
        safeFetchSlice('competitions', '/api/v1/competitions'),
        safeFetchSlice('rankings', '/api/v1/rankings/details', (d) => Array.isArray(d) ? d : [])
      ];
      if (hasPermission('registrations.review')) {
        tasks.push(safeFetchSlice('registrations', '/api/v1/registrations/paged?status=PENDING&page=0&size=6'));
      } else {
        AppState.data.registrations = [];
        AppState.errors.registrations = null;
      }
      if (hasPermission('results.approve') || hasPermission('results.submit_assigned')) {
        tasks.push(safeFetchSlice('results', '/api/v1/results/paged?status=PENDING_CONFIRMATION&page=0&size=6'));
      } else {
        AppState.data.results = [];
        AppState.errors.results = null;
      }
      await Promise.all(tasks);
      syncSelectedCompetitionFromList();
    } else if (pageId === 'tournaments') {
      await fetchPagedResource('tournaments', updateTournamentsRegion);
    } else if (pageId === 'tournament-detail') {
      await loadTournamentDetail(AppState.selectedTournamentId);
    } else if (pageId === 'teams') {
      await fetchPagedResource('teams', updateTeamsRegion);
    } else if (pageId === 'athletes') {
      await fetchPagedResource('athletes', updateAthletesRegion);
    } else if (pageId === 'schedule') {
      const tasks = [fetchPagedResource('matches', updateScheduleRegion)];
      if (AppState.data.tournaments.length === 0) {
        tasks.push(safeFetchSlice('tournaments', '/api/v1/tournaments?page=0&size=50&sortBy=id&direction=asc'));
      }
      await Promise.all(tasks);
    } else if (pageId === 'match-detail') {
      await loadMatchDetail(AppState.selectedMatchId);
    } else if (pageId === 'results') {
      await fetchPagedResource('results', updateResultsRegion);
    } else if (pageId === 'rankings') {
      await Promise.all([
        safeFetchSlice('sports', '/api/v1/sports'),
        safeFetchSlice('competitions', '/api/v1/competitions'),
        safeFetchSlice('tournaments', '/api/v1/tournaments?page=0&size=50&sortBy=id&direction=asc')
      ]);
      syncSelectedCompetitionFromList();
      await Promise.all([
        fetchPagedResource('rankings', updateRankingsBoardsRegion),
        fetchPagedResource('rankingItems', updateRankingItemsRegion)
      ]);
    } else if (pageId === 'reports') {
      await Promise.all([
        loadCoreStats(),
        safeFetchSlice('competitions', '/api/v1/competitions'),
        safeFetchSlice('rankings', '/api/v1/rankings/details', (d) => Array.isArray(d) ? d : []),
        fetchPagedResource('notifications', updateNotificationsRegion)
      ]);
      syncSelectedCompetitionFromList();
    } else if (pageId === 'users') {
      await fetchPagedResource('users', updateUsersRegion);
    } else if (pageId === 'settings') {
      await loadCoreStats();
    }
  } finally {
    AppState.loading.global = false;
  }
}

async function refreshAllData() {
  await Promise.all([loadCoreStats(), loadPageData(AppState.currentPage)]);
}

function updateEurekaFooter() {
  const footerEl = document.getElementById('eurekaFooterText');
  if (!footerEl) return;
  if (AppState.errors.gatewayRoutes) {
    footerEl.textContent = 'Eureka Registry: Lỗi kết nối';
    return;
  }
  const discovered = AppState.data.gatewayRoutes?.discoveredServicesInEureka;
  if (discovered && typeof discovered === 'object') {
    const count = Object.keys(discovered).length;
    footerEl.textContent = `Eureka Registry: ${count} Services UP`;
  } else {
    footerEl.textContent = 'Eureka Registry: Đang kiểm tra...';
  }
}

/* ---------- BACKEND PAGINATION ENGINE ---------- */
function buildPagedQueryUrl(resourceKey) {
  const slice = AppState.paged[resourceKey];
  const f = slice.filters || {};
  const params = new URLSearchParams();
  params.set('page', String(slice.page));
  params.set('size', String(slice.size));

  if (resourceKey === 'tournaments') {
    if (f.keyword) params.set('keyword', f.keyword);
    if (f.status) params.set('status', f.status);
    params.set('sortBy', 'id');
    params.set('direction', 'asc');
    return `/api/v1/tournaments?${params.toString()}`;
  }
  if (resourceKey === 'teams') {
    if (f.keyword) params.set('keyword', f.keyword);
    params.set('sortBy', 'id');
    params.set('direction', 'asc');
    return `/api/v1/teams/paged?${params.toString()}`;
  }
  if (resourceKey === 'athletes') {
    if (f.keyword) params.set('keyword', f.keyword);
    if (f.gender) params.set('gender', f.gender);
    if (f.healthStatus) params.set('healthStatus', f.healthStatus);
    params.set('sortBy', 'id');
    params.set('direction', 'asc');
    return `/api/v1/athletes/paged?${params.toString()}`;
  }
  if (resourceKey === 'matches') {
    if (f.tournamentId) params.set('tournamentId', f.tournamentId);
    if (f.status) params.set('status', f.status);
    if (f.keyword) params.set('keyword', f.keyword);
    if (f.date) params.set('date', f.date);
    params.set('sortBy', 'id');
    params.set('sortDir', 'asc');
    return `/api/v1/matches/paged?${params.toString()}`;
  }
  if (resourceKey === 'registrations') {
    if (f.tournamentId) params.set('tournamentId', f.tournamentId);
    if (f.competitionId) params.set('competitionId', f.competitionId);
    if (f.status) params.set('status', f.status);
    if (f.keyword) params.set('keyword', f.keyword);
    return `/api/v1/registrations/paged?${params.toString()}`;
  }
  if (resourceKey === 'results') {
    if (f.status) params.set('status', f.status);
    if (f.winner) params.set('winner', f.winner);
    params.set('sortBy', 'id');
    params.set('sortDir', 'asc');
    return `/api/v1/results/paged?${params.toString()}`;
  }
  if (resourceKey === 'rankings') {
    if (f.keyword) params.set('keyword', f.keyword);
    params.set('sortBy', 'id');
    params.set('sortDir', 'asc');
    return `/api/v1/rankings/paged?${params.toString()}`;
  }
  if (resourceKey === 'rankingItems') {
    const cid = f.competitionId || AppState.selectedCompetitionId || AppState.data.competitions[0]?.id;
    if (!cid) return '';
    if (f.keyword) params.set('keyword', f.keyword);
    params.set('sortBy', 'rankPosition');
    params.set('sortDir', 'asc');
    return `/api/v1/rankings/competition/${cid}/items/paged?${params.toString()}`;
  }
  if (resourceKey === 'users') {
    if (f.keyword) params.set('keyword', f.keyword);
    if (f.role) params.set('role', f.role);
    return `/api/v1/auth/users/paged?${params.toString()}`;
  }
  if (resourceKey === 'notifications') {
    if (f.read !== '') params.set('read', f.read);
    if (f.keyword) params.set('keyword', f.keyword);
    params.set('sortBy', 'createdAt');
    params.set('sortDir', 'desc');
    return `/api/v1/notifications/paged?${params.toString()}`;
  }
  return '';
}

async function fetchPagedResource(resourceKey, partialRenderer = null) {
  const slice = AppState.paged[resourceKey];
  if (!slice) return;
  const url = buildPagedQueryUrl(resourceKey);
  if (!url) {
    slice.items = [];
    slice.totalElements = 0;
    slice.totalPages = 0;
    slice.page = 0;
    slice.error = null;
    slice.stale = false;
    slice.loaded = true;
    if (partialRenderer) partialRenderer();
    return;
  }

  slice.loading = true;
  if (partialRenderer) partialRenderer();

  try {
    const res = await apiCall(url);
    const pageObj = res.data || {};
    const totalPages = Number(pageObj.totalPages ?? 0);
    const totalElements = Number(pageObj.totalElements ?? 0);
    const currentPage = Number(pageObj.page ?? pageObj.number ?? slice.page);

    if (totalPages === 0 && slice.page !== 0) {
      slice.page = 0;
      return fetchPagedResource(resourceKey, partialRenderer);
    }
    if (totalPages > 0 && currentPage >= totalPages) {
      slice.page = totalPages - 1;
      return fetchPagedResource(resourceKey, partialRenderer);
    }

    slice.items = Array.isArray(pageObj.content) ? pageObj.content : [];
    slice.page = totalPages === 0 ? 0 : currentPage;
    slice.size = Number(pageObj.size || slice.size);
    slice.totalElements = totalElements;
    slice.totalPages = totalPages;
    slice.error = null;
    slice.stale = false;
    slice.loaded = true;
    if (resourceKey === 'rankings') {
      AppState.errors.rankings = null;
      AppState.stale.rankings = false;
    } else if (resourceKey === 'rankingItems') {
      AppState.errors.rankingItems = null;
      AppState.stale.rankingItems = false;
    }
  } catch (err) {
    slice.error = err.message || `Không thể tải ${RESOURCE_ERROR_LABELS[resourceKey] || resourceKey}`;
    slice.stale = slice.items.length > 0;
    if (resourceKey === 'rankings') {
      AppState.errors.rankings = slice.error;
      AppState.stale.rankings = slice.stale;
    } else if (resourceKey === 'rankingItems') {
      AppState.errors.rankingItems = slice.error;
      AppState.stale.rankingItems = slice.stale;
    }
  } finally {
    slice.loading = false;
    if (partialRenderer) {
      partialRenderer();
    } else if (shouldAutoRenderPaged(resourceKey)) {
      renderCurrentPage(false);
    }
  }
}

function shouldAutoRenderPaged(resourceKey) {
  const pageMap = {
    tournaments: 'tournaments',
    teams: 'teams',
    athletes: 'athletes',
    matches: 'schedule',
    registrations: 'tournament-detail',
    results: 'results',
    rankings: 'rankings',
    rankingItems: 'rankings',
    users: 'users',
    notifications: 'reports'
  };
  return AppState.currentPage === pageMap[resourceKey];
}

function changePageNumber(resourceKey, nextPage) {
  const slice = AppState.paged[resourceKey];
  if (!slice) return;
  const maxPage = Math.max(0, slice.totalPages - 1);
  slice.page = Math.max(0, Math.min(nextPage, maxPage));
  fetchPagedResource(resourceKey, getPartialRendererForKey(resourceKey));
}

function changePageSize(resourceKey, newSize) {
  const slice = AppState.paged[resourceKey];
  if (!slice) return;
  slice.size = Number(newSize) || 10;
  slice.page = 0;
  fetchPagedResource(resourceKey, getPartialRendererForKey(resourceKey));
}

function getPartialRendererForKey(resourceKey) {
  const partialMap = {
    tournaments: updateTournamentsRegion,
    teams: updateTeamsRegion,
    athletes: updateAthletesRegion,
    matches: updateScheduleRegion,
    registrations: updateTournamentRegistrationsRegion,
    results: updateResultsRegion,
    rankings: updateRankingsBoardsRegion,
    rankingItems: updateRankingItemsRegion,
    users: updateUsersRegion,
    notifications: updateNotificationsRegion
  };
  return partialMap[resourceKey] || null;
}

let pagedFilterTimer = null;
function applyPagedFilter(resourceKey, patch, debounce = false) {
  const slice = AppState.paged[resourceKey];
  if (!slice) return;
  Object.assign(slice.filters, patch);
  slice.page = 0;

  const partialFn = getPartialRendererForKey(resourceKey);

  if (debounce) {
    clearTimeout(pagedFilterTimer);
    pagedFilterTimer = setTimeout(() => fetchPagedResource(resourceKey, partialFn), 240);
  } else {
    fetchPagedResource(resourceKey, partialFn);
  }
}

function renderPaginationBar(resourceKey, slice, itemLabel, pageSizes = [5, 10, 20, 50]) {
  const currentCount = slice.items.length;
  const totalElements = slice.totalElements;
  const totalPages = slice.totalPages;
  const currentPageDisplay = totalPages === 0 ? 0 : slice.page + 1;

  const pagesToShow = [];
  const start = Math.max(0, slice.page - 2);
  const end = Math.min(totalPages - 1, slice.page + 2);
  for (let p = start; p <= end; p++) pagesToShow.push(p);

  return `
    <div class="pagination-bar">
      <div>
        Đang hiển thị <strong>${currentCount}</strong> bản ghi trên trang này / Tổng cộng <strong>${totalElements}</strong> ${escapeHtml(itemLabel)}
        <span class="tabular-nums">(Trang ${currentPageDisplay}/${totalPages})</span>
      </div>
      <div class="pagination-controls">
        <label style="font-size:0.8rem;display:inline-flex;align-items:center;gap:6px;margin-right:6px;">
          Số dòng:
          <select class="select" style="padding:4px 8px;font-size:0.8rem;" onchange="changePageSize('${resourceKey}', this.value)" aria-label="Số dòng mỗi trang">
            ${pageSizes.map(sz => `<option value="${sz}" ${slice.size === sz ? 'selected' : ''}>${sz}</option>`).join('')}
          </select>
        </label>
        <button type="button" class="btn btn-secondary btn-sm" ${slice.page <= 0 || slice.loading ? 'disabled' : ''} onclick="changePageNumber('${resourceKey}', ${slice.page - 1})">‹ Trước</button>
        ${pagesToShow.map(p => `
          <button type="button" class="btn ${p === slice.page ? 'btn-primary' : 'btn-secondary'} btn-sm tabular-nums" ${slice.loading ? 'disabled' : ''} onclick="changePageNumber('${resourceKey}', ${p})">${p + 1}</button>
        `).join('')}
        <button type="button" class="btn btn-secondary btn-sm" ${slice.page + 1 >= totalPages || slice.loading ? 'disabled' : ''} onclick="changePageNumber('${resourceKey}', ${slice.page + 1})">Sau ›</button>
      </div>
    </div>
  `;
}

function renderServiceStateBanner(keys, retryAction = "retryFailedServices()") {
  const keyList = Array.isArray(keys) ? keys : [keys];
  const loadingKeys = keyList.filter(k => AppState.loading[k] || AppState.paged[k]?.loading);
  const errorEntries = keyList
    .map(k => ({
      key: k,
      msg: AppState.errors[k] || AppState.paged[k]?.error,
      stale: Boolean(AppState.stale[k] || AppState.paged[k]?.stale)
    }))
    .filter(e => Boolean(e.msg));

  let html = '';
  if (AppState.loading.global || loadingKeys.length > 0) {
    html += `
      <div class="state-banner loading" role="status">
        <span>Đang đồng bộ dữ liệu từ dịch vụ hệ thống...</span>
      </div>
    `;
  }
  if (errorEntries.length > 0) {
    const anyStale = errorEntries.some(e => e.stale);
    html += `
      <div class="state-banner error" role="alert">
        <div>
          <strong>Một số dữ liệu chưa thể tải:</strong>
          ${errorEntries.map(e => `<span>${escapeHtml(RESOURCE_ERROR_LABELS[e.key] || e.key)} (${escapeHtml(e.msg)})</span>`).join(' • ')}
          ${anyStale ? `<div style="margin-top:4px;font-size:0.82rem;opacity:0.95;">Dữ liệu hiển thị bên dưới được giữ từ lần tải trước và có thể chưa được cập nhật mới nhất.</div>` : ''}
        </div>
        <button type="button" class="btn btn-secondary btn-sm" onclick="${retryAction}">↻ Thử lại</button>
      </div>
    `;
  }
  return html;
}

async function retryFailedServices() {
  await Promise.all([loadCoreStats(), loadPageData(AppState.currentPage)]);
  renderCurrentPage(false);
}

/* ---------- SIDEBAR & HEADER UI ---------- */
function renderSidebar() {
  const container = document.getElementById('sidebarNav');
  if (!container) return;

  let html = '';
  for (const group of MENU_DEFINITIONS) {
    const allowedItems = group.items.filter(item => hasPermission(item.permission));
    if (allowedItems.length === 0) continue;

    html += `
      <div class="nav-group" style="display:flex;flex-direction:column;gap:4px;">
        <div class="nav-group-label">${escapeHtml(group.groupTitle)}</div>
        <ul class="nav-list">
          ${allowedItems.map(item => `
            <li>
              <button class="nav-item-btn ${AppState.currentPage === item.id ? 'active' : ''}" data-page="${item.id}" onclick="navigateTo('${item.id}')">
                ${item.iconSvg}
                <span>${escapeHtml(item.title)}</span>
                ${item.badgeId ? `<span class="nav-badge" id="${item.badgeId}">0</span>` : ''}
              </button>
            </li>
          `).join('')}
        </ul>
      </div>
    `;
  }
  container.innerHTML = html;

  const footer = document.getElementById('sidebarFooter');
  if (footer) {
    if (hasPermission('services.monitor')) {
      footer.style.display = '';
      footer.innerHTML = `
        <div class="soa-live-pill">
          <span class="pulse-dot" aria-hidden="true"></span>
          <span id="eurekaFooterText">Eureka Registry: Đang kiểm tra...</span>
        </div>
      `;
      updateEurekaFooter();
    } else {
      footer.style.display = 'none';
      footer.innerHTML = '';
    }
  }

  updateSidebarBadges();
}

function updateActiveNavState() {
  document.querySelectorAll('.nav-item-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.page === AppState.currentPage);
  });
}

function updateSidebarBadges() {
  const bTour = document.getElementById('badgeTournaments');
  const bTeams = document.getElementById('badgeTeams');
  const bAths = document.getElementById('badgeAthletes');
  const bMatches = document.getElementById('badgeMatches');

  const tourTotal = AppState.stats.tournaments?.total ?? AppState.data.tournaments.length;
  const teamTotal = AppState.stats.teams?.totalTeams ?? AppState.data.teams.length;
  const athTotal = AppState.stats.athletes?.total ?? AppState.data.athletes.length;
  const matchTotal = AppState.stats.matches?.totalMatches ?? AppState.data.matches.length;

  if (bTour) bTour.textContent = tourTotal;
  if (bTeams) bTeams.textContent = teamTotal;
  if (bAths) bAths.textContent = athTotal;
  if (bMatches) bMatches.textContent = matchTotal;
}

function updateHeaderUserUI() {
  const avatar = document.getElementById('headerUserAvatar');
  const label = document.getElementById('headerUserLabel');
  if (AppState.user && AppState.token) {
    avatar.textContent = (AppState.user.username || 'U').substring(0, 2).toUpperCase();
    label.textContent = `${AppState.user.username} (${getRoleDisplayName(AppState.user.role)})`;
  } else {
    avatar.textContent = 'KH';
    label.textContent = 'Khách vãng lai';
  }
}

function toggleMobileSidebar(forceOpen = null) {
  const sidebar = document.getElementById('appSidebar');
  const scrim = document.getElementById('sidebarScrim');
  const btn = document.getElementById('mobileMenuBtn');
  if (!sidebar || !scrim) return;

  const isOpen = forceOpen !== null ? forceOpen : !sidebar.classList.contains('open');
  sidebar.classList.toggle('open', isOpen);
  scrim.hidden = !isOpen;
  if (btn) btn.setAttribute('aria-expanded', String(isOpen));
}

/* ---------- NAVIGATION & ROUTE GUARD ---------- */
function navigateTo(pageId) {
  const requiredPerm = PAGE_PERMISSIONS[pageId];
  toggleMobileSidebar(false);
  closeGlobalSearchDropdown();

  if (requiredPerm && !hasPermission(requiredPerm)) {
    if (!AppState.token) {
      showToast('Vui lòng đăng nhập để truy cập trang này', 'warning');
      navigateTo('auth-page');
      return;
    }
    AppState.currentPage = pageId;
    updateActiveNavState();
    document.getElementById('breadcrumbCurrent').textContent = '403 — Không có quyền truy cập';
    renderForbiddenPage(pageId, requiredPerm);
    return;
  }

  AppState.currentPage = pageId;
  updateActiveNavState();
  document.getElementById('breadcrumbCurrent').textContent = PAGE_TITLES[pageId] || pageId;
  renderCurrentPage(true);

  if (pageId !== 'auth-page') {
    loadPageData(pageId).then(() => {
      if (AppState.currentPage === pageId) {
        updateSidebarBadges();
        if (!['tournaments', 'teams', 'athletes', 'schedule', 'results', 'users'].includes(pageId)) {
          renderCurrentPage(false);
        }
      }
    });
  }
}

function renderForbiddenPage(pageId, requiredPerm) {
  const container = document.getElementById('mainViewContainer');
  if (!container) return;
  const roleName = AppState.user ? getRoleDisplayName(AppState.user.role) : 'Khách';

  container.innerHTML = `
    <div class="card" style="text-align:center;padding:48px 24px;max-width:640px;margin:32px auto;box-shadow:var(--shadow-md);">
      <div style="font-size:3rem;margin-bottom:12px;line-height:1;">🚫</div>
      <h1 class="page-title" style="margin-bottom:10px;color:var(--color-error)">403 — Không có quyền truy cập</h1>
      <p class="page-subtitle" style="margin-bottom:18px;">
        Tài khoản hiện tại có vai trò <strong>${escapeHtml(roleName)}</strong>, không đủ quyền mở trang này.
      </p>
      <div style="background:var(--bg-surface-secondary);border:1px solid var(--border-color);border-radius:var(--radius-sm);padding:14px;margin-bottom:22px;font-size:0.84rem;text-align:left;">
        <div><strong>Trang yêu cầu:</strong> ${escapeHtml(PAGE_TITLES[pageId] || pageId)}</div>
        <div style="margin-top:4px;"><strong>Quyền cần có:</strong> <code>${escapeHtml(getPermissionDisplayName(requiredPerm))}</code></div>
      </div>
      <div class="page-actions" style="justify-content:center;">
        <button class="btn btn-primary" onclick="navigateTo('public-home')">← Quay về Trang chủ</button>
        <button class="btn btn-secondary" onclick="navigateTo('auth-page')">Đổi tài khoản</button>
      </div>
    </div>
  `;
}

function animatePageEntrance() {
  if (!window.gsap || isReducedMotion()) return;
  gsap.fromTo(
    '#mainViewContainer .card, #mainViewContainer .kpi-card, #mainViewContainer .hero-banner',
    { y: 10, autoAlpha: 0 },
    { y: 0, autoAlpha: 1, duration: 0.24, stagger: 0.03, ease: 'power2.out', clearProps: 'transform,opacity,visibility' }
  );
}

/* ---------- GLOBAL SEARCH (PERMISSION-SCOPED & NON-BLOCKING) ---------- */
let globalSearchDebounce = null;

function closeGlobalSearchDropdown() {
  const dd = document.getElementById('globalSearchDropdown');
  const inp = document.getElementById('globalSearchInput');
  if (dd) dd.hidden = true;
  if (inp) inp.setAttribute('aria-expanded', 'false');
  AppState.globalSearchActiveIdx = -1;
}

function handleGlobalSearchInput(value) {
  const q = String(value || '').trim();
  clearTimeout(globalSearchDebounce);
  if (!q) {
    closeGlobalSearchDropdown();
    return;
  }
  const dd = document.getElementById('globalSearchDropdown');
  const inp = document.getElementById('globalSearchInput');
  if (dd && inp) {
    dd.hidden = false;
    inp.setAttribute('aria-expanded', 'true');
    dd.innerHTML = `<div style="padding:12px;font-size:0.84rem;color:var(--text-secondary);">Đang tìm kiếm "${escapeHtml(q)}"...</div>`;
  }
  globalSearchDebounce = setTimeout(() => executeGlobalSearch(q), 220);
}

async function executeGlobalSearch(query) {
  const seq = ++AppState.globalSearchSeq;
  const encoded = encodeURIComponent(query);

  const tasks = [];
  if (hasPermission('tournaments.read_public')) {
    tasks.push(
      apiCall(`/api/v1/tournaments?keyword=${encoded}&page=0&size=5`)
        .then(r => ({ key: 'tournaments', items: unwrapList(r.data) }))
        .catch(err => ({ key: 'tournaments', error: err.message, items: [] }))
    );
  }
  if (hasPermission('teams.read')) {
    tasks.push(
      apiCall(`/api/v1/teams/paged?keyword=${encoded}&page=0&size=5`)
        .then(r => ({ key: 'teams', items: unwrapList(r.data) }))
        .catch(err => ({ key: 'teams', error: err.message, items: [] }))
    );
  }
  if (hasPermission('athletes.manage_own')) {
    tasks.push(
      apiCall(`/api/v1/athletes/paged?keyword=${encoded}&page=0&size=5`)
        .then(r => ({ key: 'athletes', items: unwrapList(r.data) }))
        .catch(err => ({ key: 'athletes', error: err.message, items: [] }))
    );
  }

  const results = await Promise.all(tasks);
  if (seq !== AppState.globalSearchSeq) return; // Discard stale response

  renderGlobalSearchDropdown(query, results);
}

function renderGlobalSearchDropdown(query, groups) {
  const dd = document.getElementById('globalSearchDropdown');
  const inp = document.getElementById('globalSearchInput');
  if (!dd || !inp) return;

  const flatOptions = [];
  let html = '';

  const labels = {
    tournaments: 'Giải đấu',
    teams: 'Đội tuyển',
    athletes: 'Vận động viên'
  };

  for (const g of groups) {
    if (g.error) {
      html += `<div class="search-group-header">${labels[g.key]}</div>`;
      html += `<div style="padding:6px 10px;font-size:0.8rem;color:var(--color-error);">Lỗi tìm kiếm: ${escapeHtml(g.error)}</div>`;
      continue;
    }
    if (!g.items || g.items.length === 0) continue;

    html += `<div class="search-group-header">${labels[g.key]} (${g.items.length})</div>`;
    g.items.forEach(item => {
      const idx = flatOptions.length;
      flatOptions.push({ type: g.key, item });
      if (g.key === 'tournaments') {
        html += `
          <button type="button" class="search-result-item" role="option" data-search-idx="${idx}" onclick="selectGlobalSearchItem('tournaments', ${item.id})">
            <div>
              <div><strong>${escapeHtml(item.name)}</strong></div>
              <div class="search-result-meta">${escapeHtml(item.location || 'Chưa cập nhật địa điểm')}</div>
            </div>
            ${statusBadgeHtml(item.status)}
          </button>
        `;
      } else if (g.key === 'teams') {
        html += `
          <button type="button" class="search-result-item" role="option" data-search-idx="${idx}" data-label="${escapeHtml(item.name)}" onclick="selectGlobalSearchItem('teams', ${item.id}, this.dataset.label)">
            <div>
              <div><strong>${escapeHtml(item.name)}</strong></div>
              <div class="search-result-meta">HLV: ${escapeHtml(item.managerName || 'Chưa cập nhật')}</div>
            </div>
            <span class="badge badge-primary">${escapeHtml(item.shortName || formatTeamCrestInitials(item.name))}</span>
          </button>
        `;
      } else if (g.key === 'athletes') {
        html += `
          <button type="button" class="search-result-item" role="option" data-search-idx="${idx}" data-label="${escapeHtml(item.fullName)}" onclick="selectGlobalSearchItem('athletes', ${item.id}, this.dataset.label)">
            <div>
              <div><strong>${escapeHtml(item.fullName)}</strong></div>
              <div class="search-result-meta">${escapeHtml(formatGender(item.gender))} • ${escapeHtml(item.dateOfBirth || '')}</div>
            </div>
            ${statusBadgeHtml(item.healthStatus || 'QUALIFIED')}
          </button>
        `;
      }
    });
  }

  if (flatOptions.length === 0 && !groups.some(g => g.error)) {
    html = `<div style="padding:14px;font-size:0.84rem;color:var(--text-secondary);text-align:center;">Không tìm thấy kết quả phù hợp với "${escapeHtml(query)}"</div>`;
  }

  AppState.globalSearchResults = flatOptions;
  AppState.globalSearchActiveIdx = -1;
  dd.innerHTML = html;
  dd.hidden = false;
  inp.setAttribute('aria-expanded', 'true');
}

function selectGlobalSearchItem(type, id, name = '') {
  closeGlobalSearchDropdown();
  if (type === 'tournaments') {
    AppState.selectedTournamentId = Number(id);
    AppState.tournamentDetailTab = 'bracket';
    navigateTo('tournament-detail');
  } else if (type === 'teams') {
    AppState.paged.teams.filters.keyword = name;
    AppState.paged.teams.page = 0;
    navigateTo('teams');
    if (id && name) {
      setTimeout(() => viewTeamMembersModal(id, name), 80);
    }
  } else if (type === 'athletes') {
    AppState.paged.athletes.filters.keyword = name;
    AppState.paged.athletes.page = 0;
    navigateTo('athletes');
  }
}

/* ---------- RENDER ROUTER ---------- */
function renderCurrentPage(animate = true) {
  const container = document.getElementById('mainViewContainer');
  if (!container) return;

  switch (AppState.currentPage) {
    case 'public-home': container.innerHTML = renderPublicHomePage(); break;
    case 'auth-page': container.innerHTML = renderAuthPage(); break;
    case 'dashboard':
      container.innerHTML = renderDashboardPage();
      setTimeout(() => initDashboardCharts(AppState.dashboardChartCompetitionId), 30);
      break;
    case 'tournaments': container.innerHTML = renderTournamentsPage(); break;
    case 'tournament-detail': container.innerHTML = renderTournamentDetailPage(); break;
    case 'teams': container.innerHTML = renderTeamsPage(); break;
    case 'athletes': container.innerHTML = renderAthletesPage(); break;
    case 'schedule': container.innerHTML = renderSchedulePage(); break;
    case 'match-detail': container.innerHTML = renderMatchDetailPage(); break;
    case 'results': container.innerHTML = renderResultsPage(); break;
    case 'rankings': container.innerHTML = renderRankingsPage(); break;
    case 'reports':
      container.innerHTML = renderReportsPage();
      setTimeout(() => initReportCharts(AppState.reportChartCompetitionId), 30);
      break;
    case 'users': container.innerHTML = renderUsersPage(); break;
    case 'settings': container.innerHTML = renderSettingsPage(); break;
    default: container.innerHTML = renderPublicHomePage();
  }

  if (animate) animatePageEntrance();
}

/* ---------- HELPER TO LINK RANKINGS & COMPETITIONS DYNAMICALLY ---------- */
function getCompetitionMeta(competitionId) {
  const cid = Number(competitionId);
  const comp = AppState.data.competitions.find(c => Number(c.id) === cid) || null;
  const tour = comp ? AppState.data.tournaments.find(t => Number(t.id) === Number(comp.tournamentId)) : null;
  const sport = comp ? AppState.data.sports.find(s => Number(s.id) === Number(comp.sportId)) : null;
  return { comp, tour, sport };
}

function getDynamicRankingsGroups(sportFilter = 'ALL', keyword = '') {
  const kw = String(keyword || '').trim().toLowerCase();
  const sf = String(sportFilter || 'ALL');
  const d = AppState.data;

  const rankingByCompId = new Map();
  (d.rankings || []).forEach(entry => {
    if (entry?.ranking?.competitionId) {
      rankingByCompId.set(Number(entry.ranking.competitionId), entry);
    }
  });

  // Start from existing competitions; also include any standalone rankings whose competition wasn't listed
  const allCompIds = new Set(d.competitions.map(c => Number(c.id)));
  rankingByCompId.forEach((_, cid) => allCompIds.add(cid));

  const groups = [];
  allCompIds.forEach(cid => {
    const { comp, tour, sport } = getCompetitionMeta(cid);
    const rkEntry = rankingByCompId.get(cid);

    // Filter by selected sport ID if sport filter is active
    if (sf !== 'ALL') {
      if (!comp || String(comp.sportId) !== sf) return;
    }

    const rawItems = Array.isArray(rkEntry?.items) ? rkEntry.items : [];
    const filteredItems = kw
      ? rawItems.filter(it => String(it.participantName || '').toLowerCase().includes(kw))
      : rawItems;

    // If keyword is active and nothing matched this table, omit or show empty? Keep if items > 0 or if no keyword!
    if (kw && filteredItems.length === 0) return;

    groups.push({
      competitionId: cid,
      competitionName: comp?.name || comp?.competitionName || rkEntry?.ranking?.rankingName || `Nội dung #${cid}`,
      tournamentId: tour?.id || null,
      tournamentName: tour?.name || 'Giải đấu',
      sportId: sport?.id || comp?.sportId || null,
      sportName: sport?.name || 'Môn thi đấu',
      sportType: sport?.sportType || 'TEAM',
      updatedAt: rkEntry?.ranking?.updatedAt || null,
      items: filteredItems
    });
  });

  return groups;
}

/* ---------- PAGE: TRANG CHỦ CÔNG KHAI ---------- */
function renderPublicHomePage() {
  const d = AppState.data;
  const canAdmin = hasPermission('reports.read_own_tournaments');
  const tourTotal = AppState.stats.tournaments?.total ?? d.tournaments.length;
  const ongoingCount = AppState.stats.tournaments?.ongoing ?? d.tournaments.filter(t => t.status === 'ONGOING').length;
  const teamTotal = AppState.stats.teams?.totalTeams ?? d.teams.length;
  const athTotal = AppState.stats.athletes?.total ?? d.athletes.length;
  const matchTotal = AppState.stats.matches?.totalMatches ?? d.matches.length;
  const confirmedResTotal = AppState.stats.results?.confirmedResults ?? d.results.filter(r => r.status === 'CONFIRMED').length;

  // Featured match: prefer ONGOING, then latest FINISHED with result, else first real match
  const featuredMatch =
    d.matches.find(m => m.status === 'ONGOING') ||
    d.matches.find(m => m.status === 'FINISHED' && d.results.some(r => Number(r.matchId) === Number(m.id))) ||
    d.matches[0] ||
    null;

  const featuredResult = featuredMatch
    ? d.results.find(r => Number(r.matchId) === Number(featuredMatch.id)) || null
    : null;

  // Quick rankings strictly from real backend data
  const quickCid = AppState.homeQuickCompetitionId || d.competitions[0]?.id || null;
  const quickRankEntry = quickCid
    ? (d.rankings || []).find(rk => Number(rk?.ranking?.competitionId) === Number(quickCid))
    : null;
  const quickItems = Array.isArray(quickRankEntry?.items) ? quickRankEntry.items.slice(0, 8) : [];

  return `
    ${renderServiceStateBanner(['tournaments', 'matches', 'results', 'rankings'])}

    <section class="hero-banner">
      <div>
        <div class="hero-eyebrow">
          <span class="pulse-dot"></span>
          <span>CỔNG THÔNG TIN THI ĐẤU THỂ THAO</span>
        </div>
        <h1 class="hero-headline">Quản lý Giải đấu, Lịch thi đấu & Bảng xếp hạng</h1>
        <p class="hero-desc">
          Theo dõi giải đấu đang diễn ra, lịch thi đấu, kết quả được xác nhận và bảng xếp hạng trực tiếp từ hệ thống dịch vụ.
        </p>
        <div class="page-actions">
          ${canAdmin
            ? `<button class="btn btn-primary" onclick="navigateTo('dashboard')">Mở Tổng quan</button>`
            : `<button class="btn btn-primary" onclick="navigateTo('auth-page')">Đăng nhập / Đăng ký</button>`}
          <button class="btn btn-secondary" onclick="navigateTo('rankings')">Bảng xếp hạng</button>
          <button class="btn btn-secondary" onclick="navigateTo('schedule')">Lịch thi đấu</button>
        </div>
      </div>

      ${featuredMatch ? `
        <div class="live-match-card" style="cursor:pointer" onclick="viewMatchDetails(${featuredMatch.id})">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;gap:8px;">
            <span class="badge badge-primary">${escapeHtml(featuredMatch.round || 'Trận tiêu điểm')}</span>
            ${statusBadgeHtml(featuredMatch.status)}
          </div>
          <div class="scoreboard-row">
            <div class="team-side">
              <span class="team-crest-circle" title="${escapeHtml(featuredMatch.participant1 || '')}">${escapeHtml(formatTeamCrestInitials(featuredMatch.participant1))}</span>
              <strong>${escapeHtml(featuredMatch.participant1 || 'Chưa xác định')}</strong>
            </div>
            <div class="score-pill tabular-nums">
              ${featuredResult ? `${featuredResult.scoreParticipant1 ?? '-'} : ${featuredResult.scoreParticipant2 ?? '-'}` : '- : -'}
            </div>
            <div class="team-side right">
              <span class="team-crest-circle" title="${escapeHtml(featuredMatch.participant2 || '')}">${escapeHtml(formatTeamCrestInitials(featuredMatch.participant2))}</span>
              <strong>${escapeHtml(featuredMatch.participant2 || 'Chưa xác định')}</strong>
            </div>
          </div>
          <div style="font-size:0.8rem;color:var(--text-secondary);display:flex;justify-content:space-between;gap:8px;flex-wrap:wrap;border-top:1px solid var(--border-color);padding-top:10px;">
            <span>Sân: ${escapeHtml(featuredMatch.location || 'Chưa cập nhật')}</span>
            <span>Trọng tài: ${escapeHtml(featuredMatch.referee || 'Chưa phân công')}</span>
          </div>
        </div>
      ` : `
        <div class="live-match-card state-empty-box">
          <strong>Chưa có trận đấu tiêu điểm</strong>
          <div style="margin-top:6px;">Dữ liệu trận đấu sẽ xuất hiện tại đây khi ban tổ chức tạo lịch thi đấu.</div>
        </div>
      `}
    </section>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-top"><span>Giải đấu</span><span class="badge badge-primary">${ongoingCount} đang diễn ra</span></div>
        <div class="kpi-value">${tourTotal}</div>
        <div class="kpi-meta">Tổng giải đấu trong hệ thống</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-top"><span>Đội tuyển</span><span class="badge badge-primary">Đã đăng ký</span></div>
        <div class="kpi-value">${teamTotal}</div>
        <div class="kpi-meta">Câu lạc bộ & đơn vị tham dự</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-top"><span>Vận động viên</span><span class="badge badge-primary">Hồ sơ</span></div>
        <div class="kpi-value">${athTotal}</div>
        <div class="kpi-meta">Hồ sơ vận động viên toàn hệ thống</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-top"><span>Trận đấu</span><span class="badge badge-success">${confirmedResTotal} KQ xác nhận</span></div>
        <div class="kpi-value">${matchTotal}</div>
        <div class="kpi-meta">Tổng số trận trong lịch thi đấu</div>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">Giải đấu Công khai</h2>
          ${hasPermission('tournaments.create') ? `<button class="btn btn-secondary btn-sm" onclick="navigateTo('tournaments')">Quản lý</button>` : ''}
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr><th>STT</th><th>Tên giải đấu</th><th>Địa điểm</th><th>Khởi tranh</th><th>Trạng thái</th></tr>
            </thead>
            <tbody>
              ${d.tournaments.slice(0, 8).map((t, idx) => `
                <tr style="cursor:pointer" onclick="AppState.selectedTournamentId=${t.id};navigateTo('tournament-detail')">
                  <td class="tabular-nums"><strong>#${String(idx + 1).padStart(2, '0')}</strong></td>
                  <td><strong>${escapeHtml(t.name)}</strong></td>
                  <td>${escapeHtml(t.location || 'Chưa cập nhật')}</td>
                  <td class="tabular-nums">${escapeHtml(t.startDate || '—')}</td>
                  <td>${statusBadgeHtml(t.status)}</td>
                </tr>
              `).join('') || '<tr><td colspan="5" class="state-empty-box">Chưa có giải đấu nào.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="card-header" style="flex-wrap:wrap;gap:10px;">
          <h2 class="card-title">Bảng xếp hạng theo Nội dung</h2>
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
            ${d.competitions.length > 0 ? `
              <select class="select" style="padding:4px 10px;font-size:0.8rem;" onchange="AppState.homeQuickCompetitionId=Number(this.value);renderCurrentPage(false);" aria-label="Chọn nội dung xem bảng xếp hạng">
                ${d.competitions.map(c => `<option value="${c.id}" ${Number(c.id) === Number(quickCid) ? 'selected' : ''}>${escapeHtml(c.name || c.competitionName)}</option>`).join('')}
              </select>
            ` : ''}
            <button class="btn btn-secondary btn-sm" onclick="navigateTo('rankings')">Xem tất cả →</button>
          </div>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr><th>Hạng</th><th>Đội tuyển / VĐV</th><th>Trận</th><th>T / H / B</th><th>Hiệu số</th><th>Điểm</th></tr>
            </thead>
            <tbody>
              ${quickItems.map((item, idx) => `
                <tr>
                  <td class="tabular-nums"><span class="badge ${idx === 0 ? 'badge-success' : 'badge-neutral'}">#${item.rankPosition ?? idx + 1}</span></td>
                  <td><strong>${escapeHtml(item.participantName)}</strong></td>
                  <td class="tabular-nums">${item.matchesPlayed ?? 0}</td>
                  <td class="tabular-nums">${item.won ?? 0}/${item.drawn ?? 0}/${item.lost ?? 0}</td>
                  <td class="tabular-nums">${item.goalDifference > 0 ? '+' + item.goalDifference : (item.goalDifference ?? 0)}</td>
                  <td class="tabular-nums"><strong style="color:var(--color-primary)">${item.points ?? 0}</strong></td>
                </tr>
              `).join('') || '<tr><td colspan="6" class="state-empty-box">Chưa có dữ liệu bảng xếp hạng cho nội dung này.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="grid-2" style="margin-top:24px;">
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">Danh mục Môn thể thao (${d.sports.length})</h2>
          ${hasPermission('tournaments.create') ? `<button class="btn btn-secondary btn-sm" onclick="openCreateSportModal()">+ Thêm môn</button>` : ''}
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr><th>STT</th><th>Tên môn</th><th>Phân loại</th><th>Mô tả</th></tr>
            </thead>
            <tbody>
              ${d.sports.map((s, idx) => `
                <tr>
                  <td class="tabular-nums"><strong>#${String(idx + 1).padStart(2, '0')}</strong></td>
                  <td><strong>${escapeHtml(s.name)}</strong></td>
                  <td><span class="badge ${s.sportType === 'INDIVIDUAL' ? 'badge-warning' : 'badge-primary'}">${escapeHtml(formatSportType(s.sportType))}</span></td>
                  <td>${escapeHtml(s.description || '—')}</td>
                </tr>
              `).join('') || '<tr><td colspan="4" class="state-empty-box">Chưa có môn thể thao nào.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h2 class="card-title">Lịch thi đấu gần đây</h2>
          <button class="btn btn-secondary btn-sm" onclick="navigateTo('schedule')">Xem tất cả (${d.matches.length})</button>
        </div>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr><th>Vòng đấu</th><th>Cặp đấu</th><th>Tỷ số</th><th>Trạng thái</th></tr>
            </thead>
            <tbody>
              ${d.matches.slice(0, 8).map(m => {
                const res = d.results.find(r => Number(r.matchId) === Number(m.id));
                const scoreText = res ? `${res.scoreParticipant1} - ${res.scoreParticipant2}` : '- : -';
                return `
                  <tr style="cursor:pointer" onclick="viewMatchDetails(${m.id})">
                    <td><span class="badge badge-neutral">${escapeHtml(m.round || 'Vòng đấu')}</span></td>
                    <td><strong>${escapeHtml(m.participant1 || '—')}</strong> vs <strong>${escapeHtml(m.participant2 || '—')}</strong></td>
                    <td class="tabular-nums"><strong style="color:var(--color-primary)">${escapeHtml(scoreText)}</strong></td>
                    <td>${statusBadgeHtml(m.status)}</td>
                  </tr>
                `;
              }).join('') || '<tr><td colspan="4" class="state-empty-box">Chưa có trận đấu nào.</td></tr>'}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;
}

/* ---------- PAGE: ĐĂNG NHẬP & ĐĂNG KÝ ---------- */
function renderAuthPage() {
  const u = AppState.user;
  const currentRole = u ? u.role : 'GUEST';
  const notice = AppState.sessionNotice;
  const demoAccounts = AppState.demoMode && Array.isArray(AppState.demoAccounts) && AppState.demoAccounts.length > 0
    ? AppState.demoAccounts
    : null;

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Tài khoản & Phân quyền</h1>
        <p class="page-subtitle">Đăng nhập tài khoản JWT, đăng ký người dùng mới và kiểm tra phạm vi quyền hạn được cấp từ máy chủ.</p>
      </div>
      ${u ? `<button class="btn btn-danger" onclick="handleLogout()">Đăng xuất (${escapeHtml(u.username)})</button>` : ''}
    </div>

    ${notice ? `
      <div class="state-banner error" role="alert">
        <span>${escapeHtml(notice)}</span>
        <button type="button" class="btn btn-secondary btn-sm" onclick="AppState.sessionNotice='';renderCurrentPage(false)">Đã hiểu</button>
      </div>
    ` : ''}

    <div class="card" style="margin-bottom:24px;">
      <div class="card-header">
        <h2 class="card-title">Trạng thái Phiên làm việc hiện tại</h2>
        <span class="badge badge-primary">${escapeHtml(getRoleDisplayName(currentRole))}</span>
      </div>
      <div style="font-size:0.88rem;margin-bottom:10px;">
        ${u
          ? `Đang đăng nhập với tài khoản <strong>${escapeHtml(u.username)}</strong> (${escapeHtml(u.fullName || u.role)}).`
          : 'Bạn đang ở chế độ <strong>Khách vãng lai</strong> (chỉ xem dữ liệu công khai).'}
      </div>
      <div style="background:var(--bg-surface-secondary);border:1px solid var(--border-color);border-radius:var(--radius-sm);padding:12px;font-size:0.84rem;">
        <div style="margin-bottom:6px;"><strong>Quyền được máy chủ cấp phát:</strong></div>
        <div style="line-height:1.8;">
          ${(AppState.permissions || []).map(p => `<span class="badge badge-primary" style="margin:2px 4px 2px 0;">${escapeHtml(getPermissionDisplayName(p))}</span>`).join('') || '<span class="badge badge-neutral">Chỉ xem công khai</span>'}
        </div>
      </div>

      ${demoAccounts ? `
        <div style="margin-top:16px;padding-top:14px;border-top:1px dashed var(--border-strong);">
          <div style="font-size:0.82rem;font-weight:700;color:var(--color-warning);margin-bottom:8px;">
            CHẾ ĐỘ DEMO ĐƯỢC BẬT TỪ MÁY CHỦ — Chọn nhanh vai trò kiểm thử:
          </div>
          <div class="page-actions">
            ${demoAccounts.map(acc => `
              <button type="button" class="btn ${currentRole === acc.role && u?.username === acc.username ? 'btn-primary' : 'btn-secondary'} btn-sm"
                data-username="${escapeHtml(acc.username)}" data-secret="${escapeHtml(acc.secret)}"
                onclick="loginWithCredentials(this.dataset.username, this.dataset.secret)">
                ${escapeHtml(acc.label)}
              </button>
            `).join('')}
          </div>
        </div>
      ` : ''}
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-header"><h2 class="card-title">Đăng nhập Tài khoản</h2></div>
        <form id="loginForm" onsubmit="handleLoginSubmit(event)">
          <div class="form-error-banner" hidden></div>
          <div class="form-group">
            <label for="loginUsername">Tên đăng nhập</label>
            <input id="loginUsername" data-field="username" class="input" required autocomplete="username" placeholder="Nhập tên đăng nhập" />
          </div>
          <div class="form-group">
            <label for="loginPassword">Mật khẩu</label>
            <input id="loginPassword" data-field="password" type="password" class="input" required autocomplete="current-password" placeholder="Nhập mật khẩu" />
          </div>
          <button type="submit" class="btn btn-primary" style="width:100%">Đăng nhập</button>
        </form>
      </div>

      <div class="card">
        <div class="card-header"><h2 class="card-title">Đăng ký Tài khoản mới</h2></div>
        <form id="registerForm" onsubmit="handleRegisterSubmit(event)">
          <div class="form-error-banner" hidden></div>
          <div class="form-grid">
            <div class="form-group">
              <label for="regUsername">Tên đăng nhập</label>
              <input id="regUsername" data-field="username" class="input" required placeholder="vd: hlv_quangninh" />
            </div>
            <div class="form-group">
              <label for="regPassword">Mật khẩu</label>
              <input id="regPassword" data-field="password" type="password" class="input" required minlength="6" placeholder="Tối thiểu 6 ký tự" />
            </div>
          </div>
          <div class="form-grid">
            <div class="form-group">
              <label for="regFullName">Họ và tên</label>
              <input id="regFullName" data-field="fullName" class="input" required placeholder="Nguyễn Văn A" />
            </div>
            <div class="form-group">
              <label for="regEmail">Email</label>
              <input id="regEmail" data-field="email" type="email" class="input" required placeholder="user@sports.vn" />
            </div>
          </div>
          <div class="form-group">
            <label for="regRole">Vai trò đăng ký</label>
            <select id="regRole" data-field="role" class="select">
              <option value="ROLE_VIEWER">Khán giả theo dõi giải đấu</option>
              <option value="ROLE_TEAM_MANAGER">Lãnh đội / Huấn luyện viên</option>
            </select>
            <div style="font-size:0.76rem;color:var(--text-secondary);margin-top:4px;">
              Đăng ký công khai chỉ nhận vai trò Khán giả hoặc Lãnh đội / HLV. Vai trò Ban tổ chức và Trọng tài do Quản trị viên cấp.
            </div>
          </div>
          <button type="submit" class="btn btn-primary" style="width:100%">Tạo Tài khoản mới</button>
        </form>
      </div>
    </div>
  `;
}

/* ---------- PAGE: TỔNG QUAN ĐIỀU HÀNH (DASHBOARD) ---------- */
function renderDashboardPage() {
  const d = AppState.data;
  const tourTotal = AppState.stats.tournaments?.total ?? d.tournaments.length;
  const ongoingCount = AppState.stats.tournaments?.ongoing ?? d.tournaments.filter(t => t.status === 'ONGOING').length;
  const compTotal = AppState.stats.tournaments?.totalCompetitions ?? d.competitions.length;
  const teamTotal = AppState.stats.teams?.totalTeams ?? d.teams.length;
  const athTotal = AppState.stats.athletes?.total ?? d.athletes.length;
  const matchTotal = AppState.stats.matches?.totalMatches ?? d.matches.length;
  const scheduledMatches = AppState.stats.matches?.scheduledMatches ?? d.matches.filter(m => m.status === 'SCHEDULED').length;
  const todayCount = AppState.stats.matches?.todayMatches ?? d.matches.length;

  const todayIso = getTodayLocalIsoDate();
  const todayMatches = d.matches.filter(m => !m.matchTime || String(m.matchTime).startsWith(todayIso));
  const pendingRegs = d.registrations.filter(r => String(r.status || '').toUpperCase() === 'PENDING');
  const pendingRegsTotal = AppState.stats.registrations?.pendingRegistrations ?? pendingRegs.length;
  const pendingResults = d.results.filter(r => String(r.status || '').toUpperCase() === 'PENDING_CONFIRMATION');
  const pendingResultsTotal = AppState.stats.results?.pendingResults ?? pendingResults.length;

  const canReviewRegs = hasPermission('registrations.review');
  const canViewPendingResults = hasPermission('results.approve') || hasPermission('results.submit_assigned');
  const canApproveResults = hasPermission('results.approve');

  const selectedCid = AppState.dashboardChartCompetitionId || d.competitions[0]?.id || null;

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Tổng quan Điều hành</h1>
        <p class="page-subtitle">Theo dõi các công việc ưu tiên theo ngày, trạng thái đăng ký và kết quả chờ xác nhận trong phạm vi quyền của bạn.</p>
      </div>
      <div class="page-actions">
        <button class="btn btn-secondary" onclick="retryFailedServices()">↻ Làm mới</button>
        ${hasPermission('tournaments.create') ? `<button class="btn btn-primary" onclick="openCreateTournamentModal()">+ Tạo Giải đấu</button>` : ''}
      </div>
    </div>

    ${renderServiceStateBanner(['tournaments', 'matches', 'registrations', 'results', 'rankings'])}

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-top"><span>Giải đấu</span><span class="badge badge-success">${ongoingCount} đang diễn ra</span></div>
        <div class="kpi-value">${tourTotal}</div>
        <div class="kpi-meta">${compTotal} nội dung thi đấu</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-top"><span>Đội tuyển</span><span class="badge badge-primary">Đăng ký</span></div>
        <div class="kpi-value">${teamTotal}</div>
        <div class="kpi-meta">Đơn vị / CLB tham dự</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-top"><span>Vận động viên</span><span class="badge badge-primary">Hồ sơ</span></div>
        <div class="kpi-value">${athTotal}</div>
        <div class="kpi-meta">Hồ sơ VĐV được quản lý</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-top"><span>Trận đấu</span><span class="badge badge-warning">${scheduledMatches} chờ thi đấu</span></div>
        <div class="kpi-value">${matchTotal}</div>
        <div class="kpi-meta">Trận hôm nay: ${todayCount}</div>
      </div>
    </div>

    <!-- PRIORITY WORKFLOW WIDGETS (TODAY'S MATCHES, PENDING REGISTRATIONS, PENDING RESULTS) -->
    <div class="card" style="margin-bottom:24px;">
      <div class="card-header">
        <div>
          <h2 class="card-title">Trận đấu Hôm nay (${escapeHtml(todayIso)})</h2>
          <div style="font-size:0.82rem;color:var(--text-secondary);">Danh sách trận đấu diễn ra trong ngày hôm nay</div>
        </div>
        <button class="btn btn-secondary btn-sm" onclick="navigateTo('schedule')">Xem toàn bộ lịch (${matchTotal})</button>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr><th>Giờ thi đấu</th><th>Cặp đấu</th><th>Vòng đấu</th><th>Địa điểm</th><th>Trạng thái</th><th>Thao tác</th></tr>
          </thead>
          <tbody>
            ${todayMatches.map(m => `
              <tr>
                <td class="tabular-nums">${escapeHtml(String(m.matchTime || '').replace('T', ' '))}</td>
                <td><strong>${escapeHtml(m.participant1 || '—')}</strong> vs <strong>${escapeHtml(m.participant2 || '—')}</strong></td>
                <td><span class="badge badge-neutral">${escapeHtml(m.round || 'Vòng đấu')}</span></td>
                <td>${escapeHtml(m.location || 'Chưa cập nhật')}</td>
                <td>${statusBadgeHtml(m.status)}</td>
                <td><button class="btn btn-secondary btn-sm" onclick="viewMatchDetails(${m.id})">Chi tiết</button></td>
              </tr>
            `).join('') || `<tr><td colspan="6" class="state-empty-box">Không có trận đấu nào diễn ra trong ngày hôm nay (${escapeHtml(todayIso)}). ${scheduledMatches > 0 ? `Hiện có ${scheduledMatches} trận sắp diễn ra trong lịch thi đấu.` : ''}</td></tr>`}
          </tbody>
        </table>
      </div>
    </div>

    ${(canReviewRegs || canViewPendingResults) ? `
      <div class="grid-2">
        ${canReviewRegs ? `
          <div class="card">
            <div class="card-header">
              <div>
                <h2 class="card-title">Đăng ký chờ duyệt</h2>
                <span class="badge ${pendingRegsTotal > 0 ? 'badge-warning' : 'badge-neutral'}">${pendingRegsTotal} hồ sơ chờ xử lý</span>
              </div>
              <button class="btn btn-secondary btn-sm" onclick="AppState.tournamentDetailTab='teams';navigateTo('tournament-detail')">Chi tiết giải</button>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr><th>Đội / VĐV</th><th>Nội dung</th><th>Thao tác</th></tr>
                </thead>
                <tbody>
                  ${pendingRegs.slice(0, 6).map(r => `
                    <tr>
                      <td><strong>${escapeHtml(r.participantName || `Đối tượng #${r.participantReferenceId}`)}</strong></td>
                      <td>${escapeHtml(r.competitionName || `Nội dung #${r.competitionId}`)}</td>
                      <td>
                        <div class="page-actions">
                          <button type="button" class="btn btn-primary btn-sm" onclick="approveRegistrationById(${r.id}, this)">Duyệt</button>
                          <button type="button" class="btn btn-danger btn-sm" data-name="${escapeHtml(r.participantName || 'đối tượng')}" onclick="openRejectRegistrationModal(${r.id}, this.dataset.name)">Từ chối</button>
                        </div>
                      </td>
                    </tr>
                  `).join('') || '<tr><td colspan="3" class="state-empty-box">Không có hồ sơ đăng ký nào đang chờ duyệt.</td></tr>'}
                </tbody>
              </table>
            </div>
          </div>
        ` : ''}

        ${canViewPendingResults ? `
          <div class="card">
            <div class="card-header">
              <div>
                <h2 class="card-title">Kết quả chờ xác nhận</h2>
                <span class="badge ${pendingResultsTotal > 0 ? 'badge-warning' : 'badge-neutral'}">${pendingResultsTotal} kết quả chờ duyệt</span>
                ${(AppState.stats.results?.failedSyncResults || 0) > 0
                  ? `<span class="badge badge-error" style="margin-left:6px;">${AppState.stats.results.failedSyncResults} lỗi đồng bộ BXH/Trận</span>`
                  : ((AppState.stats.results?.pendingSyncResults || 0) > 0
                    ? `<span class="badge badge-warning" style="margin-left:6px;">${AppState.stats.results.pendingSyncResults} chờ đồng bộ</span>`
                    : '')}
              </div>
              <button class="btn btn-secondary btn-sm" onclick="navigateTo('results')">Quản lý kết quả</button>
            </div>
            <div class="table-wrap">
              <table class="data-table">
                <thead>
                  <tr><th>Trận đấu</th><th>Tỷ số</th><th>Thao tác</th></tr>
                </thead>
                <tbody>
                  ${pendingResults.slice(0, 6).map(r => {
                    const m = d.matches.find(x => Number(x.id) === Number(r.matchId));
                    const matchLabel = m ? `${m.participant1} vs ${m.participant2}` : `Trận #${r.matchId}`;
                    return `
                      <tr>
                        <td><strong>${escapeHtml(matchLabel)}</strong></td>
                        <td class="tabular-nums"><strong>${r.scoreParticipant1} - ${r.scoreParticipant2}</strong></td>
                        <td>
                          ${canApproveResults
                            ? `<button type="button" class="btn btn-primary btn-sm" onclick="confirmMatchResult(${r.id}, this)">✓ Xác nhận</button>`
                            : `<span class="badge badge-warning">Chờ BTC duyệt</span>`}
                        </td>
                      </tr>
                    `;
                  }).join('') || '<tr><td colspan="3" class="state-empty-box">Không có kết quả nào đang chờ xác nhận.</td></tr>'}
                </tbody>
              </table>
            </div>
          </div>
        ` : ''}
      </div>
    ` : ''}

    <div class="grid-2">
      <div class="card">
        <div class="card-header" style="flex-wrap:wrap;gap:10px;">
          <h2 class="card-title">Điểm số & Hiệu suất BXH</h2>
          ${d.competitions.length > 0 ? `
            <select id="dashboardChartSportSelect" class="select" style="width:auto;padding:4px 10px;font-size:0.8rem;" onchange="updateDashboardChartSport(this.value)" aria-label="Chọn nội dung thống kê">
              ${d.competitions.map(c => `<option value="${c.id}" ${Number(c.id) === Number(selectedCid) ? 'selected' : ''}>${escapeHtml(c.name || c.competitionName)}</option>`).join('')}
            </select>
          ` : ''}
        </div>
        <div class="chart-box" id="dashboardStandingsChartBox"><canvas id="dashboardStandingsChart"></canvas></div>
      </div>

      <div class="card">
        <div class="card-header">
          <h2 class="card-title">Phân bổ Trạng thái Trận đấu (${matchTotal})</h2>
        </div>
        <div class="chart-box" id="dashboardMatchStatusChartBox"><canvas id="dashboardMatchStatusChart"></canvas></div>
      </div>
    </div>
  `;
}

function initDashboardCharts(selectedCid = null) {
  if (!window.Chart) return;
  const styles = getComputedStyle(document.documentElement);
  const textColor = styles.getPropertyValue('--chart-text').trim() || '#64748B';
  const gridColor = styles.getPropertyValue('--chart-grid').trim() || '#E2E8F0';
  const primaryColor = styles.getPropertyValue('--color-primary').trim() || '#2563EB';
  const successColor = styles.getPropertyValue('--color-success').trim() || '#10B981';
  const warningColor = styles.getPropertyValue('--color-warning').trim() || '#F59E0B';
  const errorColor = styles.getPropertyValue('--color-error').trim() || '#EF4444';

  const cid = Number(selectedCid || AppState.dashboardChartCompetitionId || AppState.data.competitions[0]?.id || 0);
  const rkEntry = (AppState.data.rankings || []).find(rk => Number(rk?.ranking?.competitionId) === cid);
  const items = Array.isArray(rkEntry?.items) ? rkEntry.items : [];

  const ctx1Box = document.getElementById('dashboardStandingsChartBox');
  if (ctx1Box) {
    if (AppState.charts.c1) { AppState.charts.c1.destroy(); AppState.charts.c1 = null; }
    if (items.length === 0) {
      ctx1Box.innerHTML = `<div class="state-empty-box" style="padding-top:95px;">Chưa có dữ liệu bảng xếp hạng để vẽ biểu đồ.</div>`;
    } else {
      ctx1Box.innerHTML = '<canvas id="dashboardStandingsChart"></canvas>';
      const chartItems = items.slice(0, 8);
      AppState.charts.c1 = new Chart(document.getElementById('dashboardStandingsChart'), {
        type: 'bar',
        data: {
          labels: chartItems.map(i => i.participantName || 'Đội'),
          datasets: [
            { label: 'Điểm số', data: chartItems.map(i => i.points ?? 0), backgroundColor: primaryColor, borderRadius: 6 },
            { label: 'Điểm / Bàn ghi', data: chartItems.map(i => i.goalsFor ?? 0), backgroundColor: successColor, borderRadius: 6 }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { labels: { color: textColor } } },
          scales: {
            x: { ticks: { color: textColor }, grid: { color: gridColor } },
            y: { beginAtZero: true, ticks: { color: textColor }, grid: { color: gridColor } }
          }
        }
      });
    }
  }

  const ctx2Box = document.getElementById('dashboardMatchStatusChartBox');
  if (ctx2Box) {
    if (AppState.charts.c2) { AppState.charts.c2.destroy(); AppState.charts.c2 = null; }
    const st = AppState.stats.matches;
    const fin = st ? Number(st.finishedMatches || 0) : AppState.data.matches.filter(m => m.status === 'FINISHED').length;
    const sch = st ? Number(st.scheduledMatches || 0) : AppState.data.matches.filter(m => m.status === 'SCHEDULED').length;
    const ong = st ? Number(st.ongoingMatches || 0) : AppState.data.matches.filter(m => m.status === 'ONGOING').length;
    const total = st ? Number(st.totalMatches || 0) : AppState.data.matches.length;
    const can = Math.max(0, total - fin - sch - ong);
    if (total === 0) {
      ctx2Box.innerHTML = `<div class="state-empty-box" style="padding-top:95px;">Chưa có dữ liệu trận đấu để thống kê.</div>`;
    } else {
      ctx2Box.innerHTML = '<canvas id="dashboardMatchStatusChart"></canvas>';
      AppState.charts.c2 = new Chart(document.getElementById('dashboardMatchStatusChart'), {
        type: 'doughnut',
        data: {
          labels: ['Đã kết thúc', 'Chờ thi đấu', 'Đang diễn ra', 'Đã hủy'],
          datasets: [{
            data: [fin, sch, ong, can],
            backgroundColor: [successColor, primaryColor, warningColor, errorColor],
            borderWidth: 0
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'bottom', labels: { color: textColor } } }
        }
      });
    }
  }
}

function updateDashboardChartSport(cid) {
  AppState.dashboardChartCompetitionId = Number(cid);
  initDashboardCharts(AppState.dashboardChartCompetitionId);
}

/* ---------- PAGE: QUẢN LÝ GIẢI ĐẤU (BACKEND PAGED) ---------- */
function renderTournamentsPage() {
  const slice = AppState.paged.tournaments;
  const canCreate = hasPermission('tournaments.create');

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Quản lý Giải đấu</h1>
        <p class="page-subtitle">Tạo, chỉnh sửa thông tin giải đấu, chuyển trạng thái và quản lý môn thể thao.</p>
      </div>
      <div class="page-actions">
        ${canCreate ? `<button class="btn btn-secondary" onclick="openCreateSportModal()">+ Thêm môn thể thao</button>` : ''}
        ${canCreate ? `<button class="btn btn-primary" onclick="openCreateTournamentModal()">+ Tạo giải đấu</button>` : ''}
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <input id="tourFilterKeyword" class="input" value="${escapeHtml(slice.filters.keyword || '')}" placeholder="Tìm tên giải đấu hoặc địa điểm..." oninput="applyPagedFilter('tournaments', { keyword: this.value }, true)" />
          <select id="tourFilterStatus" class="select" onchange="applyPagedFilter('tournaments', { status: this.value })">
            <option value="" ${!slice.filters.status ? 'selected' : ''}>Tất cả trạng thái</option>
            <option value="DRAFT" ${slice.filters.status === 'DRAFT' ? 'selected' : ''}>Bản nháp</option>
            <option value="OPEN_REGISTRATION" ${slice.filters.status === 'OPEN_REGISTRATION' ? 'selected' : ''}>Đang mở đăng ký</option>
            <option value="UPCOMING" ${slice.filters.status === 'UPCOMING' ? 'selected' : ''}>Sắp khởi tranh</option>
            <option value="ONGOING" ${slice.filters.status === 'ONGOING' ? 'selected' : ''}>Đang diễn ra</option>
            <option value="COMPLETED" ${slice.filters.status === 'COMPLETED' ? 'selected' : ''}>Đã hoàn thành</option>
            <option value="FINISHED" ${slice.filters.status === 'FINISHED' ? 'selected' : ''}>Đã kết thúc</option>
            <option value="CANCELLED" ${slice.filters.status === 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
          </select>
        </div>
        <span class="badge badge-primary" id="tournamentsTotalBadge">Tổng số: ${slice.totalElements} giải đấu</span>
      </div>

      <div id="tournamentsTableRegion">
        ${renderTournamentsTableInner()}
      </div>
    </div>
  `;
}

function renderTournamentsTableInner() {
  const slice = AppState.paged.tournaments;
  const canManage = hasPermission('tournaments.manage');
  const startIdx = slice.page * slice.size;

  const badge = document.getElementById('tournamentsTotalBadge');
  if (badge) badge.textContent = `Tổng số: ${slice.totalElements} giải đấu`;

  return `
    ${renderServiceStateBanner('tournaments', "fetchPagedResource('tournaments', updateTournamentsRegion)")}
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>STT</th>
            <th>Tên giải đấu</th>
            <th>Địa điểm</th>
            <th>Khởi tranh — Kết thúc</th>
            <th>Trạng thái</th>
            <th>Thao tác</th>
          </tr>
        </thead>
        <tbody>
          ${slice.items.map((t, idx) => `
            <tr>
              <td class="tabular-nums"><strong>#${String(startIdx + idx + 1).padStart(2, '0')}</strong></td>
              <td>
                <strong>${escapeHtml(t.name)}</strong>
                <div style="font-size:0.78rem;color:var(--text-secondary)">${escapeHtml(t.description || '')}</div>
              </td>
              <td>${escapeHtml(t.location || 'Chưa cập nhật')}</td>
              <td class="tabular-nums">${escapeHtml(t.startDate || '—')} → ${escapeHtml(t.endDate || '—')}</td>
              <td>${statusBadgeHtml(t.status)}</td>
              <td>
                <div class="page-actions">
                  <button class="btn btn-secondary btn-sm" onclick="AppState.selectedTournamentId=${t.id};navigateTo('tournament-detail')">Chi tiết</button>
                  ${canManage ? `<button class="btn btn-secondary btn-sm" onclick="openEditTournamentModal(${t.id})">Sửa</button>` : ''}
                  ${canManage ? `<button class="btn btn-secondary btn-sm" onclick="patchTournamentStatus(${t.id}, '${t.status === 'ONGOING' ? 'COMPLETED' : 'ONGOING'}', this)">Đổi TT</button>` : ''}
                  ${canManage ? `<button class="btn btn-danger btn-sm" data-name="${escapeHtml(t.name)}" onclick="confirmDeleteTournament(${t.id}, this.dataset.name)">Xóa</button>` : ''}
                </div>
              </td>
            </tr>
          `).join('') || `<tr><td colspan="6" class="state-empty-box">${slice.loading ? 'Đang tải danh sách giải đấu...' : 'Không tìm thấy giải đấu nào.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('tournaments', slice, 'giải đấu', [5, 10, 20, 50])}
  `;
}

function updateTournamentsRegion() {
  const el = document.getElementById('tournamentsTableRegion');
  if (el) el.innerHTML = renderTournamentsTableInner();
}

/* ---------- PAGE: CHI TIẾT GIẢI ĐẤU & NHÁNH ĐẤU (100% REAL DATA) ---------- */
function renderBracketMatchNode(m, results) {
  const res = results.find(r => Number(r.matchId) === Number(m.id)) || null;
  const hasScore = res && res.scoreParticipant1 !== null && res.scoreParticipant1 !== undefined;
  const isConfirmed = res && String(res.status || '').toUpperCase() === 'CONFIRMED';
  const score1 = hasScore ? res.scoreParticipant1 : '-';
  const score2 = hasScore ? res.scoreParticipant2 : '-';

  let team1Won = false;
  let team2Won = false;
  if (isConfirmed) {
    if (res.winner) {
      team1Won = res.winner === m.participant1;
      team2Won = res.winner === m.participant2;
    } else if (hasScore) {
      team1Won = Number(score1) > Number(score2);
      team2Won = Number(score2) > Number(score1);
    }
  }

  const p1 = m.participant1 || 'Chưa xác định';
  const p2 = m.participant2 || 'Chưa xác định';

  return `
    <div class="bracket-node" role="button" tabindex="0" onclick="viewMatchDetails(${m.id})" onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();viewMatchDetails(${m.id});}">
      <div class="bracket-node-header">
        <span class="bracket-round-badge">${escapeHtml(m.round || 'Vòng đấu')}</span>
        <span class="bracket-venue" title="${escapeHtml(m.location || '')}">${escapeHtml(m.location || 'Chưa cập nhật sân')}</span>
      </div>
      <div class="bracket-team ${team1Won ? 'winner' : ''}">
        <div class="bracket-team-info">
          <span class="team-crest-mini">${escapeHtml(formatTeamCrestInitials(p1))}</span>
          <span class="bracket-team-name">${escapeHtml(p1)}</span>
        </div>
        <span class="bracket-score tabular-nums ${team1Won ? 'winner-score' : ''}">${score1}</span>
      </div>
      <div class="bracket-team ${team2Won ? 'winner' : ''}">
        <div class="bracket-team-info">
          <span class="team-crest-mini">${escapeHtml(formatTeamCrestInitials(p2))}</span>
          <span class="bracket-team-name">${escapeHtml(p2)}</span>
        </div>
        <span class="bracket-score tabular-nums ${team2Won ? 'winner-score' : ''}">${score2}</span>
      </div>
      <div class="bracket-node-footer">
        <div>
          ${statusBadgeHtml(m.status)}
          ${res && !isConfirmed ? '<span class="badge badge-warning" style="margin-left:4px;">KQ chờ XN</span>' : ''}
        </div>
        <span style="font-size:0.72rem;color:var(--text-secondary);">${m.referee ? escapeHtml(m.referee) : 'Chưa phân công TT'}</span>
      </div>
    </div>
  `;
}

function renderDynamicTournamentBracketSection(tour, matches, regs, results) {
  const quarterMatches = matches.filter(m => /tứ kết|quarter/i.test(m.round || ''));
  const semiMatches = matches.filter(m => /bán kết|semi/i.test(m.round || ''));
  const finalMatches = matches.filter(m => /chung kết|final/i.test(m.round || '') && !/bán kết|semi/i.test(m.round || ''));

  const hasAnyKnockout = quarterMatches.length > 0 || semiMatches.length > 0 || finalMatches.length > 0;

  let championTeam = '';
  if (finalMatches.length > 0) {
    const fm = finalMatches[0];
    const fres = results.find(r => Number(r.matchId) === Number(fm.id));
    if (fres && String(fres.status || '').toUpperCase() === 'CONFIRMED') {
      if (fres.winner && fres.winner !== 'DRAW') {
        championTeam = fres.winner;
      } else if (fres.scoreParticipant1 !== null && fres.scoreParticipant2 !== null) {
        const s1 = Number(fres.scoreParticipant1);
        const s2 = Number(fres.scoreParticipant2);
        if (s1 > s2) championTeam = fm.participant1;
        else if (s2 > s1) championTeam = fm.participant2;
      }
    }
  }

  return `
    <div class="card" style="margin-bottom:24px;">
      <div class="card-header">
        <div>
          <h2 class="card-title">Sơ đồ Nhánh đấu Loại trực tiếp</h2>
          <p style="font-size:0.84rem;color:var(--text-secondary);margin:0;">
            Hiển thị các trận Tứ kết, Bán kết, Chung kết và kết quả thực tế của giải đấu
          </p>
        </div>
        <span class="badge badge-primary">${matches.length} trận thuộc giải</span>
      </div>

      ${!hasAnyKnockout ? `
        <div class="state-empty-box">
          <strong>Chưa có trận đấu loại trực tiếp (Tứ kết / Bán kết / Chung kết) cho giải đấu này.</strong>
          <div style="margin-top:6px;">
            ${matches.length > 0
              ? `Giải đấu hiện có ${matches.length} trận ở các vòng bảng/vòng tròn. Bạn có thể chuyển sang tab Lịch thi đấu để theo dõi.`
              : 'Chưa có trận đấu nào được tạo cho giải đấu này.'}
          </div>
          ${hasPermission('matches.schedule') ? `
            <div style="margin-top:12px;">
              <button class="btn btn-primary btn-sm" onclick="openAutoGenerateScheduleModal(${tour.id})">⚡ Tự động xếp lịch thi đấu</button>
            </div>
          ` : ''}
        </div>
      ` : `
        <div class="bracket-container">
          ${quarterMatches.length > 0 ? `
            <div class="bracket-column">
              <div class="bracket-stage-title">
                <span>Tứ kết</span>
                <span class="badge badge-primary">${quarterMatches.length} trận</span>
              </div>
              ${quarterMatches.map(m => renderBracketMatchNode(m, results)).join('')}
            </div>
          ` : ''}

          ${semiMatches.length > 0 ? `
            <div class="bracket-column">
              <div class="bracket-stage-title">
                <span>Bán kết</span>
                <span class="badge badge-primary">${semiMatches.length} trận</span>
              </div>
              ${semiMatches.map(m => renderBracketMatchNode(m, results)).join('')}
            </div>
          ` : ''}

          ${finalMatches.length > 0 ? `
            <div class="bracket-column">
              <div class="bracket-stage-title">
                <span>Chung kết</span>
                <span class="badge badge-warning">${finalMatches.length} trận</span>
              </div>
              ${finalMatches.map(m => renderBracketMatchNode(m, results)).join('')}
            </div>
          ` : ''}

          <div class="bracket-column">
            <div class="bracket-stage-title">
              <span>Kết quả Vô địch</span>
              <span class="badge badge-success">Danh hiệu</span>
            </div>
            <div class="bracket-trophy-node">
              <div class="bracket-trophy-icon">🏆</div>
              <div class="bracket-trophy-title">${escapeHtml(tour.name)}</div>
              <div class="bracket-trophy-team">
                ${championTeam ? `👑 <strong>${escapeHtml(championTeam)}</strong>` : '⏳ <em>Chưa xác định nhà vô địch</em>'}
              </div>
              <div class="bracket-trophy-venue">${escapeHtml(tour.location || 'Chưa cập nhật địa điểm')}</div>
              <div class="bracket-trophy-stats">${matches.length} trận đấu • ${regs.length} hồ sơ đăng ký</div>
            </div>
          </div>
        </div>
      `}
    </div>
  `;
}

function renderTournamentTeamsTableSection(tour, tourComps, canRegister, canReview) {
  const slice = AppState.paged.registrations;
  const f = slice.filters || {};

  return `
    <div class="card">
      <div class="card-header" style="flex-wrap:wrap;gap:10px;">
        <div>
          <h2 class="card-title">Danh sách Đăng ký Tham dự (<span id="tourRegsTotalBadge">${slice.totalElements}</span>)</h2>
          <p style="font-size:0.84rem;color:var(--text-secondary);margin:0;">
            Hồ sơ đăng ký thực tế của các đội tuyển / VĐV trong ${escapeHtml(tour.name)}
          </p>
        </div>
        <div class="page-actions">
          ${canRegister && tourComps.length > 0 ? `<button class="btn btn-primary btn-sm" onclick="openRegisterParticipantModal(${tour.id})">+ Đăng ký tham gia</button>` : ''}
        </div>
      </div>

      <div class="toolbar">
        <div class="toolbar-filters">
          <input id="regFilterKeyword" class="input" value="${escapeHtml(f.keyword || '')}" placeholder="Tìm tên đội tuyển / VĐV..." oninput="applyPagedFilter('registrations', { keyword: this.value }, true)" />
          ${tourComps.length > 0 ? `
            <select class="select" onchange="applyPagedFilter('registrations', { competitionId: this.value })" aria-label="Lọc theo nội dung thi đấu">
              <option value="" ${!f.competitionId ? 'selected' : ''}>Tất cả nội dung</option>
              ${tourComps.map(c => `<option value="${c.id}" ${String(f.competitionId) === String(c.id) ? 'selected' : ''}>${escapeHtml(c.name || c.competitionName)}</option>`).join('')}
            </select>
          ` : ''}
          <select class="select" onchange="applyPagedFilter('registrations', { status: this.value })" aria-label="Lọc trạng thái đăng ký">
            <option value="" ${!f.status ? 'selected' : ''}>Tất cả trạng thái</option>
            <option value="PENDING" ${f.status === 'PENDING' ? 'selected' : ''}>Chờ duyệt</option>
            <option value="APPROVED" ${f.status === 'APPROVED' ? 'selected' : ''}>Đã duyệt</option>
            <option value="REJECTED" ${f.status === 'REJECTED' ? 'selected' : ''}>Từ chối</option>
            <option value="WITHDRAWN" ${f.status === 'WITHDRAWN' ? 'selected' : ''}>Đã rút lui</option>
          </select>
        </div>
        <span class="badge badge-primary" id="tourRegsCountPill">Tổng số: ${slice.totalElements} hồ sơ</span>
      </div>

      <div id="tournamentRegsRegion">
        ${renderTournamentRegistrationsInner(tourComps, canReview)}
      </div>
    </div>
  `;
}

function renderTournamentRegistrationsInner(tourComps = null, canReview = null) {
  const slice = AppState.paged.registrations;
  const comps = tourComps || AppState.detail.tournament.competitions || AppState.data.competitions || [];
  const compNameMap = new Map(comps.map(c => [Number(c.id), c.name || c.competitionName]));
  const reviewAllowed = canReview !== null ? canReview : hasPermission('registrations.review');
  const currentUsername = String(AppState.user?.username || '').toLowerCase();
  const isAdmin = String(AppState.user?.role || '').toUpperCase() === 'ROLE_ADMIN';
  const canWithdrawAny = Boolean(currentUsername) && slice.items.some(r => {
    const st = String(r.status || '').toUpperCase();
    return (st === 'PENDING' || st === 'APPROVED') && (isAdmin || String(r.registeredBy || '').toLowerCase() === currentUsername);
  });
  const showActionCol = reviewAllowed || canWithdrawAny;
  const startIdx = slice.page * slice.size;

  const badgeEl = document.getElementById('tourRegsTotalBadge');
  if (badgeEl) badgeEl.textContent = String(slice.totalElements);
  const pillEl = document.getElementById('tourRegsCountPill');
  if (pillEl) pillEl.textContent = `Tổng số: ${slice.totalElements} hồ sơ`;

  return `
    ${renderServiceStateBanner('registrations', "fetchPagedResource('registrations', updateTournamentRegistrationsRegion)")}
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>STT</th>
            <th>Đội tuyển / VĐV</th>
            <th>Nội dung thi đấu</th>
            <th>Loại hình</th>
            <th>Ghi chú / Lý do</th>
            <th>Trạng thái</th>
            ${showActionCol ? '<th>Thao tác</th>' : ''}
          </tr>
        </thead>
        <tbody>
          ${slice.items.map((r, idx) => {
            const pName = r.participantName || `Đối tượng #${r.participantReferenceId || r.id}`;
            const isAthlete = String(r.participantType || '').toUpperCase() === 'ATHLETE';
            const typeText = isAthlete ? 'Vận động viên' : 'Đội tuyển';
            const st = String(r.status || '').toUpperCase();
            const isPending = st === 'PENDING';
            const isApproved = st === 'APPROVED';
            const canWithdrawRow = Boolean(currentUsername) && (isPending || isApproved)
              && (isAdmin || String(r.registeredBy || '').toLowerCase() === currentUsername);
            const compName = r.competitionName || compNameMap.get(Number(r.competitionId)) || `Nội dung #${r.competitionId}`;
            return `
              <tr>
                <td class="tabular-nums"><strong>#${String(startIdx + idx + 1).padStart(2, '0')}</strong></td>
                <td>
                  <div style="display:flex;align-items:center;gap:10px;">
                    <span class="team-crest-circle" style="width:32px;height:32px;font-size:0.74rem;">${escapeHtml(formatTeamCrestInitials(pName))}</span>
                    <div>
                      <strong>${escapeHtml(pName)}</strong>
                      ${r.registeredBy ? `<div style="font-size:0.74rem;color:var(--text-secondary);">Đăng ký bởi: ${escapeHtml(r.registeredBy)}</div>` : ''}
                    </div>
                  </div>
                </td>
                <td>${escapeHtml(compName)}</td>
                <td><span class="badge ${isAthlete ? 'badge-warning' : 'badge-primary'}">${typeText}</span></td>
                <td>${escapeHtml(r.rejectionReason || '—')}</td>
                <td>${statusBadgeHtml(r.status)}</td>
                ${showActionCol ? `
                  <td>
                    <div class="page-actions">
                      ${reviewAllowed && isPending ? `<button class="btn btn-primary btn-sm" onclick="approveRegistrationById(${r.id}, this)">Duyệt</button>` : ''}
                      ${reviewAllowed && (isPending || isApproved) ? `<button class="btn btn-danger btn-sm" data-name="${escapeHtml(pName)}" onclick="openRejectRegistrationModal(${r.id}, this.dataset.name)">${isApproved ? 'Hủy duyệt' : 'Từ chối'}</button>` : ''}
                      ${canWithdrawRow ? `<button class="btn btn-secondary btn-sm" onclick="withdrawRegistrationById(${r.id}, this)">Rút đơn</button>` : ''}
                      ${(!reviewAllowed || (!isPending && !isApproved)) && !canWithdrawRow ? '<span style="font-size:0.8rem;color:var(--text-secondary);">Đã xử lý</span>' : ''}
                    </div>
                  </td>
                ` : ''}
              </tr>
            `;
          }).join('') || `<tr><td colspan="${showActionCol ? 7 : 6}" class="state-empty-box">${slice.loading ? 'Đang tải danh sách đăng ký...' : 'Chưa có hồ sơ đăng ký nào phù hợp.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('registrations', slice, 'hồ sơ đăng ký', [5, 10, 20, 50])}
  `;
}

function updateTournamentRegistrationsRegion() {
  const el = document.getElementById('tournamentRegsRegion');
  if (el) el.innerHTML = renderTournamentRegistrationsInner();
}

function renderTournamentMatchesTableSection(tour, matches, results) {
  const canSchedule = hasPermission('matches.schedule');
  return `
    <div class="card">
      <div class="card-header">
        <div>
          <h2 class="card-title">Lịch thi đấu & Kết quả của Giải (${matches.length})</h2>
          <p style="font-size:0.84rem;color:var(--text-secondary);margin:0;">Các trận đấu thực tế thuộc ${escapeHtml(tour.name)}</p>
        </div>
        <button class="btn btn-secondary btn-sm" onclick="navigateTo('schedule')">Mở trang Lịch thi đấu</button>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Vòng đấu</th>
              <th>Đội / VĐV 1</th>
              <th style="text-align:center;">Tỷ số</th>
              <th>Đội / VĐV 2</th>
              <th>Sân thi đấu</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            ${matches.map((m, idx) => {
              const res = results.find(r => Number(r.matchId) === Number(m.id));
              const scoreText = res && res.scoreParticipant1 !== null && res.scoreParticipant2 !== null
                ? `${res.scoreParticipant1} - ${res.scoreParticipant2}`
                : '- : -';
              const pendingScoreTag = res && String(res.status || '').toUpperCase() !== 'CONFIRMED'
                ? '<div style="font-size:0.72rem;color:var(--color-warning);margin-top:2px;">Chờ xác nhận</div>'
                : '';
              return `
                <tr>
                  <td class="tabular-nums"><strong>#${String(idx + 1).padStart(2, '0')}</strong></td>
                  <td><span class="badge badge-primary">${escapeHtml(m.round || 'Vòng đấu')}</span></td>
                  <td><strong>${escapeHtml(m.participant1 || '—')}</strong></td>
                  <td style="text-align:center;"><span class="score-pill tabular-nums" style="font-size:0.82rem;padding:3px 8px;">${scoreText}</span>${pendingScoreTag}</td>
                  <td><strong>${escapeHtml(m.participant2 || '—')}</strong></td>
                  <td>${escapeHtml(m.location || 'Chưa cập nhật')}</td>
                  <td>${statusBadgeHtml(m.status)}</td>
                  <td>
                    <div class="page-actions">
                      <button class="btn btn-secondary btn-sm" onclick="viewMatchDetails(${m.id})">Chi tiết</button>
                      ${canSchedule ? `<button class="btn btn-secondary btn-sm" onclick="openEditMatchModal(${m.id})">Sửa</button>` : ''}
                    </div>
                  </td>
                </tr>
              `;
            }).join('') || '<tr><td colspan="8" class="state-empty-box">Chưa có trận đấu nào thuộc phạm vi đang chọn.</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>
  `;
}

function selectTournamentDetailCompetition(cid) {
  AppState.detail.tournament.selectedCompetitionId = cid || 'ALL';
  renderCurrentPage(false);
}

function renderTournamentDetailPage() {
  const d = AppState.data;
  const detail = AppState.detail.tournament;
  if (detail.loading && !detail.item) {
    return `
      <div class="page-header"><div><h1 class="page-title">Chi tiết Giải đấu & Nhánh đấu</h1></div></div>
      <div class="state-banner loading" role="status"><span>Đang tải thông tin giải đấu theo ID...</span></div>
    `;
  }
  if (detail.error && !detail.item) {
    return `
      <div class="page-header"><div><h1 class="page-title">Chi tiết Giải đấu & Nhánh đấu</h1></div></div>
      <div class="state-banner error" role="alert">
        <span>${escapeHtml(detail.error)}</span>
        <button type="button" class="btn btn-secondary btn-sm" onclick="loadTournamentDetail(AppState.selectedTournamentId)">↻ Thử lại</button>
      </div>
    `;
  }

  const tour = detail.item || null;
  if (!tour) {
    return `
      <div class="page-header">
        <div>
          <h1 class="page-title">Chi tiết Giải đấu & Nhánh đấu</h1>
        </div>
      </div>
      ${renderServiceStateBanner(['tournaments', 'competitions', 'matches', 'registrations'])}
      <div class="card state-empty-box">Chưa có giải đấu nào trong hệ thống.</div>
    `;
  }

  const tourComps = detail.competitions.length > 0
    ? detail.competitions
    : d.competitions.filter(c => Number(c.tournamentId) === Number(tour.id));
  const selectedCid = detail.selectedCompetitionId || 'ALL';
  const allTourMatches = detail.matches;
  const visibleMatches = selectedCid !== 'ALL' && tourComps.length > 0
    ? allTourMatches.filter(m => Number(m.competitionId) === Number(selectedCid))
    : allTourMatches;
  const visibleMatchIds = new Set(visibleMatches.map(m => Number(m.id)));
  const visibleResults = (detail.results || []).filter(r => visibleMatchIds.has(Number(r.matchId)));
  const regTotal = AppState.paged.registrations.totalElements;

  const canSchedule = hasPermission('matches.schedule');
  const canManageTour = hasPermission('tournaments.manage')
    && (String(AppState.user?.role || '').toUpperCase() === 'ROLE_ADMIN'
      || (tour.organizerUsername && String(tour.organizerUsername).toLowerCase() === String(AppState.user?.username || '').toLowerCase()));
  const canRegister = hasPermission('registrations.manage_own') || hasPermission('tournaments.manage');
  const canReview = hasPermission('registrations.review');
  const currentTab = AppState.tournamentDetailTab || 'bracket';

  const switcherList = d.tournaments.some(t => Number(t.id) === Number(tour.id))
    ? d.tournaments
    : [tour].concat(d.tournaments);

  return `
    ${renderServiceStateBanner(['tournaments', 'competitions', 'matches', 'registrations', 'results'], "loadTournamentDetail(AppState.selectedTournamentId)")}

    <div class="page-header">
      <div>
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:6px;flex-wrap:wrap;">
          <h1 class="page-title">${escapeHtml(tour.name)}</h1>
          ${statusBadgeHtml(tour.status)}
        </div>
        <p class="page-subtitle">
          Địa điểm: <strong>${escapeHtml(tour.location || 'Chưa cập nhật')}</strong> •
          Thời gian: ${escapeHtml(tour.startDate || '—')} → ${escapeHtml(tour.endDate || '—')} •
          ${regTotal} đăng ký • ${visibleMatches.length} trận đấu
        </p>
      </div>
      <div class="page-actions">
        <select class="select" onchange="AppState.paged.registrations.filters.competitionId='';loadTournamentDetail(Number(this.value))" aria-label="Chọn giải đấu">
          ${switcherList.map(t => `<option value="${t.id}" ${Number(t.id) === Number(tour.id) ? 'selected' : ''}>${escapeHtml(t.name)}</option>`).join('')}
        </select>
        ${tourComps.length > 0 ? `
          <select class="select" onchange="selectTournamentDetailCompetition(this.value)" aria-label="Lọc theo nội dung thi đấu">
            <option value="ALL" ${selectedCid === 'ALL' ? 'selected' : ''}>Tất cả nội dung (${tourComps.length})</option>
            ${tourComps.map(c => `<option value="${c.id}" ${String(selectedCid) === String(c.id) ? 'selected' : ''}>${escapeHtml(c.name || c.competitionName)}</option>`).join('')}
          </select>
        ` : ''}
        ${canManageTour ? `<button class="btn btn-secondary btn-sm" onclick="openCreateCompetitionModal(${tour.id})">+ Nội dung</button>` : ''}
        ${canManageTour && tourComps.length > 0 ? `<button class="btn btn-secondary btn-sm" onclick="openCompetitionSettingsModal(${tour.id}, '${selectedCid !== 'ALL' ? selectedCid : tourComps[0].id}')">⚙ Luật & Thể thức</button>` : ''}
        ${canSchedule ? `<button class="btn btn-primary" onclick="openAutoGenerateScheduleModal(${tour.id})">⚡ Xếp lịch thi đấu</button>` : ''}
      </div>
    </div>

    <div style="display:flex;gap:8px;margin-bottom:20px;border-bottom:1px solid var(--border-color);padding-bottom:12px;flex-wrap:wrap;">
      <button class="btn ${currentTab === 'bracket' ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="AppState.tournamentDetailTab='bracket';renderCurrentPage(false)">
        🏆 Nhánh đấu Knockout
      </button>
      <button class="btn ${currentTab === 'matches' ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="AppState.tournamentDetailTab='matches';renderCurrentPage(false)">
        📋 Lịch & Kết quả (${visibleMatches.length})
      </button>
      <button class="btn ${currentTab === 'teams' ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="AppState.tournamentDetailTab='teams';renderCurrentPage(false)">
        👥 Danh sách Đăng ký (${regTotal})
      </button>
    </div>

    ${currentTab === 'bracket' ? renderDynamicTournamentBracketSection(tour, visibleMatches, AppState.paged.registrations.items, visibleResults) : ''}
    ${currentTab === 'matches' ? renderTournamentMatchesTableSection(tour, visibleMatches, visibleResults) : ''}
    ${currentTab === 'teams' ? renderTournamentTeamsTableSection(tour, tourComps, canRegister, canReview) : ''}
    ${currentTab === 'bracket' ? `<div style="margin-top:24px;">${renderTournamentTeamsTableSection(tour, tourComps, canRegister, canReview)}</div>` : ''}
  `;
}

/* ---------- PAGE: QUẢN LÝ ĐỘI TUYỂN (BACKEND PAGED + EDIT/DELETE) ---------- */
function renderTeamsPage() {
  const slice = AppState.paged.teams;
  const canCreateTeam = hasPermission('teams.manage_own');
  const canAddAthlete = hasPermission('athletes.manage_own');

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Quản lý Đội tuyển & Câu lạc bộ</h1>
        <p class="page-subtitle">Quản lý hồ sơ đội tuyển, huấn luyện viên trưởng và danh sách thành viên thi đấu.</p>
      </div>
      <div class="page-actions">
        ${canAddAthlete ? `<button class="btn btn-secondary" onclick="openAddAthleteToTeamModal()">+ Thêm thành viên</button>` : ''}
        ${canCreateTeam ? `<button class="btn btn-primary" onclick="openCreateTeamModal()">+ Tạo đội tuyển</button>` : ''}
      </div>
    </div>

    <div class="card" style="margin-bottom:20px;">
      <div class="toolbar" style="margin-bottom:0;">
        <div class="toolbar-filters">
          <input id="teamFilterKeyword" class="input" value="${escapeHtml(slice.filters.keyword || '')}" placeholder="Tìm tên đội, tên viết tắt, HLV trưởng..." oninput="applyPagedFilter('teams', { keyword: this.value }, true)" />
        </div>
        <span class="badge badge-primary" id="teamsTotalBadge">Tổng số: ${slice.totalElements} đội tuyển</span>
      </div>
    </div>

    <div id="teamsGridRegion">
      ${renderTeamsGridInner()}
    </div>
  `;
}

function renderTeamsGridInner() {
  const slice = AppState.paged.teams;
  const canEdit = hasPermission('teams.manage_own');
  const canAddAthlete = hasPermission('athletes.manage_own');
  const canDelete = hasPermission('users.manage');
  const startIdx = slice.page * slice.size;

  const badge = document.getElementById('teamsTotalBadge');
  if (badge) badge.textContent = `Tổng số: ${slice.totalElements} đội tuyển`;

  return `
    ${renderServiceStateBanner('teams', "fetchPagedResource('teams', updateTeamsRegion)")}
    <div class="grid-3">
      ${slice.items.map((t, idx) => {
        const isOwn = !t.ownerUsername || (AppState.user && t.ownerUsername.toLowerCase() === AppState.user.username.toLowerCase()) || AppState.user?.role === 'ROLE_ADMIN';
        return `
          <div class="card">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
              <span class="team-crest-circle" title="${escapeHtml(t.name)}">${escapeHtml(formatTeamCrestInitials(t.name, t.shortName))}</span>
              <span class="badge badge-neutral">#${String(startIdx + idx + 1).padStart(2, '0')}</span>
            </div>
            <h3 class="card-title" style="margin-bottom:6px;">${escapeHtml(t.name)}</h3>
            <p style="font-size:0.84rem;color:var(--text-secondary);margin-bottom:14px;">
              HLV Trưởng: <strong>${escapeHtml(t.managerName || 'Chưa cập nhật')}</strong><br/>
              Liên hệ: ${escapeHtml(t.contactPhone || '—')} • ${escapeHtml(t.contactEmail || '—')}
            </p>
            <div class="page-actions" style="border-top:1px solid var(--border-color);padding-top:12px;">
              <button class="btn btn-primary btn-sm" data-name="${escapeHtml(t.name)}" onclick="viewTeamMembersModal(${t.id}, this.dataset.name)">Đội hình</button>
              ${canAddAthlete && isOwn ? `<button class="btn btn-secondary btn-sm" onclick="openAddAthleteToTeamModalForTeam(${t.id})">+ VĐV</button>` : ''}
              ${canEdit && isOwn ? `<button class="btn btn-secondary btn-sm" onclick="openEditTeamModal(${t.id})">Sửa</button>` : ''}
              ${canDelete ? `<button class="btn btn-danger btn-sm" data-name="${escapeHtml(t.name)}" onclick="confirmDeleteTeam(${t.id}, this.dataset.name)">Xóa</button>` : ''}
            </div>
          </div>
        `;
      }).join('') || `<div class="card state-empty-box" style="grid-column:1/-1;">${slice.loading ? 'Đang tải danh sách đội tuyển...' : 'Không tìm thấy đội tuyển nào.'}</div>`}
    </div>
    <div class="card" style="padding:12px 20px;">
      ${renderPaginationBar('teams', slice, 'đội tuyển', [6, 9, 12, 24])}
    </div>
  `;
}

function updateTeamsRegion() {
  const el = document.getElementById('teamsGridRegion');
  if (el) el.innerHTML = renderTeamsGridInner();
}

/* ---------- PAGE: QUẢN LÝ VẬN ĐỘNG VIÊN (BACKEND PAGED + EDIT/DELETE) ---------- */
function renderAthletesPage() {
  const slice = AppState.paged.athletes;
  const canCreate = hasPermission('athletes.manage_own');

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Quản lý Vận động viên</h1>
        <p class="page-subtitle">Tìm kiếm, cập nhật hồ sơ cá nhân và kiểm tra tình trạng thể lực thi đấu.</p>
      </div>
      <div class="page-actions">
        ${canCreate ? `<button class="btn btn-primary" onclick="openCreateAthleteModal()">+ Thêm vận động viên</button>` : ''}
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <input id="athleteFilterKeyword" class="input" value="${escapeHtml(slice.filters.keyword || '')}" placeholder="Tìm họ tên, SĐT, email, CCCD..." oninput="applyPagedFilter('athletes', { keyword: this.value }, true)" />
          <select class="select" onchange="applyPagedFilter('athletes', { gender: this.value })" aria-label="Lọc theo giới tính">
            <option value="" ${!slice.filters.gender ? 'selected' : ''}>Tất cả giới tính</option>
            <option value="MALE" ${slice.filters.gender === 'MALE' ? 'selected' : ''}>Nam</option>
            <option value="FEMALE" ${slice.filters.gender === 'FEMALE' ? 'selected' : ''}>Nữ</option>
            <option value="OTHER" ${slice.filters.gender === 'OTHER' ? 'selected' : ''}>Khác</option>
          </select>
          <select class="select" onchange="applyPagedFilter('athletes', { healthStatus: this.value })" aria-label="Lọc theo thể lực">
            <option value="" ${!slice.filters.healthStatus ? 'selected' : ''}>Tất cả thể lực</option>
            <option value="QUALIFIED" ${slice.filters.healthStatus === 'QUALIFIED' ? 'selected' : ''}>Đủ điều kiện (QUALIFIED)</option>
            <option value="INJURED" ${slice.filters.healthStatus === 'INJURED' ? 'selected' : ''}>Chấn thương (INJURED)</option>
            <option value="SUSPENDED" ${slice.filters.healthStatus === 'SUSPENDED' ? 'selected' : ''}>Treo giò (SUSPENDED)</option>
          </select>
        </div>
        <span class="badge badge-primary" id="athletesTotalBadge">Tổng số: ${slice.totalElements} VĐV</span>
      </div>

      <div id="athletesTableRegion">
        ${renderAthletesTableInner()}
      </div>
    </div>
  `;
}

function renderAthletesTableInner() {
  const slice = AppState.paged.athletes;
  const canEdit = hasPermission('athletes.manage_own');
  const canDelete = hasPermission('users.manage');
  const canSeeSensitive = hasPermission('athletes.manage_own') || hasPermission('users.manage');
  const startIdx = slice.page * slice.size;

  const badge = document.getElementById('athletesTotalBadge');
  if (badge) badge.textContent = `Tổng số: ${slice.totalElements} VĐV`;

  return `
    ${renderServiceStateBanner('athletes', "fetchPagedResource('athletes', updateAthletesRegion)")}
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>STT</th><th>Họ và tên</th><th>Ngày sinh</th><th>Giới tính</th><th>Số CCCD</th><th>Liên hệ</th><th>Thể lực</th>${canEdit ? '<th>Thao tác</th>' : ''}</tr>
        </thead>
        <tbody>
          ${slice.items.map((a, idx) => {
            const isOwnAthlete = a.ownerUsername && AppState.user?.username && String(a.ownerUsername).toLowerCase() === String(AppState.user.username).toLowerCase();
            const isPrivileged = ['ROLE_ADMIN', 'ROLE_ORGANIZER'].includes(String(AppState.user?.role || '').toUpperCase());
            const canSeeSensitive = Boolean(isPrivileged || isOwnAthlete);
            const cccdDisplay = canSeeSensitive ? (a.identityCardNumber || 'Chưa cập nhật') : '******';
            const phone = a.phoneNumber || a.phone || '';
            const contactDisplay = canSeeSensitive
              ? `${escapeHtml(phone || '—')}<div style="font-size:0.75rem;color:var(--text-secondary)">${escapeHtml(a.email || '')}</div>`
              : '<span style="color:var(--text-secondary)">Ẩn thông tin</span>';
            const hStatus = a.healthStatus || 'QUALIFIED';
            const nextHealth = hStatus === 'INJURED' ? 'QUALIFIED' : 'INJURED';
            const canEditRow = canEdit && (isPrivileged || isOwnAthlete);
            return `
              <tr>
                <td class="tabular-nums"><strong>#${String(startIdx + idx + 1).padStart(3, '0')}</strong></td>
                <td><strong>${escapeHtml(a.fullName)}</strong></td>
                <td class="tabular-nums">${escapeHtml(a.dateOfBirth || '—')}</td>
                <td>${escapeHtml(formatGender(a.gender))}</td>
                <td class="tabular-nums">${escapeHtml(cccdDisplay)}</td>
                <td>${contactDisplay}</td>
                <td>${statusBadgeHtml(hStatus)}</td>
                ${canEdit ? `
                  <td>
                    <div class="page-actions">
                      ${canEditRow ? `<button class="btn btn-secondary btn-sm" onclick="openEditAthleteModal(${a.id})">Sửa</button>` : ''}
                      ${canEditRow ? `<button class="btn btn-secondary btn-sm" onclick="patchAthleteHealth(${a.id}, '${nextHealth}', this)">Đổi thể lực</button>` : ''}
                      ${canDelete ? `<button class="btn btn-danger btn-sm" data-name="${escapeHtml(a.fullName)}" onclick="confirmDeleteAthlete(${a.id}, this.dataset.name)">Xóa</button>` : ''}
                    </div>
                  </td>
                ` : ''}
              </tr>
            `;
          }).join('') || `<tr><td colspan="${canEdit ? 8 : 7}" class="state-empty-box">${slice.loading ? 'Đang tải danh sách vận động viên...' : 'Không tìm thấy vận động viên nào.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('athletes', slice, 'vận động viên', [10, 20, 50, 100])}
  `;
}

function updateAthletesRegion() {
  const el = document.getElementById('athletesTableRegion');
  if (el) el.innerHTML = renderAthletesTableInner();
}

/* ---------- PAGE: LỊCH THI ĐẤU (BACKEND PAGED + EDIT + CONFLICT CHECK) ---------- */
function renderSchedulePage() {
  const slice = AppState.paged.matches;
  const canSchedule = hasPermission('matches.schedule');

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Lịch thi đấu</h1>
        <p class="page-subtitle">Tra cứu lịch thi đấu, tự động xếp lịch, chỉnh sửa lịch và kiểm tra xung đột sân/trọng tài/đội.</p>
      </div>
      <div class="page-actions">
        ${canSchedule ? `<button class="btn btn-secondary" onclick="openAutoGenerateScheduleModal()">⚡ Tự động xếp lịch</button>` : ''}
        ${canSchedule ? `<button class="btn btn-primary" onclick="openCreateMatchModal()">+ Tạo trận đấu</button>` : ''}
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <input id="scheduleFilterKeyword" class="input" value="${escapeHtml(slice.filters.keyword || '')}" placeholder="Tìm đội tuyển, vòng đấu, sân, trọng tài..." oninput="applyPagedFilter('matches', { keyword: this.value }, true)" />
          <select class="select" onchange="applyPagedFilter('matches', { tournamentId: this.value })" aria-label="Lọc theo giải đấu">
            <option value="" ${!slice.filters.tournamentId ? 'selected' : ''}>Tất cả giải đấu</option>
            ${AppState.data.tournaments.map(t => `<option value="${t.id}" ${String(slice.filters.tournamentId) === String(t.id) ? 'selected' : ''}>${escapeHtml(t.name)}</option>`).join('')}
          </select>
          <select class="select" onchange="applyPagedFilter('matches', { status: this.value })" aria-label="Lọc theo trạng thái trận">
            <option value="" ${!slice.filters.status ? 'selected' : ''}>Tất cả trạng thái</option>
            <option value="SCHEDULED" ${slice.filters.status === 'SCHEDULED' ? 'selected' : ''}>Chờ thi đấu</option>
            <option value="ONGOING" ${slice.filters.status === 'ONGOING' ? 'selected' : ''}>Đang diễn ra</option>
            <option value="FINISHED" ${slice.filters.status === 'FINISHED' ? 'selected' : ''}>Đã kết thúc</option>
            <option value="CANCELLED" ${slice.filters.status === 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
          </select>
        </div>
        <span class="badge badge-primary" id="matchesTotalBadge">Tổng số: ${slice.totalElements} trận đấu</span>
      </div>

      <div id="scheduleTableRegion">
        ${renderScheduleTableInner()}
      </div>
    </div>
  `;
}

function renderScheduleTableInner() {
  const slice = AppState.paged.matches;
  const canSchedule = hasPermission('matches.schedule');
  const canSubmitResult = hasPermission('results.submit_assigned');
  const canDelete = hasPermission('tournaments.manage');
  const startIdx = slice.page * slice.size;

  const badge = document.getElementById('matchesTotalBadge');
  if (badge) badge.textContent = `Tổng số: ${slice.totalElements} trận đấu`;

  return `
    ${renderServiceStateBanner('matches', "fetchPagedResource('matches', updateScheduleRegion)")}
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>STT</th><th>Vòng đấu</th><th>Cặp đấu</th><th>Thời gian</th><th>Sân thi đấu</th><th>Trọng tài</th><th>Trạng thái</th><th>Thao tác</th></tr>
        </thead>
        <tbody>
          ${slice.items.map((m, idx) => {
            const matchTitle = `${m.participant1 || 'Đội 1'} vs ${m.participant2 || 'Đội 2'}`;
            return `
              <tr>
                <td class="tabular-nums"><strong>#${String(startIdx + idx + 1).padStart(2, '0')}</strong></td>
                <td><strong>${escapeHtml(m.round || 'Vòng đấu')}</strong></td>
                <td><strong>${escapeHtml(m.participant1 || '—')}</strong> <span style="color:var(--text-secondary)">vs</span> <strong>${escapeHtml(m.participant2 || '—')}</strong></td>
                <td class="tabular-nums">${escapeHtml(String(m.matchTime || '').replace('T', ' ') || 'Chưa ấn định')}</td>
                <td>${escapeHtml(m.location || 'Chưa cập nhật')}</td>
                <td>${escapeHtml(m.referee || 'Chưa phân công')}</td>
                <td>${statusBadgeHtml(m.status)}</td>
                <td>
                  <div class="page-actions">
                    <button class="btn btn-secondary btn-sm" onclick="viewMatchDetails(${m.id})">Chi tiết</button>
                    ${canSchedule ? `<button class="btn btn-secondary btn-sm" onclick="openEditMatchModal(${m.id})">Sửa</button>` : ''}
                    ${canSubmitResult ? `<button class="btn btn-primary btn-sm" onclick="openRecordResultModal(${m.id})">Nhập KQ</button>` : ''}
                    ${canDelete ? `<button class="btn btn-danger btn-sm" data-name="${escapeHtml(matchTitle)}" onclick="confirmDeleteMatch(${m.id}, this.dataset.name)">Xóa</button>` : ''}
                  </div>
                </td>
              </tr>
            `;
          }).join('') || `<tr><td colspan="8" class="state-empty-box">${slice.loading ? 'Đang tải lịch thi đấu...' : 'Không tìm thấy trận đấu nào.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('matches', slice, 'trận đấu', [5, 10, 20, 50])}
  `;
}

function updateScheduleRegion() {
  const el = document.getElementById('scheduleTableRegion');
  if (el) el.innerHTML = renderScheduleTableInner();
}

/* ---------- PAGE: CHI TIẾT TRẬN ĐẤU (100% REAL DATA) ---------- */
function viewMatchDetails(matchId) {
  if (!matchId) return;
  AppState.selectedMatchId = Number(matchId);
  navigateTo('match-detail');
}

function renderMatchDetailPage() {
  const detail = AppState.detail.match;
  if (detail.loading && !detail.item) {
    return `
      <div class="page-header"><div><h1 class="page-title">Chi tiết Trận đấu</h1></div></div>
      <div class="state-banner loading" role="status"><span>Đang tải chi tiết trận đấu theo ID...</span></div>
    `;
  }
  if (detail.error && !detail.item) {
    return `
      <div class="page-header"><div><h1 class="page-title">Chi tiết Trận đấu</h1></div></div>
      <div class="state-banner error" role="alert">
        <span>${escapeHtml(detail.error)}</span>
        <button type="button" class="btn btn-secondary btn-sm" onclick="loadMatchDetail(AppState.selectedMatchId)">↻ Thử lại</button>
      </div>
    `;
  }

  const match = (detail.item && Number(detail.item.id) === Number(AppState.selectedMatchId || detail.id))
    ? detail.item
    : null;
  if (!match) {
    return `
      <div class="page-header">
        <div>
          <h1 class="page-title">Chi tiết Trận đấu</h1>
        </div>
      </div>
      ${renderServiceStateBanner(['matches', 'results'])}
      <div class="card state-empty-box">Chưa có trận đấu nào trong hệ thống để hiển thị chi tiết.</div>
    `;
  }

  // Strictly match result to current matchId so stale data from another match can NEVER bleed through
  const candidateRes = detail.result || AppState.caches.resultsByMatchId[String(match.id)] || null;
  const res = (candidateRes && Number(candidateRes.matchId) === Number(match.id)) ? candidateRes : null;

  const canSchedule = hasPermission('matches.schedule');
  const canSubmit = hasPermission('results.submit_assigned');
  const canApprove = hasPermission('results.approve');
  const isAssignedRef = AppState.user?.username && match.refereeUsername
    && AppState.user.username.toLowerCase() === match.refereeUsername.toLowerCase();
  const isConfirmed = res && String(res.status || '').toUpperCase() === 'CONFIRMED';
  const needsSyncRetry = isConfirmed && String(res.syncStatus || 'SYNCED').toUpperCase() !== 'SYNCED';

  return `
    ${renderServiceStateBanner(['matches'], `loadMatchDetail(${match.id})`)}

    ${detail.resultError ? `
      <div class="state-banner error" role="alert">
        <div>
          <strong>Dịch vụ Kết quả gặp sự cố khi tải trận #${match.id}:</strong>
          <span>${escapeHtml(detail.resultError)}</span>
          ${res && detail.resultStale ? `<div style="margin-top:4px;font-size:0.82rem;opacity:0.95;">Đang hiển thị tỷ số đã tải trước đó của đúng trận #${match.id} (có thể chưa phải mới nhất).</div>` : ''}
        </div>
        <button type="button" class="btn btn-secondary btn-sm" onclick="loadMatchDetail(${match.id})">↻ Thử lại</button>
      </div>
    ` : ''}

    <div class="page-header">
      <div>
        <h1 class="page-title">Trận #${match.id} — ${escapeHtml(match.round || 'Vòng đấu')}</h1>
        <p class="page-subtitle">
          Sân: ${escapeHtml(match.location || 'Chưa cập nhật')} •
          Trọng tài: ${escapeHtml(match.referee || 'Chưa phân công')}${match.refereeUsername ? ` (@${escapeHtml(match.refereeUsername)})` : ''} •
          Thời gian: ${escapeHtml(String(match.matchTime || '').replace('T', ' ') || 'Chưa ấn định')}
        </p>
      </div>
      <div class="page-actions">
        ${canSchedule ? `<button class="btn btn-secondary" onclick="openEditMatchModal(${match.id})">Sửa lịch trận</button>` : ''}
        ${isAssignedRef && (match.status === 'SCHEDULED' || match.status === 'POSTPONED') ? `<button class="btn btn-secondary" onclick="patchMatchStatusOnly(${match.id}, 'ONGOING', this)">▶ Bắt đầu trận</button>` : ''}
        ${canSubmit ? `<button class="btn btn-primary" onclick="openRecordResultModal(${match.id})">Nhập / Cập nhật tỷ số</button>` : ''}
        ${canApprove && needsSyncRetry ? `<button class="btn btn-warning" onclick="retrySyncResultNow(${res.id}, ${match.id}, this)">↻ Đồng bộ lại Trận & BXH</button>` : ''}
      </div>
    </div>

    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <span class="badge badge-primary">${escapeHtml(match.round || 'Vòng đấu')}</span>
        ${statusBadgeHtml(match.status)}
      </div>
      <div class="scoreboard-row">
        <div class="team-side">
          <span class="team-crest-circle">${escapeHtml(formatTeamCrestInitials(match.participant1))}</span>
          <h2 style="font-size:1.3rem">${escapeHtml(match.participant1 || 'Chưa xác định')}</h2>
        </div>
        <div class="score-pill tabular-nums">
          ${res ? `${res.scoreParticipant1 ?? '-'} — ${res.scoreParticipant2 ?? '-'}` : '- — -'}
        </div>
        <div class="team-side right">
          <span class="team-crest-circle">${escapeHtml(formatTeamCrestInitials(match.participant2))}</span>
          <h2 style="font-size:1.3rem">${escapeHtml(match.participant2 || 'Chưa xác định')}</h2>
        </div>
      </div>

      ${res ? `
        <div style="text-align:center;color:var(--text-secondary);font-size:0.88rem;margin-top:14px;border-top:1px solid var(--border-color);padding-top:14px;">
          <div style="display:flex;justify-content:center;align-items:center;gap:8px;flex-wrap:wrap;">
            <span>Trạng thái kết quả: ${statusBadgeHtml(res.status)}</span>
            ${syncStatusBadgeHtml(res)}
            <span>• Người xác nhận: <strong>${escapeHtml(res.confirmedBy || 'Chưa xác nhận')}</strong></span>
          </div>
          ${res.syncError ? `<div style="margin-top:6px;color:var(--color-error);">Lỗi đồng bộ: ${escapeHtml(res.syncError)}</div>` : ''}
          <div style="margin-top:6px;">Ghi chú: <strong>${escapeHtml(res.notes || 'Không có ghi chú')}</strong></div>
        </div>
      ` : `
        <div class="state-empty-box" style="margin-top:14px;border-top:1px solid var(--border-color);">
          ${detail.resultError
            ? 'Không thể hiển thị tỷ số do dịch vụ Kết quả đang lỗi. Vui lòng nhấn "Thử lại".'
            : 'Trận đấu này chưa có dữ liệu tỷ số / kết quả được ghi nhận.'}
        </div>
      `}
    </div>
  `;
}

/* ---------- PAGE: QUẢN LÝ KẾT QUẢ (BACKEND PAGED) ---------- */
function renderResultsPage() {
  const slice = AppState.paged.results;
  const canSubmit = hasPermission('results.submit_assigned');

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Quản lý Kết quả</h1>
        <p class="page-subtitle">Ghi nhận tỷ số trận đấu, chỉnh sửa bản nháp chờ duyệt và xác nhận để tự động tính bảng xếp hạng.</p>
      </div>
      <div class="page-actions">
        ${canSubmit ? `<button class="btn btn-primary" onclick="openRecordResultModal()">+ Nhập kết quả</button>` : ''}
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <input id="resultFilterWinner" class="input" value="${escapeHtml(slice.filters.winner || '')}" placeholder="Lọc theo đội thắng / từ khóa..." oninput="applyPagedFilter('results', { winner: this.value }, true)" />
          <select class="select" onchange="applyPagedFilter('results', { status: this.value })" aria-label="Lọc trạng thái kết quả">
            <option value="" ${!slice.filters.status ? 'selected' : ''}>Tất cả trạng thái</option>
            <option value="PENDING_CONFIRMATION" ${slice.filters.status === 'PENDING_CONFIRMATION' ? 'selected' : ''}>Chờ xác nhận</option>
            <option value="CONFIRMED" ${slice.filters.status === 'CONFIRMED' ? 'selected' : ''}>Đã xác nhận</option>
          </select>
        </div>
        <span class="badge badge-primary" id="resultsTotalBadge">Tổng số: ${slice.totalElements} kết quả</span>
      </div>

      <div id="resultsTableRegion">
        ${renderResultsTableInner()}
      </div>
    </div>
  `;
}

function renderResultsTableInner() {
  const slice = AppState.paged.results;
  const canSubmit = hasPermission('results.submit_assigned');
  const canApprove = hasPermission('results.approve');
  const startIdx = slice.page * slice.size;

  const badge = document.getElementById('resultsTotalBadge');
  if (badge) badge.textContent = `Tổng số: ${slice.totalElements} kết quả`;

  return `
    ${renderServiceStateBanner('results', "fetchPagedResource('results', updateResultsRegion)")}
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>STT</th><th>Trận đấu</th><th>Vòng đấu</th><th>Tỷ số</th><th>Ghi chú</th><th>Người xác nhận</th><th>Trạng thái</th><th>Thao tác</th></tr>
        </thead>
        <tbody>
          ${slice.items.map((r, idx) => {
            const m = AppState.data.matches.find(x => Number(x.id) === Number(r.matchId)) || {};
            const matchTitle = m.participant1 ? `${m.participant1} vs ${m.participant2}` : `Trận #${r.matchId}`;
            const isConfirmed = String(r.status || '').toUpperCase() === 'CONFIRMED';
            return `
              <tr>
                <td class="tabular-nums"><strong>#${String(startIdx + idx + 1).padStart(2, '0')}</strong></td>
                <td><strong>${escapeHtml(matchTitle)}</strong></td>
                <td><span class="badge badge-neutral">${escapeHtml(m.round || '—')}</span></td>
                <td class="tabular-nums">
                  <strong style="font-size:1.02rem;color:var(--color-primary)">${r.scoreParticipant1} - ${r.scoreParticipant2}</strong>
                  ${r.penaltyScore1 !== null && r.penaltyScore1 !== undefined && r.penaltyScore2 !== null && r.penaltyScore2 !== undefined
                    ? `<div style="font-size:0.75rem;color:var(--text-secondary);">Luân lưu: ${r.penaltyScore1} - ${r.penaltyScore2}</div>` : ''}
                </td>
                <td>${escapeHtml(r.notes || '—')}</td>
                <td>${escapeHtml(r.confirmedBy || 'Chưa xác nhận')}</td>
                <td>
                  <div>${statusBadgeHtml(r.status)}</div>
                  ${isConfirmed ? `<div style="margin-top:4px;">${syncStatusBadgeHtml(r)}</div>` : ''}
                  ${isConfirmed && r.syncError ? `<div style="font-size:0.74rem;color:var(--color-error);margin-top:2px;max-width:220px;">${escapeHtml(r.syncError)}</div>` : ''}
                </td>
                <td>
                  <div class="page-actions">
                    ${canSubmit && !isConfirmed ? `<button class="btn btn-secondary btn-sm" onclick="openRecordResultModal(${r.matchId})">Sửa</button>` : ''}
                    ${canApprove && !isConfirmed ? `<button class="btn btn-primary btn-sm" onclick="confirmMatchResult(${r.id}, this)">✓ Xác nhận</button>` : ''}
                    ${isConfirmed && String(r.syncStatus || 'SYNCED').toUpperCase() === 'SYNCED' ? `<span class="badge badge-success">Hoàn tất</span>` : ''}
                    ${canApprove && isConfirmed ? `<button class="btn btn-secondary btn-sm" onclick="reopenResultById(${r.id}, ${r.matchId}, this)">Mở lại</button>` : ''}
                    ${isConfirmed && String(r.syncStatus || 'SYNCED').toUpperCase() !== 'SYNCED' && canApprove ? `<button class="btn btn-secondary btn-sm" onclick="retrySyncResultNow(${r.id}, ${r.matchId}, this)">↻ Đồng bộ lại</button>` : ''}
                  </div>
                </td>
              </tr>
            `;
          }).join('') || `<tr><td colspan="8" class="state-empty-box">${slice.loading ? 'Đang tải danh sách kết quả...' : 'Chưa có kết quả nào được ghi nhận.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('results', slice, 'kết quả', [5, 10, 20, 50])}
  `;
}

function updateResultsRegion() {
  const el = document.getElementById('resultsTableRegion');
  if (el) el.innerHTML = renderResultsTableInner();
}

/* ---------- PAGE: BẢNG XẾP HẠNG (BACKEND PAGED: BOARDS & RANKING ROWS) ---------- */
function renderRankingsPage() {
  const d = AppState.data;
  const currentSport = AppState.selectedSportId || 'ALL';
  const boardSlice = AppState.paged.rankings;
  const itemSlice = AppState.paged.rankingItems;
  const canRecalculate = hasPermission('results.approve') || hasPermission('tournaments.manage');
  const canExport = hasPermission('rankings.read_public');

  const filteredComps = currentSport === 'ALL'
    ? d.competitions
    : d.competitions.filter(c => String(c.sportId) === String(currentSport));

  const selectedCid = AppState.selectedCompetitionId || filteredComps[0]?.id || d.competitions[0]?.id || null;

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Bảng xếp hạng</h1>
        <p class="page-subtitle">Phân trang danh sách bảng xếp hạng theo nội dung và phân trang chi tiết các dòng thứ hạng từ kết quả đã xác nhận.</p>
      </div>
      <div class="page-actions">
        ${canExport ? `<button class="btn btn-secondary" onclick="exportStandingsReportCSV()">⬇ Xuất CSV</button>` : ''}
        ${canRecalculate && d.competitions.length > 0 ? `<button class="btn btn-primary" onclick="recalculateStandingsNow(this)">↻ Tính lại BXH</button>` : ''}
      </div>
    </div>

    ${renderServiceStateBanner(['rankings', 'rankingItems', 'competitions', 'sports'])}

    <div class="card" style="margin-bottom:20px;">
      <div class="card-header" style="flex-wrap:wrap;gap:10px;">
        <div>
          <h2 class="card-title">Danh sách Bảng xếp hạng (Phân trang theo BXH)</h2>
          <p style="font-size:0.82rem;color:var(--text-secondary);margin:0;">Chọn một bảng xếp hạng để xem và tìm kiếm từng dòng đội tuyển / VĐV bên dưới</p>
        </div>
        <span class="badge badge-primary" id="rankingsBoardsTotalBadge">Tổng số: ${boardSlice.totalElements} bảng xếp hạng</span>
      </div>

      <div class="toolbar">
        <div class="toolbar-filters">
          <input id="rankBoardKeyword" class="input" value="${escapeHtml(boardSlice.filters.keyword || '')}"
            placeholder="Tìm tên bảng xếp hạng..."
            oninput="applyPagedFilter('rankings', { keyword: this.value }, true)" />
          <div style="display:flex;flex-wrap:wrap;gap:6px;">
            <button type="button" class="btn ${currentSport === 'ALL' ? 'btn-primary' : 'btn-secondary'} btn-sm"
              onclick="filterRankingsBySport('ALL')">
              Tất cả môn (${d.sports.length})
            </button>
            ${d.sports.map(sp => `
              <button type="button" class="btn ${String(currentSport) === String(sp.id) ? 'btn-primary' : 'btn-secondary'} btn-sm"
                onclick="filterRankingsBySport('${sp.id}')">
                ${escapeHtml(sp.name)}
              </button>
            `).join('')}
          </div>
        </div>
      </div>

      <div id="rankingsBoardsRegion">
        ${renderRankingsBoardsInner(selectedCid)}
      </div>
    </div>

    <div class="card">
      <div class="card-header" style="flex-wrap:wrap;gap:10px;">
        <div>
          <h2 class="card-title" id="rankingItemsCardTitle">Các dòng Thứ hạng Đội / VĐV trong Bảng đang chọn</h2>
          <p style="font-size:0.82rem;color:var(--text-secondary);margin:0;">Bộ lọc tìm kiếm và phân trang áp dụng trực tiếp trên toàn bộ các dòng xếp hạng ở backend</p>
        </div>
        <div class="page-actions">
          ${d.competitions.length > 0 ? `
            <select id="rankingCompSelect" class="select" style="padding:5px 10px;font-size:0.82rem;" onchange="selectRankingCompetition(this.value)" aria-label="Chọn nội dung xem thứ hạng">
              ${(filteredComps.length > 0 ? filteredComps : d.competitions).map(c => `<option value="${c.id}" ${Number(c.id) === Number(selectedCid) ? 'selected' : ''}>${escapeHtml(c.name || c.competitionName)}</option>`).join('')}
            </select>
          ` : ''}
          <span class="badge badge-primary" id="rankingItemsTotalBadge">Tổng số: ${itemSlice.totalElements} dòng xếp hạng</span>
        </div>
      </div>

      <div class="toolbar">
        <div class="toolbar-filters">
          <input id="rankRowSearchInput" class="input" type="search"
            placeholder="Tìm tên đội tuyển / VĐV trong bảng xếp hạng đang chọn..."
            value="${escapeHtml(itemSlice.filters.keyword || '')}"
            oninput="applyPagedFilter('rankingItems', { keyword: this.value }, true)" />
        </div>
      </div>

      <div id="rankingItemsRegion">
        ${renderRankingItemsInner(selectedCid)}
      </div>
    </div>
  `;
}

function filterRankingsBySport(sportId) {
  AppState.selectedSportId = String(sportId || 'ALL');
  const comps = AppState.selectedSportId === 'ALL'
    ? AppState.data.competitions
    : AppState.data.competitions.filter(c => String(c.sportId) === AppState.selectedSportId);
  if (comps.length > 0 && !comps.some(c => Number(c.id) === Number(AppState.selectedCompetitionId))) {
    AppState.selectedCompetitionId = comps[0].id;
    AppState.paged.rankingItems.filters.competitionId = String(comps[0].id);
    AppState.paged.rankingItems.page = 0;
  }
  renderCurrentPage(false);
  fetchPagedResource('rankingItems', updateRankingItemsRegion);
}

function selectRankingCompetition(competitionId) {
  const cid = Number(competitionId);
  if (!cid) return;
  AppState.selectedCompetitionId = cid;
  AppState.paged.rankingItems.filters.competitionId = String(cid);
  AppState.paged.rankingItems.page = 0;
  updateRankingsBoardsRegion();
  fetchPagedResource('rankingItems', updateRankingItemsRegion);
}

function renderRankingsBoardsInner(selectedCid = null) {
  const slice = AppState.paged.rankings;
  const activeCid = Number(selectedCid || AppState.selectedCompetitionId || 0);
  const startIdx = slice.page * slice.size;

  const badge = document.getElementById('rankingsBoardsTotalBadge');
  if (badge) badge.textContent = `Tổng số: ${slice.totalElements} bảng xếp hạng`;

  return `
    ${renderServiceStateBanner('rankings', "fetchPagedResource('rankings', updateRankingsBoardsRegion)")}
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>STT</th><th>Tên bảng xếp hạng</th><th>Nội dung / Giải đấu</th><th>Cập nhật</th><th>Thao tác</th></tr>
        </thead>
        <tbody>
          ${slice.items.map((rk, idx) => {
            const { comp, tour, sport } = getCompetitionMeta(rk.competitionId);
            const isSelected = Number(rk.competitionId) === activeCid;
            const compText = comp ? (comp.name || comp.competitionName) : `Nội dung #${rk.competitionId}`;
            const tourText = tour ? tour.name : '';
            return `
              <tr style="${isSelected ? 'background:var(--bg-surface-secondary);' : ''}">
                <td class="tabular-nums"><strong>#${String(startIdx + idx + 1).padStart(2, '0')}</strong></td>
                <td>
                  <strong>${escapeHtml(rk.rankingName || compText)}</strong>
                  ${sport ? `<span class="badge badge-primary" style="margin-left:6px;">${escapeHtml(sport.name)}</span>` : ''}
                </td>
                <td>${escapeHtml(compText)}${tourText ? ` • ${escapeHtml(tourText)}` : ''}</td>
                <td class="tabular-nums">${rk.updatedAt ? escapeHtml(String(rk.updatedAt).replace('T', ' ').slice(0, 19)) : '—'}</td>
                <td>
                  <button type="button" class="btn ${isSelected ? 'btn-primary' : 'btn-secondary'} btn-sm" onclick="selectRankingCompetition(${rk.competitionId})">
                    ${isSelected ? 'Đang xem' : 'Xem thứ hạng'}
                  </button>
                </td>
              </tr>
            `;
          }).join('') || `<tr><td colspan="5" class="state-empty-box">${slice.loading ? 'Đang tải danh sách bảng xếp hạng...' : 'Chưa có bảng xếp hạng nào trong hệ thống.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('rankings', slice, 'bảng xếp hạng', [3, 5, 10, 20])}
  `;
}

function updateRankingsBoardsRegion() {
  const el = document.getElementById('rankingsBoardsRegion');
  if (el) el.innerHTML = renderRankingsBoardsInner(AppState.selectedCompetitionId);
}

function renderRankingItemsInner(selectedCid = null) {
  const slice = AppState.paged.rankingItems;
  const cid = Number(selectedCid || slice.filters.competitionId || AppState.selectedCompetitionId || AppState.data.competitions[0]?.id || 0);
  const { comp, tour, sport } = getCompetitionMeta(cid);

  const badge = document.getElementById('rankingItemsTotalBadge');
  if (badge) badge.textContent = `Tổng số: ${slice.totalElements} dòng xếp hạng`;

  const titleEl = document.getElementById('rankingItemsCardTitle');
  if (titleEl && comp) {
    titleEl.textContent = `Thứ hạng Đội / VĐV — ${comp.name || comp.competitionName}`;
  }

  return `
    ${renderServiceStateBanner('rankingItems', "fetchPagedResource('rankingItems', updateRankingItemsRegion)")}
    ${comp ? `
      <div style="margin-bottom:10px;font-size:0.84rem;color:var(--text-secondary);">
        Nội dung: <strong>${escapeHtml(comp.name || comp.competitionName)}</strong>
        ${sport ? ` • Môn: <strong>${escapeHtml(sport.name)}</strong>` : ''}
        ${tour ? ` • Giải đấu: <strong>${escapeHtml(tour.name)}</strong>` : ''}
      </div>
    ` : ''}
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>Hạng</th>
            <th>Đội tuyển / Vận động viên</th>
            <th>Trận</th>
            <th>Thắng</th>
            <th>Hòa</th>
            <th>Thua</th>
            <th>Điểm/Bàn ghi</th>
            <th>Điểm/Bàn thua</th>
            <th>Hiệu số</th>
            <th>Điểm số</th>
          </tr>
        </thead>
        <tbody>
          ${slice.items.map((item, idx) => {
            const rankPos = item.rankPosition ?? (slice.page * slice.size + idx + 1);
            const rankBadge = rankPos === 1 ? 'badge-success' : (rankPos === 2 ? 'badge-warning' : (rankPos === 3 ? 'badge-primary' : 'badge-neutral'));
            const diff = item.goalDifference ?? ((item.goalsFor ?? 0) - (item.goalsAgainst ?? 0));
            return `
              <tr>
                <td class="tabular-nums"><span class="badge ${rankBadge}">#${rankPos}</span></td>
                <td><strong>${escapeHtml(item.participantName)}</strong></td>
                <td class="tabular-nums">${item.matchesPlayed ?? 0}</td>
                <td class="tabular-nums">${item.won ?? 0}</td>
                <td class="tabular-nums">${item.drawn ?? 0}</td>
                <td class="tabular-nums">${item.lost ?? 0}</td>
                <td class="tabular-nums">${item.goalsFor ?? 0}</td>
                <td class="tabular-nums">${item.goalsAgainst ?? 0}</td>
                <td class="tabular-nums">${diff > 0 ? '+' + diff : diff}</td>
                <td class="tabular-nums"><strong style="font-size:1.05rem;color:var(--color-primary)">${item.points ?? 0}</strong></td>
              </tr>
            `;
          }).join('') || `<tr><td colspan="10" class="state-empty-box">${slice.loading ? 'Đang tải các dòng xếp hạng...' : 'Chưa có dữ liệu thứ hạng cho nội dung thi đấu này.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('rankingItems', slice, 'dòng xếp hạng', [5, 10, 20, 50])}
  `;
}

function updateRankingItemsRegion() {
  const el = document.getElementById('rankingItemsRegion');
  if (el) el.innerHTML = renderRankingItemsInner(AppState.selectedCompetitionId);
}

/* ---------- PAGE: BÁO CÁO & THÔNG BÁO ---------- */
function renderReportsPage() {
  const canExportAll = hasPermission('reports.read_all');
  const d = AppState.data;
  const selectedCid = AppState.reportChartCompetitionId || d.competitions[0]?.id || null;
  const slice = AppState.paged.notifications;

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Thống kê & Báo cáo</h1>
        <p class="page-subtitle">Phân tích hiệu suất thi đấu theo nội dung, xuất dữ liệu CSV/JSON và theo dõi thông báo hệ thống.</p>
      </div>
      <div class="page-actions">
        <button class="btn btn-secondary" onclick="exportStandingsReportCSV()">⬇ Xuất BXH (.CSV)</button>
        ${canExportAll ? `<button id="btnExportSystemJson" class="btn btn-primary" ${AppState.exportingSystemReport ? 'disabled' : ''} onclick="exportFullSystemReportJSON(this)">${AppState.exportingSystemReport ? 'Đang xuất báo cáo...' : '⬇ Xuất dữ liệu (.JSON)'}</button>` : ''}
      </div>
    </div>

    <div id="systemExportBanner"></div>

    <div class="grid-2">
      <div class="card">
        <div class="card-header" style="flex-wrap:wrap;gap:10px;">
          <h2 class="card-title">Hiệu suất Ghi điểm / Thủng lưới</h2>
          ${d.competitions.length > 0 ? `
            <select class="select" style="width:auto;padding:4px 10px;font-size:0.8rem;" onchange="AppState.reportChartCompetitionId=Number(this.value);initReportCharts(this.value)" aria-label="Chọn nội dung báo cáo">
              ${d.competitions.map(c => `<option value="${c.id}" ${Number(c.id) === Number(selectedCid) ? 'selected' : ''}>${escapeHtml(c.name || c.competitionName)}</option>`).join('')}
            </select>
          ` : ''}
        </div>
        <div class="chart-box" id="reportGoalsChartBox"><canvas id="reportGoalsChart"></canvas></div>
      </div>

      <div class="card">
        <div class="card-header" style="flex-wrap:wrap;gap:10px;">
          <h2 class="card-title">Thông báo Hệ thống</h2>
          <div class="toolbar-filters">
            <select class="select" style="padding:4px 8px;font-size:0.8rem;" onchange="applyPagedFilter('notifications', { read: this.value })" aria-label="Lọc trạng thái đọc">
              <option value="" ${slice.filters.read === '' ? 'selected' : ''}>Tất cả</option>
              <option value="false" ${slice.filters.read === 'false' ? 'selected' : ''}>Chưa đọc</option>
              <option value="true" ${slice.filters.read === 'true' ? 'selected' : ''}>Đã đọc</option>
            </select>
          </div>
        </div>
        <div id="notificationsTableRegion">
          ${renderNotificationsTableInner()}
        </div>
      </div>
    </div>
  `;
}

function renderNotificationsTableInner() {
  const slice = AppState.paged.notifications;
  const startIdx = slice.page * slice.size;

  return `
    ${renderServiceStateBanner('notifications', "fetchPagedResource('notifications', updateNotificationsRegion)")}
    <div class="table-wrap">
      <table class="data-table">
        <thead><tr><th>STT</th><th>Loại</th><th>Nội dung</th><th>Trạng thái</th></tr></thead>
        <tbody>
          ${slice.items.map((n, idx) => `
            <tr>
              <td class="tabular-nums"><strong>#${String(startIdx + idx + 1).padStart(2, '0')}</strong></td>
              <td><span class="badge badge-primary">${escapeHtml(formatEventType(n.eventType))}</span></td>
              <td>${escapeHtml(n.message || n.title || '')}</td>
              <td>${n.isRead ? '<span class="badge badge-neutral">Đã đọc</span>' : (AppState.user ? `<button class="btn btn-secondary btn-sm" onclick="markNotificationRead(${n.id}, this)">Đánh dấu đọc</button>` : '<span class="badge badge-warning">Chưa đọc</span>')}</td>
            </tr>
          `).join('') || `<tr><td colspan="4" class="state-empty-box">${slice.loading ? 'Đang tải thông báo...' : 'Chưa có thông báo nào.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('notifications', slice, 'thông báo', [5, 8, 15])}
  `;
}

function updateNotificationsRegion() {
  const el = document.getElementById('notificationsTableRegion');
  if (el) el.innerHTML = renderNotificationsTableInner();
}

function initReportCharts(selectedCid = null) {
  if (!window.Chart) return;
  const styles = getComputedStyle(document.documentElement);
  const textColor = styles.getPropertyValue('--chart-text').trim() || '#64748B';
  const gridColor = styles.getPropertyValue('--chart-grid').trim() || '#E2E8F0';
  const successColor = styles.getPropertyValue('--color-success').trim() || '#10B981';
  const errorColor = styles.getPropertyValue('--color-error').trim() || '#EF4444';

  const cid = Number(selectedCid || AppState.reportChartCompetitionId || AppState.data.competitions[0]?.id || 0);
  const rkEntry = (AppState.data.rankings || []).find(rk => Number(rk?.ranking?.competitionId) === cid);
  const items = Array.isArray(rkEntry?.items) ? rkEntry.items.slice(0, 8) : [];

  const box = document.getElementById('reportGoalsChartBox');
  if (!box) return;
  if (AppState.charts.c3) { AppState.charts.c3.destroy(); AppState.charts.c3 = null; }

  if (items.length === 0) {
    box.innerHTML = `<div class="state-empty-box" style="padding-top:95px;">Chưa có dữ liệu xếp hạng cho nội dung này để vẽ biểu đồ.</div>`;
    return;
  }

  box.innerHTML = '<canvas id="reportGoalsChart"></canvas>';
  AppState.charts.c3 = new Chart(document.getElementById('reportGoalsChart'), {
    type: 'bar',
    data: {
      labels: items.map(i => i.participantName),
      datasets: [
        { label: 'Điểm / Bàn ghi', data: items.map(i => i.goalsFor ?? 0), backgroundColor: successColor, borderRadius: 6 },
        { label: 'Điểm / Bàn thua', data: items.map(i => i.goalsAgainst ?? 0), backgroundColor: errorColor, borderRadius: 6 }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { labels: { color: textColor } } },
      scales: {
        x: { ticks: { color: textColor }, grid: { color: gridColor } },
        y: { beginAtZero: true, ticks: { color: textColor }, grid: { color: gridColor } }
      }
    }
  });
}

/* ---------- PAGE: QUẢN LÝ NGƯỜI DÙNG (BACKEND PAGED) ---------- */
function renderUsersPage() {
  const slice = AppState.paged.users;
  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Quản lý Người dùng</h1>
        <p class="page-subtitle">Phân quyền vai trò người dùng, khóa/mở tài khoản và đổi mật khẩu.</p>
      </div>
      <div class="page-actions">
        <button class="btn btn-secondary" onclick="openChangePasswordModal()">Đổi mật khẩu</button>
        <button class="btn btn-primary" onclick="navigateTo('auth-page')">+ Tạo tài khoản</button>
      </div>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <input id="userFilterKeyword" class="input" value="${escapeHtml(slice.filters.keyword || '')}" placeholder="Tìm tên đăng nhập, họ tên, email..." oninput="applyPagedFilter('users', { keyword: this.value }, true)" />
          <select class="select" onchange="applyPagedFilter('users', { role: this.value })" aria-label="Lọc vai trò">
            <option value="" ${!slice.filters.role ? 'selected' : ''}>Tất cả vai trò</option>
            <option value="ROLE_ADMIN" ${slice.filters.role === 'ROLE_ADMIN' ? 'selected' : ''}>Quản trị viên</option>
            <option value="ROLE_ORGANIZER" ${slice.filters.role === 'ROLE_ORGANIZER' ? 'selected' : ''}>Ban tổ chức</option>
            <option value="ROLE_REFEREE" ${slice.filters.role === 'ROLE_REFEREE' ? 'selected' : ''}>Trọng tài</option>
            <option value="ROLE_TEAM_MANAGER" ${slice.filters.role === 'ROLE_TEAM_MANAGER' ? 'selected' : ''}>Lãnh đội / HLV</option>
            <option value="ROLE_VIEWER" ${slice.filters.role === 'ROLE_VIEWER' ? 'selected' : ''}>Khán giả</option>
          </select>
        </div>
        <span class="badge badge-primary" id="usersTotalBadge">Tổng số: ${slice.totalElements} tài khoản</span>
      </div>

      <div id="usersTableRegion">
        ${renderUsersTableInner()}
      </div>
    </div>
  `;
}

function renderUsersTableInner() {
  const slice = AppState.paged.users;
  const startIdx = slice.page * slice.size;

  const badge = document.getElementById('usersTotalBadge');
  if (badge) badge.textContent = `Tổng số: ${slice.totalElements} tài khoản`;

  return `
    ${renderServiceStateBanner('users', "fetchPagedResource('users', updateUsersRegion)")}
    <div class="table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>STT</th><th>Tài khoản</th><th>Họ và tên</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Cập nhật</th></tr>
        </thead>
        <tbody>
          ${slice.items.map((u, idx) => `
            <tr>
              <td class="tabular-nums"><strong>#${String(startIdx + idx + 1).padStart(2, '0')}</strong></td>
              <td><strong>${escapeHtml(u.username)}</strong></td>
              <td>${escapeHtml(u.fullName || '—')}</td>
              <td>${escapeHtml(u.email || '—')}</td>
              <td><span class="badge badge-primary">${escapeHtml(getRoleDisplayName(u.role))}</span></td>
              <td>${u.active ? '<span class="badge badge-success">Hoạt động</span>' : '<span class="badge badge-error">Đã khóa</span>'}</td>
              <td>
                <div class="page-actions">
                  <select class="select" style="padding:4px 8px;font-size:0.78rem" onchange="changeUserRoleById(${u.id}, this.value)" aria-label="Đổi vai trò của ${escapeHtml(u.username)}">
                    <option value="ROLE_ADMIN" ${u.role === 'ROLE_ADMIN' ? 'selected' : ''}>Quản trị viên</option>
                    <option value="ROLE_ORGANIZER" ${u.role === 'ROLE_ORGANIZER' ? 'selected' : ''}>Ban tổ chức</option>
                    <option value="ROLE_REFEREE" ${u.role === 'ROLE_REFEREE' ? 'selected' : ''}>Trọng tài</option>
                    <option value="ROLE_TEAM_MANAGER" ${u.role === 'ROLE_TEAM_MANAGER' ? 'selected' : ''}>Lãnh đội / HLV</option>
                    <option value="ROLE_VIEWER" ${u.role === 'ROLE_VIEWER' || u.role === 'ROLE_USER' ? 'selected' : ''}>Khán giả</option>
                  </select>
                  <button class="btn btn-secondary btn-sm" onclick="openAdminResetPasswordModal(${u.id}, '${escapeHtml(u.username)}')">Đặt lại MK</button>
                  <button class="btn btn-secondary btn-sm" onclick="toggleUserActiveStatus(${u.id}, ${!u.active}, this)">${u.active ? 'Khóa' : 'Mở'}</button>
                </div>
              </td>
            </tr>
          `).join('') || `<tr><td colspan="7" class="state-empty-box">${slice.loading ? 'Đang tải danh sách tài khoản...' : 'Không tìm thấy tài khoản nào.'}</td></tr>`}
        </tbody>
      </table>
    </div>
    ${renderPaginationBar('users', slice, 'tài khoản', [5, 10, 20, 50])}
  `;
}

function updateUsersRegion() {
  const el = document.getElementById('usersTableRegion');
  if (el) el.innerHTML = renderUsersTableInner();
}

/* ---------- PAGE: CÀI ĐẶT & THÔNG TIN KỸ THUẬT (DEMO SOA) ---------- */
function renderSettingsPage() {
  const canMonitor = hasPermission('services.monitor');
  const u = AppState.user;
  const eurekaMap = AppState.data.gatewayRoutes?.discoveredServicesInEureka || {};

  return `
    <div class="page-header">
      <div>
        <h1 class="page-title">Cài đặt Hệ thống</h1>
        <p class="page-subtitle">Tùy chỉnh giao diện Sáng/Tối, tùy chọn hiển thị và thông tin tài khoản.</p>
      </div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-header"><h2 class="card-title">Giao diện & Hiển thị</h2></div>
        <p style="font-size:0.86rem;color:var(--text-secondary);margin-bottom:14px;">
          Lựa chọn giao diện được lưu trên trình duyệt:
        </p>
        <div class="page-actions" style="margin-bottom:18px;">
          <button class="btn ${AppState.themeMode === 'light' ? 'btn-primary' : 'btn-secondary'}" onclick="applyTheme('light');renderCurrentPage(false)">☀ Sáng</button>
          <button class="btn ${AppState.themeMode === 'dark' ? 'btn-primary' : 'btn-secondary'}" onclick="applyTheme('dark');renderCurrentPage(false)">🌙 Tối</button>
          <button class="btn ${AppState.themeMode === 'system' ? 'btn-primary' : 'btn-secondary'}" onclick="applyTheme('system');renderCurrentPage(false)">💻 Hệ thống</button>
        </div>
        <div style="border-top:1px solid var(--border-color);padding-top:14px;display:flex;justify-content:space-between;align-items:center;gap:12px;">
          <div>
            <strong>Giảm hiệu ứng chuyển động</strong>
            <div style="font-size:0.8rem;color:var(--text-secondary)">Tắt hoạt ảnh chuyển trang</div>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="toggleReducedMotion()">
            ${AppState.reducedMotion ? 'Đang BẬT' : 'Đang TẮT'}
          </button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h2 class="card-title">Thông tin Tài khoản</h2>
          <span class="badge badge-primary">${escapeHtml(getRoleDisplayName(u ? u.role : null))}</span>
        </div>
        <div style="font-size:0.88rem;line-height:1.8;">
          <div><strong>Tài khoản:</strong> ${escapeHtml(u ? u.username : 'Khách vãng lai')}</div>
          <div><strong>Họ và tên:</strong> ${escapeHtml(u ? (u.fullName || '—') : 'Khách vãng lai')}</div>
          <div><strong>Email:</strong> ${escapeHtml(u ? (u.email || '—') : '—')}</div>
          <div style="margin-top:8px;"><strong>Quyền hiện có:</strong></div>
          <div style="margin-top:6px;">
            ${(AppState.permissions || []).map(p => `<span class="badge badge-primary" style="margin:2px 4px 2px 0;">${escapeHtml(getPermissionDisplayName(p))}</span>`).join('')}
          </div>
          ${u ? `<div style="margin-top:14px;"><button class="btn btn-secondary btn-sm" onclick="openChangePasswordModal()">Đổi mật khẩu cá nhân</button></div>` : ''}
        </div>
      </div>
    </div>

    ${canMonitor ? `
      <div class="card" style="margin-bottom:24px;">
        <div class="card-header" style="flex-wrap:wrap;gap:10px;">
          <div>
            <h2 class="card-title">Thông tin Kỹ thuật & Demo SOA (Eureka / Gateway / Load Balancing)</h2>
            <p style="font-size:0.84rem;color:var(--text-secondary);margin:0;">Khu vực riêng phục vụ theo dõi hạ tầng microservices và kiểm thử khi bảo vệ đồ án</p>
          </div>
          <span class="badge badge-success">${Object.keys(eurekaMap).length} Services trên Eureka (:8761)</span>
        </div>

        ${renderServiceStateBanner('gatewayRoutes')}

        <div class="table-wrap" style="margin-bottom:20px;">
          <table class="data-table">
            <thead>
              <tr><th>Dịch vụ trên Eureka</th><th>Instance & Cổng đang chạy</th><th>Trạng thái</th></tr>
            </thead>
            <tbody>
              ${Object.entries(eurekaMap).map(([svc, instList]) => `
                <tr>
                  <td><strong class="tabular-nums">${escapeHtml(svc)}</strong></td>
                  <td class="tabular-nums">${escapeHtml((instList || []).join(', '))}</td>
                  <td>${statusBadgeHtml('UP')}</td>
                </tr>
              `).join('') || '<tr><td colspan="3" class="state-empty-box">Chưa lấy được danh sách instance từ API Gateway / Eureka.</td></tr>'}
            </tbody>
          </table>
        </div>

        <div style="border-top:1px solid var(--border-color);padding-top:16px;">
          <h3 class="card-title" style="margin-bottom:8px;">Kiểm thử Cân bằng tải Round-Robin (8081 ↔ 8091)</h3>
          <p style="font-size:0.84rem;color:var(--text-secondary);margin-bottom:12px;">
            Gửi 6 request từ <code>match-service</code> sang <code>http://tournament-service</code> qua Eureka LoadBalancer.
          </p>
          <button class="btn btn-primary btn-sm" onclick="runLoadBalancingLiveTest(this)">▶ Chạy Kiểm thử 6 Requests</button>
          <div id="lbTestOutputBox" style="margin-top:12px;font-family:var(--font-mono);font-size:0.8rem;background:var(--bg-surface-secondary);padding:12px;border-radius:8px;min-height:70px;">
            Nhấn nút phía trên để kiểm tra phân tải thực tế...
          </div>
        </div>
      </div>
    ` : ''}
  `;
}

/* ==========================================================================
   INTERACTIVE ACTIONS & CRUD HANDLERS
   ========================================================================== */

async function loginWithCredentials(username, password) {
  const res = await apiCall('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  });
  AppState.token = res.data.accessToken;
  AppState.user = {
    id: res.data.userId,
    username: res.data.username,
    role: res.data.role
  };
  AppState.permissions = Array.isArray(res.data.permissions) ? res.data.permissions : [];
  AppState.sessionNotice = '';
  localStorage.setItem('vn_sports_jwt', AppState.token);
  localStorage.setItem('vn_sports_user', JSON.stringify(AppState.user));

  // Fetch /auth/me to load fullName & email as well
  try {
    const me = await apiCall('/api/v1/auth/me');
    if (me?.data?.username) {
      AppState.user = {
        id: me.data.id,
        username: me.data.username,
        fullName: me.data.fullName,
        email: me.data.email,
        role: me.data.role
      };
      AppState.permissions = Array.isArray(me.data.permissions) ? me.data.permissions : AppState.permissions;
      localStorage.setItem('vn_sports_user', JSON.stringify(AppState.user));
    }
  } catch { /* ignore */ }

  updateHeaderUserUI();
  renderSidebar();
  showToast(`Đăng nhập thành công: ${AppState.user.username} (${getRoleDisplayName(AppState.user.role)})`);

  await loadCoreStats();
  const reqPerm = PAGE_PERMISSIONS[AppState.currentPage];
  if (reqPerm && !hasPermission(reqPerm)) {
    await navigateTo('public-home');
  } else if (AppState.currentPage === 'auth-page') {
    await navigateTo(hasPermission('reports.read_own_tournaments') ? 'dashboard' : 'public-home');
  } else {
    await loadPageData(AppState.currentPage);
    renderCurrentPage(false);
  }
}

async function handleLoginSubmit(e) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const username = document.getElementById('loginUsername').value.trim();
  const password = document.getElementById('loginPassword').value;
  if (!username || !password) {
    if (!username) setFieldError(form, 'username', 'Vui lòng nhập tên đăng nhập');
    if (!password) setFieldError(form, 'password', 'Vui lòng nhập mật khẩu');
    return;
  }
  const unlock = lockFormSubmit(form, 'Đang đăng nhập...');
  try {
    await loginWithCredentials(username, password);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function handleRegisterSubmit(e) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const payload = {
    username: document.getElementById('regUsername').value.trim(),
    password: document.getElementById('regPassword').value,
    fullName: document.getElementById('regFullName').value.trim(),
    email: document.getElementById('regEmail').value.trim(),
    role: document.getElementById('regRole').value
  };
  if (payload.password.length < 6) {
    setFieldError(form, 'password', 'Mật khẩu phải có ít nhất 6 ký tự');
    return;
  }
  const unlock = lockFormSubmit(form, 'Đang tạo tài khoản...');
  try {
    await apiCall('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(payload) });
    showToast(`Đăng ký thành công tài khoản ${payload.username}!`);
    await loginWithCredentials(payload.username, payload.password);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function handleLogout() {
  AppState.sessionNotice = '';
  setGuestState();
  updateHeaderUserUI();
  renderSidebar();
  showToast('Đã đăng xuất.');
  await loadCoreStats();
  await navigateTo('public-home');
}

/* ---------- TOURNAMENT & SPORT CRUD ---------- */
function openCreateTournamentModal() {
  if (!hasPermission('tournaments.create')) {
    showToast('Bạn không có quyền tạo giải đấu', 'error');
    return;
  }
  const today = getTodayLocalIsoDate();
  openModal('Tạo Giải đấu mới', renderTournamentFormHtml(null, {
    name: '',
    description: '',
    startDate: today,
    endDate: today,
    registrationStartDate: today,
    registrationEndDate: today,
    location: '',
    status: 'UPCOMING'
  }));
}

async function openEditTournamentModal(id) {
  if (!hasPermission('tournaments.manage')) {
    showToast('Bạn không có quyền sửa giải đấu', 'error');
    return;
  }
  let t = AppState.paged.tournaments.items.find(x => Number(x.id) === Number(id))
    || AppState.data.tournaments.find(x => Number(x.id) === Number(id))
    || (Number(AppState.detail.tournament.item?.id) === Number(id) ? AppState.detail.tournament.item : null);
  if (!t) {
    try {
      const res = await apiCall(`/api/v1/tournaments/${id}`);
      t = res.data;
    } catch (err) {
      showToast(err.message || 'Không tìm thấy thông tin giải đấu', 'error');
      return;
    }
  }
  openModal(`Chỉnh sửa Giải đấu: ${t.name}`, renderTournamentFormHtml(t.id, t));
}

function renderTournamentFormHtml(editId, t) {
  return `
    <form id="tournamentForm" onsubmit="submitTournamentForm(event, ${editId ? editId : 'null'})">
      <div class="form-error-banner" hidden></div>
      <div class="form-group">
        <label for="mTourName">Tên giải đấu *</label>
        <input id="mTourName" data-field="name" class="input" required value="${escapeHtml(t.name || '')}" placeholder="VD: Giải Bóng Đá Cúp Quốc Gia" />
      </div>
      <div class="form-group">
        <label for="mTourDesc">Mô tả</label>
        <textarea id="mTourDesc" data-field="description" class="textarea" rows="2" placeholder="Thông tin tổng quan về giải đấu">${escapeHtml(t.description || '')}</textarea>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mTourRegStart">Mở đăng ký *</label>
          <input id="mTourRegStart" data-field="registrationStartDate" type="date" class="input" required value="${escapeHtml(t.registrationStartDate || '')}" />
        </div>
        <div class="form-group">
          <label for="mTourRegEnd">Đóng đăng ký *</label>
          <input id="mTourRegEnd" data-field="registrationEndDate" type="date" class="input" required value="${escapeHtml(t.registrationEndDate || '')}" />
        </div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mTourStart">Ngày khai mạc *</label>
          <input id="mTourStart" data-field="startDate" type="date" class="input" required value="${escapeHtml(t.startDate || '')}" />
        </div>
        <div class="form-group">
          <label for="mTourEnd">Ngày kết thúc *</label>
          <input id="mTourEnd" data-field="endDate" type="date" class="input" required value="${escapeHtml(t.endDate || '')}" />
        </div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mTourLoc">Địa điểm tổ chức *</label>
          <input id="mTourLoc" data-field="location" class="input" required value="${escapeHtml(t.location || '')}" placeholder="VD: SVĐ Quốc Gia Mỹ Đình" />
        </div>
        <div class="form-group">
          <label for="mTourStatus">Trạng thái *</label>
          <select id="mTourStatus" data-field="status" class="select">
            <option value="DRAFT" ${t.status === 'DRAFT' ? 'selected' : ''}>Bản nháp</option>
            <option value="OPEN_REGISTRATION" ${t.status === 'OPEN_REGISTRATION' ? 'selected' : ''}>Đang mở đăng ký</option>
            <option value="UPCOMING" ${t.status === 'UPCOMING' ? 'selected' : ''}>Sắp khởi tranh</option>
            <option value="ONGOING" ${t.status === 'ONGOING' ? 'selected' : ''}>Đang diễn ra</option>
            <option value="COMPLETED" ${t.status === 'COMPLETED' ? 'selected' : ''}>Đã hoàn thành</option>
            <option value="FINISHED" ${t.status === 'FINISHED' ? 'selected' : ''}>Đã kết thúc</option>
            <option value="CANCELLED" ${t.status === 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
          </select>
        </div>
      </div>
      <div class="page-actions" style="justify-content:flex-end;margin-top:8px;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">${editId ? 'Lưu thay đổi' : 'Tạo giải đấu'}</button>
      </div>
    </form>
  `;
}

async function refreshAfterMutation({
  clearCache = [],
  paged = [],
  reloadTournamentId = null,
  reloadMatchId = null
} = {}) {
  clearCache.forEach(k => {
    if (Array.isArray(AppState.data[k])) AppState.data[k] = [];
  });
  const tasks = [loadCoreStats()];
  const current = AppState.currentPage;
  const pageMap = {
    tournaments: 'tournaments',
    teams: 'teams',
    athletes: 'athletes',
    matches: 'schedule',
    registrations: 'tournament-detail',
    results: 'results',
    rankings: 'rankings',
    rankingItems: 'rankings',
    users: 'users',
    notifications: 'reports'
  };
  let reloadedCurrentPage = false;

  if (current === 'tournament-detail' && (reloadTournamentId || AppState.selectedTournamentId)) {
    tasks.push(loadTournamentDetail(reloadTournamentId || AppState.selectedTournamentId));
    reloadedCurrentPage = true;
  } else if (current === 'match-detail' && (reloadMatchId || AppState.selectedMatchId)) {
    tasks.push(loadMatchDetail(reloadMatchId || AppState.selectedMatchId));
    reloadedCurrentPage = true;
  }

  paged.forEach(resKey => {
    if (reloadedCurrentPage && resKey === 'registrations' && current === 'tournament-detail') return;
    if (pageMap[resKey] === current) {
      tasks.push(fetchPagedResource(resKey));
      reloadedCurrentPage = true;
    } else if (AppState.paged[resKey]) {
      AppState.paged[resKey].loaded = false;
    }
  });

  if (!reloadedCurrentPage && current !== 'auth-page') {
    tasks.push(loadPageData(current));
  }
  await Promise.all(tasks);
}

async function submitTournamentForm(e, editId) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);

  const body = {
    name: document.getElementById('mTourName').value.trim(),
    description: document.getElementById('mTourDesc').value.trim(),
    registrationStartDate: document.getElementById('mTourRegStart').value,
    registrationEndDate: document.getElementById('mTourRegEnd').value,
    startDate: document.getElementById('mTourStart').value,
    endDate: document.getElementById('mTourEnd').value,
    location: document.getElementById('mTourLoc').value.trim(),
    status: document.getElementById('mTourStatus').value
  };

  let invalid = false;
  if (!body.name) { setFieldError(form, 'name', 'Tên giải đấu không được để trống'); invalid = true; }
  if (!body.location) { setFieldError(form, 'location', 'Địa điểm tổ chức không được để trống'); invalid = true; }
  if (body.startDate && body.endDate && body.startDate > body.endDate) {
    setFieldError(form, 'endDate', 'Ngày kết thúc phải sau hoặc bằng ngày khai mạc');
    invalid = true;
  }
  if (body.registrationStartDate && body.registrationEndDate && body.registrationStartDate > body.registrationEndDate) {
    setFieldError(form, 'registrationEndDate', 'Ngày đóng đăng ký phải sau hoặc bằng ngày mở đăng ký');
    invalid = true;
  }
  if (body.registrationEndDate && body.startDate && body.registrationEndDate > body.startDate) {
    setFieldError(form, 'registrationEndDate', 'Ngày đóng đăng ký không được sau ngày khai mạc');
    invalid = true;
  }
  if (invalid) return;

  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    if (editId) {
      await apiCall(`/api/v1/tournaments/${editId}`, { method: 'PUT', body: JSON.stringify(body) });
      showToast('Đã cập nhật thông tin giải đấu!');
    } else {
      await apiCall('/api/v1/tournaments', { method: 'POST', body: JSON.stringify(body) });
      showToast('Đã tạo giải đấu mới thành công!');
    }
    closeModal();
    await refreshAfterMutation({
      clearCache: ['tournaments'],
      paged: ['tournaments'],
      reloadTournamentId: editId
    });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function patchTournamentStatus(id, status, btn = null) {
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/tournaments/${id}`, { method: 'PATCH', body: JSON.stringify({ status }) });
    showToast(`Đã cập nhật trạng thái giải đấu.`);
    await refreshAfterMutation({
      clearCache: ['tournaments'],
      paged: ['tournaments'],
      reloadTournamentId: id
    });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

function confirmDeleteTournament(id, name) {
  openConfirmDeleteModal({
    entityLabel: 'giải đấu',
    itemName: name || `#${id}`,
    warningNote: 'Hệ thống sẽ từ chối xóa nếu giải đấu vẫn còn nội dung thi đấu hoặc trận đấu liên kết.',
    onConfirm: async () => {
      await apiCall(`/api/v1/tournaments/${id}`, { method: 'DELETE' });
      showToast(`Đã xóa giải đấu "${name}"`);
      if (Number(AppState.selectedTournamentId) === Number(id)) {
        AppState.selectedTournamentId = null;
      }
      await refreshAfterMutation({
        clearCache: ['tournaments', 'competitions'],
        paged: ['tournaments']
      });
      renderCurrentPage(false);
    }
  });
}

function openCreateSportModal() {
  if (!hasPermission('tournaments.create')) {
    showToast('Bạn không có quyền thêm môn thể thao', 'error');
    return;
  }
  openModal('Thêm Môn thể thao mới', `
    <form id="sportForm" onsubmit="submitCreateSport(event)">
      <div class="form-error-banner" hidden></div>
      <div class="form-group"><label for="mSportName">Tên môn thể thao *</label><input id="mSportName" data-field="name" class="input" required placeholder="VD: Quần vợt" /></div>
      <div class="form-group"><label for="mSportCat">Phân loại</label><select id="mSportCat" data-field="sportType" class="select"><option value="TEAM">Đồng đội</option><option value="INDIVIDUAL">Cá nhân</option><option value="DUAL">Đối kháng / Đôi</option></select></div>
      <div class="form-group"><label for="mSportDesc">Mô tả</label><input id="mSportDesc" data-field="description" class="input" placeholder="Quy chuẩn thi đấu" /></div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">Lưu môn thể thao</button>
      </div>
    </form>
  `);
}

async function submitCreateSport(e) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    await apiCall('/api/v1/sports', {
      method: 'POST',
      body: JSON.stringify({
        name: document.getElementById('mSportName').value.trim(),
        sportType: document.getElementById('mSportCat').value,
        description: document.getElementById('mSportDesc').value.trim()
      })
    });
    closeModal();
    showToast('Đã thêm môn thể thao mới!');
    await Promise.all([loadCoreStats(), safeFetchSlice('sports', '/api/v1/sports')]);
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function openCreateCompetitionModal(tournamentId) {
  if (!hasPermission('tournaments.manage')) {
    showToast('Bạn không có quyền thêm nội dung thi đấu', 'error');
    return;
  }
  if ((AppState.data.sports || []).length === 0) {
    await safeFetchSlice('sports', '/api/v1/sports');
  }
  const sports = AppState.data.sports || [];
  if (sports.length === 0) {
    showToast('Vui lòng thêm môn thể thao trước khi tạo nội dung thi đấu.', 'warning');
    return;
  }
  openModal('Thêm Nội dung thi đấu mới', `
    <form id="competitionCreateForm" onsubmit="submitCreateCompetitionForm(event, ${tournamentId})">
      <div class="form-error-banner" hidden></div>
      <div class="form-group">
        <label for="mCompName">Tên nội dung thi đấu *</label>
        <input id="mCompName" data-field="name" class="input" required placeholder="VD: Bóng đá Nam U23" />
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mCompSportId">Môn thể thao *</label>
          <select id="mCompSportId" data-field="sportId" class="select" required>
            ${sports.map(s => `<option value="${s.id}">${escapeHtml(s.name)} (${escapeHtml(s.sportType || 'TEAM')})</option>`).join('')}
          </select>
        </div>
        <div class="form-group">
          <label for="mCompStatus">Trạng thái *</label>
          <select id="mCompStatus" data-field="status" class="select">
            <option value="OPEN_REGISTRATION" selected>Mở đăng ký</option>
            <option value="DRAFT">Bản nháp</option>
            <option value="SCHEDULED">Đã xếp lịch</option>
            <option value="ONGOING">Đang diễn ra</option>
            <option value="COMPLETED">Hoàn thành</option>
            <option value="FINISHED">Kết thúc</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
      </div>
      <div class="page-actions" style="justify-content:flex-end;margin-top:8px;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">Tạo nội dung</button>
      </div>
    </form>
  `);
}

async function submitCreateCompetitionForm(e, tournamentId) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const name = document.getElementById('mCompName').value.trim();
  if (!name) {
    setFieldError(form, 'name', 'Tên nội dung thi đấu không được để trống');
    return;
  }
  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    await apiCall('/api/v1/competitions', {
      method: 'POST',
      body: JSON.stringify({
        tournamentId: Number(tournamentId),
        sportId: Number(document.getElementById('mCompSportId').value),
        name,
        status: document.getElementById('mCompStatus').value
      })
    });
    closeModal();
    showToast('Đã tạo nội dung thi đấu mới!');
    await refreshAfterMutation({
      clearCache: ['competitions'],
      reloadTournamentId: tournamentId
    });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function openCompetitionSettingsModal(tournamentId, competitionId) {
  if (!hasPermission('tournaments.manage')) {
    showToast('Bạn không có quyền cấu hình luật & thể thức', 'error');
    return;
  }
  const cid = Number(competitionId);
  const tourComps = (AppState.detail.tournament.competitions || []).length > 0
    ? AppState.detail.tournament.competitions
    : AppState.data.competitions.filter(c => Number(c.tournamentId) === Number(tournamentId));
  const comp = tourComps.find(c => Number(c.id) === cid) || { id: cid, name: `Nội dung #${cid}`, status: 'OPEN_REGISTRATION' };

  const [ruleRes, fmtRes] = await Promise.all([
    apiCall(`/api/v1/competitions/${cid}/rule`).catch(() => ({ data: {} })),
    apiCall(`/api/v1/competitions/${cid}/format`).catch(() => ({ data: {} }))
  ]);
  const rule = ruleRes?.data || {};
  const fmt = fmtRes?.data || {};

  openModal(`Cấu hình Nội dung: ${comp.name}`, `
    <form id="compSettingsForm" onsubmit="submitCompetitionSettingsForm(event, ${tournamentId}, ${cid})">
      <div class="form-error-banner" hidden></div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mCfgStatus">Trạng thái nội dung</label>
          <select id="mCfgStatus" data-field="status" class="select">
            <option value="DRAFT" ${comp.status === 'DRAFT' ? 'selected' : ''}>Bản nháp</option>
            <option value="OPEN_REGISTRATION" ${comp.status === 'OPEN_REGISTRATION' ? 'selected' : ''}>Mở đăng ký</option>
            <option value="SCHEDULED" ${comp.status === 'SCHEDULED' ? 'selected' : ''}>Đã xếp lịch</option>
            <option value="ONGOING" ${comp.status === 'ONGOING' ? 'selected' : ''}>Đang diễn ra</option>
            <option value="COMPLETED" ${comp.status === 'COMPLETED' ? 'selected' : ''}>Hoàn thành</option>
            <option value="FINISHED" ${comp.status === 'FINISHED' ? 'selected' : ''}>Kết thúc</option>
            <option value="CANCELLED" ${comp.status === 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
          </select>
        </div>
        <div class="form-group">
          <label for="mCfgFormat">Thể thức thi đấu *</label>
          <select id="mCfgFormat" data-field="formatType" class="select">
            <option value="ROUND_ROBIN" ${fmt.formatType === 'ROUND_ROBIN' ? 'selected' : ''}>Vòng tròn (ROUND_ROBIN)</option>
            <option value="KNOCKOUT" ${!fmt.formatType || fmt.formatType === 'KNOCKOUT' ? 'selected' : ''}>Loại trực tiếp (KNOCKOUT)</option>
            <option value="GROUP_STAGE" ${fmt.formatType === 'GROUP_STAGE' ? 'selected' : ''}>Chia bảng (GROUP_STAGE)</option>
            <option value="HYBRID" ${fmt.formatType === 'HYBRID' ? 'selected' : ''}>Hỗn hợp (HYBRID)</option>
          </select>
        </div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mCfgMaxPart">Số đội / VĐV tối đa *</label>
          <input id="mCfgMaxPart" data-field="maxParticipants" type="number" min="2" class="input" required value="${rule.maxParticipants ?? 16}" />
        </div>
        <div class="form-group">
          <label for="mCfgGroups">Số bảng đấu</label>
          <input id="mCfgGroups" data-field="numberOfGroups" type="number" min="1" class="input" required value="${fmt.numberOfGroups ?? 1}" />
        </div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mCfgMinMem">Thành viên tối thiểu / đội *</label>
          <input id="mCfgMinMem" data-field="minTeamMembers" type="number" min="1" class="input" required value="${rule.minTeamMembers ?? 5}" />
        </div>
        <div class="form-group">
          <label for="mCfgMaxMem">Thành viên tối đa / đội *</label>
          <input id="mCfgMaxMem" data-field="maxTeamMembers" type="number" min="1" class="input" required value="${rule.maxTeamMembers ?? 20}" />
        </div>
      </div>
      <div class="form-group">
        <label for="mCfgRegDeadline">Hạn chót đăng ký nội dung (tùy chọn)</label>
        <input id="mCfgRegDeadline" data-field="registrationDeadline" type="datetime-local" class="input" value="${rule.registrationDeadline ? String(rule.registrationDeadline).slice(0, 16) : ''}" />
      </div>
      <div class="form-group" style="margin-top:4px;">
        <label style="display:flex;align-items:center;gap:8px;cursor:pointer;font-weight:500;">
          <input id="mCfgThirdPlace" data-field="hasThirdPlaceMatch" type="checkbox" ${fmt.hasThirdPlaceMatch ? 'checked' : ''} />
          <span>Có trận tranh hạng ba (dành cho thể thức Loại trực tiếp)</span>
        </label>
      </div>
      <div class="page-actions" style="justify-content:flex-end;margin-top:8px;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">Lưu cấu hình</button>
      </div>
    </form>
  `);
}

async function submitCompetitionSettingsForm(e, tournamentId, competitionId) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const maxParticipants = Number(document.getElementById('mCfgMaxPart').value);
  const minTeamMembers = Number(document.getElementById('mCfgMinMem').value);
  const maxTeamMembers = Number(document.getElementById('mCfgMaxMem').value);
  const numberOfGroups = Number(document.getElementById('mCfgGroups').value);
  if (minTeamMembers > maxTeamMembers) {
    setFieldError(form, 'minTeamMembers', 'Số thành viên tối thiểu không được lớn hơn tối đa');
    return;
  }

  const tourComps = AppState.detail.tournament.competitions || [];
  const existingComp = tourComps.find(c => Number(c.id) === Number(competitionId));
  const nextStatus = document.getElementById('mCfgStatus').value;

  const regDeadlineRaw = document.getElementById('mCfgRegDeadline')?.value;
  const rulePayload = { maxParticipants, minTeamMembers, maxTeamMembers };
  if (regDeadlineRaw) {
    rulePayload.registrationDeadline = regDeadlineRaw.length === 16 ? `${regDeadlineRaw}:00` : regDeadlineRaw;
  }

  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    const requests = [
      apiCall(`/api/v1/competitions/${competitionId}/rule`, {
        method: 'PUT',
        body: JSON.stringify(rulePayload)
      }),
      apiCall(`/api/v1/competitions/${competitionId}/format`, {
        method: 'PUT',
        body: JSON.stringify({
          formatType: document.getElementById('mCfgFormat').value,
          numberOfGroups,
          hasThirdPlaceMatch: Boolean(document.getElementById('mCfgThirdPlace')?.checked)
        })
      })
    ];
    if (existingComp && existingComp.status !== nextStatus) {
      requests.push(apiCall(`/api/v1/competitions/${competitionId}`, {
        method: 'PUT',
        body: JSON.stringify({
          tournamentId: existingComp.tournamentId,
          sportId: existingComp.sportId,
          name: existingComp.name,
          status: nextStatus
        })
      }));
    }
    await Promise.all(requests);
    closeModal();
    showToast('Đã cập nhật quy định & thể thức thi đấu!');
    await refreshAfterMutation({
      clearCache: ['competitions'],
      reloadTournamentId: tournamentId
    });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

/* ---------- TEAM & ROSTER CRUD ---------- */
function openCreateTeamModal() {
  if (!hasPermission('teams.manage_own')) {
    showToast('Bạn không có quyền tạo đội tuyển', 'error');
    return;
  }
  openModal('Tạo Đội tuyển mới', renderTeamFormHtml(null, {}));
}

async function openEditTeamModal(id) {
  if (!hasPermission('teams.manage_own')) {
    showToast('Bạn không có quyền sửa thông tin đội tuyển', 'error');
    return;
  }
  let t = AppState.paged.teams.items.find(x => Number(x.id) === Number(id))
    || AppState.data.teams.find(x => Number(x.id) === Number(id));
  if (!t) {
    try {
      const res = await apiCall(`/api/v1/teams/${id}`);
      t = res.data;
    } catch (err) {
      showToast(err.message || 'Không tìm thấy thông tin đội tuyển', 'error');
      return;
    }
  }
  openModal(`Chỉnh sửa Đội tuyển: ${t.name}`, renderTeamFormHtml(t.id, t));
}

function renderTeamFormHtml(editId, t) {
  return `
    <form id="teamForm" onsubmit="submitTeamForm(event, ${editId ? editId : 'null'})">
      <div class="form-error-banner" hidden></div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mTeamName">Tên đội tuyển / CLB *</label>
          <input id="mTeamName" data-field="name" class="input" required value="${escapeHtml(t.name || '')}" placeholder="VD: CLB Bóng Đá Huế" />
        </div>
        <div class="form-group">
          <label for="mTeamShort">Tên viết tắt *</label>
          <input id="mTeamShort" data-field="shortName" class="input" required maxlength="15" value="${escapeHtml(t.shortName || '')}" placeholder="HUE" />
        </div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mTeamMgr">HLV Trưởng *</label>
          <input id="mTeamMgr" data-field="managerName" class="input" required value="${escapeHtml(t.managerName || '')}" placeholder="HLV Nguyễn Văn B" />
        </div>
        <div class="form-group">
          <label for="mTeamPhone">Số điện thoại</label>
          <input id="mTeamPhone" data-field="contactPhone" class="input" value="${escapeHtml(t.contactPhone || '')}" placeholder="09xxxxxxxx" />
        </div>
      </div>
      <div class="form-group">
        <label for="mTeamEmail">Email liên hệ</label>
        <input id="mTeamEmail" data-field="contactEmail" type="email" class="input" value="${escapeHtml(t.contactEmail || '')}" placeholder="club@sports.vn" />
      </div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">${editId ? 'Lưu thay đổi' : 'Tạo đội tuyển'}</button>
      </div>
    </form>
  `;
}

async function submitTeamForm(e, editId) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const body = {
    name: document.getElementById('mTeamName').value.trim(),
    shortName: document.getElementById('mTeamShort').value.trim(),
    managerName: document.getElementById('mTeamMgr').value.trim(),
    contactPhone: document.getElementById('mTeamPhone').value.trim(),
    contactEmail: document.getElementById('mTeamEmail').value.trim()
  };

  let invalid = false;
  if (!body.name) { setFieldError(form, 'name', 'Tên đội tuyển không được để trống'); invalid = true; }
  if (!body.shortName) { setFieldError(form, 'shortName', 'Tên viết tắt không được để trống'); invalid = true; }
  if (!body.managerName) { setFieldError(form, 'managerName', 'Vui lòng nhập tên HLV trưởng'); invalid = true; }
  if (invalid) return;

  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    if (editId) {
      await apiCall(`/api/v1/teams/${editId}`, { method: 'PUT', body: JSON.stringify(body) });
      showToast('Đã cập nhật thông tin đội tuyển!');
    } else {
      await apiCall('/api/v1/teams', { method: 'POST', body: JSON.stringify(body) });
      showToast('Đã tạo đội tuyển mới!');
    }
    closeModal();
    await refreshAfterMutation({ clearCache: ['teams'], paged: ['teams'] });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

function confirmDeleteTeam(id, name) {
  openConfirmDeleteModal({
    entityLabel: 'đội tuyển',
    itemName: name || `#${id}`,
    warningNote: 'Nếu đội tuyển vẫn còn thành viên trong đội hình, hệ thống sẽ từ chối xóa.',
    onConfirm: async () => {
      await apiCall(`/api/v1/teams/${id}`, { method: 'DELETE' });
      showToast(`Đã xóa đội tuyển "${name}"`);
      await refreshAfterMutation({ clearCache: ['teams'], paged: ['teams'] });
      renderCurrentPage(false);
    }
  });
}

async function viewTeamMembersModal(teamId, teamName) {
  try {
    const res = await apiCall(`/api/v1/teams/${teamId}/members`);
    const members = Array.isArray(res.data) ? res.data : [];
    if (members.some(m => !m.athleteName) && AppState.data.athletes.length === 0) {
      await safeFetchSlice('athletes', '/api/v1/athletes');
    }
    const athMap = {};
    AppState.data.athletes.forEach(a => { if (a && a.id) athMap[a.id] = a.fullName; });
    const canManageRoster = hasPermission('athletes.manage_own') || hasPermission('teams.manage_own');

    openModal(`Đội hình ${teamName} (${members.length} thành viên)`, `
      <div style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
        <span style="font-size:0.85rem;color:var(--text-secondary);">Danh sách vận động viên đăng ký trong đội</span>
        ${canManageRoster ? `<button class="btn btn-primary btn-sm" onclick="openAddAthleteToTeamModalForTeam(${teamId})">+ Thêm VĐV vào đội</button>` : ''}
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>Số áo</th><th>Họ tên VĐV</th><th>Vị trí</th>${canManageRoster ? '<th>Thao tác</th>' : ''}</tr></thead>
          <tbody>
            ${members.map(m => {
              const name = m.athleteName || athMap[m.athleteId] || `VĐV #${m.athleteId}`;
              return `
                <tr>
                  <td class="tabular-nums"><strong>#${m.jerseyNumber ?? '-'}</strong></td>
                  <td><strong>${escapeHtml(name)}</strong></td>
                  <td>${escapeHtml(m.position || '—')}</td>
                  ${canManageRoster ? `<td><button class="btn btn-danger btn-sm" data-team-name="${escapeHtml(teamName)}" data-member-name="${escapeHtml(name)}" onclick="removeTeamMember(${teamId}, ${m.id}, this.dataset.teamName, this.dataset.memberName, this)">Gỡ khỏi đội</button></td>` : ''}
                </tr>
              `;
            }).join('') || `<tr><td colspan="${canManageRoster ? 4 : 3}" class="state-empty-box">Chưa có thành viên nào trong đội hình.</td></tr>`}
          </tbody>
        </table>
      </div>
    `);
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function removeTeamMember(teamId, memberId, teamName, memberName, btn) {
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/teams/${teamId}/members/${memberId}`, { method: 'DELETE' });
    showToast(`Đã gỡ ${memberName} khỏi ${teamName}`);
    await viewTeamMembersModal(teamId, teamName);
  } catch (err) {
    showToast(err.message, 'error');
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

function openAddAthleteToTeamModalForTeam(teamId) {
  openAddAthleteToTeamModal(teamId);
}

async function openAddAthleteToTeamModal(preselectedTeamId = null) {
  if (!hasPermission('athletes.manage_own') && !hasPermission('teams.manage_own')) {
    showToast('Bạn không có quyền thêm VĐV vào đội', 'error');
    return;
  }
  if (AppState.data.teams.length === 0) await safeFetchSlice('teams', '/api/v1/teams');
  if (AppState.data.athletes.length === 0) await safeFetchSlice('athletes', '/api/v1/athletes');

  const isManager = AppState.auth.role === 'ROLE_TEAM_MANAGER';
  const myUsername = String(AppState.auth.username || '').toLowerCase();
  const teams = isManager
    ? AppState.data.teams.filter(t => t.ownerUsername && t.ownerUsername.toLowerCase() === myUsername)
    : AppState.data.teams;
  const athletes = isManager
    ? AppState.data.athletes.filter(a => a.ownerUsername && a.ownerUsername.toLowerCase() === myUsername)
    : AppState.data.athletes;
  if (teams.length === 0 || athletes.length === 0) {
    showToast(
      isManager
        ? 'Bạn cần có ít nhất 1 đội tuyển và 1 VĐV thuộc quyền sở hữu của mình trước khi liên kết.'
        : 'Cần có ít nhất 1 đội tuyển và 1 VĐV trong hệ thống trước khi liên kết.',
      'warning'
    );
    return;
  }

  openModal('Thêm Vận động viên vào Đội tuyển', `
    <form id="addMemberForm" onsubmit="submitAddAthleteToTeam(event)">
      <div class="form-error-banner" hidden></div>
      <div class="form-group">
        <label for="mMemTeamId">Đội tuyển</label>
        <select id="mMemTeamId" data-field="teamId" class="select">
          ${teams.map(t => `<option value="${t.id}" ${Number(t.id) === Number(preselectedTeamId) ? 'selected' : ''}>${escapeHtml(t.name)}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label for="mMemAthId">Vận động viên</label>
        <select id="mMemAthId" data-field="athleteId" class="select">
          ${athletes.map(a => `<option value="${a.id}">${escapeHtml(a.fullName)}</option>`).join('')}
        </select>
      </div>
      <div class="form-grid">
        <div class="form-group"><label for="mMemJersey">Số áo *</label><input id="mMemJersey" data-field="jerseyNumber" type="number" min="1" max="99" class="input" value="10" required /></div>
        <div class="form-group"><label for="mMemPos">Vị trí *</label><input id="mMemPos" data-field="position" class="input" placeholder="VD: Tiền đạo / Hậu vệ" required /></div>
      </div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">Thêm vào đội</button>
      </div>
    </form>
  `);
}

async function submitAddAthleteToTeam(e) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const teamId = document.getElementById('mMemTeamId').value;
  const jerseyNumber = Number(document.getElementById('mMemJersey').value);
  const position = document.getElementById('mMemPos').value.trim();

  if (!position) {
    setFieldError(form, 'position', 'Vui lòng nhập vị trí thi đấu');
    return;
  }

  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    await apiCall(`/api/v1/teams/${teamId}/members`, {
      method: 'POST',
      body: JSON.stringify({
        athleteId: Number(document.getElementById('mMemAthId').value),
        jerseyNumber,
        position
      })
    });
    closeModal();
    showToast('Đã thêm vận động viên vào đội tuyển!');
    await refreshAfterMutation({ clearCache: ['teams'], paged: ['teams'] });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

/* ---------- ATHLETE CRUD ---------- */
function openCreateAthleteModal() {
  if (!hasPermission('athletes.manage_own')) {
    showToast('Bạn không có quyền tạo hồ sơ VĐV', 'error');
    return;
  }
  openModal('Thêm Hồ sơ Vận động viên mới', renderAthleteFormHtml(null, { healthStatus: 'QUALIFIED', gender: 'MALE', nationality: 'Vietnam' }));
}

async function openEditAthleteModal(id) {
  if (!hasPermission('athletes.manage_own')) {
    showToast('Bạn không có quyền chỉnh sửa hồ sơ VĐV', 'error');
    return;
  }
  let a = AppState.paged.athletes.items.find(x => Number(x.id) === Number(id))
    || AppState.data.athletes.find(x => Number(x.id) === Number(id));
  if (!a) {
    try {
      const res = await apiCall(`/api/v1/athletes/${id}`);
      a = res.data;
    } catch (err) {
      showToast(err.message || 'Không tìm thấy thông tin vận động viên', 'error');
      return;
    }
  }
  openModal(`Chỉnh sửa Hồ sơ: ${a.fullName}`, renderAthleteFormHtml(a.id, a));
}

function renderAthleteFormHtml(editId, a) {
  const phone = a.phoneNumber || a.phone || '';
  return `
    <form id="athleteForm" onsubmit="submitAthleteForm(event, ${editId ? editId : 'null'})">
      <div class="form-error-banner" hidden></div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mAthName">Họ và tên *</label>
          <input id="mAthName" data-field="fullName" class="input" required value="${escapeHtml(a.fullName || '')}" placeholder="Nguyễn Văn A" />
        </div>
        <div class="form-group">
          <label for="mAthDob">Ngày sinh</label>
          <input id="mAthDob" data-field="dateOfBirth" type="date" class="input" value="${escapeHtml(a.dateOfBirth || '')}" />
        </div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mAthGender">Giới tính</label>
          <select id="mAthGender" data-field="gender" class="select">
            <option value="MALE" ${a.gender === 'MALE' ? 'selected' : ''}>Nam</option>
            <option value="FEMALE" ${a.gender === 'FEMALE' ? 'selected' : ''}>Nữ</option>
            <option value="OTHER" ${a.gender === 'OTHER' ? 'selected' : ''}>Khác</option>
          </select>
        </div>
        <div class="form-group">
          <label for="mAthHealth">Tình trạng thể lực</label>
          <select id="mAthHealth" data-field="healthStatus" class="select">
            <option value="QUALIFIED" ${!a.healthStatus || a.healthStatus === 'QUALIFIED' ? 'selected' : ''}>Đủ điều kiện (QUALIFIED)</option>
            <option value="INJURED" ${a.healthStatus === 'INJURED' ? 'selected' : ''}>Chấn thương (INJURED)</option>
            <option value="SUSPENDED" ${a.healthStatus === 'SUSPENDED' ? 'selected' : ''}>Treo giò (SUSPENDED)</option>
          </select>
        </div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mAthCccd">Số CCCD</label>
          <input id="mAthCccd" data-field="identityCardNumber" class="input" value="${escapeHtml(a.identityCardNumber || '')}" placeholder="12 chữ số" />
        </div>
        <div class="form-group">
          <label for="mAthPhone">Số điện thoại</label>
          <input id="mAthPhone" data-field="phone" class="input" value="${escapeHtml(phone)}" placeholder="09xxxxxxxx" />
        </div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mAthEmail">Email</label>
          <input id="mAthEmail" data-field="email" type="email" class="input" value="${escapeHtml(a.email || '')}" placeholder="athlete@sports.vn" />
        </div>
        <div class="form-group">
          <label for="mAthNat">Quốc tịch</label>
          <input id="mAthNat" data-field="nationality" class="input" value="${escapeHtml(a.nationality || 'Vietnam')}" />
        </div>
      </div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">${editId ? 'Lưu thay đổi' : 'Tạo hồ sơ'}</button>
      </div>
    </form>
  `;
}

async function submitAthleteForm(e, editId) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);

  const body = {
    fullName: document.getElementById('mAthName').value.trim(),
    dateOfBirth: document.getElementById('mAthDob').value || null,
    gender: document.getElementById('mAthGender').value,
    identityCardNumber: document.getElementById('mAthCccd').value.trim(),
    phone: document.getElementById('mAthPhone').value.trim(),
    email: document.getElementById('mAthEmail').value.trim(),
    nationality: document.getElementById('mAthNat').value.trim() || 'Vietnam',
    healthStatus: document.getElementById('mAthHealth').value
  };

  if (!body.fullName) {
    setFieldError(form, 'fullName', 'Họ và tên vận động viên không được để trống');
    return;
  }

  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    if (editId) {
      await apiCall(`/api/v1/athletes/${editId}`, { method: 'PUT', body: JSON.stringify(body) });
      showToast('Đã cập nhật hồ sơ vận động viên!');
    } else {
      await apiCall('/api/v1/athletes', { method: 'POST', body: JSON.stringify(body) });
      showToast('Đã tạo hồ sơ vận động viên mới!');
    }
    closeModal();
    await refreshAfterMutation({ clearCache: ['athletes'], paged: ['athletes'] });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function patchAthleteHealth(id, healthStatus, btn = null) {
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/athletes/${id}`, { method: 'PATCH', body: JSON.stringify({ healthStatus }) });
    showToast(`Đã cập nhật trạng thái thể lực sang ${healthStatus}`);
    await refreshAfterMutation({ clearCache: ['athletes'], paged: ['athletes'] });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

function confirmDeleteAthlete(id, name) {
  openConfirmDeleteModal({
    entityLabel: 'vận động viên',
    itemName: name || `#${id}`,
    warningNote: 'Hồ sơ vận động viên sẽ bị xóa khỏi danh sách quản lý nếu không có ràng buộc từ chối.',
    onConfirm: async () => {
      await apiCall(`/api/v1/athletes/${id}`, { method: 'DELETE' });
      showToast(`Đã xóa vận động viên "${name}"`);
      await refreshAfterMutation({ clearCache: ['athletes'], paged: ['athletes'] });
      renderCurrentPage(false);
    }
  });
}

/* ---------- REGISTRATION WORKFLOW ---------- */
async function openRegisterParticipantModal(tournamentId) {
  if (!hasPermission('registrations.manage_own') && !hasPermission('tournaments.manage')) {
    showToast('Bạn không có quyền gửi đăng ký tham gia giải', 'error');
    return;
  }
  const tasks = [apiCall(`/api/v1/competitions?tournamentId=${tournamentId}`).catch(() => ({ data: [] }))];
  if (AppState.data.teams.length === 0) tasks.push(safeFetchSlice('teams', '/api/v1/teams'));
  if (AppState.data.athletes.length === 0) tasks.push(safeFetchSlice('athletes', '/api/v1/athletes'));

  const [compsRes] = await Promise.all(tasks);
  const comps = Array.isArray(compsRes?.data)
    ? compsRes.data
    : AppState.data.competitions.filter(c => Number(c.tournamentId) === Number(tournamentId));
  if (comps.length === 0) {
    showToast('Giải đấu này chưa có nội dung thi đấu để đăng ký.', 'warning');
    return;
  }
  const d = AppState.data;

  openModal('Đăng ký Tham gia Giải đấu', `
    <form id="registrationForm" onsubmit="submitRegistrationForm(event)">
      <div class="form-error-banner" hidden></div>
      <div class="form-group">
        <label for="mRegCompId">Nội dung thi đấu *</label>
        <select id="mRegCompId" data-field="competitionId" class="select">
          ${comps.map(c => `<option value="${c.id}">${escapeHtml(c.name || c.competitionName)}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label for="mRegType">Đối tượng đăng ký *</label>
        <select id="mRegType" data-field="participantType" class="select" onchange="toggleRegistrationParticipantOptions()">
          <option value="TEAM">Đội tuyển / Câu lạc bộ</option>
          ${d.athletes.length > 0 ? '<option value="ATHLETE">Vận động viên cá nhân</option>' : ''}
        </select>
      </div>
      <div class="form-group">
        <label for="mRegRefId">Chọn Đội tuyển / VĐV *</label>
        <select id="mRegRefId" data-field="participantReferenceId" class="select">
          ${d.teams.map(t => `<option value="${t.id}" data-label="${escapeHtml(t.name)}">${escapeHtml(t.name)}</option>`).join('')}
        </select>
      </div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">Gửi hồ sơ đăng ký</button>
      </div>
    </form>
  `);
}

function toggleRegistrationParticipantOptions() {
  const type = document.getElementById('mRegType')?.value;
  const refSel = document.getElementById('mRegRefId');
  if (!refSel) return;
  if (type === 'ATHLETE') {
    refSel.innerHTML = AppState.data.athletes
      .map(a => `<option value="${a.id}" data-label="${escapeHtml(a.fullName)}">${escapeHtml(a.fullName)}</option>`)
      .join('');
  } else {
    refSel.innerHTML = AppState.data.teams
      .map(t => `<option value="${t.id}" data-label="${escapeHtml(t.name)}">${escapeHtml(t.name)}</option>`)
      .join('');
  }
}

async function submitRegistrationForm(e) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const refSel = document.getElementById('mRegRefId');
  const selectedOpt = refSel?.options[refSel.selectedIndex];
  const refId = Number(refSel?.value);
  if (!refId) {
    setFieldError(form, 'participantReferenceId', 'Vui lòng chọn đối tượng đăng ký');
    return;
  }

  const unlock = lockFormSubmit(form, 'Đang gửi...');
  try {
    await apiCall('/api/v1/registrations', {
      method: 'POST',
      body: JSON.stringify({
        competitionId: Number(document.getElementById('mRegCompId').value),
        participantType: document.getElementById('mRegType').value,
        participantReferenceId: refId,
        participantName: selectedOpt?.dataset?.label || ''
      })
    });
    closeModal();
    showToast('Đã gửi hồ sơ đăng ký (trạng thái Chờ duyệt)!');
    await refreshAfterMutation({ paged: ['registrations'], reloadTournamentId: AppState.selectedTournamentId });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function withdrawRegistrationById(id, btn = null) {
  if (!confirm('Bạn có chắc chắn muốn rút hồ sơ đăng ký này?')) return;
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/registrations/${id}/withdraw`, { method: 'PUT' });
    showToast('Đã rút hồ sơ đăng ký thành công.');
    await refreshAfterMutation({ paged: ['registrations'], reloadTournamentId: AppState.selectedTournamentId });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

async function approveRegistrationById(id, btn = null) {
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/registrations/${id}/approve`, { method: 'PUT' });
    showToast('Đã phê duyệt hồ sơ đăng ký!');
    await refreshAfterMutation({ paged: ['registrations'], reloadTournamentId: AppState.selectedTournamentId });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

function openRejectRegistrationModal(id, participantName) {
  openModal(`Từ chối đăng ký: ${participantName}`, `
    <form id="rejectRegForm" onsubmit="submitRejectRegistration(event, ${id})">
      <div class="form-error-banner" hidden></div>
      <div class="form-group">
        <label for="mRejectReason">Lý do từ chối *</label>
        <input id="mRejectReason" data-field="reason" class="input" required placeholder="Nhập lý do từ chối hồ sơ" />
      </div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-danger">Xác nhận từ chối</button>
      </div>
    </form>
  `);
}

async function submitRejectRegistration(e, id) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const reason = document.getElementById('mRejectReason').value.trim();
  if (!reason) {
    setFieldError(form, 'reason', 'Vui lòng nhập lý do từ chối');
    return;
  }
  const unlock = lockFormSubmit(form, 'Đang xử lý...');
  try {
    await apiCall(`/api/v1/registrations/${id}/reject`, {
      method: 'PUT',
      body: JSON.stringify({ reason })
    });
    closeModal();
    showToast('Đã từ chối hồ sơ đăng ký.');
    await refreshAfterMutation({ paged: ['registrations'], reloadTournamentId: AppState.selectedTournamentId });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

/* ---------- MATCH & SCHEDULE CRUD + LIVE CONFLICT CHECK ---------- */
async function ensureMatchFormReferenceData() {
  const tasks = [];
  if (AppState.data.tournaments.length === 0) {
    tasks.push(safeFetchSlice('tournaments', '/api/v1/tournaments?page=0&size=50&sortBy=id&direction=asc'));
  }
  if (AppState.data.competitions.length === 0) {
    tasks.push(safeFetchSlice('competitions', '/api/v1/competitions'));
  }
  if (AppState.data.teams.length === 0) {
    tasks.push(safeFetchSlice('teams', '/api/v1/teams'));
  }
  if (hasPermission('matches.schedule') && (!Array.isArray(AppState.data.referees) || AppState.data.referees.length === 0)) {
    tasks.push(
      apiCall('/api/v1/auth/referees')
        .then(res => { if (Array.isArray(res?.data)) AppState.data.referees = res.data; })
        .catch(() => {})
    );
  }
  if (tasks.length > 0) await Promise.all(tasks);
}

async function fetchCompetitionParticipants(compId, force = false) {
  if (!compId) return [];
  const key = String(compId);
  if (!force && Array.isArray(AppState.data.participantsByComp[key])) {
    return AppState.data.participantsByComp[key];
  }
  try {
    const res = await apiCall(`/api/v1/participants/competition/${compId}`);
    const list = Array.isArray(res?.data) ? res.data : [];
    AppState.data.participantsByComp[key] = list;
    return list;
  } catch {
    return AppState.data.participantsByComp[key] || [];
  }
}

async function updateAutoGenParticipantsPreview() {
  const box = document.getElementById('autoGenParticipantsHint');
  const formatBox = document.getElementById('autoGenFormatInfo');
  const compId = document.getElementById('mGenCompId')?.value;
  if (!box) return;
  if (!compId) {
    box.innerHTML = '<span style="color:var(--color-warning);">Vui lòng chọn nội dung thi đấu.</span>';
    if (formatBox) {
      formatBox.innerHTML = '<span style="color:var(--text-secondary);">-- Chưa chọn nội dung --</span>';
    }
    return;
  }
  box.innerHTML = '<span style="color:var(--text-secondary);">Đang tải danh sách đội/VĐV đã duyệt...</span>';
  if (formatBox) {
    formatBox.innerHTML = '<span style="color:var(--text-secondary);">Đang tải cấu hình thể thức...</span>';
  }
  const [parts, fmtRes] = await Promise.all([
    fetchCompetitionParticipants(compId, true),
    apiCall(`/api/v1/competitions/${compId}/format`).catch(() => ({ data: null }))
  ]);
  if (formatBox) {
    const fmt = fmtRes?.data;
    if (!fmt || !fmt.formatType) {
      formatBox.innerHTML = '<span style="color:var(--color-error);">Chưa có cấu hình thể thức</span>';
    } else {
      const ft = String(fmt.formatType).toUpperCase();
      const labels = {
        KNOCKOUT: 'Loại trực tiếp (KNOCKOUT)',
        ROUND_ROBIN: 'Vòng tròn tính điểm (ROUND_ROBIN)',
        GROUP_STAGE: `Chia ${fmt.numberOfGroups || 1} bảng đấu (GROUP_STAGE)`,
        HYBRID: 'Hỗn hợp (HYBRID — chưa hỗ trợ xếp lịch tự động)'
      };
      const label = labels[ft] || ft;
      const extra = ft === 'KNOCKOUT' && fmt.hasThirdPlaceMatch ? ' • Có tranh hạng ba' : '';
      const color = ft === 'HYBRID' ? 'var(--color-error)' : 'var(--text-primary)';
      formatBox.innerHTML = `<span style="font-weight:600;color:${color};">${escapeHtml(label + extra)}</span>`;
    }
  }
  if (parts.length < 2) {
    box.innerHTML = `<span style="color:var(--color-error);">Nội dung hiện có ${parts.length} đội/VĐV đã duyệt — cần tối thiểu 2 để tự động xếp lịch.</span>`;
  } else {
    const names = parts.slice(0, 6).map(p => escapeHtml(p.displayName)).join(', ');
    const more = parts.length > 6 ? ` (+${parts.length - 6})` : '';
    box.innerHTML = `<span style="color:var(--color-success);">✓ Sẵn sàng xếp lịch từ ${parts.length} đội/VĐV đã duyệt: ${names}${more}</span>`;
  }
}

function openFormatFromAutoGenScheduleModal() {
  const tourId = Number(document.getElementById('mGenTourId')?.value);
  const compId = Number(document.getElementById('mGenCompId')?.value);
  if (!tourId || !compId) {
    showToast('Vui lòng chọn giải đấu và nội dung thi đấu trước.', 'warning');
    return;
  }
  closeModal();
  openCompetitionSettingsModal(tourId, compId);
}

async function openAutoGenerateScheduleModal(preselectedTourId = null) {
  if (!hasPermission('matches.schedule')) {
    showToast('Bạn không có quyền xếp lịch thi đấu', 'error');
    return;
  }
  await ensureMatchFormReferenceData();
  const d = AppState.data;
  if (d.tournaments.length === 0) {
    showToast('Cần ít nhất 1 giải đấu để tự động xếp lịch.', 'warning');
    return;
  }
  const tourId = preselectedTourId || AppState.selectedTournamentId || d.tournaments[0].id;
  const comps = d.competitions.filter(c => Number(c.tournamentId) === Number(tourId));
  if (comps.length === 0) {
    showToast('Giải đấu chưa có nội dung thi đấu nào để xếp lịch.', 'warning');
    return;
  }

  openModal('Tự động Xếp lịch Thi đấu', `
    <form id="autoGenScheduleForm" onsubmit="submitAutoGenerateSchedule(event)">
      <div class="form-error-banner" hidden></div>
      <div class="form-group">
        <label for="mGenTourId">Giải đấu áp dụng *</label>
        <select id="mGenTourId" data-field="tournamentId" class="select" onchange="syncCompetitionDropdown('mGenTourId', 'mGenCompId');updateAutoGenParticipantsPreview()">
          ${d.tournaments.map(t => `<option value="${t.id}" ${Number(t.id) === Number(tourId) ? 'selected' : ''}>${escapeHtml(t.name)}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label for="mGenCompId">Nội dung thi đấu *</label>
        <select id="mGenCompId" data-field="competitionId" class="select" required onchange="updateAutoGenParticipantsPreview()">
          ${comps.map(c => `<option value="${c.id}">${escapeHtml(c.name || c.competitionName)}</option>`).join('') || '<option value="">-- Chưa có nội dung --</option>'}
        </select>
        <div id="autoGenParticipantsHint" style="margin-top:6px;font-size:0.82rem;"></div>
      </div>
      <div class="form-grid">
        <div class="form-group">
          <label>Thể thức thi đấu (theo cấu hình nội dung)</label>
          <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;padding:8px 10px;border:1px solid var(--border-color);border-radius:var(--radius-md);background:var(--bg-muted,#f8fafc);min-height:40px;">
            <div id="autoGenFormatInfo" style="font-size:0.88rem;">Đang tải...</div>
            <button type="button" class="btn btn-secondary btn-sm" onclick="openFormatFromAutoGenScheduleModal()">⚙ Cấu hình</button>
          </div>
        </div>
        <div class="form-group">
          <label for="mGenDate">Ngày bắt đầu *</label>
          <input id="mGenDate" data-field="startDate" type="date" class="input" value="${getTodayLocalIsoDate()}" required />
        </div>
      </div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">⚡ Khởi tạo lịch</button>
      </div>
    </form>
  `);
  updateAutoGenParticipantsPreview();
}

function syncCompetitionDropdown(tourSelectId, compSelectId) {
  const tid = Number(document.getElementById(tourSelectId)?.value);
  const compSel = document.getElementById(compSelectId);
  if (!compSel) return;
  const comps = AppState.data.competitions.filter(c => Number(c.tournamentId) === tid);
  compSel.innerHTML = comps.map(c => `<option value="${c.id}">${escapeHtml(c.name || c.competitionName)}</option>`).join('')
    || '<option value="">-- Chưa có nội dung --</option>';
}

async function submitAutoGenerateSchedule(e) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const tourId = Number(document.getElementById('mGenTourId').value);
  const compVal = document.getElementById('mGenCompId').value;
  if (!compVal) {
    setFieldError(form, 'competitionId', 'Vui lòng chọn nội dung thi đấu hợp lệ');
    return;
  }

  const unlock = lockFormSubmit(form, 'Đang tạo lịch...');
  try {
    const res = await apiCall('/api/v1/matches/generate-schedule', {
      method: 'POST',
      body: JSON.stringify({
        tournamentId: tourId,
        competitionId: Number(compVal),
        startDate: document.getElementById('mGenDate').value
      })
    });
    closeModal();
    showToast(res.data.message || `Đã tự động tạo ${res.data.generatedCount} trận đấu!`);
    await refreshAfterMutation({ clearCache: ['matches'], paged: ['matches'], reloadTournamentId: tourId });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function openCreateMatchModal() {
  if (!hasPermission('matches.schedule')) {
    showToast('Bạn không có quyền tạo trận đấu', 'error');
    return;
  }
  await ensureMatchFormReferenceData();
  const d = AppState.data;
  if (d.tournaments.length === 0) {
    showToast('Cần có ít nhất 1 giải đấu trước khi tạo trận đấu.', 'warning');
    return;
  }
  const defaultTourId = AppState.selectedTournamentId || d.tournaments[0].id;
  const comps = d.competitions.filter(c => Number(c.tournamentId) === Number(defaultTourId));
  const defaultCompId = comps[0]?.id || null;
  if (defaultCompId) {
    await fetchCompetitionParticipants(defaultCompId, true);
  }
  openModal('Tạo Trận đấu mới', renderMatchFormHtml(null, {
    tournamentId: defaultTourId,
    competitionId: defaultCompId,
    status: 'SCHEDULED',
    matchTime: `${getTodayLocalIsoDate()}T18:00`
  }));
}

async function openEditMatchModal(id) {
  if (!hasPermission('matches.schedule')) {
    showToast('Bạn không có quyền chỉnh sửa lịch trận đấu', 'error');
    return;
  }
  await ensureMatchFormReferenceData();
  let m = AppState.paged.matches.items.find(x => Number(x.id) === Number(id))
    || AppState.data.matches.find(x => Number(x.id) === Number(id))
    || AppState.detail.tournament.matches.find(x => Number(x.id) === Number(id))
    || (Number(AppState.detail.match.item?.id) === Number(id) ? AppState.detail.match.item : null);
  if (!m) {
    try {
      const res = await apiCall(`/api/v1/matches/${id}`);
      m = res.data;
    } catch (err) {
      showToast(err.message || 'Không tìm thấy thông tin trận đấu', 'error');
      return;
    }
  }
  if (m.competitionId) {
    await fetchCompetitionParticipants(m.competitionId, true);
  }
  openModal(`Chỉnh sửa Trận đấu #${m.id}`, renderMatchFormHtml(m.id, m));
  setTimeout(() => checkMatchConflictLive(m.id), 50);
}

async function onMatchCompetitionChanged(editId = null) {
  const compId = document.getElementById('mMatchCompId')?.value;
  const p1Sel = document.getElementById('mMatchP1Id');
  const p2Sel = document.getElementById('mMatchP2Id');
  const hint = document.getElementById('matchParticipantsHint');
  if (!p1Sel || !p2Sel) return;
  if (!compId) {
    p1Sel.innerHTML = '<option value="">-- Chọn nội dung trước --</option>';
    p2Sel.innerHTML = '<option value="">-- Chọn nội dung trước --</option>';
    if (hint) hint.innerHTML = '';
    return;
  }
  const parts = await fetchCompetitionParticipants(compId, true);
  const opts = parts.map(p => `<option value="${p.id}" data-name="${escapeHtml(p.displayName)}">${escapeHtml(p.displayName)} (${p.participantType === 'ATHLETE' ? 'VĐV' : 'Đội'})</option>`).join('');
  p1Sel.innerHTML = `<option value="">-- Chọn Đội / VĐV 1 --</option>${opts}`;
  p2Sel.innerHTML = `<option value="">-- Chọn Đội / VĐV 2 --</option>${opts}`;
  if (hint) {
    hint.innerHTML = parts.length < 2
      ? `<span style="color:var(--color-error);">Nội dung #${escapeHtml(compId)} hiện chỉ có ${parts.length} đội/VĐV đã duyệt. Hãy duyệt hồ sơ ở tab Đăng ký thi đấu trước.</span>`
      : '';
  }
  checkMatchConflictLive(editId);
}

function onRefereeUsernameSelected(username, editId = null) {
  const uname = String(username || '').trim();
  const refInput = document.getElementById('mMatchRef');
  if (uname && refInput && Array.isArray(AppState.data.referees)) {
    const matched = AppState.data.referees.find(r => r.username && r.username.toLowerCase() === uname.toLowerCase());
    if (matched && (!refInput.value.trim() || AppState.data.referees.some(r => r.fullName === refInput.value.trim()))) {
      refInput.value = matched.fullName || matched.username;
    }
  }
  checkMatchConflictLive(editId);
}

function renderMatchFormHtml(editId, m) {
  const d = AppState.data;
  const tourId = m.tournamentId || AppState.selectedTournamentId || d.tournaments[0]?.id;
  const comps = d.competitions.filter(c => Number(c.tournamentId) === Number(tourId));
  const compId = m.competitionId || comps[0]?.id || '';
  const parts = compId ? (d.participantsByComp[String(compId)] || []) : [];
  const p1Selected = m.participant1Id || parts.find(p => p.displayName === m.participant1)?.id || '';
  const p2Selected = m.participant2Id || parts.find(p => p.displayName === m.participant2)?.id || '';
  const referees = Array.isArray(d.referees) ? d.referees : [];
  const timeVal = m.matchTime ? String(m.matchTime).slice(0, 16) : '';

  const renderParticipantOpts = (selectedId) => {
    return '<option value="">-- Chọn từ danh sách tham dự --</option>'
      + parts.map(p => `<option value="${p.id}" data-name="${escapeHtml(p.displayName)}" ${Number(p.id) === Number(selectedId) ? 'selected' : ''}>${escapeHtml(p.displayName)} (${p.participantType === 'ATHLETE' ? 'VĐV' : 'Đội'})</option>`).join('');
  };

  return `
    <form id="matchForm" onsubmit="submitMatchForm(event, ${editId ? editId : 'null'})">
      <div class="form-error-banner" hidden></div>
      <div class="form-grid">
        <div class="form-group">
          <label for="mMatchTourId">Giải đấu *</label>
          <select id="mMatchTourId" data-field="tournamentId" class="select" onchange="syncCompetitionDropdown('mMatchTourId', 'mMatchCompId');onMatchCompetitionChanged(${editId ? editId : 'null'})">
            ${d.tournaments.map(t => `<option value="${t.id}" ${Number(t.id) === Number(tourId) ? 'selected' : ''}>${escapeHtml(t.name)}</option>`).join('')}
          </select>
        </div>
        <div class="form-group">
          <label for="mMatchCompId">Nội dung thi đấu *</label>
          <select id="mMatchCompId" data-field="competitionId" class="select" required onchange="onMatchCompetitionChanged(${editId ? editId : 'null'})">
            ${comps.map(c => `<option value="${c.id}" ${Number(c.id) === Number(compId) ? 'selected' : ''}>${escapeHtml(c.name || c.competitionName)}</option>`).join('') || '<option value="">-- Chưa có nội dung --</option>'}
          </select>
        </div>
      </div>

      <datalist id="refereeUsernameSuggestions">
        ${referees.map(r => `<option value="${escapeHtml(r.username)}" label="${escapeHtml(r.fullName || r.username)}"></option>`).join('')}
      </datalist>

      <div class="form-grid">
        <div class="form-group">
          <label for="mMatchP1Id">Đội / VĐV 1 *</label>
          <select id="mMatchP1Id" data-field="participant1Id" class="select" required onchange="checkMatchConflictLive(${editId ? editId : 'null'})">
            ${renderParticipantOpts(p1Selected)}
          </select>
        </div>
        <div class="form-group">
          <label for="mMatchP2Id">Đội / VĐV 2 *</label>
          <select id="mMatchP2Id" data-field="participant2Id" class="select" required onchange="checkMatchConflictLive(${editId ? editId : 'null'})">
            ${renderParticipantOpts(p2Selected)}
          </select>
        </div>
      </div>
      <div id="matchParticipantsHint" style="margin:-4px 0 10px;font-size:0.8rem;">
        ${compId && parts.length < 2 ? `<span style="color:var(--color-error);">Nội dung #${escapeHtml(compId)} hiện chỉ có ${parts.length} đội/VĐV đã duyệt. Hãy duyệt đăng ký trước khi xếp trận.</span>` : ''}
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="mMatchRound">Vòng đấu *</label>
          <input id="mMatchRound" data-field="round" class="input" required value="${escapeHtml(m.round || '')}" placeholder="VD: Bán kết 1 / Vòng 2" />
        </div>
        <div class="form-group">
          <label for="mMatchTime">Thời gian thi đấu *</label>
          <input id="mMatchTime" data-field="matchTime" type="datetime-local" class="input" required value="${escapeHtml(timeVal)}" onchange="checkMatchConflictLive(${editId ? editId : 'null'})" />
        </div>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="mMatchLoc">Sân thi đấu *</label>
          <input id="mMatchLoc" data-field="location" class="input" required value="${escapeHtml(m.location || '')}" placeholder="VD: SVĐ Quốc Gia Mỹ Đình" onchange="checkMatchConflictLive(${editId ? editId : 'null'})" />
        </div>
        <div class="form-group">
          <label for="mMatchRefUser">Tài khoản Trọng tài (username)</label>
          <input id="mMatchRefUser" data-field="refereeUsername" list="refereeUsernameSuggestions" class="input" value="${escapeHtml(m.refereeUsername || '')}" placeholder="VD: referee1" onchange="onRefereeUsernameSelected(this.value, ${editId ? editId : 'null'})" />
        </div>
      </div>

      <div class="form-grid">
        <div class="form-group">
          <label for="mMatchRef">Tên hiển thị Trọng tài *</label>
          <input id="mMatchRef" data-field="referee" class="input" required value="${escapeHtml(m.referee || '')}" placeholder="Tên trọng tài điều hành" onchange="checkMatchConflictLive(${editId ? editId : 'null'})" />
        </div>
        ${editId ? `
          <div class="form-group">
            <label for="mMatchStatus">Trạng thái trận đấu</label>
            <select id="mMatchStatus" data-field="status" class="select">
              <option value="SCHEDULED" ${m.status === 'SCHEDULED' ? 'selected' : ''}>Chờ thi đấu</option>
              <option value="ONGOING" ${m.status === 'ONGOING' ? 'selected' : ''}>Đang diễn ra</option>
              <option value="POSTPONED" ${m.status === 'POSTPONED' ? 'selected' : ''}>Hoãn thi đấu</option>
              <option value="CANCELLED" ${m.status === 'CANCELLED' ? 'selected' : ''}>Đã hủy</option>
            </select>
          </div>
        ` : '<div></div>'}
      </div>

      <div id="conflictCheckResultBox" style="margin-bottom:12px;font-size:0.82rem;"></div>

      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="checkMatchConflictLive(${editId ? editId : 'null'})">🔍 Kiểm tra trùng lịch</button>
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">${editId ? 'Lưu thay đổi' : 'Tạo trận đấu'}</button>
      </div>
    </form>
  `;
}

function renderConflictDetailsBox(conflicts) {
  if (!Array.isArray(conflicts) || conflicts.length === 0) return '';
  return `
    <div style="padding:10px;border-radius:8px;background:var(--color-error-soft);color:var(--color-error);">
      <strong>Phát hiện ${conflicts.length} xung đột lịch:</strong><br/>
      ${conflicts.map(c => '• ' + escapeHtml(c.message || String(c))).join('<br/>')}
    </div>
  `;
}

async function checkMatchConflictLive(excludeId = null) {
  const box = document.getElementById('conflictCheckResultBox');
  const timeInput = document.getElementById('mMatchTime')?.value;
  if (!box || !timeInput) return { hasConflict: false, conflicts: [] };

  const p1Sel = document.getElementById('mMatchP1Id');
  const p2Sel = document.getElementById('mMatchP2Id');
  const p1Opt = p1Sel?.selectedOptions?.[0];
  const p2Opt = p2Sel?.selectedOptions?.[0];

  const candidate = {
    tournamentId: Number(document.getElementById('mMatchTourId')?.value || 1),
    competitionId: document.getElementById('mMatchCompId')?.value ? Number(document.getElementById('mMatchCompId').value) : null,
    participant1Id: p1Sel?.value ? Number(p1Sel.value) : null,
    participant2Id: p2Sel?.value ? Number(p2Sel.value) : null,
    participant1: p1Opt?.dataset?.name || '',
    participant2: p2Opt?.dataset?.name || '',
    location: document.getElementById('mMatchLoc')?.value.trim(),
    referee: document.getElementById('mMatchRef')?.value.trim(),
    refereeUsername: document.getElementById('mMatchRefUser')?.value.trim() || null,
    matchTime: timeInput.length === 16 ? `${timeInput}:00` : timeInput
  };

  try {
    const query = excludeId ? `?excludeId=${excludeId}` : '';
    const res = await apiCall(`/api/v1/matches/check-conflict${query}`, {
      method: 'POST',
      body: JSON.stringify(candidate)
    });
    if (res.data.hasConflict) {
      box.innerHTML = renderConflictDetailsBox(res.data.conflicts);
    } else {
      box.innerHTML = `
        <div style="padding:10px;border-radius:8px;background:var(--color-success-soft);color:var(--color-success);">
          ✓ Thời gian, sân thi đấu, trọng tài và hai đội không bị trùng lịch.
        </div>
      `;
    }
    return res.data;
  } catch (err) {
    box.innerHTML = `<div style="padding:8px;color:var(--text-secondary);">Không kiểm tra được trùng lịch: ${escapeHtml(err.message)}</div>`;
    return { hasConflict: false, conflicts: [] };
  }
}

async function submitMatchForm(e, editId) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);

  const p1Sel = document.getElementById('mMatchP1Id');
  const p2Sel = document.getElementById('mMatchP2Id');
  const p1Id = p1Sel?.value ? Number(p1Sel.value) : null;
  const p2Id = p2Sel?.value ? Number(p2Sel.value) : null;
  const p1Name = p1Sel?.selectedOptions?.[0]?.dataset?.name || '';
  const p2Name = p2Sel?.selectedOptions?.[0]?.dataset?.name || '';
  const round = document.getElementById('mMatchRound').value.trim();
  const timeInput = document.getElementById('mMatchTime').value;
  const location = document.getElementById('mMatchLoc').value.trim();
  const referee = document.getElementById('mMatchRef').value.trim();
  const refereeUsername = document.getElementById('mMatchRefUser')?.value.trim() || '';
  const compVal = document.getElementById('mMatchCompId')?.value;

  let invalid = false;
  if (!compVal) { setFieldError(form, 'competitionId', 'Vui lòng chọn nội dung thi đấu'); invalid = true; }
  if (!p1Id) { setFieldError(form, 'participant1Id', 'Vui lòng chọn Đội/VĐV 1 từ danh sách tham dự'); invalid = true; }
  if (!p2Id) { setFieldError(form, 'participant2Id', 'Vui lòng chọn Đội/VĐV 2 từ danh sách tham dự'); invalid = true; }
  if (p1Id && p2Id && p1Id === p2Id) {
    setFieldError(form, 'participant2Id', 'Hai participant tham gia trận đấu không được trùng nhau');
    invalid = true;
  }
  if (!round) { setFieldError(form, 'round', 'Vui lòng nhập vòng đấu'); invalid = true; }
  if (!timeInput) { setFieldError(form, 'matchTime', 'Vui lòng chọn thời gian thi đấu'); invalid = true; }
  if (!location) { setFieldError(form, 'location', 'Vui lòng nhập sân thi đấu'); invalid = true; }
  if (invalid) return;

  const body = {
    tournamentId: Number(document.getElementById('mMatchTourId').value),
    competitionId: Number(compVal),
    round,
    participant1Id: p1Id,
    participant2Id: p2Id,
    participant1: p1Name,
    participant2: p2Name,
    location,
    referee,
    refereeUsername: refereeUsername || null,
    matchTime: timeInput.length === 16 ? `${timeInput}:00` : timeInput,
    status: editId ? (document.getElementById('mMatchStatus')?.value || 'SCHEDULED') : 'SCHEDULED'
  };

  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    if (editId) {
      await apiCall(`/api/v1/matches/${editId}`, { method: 'PUT', body: JSON.stringify(body) });
      showToast('Đã cập nhật thông tin trận đấu!');
    } else {
      await apiCall('/api/v1/matches', { method: 'POST', body: JSON.stringify(body) });
      showToast('Đã tạo trận đấu mới thành công!');
    }
    closeModal();
    await refreshAfterMutation({
      clearCache: ['matches', 'results'],
      paged: ['matches'],
      reloadTournamentId: body.tournamentId,
      reloadMatchId: editId
    });
    renderCurrentPage(false);
  } catch (err) {
    const conflictBox = document.getElementById('conflictCheckResultBox');
    if (conflictBox && Array.isArray(err.conflicts) && err.conflicts.length > 0) {
      conflictBox.innerHTML = renderConflictDetailsBox(err.conflicts);
    }
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function patchMatchStatusOnly(matchId, status, btn = null) {
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/matches/${matchId}`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
    showToast(`Đã chuyển trạng thái trận #${matchId} sang ${getStatusLabel(status)}`);
    await refreshAfterMutation({
      clearCache: ['matches'],
      paged: ['matches'],
      reloadMatchId: matchId
    });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

function confirmDeleteMatch(id, title) {
  openConfirmDeleteModal({
    entityLabel: 'trận đấu',
    itemName: title || `#${id}`,
    warningNote: 'Trận đấu đang diễn ra hoặc đã kết thúc sẽ bị hệ thống từ chối xóa.',
    onConfirm: async () => {
      await apiCall(`/api/v1/matches/${id}`, { method: 'DELETE' });
      showToast(`Đã xóa trận đấu "${title}"`);
      if (Number(AppState.selectedMatchId) === Number(id)) {
        AppState.selectedMatchId = null;
      }
      await refreshAfterMutation({
        clearCache: ['matches'],
        paged: ['matches'],
        reloadTournamentId: AppState.selectedTournamentId
      });
      renderCurrentPage(false);
    }
  });
}

/* ---------- RESULT & RANKING ACTIONS ---------- */
async function openRecordResultModal(preselectedMatchId = null) {
  if (!hasPermission('results.submit_assigned')) {
    showToast('Bạn không có quyền nhập kết quả', 'error');
    return;
  }
  if (AppState.data.matches.length === 0) {
    await safeFetchSlice('matches', '/api/v1/matches/paged?page=0&size=50&sortBy=id&sortDir=asc');
  }
  const matches = [...AppState.data.matches];
  if (preselectedMatchId && AppState.detail.match.item
    && Number(AppState.detail.match.item.id) === Number(preselectedMatchId)
    && !matches.some(m => Number(m.id) === Number(preselectedMatchId))) {
    matches.unshift(AppState.detail.match.item);
  } else if (preselectedMatchId && !matches.some(m => Number(m.id) === Number(preselectedMatchId))) {
    try {
      const mRes = await apiCall(`/api/v1/matches/${preselectedMatchId}`);
      if (mRes?.data) matches.unshift(mRes.data);
    } catch { /* ignore */ }
  }
  if (matches.length === 0) {
    showToast('Chưa có trận đấu nào để nhập kết quả.', 'warning');
    return;
  }

  const selectedMid = Number(preselectedMatchId || AppState.selectedMatchId || matches[0].id);
  const modalSeq = ++AppState.modalSeq.resultForm;
  const cachedDetailRes = (Number(AppState.detail.match.id) === selectedMid
    && Number(AppState.detail.match.result?.matchId) === selectedMid)
    ? AppState.detail.match.result
    : null;
  let existingRes = cachedDetailRes
    || AppState.caches.resultsByMatchId[String(selectedMid)]
    || AppState.paged.results.items.find(r => Number(r.matchId) === selectedMid)
    || AppState.data.results.find(r => Number(r.matchId) === selectedMid)
    || null;
  if (existingRes && Number(existingRes.matchId) !== selectedMid) {
    existingRes = null;
  }
  if (!existingRes) {
    try {
      const rResp = await apiCall(`/api/v1/results/match/${selectedMid}`);
      if (AppState.modalSeq.resultForm !== modalSeq) return;
      const fetched = rResp?.data?.result || rResp?.data || null;
      existingRes = (fetched && Number(fetched.matchId) === selectedMid) ? fetched : null;
      if (existingRes) AppState.caches.resultsByMatchId[String(selectedMid)] = existingRes;
    } catch { /* ignore 404 */ }
  }

  openModal('Ghi nhận Tỷ số Trận đấu', `
    <form id="recordResultForm" onsubmit="submitRecordResult(event)">
      <div class="form-error-banner" hidden></div>
      <div class="form-group">
        <label for="mResMatchId">Chọn trận đấu *</label>
        <select id="mResMatchId" data-field="matchId" class="select" onchange="prefillExistingResultScores(this.value)">
          ${matches.map((m, idx) => `<option value="${m.id}" ${Number(m.id) === selectedMid ? 'selected' : ''}>Trận #${m.id || String(idx + 1).padStart(2, '0')}: ${escapeHtml(m.participant1)} vs ${escapeHtml(m.participant2)}</option>`).join('')}
        </select>
      </div>
      <div class="form-grid">
        <div class="form-group"><label for="mResS1">Điểm / Bàn Đội 1 *</label><input id="mResS1" data-field="scoreParticipant1" type="number" min="0" class="input" value="${existingRes ? existingRes.scoreParticipant1 : 0}" required /></div>
        <div class="form-group"><label for="mResS2">Điểm / Bàn Đội 2 *</label><input id="mResS2" data-field="scoreParticipant2" type="number" min="0" class="input" value="${existingRes ? existingRes.scoreParticipant2 : 0}" required /></div>
      </div>
      <div class="form-grid">
        <div class="form-group"><label for="mResPen1">Luân lưu Đội 1 (Knockout hòa)</label><input id="mResPen1" data-field="penaltyScore1" type="number" min="0" class="input" value="${existingRes?.penaltyScore1 ?? ''}" placeholder="Bỏ trống nếu không luân lưu" /></div>
        <div class="form-group"><label for="mResPen2">Luân lưu Đội 2 (Knockout hòa)</label><input id="mResPen2" data-field="penaltyScore2" type="number" min="0" class="input" value="${existingRes?.penaltyScore2 ?? ''}" placeholder="Bỏ trống nếu không luân lưu" /></div>
      </div>
      <div class="form-group"><label for="mResNotes">Ghi chú biên bản</label><input id="mResNotes" data-field="notes" class="input" value="${escapeHtml(existingRes?.notes || '')}" placeholder="Diễn biến chính / ghi chú trọng tài" /></div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">Lưu kết quả</button>
      </div>
    </form>
  `);
}

async function prefillExistingResultScores(matchId) {
  const mid = Number(matchId);
  const seq = ++AppState.modalSeq.resultForm;
  const s1 = document.getElementById('mResS1');
  const s2 = document.getElementById('mResS2');
  const pen1 = document.getElementById('mResPen1');
  const pen2 = document.getElementById('mResPen2');
  const notes = document.getElementById('mResNotes');

  // Clear previous match inputs immediately so old match score never persists if fetch fails
  if (s1) s1.value = 0;
  if (s2) s2.value = 0;
  if (pen1) pen1.value = '';
  if (pen2) pen2.value = '';
  if (notes) notes.value = '';

  let existing = AppState.caches.resultsByMatchId[String(mid)]
    || AppState.paged.results.items.find(r => Number(r.matchId) === mid)
    || AppState.data.results.find(r => Number(r.matchId) === mid)
    || null;
  if (existing && Number(existing.matchId) !== mid) {
    existing = null;
  }
  if (!existing) {
    try {
      const rResp = await apiCall(`/api/v1/results/match/${mid}`);
      if (AppState.modalSeq.resultForm !== seq) return;
      const fetched = rResp?.data?.result || rResp?.data || null;
      if (fetched && Number(fetched.matchId) === mid) {
        existing = fetched;
        AppState.caches.resultsByMatchId[String(mid)] = existing;
      }
    } catch { /* 404 or error -> keep 0/empty */ }
  }
  if (AppState.modalSeq.resultForm !== seq) return;
  const activeMid = Number(document.getElementById('mResMatchId')?.value || mid);
  if (activeMid !== mid) return;
  if (s1) s1.value = existing ? existing.scoreParticipant1 : 0;
  if (s2) s2.value = existing ? existing.scoreParticipant2 : 0;
  if (pen1) pen1.value = existing?.penaltyScore1 ?? '';
  if (pen2) pen2.value = existing?.penaltyScore2 ?? '';
  if (notes) notes.value = existing?.notes || '';
}

async function submitRecordResult(e) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);

  const s1 = Number(document.getElementById('mResS1').value);
  const s2 = Number(document.getElementById('mResS2').value);
  const rawPen1 = document.getElementById('mResPen1')?.value ?? '';
  const rawPen2 = document.getElementById('mResPen2')?.value ?? '';
  const pen1 = rawPen1 !== '' ? Number(rawPen1) : null;
  const pen2 = rawPen2 !== '' ? Number(rawPen2) : null;
  if (s1 < 0 || s2 < 0 || (pen1 !== null && pen1 < 0) || (pen2 !== null && pen2 < 0)) {
    if (s1 < 0) setFieldError(form, 'scoreParticipant1', 'Tỷ số không được âm');
    if (s2 < 0) setFieldError(form, 'scoreParticipant2', 'Tỷ số không được âm');
    return;
  }

  const matchId = Number(document.getElementById('mResMatchId').value);
  const unlock = lockFormSubmit(form, 'Đang lưu...');
  try {
    const res = await apiCall('/api/v1/results', {
      method: 'POST',
      body: JSON.stringify({
        matchId,
        scoreParticipant1: s1,
        scoreParticipant2: s2,
        penaltyScore1: pen1,
        penaltyScore2: pen2,
        notes: document.getElementById('mResNotes').value.trim()
      })
    });
    const saved = res?.data?.result || res?.data || null;
    if (saved && Number(saved.matchId) === matchId) {
      AppState.caches.resultsByMatchId[String(matchId)] = saved;
    }
    closeModal();
    showToast('Đã ghi nhận kết quả (trạng thái Chờ xác nhận)!');
    await refreshAfterMutation({
      clearCache: ['results'],
      paged: ['results'],
      reloadMatchId: matchId,
      reloadTournamentId: AppState.selectedTournamentId
    });
    renderCurrentPage(false);
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

async function reopenResultById(resultId, matchId = null, btn = null) {
  if (!hasPermission('results.approve')) {
    showToast('Chỉ Ban tổ chức sở hữu giải hoặc Admin mới được mở lại kết quả', 'error');
    return;
  }
  if (!confirm('Mở lại kết quả đã xác nhận? Kết quả sẽ chuyển về Chờ xác nhận và BXH sẽ được tính lại.')) return;
  if (btn) btn.disabled = true;
  try {
    const resp = await apiCall(`/api/v1/results/${resultId}`, {
      method: 'PATCH',
      body: JSON.stringify({ status: 'PENDING_CONFIRMATION' })
    });
    const saved = resp?.data || null;
    if (saved && saved.matchId) {
      AppState.caches.resultsByMatchId[String(saved.matchId)] = saved;
    }
    showToast('Đã mở lại kết quả sang Chờ xác nhận và cập nhật BXH.');
    await refreshAfterMutation({
      clearCache: ['results', 'rankings'],
      paged: ['results', 'rankingItems'],
      reloadMatchId: matchId || saved?.matchId,
      reloadTournamentId: AppState.selectedTournamentId
    });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

async function confirmMatchResult(resultId, btn = null) {
  if (!hasPermission('results.approve')) {
    showToast('Bạn không có quyền xác nhận chính thức kết quả', 'error');
    return;
  }
  if (btn) btn.disabled = true;
  try {
    const resp = await apiCall(`/api/v1/results/${resultId}/confirm`, {
      method: 'PUT',
      body: JSON.stringify({})
    });
    const saved = resp?.data?.result || resp?.data || null;
    if (saved && saved.matchId) {
      AppState.caches.resultsByMatchId[String(saved.matchId)] = saved;
    }
    const syncStatus = String(saved?.syncStatus || 'SYNCED').toUpperCase();
    if (syncStatus === 'SYNCED') {
      showToast('Đã xác nhận kết quả chính thức và đồng bộ hoàn tất Trận đấu & BXH!');
    } else if (syncStatus === 'PENDING') {
      showToast('Đã lưu xác nhận kết quả (# ' + resultId + '), hệ thống đang chờ đồng bộ Trận đấu & BXH.', 'warning');
    } else {
      showToast(
        `Đã lưu xác nhận kết quả (#${resultId}), NHƯNG đồng bộ liên dịch vụ thất bại (${saved?.syncError || 'Lỗi dịch vụ'}). Bạn có thể nhấn "Đồng bộ lại".`,
        'error'
      );
    }
    await refreshAfterMutation({
      clearCache: ['results', 'rankings'],
      paged: ['results'],
      reloadMatchId: saved?.matchId || AppState.selectedMatchId,
      reloadTournamentId: AppState.selectedTournamentId
    });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

async function retrySyncResultNow(resultId, matchId = null, btn = null) {
  if (!hasPermission('results.approve')) {
    showToast('Bạn không có quyền đồng bộ lại kết quả', 'error');
    return;
  }
  if (btn) btn.disabled = true;
  try {
    const resp = await apiCall(`/api/v1/results/${resultId}/sync`, { method: 'POST' });
    const saved = resp?.data?.result || resp?.data || null;
    if (saved && saved.matchId) {
      AppState.caches.resultsByMatchId[String(saved.matchId)] = saved;
    }
    const syncState = String(saved?.syncStatus || '').toUpperCase();
    if (syncState === 'SYNCED') {
      showToast('Đã đồng bộ thành công trạng thái Trận đấu & Bảng xếp hạng!');
    } else if (resp?.status === 202 || syncState === 'PENDING') {
      showToast('Kết quả này đang được đồng bộ ở tiến trình khác, vui lòng tải lại sau giây lát.', 'warning');
    } else {
      showToast(`Trạng thái đồng bộ hiện tại: ${saved?.syncError || syncState}`, 'error');
    }
    await refreshAfterMutation({
      clearCache: ['results', 'rankings'],
      paged: ['results'],
      reloadMatchId: matchId || saved?.matchId || AppState.selectedMatchId,
      reloadTournamentId: AppState.selectedTournamentId
    });
    renderCurrentPage(false);
  } catch (err) {
    showToast(`Đồng bộ lại chưa thành công: ${err.message}`, 'error');
    await refreshAfterMutation({
      clearCache: ['results'],
      paged: ['results'],
      reloadMatchId: matchId || AppState.selectedMatchId
    });
    renderCurrentPage(false);
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

async function recalculateStandingsNow(btn = null) {
  if (!hasPermission('results.approve') && !hasPermission('tournaments.manage')) {
    showToast('Bạn không có quyền tính toán lại bảng xếp hạng', 'error');
    return;
  }
  const cid = AppState.selectedCompetitionId || AppState.data.competitions[0]?.id;
  if (!cid) {
    showToast('Không có nội dung thi đấu để tính lại BXH.', 'warning');
    return;
  }
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/rankings/competition/${cid}/recalculate`, { method: 'POST' });
    showToast('Đã tính toán lại Bảng xếp hạng từ kết quả đã xác nhận!');
    await refreshAfterMutation({
      clearCache: ['rankings'],
      paged: ['rankings', 'rankingItems']
    });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

/* ---------- NOTIFICATION & USER MANAGEMENT ACTIONS ---------- */
async function markNotificationRead(id, btn = null) {
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/notifications/${id}/read`, { method: 'PATCH' });
    await refreshAfterMutation({ paged: ['notifications'] });
    renderCurrentPage(false);
  } catch (err) {
    showToast(err.message, 'error');
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

async function changeUserRoleById(id, role) {
  if (!hasPermission('roles.manage')) {
    showToast('Bạn không có quyền thay đổi vai trò người dùng', 'error');
    return;
  }
  try {
    await apiCall(`/api/v1/auth/users/${id}/role`, { method: 'PUT', body: JSON.stringify({ role }) });
    showToast(`Đã cập nhật vai trò người dùng sang ${getRoleDisplayName(role)}`);
    await fetchPagedResource('users', updateUsersRegion);
  } catch (err) {
    showToast(err.message, 'error');
    await fetchPagedResource('users', updateUsersRegion);
  }
}

async function toggleUserActiveStatus(id, active, btn = null) {
  if (!hasPermission('users.manage')) {
    showToast('Bạn không có quyền khóa/mở tài khoản', 'error');
    return;
  }
  if (btn) btn.disabled = true;
  try {
    await apiCall(`/api/v1/auth/users/${id}/status`, { method: 'PATCH', body: JSON.stringify({ active }) });
    showToast(active ? 'Đã mở khóa tài khoản' : 'Đã khóa tài khoản');
    await fetchPagedResource('users', updateUsersRegion);
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    if (btn && document.contains(btn)) btn.disabled = false;
  }
}

function openChangePasswordModal() {
  if (!AppState.user) {
    showToast('Vui lòng đăng nhập trước khi đổi mật khẩu', 'error');
    navigateTo('auth-page');
    return;
  }
  openModal('Đổi mật khẩu Tài khoản', `
    <form id="changePasswordForm" onsubmit="submitChangePassword(event)">
      <div class="form-error-banner" hidden></div>
      <div class="form-group" style="margin-bottom:10px;">
        <span style="font-size:0.86rem;color:var(--text-secondary);">Tài khoản đang đăng nhập: <strong>${escapeHtml(AppState.user.username || '')}</strong></span>
      </div>
      <div class="form-group"><label for="mCpCurrent">Mật khẩu hiện tại *</label><input id="mCpCurrent" data-field="currentPassword" type="password" class="input" required autocomplete="current-password" placeholder="Nhập mật khẩu hiện tại" /></div>
      <div class="form-group"><label for="mCpNew">Mật khẩu mới *</label><input id="mCpNew" data-field="newPassword" type="password" class="input" required minlength="6" autocomplete="new-password" placeholder="Tối thiểu 6 ký tự" /></div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">Cập nhật mật khẩu</button>
      </div>
    </form>
  `);
}

async function submitChangePassword(e) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const currentPassword = document.getElementById('mCpCurrent').value;
  const newPassword = document.getElementById('mCpNew').value;
  if (!currentPassword || !currentPassword.trim()) {
    setFieldError(form, 'currentPassword', 'Vui lòng nhập mật khẩu hiện tại');
    return;
  }
  if (newPassword.length < 6) {
    setFieldError(form, 'newPassword', 'Mật khẩu mới phải có ít nhất 6 ký tự');
    return;
  }
  const unlock = lockFormSubmit(form, 'Đang cập nhật...');
  try {
    await apiCall('/api/v1/auth/change-password', {
      method: 'PUT',
      body: JSON.stringify({
        currentPassword,
        newPassword
      })
    });
    closeModal();
    showToast('Đổi mật khẩu thành công!');
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

function openAdminResetPasswordModal(userId, username) {
  if (!hasPermission('users.manage')) {
    showToast('Chỉ Quản trị viên mới có quyền đặt lại mật khẩu người dùng khác', 'error');
    return;
  }
  openModal(`Đặt lại mật khẩu: ${username}`, `
    <form id="adminResetPasswordForm" onsubmit="submitAdminResetPassword(event, ${userId})">
      <div class="form-error-banner" hidden></div>
      <div class="form-group"><label for="mResetPwNew">Mật khẩu mới *</label><input id="mResetPwNew" data-field="newPassword" type="password" class="input" required minlength="6" autocomplete="new-password" placeholder="Tối thiểu 6 ký tự" /></div>
      <div class="page-actions" style="justify-content:flex-end;">
        <button type="button" class="btn btn-secondary" onclick="closeModal()">Hủy</button>
        <button type="submit" class="btn btn-primary">Đặt lại mật khẩu</button>
      </div>
    </form>
  `);
}

async function submitAdminResetPassword(e, userId) {
  e.preventDefault();
  const form = e.target;
  clearFormErrors(form);
  const newPassword = document.getElementById('mResetPwNew').value;
  if (newPassword.length < 6) {
    setFieldError(form, 'newPassword', 'Mật khẩu mới phải có ít nhất 6 ký tự');
    return;
  }
  const unlock = lockFormSubmit(form, 'Đang đặt lại...');
  try {
    await apiCall(`/api/v1/auth/users/${userId}/password`, {
      method: 'PUT',
      body: JSON.stringify({ newPassword })
    });
    closeModal();
    showToast('Đã đặt lại mật khẩu cho người dùng!');
  } catch (err) {
    showFormError(form, err);
  } finally {
    unlock();
  }
}

/* ---------- EXPORTS & TECHNICAL DEMO TESTER ---------- */
async function exportStandingsReportCSV() {
  try {
    const tasks = [safeFetchSlice('rankings', '/api/v1/rankings/details', (d) => Array.isArray(d) ? d : [])];
    if (AppState.data.sports.length === 0) tasks.push(safeFetchSlice('sports', '/api/v1/sports'));
    if (AppState.data.competitions.length === 0) tasks.push(safeFetchSlice('competitions', '/api/v1/competitions'));
    if (AppState.data.tournaments.length === 0) {
      tasks.push(safeFetchSlice('tournaments', '/api/v1/tournaments?page=0&size=50&sortBy=id&direction=asc'));
    }
    await Promise.all(tasks);
  } catch { /* fallback to currently loaded data if network hiccups */ }

  const groups = getDynamicRankingsGroups(AppState.selectedSportId || 'ALL', AppState.selectedRankKeyword || '');
  if (groups.length === 0) {
    showToast('Không có dữ liệu bảng xếp hạng để xuất CSV.', 'warning');
    return;
  }
  let csvContent = '\uFEFF';
  groups.forEach(g => {
    csvContent += `"${g.competitionName} (${g.tournamentName})"\n`;
    csvContent += 'Hang,Doi/VDV,Tran,Thang,Hoa,Thua,Ghi diem,Thung luoi,Hieu so,Diem\n';
    g.items.forEach(item => {
      const row = [
        item.rankPosition ?? '',
        `"${String(item.participantName || '').replace(/"/g, '""')}"`,
        item.matchesPlayed ?? 0,
        item.won ?? 0,
        item.drawn ?? 0,
        item.lost ?? 0,
        item.goalsFor ?? 0,
        item.goalsAgainst ?? 0,
        item.goalDifference ?? 0,
        item.points ?? 0
      ];
      csvContent += row.join(',') + '\n';
    });
    csvContent += '\n';
  });

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = `Bang_Xep_Hang_Sports_SOA.csv`;
  a.click();
  showToast('Đã xuất file Bang_Xep_Hang_Sports_SOA.csv!');
}

function triggerJsonDownload(filename, payload) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
}

async function exportFullSystemReportJSON(btn = null, allowPartial = false) {
  if (!hasPermission('reports.read_all')) {
    showToast('Bạn không có quyền xuất báo cáo toàn hệ thống', 'error');
    return;
  }
  if (AppState.exportingSystemReport) {
    return;
  }
  const targetBtn = btn || document.getElementById('btnExportSystemJson');
  const origText = targetBtn ? targetBtn.textContent : '';
  AppState.exportingSystemReport = true;
  if (targetBtn) {
    targetBtn.disabled = true;
    targetBtn.textContent = 'Đang xuất báo cáo...';
  }
  const banner = document.getElementById('systemExportBanner');
  if (banner) {
    banner.innerHTML = `<div class="state-banner loading" role="status"><span>Đang truy vấn đầy đủ các dịch vụ hệ thống để tạo báo cáo...</span></div>`;
  }

  try {
    const query = allowPartial ? '?allowPartial=true' : '';
    const resp = await apiCall(`/api/v1/reports/system-export${query}`);
    const report = resp.data || {};
    const dateTag = String(report.generatedAt || new Date().toISOString()).slice(0, 10);
    const filename = report.partial
      ? `Bao_Cao_He_Thong_Mot_Phan_${dateTag}.json`
      : `Bao_Cao_He_Thong_Sports_SOA_${dateTag}.json`;

    triggerJsonDownload(filename, report);
    closeModal();

    if (report.partial) {
      const missingNames = (report.missingSources || []).map(m => m.source).join(', ');
      if (banner) {
        banner.innerHTML = `
          <div class="state-banner error" role="alert">
            <span>Đã xuất <strong>báo cáo một phần</strong> (thiếu nguồn: ${escapeHtml(missingNames)}).</span>
            <button type="button" class="btn btn-secondary btn-sm" onclick="exportFullSystemReportJSON(null, false)">↻ Thử xuất lại đầy đủ</button>
          </div>
        `;
      }
      showToast(`Đã tải báo cáo một phần (thiếu: ${missingNames})`, 'warning');
    } else {
      if (banner) banner.innerHTML = '';
      showToast(`Đã xuất báo cáo toàn hệ thống đầy đủ (${filename})!`);
    }
    return report;
  } catch (err) {
    const missing = Array.isArray(err?.body?.missingSources) ? err.body.missingSources : [];
    if (banner) {
      banner.innerHTML = `
        <div class="state-banner error" role="alert">
          <div>
            <strong>Xuất báo cáo toàn hệ thống thất bại:</strong> ${escapeHtml(err.message)}
            ${missing.length > 0 ? `<div style="margin-top:4px;font-size:0.82rem;">Nguồn bị thiếu: ${escapeHtml(missing.map(m => m.source).join(', '))}</div>` : ''}
          </div>
          <div class="page-actions">
            <button type="button" class="btn btn-secondary btn-sm" onclick="exportFullSystemReportJSON(null, false)">↻ Thử lại</button>
            ${missing.length > 0 ? `<button type="button" class="btn btn-secondary btn-sm" onclick="exportFullSystemReportJSON(null, true)">Xuất phần đã lấy được</button>` : ''}
          </div>
        </div>
      `;
    }
    if (missing.length > 0) {
      openModal('Xuất báo cáo chưa đầy đủ', `
        <div class="form-error-banner" style="display:block;margin-bottom:12px;">
          Không thể tạo báo cáo toàn hệ thống hoàn chỉnh vì một số dịch vụ đang lỗi:
        </div>
        <ul style="margin:0 0 16px 18px;font-size:0.88rem;line-height:1.6;">
          ${missing.map(m => `<li><strong>${escapeHtml(m.source)}:</strong> ${escapeHtml(m.reason || 'Lỗi dịch vụ')}</li>`).join('')}
        </ul>
        <p style="font-size:0.84rem;color:var(--text-secondary);margin-bottom:16px;">
          Hệ thống không tự tải tệp thiếu mà báo thành công. Bạn có thể thử xuất lại hoặc chủ động xuất bản ghi nhận <code>exportStatus: "PARTIAL"</code>.
        </p>
        <div class="page-actions" style="justify-content:flex-end;">
          <button type="button" class="btn btn-secondary" onclick="closeModal()">Đóng</button>
          <button type="button" class="btn btn-secondary" onclick="exportFullSystemReportJSON(null, true)">Xuất báo cáo một phần</button>
          <button type="button" class="btn btn-primary" onclick="closeModal();exportFullSystemReportJSON(null, false)">↻ Thử xuất lại</button>
        </div>
      `);
    }
    showToast(err.message || 'Xuất báo cáo toàn hệ thống thất bại', 'error');
    return null;
  } finally {
    AppState.exportingSystemReport = false;
    if (targetBtn && document.contains(targetBtn)) {
      targetBtn.disabled = false;
      targetBtn.textContent = origText || '⬇ Xuất dữ liệu (.JSON)';
    }
  }
}

async function runLoadBalancingLiveTest(btn = null) {
  if (!hasPermission('services.monitor')) {
    showToast('Bạn không có quyền kiểm thử hạ tầng SOA', 'error');
    return;
  }
  const box = document.getElementById('lbTestOutputBox');
  if (!box) return;
  if (btn) btn.disabled = true;
  box.innerHTML = 'Đang gửi 6 request qua Eureka LoadBalancer...';
  const logs = [];
  const counts = {};
  for (let i = 1; i <= 6; i++) {
    try {
      const r = await apiCall('/api/v1/matches/discovery/tournament-instance');
      const inst = r.data.targetServiceInstance || r.data.providerInstance || {};
      const key = `${inst.instanceId || ('port:' + (inst.instancePort || inst.port || '?'))}`;
      counts[key] = (counts[key] || 0) + 1;
      logs.push(`Request #${i} → <strong>${escapeHtml(key)}</strong>`);
    } catch (e) {
      logs.push(`Request #${i} → Lỗi: ${escapeHtml(e.message)}`);
    }
  }
  const summary = Object.entries(counts).map(([k, v]) => `${k}: ${v} requests`).join(' | ');
  box.innerHTML = `<div style="color:var(--color-success);margin-bottom:6px;"><strong>Kết quả: ${escapeHtml(summary || 'Không có instance')}</strong></div>` + logs.join('<br/>');
  if (btn && document.contains(btn)) btn.disabled = false;
}

/* ---------- BOOTSTRAP ---------- */
document.addEventListener('DOMContentLoaded', async () => {
  applyTheme(AppState.themeMode);

  await loadServerDemoConfig();

  await initAuthState();
  updateHeaderUserUI();
  renderSidebar();

  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    if (AppState.themeMode === 'system') applyTheme('system');
  });

  document.querySelectorAll('.theme-btn').forEach(btn => {
    btn.addEventListener('click', () => applyTheme(btn.dataset.themeChoice));
  });

  document.getElementById('mobileMenuBtn')?.addEventListener('click', () => toggleMobileSidebar());
  document.getElementById('sidebarScrim')?.addEventListener('click', () => toggleMobileSidebar(false));
  document.getElementById('headerUserBtn')?.addEventListener('click', () => navigateTo('auth-page'));

  document.getElementById('modalCloseBtn')?.addEventListener('click', closeModal);
  document.getElementById('appModalBackdrop')?.addEventListener('click', (e) => {
    if (e.target.id === 'appModalBackdrop') closeModal();
  });

  // Global keyboard handler: Modal Escape + Focus Trap + Mobile Drawer Escape
  document.addEventListener('keydown', (e) => {
    const backdrop = document.getElementById('appModalBackdrop');
    if (backdrop && !backdrop.hidden) {
      if (e.key === 'Escape') {
        e.preventDefault();
        closeModal();
        return;
      }
      if (e.key === 'Tab') {
        const dialog = backdrop.querySelector('.modal-dialog');
        const focusables = getFocusableElements(dialog);
        if (focusables.length > 0) {
          const first = focusables[0];
          const last = focusables[focusables.length - 1];
          if (e.shiftKey && (document.activeElement === first || !dialog.contains(document.activeElement))) {
            e.preventDefault();
            last.focus();
          } else if (!e.shiftKey && document.activeElement === last) {
            e.preventDefault();
            first.focus();
          }
        }
      }
      return;
    }

    if (e.key === 'Escape') {
      toggleMobileSidebar(false);
      closeGlobalSearchDropdown();
    }
  });

  // Global Search input & keyboard accessibility
  const searchInput = document.getElementById('globalSearchInput');
  const searchContainer = document.getElementById('globalSearchContainer');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => handleGlobalSearchInput(e.target.value));
    searchInput.addEventListener('focus', () => {
      if (searchInput.value.trim()) {
        handleGlobalSearchInput(searchInput.value);
      }
    });
    searchInput.addEventListener('keydown', (e) => {
      const dd = document.getElementById('globalSearchDropdown');
      const items = dd ? Array.from(dd.querySelectorAll('.search-result-item')) : [];
      if (e.key === 'ArrowDown' && items.length > 0) {
        e.preventDefault();
        AppState.globalSearchActiveIdx = (AppState.globalSearchActiveIdx + 1) % items.length;
        items.forEach((el, i) => el.classList.toggle('active', i === AppState.globalSearchActiveIdx));
      } else if (e.key === 'ArrowUp' && items.length > 0) {
        e.preventDefault();
        AppState.globalSearchActiveIdx = (AppState.globalSearchActiveIdx - 1 + items.length) % items.length;
        items.forEach((el, i) => el.classList.toggle('active', i === AppState.globalSearchActiveIdx));
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (AppState.globalSearchActiveIdx >= 0 && items[AppState.globalSearchActiveIdx]) {
          items[AppState.globalSearchActiveIdx].click();
        } else if (items[0]) {
          items[0].click();
        } else if (searchInput.value.trim()) {
          handleGlobalSearchInput(searchInput.value);
        }
      }
    });
  }

  document.addEventListener('click', (e) => {
    if (searchContainer && !searchContainer.contains(e.target)) {
      closeGlobalSearchDropdown();
    }
  });

  await loadCoreStats();
  if (AppState.currentPage === 'auth-page') {
    await navigateTo('auth-page');
  } else {
    await navigateTo(AppState.currentPage);
  }
});
