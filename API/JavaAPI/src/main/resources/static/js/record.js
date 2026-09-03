/**
 * 学习记录 - 学习数据和折线图
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 模拟学习统计数据
    var statsData = {
        coursesCount: 12,
        notesCount: 48,
        discussionsCount: 23,
        materialsCount: 35,
        testsCount: 8
    };

    // 模拟周学习时间数据（折线图用）
    var weekData = {
        labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        values: [2.5, 1.8, 3.2, 2.0, 1.5, 4.5, 3.0]
    };

    // 渲染统计卡片
    function renderCards() {
        var cards = document.querySelectorAll('.card');
        if (cards.length >= 5) {
            cards[0].textContent = '课程 ' + statsData.coursesCount;
            cards[1].textContent = '笔记 ' + statsData.notesCount;
            cards[2].textContent = '讨论 ' + statsData.discussionsCount;
            cards[3].textContent = '教参 ' + statsData.materialsCount;
            cards[4].textContent = '测试 ' + statsData.testsCount;
        }
    }

    // 绘制简单折线图
    function drawChart() {
        var chartArea = document.querySelector('.chart-area');
        if (!chartArea) return;

        var canvas = document.createElement('canvas');
        canvas.width = chartArea.offsetWidth || 600;
        canvas.height = chartArea.offsetHeight || 200;
        canvas.style.position = 'relative';
        canvas.style.zIndex = '2';
        chartArea.style.position = 'relative';
        chartArea.innerHTML = '';
        chartArea.appendChild(canvas);
        chartArea.style.background = 'none';

        var ctx = canvas.getContext('2d');
        var w = canvas.width;
        var h = canvas.height;
        var padding = 30;
        var maxVal = Math.max.apply(null, weekData.values) * 1.2;
        var stepX = (w - padding * 2) / (weekData.values.length - 1);

        // 清除before/after伪元素
        chartArea.className = 'chart-area';

        // 网格线
        ctx.strokeStyle = '#eee';
        ctx.lineWidth = 1;
        for (var i = 0; i <= 4; i++) {
            var y = padding + (h - padding * 2) * (1 - i / 4);
            ctx.beginPath();
            ctx.moveTo(padding, y);
            ctx.lineTo(w - padding, y);
            ctx.stroke();
            ctx.fillStyle = '#999';
            ctx.font = '11px sans-serif';
            ctx.textAlign = 'right';
            ctx.fillText((maxVal * i / 4).toFixed(1) + 'h', padding - 5, y + 4);
        }

        // X轴标签
        ctx.fillStyle = '#666';
        ctx.font = '11px sans-serif';
        ctx.textAlign = 'center';
        for (var j = 0; j < weekData.labels.length; j++) {
            var x = padding + j * stepX;
            ctx.fillText(weekData.labels[j], x, h - 5);
        }

        // 折线
        ctx.beginPath();
        ctx.strokeStyle = '#007bff';
        ctx.lineWidth = 2;
        weekData.values.forEach(function(val, idx) {
            var x = padding + idx * stepX;
            var y = padding + (h - padding * 2) * (1 - val / maxVal);
            if (idx === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });
        ctx.stroke();

        // 数据点
        weekData.values.forEach(function(val, idx) {
            var x = padding + idx * stepX;
            var y = padding + (h - padding * 2) * (1 - val / maxVal);
            ctx.beginPath();
            ctx.fillStyle = '#007bff';
            ctx.arc(x, y, 3, 0, Math.PI * 2);
            ctx.fill();
        });
    }

    renderCards();

    // 标签切换
    var tabs = document.querySelectorAll('.content-tabs span');
    tabs.forEach(function(tab) {
        tab.addEventListener('click', function() {
            tabs.forEach(function(t) { t.style.background = ''; t.style.color = ''; });
            this.style.background = '#007bff';
            this.style.color = '#fff';
            if (this.textContent.trim() === '学习记录') {
                drawChart();
            } else {
                // 简单切换显示
            }
        });
    });

    setTimeout(drawChart, 100);
});