(function () {
  'use strict';

  var RBAC = window.RBAC;
  var AdminPage = window.AdminPage;

  var state = {
    roles: [],
    permissions: [],
    editing: null,
    permissionRole: null,
    orgId: '',
    orgs: []
  };

  function permissionModule(code) {
    return RBAC.permissionModule(code);
  }

  function el(id) {
    return document.getElementById(id);
  }

  document.addEventListener('DOMContentLoaded', function () {
    AdminPage.init({ permission: 'role:view' }).then(function (user) {
      if (!user || !AdminPage.can('role:view')) {
        return;
      }
      init();
    });
  });

  function init() {
    AdminPage.bindModalClose();
    bindEvents();
    if (AdminPage.isSuperAdmin()) {
      loadOrgOptions().then(loadAll);
    } else {
      var filterForm = el('roleFilterForm');
      if (filterForm) {
        filterForm.hidden = true;
      }
      loadAll();
    }
  }

  function bindEvents() {
    el('refreshBtn').addEventListener('click', loadAll);
    var filterForm = el('roleFilterForm');
    if (filterForm) {
      filterForm.addEventListener('submit', function (event) {
        event.preventDefault();
        state.orgId = el('roleOrgFilter').value;
        loadAll();
      });
    }
    var resetFilterBtn = el('resetRoleFilterBtn');
    if (resetFilterBtn) {
      resetFilterBtn.addEventListener('click', function () {
        el('roleOrgFilter').value = '';
        state.orgId = '';
        loadAll();
      });
    }
    var orgFilter = el('roleOrgFilter');
    if (orgFilter) {
      orgFilter.addEventListener('change', function () {
        state.orgId = orgFilter.value;
        loadAll();
      });
    }
    el('createBtn').addEventListener('click', openCreate);
    el('roleForm').addEventListener('submit', submitRole);
    el('permissionSearch').addEventListener('input', filterPermissions);
    el('selectAllPermissionsBtn').addEventListener('click', function () {
      setVisiblePermissions(true);
    });
    el('clearAllPermissionsBtn').addEventListener('click', function () {
      setVisiblePermissions(false);
    });
    el('saveRolePermissionsBtn').addEventListener('click', saveRolePermissions);
    el('permissionGroups').addEventListener('change', function (event) {
      if (!event.target.matches('input[type="checkbox"]')) {
        return;
      }
      var moduleKey = event.target.getAttribute('data-module-toggle');
      if (moduleKey) {
        setModulePermissions(moduleKey, event.target.checked);
      }
      updateSelectedCount();
      syncModuleToggles();
    });
    el('roleTableBody').addEventListener('click', function (event) {
      var button = event.target.closest('[data-action]');
      if (!button || button.disabled) {
        return;
      }
      var id = Number(button.dataset.id);
      var role = state.roles.filter(function (item) {
        return Number(item.id) === id;
      })[0];
      if (!role) {
        return;
      }
      if (button.dataset.action === 'edit') {
        openEdit(role);
      } else if (button.dataset.action === 'permissions') {
        openPermissions(role);
      } else if (button.dataset.action === 'delete') {
        removeRole(role);
      }
    });
  }

  async function loadOrgOptions() {
    try {
      var result = await RBAC.api('/api/organizations?page=0&size=100');
      state.orgs = result && Array.isArray(result.items) ? result.items : [];
      var select = el('roleOrgFilter');
      if (!select) {
        return;
      }
      select.innerHTML = '<option value="">当前组织</option>' + state.orgs.map(function (org) {
        return '<option value="' + org.id + '">'
          + RBAC.escapeHtml(org.orgName) + '（' + RBAC.escapeHtml(org.orgCode) + '）</option>';
      }).join('');
    } catch (e) {
      RBAC.toast('组织列表加载失败：' + (e.message || '未知错误'), 'error');
    }
  }

  async function loadAll() {
    el('listSummary').textContent = '加载中...';
    var rolesUrl = '/api/roles' + (state.orgId ? ('?orgId=' + encodeURIComponent(state.orgId)) : '');
    var rolesRequest = RBAC.api(rolesUrl).then(
      function (data) {
        return { data: data };
      },
      function (error) {
        return { error: error };
      }
    );
    var permissionsRequest = RBAC.api('/api/permissions').then(
      function (data) {
        return { data: data };
      },
      function (error) {
        return { error: error };
      }
    );
    var results = await Promise.all([rolesRequest, permissionsRequest]);

    var rolesResult = results[0];
    var rolesOk = !rolesResult.error;
    state.roles = rolesOk && Array.isArray(rolesResult.data) ? rolesResult.data : [];
    if (!rolesOk) {
      renderError(rolesResult.error.message);
    }

    var permissionsResult = results[1];
    state.permissions = !permissionsResult.error && Array.isArray(permissionsResult.data)
      ? permissionsResult.data
      : [];
    if (permissionsResult.error && rolesOk) {
      RBAC.toast('权限列表加载失败：' + permissionsResult.error.message, 'error');
    }

    if (rolesOk) {
      renderTable();
    }
  }

  function isSystemRole(role) {
    var code = role.roleCode || '';
    return code === 'SUPER_ADMIN'
      || code.indexOf('ORG_ADMIN_') === 0
      || code.indexOf('ORG_USER_') === 0;
  }

  function orgLabel(role) {
    if (role.orgId === null || typeof role.orgId === 'undefined') {
      return '全局';
    }
    var org = state.orgs.filter(function (item) {
      return String(item.id) === String(role.orgId);
    })[0];
    return org ? org.orgName : ('#' + role.orgId);
  }

  function renderTable() {
    var body = el('roleTableBody');
    if (state.roles.length === 0) {
      body.innerHTML = '<tr><td colspan="8"><div class="table-state table-empty"><strong>暂无角色</strong><span>当前组织还没有可用角色。</span></div></td></tr>';
      el('listSummary').textContent = '共 0 个角色';
      el('pageInfo').textContent = '共 0 个角色';
      return;
    }

    var canUpdate = AdminPage.can('role:update');
    var canAssign = AdminPage.can('role:assign-permission');
    var canDelete = AdminPage.can('role:delete');

    body.innerHTML = state.roles.map(function (role) {
      var permissions = Array.isArray(role.permissions) ? role.permissions : [];
      var actions = '';
      if (canAssign) {
        actions += '<button class="mini-btn" type="button" data-action="permissions" data-id="' + role.id + '">权限</button>';
      }
      if (canUpdate) {
        actions += '<button class="mini-btn" type="button" data-action="edit" data-id="' + role.id + '">编辑</button>';
      }
      if (canDelete && !isSystemRole(role)) {
        actions += '<button class="mini-btn is-danger" type="button" data-action="delete" data-id="' + role.id + '">删除</button>';
      }
      if (!actions) {
        actions = '<span class="muted-value">只读</span>';
      }

      return '<tr>'
        + '<td><span class="code-pill">' + RBAC.escapeHtml(role.roleCode) + '</span></td>'
        + '<td><strong>' + RBAC.escapeHtml(role.roleName) + '</strong></td>'
        + '<td class="muted-value">' + RBAC.escapeHtml(orgLabel(role)) + '</td>'
        + '<td class="muted-value">' + RBAC.escapeHtml(RBAC.shortText(role.description, 40)) + '</td>'
        + '<td><span class="count-pill">' + permissions.length + '</span></td>'
        + '<td>' + (isSystemRole(role)
          ? '<span class="role-badge role-badge-system">系统</span>'
          : '<span class="role-badge">自定义</span>') + '</td>'
        + '<td class="muted-value">' + RBAC.formatDateTime(role.createTime) + '</td>'
        + '<td class="td-actions"><div class="row-actions">' + actions + '</div></td>'
        + '</tr>';
    }).join('');

    el('listSummary').textContent = '共 ' + state.roles.length + ' 个角色';
    el('pageInfo').textContent = '共 ' + state.roles.length + ' 个角色';
  }

  function renderError(message) {
    el('roleTableBody').innerHTML = '<tr><td colspan="8"><div class="table-state table-empty"><strong>加载失败</strong><span>'
      + RBAC.escapeHtml(message) + '</span><button class="btn btn-ghost btn-sm" type="button" onclick="location.reload()">重新加载</button></div></td></tr>';
    el('listSummary').textContent = '加载失败';
    el('pageInfo').textContent = '加载失败';
  }

  function openCreate() {
    state.editing = null;
    el('roleForm').reset();
    el('roleId').value = '';
    el('roleCode').disabled = false;
    el('roleModalTitle').textContent = '新建角色';
    var hint = '角色编码创建后不可修改。';
    if (AdminPage.isSuperAdmin() && state.orgId) {
      var selectedOrg = state.orgs.filter(function (org) {
        return String(org.id) === String(state.orgId);
      })[0];
      hint = '新角色将创建到“' + (selectedOrg ? selectedOrg.orgName : ('#' + state.orgId)) + '”。' + hint;
    }
    el('roleModalHint').textContent = hint;
    el('roleSubmitBtn').textContent = '创建角色';
    AdminPage.openModal('roleModal');
  }

  function openEdit(role) {
    state.editing = role;
    el('roleForm').reset();
    el('roleId').value = String(role.id);
    el('roleCode').value = role.roleCode || '';
    el('roleCode').disabled = true;
    el('roleName').value = role.roleName || '';
    el('roleDescription').value = role.description || '';
    el('roleModalTitle').textContent = '编辑角色';
    el('roleModalHint').textContent = isSystemRole(role)
      ? '系统内置角色的编码不可修改，可以调整名称和描述。'
      : '角色编码创建后不可修改。';
    el('roleSubmitBtn').textContent = '保存修改';
    AdminPage.openModal('roleModal');
  }

  async function submitRole(event) {
    event.preventDefault();
    var id = el('roleId').value;
    var roleCode = el('roleCode').value.trim();
    var roleName = el('roleName').value.trim();
    var description = el('roleDescription').value.trim();

    if (!id) {
      if (!/^[A-Za-z0-9_-]{2,50}$/.test(roleCode)) {
        RBAC.toast('角色编码需为 2-50 位字母、数字、下划线或短横线', 'error');
        el('roleCode').focus();
        return;
      }
    }
    if (!roleName) {
      RBAC.toast('请输入角色名称', 'error');
      el('roleName').focus();
      return;
    }

    var body = { roleName: roleName, description: description };
    if (!id) {
      body.roleCode = roleCode;
      if (AdminPage.isSuperAdmin() && state.orgId) {
        body.orgId = Number(state.orgId);
      }
    }

    var button = el('roleSubmitBtn');
    RBAC.setButtonLoading(button, true, id ? '保存中...' : '创建中...');
    try {
      if (id) {
        await RBAC.api('/api/roles/' + id, { method: 'PUT', body: body });
        RBAC.toast('角色已更新', 'success');
      } else {
        await RBAC.api('/api/roles', { method: 'POST', body: body });
        RBAC.toast('角色已创建', 'success');
      }
      AdminPage.closeModal('roleModal');
      loadAll();
    } catch (e) {
      RBAC.toast(e.message || '保存失败', 'error');
    } finally {
      RBAC.setButtonLoading(button, false);
    }
  }

  async function openPermissions(role) {
    state.permissionRole = role;
    el('rolePermissionsTitle').textContent = '分配角色权限';
    el('rolePermissionsHint').textContent = '勾选后保存，角色下的用户权限会立即刷新。';
    el('permissionSearch').value = '';
    el('permissionGroups').innerHTML = '<div class="loading-line"><span class="spinner"></span>加载权限...</div>';
    AdminPage.openModal('rolePermissionsModal');

    try {
      if (state.permissions.length === 0) {
        var permissions = await RBAC.api('/api/permissions');
        state.permissions = Array.isArray(permissions) ? permissions : [];
      }

      var current = await RBAC.api('/api/roles/' + role.id + '/permissions');
      var selected = {};
      (Array.isArray(current) ? current : []).forEach(function (permission) {
        selected[permission.id] = true;
      });

      renderPermissionGroups(selected);
      filterPermissions();
      updateSelectedCount();
    } catch (e) {
      el('permissionGroups').innerHTML = '<div class="empty-inline">权限列表加载失败：'
        + RBAC.escapeHtml(e.message) + '</div>';
      el('permissionSelectedCount').textContent = '';
    }
  }

  // 系统管理类权限按“查看 → 创建 → 更新 → 删除 → 授权”的阅读顺序排列
  var PERMISSION_ACTION_ORDER = ['view', 'create', 'update', 'delete', 'assign-role', 'assign-permission', 'reset-password'];

  function permissionActionRank(code) {
    var parts = String(code || '').split(':');
    var action = parts.length > 1 ? parts[1] : '';
    var index = PERMISSION_ACTION_ORDER.indexOf(action);
    return index === -1 ? PERMISSION_ACTION_ORDER.length : index;
  }

  function comparePermission(module, a, b) {
    var declared = module.codes;
    if (declared) {
      var ai = declared.indexOf(a.permissionCode);
      var bi = declared.indexOf(b.permissionCode);
      if (ai !== bi) {
        if (ai === -1) {
          return 1;
        }
        if (bi === -1) {
          return -1;
        }
        return ai - bi;
      }
    } else {
      var rankDiff = permissionActionRank(a.permissionCode) - permissionActionRank(b.permissionCode);
      if (rankDiff !== 0) {
        return rankDiff;
      }
    }
    return String(a.permissionCode).localeCompare(String(b.permissionCode));
  }

  function groupPermissionsByModule() {
    var buckets = {};
    state.permissions.forEach(function (permission) {
      var module = permissionModule(permission.permissionCode);
      if (!buckets[module.key]) {
        buckets[module.key] = { module: module, items: [] };
      }
      buckets[module.key].items.push(permission);
    });

    Object.keys(buckets).forEach(function (key) {
      var module = buckets[key].module;
      buckets[key].items.sort(function (a, b) {
        return comparePermission(module, a, b);
      });
    });

    return buckets;
  }

  function renderPermissionGroups(selected) {
    var buckets = groupPermissionsByModule();
    if (Object.keys(buckets).length === 0) {
      el('permissionGroups').innerHTML = '<div class="empty-inline">暂无可分配权限。</div>';
      return;
    }

    el('permissionGroups').innerHTML = RBAC.permissionScopes.map(function (scope) {
      var modules = scope.key === 'other'
        ? (buckets.other ? [buckets.other.module] : [])
        : RBAC.permissionModules.filter(function (module) {
          return module.scope === scope.key && buckets[module.key];
        });
      if (modules.length === 0) {
        return '';
      }
      var total = modules.reduce(function (sum, module) {
        return sum + buckets[module.key].items.length;
      }, 0);
      return '<section class="permission-scope" data-scope="' + scope.key + '">'
        + '<header class="permission-scope-head">'
        + '<span class="permission-scope-title">' + RBAC.escapeHtml(scope.label) + '</span>'
        + '<span class="permission-scope-hint">' + RBAC.escapeHtml(scope.hint) + '</span>'
        + '<span class="permission-scope-total">' + total + ' 项</span>'
        + '</header>'
        + '<div class="permission-modules">'
        + modules.map(function (module) {
          return renderPermissionModule(module, buckets[module.key].items, selected);
        }).join('')
        + '</div></section>';
    }).join('');
  }

  function renderPermissionModule(module, items, selected) {
    var key = RBAC.escapeHtml(module.key);
    var selectedCount = items.filter(function (permission) {
      return selected[permission.id];
    }).length;

    return '<section class="permission-group" data-module="' + key + '">'
      + '<div class="permission-group-head">'
      + '<label class="permission-group-toggle">'
      + '<input type="checkbox" data-module-toggle="' + key + '" aria-label="全选“' + RBAC.escapeHtml(module.label) + '”">'
      + '<span class="permission-group-title">' + RBAC.escapeHtml(module.label) + '</span>'
      + '</label>'
      + '<span class="permission-group-count" data-module-count="' + key + '">'
      + selectedCount + ' / ' + items.length + '</span>'
      + '</div>'
      + '<div class="permission-options">'
      + items.map(function (permission) {
        var searchText = ((permission.permissionCode || '') + ' ' + (permission.permissionName || '')).toLowerCase();
        return '<label class="check-item permission-item" data-search="' + RBAC.escapeHtml(searchText) + '">'
          + '<input type="checkbox" value="' + permission.id + '"' + (selected[permission.id] ? ' checked' : '') + '>'
          + '<span class="check-item-body">'
          + '<span class="check-item-title">' + RBAC.escapeHtml(permission.permissionName)
          + ' <span class="code-pill code-pill-sm">' + RBAC.escapeHtml(permission.permissionCode) + '</span></span>'
          + (Number(permission.status) === 0 ? '<span class="check-item-desc text-danger">该权限当前已禁用</span>' : '')
          + '</span>'
          + '</label>';
      }).join('')
      + '</div></section>';
  }

  function filterPermissions() {
    var keyword = el('permissionSearch').value.trim().toLowerCase();
    var visibleTotal = 0;
    Array.prototype.forEach.call(el('permissionGroups').querySelectorAll('.permission-group'), function (group) {
      var visibleCount = 0;
      Array.prototype.forEach.call(group.querySelectorAll('.permission-item'), function (item) {
        var match = !keyword || (item.getAttribute('data-search') || '').indexOf(keyword) !== -1;
        item.hidden = !match;
        if (match) {
          visibleCount += 1;
        }
      });
      group.hidden = visibleCount === 0;
      visibleTotal += visibleCount;
    });
    Array.prototype.forEach.call(el('permissionGroups').querySelectorAll('.permission-scope'), function (scope) {
      scope.hidden = !scope.querySelector('.permission-group:not([hidden])');
    });
    var empty = el('permissionEmpty');
    if (empty) {
      var hasGroups = el('permissionGroups').querySelectorAll('.permission-group').length > 0;
      empty.hidden = !hasGroups || visibleTotal !== 0;
    }
    syncModuleToggles();
  }

  function syncModuleToggles() {
    Array.prototype.forEach.call(el('permissionGroups').querySelectorAll('.permission-group'), function (group) {
      var toggle = group.querySelector('[data-module-toggle]');
      if (!toggle) {
        return;
      }
      var inputs = group.querySelectorAll('.permission-item:not([hidden]) input[type="checkbox"]');
      var checked = 0;
      Array.prototype.forEach.call(inputs, function (input) {
        if (input.checked) {
          checked += 1;
        }
      });
      toggle.checked = inputs.length > 0 && checked === inputs.length;
      toggle.indeterminate = checked > 0 && checked < inputs.length;
      var count = group.querySelector('[data-module-count]');
      if (count) {
        count.textContent = checked + ' / ' + inputs.length;
      }
    });
  }

  function setModulePermissions(moduleKey, checked) {
    var group = el('permissionGroups').querySelector('.permission-group[data-module="' + moduleKey + '"]');
    if (!group) {
      return;
    }
    Array.prototype.forEach.call(
      group.querySelectorAll('.permission-item:not([hidden]) input[type="checkbox"]'),
      function (input) {
        input.checked = checked;
      }
    );
  }

  function setVisiblePermissions(checked) {
    Array.prototype.forEach.call(
      el('permissionGroups').querySelectorAll('.permission-item:not([hidden]) input[type="checkbox"]'),
      function (input) {
        input.checked = checked;
      }
    );
    updateSelectedCount();
    syncModuleToggles();
  }

  function updateSelectedCount() {
    var inputs = el('permissionGroups').querySelectorAll('.permission-item input[type="checkbox"]');
    var checked = el('permissionGroups').querySelectorAll('.permission-item input[type="checkbox"]:checked');
    el('permissionSelectedCount').textContent = '已选择 ' + checked.length + ' / ' + inputs.length + ' 项权限';
  }

  async function saveRolePermissions() {
    if (!state.permissionRole) {
      return;
    }
    var ids = Array.prototype.map.call(
      el('permissionGroups').querySelectorAll('.permission-item input[type="checkbox"]:checked'),
      function (input) {
        return Number(input.value);
      }
    );
    var button = el('saveRolePermissionsBtn');
    RBAC.setButtonLoading(button, true, '保存中...');
    try {
      await RBAC.api('/api/roles/' + state.permissionRole.id + '/permissions', {
        method: 'POST',
        body: { permissionIds: ids }
      });
      RBAC.toast('角色权限已更新', 'success');
      AdminPage.closeModal('rolePermissionsModal');
      loadAll();
    } catch (e) {
      RBAC.toast(e.message || '保存权限失败', 'error');
    } finally {
      RBAC.setButtonLoading(button, false);
    }
  }

  async function removeRole(role) {
    var confirmed = await AdminPage.confirm({
      title: '删除角色',
      message: '确定删除角色“' + role.roleName + '”吗？拥有该角色的用户会失去对应权限。',
      confirmText: '删除角色',
      danger: true
    });
    if (!confirmed) {
      return;
    }
    try {
      await RBAC.api('/api/roles/' + role.id, { method: 'DELETE' });
      RBAC.toast('角色已删除', 'success');
      loadAll();
    } catch (e) {
      RBAC.toast(e.message || '删除失败', 'error');
    }
  }
})();
