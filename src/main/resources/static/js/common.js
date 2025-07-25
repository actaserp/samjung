// 시스템 공통으로 사용되는 function script (default layout 사용)
// 공통콤보, validatin 등

'use strict';
//랜덤문자열 생성 50(랜덤문자열 42자리 + 오늘날짜 )자리로
function generateRandomStringWithDate(length = 32) {
    // 오늘 날짜를 "YYYYMMDD" 형식으로 가져오기
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 +1 필요
    const dd = String(today.getDate()).padStart(2, '0');
    const formattedDate = `${yyyy}${mm}${dd}`;

    // 지정된 길이의 랜덤 문자열 생성
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let randomString = '';
    for (let i = 0; i < length; i++) {
        randomString += characters.charAt(Math.floor(Math.random() * characters.length));
    }

    // 랜덤 문자열과 날짜를 결합하여 반환
    return `${randomString}${formattedDate}`;
}

var JQuery = {
    extends: function () {
        $.rest = function (url, data, callback, type, method) {
            if ($.isFunction(data)) {
                type = type || callback,
                    callback = data,
                    data = {}
            }

            return $.ajax({
                url: url,
                type: method,
                success: callback,
                data: data,
                contentType: type,
                beforeSend: function (xmlHttpRequest) {
                    xmlHttpRequest.setRequestHeader('AJAX', 'true'); // ajax 호출을  header에 기록
                }
            });
        }

        $.put = function (url, data, callback, type) {
            return $.rest(url, data, callback, type, 'PUT');
        }
        $.putsync = function (url, data, callback) {
            if ($.isFunction(data)) {
                type = type || callback,
                    callback = data,
                    data = {}
            }

            return $.ajax({
                url: url,
                type: 'PUT',
                success: callback,
                data: data,
                async: false,
                beforeSend: function (xmlHttpRequest) {
                    xmlHttpRequest.setRequestHeader('AJAX', 'true'); // ajax 호출을  header에 기록
                }
            });
        }

        $.delete = function (url, data, callback, type) {
            return $.rest(url, data, callback, type, 'DELETE');
        }

        $.fn.serializeObject = function () {
            var object = {};
            var array = this.serializeArray();

            $.each(array, function () {
                if (object[this.name] !== undefined) {
                    if (!object[this.name].push) {
                        object[this.name] = [object[this.name]];
                    }
                    object[this.name].push(this.value || '');
                } else {
                    object[this.name] = this.value || '';
                }
            });
            return object;
        };
    }
};

var Ajax = {
    defaults: {
        progressBarText: '레플러스 MES...'
    },
    setProgressBarText: function (text) {
        Ajax.defaults.progressBarText = text;
    },
    enableProgressBar: function () {
        var mask = new ax5.ui.mask();

        mask.setConfig({
            target: $('body', document).get(0)
        });


        //$(document).ajaxStart(function() {
        //  mask.open({
        //    content: '<h1><img src="/static/img/loading.svg"> ' + Ajax.defaults.progressBarText + '</h1>'
        //  });
        //});

        //$(document).ajaxStop(function() {
        //  setTimeout(function () {
        //    mask.close();
        //  }, 100);
        //});


    },
    enableErrorHandler: function () {
        $(document).ajaxError(function (evnet, xhr, settings, thrownError) {
            //Alert.alert('', JSON.parse(xhr.responseText).message);
            //var response = JSON.parse(xhr.responseText);
            //$.alert(response.message);
        });
    }
};

var Utils = {
    decodingTag: function (str) {
        if (str !== null) {
            str = str.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, "\"").replace(/&#39;/g, "\'")
                .replace(/&amp;gt;/g, "&gt;").replace(/&amp;nbsp;/g, "&nbsp;").replace(/&amp;amp;/g, "&").replace(/&amp;/g, "&");
        }
        return str;
    },
    decodingHTMLTag: function (str) {
        if (str !== null && typeof str === 'string') {
            str = str.replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&amp;/g, "&");
        }
        return str;
    },
    fnImagePreview: function (_noticeContent) {
        // 내용중 이미지 있을경우 클릭하여 원본크기로 팝업
        $(_noticeContent).attr('title', '클릭시 이미지원본보기');
        $(_noticeContent).on('click', function () {
            var imgCommonPreview = new Image();
            imgCommonPreview.src = $(this).attr('src');
            var scrollsize = 17;
            var swidth = screen.width - 10;
            var sheight = screen.height - 300;
            var wsize = imgCommonPreview.width
            var hsize = imgCommonPreview.height;
            if (wsize < 50) wsize = 50; // 가로 최소 크기 
            if (hsize < 50) hsize = 50; // 세로 최소 크기 
            if (wsize > swidth) wsize = swidth; // 가로 최대 크기 
            if (hsize > sheight) hsize = sheight; // 세로 최대 크기
            // 세로가 최대크기를 초과한경우 세로스크롤바 자리 확보 
            if ((wsize < swidth - scrollsize) && hsize >= sheight) wsize += scrollsize;
            // 가로가 최대크기를 초과한경우 가로스크롤바 자리 확보 
            if ((hsize < sheight - scrollsize) && wsize >= swidth) hsize += scrollsize;
            // 듀얼 모니터에서 팝업 가운데 정렬하기 
            var mtWidth = document.body.clientWidth;
            // 현재 브라우저가 있는 모니터의 화면 폭 사이즈 
            var mtHeight = document.body.clientHeight;
            // 현재 브라우저가 있는 모니터의 화면 높이 사이즈 
            var scX = window.screenLeft;
            // 현재 브라우저의 x 좌표(모니터 두 대를 합한 총 위치 기준) 
            var scY = window.screenTop;
            // 현재 브라우저의 y 좌표(모니터 두 대를 합한 총 위치 기준) 
            var popX = scX + (mtWidth - wsize) / 2 - 50;
            // 팝업 창을 띄울 x 위치 지정(모니터 두 대를 합한 총 위치 기준) 
            var popY = scY + (mtHeight - hsize) / 2 - 50;
            // 팝업 창을 띄울 y 위치 지정(모니터 두 대를 합한 총 위치 기준)
            var imageWin = window.open("", "", "top=" + 10 + ",left=" + popX + ",width=" + wsize + ",height=" + hsize + ",scrollbars=yes,resizable=yes,status=no");
            imageWin.document.write("<html><title>이미지 원본보기</title><body style='margin:0;cursor:pointer;' title='Close' onclick='window.close()'>");
            imageWin.document.write("<img src='" + imgCommonPreview.src + "'>");
            imageWin.document.write("</body></html>");
        });
    },
    addNewTab: function (_url, param) { // url로 새TAB페이지 열기
        $.getJSON('/pageinfo?targeturl=' + _url, function (datas) {
            if (parent.nthTabs.isExistsTab('#' + datas.objId)) {
                parent.nthTabs.toggleTab('#' + datas.objId);
            } else {
                parent.nthTabs.addTab({
                    id: String(datas.objId),
                    title: datas.objNm,
                    url: $.trim(datas.objUrl + param),
                    active: true,
                    allowClose: true
                });
            }
            // 신규 생성 후 북마크확인
            parent.fnCheckTabBookMark(String(datas.objId), datas.isbookmark);
        }).fail(function (e) {
            //      Notify.error('존재하지 않는 URL입니다.');
            //Notify.error(_msg_resource['valid.msg.url']);
            Alert.alert('Error', _msg_resource['valid.msg.url']);
        });
    },
    addNewTabPop: function (_url, param) { // url로 새TAB페이지 열기
        $.getJSON('/pageinfo?targeturl=' + _url, function (datas) {
            if (opener.nthTabs.isExistsTab('#' + datas.objId)) {
                opener.nthTabs.toggleTab('#' + datas.objId);
            } else {
                opener.nthTabs.addTab({
                    id: String(datas.objId),
                    title: datas.objNm,
                    url: $.trim(datas.objUrl + param),
                    active: true,
                    allowClose: true
                });
            }
            // 신규 생성 후 북마크확인
            opener.fnCheckTabBookMark(String(datas.objId), datas.isbookmark);
        }).fail(function (e) {
            //      Notify.error('존재하지 않는 URL입니다.');
            //Notify.error(_msg_resource['valid.msg.url']);
            Alert.alert('Error', _msg_resource['valid.msg.url']);
        });
    },
};

