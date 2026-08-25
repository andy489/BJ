// https://openweathermap.org/weather-conditions

const boxImgA = document.getElementById('box-a-img')
const boxImgB = document.getElementById('box-b-img')
const boxImgC = document.getElementById('box-c-img')
const boxImgD = document.getElementById('box-d-img')

const boxTempA = document.getElementById('box-a-temp')
const boxTempB = document.getElementById('box-b-temp')
const boxTempC = document.getElementById('box-c-temp')
const boxTempD = document.getElementById('box-d-temp')

const KELVIN_ZERO = -273.15;
const weatherImagesPath = '/images/weather/'

function updateDigitalTime(timeEl, utcOffsetSeconds) {
    if (!timeEl) return;
    const now = new Date();
    const utcMs = now.getTime() + now.getTimezoneOffset() * 60000;
    const local = new Date(utcMs + utcOffsetSeconds * 1000);
    const hh = String(local.getHours()).padStart(2, '0');
    const mm = String(local.getMinutes()).padStart(2, '0');
    timeEl.textContent = hh + ':' + mm;
}

function drawClock(canvas, utcOffsetSeconds) {
    const ctx = canvas.getContext('2d');
    const size = canvas.width;
    const cx = size / 2, cy = size / 2, r = size / 2 - 4;

    const isDark = document.documentElement.getAttribute('data-theme') !== 'light';
    const faceColor   = isDark ? 'rgba(20,16,8,0.85)'   : 'rgba(255,252,240,0.92)';
    const rimColor    = isDark ? '#ffd347'               : '#a07800';
    const tickColor   = isDark ? 'rgba(255,211,71,0.8)'  : 'rgba(100,70,0,0.7)';
    const hourColor   = isDark ? '#f5f0e0'               : '#1a1100';
    const minColor    = isDark ? '#f5f0e0'               : '#1a1100';
    const secColor    = '#e05050';
    const centerColor = isDark ? '#ffd347'               : '#a07800';

    ctx.clearRect(0, 0, size, size);

    // Face
    ctx.beginPath();
    ctx.arc(cx, cy, r, 0, 2 * Math.PI);
    ctx.fillStyle = faceColor;
    ctx.fill();
    ctx.strokeStyle = rimColor;
    ctx.lineWidth = 2.5;
    ctx.stroke();

    // Hour ticks
    for (let i = 0; i < 12; i++) {
        const angle = (i / 12) * 2 * Math.PI - Math.PI / 2;
        const isMain = i % 3 === 0;
        const tickLen = isMain ? r * 0.18 : r * 0.10;
        ctx.beginPath();
        ctx.moveTo(cx + Math.cos(angle) * (r - tickLen), cy + Math.sin(angle) * (r - tickLen));
        ctx.lineTo(cx + Math.cos(angle) * (r - 2),       cy + Math.sin(angle) * (r - 2));
        ctx.strokeStyle = tickColor;
        ctx.lineWidth = isMain ? 2 : 1;
        ctx.stroke();
    }

    // Local time
    const now = new Date();
    const utcMs = now.getTime() + now.getTimezoneOffset() * 60000;
    const local = new Date(utcMs + utcOffsetSeconds * 1000);
    const h = local.getHours() % 12;
    const m = local.getMinutes();
    const s = local.getSeconds();

    const secAngle  = (s / 60)        * 2 * Math.PI - Math.PI / 2;
    const minAngle  = ((m + s / 60) / 60) * 2 * Math.PI - Math.PI / 2;
    const hourAngle = ((h + m / 60) / 12) * 2 * Math.PI - Math.PI / 2;

    // Hour hand
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + Math.cos(hourAngle) * r * 0.52, cy + Math.sin(hourAngle) * r * 0.52);
    ctx.strokeStyle = hourColor;
    ctx.lineWidth = 3.5;
    ctx.lineCap = 'round';
    ctx.stroke();

    // Minute hand
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + Math.cos(minAngle) * r * 0.74, cy + Math.sin(minAngle) * r * 0.74);
    ctx.strokeStyle = minColor;
    ctx.lineWidth = 2.5;
    ctx.lineCap = 'round';
    ctx.stroke();

    // Second hand
    ctx.beginPath();
    ctx.moveTo(cx - Math.cos(secAngle) * r * 0.18, cy - Math.sin(secAngle) * r * 0.18);
    ctx.lineTo(cx + Math.cos(secAngle) * r * 0.88, cy + Math.sin(secAngle) * r * 0.88);
    ctx.strokeStyle = secColor;
    ctx.lineWidth = 1.2;
    ctx.lineCap = 'round';
    ctx.stroke();

    // Center dot
    ctx.beginPath();
    ctx.arc(cx, cy, 3.5, 0, 2 * Math.PI);
    ctx.fillStyle = centerColor;
    ctx.fill();
}

function startClock(canvas, timeEl, utcOffsetSeconds) {
    drawClock(canvas, utcOffsetSeconds);
    updateDigitalTime(timeEl, utcOffsetSeconds);
    setInterval(() => {
        drawClock(canvas, utcOffsetSeconds);
        updateDigitalTime(timeEl, utcOffsetSeconds);
    }, 1000);
}

fetch('https://api.openweathermap.org/data/2.5/weather?q=sofia&appid=8dd1b8c6c70655b59ef4f75b4d9fb753')
    .then(data => data.json())
    .then(info => {
        boxTempA.innerText = Math.round(info.main.temp + KELVIN_ZERO)
        boxImgA.src = weatherImagesPath + info.weather[0].icon + '.png'
        startClock(document.getElementById('clock-a'), document.getElementById('clock-a-time'), info.timezone)
    })

fetch('https://api.openweathermap.org/data/2.5/weather?q=lasvegas&appid=8dd1b8c6c70655b59ef4f75b4d9fb753')
    .then(data => data.json())
    .then(info => {
        boxTempB.innerText = Math.round(info.main.temp + KELVIN_ZERO)
        boxImgB.src = weatherImagesPath + info.weather[0].icon + '.png'
        startClock(document.getElementById('clock-b'), document.getElementById('clock-b-time'), info.timezone)
    })

fetch('https://api.openweathermap.org/data/2.5/weather?q=monaco&appid=8dd1b8c6c70655b59ef4f75b4d9fb753')
    .then(data => data.json())
    .then(info => {
        boxTempC.innerText = Math.round(info.main.temp + KELVIN_ZERO)
        boxImgC.src = weatherImagesPath + info.weather[0].icon + '.png'
        startClock(document.getElementById('clock-c'), document.getElementById('clock-c-time'), info.timezone)
    })

fetch('https://api.openweathermap.org/data/2.5/weather?q=macao&appid=8dd1b8c6c70655b59ef4f75b4d9fb753')
    .then(data => data.json())
    .then(info => {
        boxTempD.innerText = Math.round(info.main.temp + KELVIN_ZERO)
        boxImgD.src = weatherImagesPath + info.weather[0].icon + '.png'
        startClock(document.getElementById('clock-d'), document.getElementById('clock-d-time'), info.timezone)
    })
