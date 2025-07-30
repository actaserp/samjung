package mes.app.balju.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BalJuListServicr {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getBalJuList(String cltnm, String actnm, String date_kind, Timestamp start,
                                                Timestamp end, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("cltnm", cltnm);
    paramMap.addValue("actnm", actnm);
    paramMap.addValue("date_kind", date_kind);
    paramMap.addValue("start", start);
    paramMap.addValue("end", end);
    paramMap.addValue("spjangcd", spjangcd);

    String sql= """     
        SELECT 
          h.BALJUNUM,
          STUFF(STUFF(h.BALJUDATE, 5, 0, '-'), 8, 0, '-') AS BALJUDATE,
          d.BALJUSEQ,
          d.pmapseq,
          d.pname,
          d.PCODE,
          d.punit,
          d.pqty,
          d.puamt,
          d.pamt,
          d.pmapseq,
          CASE d.CHULFLAG
            WHEN '1' THEN '출고'
            ELSE '미출고'
          END AS CHULFLAG,
          STUFF(STUFF(d.CHULDATE, 5, 0, '-'), 8, 0, '-') AS CHULDATE,
          CASE d.FACFLAG
            WHEN '1' THEN '출고'
            ELSE '미출고'
          END AS facflag,
          STUFF(STUFF(d.FACDATE, 5, 0, '-'), 8, 0, '-') AS FACDATE,
          CASE d.HYUNFLAG
            WHEN '1' THEN '확인'
            ELSE '미확인'
          END AS hyunflag,
          STUFF(STUFF(d.HYUNDATE, 5, 0, '-'), 8, 0, '-') AS HYUNDATE,
          d.remark,
          h.CLTCD,
          c.CLTNM,
          h.PROCD as project_no,
          STUFF(STUFF(h.ICHDATE, 5, 0, '-'), 8, 0, '-') AS ichdate,
          h.PERNM,
          h.ACTNM,
          au3.last_name AS CHULPERNM,
          au2.last_name AS FACPERNM,
          au1.last_name AS HYUNPERNM,
             total_amt.TOTAL_PAMT
        FROM TB_CA661 d
        JOIN TB_CA660 h ON d.BALJUNUM = h.BALJUNUM
        LEFT JOIN (
          SELECT BALJUNUM, SUM(PAMT) AS TOTAL_PAMT
          FROM TB_CA661
          GROUP BY BALJUNUM
        ) total_amt ON d.BALJUNUM = total_amt.BALJUNUM
        LEFT JOIN TB_XCLIENT c ON c.CLTCD = h.CLTCD
        LEFT JOIN auth_user au1 ON d.HYUNPERNM = au1.username
        LEFT JOIN auth_user au2 ON d.FACPERNM = au2.username
        LEFT JOIN auth_user au3 ON d.CHULPERNM = au3.username
        WHERE h.SPJANGCD = :spjangcd""";

    // 날짜 조건
    if ("sales".equalsIgnoreCase(date_kind)) {
      sql += " AND h.BALJUDATE BETWEEN :start AND :end ";
    } else {
      sql += " AND h.ICHDATE BETWEEN :start AND :end ";
    }
    if (actnm != null && !actnm.isEmpty()) {
      sql += " and h.ACTNM like :actnm ";
      paramMap.addValue("actnm", "%" + actnm + "%");
    }

    if (cltnm != null && !cltnm.isEmpty()) {
      sql += " and c.CLTNM like :cltnm ";
      paramMap.addValue("cltnm", "%" + cltnm + "%");
    }

   sql+= """
        ORDER BY h.BALJUNUM, d.BALJUSEQ 
       """;
//    log.info("발주 list read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return sqlRunner.getRows(sql, paramMap);
  }
}