var CommonUtil = {
    onkeyupEnter: function (form) {
        if (window.event.keyCode == 13) {
            $(form).submit();
        }
    },
    onchangeCombobox: function (value, callback) {
        if (value != '') {
            $('#searchForm').submit();
            if (typeof callback == 'function') {
                callback();
            }
        }
    },
    getTimeStamp: function () {
        var d = new Date();
        var s =
            d.getFullYear() + '-' +
            d.getMonth() + 1 + '-' +
            d.getDate() + '_' +
            d.getHours() +
            d.getMinutes() +
            d.getSeconds();
        return s;
    },
    addDays: function (date, days) {
        var result = new Date(date);
        result.setDate(result.getDate() + days);
        return result;
    },
    getYYMMDD: function (_diff) {
        _diff = _diff || 0;
        var d = new Date();
        d.setDate(d.getDate() + _diff);
        var m = d.getMonth() + 1;
        var day = d.getDate();
        if (m < 10) {
            m = "0" + m;
        }
        if (day < 10) {
            day = "0" + day;
        }
        var str = d.getFullYear() + '-' + m + '-' + day;

        return str.substr(2, str.length - 2);
    },
    getYYYYMMDD: function (_diff) {
        _diff = _diff || 0;
        var d = new Date();
        d.setDate(d.getDate() + _diff);
        var m = d.getMonth() + 1;
        var day = d.getDate();
        if (m < 10) {
            m = "0" + m;
        }
        if (day < 10) {
            day = "0" + day;
        }
        var str = d.getFullYear() + '-' + m + '-' + day;
        return str;
    },
    formatYYYYMMDD: function (p_date) {
	    let m = p_date.getMonth() + 1;
	    let day = p_date.getDate();
	    if (m < 10) {
	        m = "0" + m;
	    }
	    if (day < 10) {
	        day = "0" + day;
	    }
	    var str = p_date.getFullYear() + '-' + m + '-' + day;
	    return str;
    },
    formatYYYYMMDDhhmmss: function (p_date) {
	    let m = this.zeoPadding(p_date.getMonth() + 1, 2);
	    let day = this.zeoPadding(p_date.getDate(), 2);
	    let hh = this.zeoPadding(p_date.getHours(), 2);
	    let mm = this.zeoPadding(p_date.getMinutes(), 2);
	    let ss = this.zeoPadding(p_date.getSeconds(), 2);
	    
	    var str = p_date.getFullYear() + '-' + m + '-' + day + ' ' + hh + ':' + mm + ':' + ss;
	    return str;
    },
    zeoPadding: function (number, length){
		var str = '' + number;
	    while (str.length < length) {
	      str = '0' + str;
	    }
  		return str;
	},
    //from to 숫자 입력값 유효성 체크(title 입력 필요)
    checkValidNumberRange: function (from, to) {
        if (Number(from.val()) > Number(to.val())) {
            let msg = from.attr("title") + ' 가 ' + to.attr("title") + ' 보다 높습니다'.
                //Alert.alert('', getMessage('valid.msg.validrange',[from.attr("title"),to.attr("title")]));
                Alert.alert('', msg);
            return false;
        }
        return true;
    },
    // 검색조건에 필수 여부 확인(title 입력 필요)
    isRequired: function (objs) {
        if (objs !== null && $.type(objs) === 'array') {
            $.each(objs, function (i) {
                if (objs[i].val() == "") {
                    Alert.alert('', objs[i].attr("title") + "을(를) 입력해 주십시오.");
                    //Alert.alert('', objs[i].attr("title")+getMessage('valid.msg.M000000041'));
                    return false;
                }
            });
        }
        return true;
    },
    CommaNumber: function (value) {
        // 수치값인 경우 3자리 기준으로 콤마를 넣어서 출력한다. 
        // 12345678.2345 -> 12,345,678.2345
        if (value == null)
            return null;
        let value2 = parseFloat(value);

        //if ( Number.isNaN(value2) ) return value;

        if (value2 == 0) return 0;

        let reg = /(^[+-]?\d+)(\d{3})/;
        let n = (value2 + '');

        while (reg.test(n)) n = n.replace(reg, '$1' + ',' + '$2');

        return n;
    },  // CommaNumber
    removeNullFromObject: function (dic) {
        // object의 value가 null 값인 경우 ''로 치환해 준다. 화면에 null로 표시되는 것을 방지하기 위해 사용.
        if (Array.isArray(dic)) {
            for (let obj of dic) {
                CommonUtil.removeNullFromObject(obj);
            }
        }
        for (const [key, value] of Object.entries(dic)) {
            if (Array.isArray(value))
                CommonUtil.removeNullFromObject(value);
            else if (value === null || value === undefined)
                dic[key] = '';
            else if (value.constructor.name === 'Object')
                CommonUtil.removeNullFromObject(value);
        }
        //return dic;
    },  // removeNullFromObject
    getParameters: function (paramName) {
        // 리턴값을 위한 변수 선언
        var returnValue;
        // 현재 URL 가져오기
        var url = location.href;
        // get 파라미터 값을 가져올 수 있는 ? 를 기점으로 slice 한 후 split 으로 나눔
        var parameters = (url.slice(url.indexOf('?') + 1, url.length)).split('&');

        // 나누어진 값의 비교를 통해 paramName 으로 요청된 데이터의 값만 return
        for (var i = 0; i < parameters.length; i++) {
            var varName = parameters[i].split('=')[0];
            if (varName.toUpperCase() == paramName.toUpperCase()) {
                returnValue = parameters[i].split('=')[1];
                return decodeURIComponent(returnValue);
            }
        }
    },
    openWindowPost: function (url, frm, options) {
        if (!options) {
            options = {};
        }

        if (!options.width) {
            options.width = 1024;
        }
        if (!options.height) {
            options.height = 768;
        }
        if (!options.layout) {
            options.layout = 'resizable=no, toolbar=no, menubar=no, location=no, status=no, scrollbars=yes';
        }
        if (!options.winname) {
            options.winname = '__window__' + Math.floor((Math.random() * 1000000) + 1);
        }

        var dualScreenLeft = window.screenLeft != undefined ? window.screenLeft : screen.left;
        var dualScreenTop = window.screenTop != undefined ? window.screenTop : screen.top;
        var screenWidth = window.innerWidth ? window.innerWidth : document.documentElement.clientWidth ? document.documentElement.clientWidth : screen.width;
        var screenHeight = window.innerHeight ? window.innerHeight : document.documentElement.clientHeight ? document.documentElement.clientHeight : screen.height;
        if (!options.left) {
            options.left = (screenWidth / 2) - (options.width / 2) + dualScreenLeft;
        }
        if (!options.top) {
            options.top = (screenHeight / 2) - (options.height / 2);
        }

        if (options.params) {
            var params = '';
            $.each(options.params, function (name, value) {
                if (params != '') {
                    params += '&';
                }
                params += name + '=' + value;

            });
            url += params ? '?' + params : '';
        }
        window.open('', options.winname, 'top=' + options.top + ', left=' + options.left + ', width=' + options.width + ', height=' + options.height + ', ' + options.layout);
        $('#' + frm).attr('action', url);
        $('#' + frm).attr('target', options.winname);
        $('#' + frm).submit();
        return false;
    },
    openWindow: function (url, options) {
        if (!options) {
            options = {};
        }

        if (!options.width) {
            options.width = 1024;
        }
        if (!options.height) {
            options.height = 768;
        }
        if (!options.layout) {
            options.layout = 'resizable=no, toolbar=no, menubar=no, location=no, status=no, scrollbars=yes';
        }
        if (!options.winname) {
            options.winname = '__window__' + Math.floor((Math.random() * 1000000) + 1);
        }

        var dualScreenLeft = window.screenLeft != undefined ? window.screenLeft : screen.left;
        var dualScreenTop = window.screenTop != undefined ? window.screenTop : screen.top;
        var screenWidth = window.innerWidth ? window.innerWidth : document.documentElement.clientWidth ? document.documentElement.clientWidth : screen.width;
        var screenHeight = window.innerHeight ? window.innerHeight : document.documentElement.clientHeight ? document.documentElement.clientHeight : screen.height;
        if (!options.left) {
            options.left = (screenWidth / 2) - (options.width / 2) + dualScreenLeft;
        }
        if (!options.top) {
            options.top = (screenHeight / 2) - (options.height / 2);
        }

        if (options.params) {
            var params = '';
            $.each(options.params, function (name, value) {
                if (params != '') {
                    params += '&';
                }
                params += name + '=' + value;

            });
            url += params ? '?' + params : '';
        }
        return window.open(url, options.winname, 'top=' + options.top + ', left=' + options.left + ', width=' + options.width + ', height=' + options.height + ', ' + options.layout);
    },
    onlyNumber: function (event) {
        // 숫자만 입력
        event = event || window.event;
        var keyID = (event.which) ? event.which : event.keyCode;
        if ((keyID >= 48 && keyID <= 57) || (keyID >= 96 && keyID <= 105) || keyID == 8 || keyID == 46 || keyID == 37 || keyID == 39)
            return;
        else
            return false;
    },
    removeChar: function (event) {
        //문자제거
        event = event || window.event;
        var keyID = (event.which) ? event.which : event.keyCode;
        if (keyID == 8 || keyID == 46 || keyID == 37 || keyID == 39)
            return;
        else
            event.target.value = event.target.value.replace(/[^0-9]/g, "");
    },
    sizeOf: function (obj) {
        if (obj !== null && obj !== undefined) {
            switch (typeof obj) {
                case 'number':
                    bytes += 8;
                    break;
                case 'string':
                    bytes += obj.length * 2;
                    break;
                case 'boolean':
                    bytes += 4;
                    break;
                case 'object':
                    var objClass = Object.prototype.toString.call(obj).slice(8, -1);
                    if (objClass === 'Object' || objClass === 'Array') {
                        for (var key in obj) {
                            if (!obj.hasOwnProperty(key)) continue;
                            CommonUtil.sizeOf(obj[key]);
                        }
                    }
                    else {
                        bytes += obj.toString().length * 2;
                    }
                    break;
            }
        }
        return bytes;
    },
    toggleFullScreen: function (element_id) {
        //let element = document.querySelector("body");
        let element = document.querySelector(element_id);
        if (!document.fullscreenElement) {
            if (element.requestFullscreen) return element.requestFullscreen()
            if (element.webkitRequestFullscreen)
                return element.webkitRequestFullscreen()
            if (element.mozRequestFullScreen) return element.mozRequestFullScreen()
            if (element.msRequestFullscreen) return element.msRequestFullscreen()
        } else {
            if (document.exitFullscreen) return document.exitFullscreen()
            if (document.webkitCancelFullscreen)
                return document.webkitCancelFullscreen()
            if (document.mozCancelFullScreen) return document.mozCancelFullScreen()
            if (document.msExitFullscreen) return document.msExitFullscreen()
        }
    },
    convertApprLineNameHtml: function (apprLine, apprLineState) {
        let ret = [];
        if (apprLine != null && apprLine != '') {
            let arrLine = apprLine.split(' ▶ ');
            let arrLineState = apprLineState.split('||');
            $.each(arrLineState, function (index, item) {
                let html = arrLine[index];
                if (item == 'process') {
                    html = '<span style="color:#5d9cec;">' + arrLine[index] + '</span>';
                }
                ret.push(html);
            });
        }
        return ret.join(' ▶ ');
    },
    convertApprStateCss: function (stateName, state) {
        let cssName = '';
        if (state == 'process') {
            cssName = 'grid-appr-state-blue';
        } else if (state == 'write') {
            cssName = 'grid-appr-state-yellow';
        }

        return cssName;
    }
};

