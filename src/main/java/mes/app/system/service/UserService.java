package mes.app.system.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import mes.domain.entity.User;
import mes.domain.entity.UserGroup;
import mes.domain.entity.actasEntity.TB_XCLIENT;
import mes.domain.repository.UserGroupRepository;
import mes.domain.repository.UserRepository;
import mes.domain.repository.actasRepository.TB_XClientRepository;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {

    @Autowired
    SqlRunner sqlRunner;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    TB_XClientRepository tbXClientRepository;

    // 사용자 관리 리스트
    public List<Map<String, Object>> getUserList(String id ,boolean superUser, String spjangcd, String userGroup, String name, String username) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("superUser", superUser);
        params.addValue("spjangcd", spjangcd);
        params.addValue("userGroup", userGroup);
        params.addValue("name", name);
        params.addValue("username", username);
        String sql = """
            select
                au.username as userid,
                u.perid,
                u.custcd AS cltnm,
                u.passwd1 ,
                au.email , 
                au.tel,
                jc.divinm,
                p.RSPNM,
                ug.Name AS group_name,
                j.handphone as Phone,
                j.zipcd,
                j.rzipadres ,
                au.id,
                ug.id AS group_id,
                FORMAT(au.date_joined, 'yyyy-MM-dd') AS date_joined,
                au.is_active,
                au.last_name as prenm,
                au.spjangcd,
                  xa.spjangnm AS spjType
              FROM
                  auth_user au
              LEFT JOIN
                  user_profile up ON up.User_id = au.id
              LEFT JOIN
                  user_group ug ON ug.id = up.UserGroup_id
              left join tb_xusers u on u.userid =au.username and au.last_name =u.pernm
              LEFT JOIN tb_ja001 j  ON j.perid = CONCAT('p', u.perid)
              LEFT JOIN tb_jc002 jc ON j.divicd = jc.divicd
              LEFT JOIN tb_pz001 p  ON j.rspcd = p.RSPCD
              left join tb_xa012 xa on xa.spjangcd = au.spjangcd
              where  1 = 1  AND au.spjangcd = :spjangcd
            """;

        if (userGroup != null && !userGroup.isEmpty()) {
            sql += " AND up.UserGroup_id = :userGroup ";
            params.addValue("userGroup",  userGroup );
        }

        if (username != null && !username.isEmpty()) {
            sql += " AND au.username LIKE :username ";
            params.addValue("username", "%" + username + "%");
        }

        if (name != null && !name.isEmpty()) {
            sql += " AND au.last_name LIKE :name ";
            params.addValue("name", "%" + name + "%");
        }
        if (userGroup != null && !userGroup.isEmpty()) {
            sql += " AND up.UserGroup_id = :userGroup ";
            params.addValue("userGroup",  userGroup );
        }
        if (id != null && !id.isEmpty()) {
            sql += " AND  au.id = :id ";
            params.addValue("id",  id );
        }
        sql+= """
            ORDER by au.date_joined DESC;
            """;
//    log.info("사용자 관리 read 데이터 SQL: {}", sql);
//    log.info("SQL Parameters: {}", params.getValues());
        return sqlRunner.getRows(sql, params);
    }

    // 업체관리 리스트
    public List<Map<String, Object>> getCompanyList(boolean superUser, String spjangcd, String userGroup, String name, String username) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("superUser", superUser);
        params.addValue("spjangcd", spjangcd);
        params.addValue("userGroup", userGroup);
        params.addValue("name", name);
        params.addValue("username", username);
        String sql = """
            SELECT
                au.id,
                au.last_name,
                txc.cltnm,
                au.username AS userid,
                ug.id AS group_id,
                au.email,
                au.tel,
                au.agencycd,
                ug.Name AS group_name,
                au.last_login,
                up.lang_code,
                au.is_active,
                au.phone,
                txc.biztypenm,
                txc.bizitemnm,
                txc.prenm,
                txc.relyn,
                FORMAT(au.date_joined, 'yyyy-MM-dd') AS date_joined,
                au.spjangcd ,
                xa.spjangnm AS spjType
                FROM
                    TB_XCLIENT txc
                left join auth_user au on au.username = txc.saupnum 
                LEFT JOIN user_profile up ON up.User_id = au.id
                LEFT JOIN user_group ug ON ug.id = up.UserGroup_id
                left join tb_xa012 xa on xa.spjangcd = au.spjangcd
            WHERE
                1 = 1  AND au.spjangcd = :spjangcd and  txc.relyn ='X'
            """;

        if (userGroup != null && !userGroup.isEmpty()) {
            sql += " AND up.UserGroup_id = :userGroup ";
            params.addValue("userGroup",  userGroup );
        }

        if (username != null && !username.isEmpty()) {
            sql += " AND au.username LIKE :username ";
            params.addValue("username", "%" + username + "%");
        }

        sql+= """
            ORDER by au.date_joined DESC;
            """;
