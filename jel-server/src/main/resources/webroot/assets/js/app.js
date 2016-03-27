/* global Gauge */

(function () {

  function createGauge(name, elementId) {
    return new Gauge({
      renderTo: elementId,
      width: 400,
      height: 400,
      glow: false,
      units: '°C',
      title: name,
      minValue: 0,
      maxValue: 55,
      majorTicks: ['0', '5', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'],
      minorTicks: 10,
      strokeTicks: false,
      highlights: [
        {from: 0, to: 16, color: 'rgba(0,   0, 255, .3)'},
        {from: 23, to: 55, color: 'rgba(255, 0, 0, .3)'}
      ],
      colors: {
        plate: '#222',
        majorTicks: '#f5f5f5',
        minorTicks: '#ddd',
        title: '#fff',
        units: '#ccc',
        numbers: '#eee',
        needle: {
          start: 'rgba(240, 128, 128, 1)',
          end: 'rgba(255, 160, 122, .9)',
          circle: {
            outerStart: '#333',
            outerEnd: '#111',
            innerStart: '#111',
            innerEnd: '#222'
          },
          shadowUp: false,
          shadowDown: false
        },
        circle: {
          shadow: false,
          outerStart: '#333',
          outerEnd: '#111',
          middleStart: '#222',
          middleEnd: '#111',
          innerStart: '#111',
          innerEnd: '#333'
        },
        valueBox: {
          rectStart: '#222',
          rectEnd: '#333',
          background: '#babab2',
          shadow: 'rgba(0, 0, 0, 1)'
        }
      },
      valueBox: {
        visible: true
      },
      valueText: {
        visible: true
      },
      animation: {
        delay: 10,
        duration: 1500,
        fn: 'linear'
      }
    });
  }

  var gauge = createGauge('Kök', '1446390193');
  gauge.onready = function () {
    setInterval(function () {
      Gauge.Collection.get('1446390193').setValue(Math.random() * 50);
    }, 1500);
  };
  gauge.draw();

  gauge = createGauge('Vardagsrum', '157310299');
  gauge.onready = function () {
    setInterval(function () {
      Gauge.Collection.get('157310299').setValue(Math.random() * 50);
    }, 1500);
  };
  gauge.draw();

  gauge = createGauge('Hall/pannrum', '666657313');
  gauge.onready = function () {
    setInterval(function () {
      Gauge.Collection.get('666657313').setValue(Math.random() * 50);
    }, 1500);
  };
  gauge.draw();

  gauge = createGauge('Arbetsrum', '302851082');
  gauge.onready = function () {
    setInterval(function () {
      Gauge.Collection.get('302851082').setValue(Math.random() * 50);
    }, 1500);
  };
  gauge.draw();

  gauge = createGauge('Entré', '1544184166');
  gauge.onready = function () {
    setInterval(function () {
      Gauge.Collection.get('1544184166').setValue(Math.random() * 50);
    }, 1500);
  };
  gauge.draw();

  gauge = createGauge('Badrum', '92279315');
  gauge.onready = function () {
    setInterval(function () {
      Gauge.Collection.get('92279315').setValue(Math.random() * 50);
    }, 1500);
  };
  gauge.draw();

  gauge = createGauge('Varmvatten ut', '775199740');
  gauge.onready = function () {
    setInterval(function () {
      Gauge.Collection.get('775199740').setValue(Math.random() * 50);
    }, 1500);
  };
  gauge.draw();

  gauge = createGauge('Varmvatten in', '1186084518');
  gauge.onready = function () {
    setInterval(function () {
      Gauge.Collection.get('1186084518').setValue(Math.random() * 50);
    }, 1500);
  };
  gauge.draw();

  var eb = new EventBus(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port + '/eventbus');
  eb.onopen = function () {
    // set a handler to receive a message
    eb.registerHandler('jel.eventbus.external.devices', function (error, message) {
      console.log('received a message: ' + JSON.stringify(message));
    });
  };
})();