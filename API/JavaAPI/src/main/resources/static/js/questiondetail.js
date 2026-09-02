/**
 * 提问详情 - 显示问题和回答列表
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 从URL获取问题ID
    function getQueryParam(name) {
        var params = new URLSearchParams(window.location.search);
        return params.get(name);
    }

    var questionId = getQueryParam('id');

    // 加载问题详情和回答
    function loadQuestionDetail() {
        if (!questionId) {
            // 无ID时显示静态内容
            return;
        }

        // 尝试从笔记接口获取问题详情
        fetch(API_BASE + '/notes/' + questionId)
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) {
                    var questionEl = document.querySelector('.question');
                    if (questionEl) {
                        questionEl.textContent = data.data.title || '问题详情';
                    }
                }
            })
            .catch(function(err) { console.error('加载问题详情失败', err); });

        // 加载回答（评论）
        fetch(API_BASE + '/comments/note/' + questionId)
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) {
                    renderAnswers(data.data);
                }
            })
            .catch(function(err) { console.error('加载回答失败', err); });
    }

    function renderAnswers(comments) {
        var list = document.querySelector('.answer-list');
        if (!list) return;
        var breadcrumb = list.querySelector('p');
        var question = list.querySelector('.question');
        var container = document.createElement('div');

        if (!comments || comments.length === 0) {
            container.innerHTML = '<div style="text-align:center;padding:20px;color:#999;">暂无回答</div>';
        } else {
            container.innerHTML = comments.map(function(c) {
                var date = c.createdAt ? c.createdAt.substring(0, 10) : '未知日期';
                return '<div class="answer-item">' +
                    '<img alt="头像" src="images/default.png">' +
                    '<div class="answer-info">' +
                        '<div class="left">' +
                            '<div class="answer-meta">' + (c.content || '') + '</div>' +
                        '</div>' +
                        '<div class="right">' +
                            '<div class="time">' + date + '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            }).join('');
        }

        if (breadcrumb && question) {
            list.innerHTML = breadcrumb.outerHTML + question.outerHTML;
            list.appendChild(container);
        } else if (list) {
            list.appendChild(container);
        }
    }

    loadQuestionDetail();
});