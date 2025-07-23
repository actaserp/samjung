package mes.domain.entity;


import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "TB_CA664") // 프로젝트
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class TB_CA664 {

    @Id
    @Column(name = "BOMID")
    private String bomid;   // id

    @Column(name = "ECO_NO")
    private String eco_no;  // eco 번호

    @Column(name = "PROJECT_NO")
    private String project_no;  // 수주번호

    @Column(name = "PROJECT_NM")
    private String project_nm;  // 수주명

    @Column(name = "BPDATE")
    private String bpdate;  // 전송일자

    @Column(name = "BPPERNM")
    private String bppernm; // 사용자

}
