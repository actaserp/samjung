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
          Authentication auth,
          HttpServletRequest request) {
    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";
    User user = (User) auth.getPrincipal();
    String spjangcd = user.getSpjangcd();
    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.baljuOrderService.getBaljuList(date_kind, start, end, spjangcd, CompanyName);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  @PostMapping("/multi_save")
  @Transactional
  public AjaxResult saveBaljuMulti(@RequestBody Map<String, Object> payload, Authentication auth) {

    User user = (User) auth.getPrincipal();

    AjaxResult result = new AjaxResult();

    try {
      Integer balJunum = CommonUtil.tryIntNull(payload.get("BALJUNUM"));
      String rawIchdate = (String) payload.get("ichdate");
      String ichdate = rawIchdate != null ? rawIchdate.replaceAll("-", "") : null;
      String spjangcd = user.getSpjangcd();
      String custcd = baljuOrderService.getCustcd(spjangcd);
      String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      TB_CA660 head;

      if (balJunum != null) {

        head = tb_ca660repository.findById(balJunum)
                .orElseThrow(() -> new RuntimeException("발주 헤더 없음"));

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

        // 기존 상세 삭제
        tb_ca661repository.deleteByBaljunum(balJunum);

      } else {

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
      }

      // ✅ 품목 저장
      List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

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
          detail.setG_no((String) item.get("g_no"));
          Object bomQty = item.get("bom_pqty");
          detail.setBomPqty(bomQty == null || bomQty.toString().isBlank()
                  ? null
                  : new java.math.BigDecimal(bomQty.toString().replace(",", "")));

          tb_ca661repository.save(detail);
        } catch (Exception e) {
          log.error("❌ 상세 저장 실패: {}", item, e);
          throw e; // 트랜잭션 롤백
        }
      }

      result.success = (true);
      result.message = ("발주 저장 완료");

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

    Integer BALJUNUM = head.getBalJunum();

    tb_ca661repository.deleteByBaljunum(BALJUNUM);

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

  // ===================================================================
  // 외주발주서 출력
  // ===================================================================
  @PostMapping("/print/balJuPurchase")
  public ResponseEntity<Map<String, Object>> printPurchase(@RequestParam(value = "BALJUNUM") Integer baljunum,
                                                           Authentication auth) {
    try {
      User user = (User) auth.getPrincipal();
      String userId = user.getUsername();
      String spjangcd = user.getSpjangcd();

      Map<String, Object> baljuData = baljuOrderService.getBaljuDetail(baljunum);
      Map<String, Object> clent = baljuOrderService.getxclent(spjangcd);
      Map<String, Object> userItem = baljuOrderService.getUserItem(userId);
      if (userItem == null || userItem.isEmpty()) {
        userItem = new HashMap<>();
      }

      String projectNo = String.valueOf(baljuData.get("project_no")).replaceAll("[\\\\/:*?\"<>|]", "");
      String fileName = String.format("_%s.xlsx", projectNo);
      Path tempXlsx = Paths.get("C:/Temp/mes21/외주발주서/외주발주서" + fileName);
      Files.createDirectories(tempXlsx.getParent());
      Files.deleteIfExists(tempXlsx);

      // ✅ 개수 분기 제거 — 항상 블록 복사 방식
      generateOutsourcingOrder(baljuData, clent, userItem, tempXlsx);

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

      String encodedXlsxFileName = URLEncoder.encode("외주발주서_" + projectNo + ".xlsx", StandardCharsets.UTF_8);
      String encodedPdfFileName = URLEncoder.encode("외주발주서_" + projectNo + ".pdf", StandardCharsets.UTF_8);
      String downloadUrl = "/baljuFile/" + encodedXlsxFileName;
      String pdfUrl = "/baljuFile/" + encodedPdfFileName;

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

  // 외주발주서: 단일 템플릿(외주발주서.xlsx, 품목 15개) + 초과 시 블록 복사
  private void generateOutsourcingOrder(Map<String, Object> baljuData,
                                        Map<String, Object> clent,
                                        Map<String, Object> userItem,
                                        Path tempXlsx) {
    String templatePath = "C:/Temp/mes21/문서/외주발주서.xlsx";

    // ▼▼ 외주발주서 15개 템플릿 기준 상수 (파일 확인 완료) ▼▼
    final int ITEMS_PER_PAGE = 15;   // 페이지당 품목
    final int BLOCK_ROWS     = 54;   // 한 블록 전체 행 수 (순번 19 → 73 → 127 = 54행 간격)
    final int ITEM_START     = 19;   // 품목 시작(0-based) = 20행
    final int TOTAL_REL      = 34;   // 합계행 (35행 - 1)
    final int REMARK_REL     = 35;   // 특기사항 입력 시작 (36행 - 1)
    final int BALJUDATE_REL  = 42;   // ✅ 발주일자 = 엑셀 43행 (F43)
    final int BALJUDATE_COL  = 5;    // F열
    // ▲▲▲▲

    try (FileInputStream fis = new FileInputStream(templatePath);
         Workbook workbook = new XSSFWorkbook(fis);
         FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

      Sheet sheet = workbook.getSheetAt(0);
      List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");

      int pageCount = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));

      // 1) 2페이지째부터 블록 복사
      for (int p = 1; p < pageCount; p++) {
        copyBlock(sheet, 0, BLOCK_ROWS - 1, p * BLOCK_ROWS);
      }

      // 2) 각 페이지: 공통정보 + 품목
      double totalPamt = 0;
      for (int p = 0; p < pageCount; p++) {
        int base = p * BLOCK_ROWS;

        bindCommonData(sheet, baljuData, clent, userItem, base);

        int from = p * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, items.size());
        List<Map<String, Object>> pageItems = items.subList(from, to);

        // 품목 (순번은 from+1 부터 이어서)
        totalPamt += bindItemRows(sheet, pageItems, base + ITEM_START, false, from + 1);

        // 발주일자 (각 페이지)
        setCell(sheet, base + BALJUDATE_REL, BALJUDATE_COL,
                formatYyyyMmDd(String.valueOf(baljuData.get("BALJUDATE"))));

        // 특기사항 (각 페이지)
        setCell(sheet, base + REMARK_REL,     1, String.valueOf(baljuData.get("remark01")));
        setCell(sheet, base + REMARK_REL + 1, 1, String.valueOf(baljuData.get("remark02")));
        setCell(sheet, base + REMARK_REL + 2, 1, String.valueOf(baljuData.get("remark03")));

        // 마지막 페이지에 합계
        if (to == items.size()) {
          Row totalRow = sheet.getRow(base + TOTAL_REL);
          if (totalRow == null) totalRow = sheet.createRow(base + TOTAL_REL);
          Cell totalCell = totalRow.getCell(9);
          if (totalCell == null) totalCell = totalRow.createCell(9);
          totalCell.setCellValue(totalPamt);

          CellStyle numberStyle = workbook.createCellStyle();
          numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
          totalCell.setCellStyle(numberStyle);
        }

        // 페이지 브레이크 (마지막 블록 제외)
        if (p < pageCount - 1) sheet.setRowBreak(base + BLOCK_ROWS - 1);
      }

      // 3) 인쇄영역 + 가로 1페이지 맞춤(세로는 페이지 나눔 허용)
      int endRow = pageCount * BLOCK_ROWS - 1;
      workbook.setPrintArea(workbook.getSheetIndex(sheet), 0, 16, 0, endRow);
      sheet.setFitToPage(true);
      PrintSetup ps = sheet.getPrintSetup();
      ps.setFitWidth((short) 1);
      ps.setFitHeight((short) 0);

      workbook.write(fos);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // ===================================================================
  // 구매품의서 출력
  // ===================================================================
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

      // ✅ 개수 분기 제거 — 항상 블록 복사 방식
      generatePurchaseOrder(baljuData, clent, userItem, tempXlsx);

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

      String encodedXlsxFileName = URLEncoder.encode("구매품의서_" + projectNo + ".xlsx", StandardCharsets.UTF_8);
      String encodedPdfFileName = URLEncoder.encode("구매품의서_" + projectNo + ".pdf", StandardCharsets.UTF_8);

      String downloadUrl = "/baljuFile/" + encodedXlsxFileName;
      String pdfUrl = "/baljuFile/" + encodedPdfFileName;

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

  // 구매품의서: 단일 템플릿(구매품의서.xlsx, 품목 15개) + 초과 시 블록 복사
  private void generatePurchaseOrder(Map<String, Object> baljuData,
                                     Map<String, Object> clent,
                                     Map<String, Object> userItem,
                                     Path tempXlsx) {
    String templatePath = "C:/Temp/mes21/문서/구매품의서.xlsx";

    final int ITEMS_PER_PAGE = 15;
    final int BLOCK_ROWS     = 58;   // print_area A1:Q58 기준 (2페이지 출력 후 검증)
    final int ITEM_START     = 19;   // 품목 시작 = 엑셀 20행
    final int TOTAL_REL      = 34;   // 합계 = 엑셀 35행
    final int REMARK_REL     = 35;   // 특기사항 = 엑셀 36행
    final int BALJUDATE_REL  = 42;   // ✅ 발주일자 = 엑셀 43행 (F43)
    final int BALJUDATE_COL  = 5;    // F열

    try (FileInputStream fis = new FileInputStream(templatePath);
         Workbook workbook = new XSSFWorkbook(fis);
         FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

      Sheet sheet = workbook.getSheetAt(0);
      List<Map<String, Object>> items = (List<Map<String, Object>>) baljuData.get("items");

      int pageCount = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));

      // 1) 2페이지째부터 블록 복사
      for (int p = 1; p < pageCount; p++) {
        copyBlock(sheet, 0, BLOCK_ROWS - 1, p * BLOCK_ROWS);
      }

      // 2) 각 페이지: 공통정보 + 품목
      double totalPamt = 0;
      for (int p = 0; p < pageCount; p++) {
        int base = p * BLOCK_ROWS;

        bindPurchaseCommonDataOffset(sheet, baljuData, clent, userItem, base);

        int from = p * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, items.size());
        List<Map<String, Object>> pageItems = items.subList(from, to);

        // 품목 (순번은 from+1 부터, 구매품의서는 isPurchaseForm=true)
        totalPamt += bindItemRows(sheet, pageItems, base + ITEM_START, true, from + 1);

        // 발주일자 (각 페이지)
        setCell(sheet, base + BALJUDATE_REL, BALJUDATE_COL,
                formatYyyyMmDd(String.valueOf(baljuData.get("BALJUDATE"))));

        // 특기사항 (각 페이지)
        setCell(sheet, base + REMARK_REL,     1, String.valueOf(baljuData.get("remark01")));
        setCell(sheet, base + REMARK_REL + 1, 1, String.valueOf(baljuData.get("remark02")));
        setCell(sheet, base + REMARK_REL + 2, 1, String.valueOf(baljuData.get("remark03")));

        // 마지막 페이지에 합계
        if (to == items.size()) {
          Row totalRow = sheet.getRow(base + TOTAL_REL);
          if (totalRow == null) totalRow = sheet.createRow(base + TOTAL_REL);
          Cell totalCell = totalRow.getCell(9);
          if (totalCell == null) totalCell = totalRow.createCell(9);
          totalCell.setCellValue(totalPamt);

          CellStyle numberStyle = workbook.createCellStyle();
          numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
          totalCell.setCellStyle(numberStyle);
        }

        // 페이지 브레이크 (마지막 블록 제외)
        if (p < pageCount - 1) sheet.setRowBreak(base + BLOCK_ROWS - 1);
      }

      // 3) 인쇄영역 + 가로 1페이지 맞춤(세로는 페이지 나눔 허용)
      int endRow = pageCount * BLOCK_ROWS - 1;
      workbook.setPrintArea(workbook.getSheetIndex(sheet), 0, 16, 0, endRow);
      sheet.setFitToPage(true);
      PrintSetup ps = sheet.getPrintSetup();
      ps.setFitWidth((short) 1);
      ps.setFitHeight((short) 0);

      workbook.write(fos);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // ===================================================================
  // 공통 헬퍼
  // ===================================================================

  /**
   * srcStartRow~srcEndRow(0-based) 블록을 destStartRow로 복제.
   * 셀 값/스타일/행높이 + 블록 내부 병합까지 복사.
   */
  private void copyBlock(Sheet sheet, int srcStartRow, int srcEndRow, int destStartRow) {
    int numRows = srcEndRow - srcStartRow + 1;

    for (int i = 0; i < numRows; i++) {
      Row src = sheet.getRow(srcStartRow + i);
      Row dest = sheet.getRow(destStartRow + i);
      if (dest == null) dest = sheet.createRow(destStartRow + i);
      if (src == null) continue;

      dest.setHeight(src.getHeight());

      short first = src.getFirstCellNum();
      short last  = src.getLastCellNum();
      for (int c = first; c < last; c++) {
        if (c < 0) continue;
        Cell sc = src.getCell(c);
        if (sc == null) continue;
        Cell dc = dest.getCell(c);
        if (dc == null) dc = dest.createCell(c);

        dc.setCellStyle(sc.getCellStyle());

        switch (sc.getCellType()) {
          case STRING:  dc.setCellValue(sc.getStringCellValue()); break;
          case NUMERIC: dc.setCellValue(sc.getNumericCellValue()); break;
          case BOOLEAN: dc.setCellValue(sc.getBooleanCellValue()); break;
          case FORMULA: dc.setCellFormula(sc.getCellFormula()); break;
          case BLANK:   dc.setBlank(); break;
          default: break;
        }
      }
    }

    // 블록 내부 병합 복제 (오프셋 이동)
    int offset = destStartRow - srcStartRow;
    int mergeCount = sheet.getNumMergedRegions();
    List<CellRangeAddress> toAdd = new ArrayList<>();
    for (int m = 0; m < mergeCount; m++) {
      CellRangeAddress r = sheet.getMergedRegion(m);
      if (r.getFirstRow() >= srcStartRow && r.getLastRow() <= srcEndRow) {
        toAdd.add(new CellRangeAddress(
                r.getFirstRow() + offset, r.getLastRow() + offset,
                r.getFirstColumn(), r.getLastColumn()));
      }
    }
    for (CellRangeAddress r : toAdd) safeAddMergedRegion(sheet, r);
  }

  // 외주발주서 공통정보 (offset = 블록 시작행)
  private void bindCommonData(Sheet sheet,
                              Map<String, Object> baljuData,
                              Map<String, Object> clent,
                              Map<String, Object> userItem,
                              int offset) {

    String dateStr = String.valueOf(baljuData.get("ichdate"));
    LocalDate date = LocalDate.parse(dateStr);
    String formatted = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    safeAddMergedRegion(sheet, new CellRangeAddress(offset + 4, offset + 4, 5, 7));
    setCell(sheet, offset + 4, 5, formatted);

    setCell(sheet, offset + 4, 10, String.valueOf(baljuData.get("pernm_rspcd")));
    setCell(sheet, offset + 4, 13, String.valueOf(baljuData.get("pernm")));
    setCell(sheet, offset + 4, 2, String.valueOf(baljuData.get("project_no")));
    setCell(sheet, offset + 5, 2, String.valueOf(baljuData.get("actnm")));
    setCell(sheet, offset + 5, 8, String.valueOf(baljuData.get("actaddress")));

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

  // 구매품의서 공통정보 (offset = 블록 시작행)
  private void bindPurchaseCommonDataOffset(Sheet sheet,
                                            Map<String, Object> baljuData,
                                            Map<String, Object> clent,
                                            Map<String, Object> userItem,
                                            int offset) {
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

  // 품목 바인딩 (startSeq: 이 페이지 첫 품목의 순번)
  private double bindItemRows(Sheet sheet, List<Map<String, Object>> items,
                              int startRow, boolean isPurchaseForm, int startSeq){
    double totalPamt = 0;

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

      // ✅ 순번 (A열=0) — 수식 대신 숫자값 직접 입력
      Cell seqCell = row.getCell(0);
      if (seqCell == null) seqCell = row.createCell(0);
      seqCell.setCellValue(startSeq + i);

      // 품명 (B열)
      Cell pnameCell = row.getCell(1);
      if (pnameCell == null) pnameCell = row.createCell(1);
      String pnameV = (String) item.get("txtPname");
      pnameCell.setCellValue(pnameV);

      // 규격 (D열)
      Cell psizeCell = row.getCell(3);
      if (psizeCell == null) psizeCell = row.createCell(3);
      String psizeV = (String) item.get("psize");
      psizeCell.setCellValue(psizeV);

      // 단위 (F열)
      Cell punitCell = row.getCell(5);
      if (punitCell == null) punitCell = row.createCell(5);
      punitCell.setCellValue((String) item.get("punit"));

      // 수량 (G열)
      Cell qtyCell = row.getCell(6);
      if (qtyCell == null) qtyCell = row.createCell(6);
      qtyCell.setCellValue(toDouble(item.get("pqty")));

      // 단가 (H열)
      Cell puamtCell = row.getCell(7);
      if (puamtCell == null) puamtCell = row.createCell(7);
      puamtCell.setCellValue(toDouble(item.get("puamt")));

      // 금액 (J열)
      double pamt = toDouble(item.get("pamt"));
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

      if (isPurchaseForm) {
        String pcode = (String) item.get("pcode");
        boolean isAddedMaterial = (pcode == null || pcode.isBlank());

        if (isAddedMaterial) {
          if (remark == null || remark.isBlank()) {
            remark = "※ 추가된 자재";
            remarkCell.setCellValue(remark);
          }
        }
        if ("이중 발주된 제품".equals(remark)) {
          Font redFont = sheet.getWorkbook().createFont();
          redFont.setColor(IndexedColors.RED.getIndex());

          CellStyle redStyle = sheet.getWorkbook().createCellStyle();
          redStyle.cloneStyleFrom(remarkCell.getCellStyle()); // 기존 서식 유지
          redStyle.setFont(redFont);
          redStyle.setWrapText(true);
          redStyle.setVerticalAlignment(VerticalAlignment.CENTER);

          remarkCell.setCellStyle(redStyle);
        }
      }

      // ✅ wrap + 행 높이 자동 조정 (비고 O:Q=3, 품명 B:C=2, 규격 D:E=2)
      applyWrapAndRowHeight(sheet, remarkCell, remark, 3);
      applyWrapAndRowHeight(sheet, pnameCell, pnameV, 2);
      applyWrapAndRowHeight(sheet, psizeCell, psizeV, 2);

      totalPamt += pamt;
    }

    return totalPamt;
  }

  /**
   * 숫자 값 안전 변환. null / 빈값 / 숫자가 아닌 값은 모두 0 으로 처리한다.
   * (DB 에 수량·단가·금액이 NULL 로 저장된 행 때문에 NPE 가 나던 것을 방지)
   */
  private double toDouble(Object value) {
    if (value == null) return 0d;
    if (value instanceof Number) return ((Number) value).doubleValue();
    String s = value.toString().replace(",", "").trim();
    if (s.isEmpty()) return 0d;
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return 0d;
    }
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

  private void safeAddMergedRegion(Sheet sheet, CellRangeAddress newRegion) {
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      CellRangeAddress existing = sheet.getMergedRegion(i);
      if (existing.intersects(newRegion)) {
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
      return "";
    }
  }

  /**
   * 병합 셀에 wrap text를 적용하고, 텍스트 길이에 맞춰 행 높이를 늘린다.
   *
   * @param sheet      시트
   * @param cell       대상 셀 (병합 영역의 좌상단 셀)
   * @param text       셀 값
   * @param mergedCols 병합된 열 개수 (비고 O:Q = 3, 품명 B:C = 2, 규격 D:E = 2)
   */
  private void applyWrapAndRowHeight(Sheet sheet, Cell cell, String text, int mergedCols) {
    if (cell == null) return;
    if (text == null) text = "";

    Workbook wb = sheet.getWorkbook();

    // 1) 기존 스타일 유지 + wrap 켜기 (세로 가운데 정렬)
    CellStyle base = cell.getCellStyle();
    CellStyle wrapStyle = wb.createCellStyle();
    wrapStyle.cloneStyleFrom(base);
    wrapStyle.setWrapText(true);
    wrapStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    cell.setCellStyle(wrapStyle);

    // 2) 병합된 열들의 실제 폭(문자 단위) 합산
    int colIdx = cell.getColumnIndex();
    double widthChars = 0;
    for (int c = 0; c < mergedCols; c++) {
      int wUnits = sheet.getColumnWidth(colIdx + c); // 1/256 문자폭 단위
      widthChars += wUnits / 256.0;
    }
    if (widthChars < 1) widthChars = 1;

    // 3) 한 줄에 담을 수 있는 폭 용량 (셀 여백 감안 약간 축소)
    double capacityPerLine = widthChars * 0.92;

    // 4) 수동 줄바꿈(\n) 반영하며 필요한 줄 수 계산 (한글 2배 폭 환산)
    int lines = 0;
    for (String part : text.split("\n", -1)) {
      double lineWidth = textWidth(part);
      lines += Math.max(1, (int) Math.ceil(lineWidth / capacityPerLine));
    }
    lines = Math.max(1, lines);

    // 5) 행 높이 지정 (8pt 폰트 → 한 줄 약 11pt, 최소 16.5pt, 최대 409pt)
    float lineHeightPt = 11f;
    float needed = Math.max(16.5f, lines * lineHeightPt);
    needed = Math.min(needed, 409f);   // 엑셀 행 높이 상한(409.5pt) 방어
    Row row = cell.getRow();
    if (needed > row.getHeightInPoints()) {
      row.setHeightInPoints(needed);
    }
  }

  /** 한글/전각 = 2, 그 외 = 1 로 문자열의 표시 폭 계산 */
  private double textWidth(String s) {
    if (s == null) return 0;
    double w = 0;
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if ((ch >= 0x1100 && ch <= 0x11FF)   // 한글 자모
              || (ch >= 0x3130 && ch <= 0x318F) // 호환 자모
              || (ch >= 0xAC00 && ch <= 0xD7A3) // 한글 음절
              || (ch >= 0xFF00 && ch <= 0xFFEF)) { // 전각
        w += 2;
      } else {
        w += 1;
      }
    }
    return w;
  }

  @PostMapping("/savePrice")
  public AjaxResult SaveUnitPrice(@RequestBody Map<String, Object> data){
    AjaxResult result = new AjaxResult();

    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      String baljuDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
      User user = (User) auth.getPrincipal();
      data.put("user_id", user.getId());
      data.put("BALJUDATE", baljuDate);

      this.baljuOrderService.SaveUnitPrice(data);

      result.success = true;

    } catch (Exception e) {
      result.success = false;
      result.message = "서버 오류: " + e.getMessage();
    }

    return result;
  }

  @GetMapping("/getUnitPrice")
  public AjaxResult getUnitPrice(@RequestParam(value = "partName") String partName,
                                 @RequestParam(value = "partSize") String partSize) {
    List<Map<String, Object>> items = this.baljuOrderService.getUnitPrice(partName, partSize);

    AjaxResult result = new AjaxResult();

    result.data = items;
    return result;
  }

}