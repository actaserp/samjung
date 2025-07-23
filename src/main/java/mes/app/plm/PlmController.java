package mes.app.plm;

import lombok.extern.slf4j.Slf4j;
import mes.app.plm.service.PlmService;
import mes.domain.model.AjaxResult;
import mes.domain.repository.TB_CA664Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/plm/plm")
public class PlmController {  //프로젝트 관리

    @Autowired
    PlmService plmService;

    @Autowired
    TB_CA664Repository TBCA664Repository;


    private String formatDate8(String dateStr) {
        return dateStr != null ? dateStr.replace("-", "") : null;
    }

    // 오라클 db 에서 가져옴
    @GetMapping("/read_pj_synch")
    public AjaxResult getProjectListSynch(@RequestParam(value = "srchStartDt") String srchStartDt,
                                          @RequestParam(value = "srchEndDt") String srchEndDt,
                                          @RequestParam(value = "txtDescription") String txtDescription,
                                          @RequestParam(value = "input_flag") String input_flag,
                                          HttpServletRequest request) {

        srchStartDt = formatDate8(srchStartDt);
        srchEndDt = formatDate8(srchEndDt);
        List<Map<String, Object>> items = this.plmService.getProjectListSynch(srchStartDt, srchEndDt, txtDescription, input_flag);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @GetMapping("/read_bom_buffer")
    public AjaxResult readBomBuffer(@RequestParam String ecoNo, @RequestParam String projectNo, @RequestParam String erpyn) {
        List<Map<String, Object>> list = plmService.getBomAndPartBuffer(ecoNo, projectNo, erpyn);
        AjaxResult result = new AjaxResult();
        result.data = list;
        return result;
    }

    @GetMapping("/compare_bom_buffer")
    public AjaxResult compareBomBuffer(@RequestParam String ecoNo,
                                       @RequestParam String projectNo) {
        List<Map<String, Object>> diffList = plmService.compareBomBuffer(ecoNo, projectNo);

        AjaxResult result = new AjaxResult();
        result.data = diffList;
        return result;
    }

    @PostMapping("/save_plm")
    public AjaxResult savePlm(@RequestBody List<Map<String, Object>> itemList, Authentication auth) {
        AjaxResult result = new AjaxResult();
        try {

            String username = auth.getName();
            plmService.syncToMsSql(itemList, username);
            result.success = true;

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.success = false;
            return result;
        }
    }


}
