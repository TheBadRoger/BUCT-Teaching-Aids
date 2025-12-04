# 接口：AdminQuery
声明：***com.buctta.api.reposit::\reposit\AdminQuery.java***

继承自：***JpaRepositry<AdminUser, Integer>***

---
### ***AdminUser findAdminUserByUsername(String username)***
* 通过username查询数据库中的数据，若查询到，则返回查询到的 ***AdminUser***，否则返回空 ***AdminUser***

---
### ***AdminUser findAdminUserByUsername(String username)***

* 通过username和password查询数据库中的数据，若查询到，则返回查询到的 ***AdminUser***，否则返回空 ***AdminUser***