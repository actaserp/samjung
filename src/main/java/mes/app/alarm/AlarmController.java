package mes.app.alarm;


import mes.app.alarm.Service.AlarmService;
import mes.domain.entity.User;
import mes.domain.entity.UserGroup;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/Alarm")
public class AlarmController {

    @Autowired
    AlarmService alarmService;

    @GetMapping("/notifications")
    public AjaxResult getNotifications(Authentication auth) {
        AjaxResult result = new AjaxResult();

        try {
            // 로그인한 사용자 정보
            User user = (User) auth.getPrincipal();
            String userId = user.getUsername();
            int userGroupId = user.getUserProfile().getUserGroup().getId();

            // 알림 데이터 조회
            List<Map<String, Object>> notifications = alarmService.getNotifications(userId, userGroupId);

            result.success = true;
            result.data = notifications;
            result.message = "알림 조회 성공";
        } catch (Exception e) {
            result.success = false;
            result.message = "알림 데이터를 가져오는 중 오류가 발생했습니다.";
        }

        return result;
    }

    @PostMapping("markAsRead")
    public AjaxResult markNotificationsAsRead(Authentication auth) {
        AjaxResult result = new AjaxResult();

        try {
            // 로그인한 사용자 정보 가져오기
            User user = (User) auth.getPrincipal();
            String userId = user.getUsername();
            UserGroup userGroupId = user.getUserProfile().getUserGroup();

            // 알림 상태 업데이트
            alarmService.markAsRead(userId, userGroupId);

            result.success = true;
            result.message = "알림 상태가 업데이트되었습니다.";
        } catch (Exception e) {
            result.success = false;
            result.message = "알림 상태 업데이트 중 오류가 발생했습니다.";
            e.printStackTrace();
        }

        return result;
    }

    @GetMapping("/role")
    public ResponseEntity<Map<String, Object>> GetUserRole(Authentication auth) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = (User) auth.getPrincipal();
            String userId = user.getUsername();

            Map<String, Object> userRole = alarmService.GetUserRole(userId);

            if (userRole.isEmpty()) {
                response.put("success", false);
                response.put("message", "사용자 정보를 찾을 수 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put("success", true);
            response.put("role_id", userRole.get("role_id")); // role_id 반환
            response.put("role", getRoleName((Integer) userRole.get("role_id"))); // role_name 반환
            response.put("message", "사용자 역할 조회 성공");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "사용자 역할 조회 중 오류 발생");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // role_id를 기반으로 역할명을 반환하는 메서드 (추가)
    private String getRoleName(Integer roleId) {
        return switch (roleId) {
            case 1 -> "admin_group";
            case 14 -> "order_manager";
            case 35 -> "general_client";
            default -> "unknown";
        };
    }


}