/******************************************************************/
let FormUtil = {
    extractForm: function ($form, disabledFields = []) {
        let _this = this;
        let values = {};
        if ($form) {
            let form = $form.serializeArray();
            form.map(val => {
                values[val.name] = val.value;
            });
        }
        disabledFields.map(val => {
            values[val] = $form.find('#' + val).val();
        });

        return values;
    },
    // serialize시에 disabled값도 포함하여 serialize 리턴
    disabledSerialize: function (_useForm) {
        var disableds = _useForm.find(':input:disabled').removeAttr('disabled');
        var params = _useForm.serialize();
        disableds.attr('disabled', 'disabled');
        return params;
    },

    // 데이터를 Form내부 Control 에 바인딩 (name으로 매칭)
    BindDataForm: function (_resultSet, $form) {
        $.each(_resultSet, function (key, value) {
            // 빈스트링으로 오는 값은 반드시 null 값으로 치환한다. 또는 json 에서 null 로 넘겨준다
            // 치환하지 않고 빈스트링값('') 으로 처리하면 input[value=] 이렇게 되어 오류 발생함.
            if (key === '') value = null;
            if (value === '') value = null;

            var $frmCtl = $form.find('[name=' + key + ']');

            if ($frmCtl.length == 0)
                return true;
            let object = $frmCtl[0];
            var tagName = object === undefined ? '' : object.tagName.toUpperCase();
            var tagClassName = object === undefined ? '' : object.className.toUpperCase();
            let type_name = object.type;

            if (tagName == 'SELECT') {

                //if ($('#' + key).is(':disabled')) { $('#' + key).removeAttr('disabled'); }
                if ($frmCtl.is(':disabled')) { $frmCtl.removeAttr('disabled'); }
                //$('#' + key + ' > option').each(function () {
                //$('#' + key + ' > option').each(function () {
                //    $(this).removeProp('selected');
                //});
                //$('#' + key + ' > option[value=' + value + ']').prop('selected', true);
                //_f$formorm.find('[name=' + key + '] > option[value=' + value + ']').prop('selected', true);
                $frmCtl.val(value);
            } else if (tagName == 'INPUT' || tagName == 'TEXTAREA') {

                if ($frmCtl.is(':disabled')) { $frmCtl.removeAttr('disabled'); }
                if (type_name == 'checkbox') {
                    let checkValue = $frmCtl.val();
                    if (checkValue != undefined)
                        $frmCtl.prop('checked', value == checkValue);
                    else
                        $frmCtl.prop('checked', value);
                    //$frmCtl.attr('checked', value);
                } else if (type_name == 'radio') {
                    $frmCtl.removeAttr('checked');
                    var $radioCtl = $('input:radio[name=' + key + ']:input[value=' + value + ']');
                    $radioCtl.prop('checked', true);
                    $radioCtl.attr('checked', true);
                } else {
                    if ($.isNumeric(value) || value === null) {
                        $frmCtl.val(value);
                    } else {
                        $frmCtl.val(value.replace('&amp;', '&'));
                    }
                }
            } else if (tagName == 'SPAN') {
                if (tagClassName == 'DATE') {
                    var ddspan = new Date(value);
                    $frmCtl.text(ddspan.toLocaleString());
                } else {
                    $frmCtl.text(value);
                }
            }
        });
    }
};

