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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
//    log.info("💾 [발주등록 시작] payload keys: {}", payload.keySet());
//    log.info("🧾 items 내용: {}", payload.get("items"));

    User user = (User) auth.getPrincipal();

    AjaxResult result = new AjaxResult();

    try {
      Integer balJunum = CommonUtil.tryIntNull(payload.get("BALJUNUM"));
      //log.info("📌 BALJUNUM: {}", balJunum);
      String rawIchdate = (String) payload.get("ichdate");
      String ichdate = rawIchdate != null ? rawIchdate.replaceAll("-", "") : null;
      String spjangcd = user.getSpjangcd();
      String custcd = baljuOrderService.getCustcd(spjangcd);
      String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      TB_CA660 head;

      if (balJunum != null) {
//        log.info("✏️ [수정 모드] BALJUNUM = {}", balJunum);

        head = tb_ca660repository.findById(balJunum)
            .orElseThrow(() -> new RuntimeException("발주 헤더 없음"));

        // 수정 항목 로그 생략 가능
        head.setPernm((String) payload.get("pernm"));
        head.setCustcd(custcd);
        head.setProcd((String) payload.get("project_no"));
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
        head.setSpjangcd(user.getSpjangcd());

        tb_ca660repository.save(head);
//        log.info("✅ 헤더 수정 완료");

        // 기존 상세 삭제
        tb_ca661repository.deleteByBaljunum(balJunum);
//        log.info("🧹 기존 상세 삭제 완료: BALJUNUM = {}", balJunum);

      } else {
//        log.info("🆕 [신규 등록 모드]");

        head = new TB_CA660();
        head.setSpjangcd(user.getSpjangcd());
        head.setPernm((String) payload.get("pernm"));
        head.setProcd((String) payload.get("project_no"));
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
//        log.info("✅ 신규 헤더 저장 완료: BALJUNUM = {}", balJunum);
      }

      // ✅ 품목 저장
      List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
//      log.info("📦 품목 수: {}", items.size());

      for (Map<String, Object> item : items) {
        if (item.values().stream().allMatch(v -> v == null || v.toString().trim().isEmpty())) {
          continue;
        }
        try {
          TB_CA661 detail = new TB_CA661();
          detail.setBalJunum(head);
          detail.setCustcd(custcd);
          detail.setSpjangcd(spjangcd);
          detail.setProcd((String) payload.get("project_no"));
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
//          log.info("✅ 상세 저장 완료: {}", item.get("pcode"));
        } catch (Exception e) {
          log.error("❌ 상세 저장 실패: {}", item, e);
          throw e; // 트랜잭션 롤백
        }
      }

      result.success=(true);
      result.message=("발주 저장 완료");
//      log.info("🎉 발주 전체 저장 완료: BALJUNUM = {}", balJunum);

    } catch (Exception e) {
      log.error("❌ 발주 저장 중 예외 발생", e);
      result.success = (false);
      result.message = ("저장 중 오류가 발생했습니다: " + e.getMessage());
    }

    return result;
  }

  @GetMapping("/detail")
  public AjaxResult getDetail (@RequestParam(value="id") Integer baljunum) {

    Map<String, Object> item = this.baljuOrderService.getBaljuDetail(baljunum);

    AjaxResult result = new AjaxResult();
    result.data = item;

    return result;
  }

  // 발주 삭제
  @PostMapping("/delete")
  @Transactional
  public AjaxResult deleteBalJu(
      @RequestParam("id") Integer baljunum) {

    AjaxResult result = new AjaxResult();

    Optional<TB_CA660> optionalHead = tb_ca660repository.findById(baljunum);
    if (!optionalHead.isPresent()) {
      result.success = false;
      result.message = "해당 발주 정보가 존재하지 않습니다.";
      return result;
    }

    TB_CA660 head = optionalHead.get();

    // 1. 기준 정보 추출
    Integer BALJUNUM = head.getBalJunum();

    // 2. 해당 기준으로 tb_ca661 삭제
    tb_ca661repository.deleteByBaljunum(BALJUNUM);

    // 3. balju_head 삭제
    tb_ca660repository.deleteById(baljunum);

    result.success = true;
    return result;
  }

  @PostMapping("/print/purchase")
  public ResponseEntity<Map<String, Object>> printPurchase(@RequestBody Map<String, Object> payload) {
    try {
      Integer bhId = (Integer) payload.get("bhId");
      Map<String, Object> baljuData = baljuOrderService.getBaljuDetail(bhId);

      String jumunNumber = (String) baljuData.get("JumunNumber");  // 주문번호
      String companyName = (String) baljuData.get("CompanyName");
      String fileName = String.format("%s_%s_외주발주서.xlsx", jumunNumber, companyName.replaceAll("[\\\\/:*?\"<>|]", ""));

      Path tempXlsx = Paths.get("C:/Temp/mes21/외주발주서" + fileName);
      Files.createDirectories(tempXlsx.getParent());
      Files.deleteIfExists(tempXlsx);

      try (FileInputStream fis = new FileInputStream("C:/Temp/mes21/문서/외주발주서.xlsx");
           Workbook workbook = new XSSFWorkbook(fis);
           FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

        Sheet sheet = workbook.getSheetAt(0);

        // 날짜 바인딩
        String dateStr = String.valueOf(baljuData.get("ichdate")); // 예: "2025-07-19"
        LocalDate date = LocalDate.parse(dateStr);
        String formatted = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        setCell(sheet, 2, 6, formatted); // G3 셀에 납기일자

        // 업체명
        setCell(sheet, 2, 1, (String) baljuData.get("CompanyName")); // B3

        // 현장명
        setCell(sheet, 3, 1, (String) baljuData.get("actnm")); // C6~F6 -->병합하고 가운데게 맞춤 되어있음

        // 특이사항
        setCell(sheet, 34, 1, (String) baljuData.get("remark01")); // B35
        setCell(sheet, 35, 1, (String) baljuData.get("remark02")); // B36
        setCell(sheet, 36, 1, (String) baljuData.get("remark03")); // B37

        // 품목 리스트 바인딩
        List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");
        int maxItemsPerPage = 14;
        int totalItems = items.size();
        int totalPages = (int) Math.ceil(totalItems / (double) maxItemsPerPage);

        for (int page = 0; page < totalPages; page++) {

          if (page == 0) {
            sheet = workbook.getSheetAt(0); // 첫 페이지는 원본 시트
          } else {
            sheet = workbook.cloneSheet(0); // 복제
          }

          workbook.setSheetName(workbook.getSheetIndex(sheet), "Page " + (page + 1));

          int startIdx = page * maxItemsPerPage;
          int endIdx = Math.min(startIdx + maxItemsPerPage, totalItems);
          List<Map<String, Object>> subItems = items.subList(startIdx, endIdx);

          // 품목 바인딩 시작 행
          int startRow = 14;

          for (int i = 0; i < subItems.size(); i++) {
            Map<String, Object> item = subItems.get(i);
            Row row = sheet.getRow(startRow + i);
            if (row == null) row = sheet.createRow(startRow + i);

            row.createCell(0).setCellValue(startIdx + i + 1); // NO (전체 인덱스 기준)
            row.createCell(1).setCellValue((String) item.get("txtPname")); // 품명
            row.createCell(2).setCellValue((String) item.get("psize"));    // 규격
            row.createCell(3).setCellValue((String) item.get("punit"));    // 단위
            row.createCell(4).setCellValue(((Number) item.get("pqty")).doubleValue()); // 수량
            row.createCell(5).setCellValue(((Number) item.get("puamt")).doubleValue()); // 단가
            row.createCell(6).setCellValue(((Number) item.get("pamt")).doubleValue()); // 금액
            row.createCell(7).setCellValue((String) item.get("remark")); // 비고
          }

          // 마지막 시트일 경우만 특이사항/기본정보 바인딩
          if (page == totalPages - 1) {
            // 날짜 바인딩
            dateStr = String.valueOf(baljuData.get("ichdate"));
            date = LocalDate.parse(dateStr);
            formatted = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            setCell(sheet, 2, 6, formatted); // G3

            setCell(sheet, 2, 1, (String) baljuData.get("CompanyName")); // 업체명
            setCell(sheet, 3, 1, (String) baljuData.get("actnm")); // 현장명
            setCell(sheet, 34, 1, (String) baljuData.get("remark01")); // 특이사항1
            setCell(sheet, 35, 1, (String) baljuData.get("remark02")); // 특이사항2
            setCell(sheet, 36, 1, (String) baljuData.get("remark03")); // 특이사항3
          }
        }


        workbook.write(fos);
      }

      // 파일 경로 반환
      return ResponseEntity.ok(Map.of(
          "success", true,
          "filePath", tempXlsx.toAbsolutePath().toString(),
          "fileName", fileName
      ));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(500).body(Map.of(
          "success", false,
          "message", e.getMessage()
      ));
    }
  }

  private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
    Row row = sheet.getRow(rowIdx);
    if (row == null) row = sheet.createRow(rowIdx);
    Cell cell = row.getCell(colIdx);
    if (cell == null) cell = row.createCell(colIdx);
    cell.setCellValue(value);
  }


}

