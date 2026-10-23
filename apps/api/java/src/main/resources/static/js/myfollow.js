/**
 * 我的关注 - 管理课程分类关注
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 使用localStorage存储关注分类
    function getFollowedCategories() {
        var data = localStorage.getItem('followedCategories');
        return data ? JSON.parse(data) : ['三级分类'];
    }

    function saveFollowedCategories(categories) {
        localStorage.setItem('followedCategories', JSON.stringify(categories));
    }

    // 渲染已关注列表
    function renderFollowedCategories() {
        var grid = document.querySelector('.category-grid');
        if (!grid) return;
        var categories = getFollowedCategories();
        grid.innerHTML = categories.map(function(cat, index) {
            return '<div class="category-item">' +
                '<span>' + cat + '</span>' +
                '<span class="close" data-index="' + index + '">×</span>' +
            '</div>';
        }).join('');

        // 删除事件
        grid.querySelectorAll('.close').forEach(function(el) {
            el.addEventListener('click', function() {
                var index = parseInt(this.getAttribute('data-index'));
                var cats = getFollowedCategories();
                cats.splice(index, 1);
                saveFollowedCategories(cats);
                renderFollowedCategories();
            });
        });
    }

    // 加载分类选项（从课程分类接口获取）
    function loadCategories() {
        // 使用课程搜索接口获取分类标签作为一级分类
        fetch(API_BASE + '/course/search?page=0&size=1')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000) {
                    // 成功连接后端，可在此扩展分类加载逻辑
                }
            })
            .catch(function() {
                // 后端不可用，使用默认分类
            });

        // 填充默认分类选项
        var level1 = document.getElementById('level1');
        var level2 = document.getElementById('level2');
        var level3 = document.getElementById('level3');

        if (level1) {
            var cats1 = ['', '大学', '高中', '初中', '小学', '职业培训'];
            level1.innerHTML = cats1.map(function(c) {
                return '<option value="' + c + '">' + (c || '一级分类') + '</option>';
            }).join('');
            level1.addEventListener('change', function() {
                updateLevel2(this.value);
            });
        }

        if (level2) {
            var cats2 = ['', '理工类', '文史类', '艺术类'];
            level2.innerHTML = cats2.map(function(c) {
                return '<option value="' + c + '">' + (c || '二级分类') + '</option>';
            }).join('');
            level2.addEventListener('change', function() {
                updateLevel3(this.value);
            });
        }

        if (level3) {
            var cats3 = ['', '数学', '物理', '化学', '语文', '英语'];
            level3.innerHTML = cats3.map(function(c) {
                return '<option value="' + c + '">' + (c || '三级分类') + '</option>';
            }).join('');
        }
    }

    function updateLevel2(val) {
        var level2 = document.getElementById('level2');
        if (!level2) return;
        var options = [];
        if (val === '大学') options = ['', '理工类', '文史类', '医学类'];
        else if (val === '高中') options = ['', '理科', '文科'];
        else if (val === '初中') options = ['', '语文', '数学', '英语', '科学'];
        else if (val === '小学') options = ['', '语文', '数学', '英语'];
        else if (val === '职业培训') options = ['', '编程', '设计', '管理'];
        else options = ['', '理工类', '文史类', '艺术类'];
        level2.innerHTML = options.map(function(c) {
            return '<option value="' + c + '">' + (c || '二级分类') + '</option>';
        }).join('');
        updateLevel3(options[1] || '');
    }

    function updateLevel3(val) {
        var level3 = document.getElementById('level3');
        if (!level3) return;
        var options = [];
        if (val === '理工类' || val === '理科') options = ['', '数学', '物理', '化学', '生物'];
        else if (val === '文史类' || val === '文科') options = ['', '语文', '历史', '地理', '政治'];
        else if (val === '医学类') options = ['', '基础医学', '临床医学', '药学'];
        else if (val === '编程') options = ['', 'Java', 'Python', '前端'];
        else if (val === '设计') options = ['', 'UI设计', '平面设计', '室内设计'];
        else if (val === '管理') options = ['', '项目管理', '人力资源', '市场营销'];
        else if (['语文', '数学', '英语', '科学'].indexOf(val) !== -1) {
            options = ['', '上册', '下册'];
        } else options = ['', '数学', '物理', '化学', '语文', '英语'];
        level3.innerHTML = options.map(function(c) {
            return '<option value="' + c + '">' + (c || '三级分类') + '</option>';
        }).join('');
    }

    // 添加关注
    var addBtn = document.querySelector('.add');
    if (addBtn) {
        addBtn.addEventListener('click', function() {
            var level3 = document.getElementById('level3');
            if (!level3 || !level3.value) {
                alert('请先选择三级分类');
                return;
            }
            var cats = getFollowedCategories();
            var newCat = level3.value;
            if (cats.indexOf(newCat) === -1) {
                cats.push(newCat);
                saveFollowedCategories(cats);
                renderFollowedCategories();
            } else {
                alert('该分类已关注');
            }
        });
    }

    loadCategories();
    renderFollowedCategories();

    // 搜索
    var searchBtn = document.querySelector('.top-bar-right button, .search-area button');
    var searchInput = document.querySelector('.top-bar-right input, .search-area input');
    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', function () {
            var keyword = searchInput.value.trim();
            if (keyword) {
                window.location.href = 'search.html?keyword=' + encodeURIComponent(keyword);
            }
        });
    }
});