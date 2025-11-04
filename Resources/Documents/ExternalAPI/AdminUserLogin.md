# 接口：AdminUserLoginCalls
* 默认路由：***/api/admin***
* 声明和实现：***com.buctta.api.interf::\interf\AdminUserLoginCalls.java***

---
### *public CallBackContainer\<AdminUser\> loginCall(@RequestParam String username, @RequestParam String password)*;
* 路由：***/api/admin/login***
* 接收请求：
  * 请求头：***application/x-www-form-urlencoded***
  * 参数名称：***username***，***password***
* 返回：***CallBackContaioner::code***，***CallBackContaioner::msg***，***AdminUser***
* 作用：处理管理员用户的登录请求

---
### *public CallBackContainer\<AdminUser\> loginCall(@RequestParam String username, @RequestParam String password)*;
* 路由：***/api/admin/register***
* 接收请求：
  * 请求头：***application/json***
  * 参数名称：***username***，***password***
* 返回：***CallBackContaioner::code***，***CallBackContaioner::msg***，***AdminUser***
* 作用：处理管理员用户的注册请求
