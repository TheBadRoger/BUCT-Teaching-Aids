# 接口：TeacherReposit
声明：***com.buctta.api.reposit::\reposit\TeacherReposit.java***

继承自：***JpaRepository<TeacherList,Long>, JpaSpecificationExecutor<TeacherList>***

---
### ***TeacherList findTeacherListById(long Id)***
* 通过Id查询数据库中的数据，若查询到，则返回查询到的 ***TeacherList***，否则返回空 ***TeacherList***

---
### ***TeacherList findTeacherListByName(String name)***

* 通过username和password查询数据库中的数据，若查询到，则返回查询到的 ***TeacherList***，否则返回空 ***TeacherList***

---
### ***TeacherList findTeacherListByEmail(String email)***

* 通过username和password查询数据库中的数据，若查询到，则返回查询到的 ***TeacherList***，否则返回空 ***TeacherList***

---
### ***TeacherList findTeacherListByTelephone(String telephone)***

* 通过username和password查询数据库中的数据，若查询到，则返回查询到的 ***TeacherList***，否则返回空 ***TeacherList***