//    log.info("업체관리 리스트 SQL: {}", sql);
//    log.info("SQL Parameters: {}", params.getValues());
        return sqlRunner.getRows(sql, params);
    }

    // 사용자 상세정보 조회
    public Map<String, Object> getUserDetail(Integer id){

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("id", id);

        String sql = """
			select au.id
              , up."Name"
              , au.username as login_id
              , au.email
              , ug."Name" as group_name
              , up."UserGroup_id"
              , up."Factory_id"
              , f."Name" as factory_name
              , d."Name" as dept_name
              , up."Depart_id"
              , up.lang_code
              , au.is_active
              , to_char(au.date_joined ,'yyyy-mm-dd hh24:mi') as date_joined
            from auth_user au 
            left join user_profile up on up."User_id" = au.id
            left join user_group ug on up."UserGroup_id" = ug.id 
            left join factory f on up."Factory_id" = f.id 
            left join depart d on d.id = up."Depart_id"
            where au.id = :id
		    """;

        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);

        return item;
    }

    // 사용자 그룹 조회
    public List<Map<String, Object>> getUserGrpList(Integer id) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("id", id);

        String sql = """
        		select ug.id as grp_id
	            , ug."Name" as grp_name
	            ,rd."Char1" as grp_check
	            from user_group ug 
	            left join rela_data rd on rd."DataPk2" = ug.id 
	            and "RelationName" = 'auth_user-user_group' 
	            and rd."DataPk1" = :id
	            where coalesce(ug."Code",'') <> 'dev'
        		""";

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    public Boolean SaveUser(User user, Authentication auth, String authType, String authCode){

        try {
            User UserEntity = userRepository.save(user);

            List<UserGroup> AuthGroup = userGroupRepository.findByCodeAndName(authType, authCode);


            MapSqlParameterSource dicParam = new MapSqlParameterSource();


            User loginUser = (User) auth.getPrincipal();


            dicParam.addValue("loginUser", loginUser.getId());

            String sql = """
		        	INSERT INTO user_profile 
		        	("_created", "_creater_id", "User_id", "lang_code", "Name", "UserGroup_id" ) 
		        	VALUES (now(), :loginUser, :User_id, :lang_code, :name, :UserGroup_id )
		        """;

            dicParam.addValue("name", user.getFirst_name());
            dicParam.addValue("lang_code", "ko-KR");
            //dicParam.addValue("UserGroup_id", );
            dicParam.addValue("User_id", UserEntity.getId());
            dicParam.addValue("lang_code", "ko-KR");
            dicParam.addValue("UserGroup_id", AuthGroup.get(0).getId());

            this.sqlRunner.execute(sql, dicParam);
            return true;
        }catch(Exception e){
            e.getMessage();
            return false;
        }


    }

    public List<Map<String, Object>> getUserSandanList(String id) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("id", id);

        String sql = """
                SELECT t.userid,
                       t.spcompid AS spcompcd,
                       u_c."Value" AS spcompnm,
                       t.spplanid AS spplancd,
                       u_p."Value" AS spplannm,
                       t.spworkid AS spworkcd,
                       u_w."Value" AS spworknm,
                       t.askseq
                FROM tb_rp945 AS t
                LEFT JOIN user_code u_c ON t.spcompid = u_c.id
                LEFT JOIN user_code u_p ON t.spplanid = u_p.id
                LEFT JOIN user_code u_w ON t.spworkid = u_w.id
                WHERE t.userid = :id
                ORDER BY t.askseq;
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    public List<Map<String, Object>> getSpjangList() {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        String sql = """
        		select spjangcd, spjangnm, saupnum from tb_xa012;
        		""";

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    public List<Map<String, Object>> getSpjang(String spjangcd) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("spjangcd", spjangcd);

        String sql = """
        		select spjangcd, spjangnm, saupnum from tb_xa012 where spjangcd = :spjangcd;
        		""";

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    public Map<String, Object> getUserDetailById(String id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);

        String sql = """
        SELECT 
            au.id ,
            au.last_name as prenm,
            au.username AS userid,
            au.email,
            au.tel,
            au.agencycd,
            au.last_login,
            au.is_active,
            au.Phone,
            FORMAT(au.date_joined, 'yyyy-MM-dd') AS date_joined,
            txc.biztypenm ,
            txc.bizitemnm ,
            txc.prenm ,
            txc.cltnm,
            ug.id AS group_id,
            ug.Name AS group_name,
            up.lang_code ,
            au.*,
            txc.zipcd AS postno,
            txc.*,
            CASE
                    WHEN au.spjangcd = 'ZZ' THEN '삼정엘리베이터'
                    WHEN au.spjangcd = 'PP' THEN '삼정엘리베이터(일산)'
                    ELSE '알 수 없음'
                END AS spjType
        FROM 
            auth_user au
        LEFT JOIN 
            user_profile up ON up.User_id = au.id
        LEFT JOIN 
            user_group ug ON ug.id = up.UserGroup_id
        LEFT JOIN 
          TB_XCLIENT txc ON au.username  = txc.saupnum 
        WHERE 
            au.id = :id and txc.relyn ='X'
            """;
//            System.out.println(sql);
        return sqlRunner.getRow(sql, params);
    }

    public List<Map<String, Object>> searchData(String userGroup, String name, String username) {
        // 쿼리 실행 및 결과 반환
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = """
        SELECT au.id,
               up.[Name],
               au.username AS userid,
               up.[UserGroup_id],
               ug.[id] AS group_id,
               au.email,
               au.tel,
               au.phone AS Phone,
               au.agencycd,
               ug.[Name] AS group_name,
               up.[Factory_id],
               uc.[Value],
               au.divinm,
               au.smtpid,
               au.smtppassword,
               up.[Depart_id],
               up.lang_code,
               au.is_active,
               au.is_superuser,
               txc.*,
               au.id ,
            au.last_name,
            au.username AS userid,
            au.email,
            au.tel,
            au.agencycd,
            au.last_login,
            au.is_active,
            au.Phone,
            FORMAT(au.date_joined, 'yyyy-MM-dd') AS date_joined,
            txc.biztypenm ,
            txc.bizitemnm ,
            txc.prenm ,
            txc.cltnm,
            ug.id AS group_id,
            ug.Name AS group_name,
            up.lang_code ,
            au.*,
            FORMAT(au.date_joined, 'yyyy-MM-dd') AS date_joined,
            CASE
                    WHEN au.spjangcd = 'ZZ' THEN '삼정엘리베이터'
                    WHEN au.spjangcd = 'PP' THEN '삼정엘리베이터(일산)'
                    ELSE '알 수 없음'
                END AS spjType
        FROM auth_user au
        LEFT JOIN user_profile up ON up.[User_id] = au.id
        LEFT JOIN user_group ug ON ug.id = up.[UserGroup_id]
        LEFT JOIN user_code uc ON CAST(au.agencycd AS INT) = uc.id
        LEFT JOIN TB_XCLIENT txc ON au.first_name = txc.cltnm
        WHERE 1=1
        """;

        // 조건부 쿼리 추가

        if (!StringUtils.isEmpty(userGroup)) {
            sql += " AND ug.[id] = :userGroup ";
            params.addValue("userGroup", userGroup);
        }
        if (!ObjectUtils.isEmpty(userGroup)) {
            sql += " AND ug.[id] = :userGroup ";
            params.addValue("userGroup", userGroup);
        }

        if (!StringUtils.isEmpty(name)) {
            sql += " AND up.[Name] LIKE '%' + :name + '%' ";
            params.addValue("name", name);
        }

        if (!StringUtils.isEmpty(username)) {
            sql += " AND au.username LIKE '%' + :username + '%' ";
            params.addValue("username", username);
        }

        sql += " ORDER BY au.date_joined DESC";

        // 쿼리 실행 및 결과 반환
        return sqlRunner.getRows(sql, params);
    }

    public boolean isUserIdExists(String userid) {
        return userRepository.existsByUsername(userid); // username 컬럼 확인
    }

    // 내정보리스트
    public Map<String, Object> getUserInfo(String username) {

        Map<String, Object> userInfo = new HashMap<>();

        // 사용자 정보 조회
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            userInfo.put("login_id", user.get().getUsername());
            userInfo.put("prenm", user.get().getUserProfile().getName());
            userInfo.put("email", user.get().getEmail());
            userInfo.put("phone", user.get().getPhone());
            userInfo.put("tel", user.get().getTel());
            userInfo.put("loginPwd", user.get().getPassword());
        }

        // TB_XCLIENT 정보 조회
        Optional<TB_XCLIENT> client = tbXClientRepository.findBySaupnum(username);
        if (client.isPresent()) {
            TB_XCLIENT clientData = client.get();
            userInfo.put("cltnm", clientData.getCltnm()); // 업체명
            userInfo.put("biztypenm", clientData.getBiztypenm()); // 업태
            userInfo.put("bizitemnm", clientData.getBizitemnm()); // 종목
            userInfo.put("postno", clientData.getZipcd()); // 우편번호
            // 주소 분리 및 추가
            String fullAddress = clientData.getCltadres(); // fullAddress에 전체 주소 저장
            Map<String, String> addressParts = splitAddress(fullAddress); // 주소 분리
            userInfo.put("address1", addressParts.get("address1"));
            userInfo.put("address2", addressParts.get("address2"));
        }

        return userInfo;
    }

    public Map<String, String> splitAddress(String fullAddress) {
        Map<String, String> addressParts = new HashMap<>();
        if (fullAddress != null && !fullAddress.isEmpty()) {
            int delimiterIndex = fullAddress.indexOf(" | "); // ' | ' 위치 찾기
            if (delimiterIndex != -1) {
                // ' | '를 기준으로 주소 분리
                addressParts.put("address1", fullAddress.substring(0, delimiterIndex).trim());
                addressParts.put("address2", fullAddress.substring(delimiterIndex + 3).trim());
            } else {
                // ' | '가 없으면 전체를 address1로 간주
                addressParts.put("address1", fullAddress.trim());
                addressParts.put("address2", "");
            }
        } else {
            // fullAddress가 비어있을 경우 기본값 설정
            addressParts.put("address1", "");
            addressParts.put("address2", "");
        }
        return addressParts;
    }


    public Map<String, Object> getActiveClientBySaupnumAndPrenm(String userid, String prenm) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("userid", userid);
        dicParam.addValue("prenm", prenm);

        String sql = """
        		SELECT * from TB_XCLIENT 
        		where saupnum = :userid
        		and prenm= :prenm
        		and relyn='X'
        		""";
        log.info("업체 여부 검색TB_XCLIENT SQL: {}", sql);
        log.info("SQL Parameters: {}", dicParam.getValues());
        return sqlRunner.getRow(sql, dicParam);
    }
}
