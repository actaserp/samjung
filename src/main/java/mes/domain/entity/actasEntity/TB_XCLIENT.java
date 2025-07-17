package mes.domain.entity.actasEntity;

import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = "TB_XCLIENT")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 거래처정보
public class TB_XCLIENT {

    @EmbeddedId
    private TB_XCLIENTId id; // 복합 키

    @Column(name = "cltnm")
    private String cltnm; // 거래처명

    @Column(name = "saupnum")
    private String saupnum; // 사업자번호

    @Column(name = "prenm")
    private String prenm; // 대표자명

    @Column(name = "rnumchk")
    private String rnumchk; // 주민번호여부

    @Column(name = "forenum")
    private String forenum; // 주민번호

    @Column(name = "corpperclafi")
    private String corpperclafi; // 법인구분  1: 법인, 0 : 개인

    @Column(name = "prenum")
    private String prenum; // 법인번호

    @Column(name = "biztype")
    private String biztype; // 업태코드

    @Column(name = "bizitem")
    private String bizitem; // 업태종류

    @Column(name = "biztypenm")
    private String biztypenm; // 업태명

    @Column(name = "bizitemnm")
    private String bizitemnm; // 종목명

   /* @Column(name = "cltdv")
    private String cltdv; // 거래처구분*/

    @Column(name = "zipcd")
    private String zipcd; // 우편번호

    @Column(name = "cltadres")
    private String cltadres; // 주소

    @Column(name = "telnum")
    private String telnum; // 전화번호

    @Column(name = "faxnum")
    private String faxnum; // FAX번호

    @Column(name = "liamt")
    private Float liamt; // 한도 금액

    @Column(name = "perid")
    private String perid; // 자사담당자

    @Column(name = "agnernm")
    private String agnernm; // 업체담당자명

    @Column(name = "agntel")
    private String agntel; // 업체담당자전화번호

   /* @Column(name = "agnhptel")
    private String agnhptel; // 업체담당자 HP번호*/

    @Column(name = "agnercltnm")
    private String agnercltnm; // 지급처명

    @Column(name = "spcd")
    private String spcd; // 관리대분류

    @Column(name = "area")
    private String area; // 관리중분류

    @Column(name = "team")
    private String team; // 관리소분류

    @Column(name = "azipcd")
    private String azipcd; // 지급처우편번호

    @Column(name = "aaddr")
    private String aaddr; // 지급처주소

    @Column(name = "upriceclafi")
    private String upriceclafi; // 도소매구분

    @Column(name = "prtcltnm")
    private String prtcltnm; // 인쇄거래처명

    @Column(name = "alclcd1")
    private String alclcd1; // ALCLCD1

    @Column(name = "alclcd2")
    private String alclcd2; // ALCLCD2

    @Column(name = "foreyn")
    private String foreyn; // 내외구분

    @Column(name = "relyn")
    private String relyn; // 거래중지구분

    @Column(name = "hptelnum")
    private String hptelnum; // 핸드폰번호

    @Column(name = "nation")
    private String nation; // 국가번호

    @Column(name = "homepage")
    private String homepage; // 홈페이지

    @Column(name = "agneremail")
    private String agneremail; // 담담자 email

  /*  @Column(name = "agneremail1")
    private String agneremail1; // 담당자 email1*/

    @Column(name = "agnerdivinm")
    private String agnerdivinm; // 담당자부서

    @Column(name = "remarks")
    private String remarks; // 비고

    @Column(name = "clttype")
    private String clttype; // 거래구분

    @Column(name = "bankcd")
    private String bankcd; // 은행코드

    /*@Column(name = "bankno")
    private String bankno; // 은행지점코드*/

    @Column(name = "accnum")
    private String accnum; // 계좌번호

    @Column(name = "deposit")
    private String deposit; // 예금주

    @Column(name = "accpernm")
    private String accpernm; // ACCPERNM

    @Column(name = "natcode")
    private String natcode; // 국가코드

    @Column(name = "setcls")
    private String setcls; // 결제 구분

   /* @Column(name = "taxdv")
    private String taxdv; // 계산서발행기준*/

    @Column(name = "givedate")
    private String givedate; // 지급일자

   /* @Column(name = "setcls1")
    private String setcls1; // 결제구분1

    @Column(name = "taxdv1")
    private String taxdv1; // 계산서발행기준1

    @Column(name = "givedate1")
    private String givedate1; // 지급일자 1*/

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark; // 적요

    @Column(name = "grade")
    private String grade; // 등급

    @Column(name = "lvcode")
    private String lvcode; // LVCODE

    @Column(name = "froff")
    private String froff; // 화물지점

    @Column(name = "operid")
    private String operid; // 현장담당자

    @Column(name = "opertel")
    private String opertel; // 현장담당자전화번호

    @Column(name = "ocltcd")
    private String ocltcd; // 구거래처코드

    @Column(name = "pperid")
    private String pperid; // 자사매입담당자

    @Column(name = "taxpernm")
    private String taxpernm; // 계산담당자

    @Column(name = "taxdivnm")
    private String taxdivnm; // 계산서담당부서

    @Column(name = "taxmail")
    private String taxmail; // 계산서메일

    @Column(name = "taxtelnum")
    private String taxtelnum; // 계산서담당전화번호

    @Column(name = "taxsms")
    private String taxsms; // 계산SMS

    /*@Column(name = "carrier")
    private String carrier; // 배송업체

    @Column(name = "payterm")
    private String payterm; // 결제기한*/

    /*@Column(name = "incoterm")
    private String incoterm; // 무역조건

    @Column(name = "nopa", columnDefinition = "TEXT")
    private String nopa; // 출항지

    @Column(name = "cltyul")
    private Float cltyul; // 할인율

    @Column(name = "moncls")
    private String moncls; // 화폐단위*/

    /*@Column(name = "cltynm")
    private String cltynm; // 약명*/

/*
    @Column(name = "wpass")
    private String wpass; // WPASS
    @Column(name = "taxcltcd")
    private String taxcltcd; // 계산서발행거래처
*/

    @Column(name = "nicnm1")
    private String nicnm1; // 가맹점1

    @Column(name = "nicnm2")
    private String nicnm2; // 가맹점2

    @Column(name = "nicnm3")
    private String nicnm3; // 가맹점3

    @Column(name = "nicnm4")
    private String nicnm4; // 가맹점4

    @Column(name = "nicnm5")
    private String nicnm5; // 가맹점5

    @Column(name = "nicnm6")
    private String nicnm6; // 가맹점6

    /*@Column(name = "prtdv")
    private String prtdv; // 보고서제외여부

    @Column(name = "stdate")
    private String stdate; // 거래개시일자

    @Column(name = "endate")
    private String endate; // 거래중지일자

    @Column(name = "vendor")
    private String vendor; // 납품처

    @Column(name = "officd")
    private String officd; // 점포코드

    @Column(name = "taxspcd")
    private String taxspcd; // 세무 SP코드

    @Column(name = "taxpncd")
    private String taxpncd; // 세무 PN코드

    @Column(name = "indate")
    private String indate; // 입력일자

    @Column(name = "inperid")
    private String inperid; // 입력자*/

    @Column(name = "inputdate")
    private String inputdate; // inputdate

    @Column(name = "bmar")
    private String bmar; // bmar

    @Column(name = "allchk")
    private String allchk; // allchk

    @Column(name = "emcltcd")
    private String emcltcd; // emcltcd

    /*@Column(name = "pnbcltcd")
    private String pnbcltcd; // pnbcltcd

    @Column(name = "swcltcd")
    private String swcltcd; // swcltcd*/
}
