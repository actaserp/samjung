package mes.app.balju;

import lombok.extern.slf4j.Slf4j;
import mes.app.balju.service.BalJuListServicr;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/balju/balju_list")
public class BalJuListController {

  @Autowired
  BalJuListServicr balJuListServicr;

  @GetMapping("/read")
  public AjaxResult getBalJuList(@RequestParam(value = "clt" ,required = false) String cltnm,
                                 @RequestParam(value = "actnm", required = false) String  actnm,
                                 @RequestParam(value = "date_kind", required = false) String date_kind,
                                 @RequestParam(value = "start", required = false )String start_date,
                                 @RequestParam(value = "end", required = false)String end_date,
                                 Authentication auth){
    //log.info("발주 상세 list들어옴");
    User user = (User) auth.getPrincipal();
    String spjangcd = user.getSpjangcd();

    start_date = start_date + " 00:00:00";
    end_date = end_date + " 23:59:59";

    Timestamp start = Timestamp.valueOf(start_date);
    Timestamp end = Timestamp.valueOf(end_date);

    List<Map<String, Object>> items = this.balJuListServicr.getBalJuList(cltnm, actnm, date_kind, start, end, spjangcd);

    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

}
