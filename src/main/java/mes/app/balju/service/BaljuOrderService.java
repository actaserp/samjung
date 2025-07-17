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

  public Map<String, Object> getBaljuDetail(int id) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    paramMap.addValue("id", id);

    String sql = """
        WITH balju_total AS (
            SELECT 
                "BaljuHead_id" AS bh_id,
                SUM(COALESCE("TotalAmount", 0)) AS total_amount_sum
            FROM balju
            GROUP BY "BaljuHead_id"
        )
        SELECT
            bh.id AS bh_id,
            bh."Company_id",
            c."Name" AS "CompanyName",
            bh."JumunDate",
            bh."DeliveryDate",
            bh.special_note,
            bh."JumunNumber",
            b.id AS balju_id,
            b."Material_id",
            COALESCE(m."Code", '') AS product_code,
            COALESCE(m."Name", '') AS product_name,
            COALESCE(mg."Name", '') AS "MaterialGroupName",
            COALESCE(mg.id, 0) AS "MaterialGroup_id",
            fn_code_name('mat_type', mg."MaterialType") AS "MaterialTypeName",
            s."Value" as "BaljuTypeName",
            b."SujuQty",
            u."Name" AS unit,
            b."UnitPrice" AS "BaljuUnitPrice",
            b."Price" AS "BaljuPrice",
            b."Vat" AS "BaljuVat",
            b."InVatYN",
            b."TotalAmount" AS "LineTotalAmount",
            COALESCE(bt.total_amount_sum, 0) AS "BaljuTotalPrice", 
            TO_CHAR(b."ProductionPlanDate", 'yyyy-mm-dd') AS production_plan_date,
            TO_CHAR(b."ShipmentPlanDate", 'yyyy-mm-dd') AS shiment_plan_date,
            b."Description",
            b."AvailableStock",
            b."ReservationStock",
            mi."SujuQty2",
            -- 동적 계산된 Head 상태
            (
                SELECT
                    CASE
                        WHEN COUNT(*) FILTER (WHERE b2."State" = 'received') = COUNT(*) THEN 'received'
                        WHEN COUNT(*) FILTER (WHERE b2."State" = 'draft') = COUNT(*) THEN 'draft'
                        WHEN COUNT(*) FILTER (WHERE b2."State" = 'canceled') = COUNT(*) THEN 'canceled'
                        ELSE 'partial'
                    END
                FROM balju b2
                WHERE b2."BaljuHead_id" = bh.id
            ) AS "BalJuHeadType",
            -- Head 상태명
            fn_code_name(
                'balju_state',
                (
                    SELECT
                        CASE
                            WHEN COUNT(*) FILTER (WHERE b2."State" = 'received') = COUNT(*) THEN 'received'
                            WHEN COUNT(*) FILTER (WHERE b2."State" = 'draft') = COUNT(*) THEN 'draft'
                            WHEN COUNT(*) FILTER (WHERE b2."State" = 'canceled') = COUNT(*) THEN 'canceled'
                            ELSE 'partial'
                        END
                    FROM balju b2
                    WHERE b2."BaljuHead_id" = bh.id
                )
            ) AS "bh_StateName",
            -- 개별 balju 상태
            b."State" AS "BalJuType",
            fn_code_name('balju_state', b."State") AS "balju_StateName",
            TO_CHAR(b."_created", 'yyyy-mm-dd') AS create_date
        FROM balju_head bh
        LEFT JOIN balju b ON b."BaljuHead_id" = bh.id
        LEFT JOIN material m ON m.id = b."Material_id" AND m.spjangcd = b.spjangcd
        LEFT JOIN mat_grp mg ON mg.id = m."MaterialGroup_id" AND mg.spjangcd = b.spjangcd
        LEFT JOIN unit u ON m."Unit_id" = u.id AND u.spjangcd = b.spjangcd
        LEFT JOIN company c ON c.id = b."Company_id"
        left join sys_code s on bh."SujuType" = s."Code" and s."CodeType" = 'Balju_type'
        LEFT JOIN (
            SELECT "SourceDataPk", SUM("InputQty") AS "SujuQty2"
            FROM mat_inout
            WHERE "SourceTableName" = 'balju' AND COALESCE("_status", 'a') = 'a'
            GROUP BY "SourceDataPk"
        ) mi ON mi."SourceDataPk" = b.id
        LEFT JOIN balju_total bt ON bt.bh_id = bh.id
        WHERE bh.id = :id
        """;
//    log.info("발주상세 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    List<Map<String, Object>> rows = sqlRunner.getRows(sql, paramMap);

    if (rows.isEmpty()) return Collections.emptyMap();

    // 공통 헤더 정보 (첫 번째 row 기준)
    Map<String, Object> header = new LinkedHashMap<>();
    Map<String, Object> first = rows.get(0);

    header.put("mode", "edit");
    header.put("id", first.get("bh_id"));
    header.put("Company_id", first.get("Company_id"));
    header.put("CompanyName", first.get("CompanyName"));
    header.put("JumunDate", first.get("JumunDate"));
    header.put("DeliveryDate", first.get("DeliveryDate"));
    header.put("State", first.get("BalJuHeadType"));
    header.put("StateName", first.get("bh_StateName"));
    header.put("special_note", first.get("special_note"));
    header.put("JumunNumber", first.get("JumunNumber"));

    List<Map<String, Object>> items = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Map<String, Object> item = new LinkedHashMap<>();

      item.put("id", row.get("balju_id"));
      item.put("Material_id", row.get("Material_id"));
      item.put("product_code", row.get("product_code"));
      item.put("product_name", row.get("product_name"));
      item.put("quantity", row.get("SujuQty"));
      item.put("unit_price", row.get("BaljuUnitPrice"));
      item.put("supply_price", row.get("BaljuPrice"));
      item.put("vat", row.get("BaljuVat"));
      item.put("total_price", row.get("LineTotalAmount"));
      item.put("description", row.get("Description"));
      item.put("vatIncluded", row.get("InVatYN"));
      item.put("State", row.get("BalJuType"));
      item.put("balju_StateName", row.get("balju_StateName"));

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