let AjaxUtil = {
    showLoading: function () {
        try {
            window.parent.$('#loader2').show(); // 여기만 바꾸면 됨
        } catch (e) {}
    },
    hideLoading: function () {
        try {
            window.parent.$('#loader2').hide();
        } catch (e) {}
    },
    failureCallback: function (req, status, error) {
        let message = '에러가 발생했습니다.';

        if(req.status==401){
            message = '로그아웃되었습니다.';
            Alert.alert('Error', message);
        }
        else if(req.status==403){
            Alert.alert('Error', "권한이 없습니다.");
        }
        else if(req.status==404){
            Alert.alert('Error', "페이지를 찾을수 없습니다.");
        }
        else{
            try {
                message = JSON.parse(req.responseText).message;
            }
            catch (ex) {
            }
            //Notify.error(message);
            Alert.alert('Error', message);
            
        }
        
    },
    getSyncData: function (url, p_data, fn_failure) {
        let items = null;
        $.ajax({
            async: false,
            dataType: 'json',
            type: 'GET',
            url: url,
            data: p_data,
            success: function (res) {
                items = res;
            },
            error: function (req, status, error) {
                if (typeof fn_failure !== 'undefined') {
                    fn_failure(req, status, error);
                } else {
                    AjaxUtil.failureCallback(req, status, error);
                }
            }
        });

        return items;
    },
    getAsyncData: function (url, param_data, fn_success, fn_failure) {
        $.ajax({
            async: true,
            dataType: 'json',
            type: 'GET',
            url: url,
            data: param_data,
            success: function (res) {
                fn_success(res);
            },
            error: function (req, status, error) {
                if (typeof fn_failure !== 'undefined') {
                    fn_failure(req, status, error);
                } else {
                    AjaxUtil.failureCallback(req, status, error);
                }
            }
        });
    },

    // POST저장시에는 성공여부를 확인하여 분기하는 루틴이 많으므로, items만 리턴할 것이 아니라 
    // 성공여부와 메시지도 리턴한다
    postSyncData: function (url, param_data, fn_failure) {
        let result = null;
        let csrf = $('[name=_csrf]').val();

        if (param_data != null && typeof param_data === 'object') {
            param_data['_csrf'] = csrf;
        }
        
        $.ajax({
            async: false,
            dataType: 'json',
            type: 'POST',
            url: url,
            data: param_data,
            success: function (res) {
                result = res;
            },
            error: function (req, status, error) {
                if (typeof fn_failure !== 'undefined') {
                    fn_failure(req, status, error);
                } else {
                    AjaxUtil.failureCallback(req, status, error);
                }
            }
        });
        return result;
    },
    postAsyncData: function (url, param_data, fn_success, fn_failure) {
        let result = null;

        if (param_data != null && typeof param_data === 'object') {
            if (param_data.hasOwnProperty('_csrf') == false) {
                let csrf = $('[name=_csrf]').val();
                param_data['_csrf'] = csrf;
            }
        }

        $.ajax({
            async: true,
            dataType: 'json',
            type: 'POST',
            url: url,
            data: param_data,
            success: function (res) {
                fn_success(res);
            },
            error: function (req, status, error) {
                if (typeof fn_failure !== 'undefined') {
                    fn_failure(req, status, error);
                } else {

                    AjaxUtil.failureCallback(req, status, error);
                }
            }
        });
    },
    postJsonAsyncData: function (url, data, fn_success, fn_failure) {
        let result = null;

        let csrf = $('[name=_csrf]').val();

        // let spjangcd = sessionStorage.getItem('spjangcd');

        data = data || [];

        // if (Array.isArray(data)) {
        //     data = data.map(item => ({
        //         ...item,
        //         spjangcd: spjangcd
        //     }));
        // }

        AjaxUtil.showLoading();
        $.ajax({
            async: true,
            dataType: 'json',
            type: 'POST',
            url: url,
            contentType: 'application/json',
            data: JSON.stringify(data),
            beforeSend: function(xhr) {
                xhr.setRequestHeader('X-CSRF-TOKEN', csrf); // 헤더로 명시적 전달
            },
            success: function (res) {
                fn_success(res);
            },
            error: function (req, status, error) {
                if (typeof fn_failure !== 'undefined') {
                    fn_failure(req, status, error);
                } else {

                    AjaxUtil.failureCallback(req, status, error);
                }
            },
            complete: function () {
                AjaxUtil.hideLoading();
            }
        });
    },
    postFileSyncData: function (url, form_data, fn_failure) {
        let result = null;

        if (form_data != null && typeof form_data === 'object') {
            let csrf = $('[name=_csrf]').val();
            form_data.append("_csrf", csrf);
        }
        
        $.ajax({
            async: false,
            type: 'POST',
            url: url,
            data: form_data,
            processData: false,
            contentType: false,
            success: function (res) {
                result = res;
            },
            error: function (req, status, error) {
                if (typeof fn_failure !== 'undefined') {
                    fn_failure(req, status, error);
                    console.log(req, status, error)
                } else {
                    AjaxUtil.failureCallback(req, status, error);
                    console.log(req, status, error)
                }
            }
        });
        return result;
    },
    postFileAsyncData: function (url, form_data, fn_success, fn_failure) {
        let result = null;

        if (form_data != null && typeof form_data === 'object') {
            let csrf = $('[name=_csrf]').val();
            form_data.append("_csrf", csrf);
        }

        $.ajax({
            async: true,
            type: 'POST',
            url: url,
            data: form_data,
            processData: false,
            contentType: false,
            success: function (res) {
                fn_success(res);
            },
            error: function (req, status, error) {
                if (typeof fn_failure !== 'undefined') {
                    fn_failure(req, status, error);
                } else {

                    AjaxUtil.failureCallback(req, status, error);
                }
            }
        });
    },
    getSelectData: function (combo_type, cond1, cond2, cond3) {
        let data = {
            combo_type: combo_type,
        };
        if (cond1 !== undefined) {
            data.cond1 = cond1;
        }
        if (cond2 !== undefined) {
            data.cond2 = cond2;
        }
        if (cond3 !== undefined) {
            data.cond3 = cond3;
        }
        let ret = AjaxUtil.getSyncData('/api/common/combo', data);
        
        return ret.data == null ? []: ret.data;
    },
    getSelectDataWithNull: function (combo_type, null_option, condition1, condition2, condition3) {
        let ret = AjaxUtil.getSelectData(combo_type, condition1, condition2, condition3);
        let text = null_option;
        if (null_option == 'choose') {
            text = i18n.getCommonText('선택');//'선택하세요(Choose)';
        }
        else if (null_option == 'all') {
            text = i18n.getCommonText('전체'); //'전체(All
        }
        else {
            return ret;
        }

        let option = {
            'value': '',
            'text': text,
        };

        ret.unshift(option);

        return ret;
    },
    fillSelectOptions: function ($combo, combo_type, null_option, selected_value, condition1, condition2, condition3) {
        let rows = AjaxUtil.getSelectDataWithNull(combo_type, null_option, condition1, condition2, condition3);
        $combo.empty();
        $.each(rows, function (index, row) {
            //let option = $('<option>',
            //    {
            //        value: row['value'],
            //        text: row['text'],
            //    });
            let option = $('<option>');
            option.val(row['value']).text(row['text']);
            Object.keys(row).forEach(function (key) {
                
                if (key != 'value' && key != 'text') {
                    option.data(key, row[key]);
                }
            });

            $combo.append(option);
        });

        if (selected_value) {
            $combo.val(selected_value).change();
        }

        return rows;
    },
    fillSelectOption: function ($combo, combo_type, null_option, selected_value, condition1, condition2, condition3, filterFn) {
        let rows = AjaxUtil.getSelectDataWithNull(combo_type, null_option, condition1, condition2, condition3);

        // 🔍 필터 함수가 있으면 rows 필터링
        if (typeof filterFn === 'function') {
            rows = rows.filter(filterFn);
        }

        $combo.empty();
        $.each(rows, function (index, row) {
            let option = $('<option>');
            option.val(row['value']).text(row['text']);
            Object.keys(row).forEach(function (key) {
                if (key !== 'value' && key !== 'text') {
                    option.data(key, row[key]);
                }
            });
            $combo.append(option);
        });

        if (selected_value) {
            $combo.val(selected_value).change();
        }

        return rows;
    },
    fillSelectSyncData: function ($combo, url, param, null_option, selected_value) {
        $combo.empty();
        let rows = AjaxUtil.getSyncData(url, param);
        if (rows != null) {
            let text = null_option;
            if (null_option == 'choose') {
                text = '선택';//'선택하세요(Choose)';
            }
            else if (null_option == 'all') {
                text = '전체'; //'전체(All
            }
            if (text) {
                let option = {
                    'value': '',
                    'text': text,
                };

                rows.unshift(option);
            }

            $.each(rows, function (index, row) {
                let option = $('<option>',
                    {
                        value: row['value'],
                        text: row['text'],
                    });
                $combo.append(option);
            });

            if (selected_value) {
                $combo.val(selected_value).change();
            }
        }
    },
    sendSFlog: function (use_type, payload) {

        let url = 'https://log.smart-factory.kr/apisvc/sendLogData.json';
        let dataSize = 0;
        try {
            dataSize = CommonUtil.sizeOf(payload);
        } catch {
        }

        let logDt = moment().format('YYYY-MM-DD HH:mm:ss.SSS');
        let data = {
            crtfcKey: userinfo.crtfcKey,
            logDt: logDt,
            useSe: use_type,
            sysUser: userinfo.login_id,
            connectIp: userinfo.ip_address,
            dataUsgqty: dataSize
        };

        if (userinfo.crtfcKey == '') {
            return;
        }

        $.ajax({
            async: true,
            dataType: 'json',
            type: 'POST',
            url: url,
            data: data,
            success: function (res) {
            },
            error: function (req, staus, error) {
            }
        });
    },

    //파일업로드
    uploadFile(files, menu_name, pk, action) {
        //let formData = new FormData();
        //files.forEach(file => {
        //    formData.append('file', file);
        //});
        //formData.append('action', action);
        //formData.append('menu_name', menu_name);
        //formData.append('pk', pk);
        //$.ajax({
        //    type: 'post',
        //    url: yullin.getUrl({ api: 'pop/file_upload' }),
        //    processData: false,
        //    contentType: false,
        //    data: formData,
        //    success: function (resp) {
        //    },
        //    error: function (err) {
        //    }
        //});//ajax
    },

    //파일다운로드(GET)
    downloadFile(url, filename) {

        let downloadmask = new ax5.ui.mask();
        downloadmask.setConfig({
            target: $('body', document).get(0)
        });

        downloadmask.open({
            content: '<h1><img src="/img/loading.svg">로딩중...</h1>'
        });

        fetch(url)
            .then(resp => resp.blob())
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.style.display = 'none';
                a.href = url;
                // the filename you want
                a.download = filename;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                //alert('your file has downloaded!'); 
                downloadmask.close();
                //Notify.success('다운로드 성공');
            }).catch(() => {
                let message = '에러가 발생했습니다.관리자에게 문의 주세요.';
                //Notify.error(message);
                Alert.alert('Error', message);
                downloadmask.close();
            });

        //let url = yullin.getUrl({ api: api_url + '?' + param + '=' + val });
        //var link = document.createElement("a");
        //$(link).click(function (e) {
        //    e.preventDefault();
        //    window.location.href = url;
        //});
        //$(link).click();
        //$(link).remove();
    },
};

