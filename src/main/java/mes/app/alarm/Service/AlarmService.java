package mes.app.alarm.Service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.UserGroup;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AlarmService {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getNotifications(String userId, int userGroupId) {
        // SQL 파라미터 설정
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("saupnum", userId); // 로그인 사용자 ID 추가

        // 사용자 그룹 조건 처리
        String flagCondition;
        if (userGroupId == 35) {
            flagCondition = "saupnum = :saupnum AND userflag = '0'";
        } else if (userGroupId == 1 || userGroupId == 14) {
            flagCondition = "adflag = '0'";
        } else {
            throw new IllegalArgumentException("Invalid userGroupId: " + userGroupId);
        }

        // SQL 쿼리 작성
        String sql = """
        SELECT 
            adflag, 
            userflag, 
            FORMAT(CAST(reqdate AS DATE), 'yyyy-MM-dd') AS alert_date,
            cltnm, 
            ordflag 
        FROM 
            TB_DA006W 
        WHERE 
            %s
        ORDER BY 
            reqdate DESC;
        """.formatted(flagCondition);

        // 디버깅용 로그
//        log.info("Executing SQL: {} with params: {}", sql, params.getValues());

        // 쿼리 실행 및 결과 반환
        try {
            return sqlRunner.getRows(sql, params);
        } catch (Exception e) {
//            log.error("Error executing SQL: {}", sql, e);
            throw new RuntimeException("Failed to fetch notifications", e);
        }
    }

    @Transactional
    public void markAsRead(String userId, UserGroup userGroupId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("saupnum", userId.trim());

        String updateFlagSql;

        if (userGroupId.getId() == 1 || userGroupId.getId() == 14) {
            // 관리자의 경우 adflag 업데이트
            updateFlagSql = """
            UPDATE TB_DA006W
            SET adflag = '1'
             WHERE adflag = '0' OR adflag IS NULL;
        """;
        } else if (userGroupId.getId() == 35) {
            // 일반 사용자의 경우 userflag 업데이트
            updateFlagSql = """
            UPDATE TB_DA006W
            SET userflag = '1'
            WHERE saupnum = :saupnum
              AND (userflag = '0' OR userflag IS NULL);
        """;
        } else {
            throw new IllegalArgumentException("유효하지 않은 UserGroup ID입니다.");
        }

        // SQL 실행 전 상태 확인
//        log.info("Executing SQL with params: {}", params.getValues());
//        log.info("Before update: {}", sqlRunner.getRows(
//                "SELECT saupnum, adflag, userflag FROM TB_DA006W WHERE saupnum = :saupnum", params));

        // UPDATE 쿼리 실행
        int updatedRows = sqlRunner.execute(updateFlagSql, params);

        // 업데이트 결과 로그
//        log.info("Rows updated: {}", updatedRows);
//        log.info("After update: {}", sqlRunner.getRows(
//                "SELECT saupnum, adflag, userflag FROM TB_DA006W WHERE saupnum = :saupnum", params));
    }

    public Map<String, Object> GetUserRole(String userId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);

        String sql = """
        SELECT au.username, up.UserGroup_id AS role_id, au.last_name 
        FROM auth_user au
        LEFT JOIN user_profile up ON up.User_id = au.id
        WHERE au.username = :userId
    """;

        List<Map<String, Object>> rows = sqlRunner.getRows(sql, params);

        if (!rows.isEmpty()) {
            return rows.get(0); // 첫 번째 결과만 반환
        } else {
            return Collections.emptyMap(); // 사용자가 없을 경우 빈 Map 반환
        }
    }

}



