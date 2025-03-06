package mes.app.ProgressStatus.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProgressStatusService {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getProgressStatusList(String perid, String search_spjangcd ,String startDate, String endDate) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("perid", perid);
        params.addValue("spjangcd", search_spjangcd);
        params.addValue("startDate", startDate);
        params.addValue("endDate", endDate);
        StringBuilder sql = new StringBuilder("""
                 SELECT
            tb006.*
        FROM
            TB_DA006W tb006
        WHERE
            1=1
     """);
        if (startDate != null && !startDate.isEmpty()) {
            startDate = startDate.replace("-", "");
            sql.append(" AND reqdate >= :startDate ");
            params.addValue("startDate", startDate);
        }

        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND reqdate <= :endDate ");
            params.addValue("endDate", endDate);
        }
        // ORDER BY를 항상 맨 마지막에 추가
        sql.append(" ORDER BY reqdate DESC");

//        log.info("진행현황 SQL: {}", sql);
//        log.info("SQL Parameters: {}", params.getValues());
        return sqlRunner.getRows(sql.toString(), params);
    }


    public List<Map<String, Object>> getChartData2(
            String userid, String search_spjangcd, String startDate, String endDate,
            String searchCltnm, String searchtketnm, String searchTitle) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
        SELECT cltnm, ordflag
        FROM tb_da006w
        WHERE 1=1
    """);

        if (startDate != null && !startDate.isEmpty()) {
            startDate = startDate.replace("-", "");
            sql.append(" AND reqdate >= :startDate");
            params.addValue("startDate", startDate);
        }

        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND reqdate <= :endDate");
            params.addValue("endDate", endDate);
        }

        if (searchCltnm != null && !searchCltnm.isEmpty()) {
            sql.append(" AND cltnm LIKE :cltnm");
            params.addValue("cltnm", "%" + searchCltnm + "%");
        }
        if (search_spjangcd != null && !search_spjangcd.isEmpty()) {
            sql.append(" AND spjangcd = :spjangcd");
            params.addValue("spjangcd", search_spjangcd);
        }

        if (searchtketnm != null && !searchtketnm.equals("all") && !searchtketnm.isEmpty()) {
            sql.append(" AND ordflag = :ordflag");
            params.addValue("ordflag", searchtketnm);
        }

        if (searchTitle != null && !searchTitle.isEmpty()) {
            sql.append(" AND remark LIKE :searchTitle");
            params.addValue("searchTitle", "%" + searchTitle + "%");
        }

//        log.info("실행될 SQL: {}", sql.toString());
//        log.info("바인딩된 파라미터: {}", params.getValues());

        return sqlRunner.getRows(sql.toString(), params);
    }

    public List<Map<String, Object>> searchProgressStatus(
            String startDate, String endDate,
            String searchTitle, String searchtketnm,
            String searchCltnm, String userid, String spjangcd, List<String> groupByColumns
    ) {
        // 동적 쿼리를 위한 매개변수 설정
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
        SELECT
            tb006.*
        FROM
            TB_DA006W tb006
        WHERE
            1=1
    """); // WHERE 1=1은 조건을 추가하기 쉽게 하기 위한 트릭입니다.

        // 조건 추가
        if (startDate != null && !startDate.isEmpty()) {
            startDate = startDate.replace("-", "");
            sql.append(" AND tb006.reqdate >= :startDate");
            params.addValue("startDate", startDate);
        }

        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" AND tb006.reqdate <= :endDate");
            params.addValue("endDate", endDate);
        }

        if (searchCltnm != null && !searchCltnm.isEmpty()) {
            sql.append(" AND tb006.cltnm LIKE :cltnm");
            params.addValue("cltnm", "%" + searchCltnm + "%");
        }

        if (searchtketnm != null && !searchtketnm.isEmpty()) {
            sql.append(" AND tb006.ordflag = :ordflag");
            params.addValue("ordflag", searchtketnm);
        }

        if (searchTitle != null && !searchTitle.isEmpty()) {
            sql.append(" AND tb006.remark LIKE :searchRemark");
            params.addValue("searchRemark", "%" + searchTitle + "%");
        }

        // Optional: spjangcd 필터링 추가
        if (spjangcd != null && !spjangcd.isEmpty()) {
            sql.append(" AND tb006.spjangcd = :spjangcd");
            params.addValue("spjangcd", spjangcd);
        }

        // 관리자(super)일 때는 saupnum 조건 제외
        if (userid != null && !userid.isEmpty()) {
            if (!"super".equals(userid) && !"seong".equals(userid)) {
                sql.append(" AND tb006.saupnum = :saupnum");
                params.addValue("saupnum", userid);
            }
        }

        // ORDER BY를 항상 맨 마지막에 추가
        sql.append(" ORDER BY tb006.reqdate DESC");

//   로그 출력 (활성화할 경우 사용)
//    log.info("검색 실행될 SQL: {}", sql.toString());
//    log.info("검색 바인딩된 파라미터: {}", params.getValues());

        // 데이터 조회
        return sqlRunner.getRows(sql.toString(), params);
    }

}