// 업무별 공통으로 사용되는 function script (default layout 사용)
// 버튼, TAB 권한, 대시보드 function 호출, QA 상세 호출, 녹취청취 호출등

// 버튼별 권한 처리
var authDisableBtn = function (data) {
    // servlet(modelandview에서 리턴된 권한버튼,탭 데이터로 show/hide 처리)
    // 화면별 권한에 따른 버튼 활성화 처리(show, hide)
    var btnRoleList = data.replace('[', '').replace(']', '').replace(/ /g, '').split(',');
    // [중요]권한제어할 button에는 btn-role-group 클래스명이 항상 포함되어야 함.
    $('.btn-role-group').each(function () {
        var _this = this;
        $(_this).addClass('btndisplaynone');
        $.each(btnRoleList, function () {
            var btntxt = this;
            if (_this.id == btntxt) {
                $(_this).removeClass('btndisplaynone');
            } else {
                if (btntxt.substr(btntxt.length - 1, btntxt.length) == '*') {
                    if (btntxt.substr(0, btntxt.length - 1) == _this.id.substr(0, btntxt.length - 1)) {
                        $(_this).show();
                        $(_this).removeClass('btndisplaynone');
                    }
                }
            }
        });
        $('.btndisplaynone').remove();
    });
};

// TAB별 권한처리
var authDisableTab = function (data) {
    // 화면별 권한에 따른 TAB(탭) 활성화 처리
    var tabRoleList = data.replace('[', '').replace(']', '').replace(/ /g, '').split(',');
    // [중요]권한제어할 TAP의 a태그에는 tab-role-group 클래스명이 항상 포함되어야 함.
    // tab li태그 하위의 a태그의 id로 권한 관리가 됨 - <li><a href="#tabs_detcd_tab" id="tabs_detcd" class="tab-role-group">
    // tab 화면을 삭제하기 위해 <div id="tabs_detcd_tab" class="tab-pane">처럼 tab화면 div의 ID를 'tab메뉴 a태그 id' + '_tab' 을 붙여야 함.
    $('.tab-role-group').each(function () {
        var _this = this;
        if (jQuery.inArray(_this.id, tabRoleList) < 0) {
            $('#' + _this.id).closest("li").remove();
            $('.' + _this.id + '_tab').remove();
        }
    });
    $('.tab-role-group').each(function () {
        $('#' + this.id).closest("li").addClass('active role');
        $('#' + this.id).trigger('click');
        $('.' + this.id + '_tab').addClass('active');
        return false;
    });
};

