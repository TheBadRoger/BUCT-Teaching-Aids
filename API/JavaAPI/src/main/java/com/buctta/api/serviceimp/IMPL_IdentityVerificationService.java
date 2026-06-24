package com.buctta.api.serviceimp;

import com.buctta.api.config.AppProperties;
import com.buctta.api.service.IdentityVerificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 网络身份认证服务实现
 * <p>
 * 根据配置 app.identity-verification.mock-mode 决定运行模式：
 * - true (开发模式): 直接返回模拟验证成功结果
 * - false (生产模式): 调用真实的身份认证服务（学信网、公安部等）
 */
@Slf4j
@Service
public class IMPL_IdentityVerificationService implements IdentityVerificationService {

    @Resource
    private AppProperties appProperties;

    @Override
    public VerificationResult verifyStudent(String name, String idCard, String studentNumber) {
        // 先进行参数校验，避免 NullPointerException
        if (name == null || name.isEmpty()) {
            return VerificationResult.fail("姓名不能为空");
        }

        if (idCard == null || idCard.length() != 18) {
            return VerificationResult.fail("身份证号格式不正确");
        }

        if (studentNumber == null || studentNumber.isEmpty()) {
            return VerificationResult.fail("学号不能为空");
        }

        if (appProperties.getIdentityVerification().isMockMode()) {
            // 开发模式：模拟验证
            log.info("【开发模式】学生身份验证 - 姓名: {}, 身份证: {}****, 学号: {}",
                    name, idCard.substring(0, 6), studentNumber);
            log.warn("【注意】当前为模拟验证，生产环境请设置 app.identity-verification.mock-mode=false");

            // 从身份证号解析性别（倒数第二位奇数为男，偶数为女）
            String gender = (Integer.parseInt(idCard.substring(16, 17)) % 2 == 1) ? "男" : "女";

            // 模拟返回验证成功的信息
            VerifiedInfo info = new VerifiedInfo(
                    name,
                    gender,
                    "北京化工大学",  // 模拟机构
                    null,
                    "计算机" + studentNumber.substring(0, 2) + "班",  // 模拟班级
                    "本科",
                    "20" + studentNumber.substring(0, 2) + "-09-01"  // 模拟入学日期
            );

            return VerificationResult.success(info);
        }
        else {
            // 生产模式：调用真实的学生身份验证服务
            // TODO: 接入真实的学生身份验证服务（如学信网）
            // 示例代码：
            // HttpRequest request = HttpRequest.newBuilder()
            //     .uri(URI.create(apiUrl + "/student/verify"))
            //     .header("Authorization", "Bearer " + token)
            //     .header("Content-Type", "application/json")
            //     .POST(HttpRequest.BodyPublishers.ofString(
            //         "{\"name\":\"" + name + "\",\"idCard\":\"" + idCard + "\",\"studentNumber\":\"" + studentNumber + "\"}"
            //     ))
            //     .build();
            // HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("【生产模式】学生身份验证 - 姓名: {}, 学号: {}", name, studentNumber);

            // 临时返回失败，需要实现真实的验证逻辑
            return VerificationResult.fail("生产环境身份验证服务尚未配置，请联系管理员");
        }
    }

    @Override
    public VerificationResult verifyTeacher(String name, String idCard, String employeeNumber) {
        // 先进行参数校验，避免 NullPointerException
        if (name == null || name.isEmpty()) {
            return VerificationResult.fail("姓名不能为空");
        }

        if (idCard == null || idCard.length() != 18) {
            return VerificationResult.fail("身份证号格式不正确");
        }

        if (employeeNumber == null || employeeNumber.isEmpty()) {
            return VerificationResult.fail("工号不能为空");
        }

        if (appProperties.getIdentityVerification().isMockMode()) {
            // 开发模式：模拟验证
            log.info("【开发模式】教师身份验证 - 姓名: {}, 身份证: {}****, 工号: {}",
                    name, idCard.substring(0, 6), employeeNumber);
            log.warn("【注意】当前为模拟验证，生产环境请设置 app.identity-verification.mock-mode=false");

            // 从身份证号解析性别
            String gender = (Integer.parseInt(idCard.substring(16, 17)) % 2 == 1) ? "男" : "女";

            // 模拟返回验证成功的信息
            VerifiedInfo info = new VerifiedInfo(
                    name,
                    gender,
                    "北京化工大学",  // 模拟机构
                    "信息科学与技术学院",  // 模拟院系
                    null,
                    "博士",
                    "20" + employeeNumber.substring(0, 2) + "-07-01"  // 模拟入职日期
            );

            return VerificationResult.success(info);
        }
        else {
            // 生产模式：调用真实的教师身份验证服务
            // TODO: 接入真实的教师身份验证服务
            // 可对接学校人事系统API或第三方身份认证服务

            log.info("【生产模式】教师身份验证 - 姓名: {}, 工号: {}", name, employeeNumber);

            // 临时返回失败，需要实现真实的验证逻辑
            return VerificationResult.fail("生产环境身份验证服务尚未配置，请联系管理员");
        }
    }
}

