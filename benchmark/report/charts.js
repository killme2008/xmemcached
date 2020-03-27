var filesToParse = [
    { file: './logs/memcachedjava251.log', label: 'Memcached JC 2.5.1' },
    { file: './logs/spymemcached25.log', label: 'SpyMemcached 2.5' },
    { file: './logs/xmemcached1261.log', label: 'XMemcached 1.2.6.1' }
];

// expand if needed
var colorArray = ['#3e95cd', '#8e5ea2', '#3cba9f', '#e8c3b9', '#e8c3b9'];

function makeChart(ctx, ds) {
    var chart = new Chart(ctx, {
        type: 'line',
        data: ds,
        options: {
            title: {
                display: true,
                fontSize: 20,
                text: ds.title
            },
            tooltips: {
                mode: 'index'
            },
            cubicInterpolationMode: 'linear',
            scales: {
                yAxes: [{
                    scaleLabel: {
                        display: true,
                        fontSize: 20,
                        labelString: ds.yLabel
                    }
                }],
                xAxes: [{
                    scaleLabel: {
                        fontSize: 20,
                        display: true,
                        labelString: ds.xLabel
                    }
                }]
            }
        }
    });
};

function prepareDataset(dt, gF, gV, xL, yL) {
    var ds = dt.map(function(el, idx) {

        var filteredData = el.data.filter(function(e) { return e[gF] == gV });
        return {
            datasets: {
                label: el.label,
                backgroundColor: colorArray[idx],
                borderColor: colorArray[idx],
                data: filteredData.map(function(e) { return parseFloat(e[yL]) }),
                fill: false,
                // lineTension: 0, // no interpolation?
                cubicInterpolationMode: 'monotone'
            },
            labels: filteredData.map(function(e) { return e[xL] }),
        };
    });
    return {
        xLabel: xL,
        yLabel: yL,
        title: gF + " " + gV,
        datasets: ds.map(function(e) { return e.datasets }),
        labels: ds[0].labels // assume it should all be the same
    };
};

function loadFiles(files) {
    var pps = files.map(function(el) {
        return d3.csv(el.file).then(function(data) {
            console.log("Loaded " + el.file);
            return {
                label: el.label,
                data: data
            };
        });
    });
    return Promise.all(pps);
};

function getCavnases() {
    var els = document.getElementsByTagName('canvas');
    var arr = Array.from(els)
    console.log("Found canvas elements:")
    console.log(arr);
    return arr;
}

loadFiles(filesToParse).then(function(dt) {
    console.log("Loaded data: ");
    console.log(dt);

    getCavnases().forEach(function(el) {
        console.log("Operating on:");
        console.log(el);

        var gF = el.getAttribute('groupByField')
        var gV = el.getAttribute('groupByValue')
        var x = el.getAttribute('x')
        var y = el.getAttribute('y')

        var ds = prepareDataset(dt, gF, gV, x, y);
        console.log("Prepared dataset:");
        console.log(ds);

        makeChart(el.getContext('2d'), ds)
    });
})