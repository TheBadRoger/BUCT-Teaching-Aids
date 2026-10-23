/**
 * 学习记录（二期）- 学习分布和雷达图
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 模拟数据
    var statsData = {
        coursesCount: 15,
        notesCount: 52,
        discussionsCount: 28,
        materialsCount: 40,
        testsCount: 10
    };

    // 雷达图数据
    var radarData = {
        labels: ['课程学习', '笔记', '讨论', '教参', '测试'],
        values: [85, 70, 55, 80, 60]
    };

    // 渲染卡片
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

    // 绘制雷达图
    function drawRadar() {
        var chartArea = document.querySelector('.radar-chart-area');
        if (!chartArea) return;

        var canvas = document.createElement('canvas');
        canvas.width = chartArea.offsetWidth || 600;
        canvas.height = chartArea.offsetHeight || 300;
        canvas.style.position = 'relative';
        canvas.style.zIndex = '2';
        chartArea.style.position = 'relative';
        chartArea.innerHTML = '';
        chartArea.appendChild(canvas);
        chartArea.style.background = 'none';

        var ctx = canvas.getContext('2d');
        var w = canvas.width;
        var h = canvas.height;
        var cx = w / 2;
        var cy = h / 2;
        var radius = Math.min(w, h) / 2 - 50;
        var n = radarData.labels.length;
        var angleStep = (Math.PI * 2) / n;

        // 网格
        ctx.strokeStyle = '#ddd';
        ctx.lineWidth = 1;
        for (var r = 1; r <= 5; r++) {
            ctx.beginPath();
            var r2 = radius * r / 5;
            for (var i = 0; i <= n; i++) {
                var angle = -Math.PI / 2 + i * angleStep;
                var x = cx + r2 * Math.cos(angle);
                var y = cy + r2 * Math.sin(angle);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.closePath();
            ctx.stroke();
        }

        // 轴线
        ctx.strokeStyle = '#ccc';
        for (var j = 0; j < n; j++) {
            var a = -Math.PI / 2 + j * angleStep;
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.lineTo(cx + radius * Math.cos(a), cy + radius * Math.sin(a));
            ctx.stroke();
        }

        // 数据区域
        ctx.beginPath();
        ctx.fillStyle = 'rgba(0, 123, 255, 0.2)';
        ctx.strokeStyle = '#007bff';
        ctx.lineWidth = 2;
        radarData.values.forEach(function(val, idx) {
            var angle = -Math.PI / 2 + idx * angleStep;
            var r2 = radius * val / 100;
            var x = cx + r2 * Math.cos(angle);
            var y = cy + r2 * Math.sin(angle);
            if (idx === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });
        ctx.closePath();
        ctx.fill();
        ctx.stroke();

        // 标签和数据点
        ctx.fillStyle = '#333';
        ctx.font = '12px sans-serif';
        ctx.textAlign = 'center';
        radarData.values.forEach(function(val, idx) {
            var angle = -Math.PI / 2 + idx * angleStep;
            var r2 = radius * val / 100;
            var x = cx + r2 * Math.cos(angle);
            var y = cy + r2 * Math.sin(angle);
            ctx.beginPath();
            ctx.fillStyle = '#007bff';
            ctx.arc(x, y, 3, 0, Math.PI * 2);
            ctx.fill();
            var lx = cx + (radius + 30) * Math.cos(angle);
            var ly = cy + (radius + 30) * Math.sin(angle);
            ctx.fillStyle = '#333';
            ctx.fillText(radarData.labels[idx], lx, ly + 4);
        });

        // 图例中心值
        ctx.fillStyle = '#666';
        ctx.font = 'bold 14px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('学习分布', cx, cy + 5);
    }

    renderCards();

    // 标签切换
    var tabs = document.querySelectorAll('.content-tabs span');
    tabs.forEach(function(tab) {
        tab.addEventListener('click', function() {
            tabs.forEach(function(t) { t.style.background = ''; t.style.color = ''; });
            this.style.background = '#007bff';
            this.style.color = '#fff';
        });
    });

    setTimeout(drawRadar, 100);
});