// 다국어 처리
var i18n = {
    modal: null,
    mask: null,
    commonModal: null,
    commonMask: null,
    dicMonth: {
        'ko-KR': ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
        'en-US': ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
    },
    dicDays: {
        'ko-KR': ['일', '월', '화', '수', '목', '금', '토'],
        'en-US': ['Sun', 'Mon', 'Thu', 'Wed', 'Thu', 'Fri', 'Sat'],
    },
    url: '/api/common/labels',
    storageKeys: ['lang_code', 'kr_common_common', 'en_common_common'],
    DEFAULT_LOCALE: 'ko-KR',
    commonResource: {},
    menuResource: {},
    guiResource: {},
    resetData: function () {
        let lang_cd = i18n.getLanguageCode();
        sessionStorage.clear();
        sessionStorage.setItem('lang_code', lang_cd);
    },
    initialize: function () {
        let lang_cd = i18n.getLanguageCode();
        i18n.initializeCommonData(lang_cd);
        i18n.initializeGUIData(lang_cd);
    },
    initializeMenuData: function (lang_cd) {
        let storageKey = lang_cd + '_common_menu';
        let menuStorageData = sessionStorage.getItem(storageKey);
        let loadMenuData = function (result) {
            if (result.length > 0) {
                sessionStorage.setItem(storageKey, JSON.stringify(result));
                i18n.menuResource = {};
                $.each(result, function (idx, item) {
                    i18n.menuResource[item.label_code] = { 'text': item.text, 'desc': item.descr };
                });
            }
            else {
                sessionStorage.setItem(storageKey, null);
            }
        };
        if (menuStorageData == null || menuStorageData == 'null') {
            let data = {
                lang_code: lang_cd,
                gui_code: 'common',
                template_key: 'menu',
                action: 'read'
            };
            let result = AjaxUtil.getSyncData(i18n.url, data);
            loadMenuData(result.data);
        } else {
            let result = JSON.parse(menuStorageData);
            loadMenuData(result);
        }
    },
    initializeCommonData: function (lang_cd) {
        let storageKey = lang_cd + '_common_common';
        let commonStorageData = sessionStorage.getItem(storageKey);
        let loadCommonData = function (result) {
            if (result.length > 0) {
                sessionStorage.setItem(storageKey, JSON.stringify(result));
                i18n.commonResource = {};
                $.each(result, function (idx, item) {
                    i18n.commonResource[item.label_code] = { 'text': item.text, 'desc': item.descr };
                });
            } else {
                sessionStorage.setItem(storageKey, null);
            }
            i18n.applyCommonLabel();
        };
        if (commonStorageData == null || commonStorageData == 'null') {
            let data = {
                lang_code: lang_cd,
                gui_code: 'common',
                template_key: 'common',
                action: 'read'
            };
            let fnsuccess = function (result) {
                loadCommonData(result.data);
            };
            AjaxUtil.getAsyncData(i18n.url, data, fnsuccess);
        }
        else {
            let result = JSON.parse(commonStorageData);
            loadCommonData(result);
        }
    },
    initializeGUIData: function (lang_cd) {
        if (gui.gui_code == '') {
            return;
        }
        let storageKey = lang_cd + '_' + gui.gui_code + '_' + gui.template_key;
        let guiStorageData = sessionStorage.getItem(storageKey);
        let loadGUIData = function (result) {
            if (result.length > 0) {
                sessionStorage.setItem(storageKey, JSON.stringify(result));
                i18n.guiResource = {};
                $.each(result, function (idx, item) {
                    i18n.guiResource[item.label_code] = { 'text': item.text, 'desc': item.descr };
                });
            } else {
                sessionStorage.setItem(storageKey, null);
            }
            i18n.applyGUILabel();
        };
        if (guiStorageData == null || guiStorageData == 'null') {
            let data = {
                gui_code: gui.gui_code,
                template_key: gui.template_key,
                lang_code: lang_cd,
                action: 'read'
            };
            let fnsuccess = function (result) {
                loadGUIData(result.data);
            };
            AjaxUtil.getAsyncData(i18n.url, data, fnsuccess);
        }
        else {
            let result = JSON.parse(guiStorageData);
            loadGUIData(result);
        }
    },
    getLanguageCode: function () {
        let lang_cd = sessionStorage.getItem('lang_code');
        if (lang_cd == null || lang_cd == 'null') {
            lang_cd = i18n.DEFAULT_LOCALE;
            sessionStorage.setItem('lang_code', lang_cd);
        }
        return lang_cd;
    },
    getMenuText: function (code, args) {
        let result = code;
        let exist = i18n.menuResource.hasOwnProperty(code);
        if (exist) {
            result = i18n.menuResource[code].text;
            if (args !== null && $.type(args) === 'array') {
                $.each(args, function (_idx, _val) {
                    result = result.replace('{' + _idx + '}', _val);
                });
            }
        } else {
        }
        return result;
    },
    getGUIText: function (code, args) {
        let result = code;
        let exist = i18n.guiResource.hasOwnProperty(code);
        if (exist) {
            result = i18n.guiResource[code].text;
            if (args !== null && $.type(args) === 'array') {
                $.each(args, function (_idx, _val) {
                    result = result.replace('{' + _idx + '}', _val);
                });
            }
        } else {
        }

        return result;
    },
    getGUITextDefault: function (code, args, defaultText) {
        let result = defaultText;
        if (i18n.guiResource.hasOwnProperty(code)) {
            result = i18n.guiResource[code].text;
            if (args !== null && $.type(args) === 'array') {
                $.each(args, function (_idx, _val) {
                    result = result.replace('{' + _idx + '}', _val);
                });
            }
        }
        return result;
    },
    getCommonText: function (code, args) {
        let result = code;
        let exist = i18n.commonResource.hasOwnProperty(code);
        if (exist) {
            result = i18n.commonResource[code].text;
            if (args !== null && $.type(args) === 'array') {
                $.each(args, function (_idx, _val) {
                    result = result.replace('{' + _idx + '}', _val);
                });
            }
        } else {
        }

        return result;
    },
    applyLabel: function () {
        i18n.applyCommonLabel();
        if (gui.gui_code != '') {
            i18n.applyGUILabel();
        }
    },
    applyGUILabel: function () {
        let $labels = $('[data-labelCd]');
        $labels.each(function () {
            var $this = $(this);
            let labelcd = $this.data('labelcd');
            let lbltext = i18n.getGUIText(labelcd);
            $this.text(lbltext);
            $this.attr('placeholder', lbltext);

            //if (getLabelDesc(labelcd) !== undefined) {
            //    // 라벨데이터에 설명데이터가 있을경우 & th태그의 label에만 ? 설명아이콘 활성화
            //    if ($this.prop('tagName') == 'TH') {
            //        $this.addClass('lbldescexist');
            //    } else {
            //        $this.attr('title', lbltext);
            //    }
            //} else {
            //    $this.attr('title', lbltext);
            //}
        });
        if (userinfo.group_code == 'admin') {
            $labels.unbind('contextmenu').bind('contextmenu', 'th', function (e) {
                let labelcd = $(this).data('labelcd');
                let lang_code = i18n.getLanguageCode();
                //let paramData = {
                //    lang_code: lang_code,
                //    label_code: labelcd,
                //    gui_code: gui.gui_code,
                //    template_key: gui.template_key
                //};
                i18n.modal = new ax5.ui.modal();
                i18n.mask = new ax5.ui.mask();
                let config = {
                    width: 350,
                    height: 480,
                    iframe: {
                        method: 'get',
                        url: '/page/popup/label',
                        param: 'lang_code=' + lang_code + '&label_code=' + labelcd + '&gui_code=' + gui.gui_code + '&template_key=' + gui.template_key
                    },
                    onStateChanged: function () {
                        if (this.state === 'open') {
                            i18n.mask.open();
                        }
                        else if (this.state === 'close') {
                            i18n.mask.close();
                        }
                    }
                };
                i18n.modal.open(config);
                //Ax5Modal.open({ url: '/page/popup/label', width: 480, height: 500, callbackfn: 'i18n.applyGUILabel', params: paramData });
                return false;
            });
        }
    },
    applyCommonLabel: function () {
        let $labels = $('[data-commonCd]');
        $labels.each(function () {
            var $this = $(this);
            let labelcd = $this.data('commoncd');
            let lbltext = i18n.getCommonText(labelcd);
            $this.text(lbltext);
            $this.attr('placeholder', lbltext);
        });
        if (userinfo.group_code == 'admin') {
            $labels.unbind('contextmenu').bind('contextmenu', 'th', function (e) {
                let labelcd = $(this).data('commoncd');
                let lang_code = i18n.getLanguageCode();
                let paramData = {
                    lang_code: lang_code,
                    label_code: labelcd,
                    gui_code: 'common',
                    template_key: 'common',
                    //callback: 'i18n.applyCommonLabel'
                };

                i18n.modal = new ax5.ui.modal();
                i18n.mask = new ax5.ui.mask();
                let width = 350;
                if (window.innerWidth < 350) {
                    width = 300;
                }
                let config = {
                    width: width,
                    height: 240,
                    iframe: {
                        method: 'get',
                        url: '/page/popup/label',
                        param: 'lang_code=' + lang_code + '&label_code=' + labelcd + '&gui_code=common&template_key=common'
                    },
                    onStateChanged: function () {
                        if (this.state === 'open') {
                            i18n.mask.open();
                        }
                        else if (this.state === 'close') {
                            i18n.mask.close();
                        }
                    }
                };
                i18n.modal.open(config);


                //Ax5Modal.open({ url: '/page/popup/label', width: 480, height: 500, callbackfn: null, params: paramData });
                return false;
            });
        }
    },
    applyContentLabel: function ($popupContent) {
        let $labels = $popupContent.find('[data-labelCd]');
        $labels.each(function () {
            var $this = $(this);
            let labelcd = $this.data('labelcd');
            let lbltext = i18n.getGUIText(labelcd);

            $this.text(lbltext);
            $this.attr('placeholder', lbltext);
        });

        if (userinfo.group_code == 'admin') {
            $labels.unbind('contextmenu').bind('contextmenu', 'th', function (e) {
                let labelcd = $(this).data('labelcd');
                let lang_code = i18n.getLanguageCode();
                //let paramData = {
                //    lang_code: lang_code,
                //    label_code: labelcd,
                //    gui_code: gui.gui_code,
                //    template_key: gui.template_key
                //};
                i18n.modal = new ax5.ui.modal();
                i18n.mask = new ax5.ui.mask();

                let config = {
                    width: 350,
                    height: 480,
                    iframe: {
                        method: 'get',
                        url: '/page/popup/label',
                        param: 'lang_code=' + lang_code + '&label_code=' + labelcd + '&gui_code=' + gui.gui_code + '&template_key=' + gui.template_key
                    },
                    onStateChanged: function () {
                        if (this.state === 'open') {
                            i18n.mask.open();
                        }
                        else if (this.state === 'close') {
                            i18n.mask.close();
                        }
                    }
                };
                i18n.modal.open(config);
                //Ax5Modal.open({ url: '/page/popup/label', width: 480, height: 500, callbackfn: 'i18n.applyGUILabel', params: paramData });
                return false;
            });
        }


        $labels = $('[data-commonCd]');
        $labels.each(function () {
            var $this = $(this);
            let labelcd = $this.data('commoncd');
            let lbltext = i18n.getCommonText(labelcd);
            $this.text(lbltext);
            $this.attr('placeholder', lbltext);
        });
        if (userinfo.group_code == 'admin') {
            $labels.unbind('contextmenu').bind('contextmenu', 'th', function (e) {
                let labelcd = $(this).data('commoncd');
                let lang_code = i18n.getLanguageCode();
                let paramData = {
                    lang_code: lang_code,
                    label_code: labelcd,
                    gui_code: 'common',
                    template_key: 'common',
                    //callback: 'i18n.applyCommonLabel'
                };

                i18n.modal = new ax5.ui.modal();
                i18n.mask = new ax5.ui.mask();
                let width = 350;
                if (window.innerWidth < 350) {
                    width = 300;
                }
                let config = {
                    width: width,
                    height: 240,
                    iframe: {
                        method: 'get',
                        url: '/page/popup/label',
                        param: 'lang_code=' + lang_code + '&label_code=' + labelcd + '&gui_code=common&template_key=common'
                    },
                    onStateChanged: function () {
                        if (this.state === 'open') {
                            i18n.mask.open();
                        }
                        else if (this.state === 'close') {
                            i18n.mask.close();
                        }
                    }
                };
                i18n.modal.open(config);
                //Ax5Modal.open({ url: '/page/popup/label', width: 480, height: 500, callbackfn: null, params: paramData });
                return false;
            });
        }


    },
    getMonthArrayText: function () {
        let langcd = i18n.getLanguageCode();
        if (i18n.dicMonth.hasOwnProperty(langcd)) {
            return i18n.dicMonth[langcd];
        } else {
            return i18n.dicMonth[i18n.DEFAULT_LOCALE];
        }
    },
    getDayArrayText: function () {
        let langcd = i18n.getLanguageCode();
        if (i18n.dicDays.hasOwnProperty(langcd)) {
            return i18n.dicDays[langcd];
        } else {
            return i18n.dicDays[i18n.DEFAULT_LOCALE];
        }
    },
};

