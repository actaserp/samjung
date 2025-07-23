package mes.domain.entity;


import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "TB_CA663")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class TB_CA663 {

    @Id
    @Column(name = "BOMID")
    private String bomid;   // id

    @Column(name = "ECO_NO")
    private String eco_no;  // eco 번호

    @Column(name = "PROJECT_NO")
    private String project_no;  // 수주 번호

    @Column(name = "HOGI_NO")
    private String hogi_no;  // 호기번호

    @Column(name = "PARENT_NO")
    private String parent_no;  // 모품목 번호

    @Column(name = "CHILD_NO")
    private String child_no; // 자품목 번호

    @Column(name = "SEQ")
    private String seq;  // 시퀀스

    @Column(name = "QTY")
    private String qty;  // 수량

    @Column(name = "CMT")
    private String cmt; // 주석

    @Column(name = "BPDATE")
    private String bpdate;  // 전송일자

    @Column(name = "BPPERNM")
    private String bppernm;  // 사용자

}
