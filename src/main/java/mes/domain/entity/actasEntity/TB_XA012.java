package mes.domain.entity.actasEntity;

import groovy.transform.builder.Builder;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

import javax.persistence.*;
import java.math.BigDecimal;

@Builder
@Entity
@Table(name = "TB_XA012")
@Setter
@Getter
@NoArgsConstructor
public class TB_XA012 {

    @EmbeddedId
    private TB_XA012ID id;

    @Column(name = "saupnum")
    private String saupnum; // 사업자 번호

    @Column(name = "spjangnm")
    private String spjangnm;

    @Column(name = "compnm")
    private String compnm; // 업체명

    @Column(name = "prenm")
    private String prenm; // 대표자

    @Column(name = "openymd")
    private String openymd;

    @Column(name = "custperclsf")
    private String custperclsf;

    @Column(name = "corpnum")
    private String corpnum;

    @Column(name = "jointsaup")
    private String jointsaup;

    @Column(name = "zipcd")
    private String zipcd; // 우편번호

    @Column(name = "adresa")
    private String adresa; // 주소1

    @Column(name = "adresb")
    private String adresb; // 주소2

    @Column(name = "zipcd2")
    private String zipcd2; // 추가 우편번호

    @Column(name = "adres2a")
    private String adres2a; // 추가 주소1

    @Column(name = "adres2b")
    private String adres2b; // 추가 주소2

    @Column(name = "biztype")
    private String biztype;

    @Column(name = "item")
    private String item;

    @Column(name = "tel1")
    private String tel1; // 전화번호1

    @Column(name = "tel2")
    private String tel2; // 전화번호2

    @Column(name = "fax")
    private String fax; // 팩스 번호

    @Column(name = "buzclaif")
    private String buzclaif;

    @Column(name = "comtaxoff")
    private String comtaxoff;

    @Column(name = "emailadres")
    private String emailadres; // 이메일 주소

    @Column(name = "passwd")
    private String passwd; // 비밀번호

    @Column(name = "astvaluemet")
    private String astvaluemet;

    @Column(name = "agnertel1")
    private String agnertel1;

    @Column(name = "agnertel2")
    private String agnertel2;

    @Column(name = "stdate")
    private String stdate;

    @Column(name = "eddate")
    private String eddate;

    @Column(name = "operdivsn")
    private String operdivsn;

    @Column(name = "tel3")
    private String tel3; // 전화번호3

    @Column(name = "allpay")
    private String allpay;

    @Column(name = "halfpay")
    private String halfpay;

    @Column(name = "taxagentcd")
    private String taxagentcd;

    @Column(name = "taxagentnm")
    private String taxagentnm;

    @Column(name = "ctano")
    private String ctano;

    @Column(name = "taxagentsp")
    private String taxagentsp;

    @Column(name = "taxagenttel")
    private String taxagenttel;

    @Column(name = "guchung")
    private String guchung;
 /*
    @Column(name = "espjangnm")
    private String espjangnm;

   @Column(name = "ezipcd")
    private String ezipcd;

    @Column(name = "eadresa")
    private String eadresa;

    @Column(name = "eadresb")
    private String eadresb;

    @Column(name = "etelno")
    private String etelno;

    @Column(name = "efaxno")
    private String efaxno;

    @Column(name = "epernm")
    private String epernm;*/

    @Column(name = "REQCUSTCD")
    private String reqcustcd;

    @Column(name = "bnkcode")
    private String bnkcode;

    @Column(name = "bnkjijum")
    private String bnkjijum;

    @Column(name = "cmsmemo")
    private String cmsmemo;

    @Column(name = "cmspass")
    private String cmspass;

    @Column(name = "cmsday")
    private BigDecimal cmsday;

    @Column(name = "poppw")
    private String poppw;

    @Column(name = "popid")
    private String popid;

    @Column(name = "popidmsg")
    private String popidmsg;


}
