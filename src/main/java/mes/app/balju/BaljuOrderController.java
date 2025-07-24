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
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;

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

  @GetMapping("/bomList")
  public AjaxResult getBomList(@RequestParam(value = "projcet_no")String projcet_no){
    AjaxResult result = new AjaxResult();
    List<Map<String, Object>> bomList = baljuOrderService.getBomList(projcet_no);

      result.success = false;
      result.data = bomList;
      result.message = "BOM 정보가 없습니다.";
      return result;

  }

  // 외주발주서
  @PostMapping("/print/balJuPurchase")
  public ResponseEntity<Map<String, Object>> printPurchase(@RequestParam(value = "BALJUNUM") Integer baljunum,
                                                           Authentication auth) {
    try {
      User user = (User) auth.getPrincipal();

      Map<String, Object> baljuData = baljuOrderService.getBaljuDetail(baljunum);

      String spjangcd = user.getSpjangcd();
      Map<String, Object> clent = baljuOrderService.getxclent(spjangcd);

      String project_no = (String) baljuData.get("project_no");
      project_no = project_no.replaceAll("[\\\\/:*?\"<>|]", "");  // 특수문자 제거

      String fileName = String.format("_%s.xlsx", project_no);


      Path tempXlsx = Paths.get("C:/Temp/mes21/외주발주서/외주발주서" + fileName);
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
        // 병합: F5~H5 → row=4, col=5~7
        safeAddMergedRegion(sheet, new CellRangeAddress(4, 4, 5, 7));
       // sheet.addMergedRegion(new CellRangeAddress(4, 4, 5, 7));
        // F5 셀에 납기일자 값 설정
        setCell(sheet, 4, 5, formatted);

        //현장 pm -- 직위, 이름
        String pernm_rspcd = String.valueOf(baljuData.get("pernm_rspcd"));//K5~M5 qudgkq
        String pernm = String.valueOf(baljuData.get("pernm"));  //N5~O5
        // K5~M5 병합: row=4, col=10~12 (K=10, M=12)
        //sheet.addMergedRegion(new CellRangeAddress(4, 4, 10, 12));
        setCell(sheet, 4, 10, pernm_rspcd); // K5 위치에 값 설정
        // N5~O5 병합: row=4, col=13~14 (N=13, O=14)
       // sheet.addMergedRegion(new CellRangeAddress(4, 4, 13, 14));
        setCell(sheet, 4, 13, pernm); // N5 위치에 값 설정
        setCell(sheet, 4, 2, (String) baljuData.get("project_no")); // 프로젝트 no
        // 현장명, 주소
        //sheet.addMergedRegion(new CellRangeAddress(5, 5, 2, 5)); // row 5, col 2~5
        setCell(sheet, 5, 2, (String) baljuData.get("actnm"));

        //sheet.addMergedRegion(new CellRangeAddress(5, 5, 8, 15)); // I6~P6 병합
        setCell(sheet, 5, 8, (String) baljuData.get("actaddress"));

        //<발주자>
        //sheet.addMergedRegion(new CellRangeAddress(8, 8, 2, 3));  // C5~D5 병합
        setCell(sheet, 8, 2, (String) clent.get("spjangnm"));     // C9에 값 설정
       // sheet.addMergedRegion(new CellRangeAddress(8, 8, 5, 7));  // F9~H9 병합
        setCell(sheet, 8, 5, (String) clent.get("prenm"));        // F9에 값 설정
       // sheet.addMergedRegion(new CellRangeAddress(9, 9, 8, 10));
        setCell(sheet, 9,8 ,(String) clent.get("saupnum")); //I10~K10 사업장
       // sheet.addMergedRegion(new CellRangeAddress(8, 8, 13, 15));
        setCell(sheet, 8,13,(String) clent.get("tel1") );
       // sheet.addMergedRegion(new CellRangeAddress(9, 9, 2, 7));
        setCell(sheet, 9, 2, (String) clent.get("adresa") );
       // sheet.addMergedRegion(new CellRangeAddress(9,9,13,15));
        setCell(sheet, 9,13,(String) clent.get("fax") );
       // sheet.addMergedRegion(new CellRangeAddress(10,10,2,3));
        setCell(sheet, 10,2,(String) clent.get("emailadres") );

       /* setCell(sheet, 10,7,(String) clent.get("cltjik") );
        sheet.addMergedRegion(new CellRangeAddress(10,10,8,10));
        setCell(sheet, 10,8,(String) clent.get("cltpernm") );
        sheet.addMergedRegion(new CellRangeAddress(10,10,13,15));
        setCell(sheet, 10,13,(String) clent.get("clttelno") );*/
        //<수급자>
        // 업체명
       // sheet.addMergedRegion(new CellRangeAddress(13,13,2,3));
        setCell(sheet, 13, 2, (String) baljuData.get("CompanyName")); // C14~D14

        setCell(sheet, 15,7,(String) clent.get("cltjik") );
       // sheet.addMergedRegion(new CellRangeAddress(15,15,8,10));
        setCell(sheet, 15,8,(String) clent.get("cltpernm") );
       // sheet.addMergedRegion(new CellRangeAddress(15,15,13,15));
        setCell(sheet, 10,13,(String) clent.get("clttelno") );

        // 특이사항
      //  sheet.addMergedRegion(new CellRangeAddress(44, 44, 1, 15));
        setCell(sheet, 44, 1, (String) baljuData.get("remark01"));  //B45 ~ P45
      //  sheet.addMergedRegion(new CellRangeAddress(44, 44, 1, 15));
        setCell(sheet, 45, 1, (String) baljuData.get("remark02"));  // B46 ~ P46
       // sheet.addMergedRegion(new CellRangeAddress(46, 46, 1, 15));
        setCell(sheet, 46, 1, (String) baljuData.get("remark03"));  // B47 ~ P47

        // 품목 리스트 바인딩
        List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");
        int maxItemsPerPage = 24;
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

          // 서식 설정
          CellStyle numberStyle = workbook.createCellStyle();
          DataFormat format = workbook.createDataFormat();
          numberStyle.setDataFormat(format.getFormat("#,##0"));
          // 품목 바인딩 시작 행
          int startRow = 19;
          double totalPamt = 0;
          for (int i = 0; i < subItems.size(); i++) {
            Map<String, Object> item = subItems.get(i);
            int rowIdx = startRow + i;
            Row row = sheet.getRow(rowIdx);
            if (row == null) row = sheet.createRow(rowIdx);

            // 품명
            Cell pnameCell = row.getCell(1);
            if (pnameCell == null) pnameCell = row.createCell(1);
            pnameCell.setCellValue((String) item.get("txtPname"));

            // 규격
            Cell psizeCell = row.getCell(3);
            if (psizeCell == null) psizeCell = row.createCell(3);
            psizeCell.setCellValue((String) item.get("psize"));

            // 단위
            Cell punitCell = row.getCell(5);
            if (punitCell == null) punitCell = row.createCell(5);
            punitCell.setCellValue((String) item.get("punit"));

            // 수량
            Cell qtyCell = row.getCell(6);
            if (qtyCell == null) qtyCell = row.createCell(6);
            qtyCell.setCellValue(((Number) item.get("pqty")).doubleValue());

            // 단가
            Cell puamtCell = row.getCell(7);
            if (puamtCell == null) puamtCell = row.createCell(7);
            puamtCell.setCellValue(((Number) item.get("puamt")).doubleValue());

            // 금액 (J 컬럼 = 9번 셀)
            Cell pamtCell = row.getCell(9);
            if (pamtCell == null) pamtCell = row.createCell(9);
            // 기존 스타일 복사
            CellStyle originalStyle = sheet.getRow(19).getCell(9).getCellStyle(); // 템플릿의 스타일 복사
            pamtCell.setCellStyle(originalStyle);
            // 값 입력
            double pamt = ((Number) item.get("pamt")).doubleValue();
            pamtCell.setCellValue(pamt);

            // 도번
            Cell pmapseqCell = row.getCell(12);
            if (pmapseqCell == null) pmapseqCell = row.createCell(12);
            pmapseqCell.setCellValue((String) item.get("pmapseq"));

            // 비고
            Cell remarkCell = row.getCell(14);
            if (remarkCell == null) remarkCell = row.createCell(14);
            remarkCell.setCellValue((String) item.get("remark"));

            totalPamt += pamt;
          }


          // 마지막 시트일 경우만 특이사항/기본정보 바인딩
          if (page == totalPages - 1) {

             dateStr = String.valueOf(baljuData.get("BALJUDATE")); // 예: "20250724"
            // 형식 지정하여 파싱
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            date = LocalDate.parse(dateStr, inputFormatter);
            // 원하는 출력 형식으로 변환
            formatted = date.format(DateTimeFormatter.ofPattern("yyyy. MM. dd"));
            setCell(sheet, 51, 5, formatted); // 예: "2025. 07. 24"


            /* // 날짜 바인딩 (오늘 날짜 기준)
            formatted = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd"));
            setCell(sheet, 51, 5, formatted); // F52*/

            // J44 셀에 총 금액 합계 표시
            Row totalRow = sheet.getRow(43); // J44 = row 43, column 9
            if (totalRow == null) totalRow = sheet.createRow(43);

            Cell totalCell = totalRow.getCell(9);
            if (totalCell == null) totalCell = totalRow.createCell(9);

            // 스타일 복사 (19행 9열 금액 셀에서)
            CellStyle style = sheet.getRow(19).getCell(9).getCellStyle();
            totalCell.setCellStyle(style);

            totalCell.setCellValue(totalPamt);

          }
        }
        workbook.write(fos);

        if (Files.exists(tempXlsx)) {
        //log.info("✅ 발주서 파일이 성공적으로 생성되었습니다: {}", tempXlsx.toAbsolutePath());
        } else {
          log.warn("❌ 발주서 파일 생성 실패!");
        }

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
          try {
            Files.deleteIfExists(tempXlsx);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }, 5, TimeUnit.MINUTES);

      } catch (Exception e) {
        e.printStackTrace();
      }

      // 파일 경로 반환
      String encodedFileName = URLEncoder.encode("외주발주서_" + project_no + ".xlsx", StandardCharsets.UTF_8);
      String downloadUrl = "/baljuFile/" + encodedFileName;

      return ResponseEntity.ok(Map.of(
          "success", true,
          "downloadUrl", downloadUrl,
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
  // 클래스 내부에 정의할 메서드
  private void safeAddMergedRegion(Sheet sheet, CellRangeAddress region) {
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      if (sheet.getMergedRegion(i).formatAsString().equals(region.formatAsString())) {
        return; // 이미 병합되어 있음 → 아무것도 안 함
      }
    }
    sheet.addMergedRegion(region); // 병합 수행
  }


}

