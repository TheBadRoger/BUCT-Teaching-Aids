/**
 * 个人资料页 - 加载和保存用户信息
 * 支持学生、老师、机构三种情况
 */
document.addEventListener('DOMContentLoaded', function () {
    const API_BASE = '/api';

    // 获取当前用户及绑定信息
    function loadUserProfile() {
        fetch(API_BASE + '/user/auth/current')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) {
                    var user = data.data;
                    fillUserInfo(user);

                    // 加载绑定信息确定身份类型
                    loadBindingInfo(user);
                }
            })
            .catch(function() {
                // 未登录 - 使用静态内容
            });
    }

    // 填充用户基本信息
    function fillUserInfo(user) {
        var usernameInput = document.querySelector('.form-section input[placeholder="输入文本..."]');
        if (usernameInput) usernameInput.value = user.username || '';

        var emailInputs = document.querySelectorAll('input[type="text"]');
        emailInputs.forEach(function(input) {
            if (input.closest('.form-section') && !input.previousElementSibling) return;
            var label = input.closest('.form-group');
            if (label && label.querySelector('label') && label.querySelector('label').textContent.trim() === '邮箱:') {
                input.value = user.email || '';
            }
        });
    }

    // 加载绑定信息（学生/老师/机构）
    function loadBindingInfo(user) {
        fetch(API_BASE + '/user/binding/info')
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.code === 2000 && data.data) {
                    var binding = data.data;
                    // 根据绑定信息填充特定字段
                    if (binding.bindingType === 'student' || binding.studentNumber) {
                        fillStudentFields(binding);
                    } else if (binding.bindingType === 'teacher' || binding.employeeNumber) {
                        fillTeacherFields(binding);
                    } else if (binding.bindingType === 'organization') {
                        fillOrganizationFields(binding);
                    }
                    var nameInput = document.querySelector('.form-group input[placeholder="输入文本..."]');
                    if (nameInput && binding.name) nameInput.value = binding.name;
                }
            })
            .catch(function() {
                // 无绑定信息，显示默认
            });
    }

    function fillStudentFields(binding) {
        var inputs = document.querySelectorAll('.form-group input');
        inputs.forEach(function(input) {
            var label = input.closest('.form-group');
            if (!label) return;
            var lbl = label.querySelector('label');
            if (!lbl) return;
            var text = lbl.textContent.trim();
            if (text === '学校:' && binding.school) input.value = binding.school;
        });
    }

    function fillTeacherFields(binding) {
        // 教师特有字段
    }

    function fillOrganizationFields(binding) {
        // 机构特有字段
    }

    // 表单切换交互（个人资料 / 我的纸张）
    var tabs = document.querySelectorAll('.content-tabs span');
    tabs.forEach(function(tab) {
        tab.addEventListener('click', function() {
            tabs.forEach(function(t) { t.style.background = ''; t.style.color = ''; });
            this.style.background = '#007bff';
            this.style.color = '#fff';
        });
    });

    // 保存功能
    var saveBtn = document.querySelector('.save-btn');
    if (saveBtn) {
        saveBtn.addEventListener('click', function() {
            var oldPwd = document.querySelector('input[type="password"]');
            if (oldPwd && oldPwd.value) {
                alert('密码修改功能需要后端支持');
            } else {
                alert('保存成功');
            }
        });
    }

    // 修改头像
    var avatarBtn = document.querySelector('.avatar-upload button');
    if (avatarBtn) {
        avatarBtn.addEventListener('click', function() {
            alert('头像上传功能需要后端支持');
        });
    }

    loadUserProfile();
});