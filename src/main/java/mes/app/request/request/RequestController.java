package mes.app.request.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mes.app.request.request.service.RequestService;
import mes.config.Settings;
import mes.domain.entity.User;
import mes.domain.entity.actasEntity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.actasRepository.TB_DA006WFILERepository;
import mes.domain.repository.actasRepository.TB_DA006WRepository;
import mes.domain.repository.actasRepository.TB_DA007WRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/request/request")
public class RequestController {
    @Autowired
    private RequestService requestService;


    @Autowired
    private TB_DA007WRepository tbda007WRepository;

    @Autowired
    private TB_DA006WRepository tbda006WRepository;

    @Autowired
    private TB_DA006WFILERepository tbDa006WFILERepository;

    @Autowired
    Settings settings;
    // 주문출고 그리드 read
    @GetMapping("/order_read")
    public AjaxResult getOrderList(@RequestParam(value = "search_startDate", required = false) String searchStartDate,
                                   @RequestParam(value = "search_endDate", required = false) String searchEndDate,
                                   @RequestParam(value = "search_remark", required = false) String searchRemark,
                                   @RequestParam(value = "search_chulflag", required = false) String searchChulflag,
                                   @RequestParam(value = "saupnumHidden", required = false) String saupnumHidden,
                                   @RequestParam(value = "saupnum", required = false) String Saupnum,
                                   @RequestParam(value = "search_comnm", required = false) String searchComnm,
                                   @RequestParam(value = "spjangcd", required = false) String spjangcd,
                                   Authentication auth) {
        String username = (Saupnum != null && !Saupnum.isEmpty()) ? Saupnum : "";
        String saupnum;
        String cltcd;
        // username이 없을경우/session삭제후 진입(관리자 진입)
        if (username.isEmpty()) {
            username = (saupnumHidden != null && !saupnumHidden.isEmpty()) ? saupnumHidden : "";
        }
        if (username.isEmpty()) {
            User user = (User) auth.getPrincipal();
            username = user.getUsername();
        }

        Map<String, Object> userInfo = requestService.getUserInfo(username);

        // 관리자 진입시 사업자 번호 없이 업체명(searchComnm)만으로 조회
        if(userInfo == null) {
            saupnum = null;
            cltcd = null;
        } else{
            saupnum = (String) userInfo.get("saupnum");
            cltcd = (String) userInfo.get("cltcd");
        }
        String search_startDate = (searchStartDate).replaceAll("-","");
        String search_endDate = (searchEndDate).replaceAll("-","");
        List<Map<String, Object>> items = this.requestService.getOrderList(spjangcd,
                search_startDate, search_endDate, searchRemark, searchChulflag, saupnum, cltcd, searchComnm);
//        Map<String, Object> headitem = this.requestService.getHeadList(tbDa006WPk);
//        items.add(headitem);
        for (Map<String, Object> item : items) {
            // 날짜 형식 변환 (reqdate)
            if (item.containsKey("CHULDATE")) {
                String setupdt = (String) item.get("CHULDATE");
                if (setupdt != null && setupdt.length() == 8) {
                    String formattedDate = setupdt.substring(0, 4) + "-" + setupdt.substring(4, 6) + "-" + setupdt.substring(6, 8);
                    item.put("CHULDATE", formattedDate);
                }
            }
        }

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @PostMapping("/downloader")
    public ResponseEntity<?> downloadFile(@RequestBody List<Map<String, Object>> reqnums) throws IOException {

        // 파일 목록과 파일 이름을 담을 리스트 초기화
        List<File> filesToDownload = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        // ZIP 파일 이름을 설정할 변수 초기화
        String tketcrdtm = null;
        String tketnm = null;

        // 파일을 메모리에 쓰기
        for (Map<String, Object> reqnum : reqnums) {
            // 다운로드 위한 파일 정보 조회
            List<Map<String, Object>> fileList = requestService.download(reqnum);

            for (Map<String, Object> fileInfo : fileList) {
                String filePath = (String) fileInfo.get("filepath");    // 파일 경로
                String fileName = (String) fileInfo.get("filesvnm");    // 파일 이름(uuid)
                String originFileName = (String) fileInfo.get("fileornm");  //파일 원본이름(origin Name)

                if (tketcrdtm == null) {
                    tketcrdtm = (String) fileInfo.get("reqdate");
                }
                if (tketnm == null) {
                    tketnm = "주문등록";
                }

                File file = new File(filePath + File.separator + fileName);

                // 파일이 실제로 존재하는지 확인
                if (file.exists()) {
                    filesToDownload.add(file);
                    fileNames.add(originFileName); // 다운로드 받을 파일 이름을 originFileName으로 설정
                }
            }
        }

        // 파일이 없는 경우
        if (filesToDownload.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 파일이 하나인 경우 그 파일을 바로 다운로드
        if (filesToDownload.size() == 1) {
            File file = filesToDownload.get(0);
            String originFileName = fileNames.get(0); // originFileName 가져오기

            HttpHeaders headers = new HttpHeaders();
            String encodedFileName = URLEncoder.encode(originFileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=*''" + encodedFileName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(file.length());

            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(file.toPath()));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        }

        String zipFileName = (tketcrdtm != null && tketnm != null) ? tketcrdtm + "_" + tketnm + ".zip" : "download.zip";

        // 파일이 두 개 이상인 경우 ZIP 파일로 묶어서 다운로드
        ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(zipBaos)) {

            Set<String> addedFileNames = new HashSet<>(); // 이미 추가된 파일 이름을 저장할 Set
            int fileCount = 1;

            for (int i = 0; i < filesToDownload.size(); i++) {
                File file = filesToDownload.get(i);
                String originFileName = fileNames.get(i); // originFileName 가져오기

                // 파일 이름이 중복될 경우 숫자를 붙여 고유한 이름으로 만듦
                String uniqueFileName = originFileName;
                while (addedFileNames.contains(uniqueFileName)) {
                    uniqueFileName = originFileName.replace(".", "_" + fileCount++ + ".");
                }

                // 고유한 파일 이름을 Set에 추가
                addedFileNames.add(uniqueFileName);

                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(originFileName);
                    zipOut.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, len);
                    }

                    zipOut.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            }

            zipOut.finish();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        ByteArrayResource zipResource = new ByteArrayResource(zipBaos.toByteArray());

        HttpHeaders headers = new HttpHeaders();
        String encodedZipFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=*''" + encodedZipFileName);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(zipResource.contentLength());

        return ResponseEntity.ok()
                .headers(headers)
                .body(zipResource);
    }
    //제품구성 리스트 불러오는 function
    @GetMapping("/getListHgrb")
    public AjaxResult getListHgrb(){
        List<Map<String, Object>> items = this.requestService.getListHgrb();

        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }
    //보강재, 마감재 리스트 불러오는 function
    @GetMapping("/getListCgrb")
    public AjaxResult getListCgrb(){
        List<Map<String, Object>> ACgrb = this.requestService.getListACgrb();// 마감재
        List<Map<String, Object>> CCgrb = this.requestService.getListCCgrb();// 보강재
        Map<String, Object> items = new HashMap<>();
        items.put("ACgrb", ACgrb);
        items.put("CCgrb", CCgrb);
        AjaxResult result = new AjaxResult();
        result.data = items;
        return result;
    }
    // 유저정보 불러와 input태그 value
    @GetMapping("/getUserInfo")
    public AjaxResult getUserInfo(@RequestParam(value = "saupnum", required = false) String Saupnum,
                                  Authentication auth){
        String username;
        if (Saupnum != null && !Saupnum.isEmpty()) {
            username = Saupnum;
        } else {
            User user = (User) auth.getPrincipal();
            username = user.getUsername();
            System.out.println("username : " + username);
        }
        Map<String, Object> userInfo = requestService.getMyInfo(username);

        AjaxResult result = new AjaxResult();
        result.data = userInfo;
        return result;
    }
    @PostMapping("/uploadEditor")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String uploadDir = "c:\\temp\\editorFile\\";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        try {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File destinationFile = new File(uploadDir + fileName);
            file.transferTo(destinationFile);

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String fileUrl = baseUrl + "/editorFile/" + fileName; // 클라이언트 접근 URL

            return ResponseEntity.ok(Collections.singletonMap("location", fileUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "파일 업로드 실패: " + e.getMessage()));
        }
    }
    
    // 휴일 조회 메서드
    @GetMapping("/getHoliday")
    public AjaxResult getHoliday(){
        AjaxResult result = new AjaxResult();
        result.data = requestService.getHoliday();
        return result;
    }

    // 엑셀파일 조회 및 파일 보기 메서드
    @GetMapping("/readVacFile")
    public void readVacFile(@RequestParam("appnum") String appnum, HttpServletResponse response) throws Exception {
        Map<String, Object> vacData = new HashMap<>(); // paymentDetailService.getVacFileList(appnum);

        String frdateStr = vacData.get("frdate").toString();  // "YYYYMMDD"
        String todateStr = vacData.get("todate").toString();  // "YYYYMMDD"
        String daynum = vacData.get("daynum").toString();     //
        String reqdateStr = vacData.get("reqdate").toString();

        DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy 년 MM 월 dd 일");

        LocalDate frDate = LocalDate.parse(frdateStr, ymdFormatter);
        LocalDate toDate = LocalDate.parse(todateStr, ymdFormatter);
        LocalDate reqDate = LocalDate.parse(reqdateStr, ymdFormatter);

        String repodateFormat = String.format("%s  ~  %s  ( %s ) 일간",
                frDate.format(displayFormatter),
                toDate.format(displayFormatter),
                daynum);


        // 1. UUID 기반 임시 파일명 생성
        String uuid = UUID.randomUUID().toString();
        Path tempXlsx = Files.createTempFile(uuid, ".xlsx");
        Path tempPdf = Path.of(tempXlsx.toString().replace(".xlsx", ".pdf"));

        // 2. 엑셀 템플릿 불러오기 및 수정
        try (FileInputStream fis = new FileInputStream("C:/Temp/mes21/문서/VacDemoFile.xlsx");
             Workbook workbook = new XSSFWorkbook(fis);
             FileOutputStream fos = new FileOutputStream(tempXlsx.toFile())) {

            Sheet sheet = workbook.getSheetAt(0);
            // sheet.getRow(5).getCell(2).setCellValue((String) vacData.get("papernm")); // 서류구분 (휴가신청서)
//      sheet.getRow(2).getCell(2).setCellValue((String) vacData.get("worknm")); //  휴가구분 (연차, 반차, 병가 등)
//      sheet.getRow(9).getCell(2).setCellValue((String) vacData.get("repopernm")); // 휴가신청자 이름
//      sheet.getRow(7).getCell(2).setCellValue((String) vacData.get("jiknm")); // 직급명
//      sheet.getRow(5).getCell(2).setCellValue((String) vacData.get("departnm")); // 부서명
//      sheet.getRow(16).getCell(0).setCellValue((String) vacData.get("remark")); // 사유
//      sheet.getRow(11).getCell(2).setCellValue(repodateFormat); // 기간
//      sheet.getRow(24).getCell(3).setCellValue((String) vacData.get("worknm")); // 신청휴가구분
//      sheet.getRow(27).getCell(0).setCellValue(reqDate.format(displayFormatter)); // 신청일
//      sheet.getRow(29).getCell(10).setCellValue((String) vacData.get("repopernm")); // 제출인
            setCell(sheet, 2, 2, (String) vacData.get("worknm"));
            setCell(sheet, 9, 2, (String) vacData.get("repopernm"));
            setCell(sheet, 7, 2, (String) vacData.get("jiknm"));
            setCell(sheet, 5, 2, (String) vacData.get("departnm"));
            setCell(sheet, 16, 0, (String) vacData.get("remark"));
            setCell(sheet, 11, 2, repodateFormat);
            setCell(sheet, 24, 3, (String) vacData.get("worknm"));
            setCell(sheet, 27, 0, reqDate.format(displayFormatter));
            setCell(sheet, 30, 10, (String) vacData.get("repopernm"));

            workbook.write(fos);
        }

        // 3. LibreOffice로 PDF 변환
        ProcessBuilder pb = new ProcessBuilder(
                "C:/Program Files/LibreOffice/program/soffice.exe",
                "--headless",
                "--convert-to", "pdf",
                "--outdir", tempPdf.getParent().toString(),
                tempXlsx.toAbsolutePath().toString()
        );
        pb.inheritIO();
        Process process = pb.start();
        process.waitFor();

        // 4. PDF 응답 전송
        try (FileInputStream fis = new FileInputStream(tempPdf.toFile())) {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=vacation.pdf");
            IOUtils.copy(fis, response.getOutputStream());
            response.flushBuffer();
        }

        // 5. 일정 시간 후 임시파일 자동 삭제 (5분)
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                Files.deleteIfExists(tempXlsx);
                Files.deleteIfExists(tempPdf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 5, TimeUnit.MINUTES);
    }
    private void setCell(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);
        cell.setCellValue(value);
    }


}

