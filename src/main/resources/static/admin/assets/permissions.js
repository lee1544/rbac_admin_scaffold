(function () {
  'use strict';

  var RBAC = window.RBAC;
  var AdminPage = window.AdminPage;

  var state = {
    permissions: [],
    roles: [],
    keyword: '',
    status: '',
    editing: null
  };

  function el(id) {
    return document.getElementById(id);
  }

  document.addEventListener('DOMContentLoaded', function () {
    AdminPage.init({ permission: 'permission:view' }).then(function (user) {
      if (!user || !AdminPage.can('permission:view')) {
        return;
      }
      init();
    });
  });

  function init() {
    AdminPage.bindModalClose();
    bindEvents();
    loadAll();
  }

  function bindEvents() {
    el('filterForm').addEventListener('submit', function (event) {
      event.preventDefault();
      state.keyword = el('keyword').value.trim().toLowerCase();
      state.status = el('statusFilter').value;
      renderTable();
    });
    el('resetFilterBtn').addEventListener('click', function () {
      el('keyword').value = '';
      el('statusFilter').value = '';
      state.keyword = '';
      state.status = '';
      renderTable();
    });
    el('refreshBtn').addEventListener('click', loadAll);
    el('createBtn').addEventListener('click', openCreate);
    el('permissionForm').addEventListener('submit', submitPermission);
    el('permissionTableBody').addEventListener('click', function (event) {
      var button = event.target.closest('[data-action]');
      if (!button || button.disabled) {
        return;
      }
      var id = Number(button.dataset.id);
      var permission = state.permissions.filter(function (item) {
        return Number(item.id) === id;
      })[0];
      if (!permission) {
        return;
      }
      if (button.dataset.action === 'edit') {
        openEdit(permission);
      } else if (button.dataset.action === 'toggle') {
        toggleStatus(permission);
      } else if (button.dataset.action === 'delete') {
        removePermission(permission);
      }
    });
  }

  async function loadAll() {
    el('listSummary').textContent = '加载中...';
    var permissionsRequest = RBAC.api('/api/permissions').then(
      function (data) {
        return { data: data };
      },
      function (error) {
        return { error: error };
      }
    );
    var rolesRequest = RBAC.api('/api/roles').then(
      function (data) {
        return { data: data };
      },
      function (error) {
        return { error: error };
      }
    );
    var results = await Promise.all([permissionsRequest, rolesRequest]);

    var permissionsResult = results[0];
    if (permissionsResult.error) {
      state.permissions = [];
      state.roles = [];
      renderError(permissionsResult.error.message);
      return;
    }
    state.permissions = Array.isArray(permissionsResult.data) ? permissionsResult.data : [];

    var rolesResult = results[1];
    state.roles = !rolesResult.error && Array.isArray(rolesResult.data) ? rolesResult.data : [];
    renderTable();
  }

  function categoryOf(code) {
    return RBAC.permissionModule(code).label;
  }

  function usageOf(permissionId) {
    return state.roles.filter(function (role) {
      var permissions = Array.isArray(role.permissions) ? role.permissions : [];
      return permissions.some(function (permission) {
        return Number(permission.id) === Number(permissionId);
      });
    });
  }

  function filteredPermissions() {
    return state.permissions.filter(function (permission) {
      if (state.status !== '' && String(permission.status) !== state.status) {
        return false;
      }
      if (!state.keyword) {
        return true;
      }
      var text = ((permission.permissionCode || '') + ' ' + (permission.permissionName || '')).toLowerCase();
      return text.indexOf(state.keyword) !== -1;
    });
  }

  function renderTable() {
    var body = el('permissionTableBody');
    var items = filteredPermissions();
    var canUpdate = AdminPage.can('permission:update');
    var canDelete = AdminPage.can('permission:delete');
    var enabledCount = state.permissions.filter(function (permission) {
      return Number(permission.status) === 1;
    }).length;

    el('listSummary').textContent = '共 ' + state.permissions.length + ' 个权限，' + enabledCount + ' 个启用'
      + (state.keyword || state.status !== '' ? '，当前筛选 ' + items.length + ' 个' : '');
    el('pageInfo').textContent = '共 ' + state.permissions.length + ' 个权限';

    if (items.length === 0) {
      body.innerHTML = '<tr><td colspan="7"><div class="table-state table-empty"><strong>没有匹配的权限</strong><span>请调整关键字或状态筛选。</span></div></td></tr>';
      return;
    }

    body.innerHTML = items.map(function (permission) {
      var roles = usageOf(permission.id);
      var roleNames = roles.map(function (role) {
        return role.roleName;
      }).join('、');
      var usage = roles.length > 0
        ? '<span class="count-pill" title="' + RBAC.escapeHtml(roleNames) + '">' + roles.length + ' 个角色</span>'
        : '<span class="muted-value">未分配</span>';
      var actions = '';
      if (canUpdate) {
        actions += '<button class="mini-btn" type="button" data-action="edit" data-id="' + permission.id + '">编辑</button>';
        actions += '<button class="mini-btn' + (Number(permission.status) === 1 ? ' is-danger' : '') + '" type="button" data-action="toggle" data-id="'
          + permission.id + '">' + (Number(permission.status) === 1 ? '禁用' : '启用') + '</button>';
      }
      if (canDelete) {
        actions += '<button class="mini-btn is-danger" type="button" data-action="delete" data-id="' + permission.id + '">删除</button>';
      }
      if (!actions) {
        actions = '<span class="muted-value">只读</span>';
      }

      return '<tr>'
        + '<td><span class="code-pill">' + RBAC.escapeHtml(permission.permissionCode) + '</span></td>'
        + '<td><strong>' + RBAC.escapeHtml(permission.permissionName) + '</strong></td>'
        + '<td><span class="role-badge">' + RBAC.escapeHtml(categoryOf(permission.permissionCode)) + '</span></td>'
        + '<td>' + usage + '</td>'
        + '<td>' + statusBadge(permission.status) + '</td>'
        + '<td class="muted-value">' + RBAC.formatDateTime(permission.updateTime) + '</td>'
        + '<td class="td-actions"><div class="row-actions">' + actions + '</div></td>'
        + '</tr>';
    }).join('');
  }

  function statusBadge(status) {
    var enabled = Number(status) === 1;
    return '<span class="status-badge ' + (enabled ? 'status-activated' : 'status-disabled') + '">'
      + (enabled ? '启用' : '禁用') + '</span>';
  }

  function renderError(message) {
    el('permissionTableBody').innerHTML = '<tr><td colspan="7"><div class="table-state table-empty"><strong>加载失败</strong><span>'
      + RBAC.escapeHtml(message) + '</span><button class="btn btn-ghost btn-sm" type="button" onclick="location.reload()">重新加载</button></div></td></tr>';
    el('listSummary').textContent = '加载失败';
    el('pageInfo').textContent = '加载失败';
  }

  function openCreate() {
    state.editing = null;
    el('permissionForm').reset();
    el('permissionId').value = '';
    el('permissionCode').disabled = false;
    el('permissionStatus').value = '1';
    el('permissionModalTitle').textContent = '新建权限';
    el('permissionModalHint').textContent = '权限编码建议使用“模块:动作”格式，例如 product:view。';
    el('permissionSubmitBtn').textContent = '创建权限';
    AdminPage.openModal('permissionModal');
  }

  function openEdit(permission) {
    state.editing = permission;
    el('permissionForm').reset();
    el('permissionId').value = String(permission.id);
    el('permissionCode').value = permission.permissionCode || '';
    el('permissionName').value = permission.permissionName || '';
    el('permissionStatus').value = String(permission.status);
    el('permissionModalTitle').textContent = '编辑权限';
    el('permissionModalHint').textContent = '修改权限编码会影响所有使用该编码的接口声明，请谨慎操作。';
    el('permissionSubmitBtn').textContent = '保存修改';
    AdminPage.openModal('permissionModal');
  }

  async function submitPermission(event) {
    event.preventDefault();
    var id = el('permissionId').value;
    var code = el('permissionCode').value.trim();
    var name = el('permissionName').value.trim();
    var status = Number(el('permissionStatus').value);

    if (!/^[A-Za-z0-9:_-]{2,100}$/.test(code)) {
      RBAC.toast('权限编码需为 2-100 位字母、数字、冒号、下划线或短横线', 'error');
      el('permissionCode').focus();
      return;
    }
    if (!name) {
      RBAC.toast('请输入权限名称', 'error');
      el('permissionName').focus();
      return;
    }

    var body = { permissionCode: code, permissionName: name, status: status };
    var button = el('permissionSubmitBtn');
    RBAC.setButtonLoading(button, true, id ? '保存中...' : '创建中...');
    try {
      if (id) {
        await RBAC.api('/api/permissions/' + id, { method: 'PUT', body: body });
        RBAC.toast('权限已更新', 'success');
      } else {
        await RBAC.api('/api/permissions', { method: 'POST', body: body });
        RBAC.toast('权限已创建', 'success');
      }
      AdminPage.closeModal('permissionModal');
      loadAll();
    } catch (e) {
      RBAC.toast(e.message || '保存失败', 'error');
    } finally {
      RBAC.setButtonLoading(button, false);
    }
  }

  async function toggleStatus(permission) {
    var next = Number(permission.status) === 1 ? 0 : 1;
    var confirmed = await AdminPage.confirm({
      title: next === 0 ? '禁用权限' : '启用权限',
      message: next === 0
        ? '禁用“' + permission.permissionName + '”后，所有角色和用户将立即失去该权限，确定继续吗？'
        : '启用“' + permission.permissionName + '”后，已分配该权限的角色会立即恢复使用，确定继续吗？',
      confirmText: next === 0 ? '禁用权限' : '启用权限',
      danger: next === 0
    });
    if (!confirmed) {
      return;
    }
    try {
      await RBAC.api('/api/permissions/' + permission.id + '/status?status=' + next, { method: 'PUT' });
      RBAC.toast(next === 1 ? '权限已启用' : '权限已禁用', 'success');
      loadAll();
    } catch (e) {
      RBAC.toast(e.message || '操作失败', 'error');
    }
  }

  async function removePermission(permission) {
    var confirmed = await AdminPage.confirm({
      title: '删除权限',
      message: '确定删除权限“' + permission.permissionName + '”吗？已分配给角色的权限需要先解除关联。',
      confirmText: '删除权限',
      danger: true
    });
    if (!confirmed) {
      return;
    }
    try {
      await RBAC.api('/api/permissions/' + permission.id, { method: 'DELETE' });
      RBAC.toast('权限已删除', 'success');
      loadAll();
    } catch (e) {
      RBAC.toast(e.message || '删除失败', 'error');
    }
  }
})();
