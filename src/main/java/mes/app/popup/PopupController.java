package mes.app.popup;


import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.domain.model.AjaxResult;
import mes.domain.services.DateUtil;
import mes.domain.services.SqlRunner;

@Slf4j
@RestController
@RequestMapping("/api/popup")
public class PopupController {
	
	@Autowired
	SqlRunner sqlRunner;
	
	@RequestMapping("/search_material")
	public AjaxResult getSearchMaterial(
			@RequestParam(value="keyword", required=false) String keyword
			) {
		AjaxResult result = new AjaxResult();
		
		String sql ="""
	           select
									bpid ,
								 eco_no,
								 PART_NO ,
								 PART_NM ,
								 BLOCK_NO ,
								 G_NO ,
								 DRAWING_NO ,
								 GUBUN ,
								 unit,
								 spec,
								 PART_SIZE
								 from TB_CA662
								 where 1=1
	    """;
		if(StringUtils.hasText(keyword)){
            sql+="""
            and (PART_NO like concat('%%',:keyword,'%%') or PART_NM like concat('%%',:keyword,'%%'))
            """;
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("keyword", keyword);
		result.data = this.sqlRunner.getRows(sql, paramMap);
		return result;
	}	
	
	
	@RequestMapping("/search_equipment")
	public AjaxResult getSearchMaterial(
			@RequestParam(value="group_id", required=false) Integer equipment_group,			
			@RequestParam(value="keyword", required=false) String keyword			
			) {
		AjaxResult result = new AjaxResult();
		
		String sql ="""
	            select 
                 e.id
                 , e."Code"
                 , e."Name"
                 , eg."Name" as group_name
                 , eg."EquipmentType"
                 , fn_code_name('equipment_type',  eg."EquipmentType") as "EquipmentTypeName"
                from equ e
                  left join equ_grp eg on e."EquipmentGroup_id" = eg.id
                where 1=1  
	    """;
		
		if(equipment_group!=null){
            sql+="""            		
            and e."EquipmentGroup_id"=:equipment_group
            """;	
		}
		
		if(StringUtils.hasText(keyword)){
            sql+="""
            and upper(e."Name") like concat('%%',:keyword,'%%')
            """;		
		}
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("equipment_group", equipment_group, java.sql.Types.INTEGER);
		paramMap.addValue("keyword", keyword);
		
		result.data = this.sqlRunner.getRows(sql, paramMap);
		
		return result;		
	}
	
	@RequestMapping("/pop_prod_input/mat_list")
	public AjaxResult getMatList(
			@RequestParam(value="mat_type", required=false) String matType,
			@RequestParam(value="mat_grp_id", required=false) Integer matGrpId,
			@RequestParam(value="keyword", required=false) String keyword,
			@RequestParam(value="jr_pk", required=false) Integer jrPk,
			@RequestParam(value="bom_comp_yn", required=false) String bomCompYn) {
		
		AjaxResult result = new AjaxResult();
		
		Timestamp today = DateUtil.getNowTimeStamp();
		
		String sql = "";
		
		if (bomCompYn.equals("Y")) {
			sql = """
				 select m.id, m."Code" as mat_code, m."Name" as mat_name
                , m."MaterialGroup_id" as mat_grp_id, mg."Name" as mat_grp_name
                , mg."MaterialType" as mat_type
                , sc."Value" as mat_type_name
                , u."Name" as unit_name
                from job_res jr
                inner join tbl_bom_detail(jr."Material_id"::text, cast(to_char(cast(:today as date),'YYYY-MM-DD') as text)) a  on a.b_level = 1 
                inner join material m on m.id = a.mat_pk
                left join unit u on m."Unit_id" = u.id
                left join mat_grp mg on m."MaterialGroup_id" = mg.id
                left join sys_code sc on mg."MaterialType" = sc."Code" 
                and sc."CodeType" ='mat_type'
                where jr.id =  :jrPk
                and m."LotUseYN" = 'Y'
				 """;
			
			MapSqlParameterSource paramMap = new MapSqlParameterSource();
			paramMap.addValue("jrPk", jrPk);
			paramMap.addValue("today", today);
			
			result.data = this.sqlRunner.getRows(sql, paramMap);
			
		} else {
			sql = """
					select m.id, m."Code" as mat_code, m."Name" as mat_name
	                , m."MaterialGroup_id" as mat_grp_id, mg."Name" as mat_grp_name
	                , mg."MaterialType" as mat_type
	                , sc."Value" as mat_type_name
	                , u."Name" as unit_name
	                from material m
	                left join unit u on m."Unit_id" = u.id
	                left join mat_grp mg on m."MaterialGroup_id" = mg.id
	                left join sys_code sc on mg."MaterialType" = sc."Code" 
	                and sc."CodeType" ='mat_type'
	                where 1=1
	                and m."LotUseYN" = 'Y'
				  """;
			
			if(!matType.isEmpty()) sql += "and mg.\"MaterialType\" = :matType ";
			if(matGrpId != null) sql += "and mg.\"id\" = :matGrpId ";
			if(!keyword.isEmpty()) sql += " and m.\"Name\" like concat('%%',:keyword,'%%') ";
			
			MapSqlParameterSource paramMap = new MapSqlParameterSource();
			paramMap.addValue("matType", matType);
			paramMap.addValue("matGrpId", matGrpId);
			paramMap.addValue("keyword", keyword);
			
			result.data = this.sqlRunner.getRows(sql, paramMap);
		}
		
		
		return result;
	}
	
	@RequestMapping("/pop_prod_input/mat_lot_list")
	public AjaxResult getMatLotList(
			@RequestParam(value="mat_pk", required=false) Integer matPk,
			@RequestParam(value="jr_pk", required=false) Integer jrPk) {
	
		AjaxResult result = new AjaxResult();
		
		String sql = """
		        with aa as (
		        	select mpi."MaterialLot_id" as mat_lot_id from job_res jr 
			        inner join mat_proc_input mpi on jr."MaterialProcessInputRequest_id" = mpi."MaterialProcessInputRequest_id" 
			        where jr.id = :jrPk
		        )
		       	select 
				a.id, m."Name" as mat_name, a."LotNumber" as lot_number
		        , a."CurrentStock" as cur_stock
		        , a."InputQty" as first_qty
		        , sh."Name" as storehouse_name
		        , to_char(a."EffectiveDate",'yyyy-mm-dd') as effective_date
		        , to_char(a."InputDateTime",'yyyy-mm-dd') as create_date
		        , case when aa.mat_lot_id is not null then 'Y' else 'N' end as lot_use
		        from mat_lot a
		        inner join material m on m.id = a."Material_id"
		        left join aa on aa.mat_lot_id = a.id
		        left join store_house sh on sh.id = a."StoreHouse_id" 
		        where a."Material_id" = :matPk
		        and a."CurrentStock" > 0
		        order by a."EffectiveDate" , a."InputDateTime" 
				""";
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("matPk", matPk);
		paramMap.addValue("jrPk", jrPk);
		
		result.data = this.sqlRunner.getRows(sql, paramMap);
		
		return result;
	}
	
	@RequestMapping("/pop_prod_input/lot_info")
	public AjaxResult getLotInfo(
			@RequestParam(value="lot_number", required=false) String lotNumber) {
		
		AjaxResult result = new AjaxResult();
		
		String sql = """
			select a.id
			, mg."Name" as mat_grp_name
			, m."Name" as mat_name
	        , a."CurrentStock" as cur_stock
	        , a."InputQty" as first_qty
	        , sh."Name" as storehouse_name
		    , to_char(a."EffectiveDate",'yyyy-mm-dd') as effective_date
		    , to_char(a."InputDateTime",'yyyy-mm-dd') as create_date
		    from mat_lot a
		    inner join material m on m.id = a."Material_id" 
		    left join store_house sh on sh.id = a."StoreHouse_id"
		    left join mat_grp mg on mg.id = m."MaterialGroup_id" 
		    where a."LotNumber" = :lotNumber
			""";
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("lotNumber", lotNumber);
		
		result.data = this.sqlRunner.getRow(sql, paramMap);
		
		return result;
	}
	
	@RequestMapping("/search_approver/read")
	public List<Map<String, Object>> getSearchApprover(
			@RequestParam(value="depart_id", required=false) Integer depart_id,
			@RequestParam(value="keyword", required=false) String keyword) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("depart_id", depart_id);
		paramMap.addValue("keyword", keyword);
				
		String sql = """
				select up."User_id"
		        ,up."Name"
		        ,up."Depart_id" 
		        ,d."Name" as "DepartName"
		        from user_profile up 
		        left join depart d on d.id = up."Depart_id"
	            where 1=1 
				""";
		
		if (keyword != null) {
			sql += " and upper(up.\"Name\") like concat('%%',upper(:keyword),'%%') ";
        }
        
		if (depart_id != null) {
        	sql += " and up.\"Depart_id\" = :depart_id ";
        }
        
    	sql += " order by COALESCE(d.\"Name\",'Z') , up.\"Name\" ";
    	
		
    	List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
	}
	
