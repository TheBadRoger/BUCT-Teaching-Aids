/**
 * 我的提问 - 获取学生提问/笔记列表，支持编辑删除
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    function getCurrentUser() {
        return fetch(API_BASE + '/user/auth/current')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) return data.data;
                throw new Error('未登录');
            });
    }

    function renderQuestions(notes) {
        var list = document.querySelector('.question-list');
        if (!list) return;
        var breadcrumb = list.querySelector('p');
        var container = document.createElement('div');
        if (!notes || notes.length === 0) {
            container.innerHTML = '<div style="text-align:center;padding:30px;color:#999;">暂无提问</div>';
        } else {
            container.innerHTML = notes.map(function(note) {
                var date = note.createdAt ? note.createdAt.substring(0, 10) : '2013-09-23';
                var views = note.likeCount || 0;
                var answers = note.commentCount || 0;
                return '<div class="question-item">' +
                    '<img alt="我的头像" src="images/default.png">' +
                    '<div class="question-info">' +
                        '<div class="left">' +
                            '<a href="questiondetail.html?id=' + (note.id || '') + '">' + (note.title || '无标题') + '</a>' +
                            '<div class="question-meta">浏览 (' + views + ') | 回答 (' + answers + ')</div>' +
                        '</div>' +
                        '<div class="right">' +
                            '<div class="question-date">' + date + '</div>' +
                            '<div class="question-op" data-id="' + (note.id || '') + '">编辑 | 删除</div>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            }).join('');
        }
        if (breadcrumb) {
            list.innerHTML = breadcrumb.outerHTML;
            list.appendChild(container);
        } else {
            list.appendChild(container);
        }
    }

    getCurrentUser()
        .then(function(user) {
            fetch(API_BASE + '/notes/student/' + user.id + '?page=0&size=20')
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    if (data && data.code === 2000 && data.data) {
                        renderQuestions(data.data.content || data.data);
                    } else {
                        renderQuestions([]);
                    }
                })
                .catch(function(err) { console.error('加载提问失败', err); renderQuestions([]); });
        })
        .catch(function() {
            renderQuestions([
                { id: 1, title: '为什么下雨天先看到闪电后听到雷声', likeCount: 23, commentCount: 4, createdAt: '2013-09-23' },
                { id: 2, title: '如何理解牛顿第二定律', likeCount: 15, commentCount: 3, createdAt: '2013-09-22' },
                { id: 3, title: 'Java中HashMap的工作原理', likeCount: 45, commentCount: 8, createdAt: '2013-09-21' }
            ]);
        });
});
