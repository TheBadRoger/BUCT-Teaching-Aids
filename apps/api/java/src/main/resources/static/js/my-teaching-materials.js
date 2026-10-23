/**
 * 我的教参 - 获取学生的笔记/教参列表
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 获取当前登录用户
    function getCurrentUser() {
        return fetch(API_BASE + '/user/auth/current')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) return data.data;
                throw new Error('未登录');
            });
    }

    // 渲染教参列表
    function renderMaterials(notes) {
        var list = document.querySelector('.list');
        if (!list) return;
        if (!notes || notes.length === 0) {
            list.innerHTML = '<div style="text-align:center;padding:30px;color:#999;">暂无教参资料</div>';
            return;
        }
        list.innerHTML = notes.map(function(note) {
            var date = note.createdAt ? note.createdAt.substring(0, 10) : '未知日期';
            return '<div class="list-item">' +
                '<div>' + (note.title || '无标题') + '</div>' +
                '<div>' + date + '</div>' +
            '</div>';
        }).join('');
    }

    // 筛选排序
    var filterSelect = document.querySelector('.filter-select');
    if (filterSelect) {
        filterSelect.addEventListener('change', function () {
            var items = document.querySelectorAll('.list-item');
            var arr = Array.prototype.slice.call(items);
            arr.reverse();
            var list = document.querySelector('.list');
            if (list) {
                list.innerHTML = '';
                arr.forEach(function(item) { list.appendChild(item); });
            }
        });
    }

    getCurrentUser()
        .then(function(user) {
            fetch(API_BASE + '/notes/student/' + user.id + '?page=0&size=20')
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    if (data && data.code === 2000 && data.data) {
                        renderMaterials(data.data.content || data.data);
                    }
                })
                .catch(function(err) { console.error('加载教参失败', err); });
        })
        .catch(function() {
            // 模拟数据
            renderMaterials([
                { title: '小学语文诗歌《将进酒》', createdAt: '2013-09-23' },
                { title: '小学数学《加减法》', createdAt: '2013-09-22' },
                { title: '英语语法基础', createdAt: '2013-09-21' }
            ]);
        });

    // 搜索
    var searchBtn = document.querySelector('.search-btn');
    var searchInput = document.querySelector('.search-input');
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', function () {
            var keyword = searchInput.value.trim();
            if (keyword) {
                window.location.href = 'search.html?keyword=' + encodeURIComponent(keyword);
            }
        });
    }
});