	@RequestMapping("/search_user_code/read")
	public List<Map<String, Object>> getSearchUserCode(
			@RequestParam(value="parent_code", required=false) String parentCode){
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("parentCode", parentCode);
		
		String sql = """
	            select c.id, c."Code", c."Value", c."Description"
	            from user_code c
	            where exists (
		            select 1
		            from user_code
		            where "Code" = :parentCode
		            and "Parent_id" is null
		            and c."Parent_id" = id
	            )
	            order by _order
				""";
		
    	List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
		
	}

	@RequestMapping("/search_Comp_purchase")
	public AjaxResult getSearchCompPurchase(
			@RequestParam(value = "cltcd", required = false) String cltcd,
			@RequestParam(value = "cltnm", required = false) String cltnm,
			@RequestParam(value = "saupnum", required = false) String saupnum) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("cltcd", cltcd);
		paramMap.addValue("cltnm", cltnm);
		paramMap.addValue("saupnum", saupnum);
		AjaxResult result = new AjaxResult();

		String sql = """
		 select
				cltcd , -- 업체코드
				cltnm , -- 업체명
				prenm, -- 재표자 명
				saupnum , -- 사업자 번허
				cltadres , -- 주소
				bizitemnm , -- 업종
				biztypenm , -- 업태
				telnum ,
				taxmail,
				agnernm, 	--업체 담당자
				agntel, 	-- 업체 담당자 전화번호
				agneremail,	--담당자 이메일
				agnerdivinm	--담당자부서
				from TB_XCLIENT where relyn ='X' -- 영문 대문자(O, X) O: 거래중지, X: 거래중
				AND clttype IN ('1', '3') 
    """;

