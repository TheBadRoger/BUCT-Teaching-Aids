# 接口：TeacherListCtrl
* 默认路由：***/api/findteacher***
* 声明和实现：***com.buctta.api.interf::\interf\TeacherListCtrl.java***

---
### *public CallBackContainer<TeacherList> addteacherCall(@RequestBody TeacherList newteacher)*;
* 路由：***/api/findteacher/addteacher***
* 接收请求：
    * 请求头：***application/x-www-form-urlencoded***
      * 参数名称：***TeacherList***
* 返回：***CallBackContaioner::code***，***CallBackContaioner::msg***，***TeacherList***
* 作用：处理添加教师请求

---

### *public CallBackContainer<Page<TeacherList>> SearchTeacherCall(@RequestParam(required = false) String name,@RequestParam(required = false) String organization,@RequestParam(required = false) String jointime,@RequestParam(required = false) String gender,@RequestParam(required = false) String education,@RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size,@RequestParam(defaultValue = "id") String sort) ;*
* 路由：***/api/findteacher/searchteacher***
* 接收请求：
    * 请求头：***application/x-www-form-urlencoded***
    * 参数名称：***TeacherList***，***page***,***size***,***sort***
    * 各个参数都可以不填，***page***参数是当前页码，***size***参数是每页显示的最大数量，***sort***参数是排序规则。
* 返回：***CallBackContaioner::code***，***CallBackContaioner::msg***，***TeacherList***
* 作用：处理查询教师请求
