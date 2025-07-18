package mes.app.balju.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class BaljuOrderService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getBaljuList(String date_kind, Timestamp start, Timestamp end, String spjangcd) {
    MapSqlParameterSource param = new MapSqlParameterSource();

    param.addValue("spjangcd", spjangcd);
    param.addValue("start", start);
    param.addValue("end", end);

    String sql = """
WITH main_data AS (
        SELECT
            h.BALJUNUM,
            h.CUSTCD,
            h.SPJANGCD,
            STUFF(STUFF(h.BALJUDATE, 5, 0, '-'), 8, 0, '-') AS BALJUDATE,
            h.CLTCD,
            h.PROCD as projcet_no,
            STUFF(STUFF(h.ICHDATE, 5, 0, '-'), 8, 0, '-') as ichdate,
            h.PERNM,
            h.PERTELNO,
            h.ACTCD,
            h.ACTNM,
            h.ACTADDRESS,
            h.CLTPERNM,
            h.CLTJIK,
            h.CLTTELNO,
            h.CLTEMAIL,
            p.PROJECT_NM,
            h.REMARK01 as remark,
            SUM(d.PQTY)    AS pqty,
            SUM(d.PUAMT)   AS puamt,
            SUM(d.PAMT)    AS pamt,
            -- 출고 상태
            CASE
              WHEN COUNT(*) = SUM(CASE WHEN d.CHULFLAG = '1' THEN 1 ELSE 0 END) THEN '확인'
              WHEN SUM(CASE WHEN d.CHULFLAG = '1' THEN 1 ELSE 0 END) = 0 THEN '미확인'
              ELSE '부분 확인'
            END AS chulflag,
            -- 공장 상태
            CASE
              WHEN COUNT(*) = SUM(CASE WHEN d.FACFLAG = '1' THEN 1 ELSE 0 END) THEN '확인'
              WHEN SUM(CASE WHEN d.FACFLAG = '1' THEN 1 ELSE 0 END) = 0 THEN '미확인'
              ELSE '부분 확인'
            END AS facflag,
            -- 현장 상태
            CASE
              WHEN COUNT(*) = SUM(CASE WHEN d.HYUNFLAG = '1' THEN 1 ELSE 0 END) THEN '확인'
              WHEN SUM(CASE WHEN d.HYUNFLAG = '1' THEN 1 ELSE 0 END) = 0 THEN '미확인'
              ELSE '부분 확인'
            END AS hyunflag
        FROM TB_CA660 h
        JOIN TB_CA661 d ON h.BALJUNUM = d.BALJUNUM
        JOIN TB_CA664 p ON p.PROJECT_NO = h.PROCD
        WHERE h.SPJANGCD = :spjangcd
    """;

    // 날짜 조건
    if ("sales".equalsIgnoreCase(date_kind)) {
      sql += " AND h.BALJUDATE BETWEEN :start AND :end ";
    } else {
      sql += " AND h.ICHDATE BETWEEN :start AND :end ";
    }

    sql += """
    GROUP BY
               h.BALJUNUM, h.CUSTCD, h.SPJANGCD, h.BALJUDATE, h.CLTCD, h.ICHDATE,
               h.PERNM, h.PERTELNO, h.ACTCD, h.ACTNM, h.ACTADDRESS,
               h.CLTPERNM, h.CLTJIK, h.CLTTELNO, h.CLTEMAIL,
               h.PROCD, p.PROJECT_NM, h.REMARK01
       ),
       first_pmapseq AS (
       SELECT d.BALJUNUM, d.PMAPSEQ, d.PNAME, d.punit
       FROM (
           SELECT BALJUNUM, PMAPSEQ, PNAME, punit,
                  ROW_NUMBER() OVER (PARTITION BY BALJUNUM ORDER BY BALJUSEQ ASC) AS rn
           FROM TB_CA661
       ) d
       WHERE rn = 1
   )
       SELECT
           m.*,
           f.PMAPSEQ AS pmapseq,
           f.PNAME AS pname,
           f.punit as punit
       FROM main_data m
       LEFT JOIN first_pmapseq f ON m.BALJUNUM = f.BALJUNUM
       ORDER BY m.BALJUDATE ASC
    """;

//    log.info("발주 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", param.getValues());

    return sqlRunner.getRows(sql, param);
  }

  public Map<String, Object> getBaljuDetail(int baljunum) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("baljunum", baljunum);

    String sql = """
        select
        h.BALJUNUM,
        h.PROCD as projcet_no,
        p.PROJECT_NM ,
        STUFF(STUFF(h.ICHDATE, 5, 0, '-'), 8, 0, '-') as ichdate,
        h.PERNM as pernm,
        jp.rspcd as pernm_rspcd,
        pz.rspnm as pernm_rspcdcd,
        jp.perid as pernmcd,
        h.PERTELNO as pertelno,
        h.ACTCD,
        h.ACTNM as actcd,
        h.ACTADDRESS as actaddress,
        h.CLTCD as cltcd,
        c.cltnm as CompanyName,
        jb.rspcd as cltjikcd,
        h.CLTJIK as cltjik,
        jb.perid as cbocltcd,
        h.CLTPERNM as cltpernm,
        h.CLTTELNO as clttelno,
        h.CLTEMAIL as cltemail,
        h.remark01,
        h.remark02,
        h.remark01,
        d.BALJUSEQ,
        d.procd ,
        d.PNAME as txtPname,
        d.psize,
        d.pqty, d.punit,
        d.puamt,d.pamt, d.pmapseq, d.remark
        from tb_ca660 h
        left join tb_ca661 d on h.BALJUNUM = d.BALJUNUM and d.BALJUDATE = h.BALJUDATE
        left join tb_ca664 p on h.PROCD = p.PROJECT_NO
        left join tb_ja001 jp on h.PERNM = jp.pernm
        left join tb_ja001 jb on jb.pernm = h.CLTPERNM
        left join tb_pz001 pz on pz.RSPCD = jp.rspcd
        left join TB_XCLIENT c on c.cltcd =h.CLTCD
        where h.BALJUNUM = :baljunum;
        """;
    log.info("발주상세 데이터 SQL: {}", sql);
    log.info("SQL Parameters: {}", paramMap.getValues());
    List<Map<String, Object>> rows = sqlRunner.getRows(sql, paramMap);

    if (rows.isEmpty()) return Collections.emptyMap();

    // 공통 헤더 정보 (첫 번째 row 기준)
    Map<String, Object> header = new LinkedHashMap<>();
    Map<String, Object> first = rows.get(0);

    header.put("mode", "edit");
    header.put("BALJUNUM", first.get("BALJUNUM"));
    header.put("project_no", first.get("projcet_no"));
    header.put("PROJECT_NM", first.get("PROJECT_NM"));
    header.put("ichdate", first.get("ichdate"));
    header.put("pernm", first.get("pernm"));
    header.put("pernm_rspcd", first.get("pernm_rspcd"));
    header.put("pernm_rspcdcd", first.get("pernm_rspcdcd"));
    header.put("pernmcd", first.get("pernmcd"));
    header.put("pertelno", first.get("pertelno"));
    header.put("ACTCD", first.get("ACTCD"));
    header.put("actnm", first.get("actcd"));  // 중복 있음
    header.put("actaddress", first.get("actaddress"));
    header.put("cltcd", first.get("cltcd"));
    header.put("CompanyName", first.get("CompanyName"));
    header.put("cltjikcd", first.get("cltjikcd"));
    header.put("cltjik", first.get("cltjik"));
    header.put("cbocltcd", first.get("cbocltcd"));
    header.put("cltpernm", first.get("cltpernm"));
    header.put("clttelno", first.get("clttelno"));
    header.put("cltemail", first.get("cltemail"));
    header.put("remark01", first.get("remark01"));
    header.put("remark02", first.get("remark02"));

// items 처리
    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("BALJUSEQ", row.get("BALJUSEQ"));
      item.put("procd", row.get("procd"));
      item.put("punit", row.get("punit"));
      item.put("txtPname", row.get("txtPname"));
      item.put("psize", row.get("psize"));
      item.put("pqty", row.get("pqty"));
      item.put("puamt", row.get("puamt"));
      item.put("pamt", row.get("pamt"));
      item.put("pmapseq", row.get("pmapseq"));
      item.put("remark", row.get("remark"));

      items.add(item);
    }

    header.put("items", items);
    return header;
  }

  public String getCustcd(String spjangcd) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("spjangcd", spjangcd);

    String sql= """
        select custcd  from tb_xa012
        where spjangcd =:spjangcd
        """;
    List<Map<String, Object>> rows = this.sqlRunner.getRows(sql, dicParam);

    if (rows.isEmpty()) {
      return null;
    }

    Object val = rows.get(0).get("custcd");
    return val != null ? val.toString() : null;
  }


}
