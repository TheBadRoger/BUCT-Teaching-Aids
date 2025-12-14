# 接口：TeacherListCalls
* 默认路由：***/api/findteacher***
* 声明和实现：***com.buctta.api.interf::\interf\TeacherListCalls.java***

---
### *public CallBackContainer<TeacherList> addteacherCall(@RequestBody TeacherList newteacher)*;
* 路由：***/api/findteacher/addteacher***
* 接收请求：
    * 请求头：***application/x-www-form-urlencoded***
      * 参数名称：***TeacherList***
* 返回：***CallBackContaioner::code***，***CallBackContaioner::msg***，***TeacherList***
* 作用：处理管理员用户的登录请求

---
### *public CallBackContainer<Page<TeacherList>> SearchTeacherCall(@RequestParam(required = false) String name,@RequestParam(required = false) String organization,@RequestParam(required = false) String jointime,@RequestParam(required = false) String gender,@RequestParam(required = false) String education,@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size,@RequestParam(defaultValue = "id") String sort) *;
* 路由：***/api/findteacher/searchteacher***
* 接收请求：
    * 请求头：***application/x-www-form-urlencoded***
    * 参数名称：***TeacherList***，***page***,***size***,***sort***
* 返回：***CallBackContaioner::code***，***CallBackContaioner::msg***，***TeacherList***
* 作用：处理管理员用户的注册请求
