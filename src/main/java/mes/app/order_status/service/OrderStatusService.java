package mes.app.order_status.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.actasEntity.TB_DA006W;
import mes.domain.entity.actasEntity.TB_DA006W_PK;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrderStatusService {

    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getOrderStatusByOperid(String startDate, String endDate, String perid, String spjangcd, String searchCltnm, String searchtketnm, String searchstate) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("perid", perid);
        params.addValue("spjangcd", spjangcd);

        if (startDate != null && !startDate.isEmpty()) {
            params.addValue("startDate", startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            params.addValue("endDate", endDate);
        }

        StringBuilder sql = new StringBuilder("""
       SELECT
          tb006.*,
          uc.Value AS ordflag_display,
          (
              SELECT ISNULL((
                  SELECT
                      bd.filepath,
                      bd.filesvnm,
                      bd.fileextns,
                      bd.fileurl,
                      bd.fileornm,
                      bd.filesize,
                      bd.fileid
                  FROM
                      tb_DA006WFILE bd
                  WHERE
                      bd.custcd = tb006.custcd
                      AND bd.spjangcd = tb006.spjangcd
                      AND bd.reqdate = tb006.reqdate
                      AND bd.reqnum = tb006.reqnum
                  ORDER BY
                      bd.indatem DESC
                  FOR JSON PATH
              ), '[]')
          ) AS hd_files
      FROM
          TB_DA006W tb006 
      left join user_code uc on uc.Code = tb006.ordflag
      WHERE
          tb006.spjangcd = :spjangcd
    """);
        
        // 날짜 필터링 (TB_DA006W 기준)
        if (startDate != null && !startDate.isEmpty()) {
            startDate = startDate.replace("-", ""); // "2025-03-01" -> "20250301"
            sql.append(" and tb006.reqdate >= :startDate ");
            params.addValue("startDate", startDate );
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append("  AND tb006.reqdate <= :endDate ");
            params.addValue("endDate", endDate);
        }

        // 검색 조건 추가 (TB_DA006W 기준)
        if (searchCltnm != null && !searchCltnm.isEmpty()) {
            sql.append(" AND tb006.cltnm LIKE :searchCltnm ");
            params.addValue("searchCltnm", "%" + searchCltnm + "%"); //`%` 추가하여 LIKE 검색 가능하도록 변경
        }
        if (searchtketnm != null && !searchtketnm.isEmpty()) {
            sql.append(" AND tb006.remark LIKE :searchtketnm ");
            params.addValue("searchtketnm", "%" + searchtketnm + "%");
        }

        // "전체"일 경우 조건을 추가하지 않음
        if (searchstate != null && !searchstate.equals("all") && !searchstate.isEmpty()) {
            sql.append(" AND tb006.ordflag = :searchstate ");
            params.addValue("searchstate", searchstate);
        }

        // 정렬 조건 추가
        sql.append(" ORDER BY tb006.reqdate DESC");

        //log.info(" 실행될 SQL: {}", sql);
        //log.info("바인딩된 파라미터: {}", params.getValues());

        return sqlRunner.getRows(sql.toString(), params);
    }



    public List<Map<String, Object>> getModalListByClientName(String searchTerm) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        // searchTerm이 있을 때만 LIKE 조건 추가
        String sql = """
        SELECT *
                FROM TB_XCLIENT
        """ + (searchTerm != null && !searchTerm.isEmpty() ? " WHERE cltnm LIKE :searchTerm" : "");

        // searchTerm이 비어 있지 않을 때만 파라미터에 추가
        if (searchTerm != null && !searchTerm.isEmpty()) {
            params.addValue("searchTerm", "%" + searchTerm + "%");
        }
        return sqlRunner.getRows(sql, params);
    }

    public List<Map<String, Object>> searchData(String startDate, String endDate, String searchCltnm, String searchtketnm, String searchstate) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (startDate != null && !startDate.isEmpty()) {
            params.addValue("startDate", startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            params.addValue("endDate", endDate);
        }
        if (searchCltnm != null && !searchCltnm.isEmpty()) {
            params.addValue("searchCltnm", "%" + searchCltnm + "%");
        }
        if (searchtketnm != null && !searchtketnm.isEmpty()) {
            params.addValue("searchtketnm", "%" + searchtketnm + "%");
        }
        if (searchstate != null && !searchstate.isEmpty() && !searchstate.equals("전체")) {
            params.addValue("searchstate", searchstate);
        }

        // 기본 SQL 쿼리 작성
        String sql = """
        SELECT
            tb007.*,
            tb007.reqdate,
            tb006.*,
            tb006.cltnm,
            tb006.remark,
            tb006.ordflag,
            (
                   SELECT
                       bd.filepath,
                       bd.filesvnm,
                       bd.fileextns,
                       bd.fileurl,
                       bd.fileornm,
                       bd.filesize,
                       bd.fileid
                   FROM
                       tb_DA006WFILE bd
                   WHERE
                       bd.custcd = tb007.custcd
                       AND bd.spjangcd = tb007.spjangcd
                       AND bd.reqdate = tb007.reqdate
                       AND bd.reqnum = tb007.reqnum
                   ORDER BY
                       bd.indatem DESC
                   FOR JSON PATH
               ) AS hd_files
        FROM
            TB_DA007W tb007
        LEFT JOIN
            TB_DA006W tb006
        ON
            tb007.custcd = tb006.custcd
            AND tb007.spjangcd = tb006.spjangcd
            AND tb007.reqdate = tb006.reqdate
            AND tb007.reqnum = tb006.reqnum
        WHERE 1=1
    """;

        // 조건 추가
        if (params.hasValue("startDate")) {
            sql += " AND tb007.reqdate >= :startDate";
        }
        if (params.hasValue("endDate")) {
            sql += " AND tb007.reqdate <= :endDate";
        }
        if (params.hasValue("searchCltnm")) {
            sql += " AND tb006.cltnm LIKE :searchCltnm";
        }
        if (params.hasValue("searchtketnm")) {
            sql += " AND tb006.remark LIKE :searchtketnm";
        }
        if (params.hasValue("searchstate")) {
            sql += " AND tb006.ordflag = :searchstate";
        }

        // 쿼리 실행 및 결과 반환
//        log.info(" 실행될 SQL: {}", sql);
//        log.info("바인딩된 파라미터: {}", params.getValues());
        return sqlRunner.getRows(sql, params);
    }

    public String getOrdtextByParams(String reqdate, String remark) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("reqdate", reqdate);
        params.addValue("remark", remark);

        // ordtext를 가져오는 SQL 쿼리 작성
        String sql = """  
        SELECT
            tb007.ordtext,
            tb007.*,
            tb006.*
        FROM
            TB_DA007W tb007
        JOIN
            TB_DA006W tb006
        ON
            tb007.custcd = tb006.custcd
            AND tb007.spjangcd = tb006.spjangcd
            AND tb007.reqdate = tb006.reqdate
            AND tb007.reqnum = tb006.reqnum
        WHERE
            tb006.reqdate = :reqdate
            AND tb006.remark = :remark
        """;

        // 쿼리 실행 및 결과 반환
        List<Map<String, Object>> result = sqlRunner.getRows(sql, params);

        // 결과에서 ordtext 값을 추출
        if (!result.isEmpty() && result.get(0).get("ordtext") != null) {
            return result.get(0).get("ordtext").toString();
        }
        return null; // 데이터가 없을 경우 null 반환
    }

    // username으로 cltcd, cltnm, saupnum, custcd 가지고 오기
    public Map<String, Object> getUserInfo(String username) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                select xc.custcd,
                       xc.cltcd,
                       xc.cltnm,
                       xc.saupnum,
                       au.spjangcd
                FROM TB_XCLIENT xc
                left join auth_user au on au."username" = xc.saupnum
                WHERE xc.saupnum = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> userInfo = this.sqlRunner.getRow(sql, dicParam);
        return userInfo;
    }

    //주문현황 그리드
    public List<Map<String, Object>> getOrderList(TB_DA006W_PK tbDa006W_pk,
                                                  String searchStartDate, String searchEndDate, String searchType) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("searchStartDate", searchStartDate);
        dicParam.addValue("searchEndDate", searchEndDate);
        dicParam.addValue("searchType", searchType);
        dicParam.addValue("custcd", tbDa006W_pk.getCustcd());
        dicParam.addValue("spjangcd", tbDa006W_pk.getSpjangcd());

        StringBuilder sql = new StringBuilder("""
                SELECT
                    *
                FROM
                    TB_DA006W hd
                WHERE
                    hd.spjangcd = :spjangcd
                """);
        // 날짜 필터
        if (searchStartDate != null && !searchStartDate.isEmpty()) {
            sql.append(" AND reqdate >= :searchStartDate");
        }
        //
        if (searchEndDate != null && !searchEndDate.isEmpty()) {
            sql.append(" AND reqdate <= :searchEndDate");
        }
        // 진행구분 필터
        if (searchType != null && !searchType.isEmpty()) {
            sql.append(" AND ordflag LIKE :searchType");
        }
        // 정렬 조건 추가
        sql.append(" ORDER BY reqdate ASC");

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }

    public List<Map<String, Object>> initDatas(TB_DA006W_PK tbDa006WPk, String searchStartDate, String searchEndDate) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder("""
                SELECT
                    hd.ordflag,
                    COUNT(*) AS ordflag_count
                FROM
                    TB_DA006W hd
                WHERE
                    hd.spjangcd = :spjangcd
                    AND hd.reqdate BETWEEN :searchStartDate AND :searchEndDate
                GROUP BY
                    hd.ordflag;
                """);
        dicParam.addValue("spjangcd", tbDa006WPk.getSpjangcd());
        dicParam.addValue("searchStartDate", searchStartDate);
        dicParam.addValue("searchEndDate", searchEndDate);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }

    // 주문현황 캘린더
    public List<Map<String, Object>> getOrderList2(TB_DA006W_PK tbDa006W_pk) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("custcd", tbDa006W_pk.getCustcd());
        dicParam.addValue("spjangcd", tbDa006W_pk.getSpjangcd());

        StringBuilder sql = new StringBuilder("""
                SELECT
                    custcd,
                    spjangcd,
                    reqnum,
                    reqdate,
                    ordflag,
                    deldate,
                    telno,
                    perid,
                    cltzipcd,
                    cltaddr,
                    remark
                FROM
                    TB_DA006W hd
                WHERE
                    hd.spjangcd = :spjangcd
                    AND hd.reqdate BETWEEN
                        CAST(CAST(YEAR(GETDATE()) - 1 AS VARCHAR(4)) + '0101' AS INT)
                        AND CAST(CAST(YEAR(GETDATE()) AS VARCHAR(4)) + '1231' AS INT)
                """);
        // 정렬 조건 추가
        sql.append(" ORDER BY reqdate ASC");

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }


    public TB_DA006W UpdateOrdflag(List<Map<String, Object>> orders) {
        for (Map<String, Object> order : orders) {
            String reqnum = (String) order.get("reqnum"); // 주문 번호
            String ordflag = (String) order.get("ordflag"); // 0 또는 1 (문자열)

            // "0" → "1", "1" → "0" 변환 후 업데이트
            String newOrdflag = "0".equals(ordflag) ? "1" : "0";

            String sql = """
            UPDATE TB_DA006W 
            SET ordflag = :ordflag
            WHERE reqnum = :reqnum
        """;

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("ordflag", newOrdflag);
            params.addValue("reqnum", reqnum);

//            log.info("📌 주문 상태 변경 SQL 실행: {}", sql);
//            log.info("📌 SQL Parameters: {}", params.getValues());

            sqlRunner.execute(sql, params);
        }

        return new TB_DA006W(); // 업데이트 결과 반환 (실제 로직에 맞게 수정 필요)
    }

    public int CancelOrderUpdateOrdflag(List<Map<String, Object>> orders) {
        int updatedCount = 0;
        for (Map<String, Object> order : orders) {
            String reqnum = (String) order.get("reqnum"); // 주문 번호

            String sql = """
        UPDATE TB_DA006W 
        SET ordflag = :ordflag
        WHERE reqnum = :reqnum
        """;

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("ordflag", "5"); // 컨트롤러에서 "5"로 변환했으므로 그대로 저장
            params.addValue("reqnum", reqnum);

            // SQL 실행 로그 추가
//            log.info("📌 실행할 SQL: {}", sql);
//            log.info("📌 SQL 파라미터: {}", params.getValues());

            // SQL 실행 및 변경된 행 수 확인
            int result = sqlRunner.execute(sql, params);
            updatedCount += result;
        }

        // 업데이트된 행 수 반환
        return updatedCount;
    }


}