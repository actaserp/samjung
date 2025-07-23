package mes.app.plm.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class PlmService {

    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    @Qualifier("sqlRunnerOracle")
    SqlRunner oracleSqlRunner;

    public List<Map<String, Object>> getProjectListSynch(String srchStartDt, String srchEndDt, String txtDescription, String inputFlag) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("srchStartDt", srchStartDt);
        dicParam.addValue("srchEndDt", srchEndDt);
        dicParam.addValue("txtDescription", txtDescription);

        List<Map<String, Object>> result = new ArrayList<>();

        // inputFlag = "N" → Oracle에서 ERP_YN = 'N' 만 조회
        if ("N".equalsIgnoreCase(inputFlag)) {
            String oracleSql = """
            SELECT ECO_NO, PROJECT_NO, PROJECT_NM, ERP_YN
            FROM SJ_DEFAULT.ERP_PROJECT_BUFFER
            WHERE ERP_YN = 'N'
        """;

            if (StringUtils.hasText(txtDescription)) {
                oracleSql += """
                AND (
                    PROJECT_NM LIKE '%' || :txtDescription || '%'
                    OR PROJECT_NO LIKE '%' || :txtDescription || '%'
                )
            """;
            }

            result.addAll(oracleSqlRunner.getRows(oracleSql, dicParam));
        }
        // inputFlag = "Y" → Oracle의 ERP_YN = 'N' + MS에서 날짜 범위 조회
        else if ("Y".equalsIgnoreCase(inputFlag)) {
            // 1. Oracle ERP_YN = 'N'
            String oracleSql = """
                SELECT ECO_NO, PROJECT_NO, PROJECT_NM, ERP_YN
                FROM SJ_DEFAULT.ERP_PROJECT_BUFFER
                WHERE ERP_YN = 'N'
            """;

            if (StringUtils.hasText(txtDescription)) {
                oracleSql += """
                AND (
                    PROJECT_NM LIKE '%' || :txtDescription || '%'
                    OR PROJECT_NO LIKE '%' || :txtDescription || '%'
                    )
                """;
            }

            result.addAll(oracleSqlRunner.getRows(oracleSql, dicParam));

            // MS SQL TB_CA664 에서 날짜 범위로 조회
            String msSql = """
                SELECT ECO_NO, PROJECT_NO, PROJECT_NM, 'Y' AS ERP_YN, BPDATE, BPPERNM
                FROM TB_CA664
                WHERE BPDATE BETWEEN :srchStartDt AND :srchEndDt
            """;

            if (StringUtils.hasText(txtDescription)) {
                msSql += """
                AND (
                    PROJECT_NM LIKE '%' + :txtDescription + '%'
                    OR PROJECT_NO LIKE '%' + :txtDescription + '%'
                )
            """;
            }

            result.addAll(sqlRunner.getRows(msSql, dicParam));
        }

        return result;
    }


    public List<Map<String, Object>> getBomAndPartBuffer(String ecoNo, String projectNo, String erpYn) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ecoNo", ecoNo);
        params.addValue("projectNo", projectNo);

        if ("N".equalsIgnoreCase(erpYn)) {
            // Oracle 조회
            String oracleSql = """
            SELECT
                LEVEL AS BOM_LEVEL,
                b.ECO_NO,
                b.PROJECT_NO,
                b.HOGI_NO,
                b.PARENT_NO,
                b.CHILD_NO,
                b.SEQ,
                b.QTY,
                b.CMT,
            
                -- 모품 정보
                p1.PART_NM AS PARENT_PART_NM,
                p1.PLM_VERSION AS PARENT_VERSION,
            
                -- 자품 정보 (CHILD_NO 기준)
                p2.PART_NO,
                p2.PART_NM,
                p2.PLM_VERSION,
                p2.BLOCK_NO,
                p2.G_NO,
                p2.DRAWING_NO,
                p2.GUBUN,
                p2.UNIT,
                p2.PART_SIZE,
                p2.SPEC
            
            FROM SJ_DEFAULT.ERP_BOM_BUFFER b
            LEFT JOIN SJ_DEFAULT.ERP_PART_BUFFER p1
              ON b.PARENT_NO = p1.PART_NO
             AND b.ECO_NO = p1.ECO_NO
            LEFT JOIN SJ_DEFAULT.ERP_PART_BUFFER p2
              ON b.CHILD_NO = p2.PART_NO
             AND b.ECO_NO = p2.ECO_NO
            
            START WITH b.PARENT_NO IS NULL
            CONNECT BY PRIOR b.CHILD_NO = b.PARENT_NO
            
            ORDER SIBLINGS BY b.SEQ
            
        """;

            return oracleSqlRunner.getRows(oracleSql, params);

        } else {
            // MS SQL 조회
            String msSql = """
            -- 1. 자품목 행들
                  SELECT
                      b.ECO_NO,
                      b.PROJECT_NO,
                      b.HOGI_NO,
                      b.PARENT_NO,
                      b.CHILD_NO,
                      b.SEQ,
                      b.QTY,
                      b.CMT,
                      
                      -- 모품 정보
                      p1.PART_NM AS PARENT_PART_NM,
                      p1.PLM_VERSION AS PARENT_VERSION,
                  
                      -- 자품 정보 (자품 기준 alias 통일)
                      p2.PART_NO     AS PART_NO,
                      p2.PART_NM     AS PART_NM,
                      p2.PLM_VERSION AS PLM_VERSION,
                      p2.BLOCK_NO,
                      p2.G_NO,
                      p2.DRAWING_NO,
                      p2.GUBUN,
                      p2.UNIT,
                      p2.PART_SIZE,
                      p2.SPEC
                  
                  FROM TB_CA663 b
                  LEFT JOIN TB_CA662 p1
                    ON b.ECO_NO = p1.ECO_NO
                   AND b.PARENT_NO = p1.PART_NO
                  LEFT JOIN TB_CA662 p2
                    ON b.ECO_NO = p2.ECO_NO
                   AND b.CHILD_NO = p2.PART_NO
                  WHERE b.ECO_NO = :ecoNo
                    AND b.PROJECT_NO = :projectNo
                  
                  UNION ALL
                  
                  -- 2. CHILD_NO로 단 한 번도 등장하지 않은 최상위 모품목
                  SELECT
                      b.ECO_NO,
                      b.PROJECT_NO,
                      NULL AS HOGI_NO,
                      NULL AS PARENT_NO,
                      b.PARENT_NO AS CHILD_NO,
                      NULL AS SEQ,
                      NULL AS QTY,
                      NULL AS CMT,
                  
                      NULL AS PARENT_PART_NM,
                      NULL AS PARENT_VERSION,
                  
                      -- 모품 정보를 자품 alias에 맞춰 출력
                      p.PART_NO     AS PART_NO,
                      p.PART_NM     AS PART_NM,
                      p.PLM_VERSION AS PLM_VERSION,
                      p.BLOCK_NO,
                      p.G_NO,
                      p.DRAWING_NO,
                      p.GUBUN,
                      p.UNIT,
                      p.PART_SIZE,
                      p.SPEC
                  
                  FROM TB_CA663 b
                  LEFT JOIN TB_CA662 p
                    ON b.ECO_NO = p.ECO_NO
                   AND b.PARENT_NO = p.PART_NO
                  WHERE b.ECO_NO = :ecoNo
                    AND b.PROJECT_NO = :projectNo
                    AND NOT EXISTS (
                          SELECT 1
                          FROM TB_CA663 sub
                          WHERE sub.ECO_NO = b.ECO_NO
                            AND sub.PROJECT_NO = b.PROJECT_NO
                            AND sub.CHILD_NO = b.PARENT_NO
                      )
                  GROUP BY
                      b.ECO_NO, b.PROJECT_NO, b.PARENT_NO,
                      p.PART_NO, p.PART_NM, p.PLM_VERSION, p.BLOCK_NO, p.G_NO,
                      p.DRAWING_NO, p.GUBUN, p.UNIT, p.PART_SIZE, p.SPEC
                  
        """;

            return sqlRunner.getRows(msSql, params);
        }
    }

    public List<Map<String, Object>> compareBomBuffer(String ecoNo, String projectNo) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ecoNo", ecoNo);
        params.addValue("projectNo", projectNo);

        // 최신 Oracle BOM
        String oracleSql = """
            SELECT b.ECO_NO, b.PROJECT_NO, b.HOGI_NO, b.PARENT_NO, b.CHILD_NO,
                   b.SEQ, b.QTY, b.CMT,
                   b.ERP_YN AS BOM_ERP_YN,
                   p1.PART_NM AS PARENT_PART_NM,
                   p1.PLM_VERSION AS PARENT_VERSION,
                   p2.ERP_YN AS PART_ERP_YN,
                   p2.PART_NO, p2.PART_NM, p2.PLM_VERSION,
                   p2.BLOCK_NO, p2.G_NO, p2.DRAWING_NO, p2.GUBUN,
                   p2.UNIT, p2.PART_SIZE, p2.SPEC
            FROM SJ_DEFAULT.ERP_BOM_BUFFER b
            LEFT JOIN SJ_DEFAULT.ERP_PART_BUFFER p1 ON b.PARENT_NO = p1.PART_NO AND b.ECO_NO = p1.ECO_NO
            LEFT JOIN SJ_DEFAULT.ERP_PART_BUFFER p2 ON b.CHILD_NO = p2.PART_NO AND b.ECO_NO = p2.ECO_NO
            WHERE b.ECO_NO = :ecoNo AND b.PROJECT_NO = :projectNo
        """;
        List<Map<String, Object>> oracleList = oracleSqlRunner.getRows(oracleSql, params);

        // 부모 노드를 직접 수집 (Oracle 기준)
        String oracleParentSql = """
            SELECT DISTINCT b.ECO_NO, b.PROJECT_NO, b.HOGI_NO,
                   b.PARENT_NO AS CHILD_NO, NULL AS PARENT_NO,
                   b.ERP_YN AS BOM_ERP_YN,
                   NULL AS SEQ, NULL AS QTY, NULL AS CMT,
                   p1.ERP_YN AS PART_ERP_YN,
                   p1.PART_NM AS PARENT_PART_NM,
                   p1.PLM_VERSION AS PARENT_VERSION,
                   p1.PART_NO, p1.PART_NM, p1.PLM_VERSION,
                   p1.BLOCK_NO, p1.G_NO, p1.DRAWING_NO, p1.GUBUN,
                   p1.UNIT, p1.PART_SIZE, p1.SPEC
            FROM SJ_DEFAULT.ERP_BOM_BUFFER b
            LEFT JOIN SJ_DEFAULT.ERP_PART_BUFFER p1
                   ON b.PARENT_NO = p1.PART_NO AND b.ECO_NO = p1.ECO_NO
            WHERE b.ECO_NO = :ecoNo AND b.PROJECT_NO = :projectNo
              AND b.PARENT_NO NOT IN (
                SELECT DISTINCT CHILD_NO FROM SJ_DEFAULT.ERP_BOM_BUFFER
                WHERE ECO_NO = :ecoNo AND PROJECT_NO = :projectNo
              )
        """;

        // 위 결과 + 기존 children 모두 합치기
        List<Map<String, Object>> oracleParentList  = oracleSqlRunner.getRows(oracleParentSql, params);

        // 기존 MS BOM
        String msSql = """
            SELECT b.ECO_NO, b.PROJECT_NO, b.HOGI_NO, b.PARENT_NO, b.CHILD_NO,
                   b.SEQ, b.QTY, b.CMT,
                   b.BPDATE AS BOM_BPDATE,
                   b.BPPERNM AS BOM_BPPERNM,
                   p1.PART_NM AS PARENT_PART_NM,
                   p1.PLM_VERSION AS PARENT_VERSION,
                   p2.PART_NO, p2.PART_NM, p2.PLM_VERSION,
                   p2.BLOCK_NO, p2.G_NO, p2.DRAWING_NO, p2.GUBUN,
                   p2.UNIT, p2.PART_SIZE, p2.SPEC,
                   p2.BPDATE AS PART_BPDATE,
                   p2.BPPERNM AS PART_BPPERNM
            FROM TB_CA663 b
            LEFT JOIN TB_CA662 p1 ON b.ECO_NO = p1.ECO_NO AND b.PARENT_NO = p1.PART_NO
            LEFT JOIN TB_CA662 p2 ON b.ECO_NO = p2.ECO_NO AND b.CHILD_NO = p2.PART_NO
            WHERE b.ECO_NO = :ecoNo AND b.PROJECT_NO = :projectNo
        """;
        List<Map<String, Object>> msList = sqlRunner.getRows(msSql, params);

        // MS 부모 노드 직접 수집
        String msParentSql = """
            SELECT DISTINCT b.ECO_NO, b.PROJECT_NO, b.HOGI_NO,
               b.PARENT_NO AS CHILD_NO, NULL AS PARENT_NO,
               b.BPDATE AS BOM_BPDATE,
               b.BPPERNM AS BOM_BPPERNM,
               NULL AS SEQ, NULL AS QTY, NULL AS CMT,
               p1.PART_NM AS PARENT_PART_NM,
               p1.PLM_VERSION AS PARENT_VERSION,
               p1.PART_NO, p1.PART_NM, p1.PLM_VERSION,
               p1.BLOCK_NO, p1.G_NO, p1.DRAWING_NO, p1.GUBUN,
               p1.UNIT, p1.PART_SIZE, p1.SPEC,
               p1.BPDATE AS PART_BPDATE,
               p1.BPPERNM AS PART_BPPERNM
            FROM TB_CA663 b
            LEFT JOIN TB_CA662 p1 ON b.ECO_NO = p1.ECO_NO AND b.PARENT_NO = p1.PART_NO
            WHERE b.ECO_NO = :ecoNo AND b.PROJECT_NO = :projectNo
              AND b.PARENT_NO NOT IN (
                SELECT DISTINCT CHILD_NO FROM TB_CA663
                WHERE ECO_NO = :ecoNo AND PROJECT_NO = :projectNo
              )
        """;
        List<Map<String, Object>> msParentList = sqlRunner.getRows(msParentSql, params);


        return compareBomList(oracleList, oracleParentList, msList, msParentList);
    }


    private List<Map<String, Object>> compareBomList(List<Map<String, Object>> oracleList,
                                                     List<Map<String, Object>> oracleParentList,
                                                     List<Map<String, Object>> msList,
                                                     List<Map<String, Object>> msParentList) {

        List<Map<String, Object>> resultList = new ArrayList<>();

        // 1. 일반 BOM 비교
        Map<String, Map<String, Object>> msMap = msList.stream()
                .collect(Collectors.toMap(
                        m -> m.get("PARENT_NO") + "|" + m.get("CHILD_NO"),
                        m -> m,
                        (m1, m2) -> m1,
                        LinkedHashMap::new
                ));

        for (Map<String, Object> oracleRow : oracleList) {
            Object parentNo = oracleRow.get("PARENT_NO");
            Object childNo = oracleRow.get("CHILD_NO");

            String key = parentNo + "|" + childNo;
            Map<String, Object> msRow = msMap.remove(key);


            if (msRow == null) {
                oracleRow.put("DIFF_TYPE", "NEW");
            } else {
                BigDecimal qtyOracle = getDecimal(oracleRow.get("QTY"));
                BigDecimal qtyMs = getDecimal(msRow.get("QTY"));
                String verOracle = Objects.toString(oracleRow.get("PLM_VERSION"), "");
                String verMs = Objects.toString(msRow.get("PLM_VERSION"), "");
                String seqOracle = Objects.toString(oracleRow.get("SEQ"), "");
                String seqMs = Objects.toString(msRow.get("SEQ"), "");

                boolean isBomChanged = qtyOracle.compareTo(qtyMs) != 0 || !seqOracle.equals(seqMs);
                boolean isPartChanged =
                        !Objects.toString(oracleRow.get("PART_NM"), "").equals(Objects.toString(msRow.get("PART_NM"), "")) ||
                                !Objects.toString(oracleRow.get("BLOCK_NO"), "").equals(Objects.toString(msRow.get("BLOCK_NO"), "")) ||
                                !Objects.toString(oracleRow.get("G_NO"), "").equals(Objects.toString(msRow.get("G_NO"), "")) ||
                                !Objects.toString(oracleRow.get("DRAWING_NO"), "").equals(Objects.toString(msRow.get("DRAWING_NO"), "")) ||
                                !Objects.toString(oracleRow.get("GUBUN"), "").equals(Objects.toString(msRow.get("GUBUN"), "")) ||
                                !Objects.toString(oracleRow.get("UNIT"), "").equals(Objects.toString(msRow.get("UNIT"), "")) ||
                                !Objects.toString(oracleRow.get("PART_SIZE"), "").equals(Objects.toString(msRow.get("PART_SIZE"), "")) ||
                                !Objects.toString(oracleRow.get("SPEC"), "").equals(Objects.toString(msRow.get("SPEC"), "")) ||
                                !verOracle.equals(verMs);

                if (isBomChanged && isPartChanged) {
                    oracleRow.put("DIFF_TYPE", "MODIFIED_BOTH");
                } else if (isBomChanged) {
                    oracleRow.put("DIFF_TYPE", "MODIFIED_BOM");
                } else if (isPartChanged) {
                    oracleRow.put("DIFF_TYPE", "MODIFIED_PART");
                } else {
                    oracleRow.put("DIFF_TYPE", "UNCHANGED");
                }

                oracleRow.put("MS_QTY", qtyMs);
                oracleRow.put("MS_VERSION", verMs);
                oracleRow.put("MS_SEQ", seqMs);

                // 조건 1: ERP_YN 판단 (Oracle 기준)
                String bomErpYn = Objects.toString(oracleRow.get("BOM_ERP_YN"), "");
                String partErpYn = Objects.toString(oracleRow.get("PART_ERP_YN"), "");

                if ("N".equalsIgnoreCase(bomErpYn) || "N".equalsIgnoreCase(partErpYn)
                        || bomErpYn.isEmpty() || partErpYn.isEmpty()) {
                    oracleRow.put("ERP_YN", "N");
                } else {
                    oracleRow.put("ERP_YN", "Y");
                }

                // 조건 2: 최신 BPDATE
                oracleRow.put("BPDATE", Stream.of(msRow.get("BOM_BPDATE"), msRow.get("PART_BPDATE"))
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .max(String::compareTo) // 문자열 날짜 비교 (yyyyMMdd 형식일 경우 OK)
                        .orElse(null));

                // 조건 3: BPPERNM 중복 제거 후 합치기
                Set<String> bppernmSet = new LinkedHashSet<>();
                if (msRow.get("BOM_BPPERNM") != null) bppernmSet.addAll(Arrays.asList(msRow.get("BOM_BPPERNM").toString().split(",")));
                if (msRow.get("PART_BPPERNM") != null) bppernmSet.addAll(Arrays.asList(msRow.get("PART_BPPERNM").toString().split(",")));
                String joinedBppernm = String.join(", ", bppernmSet.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
                oracleRow.put("BPPERNM", joinedBppernm);

            }

            resultList.add(oracleRow);
        }

        // 2. MS에만 있는 항목
        for (Map<String, Object> msOnlyRow : msMap.values()) {
            msOnlyRow.put("DIFF_TYPE", "DELETED");
            // ERP_YN은 무조건 'Y' 처리 (MS에는 오라클 기준이 없으므로)
            msOnlyRow.put("ERP_YN", "Y");

            // BPDATE 계산
            String maxDate = Stream.of(msOnlyRow.get("BOM_BPDATE"), msOnlyRow.get("PART_BPDATE"))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .max(String::compareTo)
                    .orElse(null);
            msOnlyRow.put("BPDATE", maxDate);

            // BPPERNM 병합
            Set<String> bppernmSet = new LinkedHashSet<>();
            if (msOnlyRow.get("BOM_BPPERNM") != null)
                bppernmSet.addAll(Arrays.asList(msOnlyRow.get("BOM_BPPERNM").toString().split(",")));
            if (msOnlyRow.get("PART_BPPERNM") != null)
                bppernmSet.addAll(Arrays.asList(msOnlyRow.get("PART_BPPERNM").toString().split(",")));
            String joinedBppernm = String.join(", ",
                    bppernmSet.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            msOnlyRow.put("BPPERNM", joinedBppernm);

            resultList.add(msOnlyRow);

        }

        // 3. 루트 노드 비교
        Map<String, Map<String, Object>> msParentMap = msParentList.stream()
                .collect(Collectors.toMap(
                        m -> Objects.toString(m.get("CHILD_NO")),
                        m -> m,
                        (m1, m2) -> m1,
                        LinkedHashMap::new
                ));

        for (Map<String, Object> oracleParent : oracleParentList) {
            String childNo = Objects.toString(oracleParent.get("CHILD_NO"));
            Map<String, Object> msParent = msParentMap.remove(childNo);

            if (msParent == null) {
                oracleParent.put("DIFF_TYPE", "NEW_PARENT");
            } else {
                oracleParent.put("DIFF_TYPE", "UNCHANGED_PARENT");
            }

            oracleParent.put("items", new ArrayList<>());
            resultList.add(oracleParent);
        }

        for (Map<String, Object> msParentOnly : msParentMap.values()) {
            msParentOnly.put("DIFF_TYPE", "DELETED_PARENT");

            // ERP_YN은 무조건 'Y' (Oracle에는 없으므로)
            msParentOnly.put("ERP_YN", "Y");

            // BPDATE 계산
            String maxDate = Stream.of(msParentOnly.get("BOM_BPDATE"), msParentOnly.get("PART_BPDATE"))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .max(String::compareTo)
                    .orElse(null);
            msParentOnly.put("BPDATE", maxDate);

            // BPPERNM 병합
            Set<String> bppernmSet = new LinkedHashSet<>();
            if (msParentOnly.get("BOM_BPPERNM") != null)
                bppernmSet.addAll(Arrays.asList(msParentOnly.get("BOM_BPPERNM").toString().split(",")));
            if (msParentOnly.get("PART_BPPERNM") != null)
                bppernmSet.addAll(Arrays.asList(msParentOnly.get("PART_BPPERNM").toString().split(",")));
            String joinedBppernm = String.join(", ",
                    bppernmSet.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            msParentOnly.put("BPPERNM", joinedBppernm);

            msParentOnly.put("items", new ArrayList<>());
            resultList.add(msParentOnly);
        }

        return resultList;
    }

    private BigDecimal getDecimal(Object val) {
        try {
            return val == null ? BigDecimal.ZERO : new BigDecimal(val.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // Oracle -> MS SQL 경우에 맞게 형식을 재구성
    @Transactional
    public void syncToMsSql(List<Map<String, Object>> itemList, String username) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        for (Map<String, Object> project : itemList) {
            String ecoNo = project.get("eco_no").toString();
            String projectNo = project.get("project_no").toString();
            String projectNm = project.get("project_nm").toString();

            // 1. 프로젝트 저장 (TB_CA664)
            saveProjectToMs(ecoNo, projectNo, projectNm, today, username);

            MapSqlParameterSource param = new MapSqlParameterSource()
                    .addValue("ecoNo", ecoNo)
                    .addValue("projectNo", projectNo);

            // 2. 오라클 BOM 리스트 조회
            List<Map<String, Object>> oracleBoms = oracleSqlRunner.getRows(
                    "SELECT * FROM erp_bom_buffer WHERE eco_no = :ecoNo AND project_no = :projectNo", param);

            // 3. BOM 저장 (TB_CA663)
            for (Map<String, Object> bom : oracleBoms) {
                saveBomToMs(bom, today, username);
            }

            // 4. PART 조회용 part_no 리스트 구성 (parent + child)
            Set<String> partNos = new HashSet<>();
            for (Map<String, Object> bom : oracleBoms) {
                partNos.add(Objects.toString(bom.get("PARENT_NO"), ""));
                partNos.add(Objects.toString(bom.get("CHILD_NO"), ""));
            }

            // 5. 오라클 PART 리스트 조회
            List<Map<String, Object>> oracleParts = new ArrayList<>();
            for (String partNo : partNos) {
                MapSqlParameterSource partParam = new MapSqlParameterSource()
                        .addValue("ecoNo", ecoNo)
                        .addValue("partNo", partNo);
                List<Map<String, Object>> parts = oracleSqlRunner.getRows(
                        "SELECT * FROM erp_part_buffer WHERE eco_no = :ecoNo AND part_no = :partNo", partParam);
                oracleParts.addAll(parts);
            }

            // 6. PART 저장 (TB_CA662)
            for (Map<String, Object> part : oracleParts) {
                savePartToMs(part, today, username);
            }

            // 7. 오라클 BOM, PART, PROJECT에 erp_yn = 'Y' 처리
            MapSqlParameterSource updateParam = new MapSqlParameterSource()
                    .addValue("ecoNo", ecoNo)
                    .addValue("projectNo", projectNo);

            oracleSqlRunner.execute(
                    "UPDATE erp_bom_buffer SET erp_yn = 'Y' WHERE eco_no = :ecoNo AND project_no = :projectNo",
                    updateParam
            );
            oracleSqlRunner.execute(
                    "UPDATE erp_part_buffer SET erp_yn = 'Y' WHERE eco_no = :ecoNo AND part_no IN (:partNos)",
                    new MapSqlParameterSource()
                            .addValue("ecoNo", ecoNo)
                            .addValue("partNos", partNos)
            );
            oracleSqlRunner.execute(
                    "UPDATE erp_project_buffer SET erp_yn = 'Y' WHERE eco_no = :ecoNo AND project_no = :projectNo",
                    updateParam
            );

        }
    }

    private void saveProjectToMs(String ecoNo, String projectNo, String projectNm, String today, String username) {
        String sql = """
        MERGE INTO tb_ca664 AS target
        USING (SELECT :ecoNo AS eco_no, :projectNo AS project_no) AS source
        ON (target.eco_no = source.eco_no AND target.project_no = source.project_no)
        WHEN MATCHED THEN
            UPDATE SET project_nm = :projectNm, erp_yn = 'Y', bpdate = :bpdate, bppernm = :bppernm
        WHEN NOT MATCHED THEN
            INSERT (eco_no, project_no, project_nm, erp_yn, bpdate, bppernm)
            VALUES (:ecoNo, :projectNo, :projectNm, 'Y', :bpdate, :bppernm);
    """;

        sqlRunner.execute(sql, new MapSqlParameterSource()
                .addValue("ecoNo", ecoNo)
                .addValue("projectNo", projectNo)
                .addValue("projectNm", projectNm)
                .addValue("bpdate", today)
                .addValue("bppernm", username));
    }

    private void saveBomToMs(Map<String, Object> item, String today, String username) {
        String sql = """
        MERGE INTO tb_ca663 AS target
        USING (SELECT :ecoNo AS eco_no, :parentNo AS parent_no, :childNo AS child_no) AS source
        ON (target.eco_no = source.eco_no AND target.parent_no = source.parent_no AND target.child_no = source.child_no)
        WHEN MATCHED THEN
            UPDATE SET qty = :qty, seq = :seq, plm_version = :plmVersion, erp_yn = 'Y', bpdate = :bpdate, bppernm = :bppernm
        WHEN NOT MATCHED THEN
            INSERT (eco_no, parent_no, child_no, qty, seq, plm_version, erp_yn, bpdate, bppernm)
            VALUES (:ecoNo, :parentNo, :childNo, :qty, :seq, :plmVersion, 'Y', :bpdate, :bppernm);
    """;

        sqlRunner.execute(sql, new MapSqlParameterSource()
                .addValue("ecoNo", item.get("ECO_NO"))
                .addValue("parentNo", item.get("PARENT_NO"))
                .addValue("childNo", item.get("CHILD_NO"))
                .addValue("qty", item.get("QTY"))
                .addValue("seq", item.get("SEQ"))
                .addValue("plmVersion", item.get("PLM_VERSION"))
                .addValue("bpdate", today)
                .addValue("bppernm", username));
    }

    private void savePartToMs(Map<String, Object> item, String today, String username) {
        String sql = """
        MERGE INTO tb_ca662 AS target
        USING (SELECT :ecoNo AS eco_no, :partNo AS part_no) AS source
        ON (target.eco_no = source.eco_no AND target.part_no = source.part_no)
        WHEN MATCHED THEN
            UPDATE SET part_nm = :partNm, spec = :spec, unit = :unit, erp_yn = 'Y', bpdate = :bpdate, bppernm = :bppernm
        WHEN NOT MATCHED THEN
            INSERT (eco_no, part_no, part_nm, spec, unit, erp_yn, bpdate, bppernm)
            VALUES (:ecoNo, :partNo, :partNm, :spec, :unit, 'Y', :bpdate, :bppernm);
    """;

        sqlRunner.execute(sql, new MapSqlParameterSource()
                .addValue("ecoNo", item.get("ECO_NO"))
                .addValue("partNo", item.get("PART_NO"))
                .addValue("partNm", item.get("PART_NM"))
                .addValue("spec", item.get("SPEC"))
                .addValue("unit", item.get("UNIT"))
                .addValue("bpdate", today)
                .addValue("bppernm", username));
    }
}
