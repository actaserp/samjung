package mes.app.request.subRequest.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.actasEntity.TB_DA006W;
import mes.domain.entity.actasEntity.TB_DA006WFile;
import mes.domain.entity.actasEntity.TB_DA006W_PK;
import mes.domain.entity.actasEntity.TB_DA007W;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SubRequestService {

    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    TB_DA006WRepository tbDa006WRepository;

    @Autowired
    TB_DA007WRepository tbDa007WRepository;

    @Autowired
    TB_DA006WFILERepository tbDa006WFILERepository;

    // 헤드정보 저장
    @Transactional
    public Boolean save(TB_DA006W tbDa006W){

        try{
            tbDa006WRepository.save(tbDa006W);
            return true;
        }catch (Exception e){
            System.out.println(e + ": 에러발생");
            return false;
        }
    }
    // 세부항목 저장
    @Transactional
    public Boolean saveBody(TB_DA007W tbDa007W){

        try{
            tbDa007WRepository.save(tbDa007W);

            return true;

        }catch (Exception e){
            System.out.println(e + ": 에러발생");
            return false;
        }
    }
    //세부항목 불러오기
    public List<Map<String, Object>> getInspecList(TB_DA006W_PK tbDa006W_pk) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                select
                *
                from TB_DA007W
                WHERE reqnum = :reqnum
                AND reqdate = :reqdate
                AND   custcd = :custcd
                AND   spjangcd = :spjangcd
                order by indate desc
                """;
        dicParam.addValue("reqdate", tbDa006W_pk.getReqdate());
        dicParam.addValue("reqnum", tbDa006W_pk.getReqnum());
        dicParam.addValue("custcd", tbDa006W_pk.getCustcd());
        dicParam.addValue("spjangcd", tbDa006W_pk.getSpjangcd());
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }
    //주문출고 불러오기
    public List<Map<String, Object>> getOrderList(
                                                  String spjangcd,
                                                  String searchStartDate,
                                                  String searchEndDate,
                                                  String searchRemark,
                                                  String searchChulflag,
                                                  String saupnum,
                                                  String cltcd,
                                                  String searchActnm) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String facFlag = searchChulflag.equals("shipment") ? "1" : "0" ;
        dicParam.addValue("searchStartDate", searchStartDate);
        dicParam.addValue("searchEndDate", searchEndDate);
        dicParam.addValue("searchRemark", "%" + searchRemark + "%");
        dicParam.addValue("facFlag",  facFlag);
        dicParam.addValue("CLTCD",  cltcd);
        dicParam.addValue("searchActnm",  "%" + searchActnm + "%");

        StringBuilder sql = new StringBuilder("""
                SELECT
                    hd.*,
                    dt.*
                FROM
                    TB_CA660 hd
                JOIN
                    TB_CA661 dt
                    ON hd.BALJUNUM = dt.BALJUNUM
                WHERE
                    1=1
                    AND dt.CHULFLAG = '1'
                """);

        // 날짜 필터
        if (searchStartDate != null && !searchStartDate.isEmpty()) {
            sql.append(" AND hd.BALJUDATE >= :searchStartDate");
        }
        //
        if (searchEndDate != null && !searchEndDate.isEmpty()) {
            sql.append(" AND hd.BALJUDATE <= :searchEndDate");
        }
        // 현장명 필터
        if (searchActnm != null && !searchActnm.isEmpty()) {
            sql.append(" AND hd.ACTNM LIKE :searchActnm");
        }else{
            return null;
        }
        // 진행구분 필터
        if (searchChulflag != null && !searchChulflag.isEmpty()) {
            sql.append(" AND dt.FACFLAG = :facFlag");
        }
        // 정렬 조건 추가
        sql.append(" ORDER BY hd.BALJUDATE ASC");

        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }
    // 주문출고 flag값 update
    public void updateChulInfo(String username, String today, String CHULFLAG, Integer BALJUNUM, Integer BALJUSEQ) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("username", username);
        dicParam.addValue("today", today);
        dicParam.addValue("CHULFLAG", CHULFLAG);
        dicParam.addValue("BALJUNUM", BALJUNUM);
        dicParam.addValue("BALJUSEQ", BALJUSEQ);
        String sql = """
                update TB_CA661
                set "CHULFLAG" = :CHULFLAG
                , "CHULDATE" = :today
                , "CHULPERNM" = :username
                where BALJUNUM = :BALJUNUM
                AND BALJUSEQ = :BALJUSEQ
                """;

        int result = this.sqlRunner.execute(sql, dicParam);
    }
    // 주문출고 해제 메서드
    public void clearChulInfo(String chulFlag, Integer baljuNum, Integer baljuSeq) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("CHULFLAG", chulFlag);
        dicParam.addValue("BALJUNUM", baljuNum);
        dicParam.addValue("BALJUSEQ", baljuSeq);
        String sql = """
        update TB_CA661
        set "CHULFLAG" = :CHULFLAG
          , "CHULDATE" = ''
          , "CHULPERNM" = ''
        where BALJUNUM = :BALJUNUM
          AND BALJUSEQ = :BALJUSEQ
    """;
        // 파라미터 세팅 후 update() 호출
        int result = this.sqlRunner.execute(sql, dicParam);
    }
    public Map<String, Object> getChulFlag(Integer baljuNum, Integer baljuSeq) {
        // queryForObject나 queryForString 사용
        String sql = "SELECT CHULFLAG FROM TB_CA661 WHERE BALJUNUM = :BALJUNUM AND BALJUSEQ = :BALJUSEQ";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("BALJUNUM", baljuNum)
                .addValue("BALJUSEQ", baljuSeq);
        return sqlRunner.getRow(sql, params);
    }
    // 주문의뢰현황 head정보 불러오기
    public List<Map<String, Object>> getHeadList(TB_DA006W_PK tbDa006W_pk) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        String sql = """
                select 
                *
                from TB_DA007W
                order by indate desc
                """;
        dicParam.addValue("custcd", tbDa006W_pk.getCustcd());
        dicParam.addValue("spjangcd", tbDa006W_pk.getSpjangcd());
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }
    // 세부항목 업데이트
    public boolean updateBody(TB_DA007W tbDa007W) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        try {
            String sql = """
                    UPDATE TB_DA007W
                    SET
                        hgrb = :hgrb,
                        panel_t = :panel_t,
                        panel_w = :panel_w,
                        panel_l = :panel_l,
                        panel_h = :panel_h,
                        qty = :qty,
                        exfmtypedv = :exfmtypedv,
                        infmtypedv = :infmtypedv,
                        stframedv = :stframedv,
                        stexplydv = :stexplydv,
                        indate = :indate,
                        inperid = :inperid,
                        ordtext = :ordtext
                    WHERE
                        custcd = :custcd
                        AND spjangcd = :spjangcd
                        AND reqnum = :reqnum
                        AND reqseq = :reqseq
                    """;
            dicParam.addValue("hgrb", tbDa007W.getHgrb());
            dicParam.addValue("panel_t", tbDa007W.getPanel_t());
            dicParam.addValue("panel_w", tbDa007W.getPanel_w());
            dicParam.addValue("panel_l", tbDa007W.getPanel_l());
            dicParam.addValue("panel_h", tbDa007W.getPanel_h());
            dicParam.addValue("qty", tbDa007W.getQty());
            dicParam.addValue("exfmtypedv", tbDa007W.getExfmtypedv());
            dicParam.addValue("infmtypedv", tbDa007W.getInfmtypedv());
            dicParam.addValue("stframedv", tbDa007W.getStframedv());
            dicParam.addValue("stexplydv", tbDa007W.getStexplydv());
            dicParam.addValue("indate", tbDa007W.getIndate());
            dicParam.addValue("inperid", tbDa007W.getInperid());
            dicParam.addValue("ordtext", tbDa007W.getOrdtext());

            dicParam.addValue("custcd", tbDa007W.getPk().getCustcd());
            dicParam.addValue("spjangcd", tbDa007W.getPk().getSpjangcd());
            dicParam.addValue("reqnum", tbDa007W.getPk().getReqnum());
            dicParam.addValue("reqseq", tbDa007W.getPk().getReqseq());


            this.sqlRunner.execute(sql, dicParam);
        }catch(Exception e){
            e.getMessage();
            e.printStackTrace();
            return false;
        }
        return true;
    }
    // file download
    public List<Map<String, Object>> download(Map<String, Object> reqnum) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        StringBuilder sql = new StringBuilder();
        dicParam.addValue("reqnum", reqnum.get("reqnum"));

        sql.append("""
                select
                        filepath,
                        reqdate,
                        filesvnm,
                        fileornm
                from tb_DA006WFILE
                where
                    reqnum = :reqnum
                """);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }
    // 제품구성 리스트 불러오는 함수
    public List<Map<String, Object>> getListHgrb() {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                select 
                TB_CA503_07.hgrb,
                TB_CA503_07.hgrbnm,
                TB_CA503_07.sortno
                from TB_CA503_07 WITH(NOLOCK)
                WHERE TB_CA503_07.custcd = 'SWSPANEL'
                 AND TB_CA503_07.grb = '1'
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }
    // 보강재, 마감재 리스트 불러오는 함수
    public List<Map<String, Object>> getListACgrb() {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                SELECT agrb + bgrb + cgrb as cgrb, 
                 cgrbnm
                 FROM TB_CA503_C
                WHERE agrb = 'M'
                AND bgrb = 'A'
                
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }
    public List<Map<String, Object>> getListCCgrb() {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                SELECT agrb + bgrb + cgrb as cgrb,
                 cgrbnm
                 FROM TB_CA503_C
                WHERE agrb = 'M'
                AND bgrb = 'C'
                
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
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
    // username으로 주문의뢰 필요 데이터 가져오기
    public Map<String, Object> getMyInfo(String username) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                select x.prenm,
                       x.hptelnum,
                       x.zipcd,
                       x.cltadres,
                       x.cltnm
                FROM TB_XCLIENT x
                WHERE saupnum = :username
                """;
        dicParam.addValue("username", username);
        Map<String, Object> userInfo = this.sqlRunner.getRow(sql, dicParam);
        return userInfo;
    }
    // savefile
    public boolean saveFile(TB_DA006WFile tbDa006WFile) {

        try {
            tbDa006WFILERepository.save(tbDa006WFile);
            return true;

        } catch (Exception e) {
            System.out.println(e + ": 에러발생");
            return false;
        }
    }
    // delete
    @Transactional
    public Boolean delete(String reqnum) {
        // TB_DA006W 삭제
        headDelete(reqnum);

        // 007 정보 삭제
        bodyDelete(reqnum);

        // tb_DA006WFILE 찾기
        List<TB_DA006WFile> tbDa006WFiles = tbDa006WFILERepository.findAllByReqnum(reqnum);
        // 파일 삭제
        for (TB_DA006WFile tbDa006WFile : tbDa006WFiles) {
            String filePath = tbDa006WFile.getFilepath();
            String fileName = tbDa006WFile.getFilesvnm();
            File file = new File(filePath, fileName);
            if (file.exists()) {
                file.delete();
            }
        }
        // 006WFile DB정보 삭제
        fileDelete(reqnum);
        return true;
    }
    // TB_DA006W 삭제
    public void headDelete(String reqnum){
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        try {
            String sql = """
                DELETE FROM TB_DA006W
                WHERE reqnum = :reqnum
            """;
            dicParam.addValue("reqnum", reqnum);
            this.sqlRunner.execute(sql, dicParam); // update 메서드로 변경
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TB_DA007W 삭제
    public void bodyDelete(String reqnum){
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        try {
            String sql = """
                DELETE FROM TB_DA007W
                WHERE reqnum = :reqnum
            """;
            dicParam.addValue("reqnum", reqnum);
            this.sqlRunner.execute(sql, dicParam); // update 메서드로 변경
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TB_DA006Wfile 삭제
    public void fileDelete(String reqnum){
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        try {
            String sql = """
                DELETE FROM tb_DA006WFILE
                WHERE reqnum = :reqnum
            """;
            dicParam.addValue("reqnum", reqnum);
            this.sqlRunner.execute(sql, dicParam); // update 메서드로 변경
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public boolean deleteBody(String reqseq, String reqnum) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        try {
            String sql = """
                DELETE FROM TB_DA007W
                WHERE reqseq = :reqseq
                AND reqnum = :reqnum
            """;
            dicParam.addValue("reqseq", reqseq);
            dicParam.addValue("reqnum", reqnum);
            this.sqlRunner.execute(sql, dicParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
    //세부항목 하나 불러오기
    public Map<String, Object> getInspecList2(TB_DA006W_PK tbDa006W_pk, String reqseq, String reqdate) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                select
                *
                from TB_DA007W
                WHERE reqnum = :reqnum
                AND   custcd = :custcd
                AND   spjangcd = :spjangcd
                AND   reqseq = :reqseq
                AND   reqdate = :reqdate
                order by indate desc
                """;
        dicParam.addValue("reqnum", tbDa006W_pk.getReqnum());
        dicParam.addValue("custcd", tbDa006W_pk.getCustcd());
        dicParam.addValue("spjangcd", tbDa006W_pk.getSpjangcd());
        dicParam.addValue("reqseq", reqseq);
        dicParam.addValue("reqdate", reqdate);
        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
        return item;
    }
    public List<Map<String, Object>> getHoliday(){
        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
                select
                *
                from TB_PZ010
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    // pdf 파일 미리보기 데이터
    public List<Map<String, Object>> getVacFileList(String searchDate,
                                                    String searchActnm,
                                                    String cltcd,
                                                    String spjangcd
    ) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("searchDate", searchDate);
        dicParam.addValue("searchActnm",  "%" + searchActnm + "%");

        StringBuilder sql = new StringBuilder("""
                SELECT
                    hd.*,
                    dt.*,
                    au.last_name,
                    xc.cltnm,
                    au.phone
                
                FROM
                    TB_CA660 hd
                JOIN
                    TB_CA661 dt
                    ON hd.BALJUNUM = dt.BALJUNUM
                JOIN
                    auth_user au
                    ON au.username = dt.CHULPERNM
                JOIN
                    TB_XCLIENT xc
                    ON au.username = xc.saupnum
                WHERE
                    1=1
                AND dt.FACFLAG = '1'
                AND dt.FACFLAG = :searchDate
                """);
        // 정렬 조건 추가
        sql.append(" ORDER BY hd.BALJUDATE ASC");

        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }
    // pdf 파일 미리보기 데이터(거래명세서)
    public List<Map<String, Object>> getVacFileList2(String searchDate,
                                                     String searchActnm,
                                                     String cltcd,
                                                     String spjangcd
    ) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("searchDate", searchDate);
        dicParam.addValue("searchActnm",  "%" + searchActnm + "%");

        StringBuilder sql = new StringBuilder("""
                SELECT
                    hd.*,
                    dt.*,
                    au.last_name,
                    xc.cltnm,
                    xc.saupnum,
                    xc.prenm,
                    xc.telnum,
                    xc.cltadres,
                    xc.biztypenm,
                    xc.bizitemnm,
                    xc.agnernm,
                    xc.agntel,
                    au.phone,
                    aus.last_name as sub_last_name,
                    xcs.cltnm as sub_cltnm,
                    aus.phone as sub_phone
                FROM
                    TB_CA660 hd
                JOIN
                    TB_CA661 dt
                    ON hd.BALJUNUM = dt.BALJUNUM
                JOIN
                    auth_user au
                    ON au.username = dt.CHULPERNM
                JOIN
                    TB_XCLIENT xc
                    ON au.username = xc.saupnum
                JOIN
                    auth_user aus
                    ON aus.username = dt.FACPERNM
                JOIN
                    TB_XCLIENT xcs
                    ON aus.username = xcs.saupnum
                WHERE
                    1=1
                AND dt.FACFLAG = '1'
                AND dt.FACDATE = :searchDate
                """);
        // cltcd 필터(자기 발주처건만 확인할 수 있도록)
//        if (cltcd != null && !cltcd.isEmpty()) {
//            sql.append(" AND hd.CLTCD = :CLTCD");
//        }
        // 정렬 조건 추가
        sql.append(" ORDER BY hd.BALJUDATE ASC");

        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }
    //주문출고(공장) 불러오기
    public List<Map<String, Object>> getOrderList2(
            String spjangcd,
            String searchStartDate,
            String searchEndDate,
            String searchRemark,
            String searchChulflag,
            String saupnum,
            String cltcd,
            String searchActnm) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("searchStartDate", searchStartDate);
        dicParam.addValue("searchEndDate", searchEndDate);
        dicParam.addValue("searchRemark", "%" + searchRemark + "%");
        dicParam.addValue("searchOrdflag",  searchChulflag);
        dicParam.addValue("CLTCD",  cltcd);
        dicParam.addValue("searchActnm",  "%" + searchActnm + "%");

        StringBuilder sql = new StringBuilder("""
                SELECT
                    hd.*,
                    dt.*
                FROM
                    TB_CA660 hd
                JOIN
                    TB_CA661 dt
                    ON hd.BALJUNUM = dt.BALJUNUM
                WHERE
                    1=1
                    AND dt.CHULFLAG = '1'
                """);

        // 날짜 필터
        if (searchStartDate != null && !searchStartDate.isEmpty()) {
            sql.append(" AND hd.BALJUDATE >= :searchStartDate");
        }
        //
        if (searchEndDate != null && !searchEndDate.isEmpty()) {
            sql.append(" AND hd.BALJUDATE <= :searchEndDate");
        }
        // 현장명 필터
        if (searchActnm != null && !searchActnm.isEmpty()) {
            sql.append(" AND hd.ACTNM LIKE :searchActnm");
        }else{
            return null;
        }
        // 진행구분 필터
        if (searchChulflag != null && !searchChulflag.isEmpty()) {
            sql.append(" AND hd.ordflag = :searchOrdflag");
        }
        // 정렬 조건 추가
        sql.append(" ORDER BY hd.BALJUDATE ASC");

        dicParam.addValue("spjangcd", spjangcd);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql.toString(), dicParam);
        return items;
    }
    // 주문출고(공장) flag값 update
    public void updateChulInfo2(String username, String today, String CHULFLAG, Integer BALJUNUM, Integer BALJUSEQ) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("username", username);
        dicParam.addValue("today", today);
        dicParam.addValue("CHULFLAG", CHULFLAG);
        dicParam.addValue("BALJUNUM", BALJUNUM);
        dicParam.addValue("BALJUSEQ", BALJUSEQ);
        String sql = """
                update TB_CA661
                set "FACFLAG" = :CHULFLAG
                , "FACDATE" = :today
                , "FACPERNM" = :username
                where BALJUNUM = :BALJUNUM
                AND BALJUSEQ = :BALJUSEQ
                """;

        int result = this.sqlRunner.execute(sql, dicParam);
    }
    // 주문출고(공장) 해제 메서드
    public void clearChulInfo2(String chulFlag, Integer baljuNum, Integer baljuSeq) {
        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("CHULFLAG", chulFlag);
        dicParam.addValue("BALJUNUM", baljuNum);
        dicParam.addValue("BALJUSEQ", baljuSeq);
        String sql = """
        update TB_CA661
        set "FACFLAG" = :CHULFLAG
          , "FACDATE" = ''
          , "FACPERNM" = ''
        where BALJUNUM = :BALJUNUM
          AND BALJUSEQ = :BALJUSEQ
    """;
        // 파라미터 세팅 후 update() 호출
        int result = this.sqlRunner.execute(sql, dicParam);
    }
    public Map<String, Object> getFacFlag(Integer baljuNum, Integer baljuSeq) {
        // queryForObject나 queryForString 사용
        String sql = "SELECT FACFLAG FROM TB_CA661 WHERE BALJUNUM = :BALJUNUM AND BALJUSEQ = :BALJUSEQ";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("BALJUNUM", baljuNum)
                .addValue("BALJUSEQ", baljuSeq);
        return sqlRunner.getRow(sql, params);
    }

}