//권한처리
let yullinAuth = {
    //inspection: function () {
    //    let $items = $('[data-authCd]');
    //    $items.each(function () {
    //        let $this = $(this);
    //        let authcd = $this.data('authcd');
    //        if (authcd == 'W') {
    //            if (userinfo.can_write() == false) {
    //                $this.remove();
    //            }
    //        }
    //    });
    //},
    removeWriteButton: function ($content) {
        let $items;
        if ($content)
            $items = $content.find('.y_write_auth');
        else
            $items = $('.y_write_auth');
        $items.each(function () {
            if (!userinfo.can_write())
                $(this).remove();
        });
    },
};

// 시간(hh24:mi)형식 validation
let DataValidation = { timeCheck: null, validateTime: null };

DataValidation.timeCheck = function (hours, minutes) {
    let i = 0;

    if (hours == "" || isNaN(hours) || parseInt(hours) > 23) {
        i++;
    } else if (parseInt(hours) == 0) {
        hours = "00";
    } else if (hours < 10 && hours.length < 2) {
        hours = "0" + hours;
    }

    if (minutes == "" || isNaN(minutes) || parseInt(minutes) > 59) {
        i++;
    } else if (parseInt(minutes) == 0) {
        minutes = "00";
    } else if (minutes < 10 && minutes.length < 2) {
        minutes = "0" + minutes;
    }

    if (i == 0) {
        return hours + ":" + minutes;
    } else {
            /*alert*/("Invalid Time Format.");
        return "";
    }
}

