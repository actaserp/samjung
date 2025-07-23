package mes.domain.entity;


import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "TB_CA662")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
public class TB_CA662 {

    @Id
    @Column(name = "BPID")
    private String bpid;   // id

    @Column(name = "ECO_NO")
    private String eco_no;  // eco 번호

    @Column(name = "PART_NO")
    private String part_no;  // 품번

    @Column(name = "PART_NM")
    private String part_nm;  // 품명

    @Column(name = "PLM_VERSION")
    private String plm_version;  // 버전

    @Column(name = "BLOCK_NO")
    private String block_no; // 블럭번호

    @Column(name = "G_NO")
    private String g_no;  // g no

    @Column(name = "DRAWING_NO")
    private String brawing_no;  // 도면번호

    @Column(name = "GUBUN")
    private String gubun; // 내외작 구분

    @Column(name = "UNIT")
    private String unit;  // 단위

    @Column(name = "PART_SIZE")
    private String part_size;  // size

    @Column(name = "SPEC")
    private String spec;  // spec

    @Column(name = "ERP_YN")
    private String erp_yn;  // 전송여부

    @Column(name = "BPDATE")
    private String bpdate;  // 전송일자

    @Column(name = "BPPERNM")
    private String bppernm;  // 사용자

}
