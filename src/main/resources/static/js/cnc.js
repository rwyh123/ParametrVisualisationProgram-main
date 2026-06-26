document.addEventListener('DOMContentLoaded', function () {

        console.log("Загруженные пороги из Java:", window.APP_THRESHOLDS);
    // --- ВНЕДРЕНИЕ SVG-ПАТТЕРНА ДЛЯ ПОЛОСАТОГО ФОНА (КРАСНО-ЧЕРНЫЙ) ---
        function injectStripePattern() {
            const svgNS = "http://www.w3.org/2000/svg";
            const svg = document.createElementNS(svgNS, "svg");
            svg.setAttribute("style", "width:0;height:0;position:absolute;");

            const defs = document.createElementNS(svgNS, "defs");

            const pattern = document.createElementNS(svgNS, "pattern");
            pattern.setAttribute("id", "stripe-pattern");
            pattern.setAttribute("patternUnits", "userSpaceOnUse");
            pattern.setAttribute("width", "20");
            pattern.setAttribute("height", "20");
            pattern.setAttribute("patternTransform", "rotate(45)");

            const rect1 = document.createElementNS(svgNS, "rect");
            rect1.setAttribute("width", "10");
            rect1.setAttribute("height", "20");
            rect1.setAttribute("fill", "#ff0000"); // Красный

            const rect2 = document.createElementNS(svgNS, "rect");
            rect2.setAttribute("width", "10");
            rect2.setAttribute("height", "20");
            rect2.setAttribute("x", "10");
            rect2.setAttribute("fill", "#000000"); // Черный

            pattern.appendChild(rect1);
            pattern.appendChild(rect2);
            defs.appendChild(pattern);
            svg.appendChild(defs);
            document.body.appendChild(svg);
        }
        injectStripePattern();


    // =========================================================================
    // 1. ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ И НАСТРОЙКИ
    // =========================================================================

    const MAX_POINTS = 50; // Размер окна для графиков
    const charts = {};     // Хранилище объектов C3
    const chartBuffers = {}; // Буферы данных для графиков

    // === ИНИЦИАЛИЗАЦИЯ ВИЗУАЛИЗАТОРОВ ПУТИ (CANVAS) ===
    // Ищем все элементы с ID 'toolPathCanvas'.
    // [0] -> Левая панель (Machine CS)
    // [1] -> Правая панель (Work CS)
    const pathCanvases = document.querySelectorAll('#toolPathCanvas');

    // Создаем независимые контроллеры отрисовки
    const mcsVisualizer = new PathVisualizer(pathCanvases[0]);
    const wcsVisualizer = new PathVisualizer(pathCanvases[1]);

    /*
       СЛОВАРЬ МЭППИНГА (СВЯЗЬ ID БАЗЫ -> ЭЛЕМЕНТЫ UI)
    */
    const METRIC_MAP = {
        // --- TEMP SCREEN ---
        'z_temp':       { type: 'chart', id: 'tempChart',       label: 'z_temp' },
        'y_temp':       { type: 'chart', id: 'yTempChart',      label: 'y_temp' },
        'x_temp':       { type: 'chart', id: 'zTempChart',      label: 'x_temp' },
        'spindle_temp': { type: 'chart', id: 'spindleTempChart',label: 'spindle_temp' },

        // --- SPIN SCREEN ---
        'spindle_smoothed_speed_rpm':   { type: 'chart', id: 'spindleSmoothedSpeedChart', label: 'smooth_rpm' },
        'spindle_speed_rpm':            { type: 'chart', id: 'spindleSpeedChart',         label: 'speed_rpm' },
        'spindle_angular_velocity_rpm': { type: 'chart', id: 'spindleAngularVelocityChart',label: 'ang_vel' },
        // Используем вторую ось, чтобы заполнить 4-й график
        'RotaryPosition_2':             { type: 'chart', id: 'rotarityVelocityChart',     label: 'R_POS_2' },

        // --- VOLT SCREEN ---
        'ElectricCurrent_1': { type: 'chart', id: 'electricCurrentChart', label: 'Current' },
        'Voltage_1':         { type: 'chart', id: 'voltageChart',         label: 'Voltage' },
        'Power_1':           { type: 'chart', id: 'powerChart',           label: 'Power' },
        'Torque_1':          { type: 'chart', id: 'torqueChart',          label: 'Torque' },

        // --- CORD SCREEN (ЦИФРЫ) ---
        // Machine Coordinate System
        'z_mcs': { type: 'display', label: 'Z_MCS' },
        'x_mcs': { type: 'display', label: 'X_MCS' },
        'y_mcs': { type: 'display', label: 'Y_MCS' },

        // Work Coordinate System
        'z_wcs': { type: 'display', label: 'Z_WCS' },
        'x_wcs': { type: 'display', label: 'X_WCS' },
        'y_wcs': { type: 'display', label: 'Y_WCS' },

        // Rotary Position
        'RotaryPosition_1': { type: 'display', label: 'R_POS' },

        // --- COND SCREEN (СОБЫТИЯ) ---
        'Event_1': { type: 'event', label: 'EVENT 1' },
        'Event_2': { type: 'event', label: 'EVENT 2' }
    };


    // =========================================================================
    // 2. УПРАВЛЕНИЕ ИНТЕРФЕЙСОМ (ТАБЫ)
    // =========================================================================

    const buttons = document.querySelectorAll('.cnc-btn');
        const screens = document.querySelectorAll('.cnc-screen');

        function showScreen(key) {
            const targetId = 'screen-' + key;
            screens.forEach(s => s.classList.remove('cnc-screen--active'));
            const target = document.getElementById(targetId);
            if (target) {
                target.classList.add('cnc-screen--active');
                resizeVisibleCharts(target);
            }
        }

        // Словарь: связываем русский текст на кнопке с английским ID секции
        const SCREEN_MAP = {
            'глав': 'main',
            'корд': 'cord',
            'темп': 'temp',
            'обор': 'spin',
            'сеть': 'volt',
            'стат': 'cond'
        };

        buttons.forEach(btn => {
            btn.addEventListener('click', () => {
                buttons.forEach(b => b.classList.remove('cnc-btn--active'));
                btn.classList.add('cnc-btn--active');

                // Читаем текст кнопки
                let txt = btn.textContent.trim().toLowerCase();
                // Ищем английский ключ в словаре. Если не находим, используем сам текст
                let targetKey = SCREEN_MAP[txt] || txt;

                showScreen(targetKey);
            });
        });

        // Инициализация при старте
        initAllCharts();
        showScreen('main');


    // =========================================================================
    // 3. ПОДКЛЮЧЕНИЕ К SSE (ПОТОК ДАННЫХ)
    // =========================================================================

    const eventSource = new EventSource("/stream");

        eventSource.addEventListener("message", function(event) {
            try {
                const dataList = JSON.parse(event.data);
                if (Array.isArray(dataList)) {
                    // Буферы для одновременной фиксации координат из одного фрейма
                    let mcsX = null, mcsY = null;
                    let wcsX = null, wcsY = null;

                    dataList.forEach(ind => {
                        const metricId = ind.dataItem ? ind.dataItem.dataItemId : null;
                            if (!metricId) return;

                                        // 1. ПЕРЕХВАТ КООРДИНАТ: Собираем X и Y для одновременной отрисовки траектории
                                        if (metricId === 'x_mcs') mcsX = ind.value;
                                        if (metricId === 'y_mcs') mcsY = ind.value;
                                        if (metricId === 'x_wcs') wcsX = ind.value;
                                        if (metricId === 'y_wcs') wcsY = ind.value;

                                        // 2. ОБНОВЛЕНИЕ ИНТЕРФЕЙСА: Эта часть теперь выполняется ДЛЯ ВСЕХ метрик без исключения
                                        const config = METRIC_MAP[metricId];
                                        if (!config) return;

                                        const time = new Date(ind.time);
                                        const val = ind.value;
                                        const txt = ind.textValue;

                                        if (config.type === 'chart') {
                                            updateChartWidget(config.id, config.label, time, val);
                                        }
                                        else if (config.type === 'display') {
                                            updateDoubleDisplay(config.label, val);
                                        }
                                        else if (config.type === 'event') {
                                            updateEventPlate(config.label, txt);
                                        }
                    });

                    // Синхронно отправляем координаты на холсты только после полной сборки пакета
                    if (mcsX !== null || mcsY !== null) {
                        mcsVisualizer.update(mcsX, mcsY);
                    }
                    if (wcsX !== null || wcsY !== null) {
                        wcsVisualizer.update(wcsX, wcsY);
                    }
                }
            } catch (e) {
                console.error("Ошибка разбора SSE:", e);
            }
        });

        eventSource.onerror = function() {
            console.warn("SSE connection lost. Reconnecting...");
        };


    // =========================================================================
    // 4. МАРШРУТИЗАЦИЯ И ОБРАБОТКА ДАННЫХ
    // =========================================================================

    function processIndicator(ind) {
        // Получаем ID метрики (x_temp, x_mcs...)
        const metricId = ind.dataItem ? ind.dataItem.dataItemId : null;
        if (!metricId) return;

        // === А) СПЕЦИАЛЬНАЯ ЛОГИКА ДЛЯ ХОЛСТОВ (PATH) ===
        // Перехватываем координаты x/y для отрисовки пути

        // Machine CS -> Левый экран
        if (metricId === 'x_mcs') {
            mcsVisualizer.update(ind.value, null);
        } else if (metricId === 'y_mcs') {
            mcsVisualizer.update(null, ind.value);
        }

        // Work CS -> Правый экран
        else if (metricId === 'x_wcs') {
            wcsVisualizer.update(ind.value, null);
        } else if (metricId === 'y_wcs') {
            wcsVisualizer.update(null, ind.value);
        }

        // === Б) СТАНДАРТНАЯ ЛОГИКА ДЛЯ ВИДЖЕТОВ (ИЗ MAP) ===
        const config = METRIC_MAP[metricId];
        if (!config) return; // Если метрики нет в карте — игнорируем

        const time = new Date(ind.time);
        const val = ind.value;     // Число
        const txt = ind.textValue; // Текст (для событий)

        if (config.type === 'chart') {
            updateChartWidget(config.id, config.label, time, val);
        }
        else if (config.type === 'display') {
            updateDoubleDisplay(config.label, val);
        }
        else if (config.type === 'event') {
            updateEventPlate(config.label, txt);
        }
    }


    // =========================================================================
    // 5. ФУНКЦИИ ОБНОВЛЕНИЯ ВИДЖЕТОВ
    // =========================================================================

    // --- ГРАФИКИ (C3.js) ---
    function initAllCharts() {
        const plotContainers = document.querySelectorAll('.cnc-chart-screen__plot-inner');
        const now = new Date();

        plotContainers.forEach(el => {
            const chartId = el.id;
            if (!chartId) return;

            let metricKey = null;
            for (const key in METRIC_MAP) {
                if (METRIC_MAP[key].id === chartId) {
                    metricKey = key;
                    break;
                }
            }

            // Для теста жестко задаем 40.
            // Позже сможешь вернуть получение из window.APP_THRESHOLDS
            let threshold = 50.0;

            charts[chartId] = c3.generate({
                bindto: '#' + chartId,
                // Инициализируем опасную зону ОДИН РАЗ здесь
                regions: [
                    { axis: 'y', start: threshold, class: 'danger-zone' }
                ],
                data: {
                    x: 'x',
                    columns: [
                        ['x', now],
                        ['data', 0.0]
                    ],
                    type: 'line'
                },
                transition: { duration: 0 },
                axis: {
                    x: {
                        type: 'timeseries',
                        tick: { format: '%H:%M:%S', count: 5 }
                    },
                    y: {
                        tick: { format: d3.format('.1f') }
                        // Убрали max - теперь график масштабируется абсолютно свободно
                    }
                },
                legend: { show: false },
                point: { show: false },
                padding: { top: 5, right: 20, bottom: 0, left: 40 }
            });
        });
    }

    function updateChartWidget(chartId, label, timeObj, value) {
        if (value === null || value === undefined) return;

        // Буфер
        if (!chartBuffers[chartId]) chartBuffers[chartId] = [];
        const buffer = chartBuffers[chartId];

        buffer.push({ time: timeObj, value: value });
        if (buffer.length > MAX_POINTS) buffer.shift();

        // Обновление C3
        const chart = charts[chartId];
        if (chart) {
            const xCol = ['x', ...buffer.map(b => b.time)];
            const yCol = [label, ...buffer.map(b => b.value)];

            // =================================================================
            // ПОЛУЧЕНИЕ ДИНАМИЧЕСКОГО ПОРОГА ИЗ НАСТРОЕК (application.properties)
            // =================================================================
            let metricKey = null;
            for (const key in METRIC_MAP) {
                if (METRIC_MAP[key].id === chartId) {
                    metricKey = key;
                    break;
                }
            }

            // 50.0 — это запасное значение, если в файле вдруг забыли указать настройку
            let threshold = 50.0;
            if (metricKey && window.APP_THRESHOLDS && window.APP_THRESHOLDS[metricKey] !== undefined) {
                // parseFloat гарантирует, что мы получим число для правильных математических расчетов
                threshold = parseFloat(window.APP_THRESHOLDS[metricKey]);
            }
            // =================================================================

            chart.load({
                columns: [xCol, yCol],
                unload: ['data'],
                // Используем встроенный коллбэк, который срабатывает,
                // когда график закончил пересчет масштаба
                done: function() {
                    // Находим SVG именно этого графика
                    let svg = d3.select('#' + chartId + ' svg');

                    // Проверяем, что внутренний движок C3 готов
                    if (chart.internal && chart.internal.y) {
                        // chart.internal.y - это шкала. Она переводит значение (например, 40 из настроек) в пиксели.
                        let yPos = chart.internal.y(threshold);

                        if (!isNaN(yPos)) {
                            // В SVG координата Y=0 — это самый верх графика.
                            // Поэтому мы жестко привязываем прямоугольник к верху (y=0)
                            // и тянем его вниз ровно до линии порога (height = yPos).
                            svg.select('.c3-region.danger-zone rect')
                               .attr('y', 0)
                               .attr('height', Math.max(0, yPos));
                        }
                    }
                }
            });
        }
        updateChartFooter(chartId, label, timeObj, value);
    }

    function updateChartFooter(chartId, title, timeObj, value) {
        const el = document.getElementById(chartId);
        if (!el) return;
        const screen = el.closest('.cnc-chart-screen');
        if (!screen) return;

        const legendSpan = screen.querySelector('.cnc-chart-screen__legend span');
        const stats = screen.querySelectorAll('.cnc-chart-screen__stat span');

        if (legendSpan) legendSpan.textContent = title;
        if (stats[0]) stats[0].textContent = timeObj.toLocaleTimeString('ru-RU');
        if (stats[1]) stats[1].textContent = value.toFixed(2);
    }

    function resizeVisibleCharts(container) {
        const chartDivs = container.querySelectorAll('.cnc-chart-screen__plot-inner');
        chartDivs.forEach(div => {
            const id = div.id;
            if (charts[id]) charts[id].flush();
        });
    }

    // --- ЦИФРОВЫЕ ДИСПЛЕИ ---
    function updateDoubleDisplay(labelStr, value) {
        const allDisplays = document.querySelectorAll('.cnc-double-display');
        for (let disp of allDisplays) {
            const labelEl = disp.querySelector('.cnc-double-display__screen--label span');
            if (labelEl && labelEl.textContent.trim() === labelStr) {
                const valueEl = disp.querySelector('.cnc-double-display__screen--value span');
                if (valueEl) {
                    valueEl.textContent = (value !== null) ? value.toFixed(3) : "---";
                }
                break;
            }
        }
    }

    // --- СОБЫТИЯ (ЛАМПОЧКИ) ---
    function updateEventPlate(labelStr, statusText) {
        if (!statusText) return;
        const normalizedStatus = statusText.trim().toUpperCase();
        const allPlates = document.querySelectorAll('.cnc-event-plate');
        for (let plate of allPlates) {
            const labelEl = plate.querySelector('.cnc-event-plate__screen--label span');
            if (labelEl && labelEl.textContent.trim() === labelStr) {
                const stateScreen = plate.querySelector('.cnc-event-plate__screen--state');
                const blocks = stateScreen.querySelectorAll('.cnc-event-plate__state-block');
                if (blocks.length >= 2) {
                    const lampOn = blocks[0].querySelector('.cnc-lamp');
                    const lampOff = blocks[1].querySelector('.cnc-lamp');
                    if (normalizedStatus === 'ON' || normalizedStatus === 'ACTIVE') {
                        lampOn.classList.add('cnc-lamp--on'); lampOn.classList.remove('cnc-lamp--off');
                        lampOff.classList.add('cnc-lamp--off'); lampOff.classList.remove('cnc-lamp--on');
                    } else {
                        lampOn.classList.add('cnc-lamp--off'); lampOn.classList.remove('cnc-lamp--on');
                        lampOff.classList.add('cnc-lamp--on'); lampOff.classList.remove('cnc-lamp--off');
                    }
                }
                break;
            }
        }
    }


    // =========================================================================
    // 6. КЛАСС PATH VISUALIZER (ОТРИСОВКА ПУТИ)
    // =========================================================================

    function PathVisualizer(canvasElement) {
            if (!canvasElement) {
                this.isValid = false;
                return;
            }
            this.canvas = canvasElement;
            this.ctx = canvasElement.getContext('2d');
            this.isValid = true;

            // Стиль
            this.ctx.font = '12px "Share Tech Mono", monospace';
            this.ctx.textAlign = 'right';
            this.ctx.textBaseline = 'bottom';
            this.ctx.imageSmoothingEnabled = false;

            // Состояние (Изначально null, чтобы избежать паразитных линий от 0.0)
            this.state = {
                x: null,
                y: null,
                history: [],
                minX: 0, maxX: 0,
                minY: 0, maxY: 0
            };
        }

        PathVisualizer.prototype.update = function(newX, newY) {
            if (!this.isValid) return;

            // Обновляем внутреннее состояние только при наличии фактических данных
            if (newX !== null && newX !== undefined) this.state.x = newX;
            if (newY !== null && newY !== undefined) this.state.y = newY;

            // Блокируем отрисовку, пока не будет получена полноценная первая пара координат
            if (this.state.x === null || this.state.y === null) return;

            const p = { x: this.state.x, y: this.state.y };
            this.state.history.push(p);

            // Границы мира
            if (this.state.history.length === 1) {
                this.state.minX = this.state.maxX = p.x;
                this.state.minY = this.state.maxY = p.y;
            } else {
                if (p.x < this.state.minX) this.state.minX = p.x;
                if (p.x > this.state.maxX) this.state.maxX = p.x;
                if (p.y < this.state.minY) this.state.minY = p.y;
                if (p.y > this.state.maxY) this.state.maxY = p.y;
            }

            // Очистка памяти
            if (this.state.history.length > 5000) {
                this.state.history.shift();
            }

            this.draw();
        };

    PathVisualizer.prototype.draw = function() {
        if (!this.isValid) return;
        const ctx = this.ctx;
        const canvas = this.canvas;
        const state = this.state;

        // Фон
        ctx.fillStyle = '#000000';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        // Масштабирование
        const margin = 20;
        const w = canvas.width - margin * 2;
        const h = canvas.height - margin * 2;

        const rangeX = state.maxX - state.minX;
        const rangeY = state.maxY - state.minY;
        const spanX = rangeX === 0 ? 1 : rangeX;
        const spanY = rangeY === 0 ? 1 : rangeY;

        let scale = Math.min(w / spanX, h / spanY);

        const transformX = (x) => margin + (x - state.minX) * scale;
        const transformY = (y) => margin + (state.maxY - y) * scale; // Y инвертирован

        // Рисуем путь
        if (state.history.length > 1) {
            ctx.beginPath();
            ctx.strokeStyle = '#ffffff';
            ctx.lineWidth = 1.5;

            const start = state.history[0];
            ctx.moveTo(transformX(start.x), transformY(start.y));

            for (let i = 1; i < state.history.length; i++) {
                const pt = state.history[i];
                ctx.lineTo(transformX(pt.x), transformY(pt.y));
            }
            ctx.stroke();
        }

        // Рисуем головку (текущую позицию)
        const curX = state.history.length > 0 ? state.history[state.history.length-1].x : 0;
        const curY = state.history.length > 0 ? state.history[state.history.length-1].y : 0;

        const lx = transformX(curX);
        const ly = transformY(curY);

        ctx.fillStyle = '#00eaff';
        ctx.beginPath();
        ctx.arc(lx, ly, 3, 0, Math.PI * 2);
        ctx.fill();

        // Координаты
        const coordText = `X:${curX.toFixed(1)} Y:${curY.toFixed(1)}`;
        ctx.fillText(coordText, canvas.width - 5, canvas.height - 5);
    };
    // =========================================================================
        // 7. ЧАСЫ И ДАТА В ИНТЕРФЕЙСЕ
        // =========================================================================
        function updateDateTime() {
            const now = new Date();

            const hours = String(now.getHours()).padStart(2, '0');
            const minutes = String(now.getMinutes()).padStart(2, '0');

            const day = String(now.getDate()).padStart(2, '0');
            const month = String(now.getMonth() + 1).padStart(2, '0');
            const year = now.getFullYear();

            // Проверяем, существуют ли элементы, чтобы избежать ошибок,
            // если часов нет на каком-то из экранов
            const timeEl = document.getElementById('cnc-time');
            const dateEl = document.getElementById('cnc-date');

            if (timeEl) timeEl.textContent = `${hours} : ${minutes}`;
            if (dateEl) dateEl.textContent = `${day} / ${month} / ${year}`;
        }

        updateDateTime();
        setInterval(updateDateTime, 1000);

});