DataValidation.validateTime = function (obj) {
    /*
         _this.$addModal.find('#start_time').blur(function (event) {
                DataValidation.validateTime(event.target);
          });
     */
    let timeValue = obj.value;
    let sHours;
    let sMinutes;

    if (timeValue == "") {
            /*alert*/("Invalid Time format.");
        obj.value = "";
        return false;
    }
    else {
        if (timeValue.indexOf(":") > 0) {
            sHours = timeValue.split(':')[0];
            sMinutes = timeValue.split(':')[1];
            obj.value = DataValidation.timeCheck(sHours, sMinutes);
        }
        else {
            if (timeValue.length >= 4) {
                sHours = timeValue.substring(0, 2);
                sMinutes = timeValue.substring(2, 4);
                obj.value = DataValidation.timeCheck(sHours, sMinutes);
            }
            else if (timeValue.length == 3) {
                sHours = timeValue.substring(0, 2);
                sMinutes = timeValue.substring(2, 3);
                if (parseInt(sHours) > 23) {
                    sHours = timeValue.substring(0, 1);
                    sMinutes = timeValue.substring(1, 3);
                }
                obj.value = DataValidation.timeCheck(sHours, sMinutes);
            }
            else if (timeValue.length <= 2) {
                sHours = timeValue.substring(0, 2);
                sMinutes = '00';
                if (parseInt(sHours) > 23) {
                    sHours = timeValue.substring(0, 1);
                    sMinutes = timeValue.substring(1, 3);
                }
                obj.value = DataValidation.timeCheck(sHours, sMinutes);
            }
        }
        return true;
    }
}

var dynamicLinkCss = function (srctop, srcleft) {
    localStorage.setItem('theme-top', srctop);
    localStorage.setItem('theme-left', srcleft);
    $('link[href*="/static/css/theme-top"]').remove();
    $('link[href*="/static/css/theme-left"]').remove();

    var linkTop = document.createElement('link');
    linkTop.href = srctop + '.css';
    linkTop.async = false;
    linkTop.rel = 'stylesheet';
    linkTop.type = 'text/css';
    document.head.appendChild(linkTop);
    var linkLeft = document.createElement('link');
    linkLeft.href = srcleft + '.css';
    linkLeft.async = false;
    linkLeft.rel = 'stylesheet';
    linkLeft.type = 'text/css';
    document.head.appendChild(linkLeft);
    $('label[data-load-top-css="' + srctop + '"][data-load-left-css="' + srcleft + '"]').children('input[type=radio]').prop('checked', true);
};

var dynamicLinkCssPage = function (src) {
    $('link[href*="/static/css/theme-top"]').remove();
    var linkContent = document.createElement('link');
    linkContent.href = src + '.css';
    linkContent.async = false;
    linkContent.rel = 'stylesheet';
    linkContent.type = 'text/css';
    document.head.appendChild(linkContent);
};

