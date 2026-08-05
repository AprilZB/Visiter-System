package com.maitong.visitor.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maitong.visitor.service.DingTalkNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DingTalkNotificationServiceImpl implements DingTalkNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DingTalkNotificationServiceImpl.class);

    private static final String DING_SEARCH_URL = "http://10.11.100.154:8089/api/users/dingtalk-search?name=";
    private static final String DING_NOTIFY_URL = "http://10.11.100.154:8089/api/workflow/test-notify";

    // 测试人员限制锁定 (张勃 / zhangb9 / Bo.Zhang@accupathmed.com / 工号: 404256402 / UserID: 1636684046776099)
    private static final String TEST_USER_WORK_NO = "404256402";
    private static final String TEST_USER_NAME = "张勃";
    private static final String TEST_USER_DING_ID = "1636684046776099";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getDingUserIdByWorkNo(String workNo, String name) {
        // 如果没有指定姓名，默认用张勃查询
        String searchName = StringUtils.hasText(name) ? name.trim() : TEST_USER_NAME;
        try {
            String url = DING_SEARCH_URL + searchName;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode list = root.path("value");
                if (list.isArray()) {
                    for (JsonNode node : list) {
                        String jobNum = node.path("job_number").asText();
                        String userId = node.path("userid").asText();
                        if (StringUtils.hasText(workNo) && workNo.trim().equals(jobNum)) {
                            return userId;
                        }
                        // 如果名字匹配到了张勃且还没拿到 userId，备选使用
                        if (TEST_USER_NAME.equals(node.path("name").asText())) {
                            return userId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询钉钉 UserID 接口异常: name={}, workNo={}", name, workNo, e);
        }
        // 兜底返回测试人员张勃的 UserID
        return TEST_USER_DING_ID;
    }

    @Override
    public boolean sendWorkNotificationByWorkNo(String workNo, String name, String content) {
        // 关键限定：测试阶段只推送给测试人员张勃 (zhangb9)，防止扰乱他人
        String targetUserId = TEST_USER_DING_ID;

        // 如果明确指定了工号，且与张勃一致，进行线上 UserID 校验
        if (TEST_USER_WORK_NO.equals(workNo) || TEST_USER_NAME.equals(name)) {
            String searchedId = getDingUserIdByWorkNo(workNo, name);
            if (StringUtils.hasText(searchedId)) {
                targetUserId = searchedId;
            }
        } else {
            log.info("【测试阶段安全拦截】目标受访人 [{}-{}] 非测试人员张勃，通知已重定向发送给张勃(zhangb9)", name, workNo);
            content = "【测试拦截提醒】原始拟发给 [" + name + "] 的通知，当前测试重定向发给您:\n" + content;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("userId", targetUserId);
            body.put("msg", content);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(DING_NOTIFY_URL, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("成功发送钉钉工作通知给张勃 (userId={}): {}", targetUserId, response.getBody());
                return true;
            } else {
                log.error("发送钉钉通知失败, HTTP Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("发送钉钉工作通知发生网络或接口异常", e);
        }
        return false;
    }
}
