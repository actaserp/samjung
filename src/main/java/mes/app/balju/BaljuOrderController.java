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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
      @RequestParam(value = "CompanyName", required = false) String CompanyName ,
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

    List<Map<String, Object>> items = this.baljuOrderService.getBaljuList(date_kind, start, end, spjangcd,CompanyName);

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
  public AjaxResult getBomList(@RequestParam(value = "project_no") String project_no,
                               @RequestParam(value = "eco_no") String eco_no) {
    log.info("bomlist 들어옴 :project_no:{},eco_no:{} ", project_no, eco_no);

    AjaxResult result = new AjaxResult();
    List<Map<String, Object>> bomList = baljuOrderService.getBomList(project_no, eco_no);

    if (bomList != null && !bomList.isEmpty()) {
      result.success = true;
      result.data = bomList;
    } else {
      result.success = false;
      result.message = "BOM 정보가 없습니다.";
    }

    return result;
  }

  // 외주발주서
  @PostMapping("/print/balJuPurchase")
  public ResponseEntity<Map<String, Object>> printPurchase(@RequestParam(value = "BALJUNUM") Integer baljunum,
                                                           Authentication auth) {
    try {
      User user = (User) auth.getPrincipal();
      String userId = user.getUsername();
      String spjangcd = user.getSpjangcd();

      // 📦 데이터 조회
      Map<String, Object> baljuData = baljuOrderService.getBaljuDetail(baljunum);
      Map<String, Object> clent = baljuOrderService.getxclent(spjangcd);
      Map<String, Object> userItem = baljuOrderService.getUserItem(userId);
      if (userItem == null || userItem.isEmpty()) {
        userItem = new HashMap<>();
      }

      List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");

      // 🔐 파일 경로 설정
      String projectNo = String.valueOf(baljuData.get("project_no")).replaceAll("[\\\\/:*?\"<>|]", "");
      String fileName = String.format("_%s.xlsx", projectNo);
      Path tempXlsx = Paths.get("C:/Temp/mes21/외주발주서/외주발주서" + fileName);
      Files.createDirectories(tempXlsx.getParent());
      Files.deleteIfExists(tempXlsx);

      // 📄 템플릿 선택
      if (items.size() > 24) {
        generateExcelTemplate2(baljuData, clent, userItem, tempXlsx);
      } else {
        generateExcelTemplate1(baljuData, clent, userItem, tempXlsx);
      }

      // 📄 PDF 변환
      Path tempPdf = Paths.get("C:/Temp/mes21/외주발주서/외주발주서_" + projectNo + ".pdf");
      ProcessBuilder pb = new ProcessBuilder(
          "C:/Program Files/LibreOffice/program/soffice.exe",
          "--headless",
          "--convert-to", "pdf",
          "--outdir", tempPdf.getParent().toString(),
          tempXlsx.toAbsolutePath().toString()
      );
      pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
      pb.redirectError(ProcessBuilder.Redirect.DISCARD);

      Process process = pb.start();
      process.waitFor();

      if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
        throw new IOException("PDF 변환 실패");
      }

      // 📤 다운로드 URL
      String encodedXlsxFileName = URLEncoder.encode("외주발주서_" + projectNo + ".xlsx", StandardCharsets.UTF_8);
      String encodedPdfFileName = URLEncoder.encode("외주발주서_" + projectNo + ".pdf", StandardCharsets.UTF_8);
      String downloadUrl = "/baljuFile/" + encodedXlsxFileName;
      String pdfUrl = "/baljuFile/" + encodedPdfFileName;

      // ⏱ 삭제 예약
      Executors.newSingleThreadScheduledExecutor().schedule(() -> {
        try { Files.deleteIfExists(tempXlsx); } catch (IOException e) { e.printStackTrace(); }
        try { Files.deleteIfExists(tempPdf); } catch (IOException e) { e.printStackTrace(); }
      }, 5, TimeUnit.MINUTES);

      return ResponseEntity.ok(Map.of(
          "success", true,
          "downloadUrl", downloadUrl,
          "pdfUrl", pdfUrl,
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

  //24개 이하
  private void generateExcelTemplate1(Map<String, Object> baljuData,
                                      Map<String, Object> clent,
                                      Map<String, Object> userItem,
                                      Path tempXlsx) {
    String templatePath = "C:/Temp/mes21/문서/외주발주서.xlsx";

    try (FileInputStream fis = new FileInputStream(templatePath);
         Workbook workbook = new XSSFWorkbook(fis);
         FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

      Sheet sheet = workbook.getSheetAt(0);

      // 날짜 바인딩
      String dateStr = String.valueOf(baljuData.get("ichdate")); // 예: "2025-07-19"
      LocalDate date = LocalDate.parse(dateStr);
      String formatted = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
      safeAddMergedRegion(sheet, new CellRangeAddress(4, 4, 5, 7));
      setCell(sheet, 4, 5, formatted);

      // 현장 pm
      setCell(sheet, 4, 10, String.valueOf(baljuData.get("pernm_rspcd")));
      setCell(sheet, 4, 13, String.valueOf(baljuData.get("pernm")));
      setCell(sheet, 4, 2, String.valueOf(baljuData.get("project_no")));
      setCell(sheet, 5, 2, String.valueOf(baljuData.get("actnm")));
      setCell(sheet, 5, 8, String.valueOf(baljuData.get("actaddress")));

      // 발주자 정보
      setCell(sheet, 8, 2, String.valueOf(clent.get("spjangnm")));
      setCell(sheet, 8, 5, String.valueOf(clent.get("prenm")));
      setCell(sheet, 9, 8, String.valueOf(clent.get("saupnum")));
      setCell(sheet, 8, 13, String.valueOf(clent.get("tel1")));
      setCell(sheet, 9, 2, String.valueOf(clent.get("adresa")));
      setCell(sheet, 9, 13, String.valueOf(clent.get("fax")));
      setCell(sheet, 10, 2, String.valueOf(clent.get("emailadres")));

      // 로그인 사용자 정보
      setCell(sheet, 10, 5, safeToString(userItem.get("divinm")));
      setCell(sheet, 10, 7, safeToString(userItem.get("RSPNM")));
      setCell(sheet, 10, 8, safeToString(userItem.get("pernm")));
      setCell(sheet, 10, 13, safeToString(userItem.get("handphone")));

      // 수급자
      setCell(sheet, 13, 2, String.valueOf(baljuData.get("CompanyName")));
      setCell(sheet, 15, 2, String.valueOf(baljuData.get("cltemail"))); //이메일
      setCell(sheet, 15, 7, String.valueOf(baljuData.get("cltjik"))); //수급자 직위
      setCell(sheet, 15, 8, String.valueOf(baljuData.get("cltpernm"))); //구급자 이름
      setCell(sheet, 15, 13, String.valueOf(baljuData.get("clttelno")));  //연락처

      // 특이사항
      setCell(sheet, 44, 1, String.valueOf(baljuData.get("remark01")));
      setCell(sheet, 45, 1, String.valueOf(baljuData.get("remark02")));
      setCell(sheet, 46, 1, String.valueOf(baljuData.get("remark03")));

      // 품목 리스트 바인딩
      List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");

      // 서식 설정
      CellStyle numberStyle = workbook.createCellStyle();
      DataFormat format = workbook.createDataFormat();
      numberStyle.setDataFormat(format.getFormat("#,##0"));
      // 품목 바인딩 시작 행
      int startRow = 19;
      double totalPamt = 0;
      for (int i = 0; i < items.size(); i++) {
        Map<String, Object> item = items.get(i);
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

      // 합계 및 발주일자
      setCell(sheet, 51, 5, formatYyyyMmDd(String.valueOf(baljuData.get("BALJUDATE"))));
      Row totalRow = sheet.getRow(43); // J44 = row 43, column 9
      if (totalRow == null) totalRow = sheet.createRow(43);
      Cell totalCell = totalRow.getCell(9);
      if (totalCell == null) totalCell = totalRow.createCell(9);

      totalCell.setCellValue(totalPamt);

      workbook.write(fos);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //25개 이상
  private void generateExcelTemplate2(Map<String, Object> baljuData,
                                      Map<String, Object> clent,
                                      Map<String, Object> userItem,
                                      Path tempXlsx) {
    String templatePath = "C:/Temp/mes21/문서/외주발주서2.xlsx";

    try (FileInputStream fis = new FileInputStream(templatePath);
         Workbook workbook = new XSSFWorkbook(fis);
         FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

      Sheet sheet = workbook.getSheetAt(0);

      List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");

      int itemsPerPage = 24;
      int startRowBase = 19;
      int pageOffset = 57; // 외주발주서 기준 A58 → A115 → A172 ...

      double totalPamt = 0;

      for (int page = 0; page * itemsPerPage < items.size(); page++) {
        int fromIndex = page * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, items.size());
        List<Map<String, Object>> pageItems = items.subList(fromIndex, toIndex);

        int startRow = startRowBase + page * pageOffset;

        // ✅ 공통 데이터 바인딩
        if (page == 0) {
          bindCommonData(sheet, baljuData, clent, userItem, 0); // 첫 페이지
        } else {
          bindCommonData(sheet, baljuData, clent, userItem, page * pageOffset);
        }

        // ✅ 품목 바인딩
        totalPamt += bindItemRows(sheet, pageItems, startRow, false);

        // ✅ 발주일자
        setCell(sheet, startRow + 32, 5, formatYyyyMmDd(String.valueOf(baljuData.get("BALJUDATE"))));

        // ✅ 특기사항 (페이지별)
        int remarkBaseRow = 102 + (page * pageOffset);
        setCell(sheet, remarkBaseRow, 1, String.valueOf(baljuData.get("remark01")));
        setCell(sheet, remarkBaseRow + 1, 1, String.valueOf(baljuData.get("remark02")));
        setCell(sheet, remarkBaseRow + 2, 1, String.valueOf(baljuData.get("remark03")));

        // ✅ 마지막 페이지에 합계
        if (toIndex == items.size()) {
          int totalRowNum = 43 + (page * pageOffset);  // J44, J101, J158 ...
          Row totalRow = sheet.getRow(totalRowNum);
          if (totalRow == null) totalRow = sheet.createRow(totalRowNum);

          Cell totalCell = totalRow.getCell(9); // J열
          if (totalCell == null) totalCell = totalRow.createCell(9);

          totalCell.setCellValue(totalPamt);

          CellStyle numberStyle = workbook.createCellStyle();
          numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
          totalCell.setCellStyle(numberStyle);
        }

      }

      // ✅ 인쇄 영역 제한 (불필요한 빈 페이지 제거)
      int usedPageCount = (int) Math.ceil((double) items.size() / itemsPerPage);
      int endRow = (usedPageCount * pageOffset) - 1;

      workbook.setPrintArea(
          workbook.getSheetIndex(sheet),
          0, 16, // A~Q
          0, endRow
      );

      workbook.write(fos);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void bindCommonData(Sheet sheet,
                              Map<String, Object> baljuData,
                              Map<String, Object> clent,
                              Map<String, Object> userItem,
                              int offset) {

    // 날짜
    String dateStr = String.valueOf(baljuData.get("ichdate")); // 예: "2025-07-19"
    LocalDate date = LocalDate.parse(dateStr);
    String formatted = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    safeAddMergedRegion(sheet, new CellRangeAddress(offset + 4, offset + 4, 5, 7));
    setCell(sheet, offset + 4, 5, formatted);

    // 현장 pm
    setCell(sheet, offset + 4, 10, String.valueOf(baljuData.get("pernm_rspcd")));
    setCell(sheet, offset + 4, 13, String.valueOf(baljuData.get("pernm")));
    setCell(sheet, offset + 4, 2, String.valueOf(baljuData.get("project_no")));
    setCell(sheet, offset + 5, 2, String.valueOf(baljuData.get("actnm")));
    setCell(sheet, offset + 5, 8, String.valueOf(baljuData.get("actaddress")));

    // 발주자 정보
    setCell(sheet, offset + 8, 2, String.valueOf(clent.get("spjangnm")));
    setCell(sheet, offset + 8, 5, String.valueOf(clent.get("prenm")));
    setCell(sheet, offset + 9, 8, String.valueOf(clent.get("saupnum")));
    setCell(sheet, offset + 8, 13, String.valueOf(clent.get("tel1")));
    setCell(sheet, offset + 9, 2, String.valueOf(clent.get("adresa")));
    setCell(sheet, offset + 9, 13, String.valueOf(clent.get("fax")));
    setCell(sheet, offset + 10, 2, String.valueOf(clent.get("emailadres")));

    // 로그인 사용자 정보
    setCellIfNotBlank(sheet, offset + 10, 5, userItem.get("divinm"));
    setCellIfNotBlank(sheet, offset + 10, 7, userItem.get("RSPNM"));
    setCellIfNotBlank(sheet, offset + 10, 8, userItem.get("pernm"));
    setCellIfNotBlank(sheet, offset + 10, 13, userItem.get("handphone"));

    // 수급자
    setCell(sheet, offset + 13, 2, String.valueOf(baljuData.get("CompanyName")));
    setCell(sheet, offset + 15, 2, String.valueOf(baljuData.get("cltemail"))); //이메일
    setCell(sheet, offset + 15, 7, String.valueOf(baljuData.get("cltjik")));    //수급자 직위
    setCell(sheet, offset + 15, 8, String.valueOf(baljuData.get("cltpernm")));  //구급자 이름
    setCell(sheet, offset + 15, 13, String.valueOf(baljuData.get("clttelno"))); //연락처

    //특기 사항
    setCell(sheet, 101, 1, String.valueOf(baljuData.get("remark01")));
    setCell(sheet, 102, 1, String.valueOf(baljuData.get("remark02")));
    setCell(sheet, 103, 1, String.valueOf(baljuData.get("remark03")));

  }

  private double bindItemRows(Sheet sheet, List<Map<String, Object>> items, int startRow, boolean isPurchaseForm){
    double totalPamt = 0;

    // 기준 스타일 복사 (J20 → row 19, col 9 기준)
    Row templateRow = sheet.getRow(19);
    CellStyle originalStyle = null;
    if (templateRow != null && templateRow.getCell(9) != null) {
      originalStyle = templateRow.getCell(9).getCellStyle();
    }

    for (int i = 0; i < items.size(); i++) {
      Map<String, Object> item = items.get(i);
      int rowIdx = startRow + i;
      Row row = sheet.getRow(rowIdx);
      if (row == null) row = sheet.createRow(rowIdx);

      // 품명 (B열)
      Cell pnameCell = row.getCell(1);
      if (pnameCell == null) pnameCell = row.createCell(1);
      pnameCell.setCellValue((String) item.get("txtPname"));

      // 규격 (D열)
      Cell psizeCell = row.getCell(3);
      if (psizeCell == null) psizeCell = row.createCell(3);
      psizeCell.setCellValue((String) item.get("psize"));

      // 단위 (F열)
      Cell punitCell = row.getCell(5);
      if (punitCell == null) punitCell = row.createCell(5);
      punitCell.setCellValue((String) item.get("punit"));

      // 수량 (G열)
      Cell qtyCell = row.getCell(6);
      if (qtyCell == null) qtyCell = row.createCell(6);
      qtyCell.setCellValue(((Number) item.get("pqty")).doubleValue());

      // 단가 (H열)
      Cell puamtCell = row.getCell(7);
      if (puamtCell == null) puamtCell = row.createCell(7);
      puamtCell.setCellValue(((Number) item.get("puamt")).doubleValue());

      // 금액 (J열)
      double pamt = ((Number) item.get("pamt")).doubleValue();
      Cell pamtCell = row.getCell(9);
      if (pamtCell == null) pamtCell = row.createCell(9);
      pamtCell.setCellValue(pamt);
      if (originalStyle != null) {
        pamtCell.setCellStyle(originalStyle);
      }

      // 도번 (M열)
      Cell pmapseqCell = row.getCell(12);
      if (pmapseqCell == null) pmapseqCell = row.createCell(12);
      pmapseqCell.setCellValue((String) item.get("pmapseq"));

      // 비고 (O열)
      Cell remarkCell = row.getCell(14);
      if (remarkCell == null) remarkCell = row.createCell(14);

      String remark = (String) item.get("remark");
      remarkCell.setCellValue(remark);

      //"추가된 자재" 여부 판단 (pcode만 기준)
      if (isPurchaseForm) {
        String pcode = (String) item.get("pcode");
        boolean isAddedMaterial = (pcode == null || pcode.isBlank());

        if (isAddedMaterial) {
          // 비고가 비어있으면 자동 문구 추가도 가능
          if (remark == null || remark.isBlank()) {
            remarkCell.setCellValue("※ 추가된 자재");
          }
        }
        if ("이중 발주된 제품".equals(remark)) {
          Font redFont = sheet.getWorkbook().createFont();
          redFont.setColor(IndexedColors.RED.getIndex());

          CellStyle redStyle = sheet.getWorkbook().createCellStyle();
          redStyle.setFont(redFont);

          remarkCell.setCellStyle(redStyle); // 🔁 덮어쓰기
        }
      }

      totalPamt += pamt;
    }

    return totalPamt;
  }


  // 구매품의서
  @PostMapping("/print/balJuPrinted")
  public ResponseEntity<Map<String, Object>> balJuPrinted(@RequestParam(value = "BALJUNUM") Integer baljunum,
                                                          Authentication auth) {
    try {
      User user = (User) auth.getPrincipal();
      String userId = user.getUsername();
      Map<String, Object> baljuData = baljuOrderService.getBaljuDetail(baljunum);
      Map<String, Object> clent = baljuOrderService.getxclent(user.getSpjangcd());
      Map<String, Object> userItem = baljuOrderService.getUserItem(userId);
      if (userItem == null || userItem.isEmpty()) {
        userItem = new HashMap<>();
      }

      String projectNo = String.valueOf(baljuData.get("project_no")).replaceAll("[\\\\/:*?\"<>|]", "");
      String fileName = String.format("_%s.xlsx", projectNo);
      Path tempXlsx = Paths.get("C:/Temp/mes21/구매품의서/구매품의서" + fileName);
      Files.createDirectories(tempXlsx.getParent());
      Files.deleteIfExists(tempXlsx);

      List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");
      if (items.size() > 24) {
        generatePurchaseTemplate2(baljuData, clent, userItem, tempXlsx);
      } else {
        generatePurchaseTemplate1(baljuData, clent, userItem, tempXlsx);
      }

      // ⬇️ PDF 변환
      Path tempPdf = Paths.get("C:/Temp/mes21/구매품의서/구매품의서_" + projectNo + ".pdf");

      ProcessBuilder pb = new ProcessBuilder(
          "C:/Program Files/LibreOffice/program/soffice.exe",
          "--headless",
          "--convert-to", "pdf",
          "--outdir", tempPdf.getParent().toString(),
          tempXlsx.toAbsolutePath().toString()
      );
      pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
      pb.redirectError(ProcessBuilder.Redirect.DISCARD);

      Process process = pb.start();
      process.waitFor();

      if (!Files.exists(tempPdf) || Files.size(tempPdf) == 0) {
        throw new IOException("PDF 변환 실패");
      }

      // ⬇️ URL 인코딩
      String encodedXlsxFileName = URLEncoder.encode("구매품의서_" + projectNo + ".xlsx", StandardCharsets.UTF_8);
      String encodedPdfFileName = URLEncoder.encode("구매품의서_" + projectNo + ".pdf", StandardCharsets.UTF_8);

      String downloadUrl = "/baljuFile/" + encodedXlsxFileName;
      String pdfUrl = "/baljuFile/" + encodedPdfFileName;

      // ⬇️ 삭제 예약
      Executors.newSingleThreadScheduledExecutor().schedule(() -> {
        try { Files.deleteIfExists(tempXlsx); } catch (IOException e) { e.printStackTrace(); }
        try { Files.deleteIfExists(tempPdf); } catch (IOException e) { e.printStackTrace(); }
      }, 5, TimeUnit.MINUTES);

      return ResponseEntity.ok(Map.of(
          "success", true,
          "downloadUrl", downloadUrl,   // 엑셀 다운로드용
          "pdfUrl", pdfUrl,             // PDF 미리보기/다운로드용
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

  private void generatePurchaseTemplate2(Map<String, Object> baljuData,
                                         Map<String, Object> clent,
                                         Map<String, Object> userItem,
                                         Path tempXlsx) {
    String templatePath = "C:/Temp/mes21/문서/구매품의서2.xlsx";

    try (FileInputStream fis = new FileInputStream(templatePath);
         Workbook workbook = new XSSFWorkbook(fis);
         FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

      Sheet sheet = workbook.getSheetAt(0);

      // 품목 리스트 및 페이징 기준
      List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");
      int itemsPerPage = 24;
      int startRowBase = 19;   // 첫 페이지의 품목 시작 행 (B20)
      int pageOffset = 58;     // 페이지당 오프셋 (A59 → A117 → A175 → A233 -> ...)

      double totalPamt = 0;

      for (int page = 0; page * itemsPerPage < items.size(); page++) {
        int fromIndex = page * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, items.size());
        List<Map<String, Object>> pageItems = items.subList(fromIndex, toIndex);

        int startRow = startRowBase + page * pageOffset;

        // 품목 출력
        totalPamt += bindItemRows(sheet, pageItems, startRow, true);

        // 페이지별 공통 데이터 출력
        if (page == 0) {
          bindPurchaseCommonData(sheet, baljuData, clent, userItem); // 1페이지
        } else {
          bindPurchaseCommonDataOffset(sheet, baljuData, clent, userItem, page * pageOffset);
        }

        // 발주일자 셀 위치: startRow + 32행 기준 (예: 51행, 109행, 166행 ...)
        setCell(sheet, startRow + 32, 6, formatYyyyMmDd(String.valueOf(baljuData.get("BALJUDATE"))));

        // ✅ 특기사항 반복 적용 (remark01~03)
        int remarkBaseRow = 102 + (page * pageOffset);
        setCell(sheet, remarkBaseRow, 1, String.valueOf(baljuData.get("remark01")));
        setCell(sheet, remarkBaseRow + 1, 1, String.valueOf(baljuData.get("remark02")));
        setCell(sheet, remarkBaseRow + 2, 1, String.valueOf(baljuData.get("remark03")));


        if (toIndex == items.size()) {
          int totalRowNum = 43 + (page * pageOffset);  // 예: page=0 → 43 (J44), page=1 → 43+58=101 (J102), page=2 → 43+116=159 (J160)
          Row totalRow = sheet.getRow(totalRowNum);
          if (totalRow == null) totalRow = sheet.createRow(totalRowNum);

          Cell totalCell = totalRow.getCell(9); // J열 (인덱스 9)
          if (totalCell == null) totalCell = totalRow.createCell(9);

          totalCell.setCellValue(totalPamt);

          CellStyle numberStyle = workbook.createCellStyle();
          numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
          totalCell.setCellStyle(numberStyle);
        }

      }

      int usedPageCount = (int) Math.ceil((double) items.size() / itemsPerPage);
      int endRow = (usedPageCount * pageOffset) - 1;  // A58 기준 구조

      workbook.setPrintArea(
          workbook.getSheetIndex(sheet),
          0, 16,     // 열: A ~ Q
          0, endRow  // 행: A1 ~ A{끝행}
      );

      workbook.write(fos);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void bindPurchaseCommonDataOffset(Sheet sheet,
                                            Map<String, Object> baljuData,
                                            Map<String, Object> clent,
                                            Map<String, Object> userItem,
                                            int offset) {
    // 행 번호에 offset만 추가해서 처리 (한 줄씩 위로 조정됨)
    String dateStr = String.valueOf(baljuData.get("ichdate"));
    LocalDate date = LocalDate.parse(dateStr);
    String formatted = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));

    safeAddMergedRegion(sheet, new CellRangeAddress(offset + 4, offset + 4, 5, 7));
    setCell(sheet, offset + 4, 5, formatted);

    setCell(sheet, offset + 4, 4, String.valueOf(baljuData.get("pernm_rspcd")));
    setCell(sheet, offset + 5, 7, String.valueOf(baljuData.get("pernm")));
    setCell(sheet, offset + 4, 2, String.valueOf(baljuData.get("project_no")));
    setCell(sheet, offset + 5, 2, String.valueOf(baljuData.get("actnm")));

    setCell(sheet, offset + 8, 2, String.valueOf(clent.get("spjangnm")));
    setCell(sheet, offset + 8, 5, String.valueOf(clent.get("prenm")));
    setCell(sheet, offset + 9, 8, String.valueOf(clent.get("saupnum")));
    setCell(sheet, offset + 8, 13, String.valueOf(clent.get("tel1")));
    setCell(sheet, offset + 9, 2, String.valueOf(clent.get("adresa")));
    setCell(sheet, offset + 9, 13, String.valueOf(clent.get("fax")));
    setCell(sheet, offset + 10, 2, String.valueOf(clent.get("emailadres")));

    setCellIfNotBlank(sheet, offset + 10, 5, userItem.get("divinm"));
    setCellIfNotBlank(sheet, offset + 10, 7, userItem.get("RSPNM"));
    setCellIfNotBlank(sheet, offset + 10, 8, userItem.get("pernm"));
    setCellIfNotBlank(sheet, offset + 10, 13, userItem.get("handphone"));

    setCell(sheet, offset + 13, 2, String.valueOf(baljuData.get("CompanyName")));
    setCell(sheet, offset + 15, 2, String.valueOf(baljuData.get("cltemail")));
    setCell(sheet, offset + 15, 7, String.valueOf(baljuData.get("cltjik")));
    setCell(sheet, offset + 15, 8, String.valueOf(baljuData.get("cltpernm")));
    setCell(sheet, offset + 15, 13, String.valueOf(baljuData.get("clttelno")));
  }

  private void generatePurchaseTemplate1(Map<String, Object> baljuData,
                                         Map<String, Object> clent,
                                         Map<String, Object> userItem,
                                         Path tempXlsx) {
    String templatePath = "C:/Temp/mes21/문서/구매품의서.xlsx";

    try (FileInputStream fis = new FileInputStream(templatePath);
         Workbook workbook = new XSSFWorkbook(fis);
         FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

      Sheet sheet = workbook.getSheetAt(0);

      // 공통 데이터 바인딩
      bindPurchaseCommonData(sheet, baljuData, clent, userItem);

      // 특이사항 바인딩
      bindRemarks(sheet, baljuData);

      // 품목 바인딩 및 총합 계산
      List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");
      double totalPamt = bindItemRows(sheet, items, 19,true);

      // 발주일자
      setCell(sheet, 51, 5, formatYyyyMmDd(String.valueOf(baljuData.get("BALJUDATE"))));

      // 합계 금액 바인딩 (J44 = row 43, column 9)
      Row totalRow = sheet.getRow(43);
      if (totalRow == null) totalRow = sheet.createRow(43);
      Cell totalCell = totalRow.getCell(9);
      if (totalCell == null) totalCell = totalRow.createCell(9);
      totalCell.setCellValue(totalPamt);

      // 숫자 포맷 적용 (#,##0)
      CellStyle numberStyle = workbook.createCellStyle();
      numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
      totalCell.setCellStyle(numberStyle);

      workbook.write(fos);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void bindPurchaseCommonData(Sheet sheet,
                                      Map<String, Object> baljuData,
                                      Map<String, Object> clent,
                                      Map<String, Object> userItem) {

    // 날짜
    String dateStr = String.valueOf(baljuData.get("ichdate"));
    LocalDate date = LocalDate.parse(dateStr);
    String formatted = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    safeAddMergedRegion(sheet, new CellRangeAddress(4, 4, 5, 7));
    setCell(sheet, 4, 5, formatted);

    // 현장 pm
    setCell(sheet, 4, 4, String.valueOf(baljuData.get("pernm_rspcd")));  // 직위
    setCell(sheet, 5, 7, String.valueOf(baljuData.get("pernm")));        // 이름
    setCell(sheet, 4, 2, String.valueOf(baljuData.get("project_no")));
    setCell(sheet, 5, 2, String.valueOf(baljuData.get("actnm")));

    // 발주자
    setCell(sheet, 8, 2, String.valueOf(clent.get("spjangnm")));
    setCell(sheet, 8, 5, String.valueOf(clent.get("prenm")));
    setCell(sheet, 9, 8, String.valueOf(clent.get("saupnum")));
    setCell(sheet, 8, 13, String.valueOf(clent.get("tel1")));
    setCell(sheet, 9, 2, String.valueOf(clent.get("adresa")));
    setCell(sheet, 9, 13, String.valueOf(clent.get("fax")));
    setCell(sheet, 10, 2, String.valueOf(clent.get("emailadres")));

    // 로그인 사용자
    setCell(sheet, 10, 5, safeToString(userItem.get("divinm")));
    setCell(sheet, 10, 7, safeToString(userItem.get("RSPNM")));
    setCell(sheet, 10, 8, safeToString(userItem.get("pernm")));
    setCell(sheet, 10, 13, safeToString(userItem.get("handphone")));


    // 수급자
    setCell(sheet, 13, 2, String.valueOf(baljuData.get("CompanyName")));
    setCell(sheet, 15, 2, String.valueOf(baljuData.get("cltemail")));
    setCell(sheet, 15, 7, String.valueOf(baljuData.get("cltjik")));
    setCell(sheet, 15, 8, String.valueOf(baljuData.get("cltpernm")));
    setCell(sheet, 15, 13, String.valueOf(baljuData.get("clttelno")));
  }

  private void bindRemarks(Sheet sheet, Map<String, Object> baljuData) {
    setCell(sheet, 44, 1, String.valueOf(baljuData.get("remark01")));
    setCell(sheet, 45, 1, String.valueOf(baljuData.get("remark02")));
    setCell(sheet, 46, 1, String.valueOf(baljuData.get("remark03")));
  }
  private String safeToString(Object obj) {
    return (obj != null) ? obj.toString() : "";
  }


  private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
    Row row = sheet.getRow(rowIdx);
    if (row == null) row = sheet.createRow(rowIdx);
    Cell cell = row.getCell(colIdx);
    if (cell == null) cell = row.createCell(colIdx);
    cell.setCellValue(value);
  }
  private void setCellIfNotBlank(Sheet sheet, int rowIdx, int colIdx, Object value) {
    if (value != null && !String.valueOf(value).isBlank()) {
      setCell(sheet, rowIdx, colIdx, String.valueOf(value));
    }
  }

  // 클래스 내부에 정의할 메서드
  private void safeAddMergedRegion(Sheet sheet, CellRangeAddress newRegion) {
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      CellRangeAddress existing = sheet.getMergedRegion(i);
      if (existing.intersects(newRegion)) {
        // 이미 겹치는 병합 영역이 있으면 추가하지 않음
        return;
      }
    }
    sheet.addMergedRegion(newRegion);
  }

  private String formatYyyyMmDd(String dateStr) {
    try {
      DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
      LocalDate date = LocalDate.parse(dateStr, inputFormatter);
      return date.format(DateTimeFormatter.ofPattern("yyyy. MM. dd"));
    } catch (Exception e) {
      return ""; // 파싱 실패 시 빈 문자열 반환 (또는 적절한 예외 처리)
    }
  }

  @PostMapping("savePrice")
  public AjaxResult SaveUnitPrice(@RequestBody Map<String, Object> data){
    AjaxResult result = new AjaxResult();

    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      String baljuDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      User user = (User) auth.getPrincipal();
      data.put("user_id", user.getId());
      data.put("BALJUDATE", baljuDate);

      int saveCount = this.baljuOrderService.SaveUnitPrice(data);

      if (saveCount > 0) {
        result.success = true;
      } else {
        result.success = false;
        result.message = "저장 실패: 중복된 데이터이거나 입력값이 올바르지 않습니다.";
      }
    }catch (Exception e) {
      result.success = false;
      result.message = "서버 오류: " + e.getMessage();
    }

    return result;
  }

  @GetMapping("/getUnitPrice")
  public AjaxResult getUnitPrice(@RequestParam(value = "partName") String partName,
                                 @RequestParam(value = "partSize") String partSize) {
//    log.info("단가 조회 :partName:{},partSize:{}",partName, partSize);
    List<Map<String, Object>> items = this.baljuOrderService.getUnitPrice(partName, partSize);

    AjaxResult result = new AjaxResult();

    result.data = items;
    return result;
  }

}

