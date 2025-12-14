# 接口：AdminUserLogin
* 默认路由：***/api/admin***
* 声明：***com.buctta.api.service::\service\AdminUserLogin.java***
* 实现：***com.buctta.api.serviceimpl::\serviceimpl\IMPL_AdminUserLogin.java***

---
### ***AdminUser login(String username, String password)***
* 若查询到用户，则返回由此用户数据构造的 ***AdminUser***，否则返回空 ***AdminUser***

---
### ***AdminUser register(AdminUser RequestUser)***
* 若注册成功，则返回由这个新注册用户的数据构造的 ***AdminUser***，否则返回空 ***AdminUser***