function goto(requestParamKey, requestParamValue){
    window.location.href = replaceQueryParam(requestParamKey, requestParamValue)
}

// Replace or add query param if existing or not, respectively
function replaceQueryParam(requestParamKey, requestParamValue) {

    let url = getCurrentURL()

    let strRegExPattern = '\\b' + requestParamKey + '\\b=([^&#]*)';

    if (url.match(requestParamKey)) {
        url = url.replace(new RegExp(strRegExPattern), `${requestParamKey}=${requestParamValue}`)
    } else {
        if (!url.includes("?")) {
            url += '?'
        } else {
            url += "&"
        }

        url += `${requestParamKey}=${requestParamValue}`
    }

    return url
}

function getCurrentURL() {
    return window.location.href
}

function initLangToggle() {
    const btn = document.getElementById('langToggleBtn');
    const dropdown = document.getElementById('langDropdown');
    if (!btn || !dropdown) return;

    btn.addEventListener('click', function (e) {
        e.stopPropagation();
        const open = dropdown.classList.toggle('open');
        btn.classList.toggle('open', open);
    });

    document.addEventListener('click', function () {
        dropdown.classList.remove('open');
        btn.classList.remove('open');
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initLangToggle);
} else {
    initLangToggle();
}
