package mes.app.balju;

import lombok.extern.slf4j.Slf4j;
import mes.app.balju.service.BaljuOrderService;
import mes.domain.entity.User;
import mes.domain.entity.samjungEntity.TB_CA660;
import mes.domain.entity.samjungEntity.TB_CA661;
import mes.domain.model.AjaxResult;
import mes.domain.repository.samjangRepository.TB_CA660repository;
import mes.domain.repository.samjangRepository.TB_CA661repository;
import mes.domain.services.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/balju/balju_order")
public class BaljuOrderController {

  @Autowired
  BaljuOrderService baljuOrderService;

  @Autowired
  TB_CA660repository tb_ca660repository;

  @Autowired
  TB_CA661repository tb_ca661repository;

  @GetMapping("/read")
  public AjaxResult getSujuList(
      @RequestParam(value = "date_kind", required = false) String date_kind,
      @RequestParam(value = "start", required = false) String start_date,
      @RequestParam(value = "end", required = false) String end_date,
      //@RequestParam(value = "spjangcd") String spjangcd,
      Authentication auth,
      HttpServletRequest request) {
    //log.info("발주 read--- date_kind:{}, start_date:{},end_date:{} , spjangcd:{} " ,date_kind,start_date , end_date, spjangcd);
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";
    User user = (User) auth.getPrincipal();
    String spjangcd = user.getSpjangcd();
    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.baljuOrderService.getBaljuList(date_kind, start, end, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  @PostMapping("/multi_save")
  @Transactional
  public AjaxResult saveBaljuMulti(@RequestBody Map<String, Object> payload, Authentication auth) {
    log.info("💾 [발주등록 시작] payload keys: {}", payload.keySet());
    log.info("🧾 items 내용: {}", payload.get("items"));

    User user = (User) auth.getPrincipal();

    AjaxResult result = new AjaxResult();

    try {
      Integer balJunum = CommonUtil.tryIntNull(payload.get("BALJUNUM"));
      log.info("📌 BALJUNUM: {}", balJunum);

      String rawIchdate = (String) payload.get("ichdate");
      String ichdate = rawIchdate != null ? rawIchdate.replaceAll("-", "") : null;
      log.info("📅 입고예정일: {} → {}", rawIchdate, ichdate);

      String spjangcd = user.getSpjangcd();
      String custcd = baljuOrderService.getCustcd(spjangcd);

      log.info("🏢 고객사코드 조회: spjangcd = {}, custcd = {}", spjangcd, custcd);

      String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      log.info("📆 오늘 날짜: {}", today);

      TB_CA660 head;

      if (balJunum != null) {
        log.info("✏️ [수정 모드] BALJUNUM = {}", balJunum);

        head = tb_ca660repository.findById(balJunum)
            .orElseThrow(() -> new RuntimeException("발주 헤더 없음"));

        // 수정 항목 로그 생략 가능
        head.setPernm((String) payload.get("pernm"));
        head.setCustcd(custcd);
        head.setSpjangcd(user.getSpjangcd());
        head.setProcd((String) payload.get("projcet_no"));
        head.setPertelno((String) payload.get("pertelno"));
        head.setActcd((String) payload.get("actcd"));
        head.setActnm((String) payload.get("actnm"));
        head.setCltcd((String) payload.get("cltcd"));
        head.setIchdate(ichdate);
        head.setActaddress((String) payload.get("actaddress"));
        head.setCltpernm((String) payload.get("cltpernm"));
        head.setCltjik((String) payload.get("cltjik"));
        head.setClttelno((String) payload.get("clttelno"));
        head.setCltemail((String) payload.get("cltemail"));
        head.setRemark01((String) payload.get("remark01"));
        head.setRemark02((String) payload.get("remark02"));
        head.setRemark03((String) payload.get("remark03"));

        tb_ca660repository.save(head);
        log.info("✅ 헤더 수정 완료");

        // 기존 상세 삭제
        tb_ca661repository.deleteByBaljunum(balJunum);
        log.info("🧹 기존 상세 삭제 완료: BALJUNUM = {}", balJunum);

      } else {
        log.info("🆕 [신규 등록 모드]");

        head = new TB_CA660();
        head.setSpjangcd(user.getSpjangcd());
        head.setPernm((String) payload.get("pernm"));
        head.setCustcd(custcd);
        head.setPertelno((String) payload.get("pertelno"));
        head.setActcd((String) payload.get("actcd"));
        head.setActnm((String) payload.get("actnm"));
        head.setCltcd((String) payload.get("cltcd"));
        head.setIchdate(ichdate);
        head.setActaddress((String) payload.get("actaddress"));
        head.setCltpernm((String) payload.get("cltpernm"));
        head.setCltjik((String) payload.get("cltjik"));
        head.setClttelno((String) payload.get("clttelno"));
        head.setCltemail((String) payload.get("cltemail"));
        head.setRemark01((String) payload.get("remark01"));
        head.setRemark02((String) payload.get("remark02"));
        head.setRemark03((String) payload.get("remark03"));
        head.setBaljudate(today);

        tb_ca660repository.save(head);
        balJunum = head.getBalJunum(); // 신규 발번
        log.info("✅ 신규 헤더 저장 완료: BALJUNUM = {}", balJunum);
      }

      // ✅ 품목 저장
      List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
      log.info("📦 품목 수: {}", items.size());

      for (Map<String, Object> item : items) {
        if (item.values().stream().allMatch(v -> v == null || v.toString().trim().isEmpty())) {
          continue;
        }
        try {
          TB_CA661 detail = new TB_CA661();
          detail.setBalJunum(head);
          detail.setCustcd(custcd);
          detail.setSpjangcd(spjangcd);
          detail.setProcd((String) payload.get("actcd"));
          detail.setBaljudate(today);

          detail.setPcode((String) item.get("pcode"));
          detail.setPname((String) item.get("pname"));
          detail.setPsize((String) item.get("psize"));
          detail.setPunit((String) item.get("punit"));
          detail.setPqty(CommonUtil.tryIntNull(item.get("quantity")));
          detail.setPuamt(CommonUtil.tryIntNull(item.get("unit_price")));
          detail.setPamt(CommonUtil.tryIntNull(item.get("amount")));
          detail.setPmapseq((String) item.get("pmapseq"));
          detail.setChulflag("0");
          detail.setFacflag("0");
          detail.setHyunflag("0");
          detail.setRemark((String) item.get("remark"));

          tb_ca661repository.save(detail);
          log.info("✅ 상세 저장 완료: {}", item.get("pcode"));
        } catch (Exception e) {
          log.error("❌ 상세 저장 실패: {}", item, e);
          throw e; // 트랜잭션 롤백
        }
      }

      result.success=(true);
      result.message=("발주 저장 완료");
      log.info("🎉 발주 전체 저장 완료: BALJUNUM = {}", balJunum);

    } catch (Exception e) {
      log.error("❌ 발주 저장 중 예외 발생", e);
      result.success = (false);
      result.message = ("저장 중 오류가 발생했습니다: " + e.getMessage());
    }

    return result;
  }



  /*@PostMapping("/multi_save")
  @Transactional
  public AjaxResult saveBaljuMulti(@RequestBody Map<String, Object> payload, Authentication auth) {
    log.info("발주등록 들어옴");
    log.info("📦 payload keys: {}", payload.keySet());  // items가 포함되어야 함
    log.info("🧾 payload 내용: {}", payload);
    log.info("🧾 items 내용: {}", payload.get("items"));
    User user = (User) auth.getPrincipal();

    Integer balJunum = CommonUtil.tryIntNull(payload.get("BALJUNUM"));
    String rawIchdate = (String) payload.get("ichdate");
    String ichdate = rawIchdate != null ? rawIchdate.replaceAll("-", "") : null;
    String spjangcd = user.getSpjangcd();
    String custcd = baljuOrderService.getCustcd(spjangcd);
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    TB_CA660 head;

    if (balJunum != null) {
      // ✅ 수정 로직
      head = tb_ca660repository.findById(balJunum)
          .orElseThrow(() -> new RuntimeException("발주 헤더 없음"));

      head.setPernm((String) payload.get("pernm"));
      head.setCustcd(custcd);
      head.setPertelno((String) payload.get("pertelno"));
      head.setActcd((String) payload.get("actcd")); //현장 코드
      head.setActnm((String) payload.get("actnm")); //현장명
      head.setCltcd((String) payload.get("cltcd"));
      head.setIchdate(ichdate);
      head.setActaddress((String) payload.get("actaddress")); //현장주소
      head.setCltpernm((String) payload.get("cltpernm")); //발주처 담당자
      head.setCltjik((String) payload.get("cltjik"));
      head.setClttelno((String) payload.get("clttelno"));
      head.setCltemail((String) payload.get("cltemail"));
      head.setRemark01((String) payload.get("remark01"));
      head.setRemark02((String) payload.get("remark02"));
      head.setRemark03((String) payload.get("remark03"));


      tb_ca660repository.save(head);
      log.info("✅ 헤더 수정 완료");
    } else {
      // ✅ 신규 등록 로직

      head = new TB_CA660();
      head.setPernm((String) payload.get("pernm"));
      head.setCustcd(custcd);
      head.setPertelno((String) payload.get("pertelno"));
      head.setActcd((String) payload.get("actcd")); //현장 코드
      head.setActnm((String) payload.get("actnm")); //현장명
      head.setCltcd((String) payload.get("cltcd"));
      head.setIchdate(ichdate);
      head.setActaddress((String) payload.get("actaddress")); //현장주소
      head.setCltpernm((String) payload.get("cltpernm")); //발주처 담당자
      head.setCltjik((String) payload.get("cltjik"));
      head.setClttelno((String) payload.get("clttelno"));
      head.setCltemail((String) payload.get("cltemail"));
      head.setRemark01((String) payload.get("remark01"));
      head.setRemark02((String) payload.get("remark02"));
      head.setRemark03((String) payload.get("remark03"));
      head.setBaljudate(today);

      // 저장 시 자동으로 BALJUNUM 생성됨 (IDENTITY)
      tb_ca660repository.save(head);
      balJunum = head.getBalJunum(); // 신규 발번
      log.info("✅ 신규 헤더 저장 완료: BALJUNUM = {}", balJunum);

    }

    //품목 저장
    // 기존 상세 삭제 (수정 시)
    if (balJunum != null) {
      tb_ca661repository.deleteByBaljunum(head.getBalJunum());
    }

    // 상세 저장
    List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

    for (Map<String, Object> item : items) {
      TB_CA661 detail = new TB_CA661();

      // 💡 발주헤더 객체를 직접 주입 (FK 매핑 처리)
      detail.setBalJunum(head);  // 여기서 Integer가 아니라 TB_CA660 객체임

      detail.setCustcd(custcd);
      detail.setSpjangcd(user.getSpjangcd()); // 사업장코드
      detail.setProcd((String) payload.get("actcd")); // 현장코드를 프로젝트코드로 저장

      // 현재 날짜를 발주일자로 저장
      detail.setBaljudate(today);

      detail.setPcode((String) item.get("pcode"));
      detail.setPname((String) item.get("pname"));
      detail.setPsize((String) item.get("psize"));
      detail.setPqty((Integer) item.get("quantity")); //수량
      detail.setPmapseq((String) item.get("pmapseq"));  //도번
      detail.setRemark((String) item.get("remark"));

      detail.setPqty(CommonUtil.tryIntNull(item.get("quantity")));
      detail.setPuamt(CommonUtil.tryIntNull(item.get("unit_price")));
      detail.setPamt(CommonUtil.tryIntNull(item.get("amount")));

      tb_ca661repository.save(detail);
      log.info("상세 데이터 저장 :{}", detail);
    }
    AjaxResult result = new AjaxResult();
    return result;
  }*/

}