		if (cltcd != null && !cltcd.isEmpty()) {
			sql += " and cltcd like :cltcd ";
			paramMap.addValue("cltcd", "%" + cltcd + "%");
		}

		if (cltnm != null && !cltnm.isEmpty()) {
			sql += " and cltnm like :cltnm ";
			paramMap.addValue("cltnm", "%" + cltnm + "%");
		}

		if (saupnum != null && !saupnum.isEmpty()) {
			sql += " and saupnum like :saupnum ";
			paramMap.addValue("saupnum", "%" + saupnum + "%");
		}

		sql += " ORDER BY cltnm ASC ";

		result.data = this.sqlRunner.getRows(sql, paramMap);
		return result;
	}

	@RequestMapping("/search_Comp_all")
	public AjaxResult getSearchCompAll(
			@RequestParam(value = "compCode", required = false) String compCode,
			@RequestParam(value = "compName", required = false) String compName,
			@RequestParam(value = "business_number", required = false) String business_number){

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("compCode", compCode);
		paramMap.addValue("compName", compName);
		paramMap.addValue("business_number", business_number);
		AjaxResult result = new AjaxResult();

		String sql = """
            select id as id
            , "Name" as compName
            , "Code" as compCode
            , "BusinessNumber" as business_number
            , "TelNumber" as tel_number
            , "CEOName" as invoiceeceoname
            , "Address" as invoiceeaddr
            , "BusinessType" as invoiceebiztype
            , "BusinessItem" as invoiceebizclass
            , "AccountManager" as invoiceecontactname1
            , "AccountManagerPhone" as invoiceetel1
            , "Email" as invoiceeemail1
            from company
            WHERE  "relyn" = '0'
			""";

		if (compCode != null && !compCode.isEmpty()) {
			sql += " AND \"Code\" LIKE :compCode ";
			paramMap.addValue("compCode", "%" + compCode + "%");
		}

		if (compName != null && !compName.isEmpty()) {
			sql += " AND \"Name\" LIKE :compName ";
			paramMap.addValue("compName", "%" + compName + "%");
		}

		if (business_number != null && !business_number.isEmpty()) {
			sql += " AND \"BusinessNumber\" LIKE :business_number ";
			paramMap.addValue("business_number", "%" + business_number + "%");
		}

		sql += " ORDER BY \"Name\" ASC ";

		result.data = this.sqlRunner.getRows(sql, paramMap);

		return result;
	}

	@RequestMapping("/project_list")
	public AjaxResult getProjcet_list(
			@RequestParam(value = "PROJECT_NO", required = false) String project_no,
			@RequestParam(value = "PROJECT_NM", required = false) String project_nm){

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("PROJECT_NO", project_no);
		paramMap.addValue("PROJECT_NM", project_nm);
		AjaxResult result = new AjaxResult();

		String sql = """
				select
					 BOMID ,
					 ECO_NO as eco_no,
					 PROJECT_NO as project_no,
					 PROJECT_NM as project_nm,
					 BPDATE ,
					 BPPERNM 
					 from TB_CA664
			""";

		if (project_no != null && !project_no.isEmpty()) {
			sql += " AND PROJECT_NO LIKE :project_no ";
			paramMap.addValue("project_no", "%" + project_no + "%");
		}
		if (project_nm != null && !project_nm.isEmpty()) {
				sql += " AND PROJECT_NM LIKE :project_nm ";
				paramMap.addValue("project_nm", "%" + project_nm + "%");
		}

		result.data = this.sqlRunner.getRows(sql, paramMap);

		return result;
	}

	@RequestMapping("/search_staff")
	public AjaxResult getsearch_staffchase(
			@RequestParam(value = "pernm", required = false) String pernm,
			@RequestParam(value = "perid", required = false) String perid) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("pernm", pernm);
		paramMap.addValue("perid", perid);
		AjaxResult result = new AjaxResult();

		String sql = """
       select
				 j.rspcd ,
				 p.RSPNM ,
				 j.perid ,
				 j.pernm ,
				 j.handphone ,
				 j.spjangcd ,
				 t.spjangnm
				 from tb_ja001 j
				 left join tb_pz001 p on j.spjangcd = p.SPJANGCD and j.rspcd = p.RSPCD
				 left join tb_xa012 t on j.spjangcd = t.spjangcd
				 where j.rtclafi = '001'
    """;

		if (pernm != null && !pernm.isEmpty()) {
			sql += " and j.pernm like :pernm ";
			paramMap.addValue("pernm", "%" + pernm + "%");
		}

		if (perid != null && !perid.isEmpty()) {
			sql += " and j.perid like :perid ";
			paramMap.addValue("perid", "%" + perid + "%");
		}

		sql += " ORDER BY j.pernm ASC ";

		result.data = this.sqlRunner.getRows(sql, paramMap);
		return result;
	}

	@RequestMapping("/search_userid")
	public AjaxResult getSearch_userId_chase(
			@RequestParam(value = "pernm", required = false) String pernm,
			@RequestParam(value = "spjangcd", required = false) String spjangcd,
			@RequestParam(value = "perid", required = false) String perid) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("pernm", pernm);
		paramMap.addValue("perid", perid);
		paramMap.addValue("spjangcd", spjangcd);
		AjaxResult result = new AjaxResult();

		String sql = """
     SELECT
				     u.userid,
				     u.perid,
				     jc.divinm,
				     p.RSPNM,
				     j.handphone,
						 j.zipcd,
						 j.rzipadres ,
				     u.*
				 FROM
				     tb_xusers u
				 LEFT JOIN tb_ja001 j
				     ON j.perid = CONCAT('p', u.perid)
				 LEFT JOIN tb_jc002 jc
				     ON j.divicd = jc.divicd
				 LEFT JOIN tb_pz001 p
				     ON j.rspcd = p.RSPCD
				 WHERE
				     u.useyn = '1'
				     AND j.rtclafi = '001'
				     and u.spjangcd =:spjangcd
    """;

		if (pernm != null && !pernm.isEmpty()) {
			sql += " and u.pernm like :pernm ";
			paramMap.addValue("pernm", "%" + pernm + "%");
		}

		if (perid != null && !perid.isEmpty()) {
			sql += " and u.perid like :perid ";
			paramMap.addValue("perid", "%" + perid + "%");
		}

		sql += " ORDER BY pernm ASC ";

		result.data = this.sqlRunner.getRows(sql, paramMap);
		return result;
	}

	@RequestMapping("/search_acmtnm")
	public AjaxResult getsearch_acmtnmchase(
			@RequestParam(value = "actnm", required = false) String actnm,
			@RequestParam(value = "address", required = false) String address) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("actnm", actnm);
		paramMap.addValue("address", address);
		AjaxResult result = new AjaxResult();

		String sql = """
      SELECT
				  actcd ,
				  actnm,
				  address
				  FROM TB_E601
				  where 1 = 1
    """;

		if (actnm != null && !actnm.isEmpty()) {
			sql += " and actnm like :actnm ";
			paramMap.addValue("actnm", "%" + actnm + "%");
		}

		if (address != null && !address.isEmpty()) {
			sql += " and address like :address ";
			paramMap.addValue("address", "%" + address + "%");
		}
		sql += " ORDER BY actnm ASC ";

		result.data = this.sqlRunner.getRows(sql, paramMap);
		return result;
	}

	@RequestMapping("/search_part")
	public AjaxResult getSearch_part(
			@RequestParam(value = "PART_NM", required = false) String PART_NM,
			@RequestParam(value = "PART_NO", required = false) String PART_NO,
			@RequestParam(value = "eco_no", required = false) String eco_no){

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("PART_NM", PART_NM);
		paramMap.addValue("PART_NO", PART_NO);
		paramMap.addValue("eco_no", eco_no);
		log.info("PART_NM:{},PART_NO:{},eco_no:{}", PART_NM, PART_NO, eco_no);
		AjaxResult result = new AjaxResult();

		String sql = """
			select
				bpid ,
				eco_no,
				PART_NO ,
				PART_NM ,
				BLOCK_NO ,
				G_NO ,
				DRAWING_NO ,
				GUBUN ,
				unit,
				spec,
				PART_SIZE
				from TB_CA662
				where 1=1
    """;

		if (eco_no != null && !eco_no.isEmpty()) {
			sql += " and eco_no like :eco_no ";
			paramMap.addValue("eco_no", "%" + eco_no + "%");
		}
		if (PART_NM != null && !PART_NM.isEmpty()) {
			sql += " and PART_NM like :PART_NM ";
			paramMap.addValue("PART_NM", "%" + PART_NM + "%");
		}

		if (PART_NO != null && !PART_NO.isEmpty()) {
			sql += " and PART_NO like :PART_NO ";
			paramMap.addValue("PART_NO", "%" + PART_NO + "%");
		}
		sql += " ORDER BY bpid ASC ";

		result.data = this.sqlRunner.getRows(sql, paramMap);
		return result;
	}

}