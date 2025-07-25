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
            h.PROCD as project_no,
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
              WHEN COUNT(*) = SUM(CASE WHEN d.CHULFLAG = '1' THEN 1 ELSE 0 END) THEN '출고'
              WHEN SUM(CASE WHEN d.CHULFLAG = '1' THEN 1 ELSE 0 END) = 0 THEN '미출고'
              ELSE '부분 출고'
            END AS chulflag,
            -- 공장 상태
            CASE
              WHEN COUNT(*) = SUM(CASE WHEN d.FACFLAG = '1' THEN 1 ELSE 0 END) THEN '출고'
              WHEN SUM(CASE WHEN d.FACFLAG = '1' THEN 1 ELSE 0 END) = 0 THEN '미출고'
              ELSE '부분 출고'
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
        h.PROCD as project_no,
        p.PROJECT_NM ,
        STUFF(STUFF(h.ICHDATE, 5, 0, '-'), 8, 0, '-') as ichdate,
        h.PERNM as pernm,
        h.BALJUDATE ,
        jp.rspcd as pernm_rspcdcd,
        pz.rspnm as pernm_rspcd,
        jp.perid as pernmcd,
        h.PERTELNO as pertelno,
        h.actcd, 
        h.ACTNM as actnm,
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
        d.pcode,
        d.procd ,
        d.PNAME as txtPname,
        d.psize,
        d.pqty, d.punit,
        d.puamt,d.pamt, d.pmapseq, d.remark,
        d.chulflag, d.facflag, d.hyunflag
        from tb_ca660 h
        left join tb_ca661 d on h.BALJUNUM = d.BALJUNUM and d.BALJUDATE = h.BALJUDATE
        left join tb_ca664 p on h.PROCD = p.PROJECT_NO
        left join tb_ja001 jp on h.PERNM = jp.pernm
        left join tb_ja001 jb on jb.pernm = h.CLTPERNM
        left join tb_pz001 pz on pz.RSPCD = jp.rspcd
        left join TB_XCLIENT c on c.cltcd =h.CLTCD
        where h.BALJUNUM = :baljunum;
        """;
//    log.info("발주상세 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    List<Map<String, Object>> rows = sqlRunner.getRows(sql, paramMap);

    if (rows.isEmpty()) return Collections.emptyMap();

    // 공통 헤더 정보 (첫 번째 row 기준)
    Map<String, Object> header = new LinkedHashMap<>();
    Map<String, Object> first = rows.get(0);

    header.put("mode", "edit");
    header.put("BALJUDATE", first.get("BALJUDATE"));
    header.put("BALJUNUM", first.get("BALJUNUM"));
    header.put("project_no", first.get("project_no"));
    header.put("PROJECT_NM", first.get("PROJECT_NM"));
    header.put("ichdate", first.get("ichdate"));
    header.put("pernm", first.get("pernm"));
    header.put("pernm_rspcd", first.get("pernm_rspcd"));
    header.put("pernm_rspcdcd", first.get("pernm_rspcdcd"));
    header.put("pernmcd", first.get("pernmcd"));
    header.put("pertelno", first.get("pertelno"));
    header.put("actcd", first.get("actcd"));
    header.put("actnm", first.get("actnm"));
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
      item.put("pcode", row.get("pcode"));
      item.put("procd", row.get("procd"));
      item.put("punit", row.get("punit"));
      item.put("txtPname", row.get("txtPname"));
      item.put("psize", row.get("psize"));
      item.put("pqty", row.get("pqty"));
      item.put("puamt", row.get("puamt"));
      item.put("pamt", row.get("pamt"));
      item.put("pmapseq", row.get("pmapseq"));
      item.put("remark", row.get("remark"));
      item.put("chulflag", row.get("chulflag"));
      item.put("facflag", row.get("facflag"));
      item.put("hyunflag", row.get("hyunflag"));

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

  public Map<String, Object> getxclent(String spjangcd) {

    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("spjangcd", spjangcd);

    String sql = """
        select
        x.saupnum ,
        x.spjangnm ,
        x.prenm ,
        x.adresa ,
        x.fax,
        x.tel1,
        x.emailadres 
        from tb_xa012 x
        where spjangcd =:spjangcd;
        """;
    return sqlRunner.getRow(sql, dicParam);
  }


  public List<Map<String, Object>> getBomList(String project_no, String eco_no) {
  MapSqlParameterSource dicParam = new MapSqlParameterSource();
  dicParam.addValue("projectNo", project_no);
  dicParam.addValue("ecoNo", eco_no);
  String sql = """
     SELECT 
      b.ECO_NO,
      b.PROJECT_NO,
      b.HOGI_NO, 
      b.PARENT_NO, 
      b.CHILD_NO,
      b.SEQ, b.QTY, b.CMT,
      b.BPDATE AS BOM_BPDATE,
      b.BPPERNM AS BOM_BPPERNM,
      p1.PART_NM AS PARENT_PART_NM, --모품목
      p1.PLM_VERSION AS PARENT_VERSION,
      p2.PART_NO, p2.PART_NM, p2.PLM_VERSION,
      p2.BLOCK_NO, p2.G_NO, 
      p2.DRAWING_NO,
      p2.GUBUN,
      p2.UNIT, 
      p2.PART_SIZE, 
      p2.SPEC,
      p2.BPDATE AS PART_BPDATE,
      p2.BPPERNM AS PART_BPPERNM
     FROM TB_CA663 b
     LEFT JOIN TB_CA662 p1 ON b.ECO_NO = p1.ECO_NO AND b.PARENT_NO = p1.PART_NO
     LEFT JOIN TB_CA662 p2 ON b.ECO_NO = p2.ECO_NO AND b.CHILD_NO = p2.PART_NO
     WHERE b.ECO_NO = :ecoNo AND b.PROJECT_NO = :projectNo;
      """;
//    log.info("bom list 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", dicParam.getValues());
    return sqlRunner.getRows(sql, dicParam);
  }

}
